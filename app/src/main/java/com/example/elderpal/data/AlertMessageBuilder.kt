// Shared builder that formats emergency alert messages containing alert type, timestamp, recent medicine log, and location.
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

    /**
     * Builds the complete alert SMS message text.
     *
     * Example:
     * "ElderPal alert: SOS pressed at 4:12 PM. Last medicine taken: Blood Pressure pill at 3:45 PM (27 min ago). Location: https://maps.google.com/?q=12.9716,77.5946"
     */
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

    /**
     * Builds the alert header and medicine portion of the message (useful for previewing chat bubbles).
     */
    fun buildTextWithoutLocation(
        alertType: AlertType,
        currentTime: LocalTime = LocalTime.now(),
        lastTakenDose: DoseLogEntry? = null
    ): String {
        val alertHeader = buildAlertHeader(alertType, currentTime)
        val medicinePart = formatMedicinePart(lastTakenDose, currentTime)
        return "$alertHeader $medicinePart"
    }

    /**
     * Formats the alert header: "ElderPal alert: [Fall detected / SOS pressed] at [time]."
     */
    fun buildAlertHeader(alertType: AlertType, currentTime: LocalTime = LocalTime.now()): String {
        val timeStr = currentTime.format(timeFormatter)
        return "ElderPal alert: ${alertType.actionText} at $timeStr."
    }

    /**
     * Formats the medicine status.
     * If no dose today: "Last medicine taken: No medicine logged today."
     * If dose taken today within 2 hours: includes elapsed minutes, e.g. "(40 min ago)".
     */
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

    /**
     * Formats the location portion of the message.
     */
    fun formatLocationPart(locationLink: String?): String {
        return if (!locationLink.isNullOrBlank()) {
            "Location: $locationLink"
        } else {
            "Location: Location unavailable."
        }
    }

    /**
     * Computes the elapsed minutes between the logged dose time and the current time.
     */
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
