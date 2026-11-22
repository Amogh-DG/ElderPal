// Defines the data models for medicines, daily scheduled doses, and dose statuses.
package com.example.elderpal.data

import java.util.UUID

data class Medicine(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val dose: String,
    val timesPerDay: Int,
    val doseTimes: List<String>,
    val takesWithFood: Boolean
) {
    companion object {
        // Generates sensible default clock times based on the chosen frequency per day
        fun defaultDoseTimes(timesPerDay: Int): List<String> {
            return when (timesPerDay) {
                1 -> listOf("8:00 AM")
                2 -> listOf("8:00 AM", "6:00 PM")
                3 -> listOf("8:00 AM", "1:00 PM", "7:00 PM")
                4 -> listOf("8:00 AM", "12:00 PM", "4:00 PM", "8:00 PM")
                else -> listOf("8:00 AM")
            }
        }
    }
}

enum class DoseStatus {
    UPCOMING,
    TAKEN,
    MISSED
}

data class ScheduledDose(
    val medicineId: String,
    val medicineName: String,
    val dose: String,
    val time: String,
    val takesWithFood: Boolean,
    val status: DoseStatus = DoseStatus.UPCOMING
)
