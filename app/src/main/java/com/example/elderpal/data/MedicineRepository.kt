// Manages local persistence for medicines, daily scheduled doses, and dose logs using Jetpack DataStore.
package com.example.elderpal.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "medicines_prefs")

class MedicineRepository(private val context: Context) {

    private val medicinesKey = stringPreferencesKey("saved_medicines_json")
    private val doseLogKey = stringPreferencesKey("saved_dose_log_json")

    // Flow of all saved medicines stored locally on the phone
    val medicines: Flow<List<Medicine>> = context.dataStore.data.map { preferences ->
        val json = preferences[medicinesKey] ?: ""
        deserializeMedicines(json)
    }

    // Flow of all logged dose records
    val doseLog: Flow<List<DoseLogEntry>> = context.dataStore.data.map { preferences ->
        val json = preferences[doseLogKey] ?: ""
        deserializeDoseLog(json)
    }

    // Flow of all scheduled doses for today with real status (Upcoming, Taken, Missed)
    val todayDoses: Flow<List<ScheduledDose>> = combine(medicines, doseLog) { medicineList, logList ->
        val todayStr = LocalDate.now().toString()
        val now = LocalTime.now()
        val currentMinutes = now.hour * 60 + now.minute

        val doses = mutableListOf<ScheduledDose>()
        for (med in medicineList) {
            val times = if (med.doseTimes.isNotEmpty()) {
                med.doseTimes
            } else {
                Medicine.defaultDoseTimes(med.timesPerDay)
            }
            for (time in times) {
                // Check if this dose was marked as taken today
                val isTaken = logList.any { log ->
                    log.medicineId == med.id &&
                    log.scheduledTime == time &&
                    log.date == todayStr
                }

                val scheduledMinutes = parseTimeToMinutes(time)

                val status = when {
                    isTaken -> DoseStatus.TAKEN
                    currentMinutes > scheduledMinutes + 60 -> DoseStatus.MISSED
                    else -> DoseStatus.UPCOMING
                }

                doses.add(
                    ScheduledDose(
                        medicineId = med.id,
                        medicineName = med.name,
                        dose = med.dose,
                        time = time,
                        takesWithFood = med.takesWithFood,
                        status = status
                    )
                )
            }
        }
        doses.sortedBy { parseTimeToMinutes(it.time) }
    }

