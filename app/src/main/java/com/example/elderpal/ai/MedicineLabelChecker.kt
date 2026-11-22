// Sends a medicine label photo to Gemini API, extracts printed label text, and determines schedule status.
package com.example.elderpal.ai

import android.content.Context
import android.graphics.Bitmap
import com.example.elderpal.BuildConfig
import com.example.elderpal.data.Medicine
import com.example.elderpal.data.MedicineRepository
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
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
    }

    private val repository = MedicineRepository(context)

    // Analyzes a captured in-memory bitmap and checks against the local schedule
    suspend fun checkMedicineLabel(bitmap: Bitmap): LabelCheckResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY.trim()
        if (apiKey.isBlank() || apiKey.startsWith("YOUR_", ignoreCase = true) || apiKey.startsWith("your_", ignoreCase = true)) {
            return@withContext LabelCheckResult(
                type = CheckResultType.UNSURE,
                reason = "API key not configured in local.properties"
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

        try {
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

            // Execute request with a 20-second timeout
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
                    reason = plainReason.ifBlank { "Could not read the medicine label clearly." }
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

            if (dueNowTime != null) {
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
            // Any network, timeout, or parsing error fails gracefully to UNSURE
            LabelCheckResult(
                type = CheckResultType.UNSURE,
                reason = "Unable to verify label. Please ask for assistance."
            )
        }
    }
}
