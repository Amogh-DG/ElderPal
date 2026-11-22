// Represents an entry in the dose log recording when a medicine dose was taken.
package com.example.elderpal.data

import java.util.UUID

data class DoseLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val medicineId: String,
    val medicineName: String,
    val scheduledTime: String,
    val takenTime: String,
    val date: String
)
