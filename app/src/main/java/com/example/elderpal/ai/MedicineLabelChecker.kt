// Sends a medicine label photo to Gemini API, extracts printed label text, and determines schedule status.
package com.example.elderpal.ai

import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.elderpal.BuildConfig
import com.example.elderpal.data.Medicine
import com.example.elderpal.data.MedicineRepository
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class CheckResultType {
    OK,
    WRONG,
    NOT_DUE,
    UNSURE
}

data class LabelCheckResult(
    val type: CheckResultType,
    val detectedLabelName: String = "",
    val matchedMedicine: Medicine? = null,
    val matchedScheduledTime: String = "",
    val reason: String = ""
)

class MedicineLabelChecker(private val context: Context) {

    companion object {
        // Window in minutes before and after scheduled dose time to consider a medicine "due now"
        const val DUE_NOW_WINDOW_BEFORE_MINUTES = 60
        const val DUE_NOW_WINDOW_AFTER_MINUTES = 60
        const val GEMINI_MODEL_NAME = "gemini-3.5-flash"

        // Retry settings for transient server errors (429 rate-limit or 503 overload).
        // The app retries up to MAX_RETRIES times, waiting RETRY_DELAYS_MS[attempt] milliseconds between tries.
        const val MAX_RETRIES = 3
        val RETRY_DELAYS_MS = longArrayOf(1_000L, 2_000L, 4_000L)

        // Phrases in exception messages that signal a server-side overload rather than a network failure.
        // The Gemini SDK throws a RuntimeException/IOException whose message contains the HTTP status code.
        private val RETRYABLE_PATTERNS = listOf("429", "503", "rate", "quota", "overload", "unavailable")
    }

    private val repository = MedicineRepository(context)

    // Returns true when the device has an active internet connection (Wi-Fi or mobile data).
    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    // Returns true when an exception looks like a transient server overload (429 or 503).
    private fun isRetryable(e: Exception): Boolean {
        val msg = (e.message ?: "").lowercase()
        return RETRYABLE_PATTERNS.any { msg.contains(it) }
    }

    // Analyzes a captured in-memory bitmap and checks against the local schedule.
    // Retries up to MAX_RETRIES times on transient server errors before giving up.
    suspend fun checkMedicineLabel(bitmap: Bitmap): LabelCheckResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY.trim()
        if (apiKey.isBlank() || apiKey.startsWith("YOUR_", ignoreCase = true) || apiKey.startsWith("your_", ignoreCase = true)) {
            return@withContext LabelCheckResult(
                type = CheckResultType.UNSURE,
                reason = "API key not configured in local.properties"
            )
        }

        // Fast-fail with a clear no-internet message before spending time on API setup.
        if (!isOnline()) {
            return@withContext LabelCheckResult(
                type = CheckResultType.UNSURE,
                reason = "No internet connection. Please connect to Wi-Fi or mobile data and try again."
            )
        }

        val allMedicines = repository.medicines.first()
        val currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a"))

        // Build list of scheduled medicines to provide as context
        val medicineNamesList = allMedicines.map { "${it.name} (${it.dose})" }.joinToString(", ")

        val prompt = buildString {
            appendLine("You are an assistive health helper for an elderly person.")
            appendLine("Read the PRINTED TEXT on the medicine bottle or box label shown in the photo.")
            appendLine("Rely ONLY on legible printed label text. Do NOT guess from pill color, shape, or appearance.")
            appendLine()
            appendLine("Patient's scheduled medicines: [$medicineNamesList]")
            appendLine("Current time: $currentTime")
            appendLine()
            appendLine("Respond with ONLY a raw JSON object (no markdown, no backticks) with keys:")
            appendLine("  \"detected_label_name\": string (the drug/medicine name printed on label, or empty if unclear),")
            appendLine("  \"matched_medicine_name\": string (name matching the scheduled list, or \"NONE\" if not on list, or \"UNSURE\" if unreadable),")
            appendLine("  \"is_label_clear\": boolean (true if label text is clearly legible, false if blurry, dark, cut off, or missing),")
            appendLine("  \"plain_reason\": string (one short sentence explaining what was read)")
            appendLine()
            appendLine("If the label is unreadable or not clearly legible, set \"is_label_clear\": false and \"matched_medicine_name\": \"UNSURE\".")
        }