    // Persist a new medicine
    suspend fun saveMedicine(medicine: Medicine) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[medicinesKey] ?: ""
            val list = deserializeMedicines(currentJson).toMutableList()
            list.add(medicine)
            preferences[medicinesKey] = serializeMedicines(list)
        }
    }

    // Records a dose as taken right now in the local dose log
    suspend fun markDoseTaken(medicineId: String, medicineName: String, scheduledTime: String) {
        val now = LocalTime.now()
        val formattedTime = now.format(DateTimeFormatter.ofPattern("h:mm a"))
        val todayStr = LocalDate.now().toString()

        val newEntry = DoseLogEntry(
            medicineId = medicineId,
            medicineName = medicineName,
            scheduledTime = scheduledTime,
            takenTime = formattedTime,
            date = todayStr
        )

        context.dataStore.edit { preferences ->
            val currentJson = preferences[doseLogKey] ?: ""
            val list = deserializeDoseLog(currentJson).toMutableList()
            list.add(newEntry)
            preferences[doseLogKey] = serializeDoseLog(list)
        }
    }

    // Retrieves the most recently taken doses (most recent first) for alert messages
    suspend fun getRecentTakenDoses(limit: Int = 3): List<DoseLogEntry> {
        val preferences = context.dataStore.data.first()
        val json = preferences[doseLogKey] ?: ""
        val allLogs = deserializeDoseLog(json)
        return allLogs.reversed().take(limit)
    }

    // Helper to calculate the next upcoming dose relative to current clock time
    fun findNextDose(doses: List<ScheduledDose>): ScheduledDose? {
        if (doses.isEmpty()) return null
        val now = LocalTime.now()
        val currentMinutes = now.hour * 60 + now.minute

        // First look for upcoming doses scheduled for later today
        val nextUpcomingLaterToday = doses.firstOrNull { dose ->
            dose.status == DoseStatus.UPCOMING && parseTimeToMinutes(dose.time) >= currentMinutes
        }
        if (nextUpcomingLaterToday != null) return nextUpcomingLaterToday

        // Otherwise look for any remaining upcoming dose
        val anyUpcoming = doses.firstOrNull { it.status == DoseStatus.UPCOMING }
        if (anyUpcoming != null) return anyUpcoming

        // If no doses are upcoming, return first non-taken or first dose of the cycle
        return doses.firstOrNull { it.status != DoseStatus.TAKEN } ?: doses.firstOrNull()
    }

    private fun serializeMedicines(medicines: List<Medicine>): String {
        val array = JSONArray()
        for (m in medicines) {
            val obj = JSONObject()
            obj.put("id", m.id)
            obj.put("name", m.name)
            obj.put("dose", m.dose)
            obj.put("timesPerDay", m.timesPerDay)
            val timesArr = JSONArray()
            m.doseTimes.forEach { timesArr.put(it) }
            obj.put("doseTimes", timesArr)
            obj.put("takesWithFood", m.takesWithFood)
            array.put(obj)
        }
        return array.toString()
    }

    private fun deserializeMedicines(jsonStr: String): List<Medicine> {
        if (jsonStr.isBlank()) return emptyList()
        val list = mutableListOf<Medicine>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val timesArr = obj.optJSONArray("doseTimes")
                val timesList = mutableListOf<String>()
                if (timesArr != null) {
                    for (j in 0 until timesArr.length()) {
                        timesList.add(timesArr.getString(j))
                    }
                }
                list.add(
                    Medicine(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        dose = obj.getString("dose"),
                        timesPerDay = obj.getInt("timesPerDay"),
                        doseTimes = timesList,
                        takesWithFood = obj.optBoolean("takesWithFood", false)
                    )
                )
            }
        } catch (e: Exception) {
            // Handle parsing issue gracefully
        }
        return list
    }

    private fun serializeDoseLog(log: List<DoseLogEntry>): String {
        val array = JSONArray()
        for (entry in log) {
            val obj = JSONObject()
            obj.put("id", entry.id)
            obj.put("medicineId", entry.medicineId)
            obj.put("medicineName", entry.medicineName)
            obj.put("scheduledTime", entry.scheduledTime)
            obj.put("takenTime", entry.takenTime)
            obj.put("date", entry.date)
            array.put(obj)
        }
        return array.toString()
    }

    private fun deserializeDoseLog(jsonStr: String): List<DoseLogEntry> {
        if (jsonStr.isBlank()) return emptyList()
        val list = mutableListOf<DoseLogEntry>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    DoseLogEntry(
                        id = obj.getString("id"),
                        medicineId = obj.getString("medicineId"),
                        medicineName = obj.optString("medicineName", ""),
                        scheduledTime = obj.getString("scheduledTime"),
                        takenTime = obj.getString("takenTime"),
                        date = obj.getString("date")
                    )
                )
            }
        } catch (e: Exception) {
            // Handle parsing issue gracefully
        }
        return list
    }

    companion object {
        // Converts 12-hour AM/PM string into total minutes since midnight for sorting
        fun parseTimeToMinutes(timeStr: String): Int {
            return try {
                val parts = timeStr.trim().split(" ")
                val clock = parts[0].split(":")
                var hour = clock[0].toInt()
                val minute = if (clock.size > 1) clock[1].toInt() else 0
                val isPm = parts.getOrNull(1)?.equals("PM", ignoreCase = true) == true
                val isAm = parts.getOrNull(1)?.equals("AM", ignoreCase = true) == true
                if (isPm && hour < 12) hour += 12
                if (isAm && hour == 12) hour = 0
                hour * 60 + minute
            } catch (e: Exception) {
                0
            }
        }
    }
}
