// Manages emergency alert execution: retrieves device location, sends alert SMS to all contacts, and calls the primary contact.
package com.example.elderpal.data

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import android.util.Base64
import androidx.core.content.ContextCompat
import com.example.elderpal.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class SmsDeliveryStatus {
    SENT,
    OPENED_IN_APP,
    FAILED
}

enum class PhoneCallStatus {
    PLACED,
    DIALER_OPENED,
    FAILED,
    NONE
}

data class ContactAlertStatus(
    val contact: Contact,
    val smsStatus: SmsDeliveryStatus,
    val failureReason: String? = null
)

data class AlertDispatchReport(
    val contactStatuses: List<ContactAlertStatus>,
    val primaryContact: Contact?,
    val callStatus: PhoneCallStatus,
    val dispatchedTime: String,
    val locationDescription: String
)

class EmergencyAlertManager(private val context: Context) {

    // Retrieves the last known location using the Android framework LocationManager
    fun getLastKnownLocation(): Location? {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            return null
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )

        var bestLocation: Location? = null
        for (provider in providers) {
            try {
                if (locationManager.isProviderEnabled(provider)) {
                    val loc = locationManager.getLastKnownLocation(provider)
                    if (loc != null) {
                        if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                            bestLocation = loc
                        }
                    }
                }
            } catch (e: SecurityException) {
                // Permission revoked during call
            } catch (e: Exception) {
                // Ignore provider error and try next
            }
        }
        return bestLocation
    }

    // Executes the entire emergency dispatch sequence: SMS first, then phone call
    fun dispatchEmergencyAlert(
        contacts: List<Contact>,
        location: Location?,
        lastTakenDose: DoseLogEntry? = null,
        alertType: AlertType = AlertType.SOS_PRESSED
    ): AlertDispatchReport {
        val now = LocalTime.now()
        val timeStr = now.format(DateTimeFormatter.ofPattern("h:mm a"))

        val locationLink = location?.let {
            "https://maps.google.com/?q=${it.latitude},${it.longitude}"
        }

        // Shared message builder formats alert type, current time, last medicine taken, and location
        val alertMessage = AlertMessageBuilder.buildAlertMessage(
            alertType = alertType,
            currentTime = now,
            lastTakenDose = lastTakenDose,
            locationLink = locationLink
        )

        val hasSmsPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        // 1. Send SMS to all emergency contacts
        val contactStatuses = mutableListOf<ContactAlertStatus>()
        for (contact in contacts) {
            val normalized = normalizePhoneNumber(contact.phoneNumber)
            val status = if (hasSmsPermission) {
                sendDirectSms(normalized, alertMessage)
            } else {
                openMessagingAppFallback(normalized, alertMessage)
            }
            contactStatuses.add(
                ContactAlertStatus(
                    contact = contact,
                    smsStatus = status
                )
            )
        }

        // 2. Identify the primary "call first" contact
        val primaryContact = contacts.firstOrNull { it.isCallFirst } ?: contacts.firstOrNull()
        var callStatus = PhoneCallStatus.NONE

        if (primaryContact != null) {
            val normalized = normalizePhoneNumber(primaryContact.phoneNumber)
            val digits = normalized.filter { it.isDigit() }

            // Guard: Never auto-dial emergency service numbers
            if (digits !in ContactsRepository.EMERGENCY_NUMBERS) {
                callStatus = if (hasCallPermission) {
                    placeDirectPhoneCall(normalized)
                } else {
                    openDialerFallback(normalized)
                }
            } else {
                callStatus = PhoneCallStatus.FAILED
            }
        }

        // 3. Optional Twilio voice calls to family contacts (off main thread, never delays SMS or direct call)
        triggerTwilioVoiceCallsAsync(
            contacts = contacts,
            primaryContact = primaryContact,
            alertType = alertType,
            currentTime = now,
            lastTakenDose = lastTakenDose
        )

        return AlertDispatchReport(
            contactStatuses = contactStatuses,
            primaryContact = primaryContact,
            callStatus = callStatus,
            dispatchedTime = timeStr,
            locationDescription = locationLink ?: "Location unavailable"
        )
    }

    // Convenience function for dispatching an SOS emergency alert
    fun dispatchSosAlert(
        contacts: List<Contact>,
        location: Location?,
        lastTakenDose: DoseLogEntry? = null
    ): AlertDispatchReport = dispatchEmergencyAlert(
        contacts = contacts,
        location = location,
        lastTakenDose = lastTakenDose,
        alertType = AlertType.SOS_PRESSED
    )

    // Sends SMS directly via SmsManager, dividing multipart messages if necessary
    private fun sendDirectSms(normalizedNumber: String, message: String): SmsDeliveryStatus {
        return try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            val parts = smsManager.divideMessage(message)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(normalizedNumber, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(normalizedNumber, null, message, null, null)
            }
            SmsDeliveryStatus.SENT
        } catch (e: Exception) {
            // If direct SMS fails (e.g. no carrier network / SIM), fall back to messaging app
            openMessagingAppFallback(normalizedNumber, message)
        }
    }

    // Opens the user's default SMS app with the recipient and text prefilled
    private fun openMessagingAppFallback(normalizedNumber: String, message: String): SmsDeliveryStatus {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$normalizedNumber")
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            SmsDeliveryStatus.OPENED_IN_APP
        } catch (e: Exception) {
            SmsDeliveryStatus.FAILED
        }
    }

    // Initiates a phone call directly using ACTION_CALL
    fun placeDirectPhoneCall(normalizedNumber: String): PhoneCallStatus {
        return try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$normalizedNumber")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            PhoneCallStatus.PLACED
        } catch (e: Exception) {
            openDialerFallback(normalizedNumber)
        }
    }

    // Opens the system dialer with the phone number pre-filled using ACTION_DIAL
    fun openDialerFallback(normalizedNumber: String): PhoneCallStatus {
        return try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$normalizedNumber")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            PhoneCallStatus.DIALER_OPENED
        } catch (e: Exception) {
            PhoneCallStatus.FAILED
        }
    }

    // Dispatches optional spoken Twilio voice calls to non-primary family contacts asynchronously
    private fun triggerTwilioVoiceCallsAsync(
        contacts: List<Contact>,
        primaryContact: Contact?,
        alertType: AlertType,
        currentTime: LocalTime,
        lastTakenDose: DoseLogEntry?
    ) {
        val accountSid = BuildConfig.TWILIO_ACCOUNT_SID.trim()
        val authToken = BuildConfig.TWILIO_AUTH_TOKEN.trim()
        val fromNumber = BuildConfig.TWILIO_FROM_NUMBER.trim()

        // Feature is silently off if any credential is missing or blank
        if (accountSid.isBlank() || authToken.isBlank() || fromNumber.isBlank()) {
            return
        }

        val alsoCallFirst = isVoiceCallAlsoFirstContact(context)
        val contactsToCall = contacts.filter { contact ->
            if (alsoCallFirst) {
                true
            } else {
                contact != primaryContact && !contact.isCallFirst
            }
        }

        if (contactsToCall.isEmpty()) {
            return
        }

        val spokenText = AlertMessageBuilder.buildSpokenAlertMessage(
            alertType = alertType,
            currentTime = currentTime,
            lastTakenDose = lastTakenDose
        )
        val twiml = AlertMessageBuilder.buildInlineTwiml(spokenText)

        // Run network call off the main thread with short timeout
        Thread {
            val calledNumbers = mutableSetOf<String>()
            for (contact in contactsToCall) {
                val normalized = normalizePhoneNumber(contact.phoneNumber)
                val digits = normalized.filter { it.isDigit() }

                // Guard: Never auto-dial emergency numbers
                if (digits in ContactsRepository.EMERGENCY_NUMBERS) {
                    continue
                }

                // Never place more than one voice call per contact per alert
                if (normalized.isBlank() || !calledNumbers.add(normalized)) {
                    continue
                }

                makeTwilioCallWithSingleRetry(
                    accountSid = accountSid,
                    authToken = authToken,
                    fromNumber = fromNumber,
                    toNumber = normalized,
                    twiml = twiml
                )
            }
        }.start()
    }

    // Attempts a Twilio call and retries at most once if it fails
    private fun makeTwilioCallWithSingleRetry(
        accountSid: String,
        authToken: String,
        fromNumber: String,
        toNumber: String,
        twiml: String
    ): Boolean {
        val success = executeTwilioCall(accountSid, authToken, fromNumber, toNumber, twiml)
        if (!success) {
            // Do not retry more than once
            return executeTwilioCall(accountSid, authToken, fromNumber, toNumber, twiml)
        }
        return true
    }

    // Executes the HTTP POST request to Twilio Calls REST API using standard HttpURLConnection
    private fun executeTwilioCall(
        accountSid: String,
        authToken: String,
        fromNumber: String,
        toNumber: String,
        twiml: String
    ): Boolean {
        var connection: HttpURLConnection? = null
        return try {
            val endpointUrl = URL("https://api.twilio.com/2010-04-01/Accounts/$accountSid/Calls.json")
            connection = (endpointUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 5000 // 5 seconds
                readTimeout = 5000    // 5 seconds
                doOutput = true
                instanceFollowRedirects = false

                val userPass = "$accountSid:$authToken"
                val basicAuth = "Basic " + Base64.encodeToString(userPass.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                setRequestProperty("Authorization", basicAuth)
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
            }

            val postData = buildString {
                append("To=").append(URLEncoder.encode(toNumber, "UTF-8"))
                append("&From=").append(URLEncoder.encode(fromNumber, "UTF-8"))
                append("&Twiml=").append(URLEncoder.encode(twiml, "UTF-8"))
            }

            connection.outputStream.use { os ->
                os.write(postData.toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val code = connection.responseCode
            // HTTP 201 Created indicates Twilio queued/initiated the call
            code in 200..299
        } catch (e: Exception) {
            // Never log credentials, phone numbers, or message text
            false
        } finally {
            connection?.disconnect()
        }
    }

    companion object {
        private const val PREFS_NAME = "elderpal_voice_call_prefs"
        private const val KEY_VOICE_CALL_ALSO_FIRST = "voice_call_also_first_contact"

        fun isVoiceCallAlsoFirstContact(context: Context): Boolean {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_VOICE_CALL_ALSO_FIRST, false)
        }

        fun setVoiceCallAlsoFirstContact(context: Context, enabled: Boolean) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_VOICE_CALL_ALSO_FIRST, enabled)
                .apply()
        }

        // Normalizes phone numbers by stripping whitespace, hyphens, brackets, and periods.
        // If a number does not contain an international country code (no leading '+'), it is kept as entered;
        // standard Android telephony automatically routes domestic numbers according to the active SIM carrier.
        fun normalizePhoneNumber(phone: String): String {
            return phone.trim().replace(Regex("[\\s\\-\\(\\)\\.]"), "")
        }
    }
}