        val generativeModel = GenerativeModel(
            modelName = GEMINI_MODEL_NAME,
            apiKey = apiKey,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                temperature = 0.1f
            }
        )

        val inputContent = content {
            image(bitmap)
            text(prompt)
        }

        // Attempt the API call with retries on transient server errors (429 / 503).
        var lastException: Exception? = null
        var lastWasRetryable = false

        repeat(MAX_RETRIES) { attempt ->
            try {
                // Execute request with a 20-second timeout per attempt
                val response = withTimeoutOrNull(20_000L) {
                    generativeModel.generateContent(inputContent)
                }

                if (response == null || response.text.isNullOrBlank()) {
                    return@withContext LabelCheckResult(
                        type = CheckResultType.UNSURE,
                        reason = "No response from verification service. Please try again."
                    )
                }

                // Parse response safely
                val jsonText = response.text!!.trim()
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()

                val json = JSONObject(jsonText)
                val detectedName = json.optString("detected_label_name", "").trim()
                val matchedName = json.optString("matched_medicine_name", "").trim()
                val isLabelClear = json.optBoolean("is_label_clear", false)
                val plainReason = json.optString("plain_reason", "").trim()

                // Rule: If label is unclear or unreadable, result is UNSURE
                if (!isLabelClear || matchedName.equals("UNSURE", ignoreCase = true) || detectedName.isBlank()) {
                    return@withContext LabelCheckResult(
                        type = CheckResultType.UNSURE,
                        detectedLabelName = detectedName,
                        reason = plainReason.ifBlank { "The label could not be read clearly. Please point the camera directly at the label text and try again, or ask a family member." }
                    )
                }

                // Rule: If medicine is not on the user's schedule, result is WRONG
                if (matchedName.equals("NONE", ignoreCase = true)) {
                    return@withContext LabelCheckResult(
                        type = CheckResultType.WRONG,
                        detectedLabelName = detectedName,
                        reason = "This medicine is not on your schedule."
                    )
                }

                // Find matching medicine from user's repository
                val matchedMedicine = allMedicines.firstOrNull {
                    it.name.equals(matchedName, ignoreCase = true) ||
                    it.name.contains(detectedName, ignoreCase = true) ||
                    detectedName.contains(it.name, ignoreCase = true)
                }

                if (matchedMedicine == null) {
                    return@withContext LabelCheckResult(
                        type = CheckResultType.WRONG,
                        detectedLabelName = detectedName,
                        reason = "This medicine is not on your schedule."
                    )
                }

                // -------------------------------------------------------------
                // App-side deterministic "due now" calculation:
                // Never trust AI alone for clock times; enforce app logic
                // -------------------------------------------------------------
                val now = LocalTime.now()
                val currentMinutes = now.hour * 60 + now.minute
                val todayStr = LocalDate.now().toString()
                val allLogs = repository.doseLog.first()

                val times = if (matchedMedicine.doseTimes.isNotEmpty()) {
                    matchedMedicine.doseTimes
                } else {
                    Medicine.defaultDoseTimes(matchedMedicine.timesPerDay)
                }

                // Find scheduled doses not yet taken today
                val untakenTimes = times.filter { time ->
                    val alreadyTaken = allLogs.any { log ->
                        log.medicineId == matchedMedicine.id &&
                        log.scheduledTime == time &&
                        log.date == todayStr
                    }
                    !alreadyTaken
                }

                // Check if any untaken dose is currently within the "due now" window
                val dueNowTime = untakenTimes.firstOrNull { time ->
                    val scheduledMinutes = MedicineRepository.parseTimeToMinutes(time)
                    currentMinutes >= (scheduledMinutes - DUE_NOW_WINDOW_BEFORE_MINUTES) &&
                    currentMinutes <= (scheduledMinutes + DUE_NOW_WINDOW_AFTER_MINUTES)
                }

                return@withContext if (dueNowTime != null) {
                    // Verified: Right medicine and due now
                    LabelCheckResult(
                        type = CheckResultType.OK,
                        detectedLabelName = detectedName,
                        matchedMedicine = matchedMedicine,
                        matchedScheduledTime = dueNowTime,
                        reason = plainReason
                    )
                } else {
                    // Medicine is on the list, but not due now
                    val nextScheduled = untakenTimes.firstOrNull { time ->
                        MedicineRepository.parseTimeToMinutes(time) > currentMinutes
                    } ?: times.firstOrNull() ?: ""

                    LabelCheckResult(
                        type = CheckResultType.NOT_DUE,
                        detectedLabelName = detectedName,
                        matchedMedicine = matchedMedicine,
                        matchedScheduledTime = nextScheduled,
                        reason = "This medicine is scheduled for $nextScheduled."
                    )
                }
            } catch (e: Exception) {
                lastException = e
                lastWasRetryable = isRetryable(e)

                if (lastWasRetryable && attempt < MAX_RETRIES - 1) {
                    // Wait before retrying: 1s, 2s, 4s
                    delay(RETRY_DELAYS_MS[attempt])
                    // Re-check connectivity before each retry
                    if (!isOnline()) {
                        return@withContext LabelCheckResult(
                            type = CheckResultType.UNSURE,
                            reason = "No internet connection. Please connect to Wi-Fi or mobile data and try again."
                        )
                    }
                }
                // If not retryable, break out immediately by returning early
                if (!lastWasRetryable) {
                    return@withContext LabelCheckResult(
                        type = CheckResultType.UNSURE,
                        reason = "Unable to read the label. Please ask a family member or pharmacist for help."
                    )
                }
            }
        }

        // All retries exhausted on a server-side overload (429 / 503)
        LabelCheckResult(
            type = CheckResultType.UNSURE,
            reason = "The checker is busy right now. Please try again in a moment."
        )
    }
}
