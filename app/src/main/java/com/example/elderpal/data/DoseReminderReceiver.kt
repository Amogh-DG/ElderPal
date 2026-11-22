// Broadcast receiver that receives alarm events and displays the high-priority dose reminder notification.
package com.example.elderpal.data

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.elderpal.MainActivity
import com.example.elderpal.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

class DoseReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val medicineId = intent.getStringExtra(DoseReminderScheduler.EXTRA_MEDICINE_ID) ?: return
        val medicineName = intent.getStringExtra(DoseReminderScheduler.EXTRA_MEDICINE_NAME) ?: "Medicine"
        val dose = intent.getStringExtra(DoseReminderScheduler.EXTRA_DOSE) ?: ""
        val scheduledTime = intent.getStringExtra(DoseReminderScheduler.EXTRA_SCHEDULED_TIME) ?: ""
        val notificationId = intent.getIntExtra(DoseReminderScheduler.EXTRA_NOTIFICATION_ID, 1001)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Ensure notification channel exists
                DoseReminderScheduler.createNotificationChannel(context)

                // Check whether this dose was already marked as taken today in the log
                val repository = MedicineRepository(context)
                val todayStr = LocalDate.now().toString()
                val allLogs = repository.doseLog.first()
                val isTakenToday = allLogs.any { log ->
                    log.medicineId == medicineId &&
                    log.scheduledTime == scheduledTime &&
                    log.date == todayStr
                }

                // If already taken, skip notification
                if (!isTakenToday) {
                    showNotification(
                        context = context,
                        medicineId = medicineId,
                        medicineName = medicineName,
                        dose = dose,
                        scheduledTime = scheduledTime,
                        notificationId = notificationId
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(
        context: Context,
        medicineId: String,
        medicineName: String,
        dose: String,
        scheduledTime: String,
        notificationId: Int
    ) {
        // Tapping the notification launches the Reminder screen in MainActivity
        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("destination", "reminder")
            putExtra("medicine_id", medicineId)
            putExtra("medicine_name", medicineName)
            putExtra("dose", dose)
            putExtra("scheduled_time", scheduledTime)
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Concise plain text readable on smartwatches and phone notification heads-up banners
        val notification = NotificationCompat.Builder(context, DoseReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_pill)
            .setContentTitle("Time for your medicine")
            .setContentText("$medicineName, $dose")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }
}
