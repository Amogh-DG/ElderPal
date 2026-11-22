// Reschedules active medicine reminders and restarts fall detection service when the device reboots.
package com.example.elderpal.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            DoseReminderScheduler.rescheduleAll(context)

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val fallPrefs = FallDetectionPreferences(context)
                    if (fallPrefs.isEnabled()) {
                        FallDetectionPreferences.startService(context)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
