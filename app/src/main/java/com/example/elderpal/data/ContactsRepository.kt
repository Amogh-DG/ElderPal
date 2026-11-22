// Manages local persistence for emergency family contacts using Jetpack DataStore.
package com.example.elderpal.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.contactsDataStore: DataStore<Preferences> by preferencesDataStore(name = "contacts_prefs")

class ContactsRepository(private val context: Context) {

    private val contactsKey = stringPreferencesKey("saved_contacts_json")

    // Flow of all emergency contacts stored locally on the phone
    val contacts: Flow<List<Contact>> = context.contactsDataStore.data.map { preferences ->
        val json = preferences[contactsKey] ?: ""
        deserializeContacts(json)
    }

    // Fetches a snapshot of all contacts
    suspend fun getAllContacts(): List<Contact> {
        val preferences = context.contactsDataStore.data.first()
        val json = preferences[contactsKey] ?: ""
        return deserializeContacts(json)
    }

    // Returns the primary contact designated to be called first
    suspend fun getCallFirstContact(): Contact? {
        val list = getAllContacts()
        return list.firstOrNull { it.isCallFirst } ?: list.firstOrNull()
    }

    // Saves a new contact. If marked as call first, unmarks all other contacts.
    suspend fun saveContact(contact: Contact) {
        context.contactsDataStore.edit { preferences ->
            val currentJson = preferences[contactsKey] ?: ""
            val list = deserializeContacts(currentJson).toMutableList()

            val sanitizedList = if (contact.isCallFirst) {
                // Ensure only one contact has isCallFirst = true
                list.map { it.copy(isCallFirst = false) }.toMutableList()
            } else {
                list
            }

            sanitizedList.add(contact)
            preferences[contactsKey] = serializeContacts(sanitizedList)
        }
    }

    // Updates an existing contact by id. If marked as call first, unmarks all other contacts.
    suspend fun updateContact(updatedContact: Contact) {
        context.contactsDataStore.edit { preferences ->
            val currentJson = preferences[contactsKey] ?: ""
            val list = deserializeContacts(currentJson)

            val updatedList = list.map { existing ->
                when {
                    existing.id == updatedContact.id -> updatedContact
                    updatedContact.isCallFirst -> existing.copy(isCallFirst = false)
                    else -> existing
                }
            }

            preferences[contactsKey] = serializeContacts(updatedList)
        }
    }

    // Deletes a contact by id
    suspend fun deleteContact(contactId: String) {
        context.contactsDataStore.edit { preferences ->
            val currentJson = preferences[contactsKey] ?: ""
            val list = deserializeContacts(currentJson)
            val filtered = list.filterNot { it.id == contactId }
            preferences[contactsKey] = serializeContacts(filtered)
        }
    }

    private fun serializeContacts(contacts: List<Contact>): String {
        val array = JSONArray()
        for (c in contacts) {
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("name", c.name)
            obj.put("relationship", c.relationship)
            obj.put("phoneNumber", c.phoneNumber)
            obj.put("isCallFirst", c.isCallFirst)
            array.put(obj)
        }
        return array.toString()
    }

    private fun deserializeContacts(jsonStr: String): List<Contact> {
        if (jsonStr.isBlank()) return emptyList()
        val list = mutableListOf<Contact>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    Contact(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        relationship = obj.optString("relationship", ""),
                        phoneNumber = obj.getString("phoneNumber"),
                        isCallFirst = obj.optBoolean("isCallFirst", false)
                    )
                )
            }
        } catch (e: Exception) {
            // Handle parsing issue gracefully
        }
        return list
    }

    companion object {
        // List of emergency service numbers that must never be added as family contacts
        val EMERGENCY_NUMBERS = setOf(
            "100", // Police (India)
            "101", // Fire (India)
            "102", // Ambulance (India)
            "108", // Emergency medical / disaster (India)
            "112", // National emergency (India / Europe / universal)
            "911", // Emergency (US / Canada)
            "999", // Emergency (UK)
            "000", // Emergency (Australia)
            "119", // Fire / Ambulance (Japan / Korea)
            "1090", // Women helpline
            "1091", // Women in distress
            "1098"  // Childline
        )

        // Validates contact form inputs and returns an error message if invalid, or null if valid
        fun validate(name: String, phoneNumber: String): String? {
            if (name.trim().isBlank()) {
                return "Please enter a name."
            }

            val rawPhone = phoneNumber.trim()
            if (rawPhone.isBlank()) {
                return "Please enter a phone number."
            }

            // Extract digits only for checking emergency numbers and digit count
            val digits = rawPhone.filter { it.isDigit() }

            if (digits in EMERGENCY_NUMBERS) {
                return "Please enter a family or caregiver number, not emergency services (like 112 or 911)."
            }

            // A phone number should contain standard characters and between 7 and 15 digits
            val allowedChars = setOf('+', '-', ' ', '(', ')', '.', '/')
            val hasInvalidChars = rawPhone.any { !it.isDigit() && it !in allowedChars }

            if (hasInvalidChars || digits.length < 7 || digits.length > 15) {
                return "Please enter a valid phone number (at least 7 digits)."
            }

            return null
        }
    }
}
