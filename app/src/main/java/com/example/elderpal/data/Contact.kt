// Data model representing an emergency family or caregiver contact.
package com.example.elderpal.data

import java.util.UUID

data class Contact(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val relationship: String,
    val phoneNumber: String,
    val isCallFirst: Boolean = false
)
