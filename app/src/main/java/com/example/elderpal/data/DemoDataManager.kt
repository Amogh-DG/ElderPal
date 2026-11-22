// Loads clearly fake demo data for hackathon judges and resets all app data back to empty.
package com.example.elderpal.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

// Separate DataStore is not needed: we reuse the medicine and contacts repos directly.

object DemoDataManager {

    // Fake placeholder used when the user leaves the phone number field blank.
    // It is intentionally not a valid phone number so no SMS reaches a real person.
    const val PLACEHOLDER_NUMBER = "+00-000-DEMO-TEST"

    // Formats the current time as "h:mm AM/PM" offset by the given minutes
    private fun offsetTime(offsetMinutes: Int): String {
        val t = LocalTime.now().plusMinutes(offsetMinutes.toLong())
        return t.format(DateTimeFormatter.ofPattern("h:mm a"))
    }

    // Builds the three demo medicines. Times are relative to now so the schedule looks live.
    // Names are obviously fake: "Sample Medicine A/B/C" and doses match nothing real.
    fun buildDemoMedicines(): List<Medicine> {
        val past = offsetTime(-45)   // just missed — will show as "Missed" after 60-min window
        val soon = offsetTime(10)    // upcoming in 10 min
        val later = offsetTime(180)  // upcoming in 3 hours

        return listOf(
            Medicine(
                id = "demo-med-a",
                name = "Sample Medicine A",
                dose = "1 tablet (DEMO)",
                timesPerDay = 1,
                doseTimes = listOf(past),
                takesWithFood = false
            ),
            Medicine(
                id = "demo-med-b",
                name = "Sample Medicine B",
                dose = "2 capsules (DEMO)",
                timesPerDay = 1,
                doseTimes = listOf(soon),
                takesWithFood = true
            ),
            Medicine(
                id = "demo-med-c",
                name = "Sample Medicine C",
                dose = "5 ml syrup (DEMO)",
                timesPerDay = 1,
                doseTimes = listOf(later),
                takesWithFood = false
            )
        )
    }

    // Builds a fake contact. phoneNumber must be supplied by the judge (or the placeholder is used).
    fun buildDemoContact(phoneNumber: String): Contact {
        return Contact(
            id = "demo-contact-1",
            name = "Demo Family Member",
            relationship = "Family (DEMO)",
            phoneNumber = phoneNumber.ifBlank { PLACEHOLDER_NUMBER },
            isCallFirst = true
        )
    }

    // Builds two logged dose entries for today (sample A taken 90 min ago, sample C taken 2 hours ago).
    fun buildDemoDoseLog(medicines: List<Medicine>): List<DoseLogEntry> {
        val todayStr = LocalDate.now().toString()
        val takenA = LocalTime.now().minusMinutes(90)
            .format(DateTimeFormatter.ofPattern("h:mm a"))
        val takenC = LocalTime.now().minusMinutes(120)
            .format(DateTimeFormatter.ofPattern("h:mm a"))

        val medA = medicines.firstOrNull { it.id == "demo-med-a" } ?: return emptyList()
        val medC = medicines.firstOrNull { it.id == "demo-med-c" } ?: return emptyList()

        return listOf(
            DoseLogEntry(
                id = UUID.randomUUID().toString(),
                medicineId = medA.id,
                medicineName = medA.name,
                scheduledTime = medA.doseTimes.firstOrNull() ?: "",
                takenTime = takenA,
                date = todayStr
            ),
            DoseLogEntry(
                id = UUID.randomUUID().toString(),
                medicineId = medC.id,
                medicineName = medC.name,
                scheduledTime = medC.doseTimes.firstOrNull() ?: "",
                takenTime = takenC,
                date = todayStr
            )
        )
    }

    // Saves medicines, contact and dose log, then schedules reminders.
    // Does NOT clear existing data first so it can be called after reset.
    suspend fun loadDemoData(context: Context, phoneNumber: String) {
        val medicineRepo = MedicineRepository(context)
        val contactsRepo = ContactsRepository(context)

        val medicines = buildDemoMedicines()
        for (med in medicines) {
            medicineRepo.saveMedicine(med)
        }

        val contact = buildDemoContact(phoneNumber)
        contactsRepo.saveContact(contact)

        val logEntries = buildDemoDoseLog(medicines)
        medicineRepo.saveDemoLog(logEntries)

        // Schedule reminders for the two upcoming demo doses (past doses are skipped automatically)
        for (med in medicines) {
            for (time in med.doseTimes) {
                val triggerMillis = DoseReminderScheduler.calculateNextTriggerMillis(time)
                DoseReminderScheduler.scheduleReminder(
                    context = context,
                    medicineId = med.id,
                    medicineName = med.name,
                    dose = med.dose,
                    scheduledTime = time,
                    triggerAtMillis = triggerMillis
                )
            }
        }
    }

    // Clears all medicines, contacts, and dose log, and cancels all pending reminders.
    suspend fun resetAllData(context: Context) {
        val medicineRepo = MedicineRepository(context)
        val contactsRepo = ContactsRepository(context)

        // Cancel reminders for every existing medicine before deleting
        val allMedicines = medicineRepo.medicines.first()
        for (med in allMedicines) {
            for (time in med.doseTimes) {
                DoseReminderScheduler.cancelReminder(context, med.id, time)
            }
        }

        medicineRepo.clearAll()
        contactsRepo.clearAll()
    }
}
