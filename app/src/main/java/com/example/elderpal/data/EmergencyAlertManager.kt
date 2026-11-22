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
import androidx.core.content.ContextCompat
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

    companion object {
        // Normalizes phone numbers by stripping whitespace, hyphens, brackets, and periods.
        // If a number does not contain an international country code (no leading '+'), it is kept as entered;
        // standard Android telephony automatically routes domestic numbers according to the active SIM carrier.
        fun normalizePhoneNumber(phone: String): String {
            return phone.trim().replace(Regex("[\\s\\-\\(\\)\\.]"), "")
        }
    }
}
