// Foreground service continuously monitoring the phone's accelerometer for suspected falls.
package com.example.elderpal.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.elderpal.MainActivity
import com.example.elderpal.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FallDetectionService : Service() {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var fallDetector: FallDetector
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    companion object {
        const val CHANNEL_ID = "fall_detection_foreground_channel"
        const val NOTIFICATION_ID = 2001
        const val ALERT_NOTIFICATION_ID = 2002
        const val ACTION_FALL_ALERT = "com.example.elderpal.ACTION_FALL_ALERT"

        // Global reference so the UI can notify the detector of cancellation or alert completion
        var activeDetector: FallDetector? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        fallDetector = FallDetector(
            onFallDetected = { onSuspectedFall() }
        )
        activeDetector = fallDetector

        accelerometer?.let { sensor ->
            sensorManager.registerListener(
                fallDetector,
                sensor,
                SensorManager.SENSOR_DELAY_GAME
            )
        }

        startForegroundServiceNotification()
    }

    private fun startForegroundServiceNotification() {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_motion)
            .setContentTitle("ElderPal Fall Detection")
            .setContentText("Monitoring motion in the background for safety.")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
            startForeground(NOTIFICATION_ID, notification, fgsType)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun onSuspectedFall() {
        serviceScope.launch {
            val contactsRepository = ContactsRepository(applicationContext)
            val contacts = contactsRepository.getAllContacts()

            if (contacts.isEmpty()) {
                // Requirement 5: Do not start a countdown that cannot send. Show add contact message instead.
                showNoContactsNotification()
                return@launch
            }

            fallDetector.isAlertActive = true
            launchFullScreenFallAlert()
        }
    }

    private fun launchFullScreenFallAlert() {
        val fallIntent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_FALL_ALERT
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            1,
            fallIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alertNotification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle("Are you okay?")
            .setContentText("Suspected fall detected. Tap to confirm you are safe.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(ALERT_NOTIFICATION_ID, alertNotification)

        // Attempt direct launch if allowed
        try {
            startActivity(fallIntent)
        } catch (e: Exception) {
            // Full-screen intent notification serves as fallback
        }
    }

    private fun showNoContactsNotification() {
        val contactsIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            2,
            contactsIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle("Fall detected: No contacts saved")
            .setContentText("Please add an emergency contact so ElderPal can send alerts.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(ALERT_NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Fall Detection Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps fall detection active in the background"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(fallDetector)
        activeDetector = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
