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
        const val ALERT_CHANNEL_ID = "fall_alert_high_priority_channel"
        const val NOTIFICATION_ID = 2001
        const val ALERT_NOTIFICATION_ID = 2002
        const val ACTION_FALL_ALERT = "com.example.elderpal.ACTION_FALL_ALERT"

        var activeDetector: FallDetector? = null
            private set

        fun isRunning(): Boolean = activeDetector != null

        fun start(context: Context) {
            val intent = Intent(context, FallDetectionService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FallDetectionService::class.java)
            context.stopService(intent)
        }
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
                // skip countdown if no contacts are saved
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
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            1,
            fallIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val canUseFullScreen = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            notificationManager.canUseFullScreenIntent()
        } else {
            true
        }

        val contentText = if (canUseFullScreen) {
            "Suspected fall detected. Confirm you are safe."
        } else {
            "Suspected fall detected. Tap to open and confirm you are safe."
        }

        val alertNotification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle("Are you okay?")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        notificationManager.notify(ALERT_NOTIFICATION_ID, alertNotification)

        // open countdown directly if already in foreground
        if (MainActivity.isForeground) {
            MainActivity.instance?.navigateToFallAlert()
        } else {
            try {
                startActivity(fallIntent)
            } catch (e: Exception) {
            }
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

        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
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
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Fall Detection Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps fall detection active in the background"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(serviceChannel)

            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Fall Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical alerts when a suspected fall is detected"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableVibration(true)
                enableLights(true)
                setBypassDnd(true)
            }
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(fallDetector)
        activeDetector = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
