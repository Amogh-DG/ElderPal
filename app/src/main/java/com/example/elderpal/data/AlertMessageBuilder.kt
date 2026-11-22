package com.example.elderpal.data

import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale

enum class AlertType(val actionText: String) {
    SOS_PRESSED("SOS pressed"),
    FALL_DETECTED("Fall detected")
}

object AlertMessageBuilder {

    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

    // build the complete alert message with time, recent dose and maps link
    fun buildAlertMessage(
        alertType: AlertType,
        currentTime: LocalTime = LocalTime.now(),
        lastTakenDose: DoseLogEntry? = null,
        locationLink: String? = null
    ): String {
        val alertHeader = buildAlertHeader(alertType, currentTime)
        val medicinePart = formatMedicinePart(lastTakenDose, currentTime)
        val locationPart = formatLocationPart(locationLink)

        return "$alertHeader $medicinePart $locationPart"
    }

    // preview text without location
    fun buildTextWithoutLocation(
        alertType: AlertType,
        currentTime: LocalTime = LocalTime.now(),
        lastTakenDose: DoseLogEntry? = null
    ): String {
        val alertHeader = buildAlertHeader(alertType, currentTime)
        val medicinePart = formatMedicinePart(lastTakenDose, currentTime)
        return "$alertHeader $medicinePart"
    }

    fun buildAlertHeader(alertType: AlertType, currentTime: LocalTime = LocalTime.now()): String {
        val timeStr = currentTime.format(timeFormatter)
        return "ElderPal alert: ${alertType.actionText} at $timeStr."
    }

    // format last medicine taken and how many minutes ago if recent
    fun formatMedicinePart(dose: DoseLogEntry?, currentTime: LocalTime = LocalTime.now()): String {
        if (dose == null) {
            return "Last medicine taken: No medicine logged today."
        }

        val elapsedMinutes = calculateElapsedMinutes(dose.takenTime, currentTime)
        val elapsedNote = when {
            elapsedMinutes == null -> ""
            elapsedMinutes <= 0 -> " (just now)"
            elapsedMinutes == 1L -> " (1 min ago)"
            elapsedMinutes <= 120 -> " ($elapsedMinutes min ago)"
            else -> ""
        }

        return "Last medicine taken: ${dose.medicineName} at ${dose.takenTime}$elapsedNote."
    }

    fun formatLocationPart(locationLink: String?): String {
        return if (!locationLink.isNullOrBlank()) {
            "Location: $locationLink"
        } else {
            "Location: Location unavailable."
        }
    }

    // spoken alert text for phone calls
    fun buildSpokenAlertMessage(
        alertType: AlertType,
        currentTime: LocalTime = LocalTime.now(),
        lastTakenDose: DoseLogEntry? = null
    ): String {
        val timeStr = currentTime.format(timeFormatter)
        val actionText = when (alertType) {
            AlertType.FALL_DETECTED -> "A fall was detected at $timeStr."
            AlertType.SOS_PRESSED -> "An SOS emergency alert was triggered at $timeStr."
        }

        val medicinePart = if (lastTakenDose != null) {
            "Last medicine taken: ${lastTakenDose.medicineName} at ${lastTakenDose.takenTime}."
        } else {
            "Last medicine taken: No medicine logged today."
        }

        return "ElderPal alert. $actionText $medicinePart A text message with the location has been sent."
    }

    fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    // wrap in twiml say verb
    fun buildInlineTwiml(spokenText: String): String {
        val escaped = escapeXml(spokenText)
        return "<Response><Say>$escaped</Say></Response>"
    }

    private fun calculateElapsedMinutes(takenTimeStr: String, currentTime: LocalTime): Long? {
        return try {
            val parser = DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("h:mm a")
                .toFormatter(Locale.US)
            val parsedTakenTime = LocalTime.parse(takenTimeStr.trim(), parser)
            val diff = Duration.between(parsedTakenTime, currentTime).toMinutes()
            if (diff >= 0) diff else null
        } catch (e: Exception) {
            null
        }
    }
}
        } catch (e: Exception) {
            null
        }
    }
}
