// Handles scheduling, snoozing, and canceling medicine reminder notifications using Android AlarmManager.
package com.example.elderpal.data

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object DoseReminderScheduler {

    const val CHANNEL_ID = "dose_reminders"
    const val ACTION_DOSE_REMINDER = "com.example.elderpal.ACTION_DOSE_REMINDER"

    const val EXTRA_MEDICINE_ID = "extra_medicine_id"
    const val EXTRA_MEDICINE_NAME = "extra_medicine_name"
    const val EXTRA_DOSE = "extra_dose"
    const val EXTRA_SCHEDULED_TIME = "extra_scheduled_time"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

    // Registers the high-priority medicine notification channel with the system
    fun createNotificationChannel(context: Context) {
        val name = "Medicine Reminders"
        val descriptionText = "Alerts when it is time to take your scheduled medicine."
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
            enableVibration(true)
            enableLights(true)
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    // Schedules a reminder at a specific clock time
    fun scheduleReminder(
        context: Context,
        medicineId: String,
        medicineName: String,
        dose: String,
        scheduledTime: String,
        triggerAtMillis: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val notificationId = (medicineId.hashCode() xor scheduledTime.hashCode()) and 0x7FFFFFFF

        val intent = Intent(context, DoseReminderReceiver::class.java).apply {
            action = ACTION_DOSE_REMINDER
            putExtra(EXTRA_MEDICINE_ID, medicineId)
            putExtra(EXTRA_MEDICINE_NAME, medicineName)
            putExtra(EXTRA_DOSE, dose)
            putExtra(EXTRA_SCHEDULED_TIME, scheduledTime)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule exact alarm if permission allows, otherwise fallback gracefully
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    // Cancels an active or pending reminder for a specific medicine and dose time
    fun cancelReminder(context: Context, medicineId: String, scheduledTime: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val notificationId = (medicineId.hashCode() xor scheduledTime.hashCode()) and 0x7FFFFFFF

        val intent = Intent(context, DoseReminderReceiver::class.java).apply {
            action = ACTION_DOSE_REMINDER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }

        // Dismiss active notification if already displayed in the notification drawer
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
    }

    // Snoozes a reminder for 10 minutes
    fun scheduleSnooze(
        context: Context,
        medicineId: String,
        medicineName: String,
        dose: String,
        scheduledTime: String,
        delayMinutes: Int = 10
    ) {
        val snoozeTimeMillis = System.currentTimeMillis() + (delayMinutes * 60 * 1000L)
        scheduleReminder(
            context = context,
            medicineId = medicineId,
            medicineName = medicineName,
            dose = dose,
            scheduledTime = scheduledTime,
            triggerAtMillis = snoozeTimeMillis
        )

        // Dismiss current notification while snooze timer runs
        val notificationId = (medicineId.hashCode() xor scheduledTime.hashCode()) and 0x7FFFFFFF
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
    }

    // Triggers a test reminder in 10 seconds for development and testing
    fun triggerTestReminder(context: Context, medicineName: String = "Blood Pressure pill", dose: String = "1 tablet") {
        val testTriggerTime = System.currentTimeMillis() + 10_000L // 10 seconds from now
        scheduleReminder(
            context = context,
            medicineId = "test_med_id",
            medicineName = medicineName,
            dose = dose,
            scheduledTime = "3:45 PM",
            triggerAtMillis = testTriggerTime
        )
    }

    // Reschedules all reminders for all saved medicines
    fun rescheduleAll(context: Context) {
        val repository = MedicineRepository(context)
        CoroutineScope(Dispatchers.IO).launch {
            val allMedicines = repository.medicines.first()
            val allLogs = repository.doseLog.first()
            val todayStr = LocalDate.now().toString()

            for (med in allMedicines) {
                val times = if (med.doseTimes.isNotEmpty()) med.doseTimes else Medicine.defaultDoseTimes(med.timesPerDay)
                for (time in times) {
                    val isTakenToday = allLogs.any { log ->
                        log.medicineId == med.id &&
                        log.scheduledTime == time &&
                        log.date == todayStr
                    }

                    // Skip doses already marked taken today
                    if (!isTakenToday) {
                        val triggerMillis = calculateNextTriggerMillis(time)
                        scheduleReminder(
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
        }
    }

    // Calculates the epoch millisecond timestamp for a given clock time string (e.g. "8:00 AM")
    fun calculateNextTriggerMillis(timeStr: String): Long {
        val now = LocalDateTime.now()
        val minutesOfDay = MedicineRepository.parseTimeToMinutes(timeStr)
        val hour = minutesOfDay / 60
        val minute = minutesOfDay % 60

        var scheduledDateTime = now.toLocalDate().atTime(hour, minute, 0)
        // If scheduled time for today has already passed, schedule for tomorrow
        if (scheduledDateTime.isBefore(now)) {
            scheduledDateTime = scheduledDateTime.plusDays(1)
        }

        return scheduledDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
