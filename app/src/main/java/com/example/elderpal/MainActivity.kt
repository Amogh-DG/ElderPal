// Main activity that serves as the entry point for the ElderPal application.
package com.example.elderpal

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.elderpal.data.DoseReminderScheduler
import com.example.elderpal.screens.ElderPalNavHost
import com.example.elderpal.screens.ElderPalRoutes
import com.example.elderpal.ui.theme.BackgroundWarmCream
import com.example.elderpal.ui.theme.ElderPalTheme

class MainActivity : ComponentActivity() {

    private val pendingRoute = mutableStateOf<String?>(null)

    companion object {
        var isForeground: Boolean = false
            private set
        var instance: MainActivity? = null
            private set
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
        wakeAndUnlockScreen()
        enableEdgeToEdge()

        // Initialize high-priority medicine notification channel
        DoseReminderScheduler.createNotificationChannel(applicationContext)

        handleIncomingIntent(intent)

        setContent {
            ElderPalTheme {
                val navController = rememberNavController()

                LaunchedEffect(pendingRoute.value) {
                    val route = pendingRoute.value
                    if (route != null) {
                        navController.navigate(route) {
                            launchSingleTop = true
                        }
                        pendingRoute.value = null
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = BackgroundWarmCream
                ) { innerPadding ->
                    ElderPalNavHost(
                        modifier = Modifier.padding(innerPadding),
                        navController = navController
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isForeground = true
        instance = this
    }

    override fun onPause() {
        super.onPause()
        isForeground = false
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        wakeAndUnlockScreen()
        handleIncomingIntent(intent)
    }

    fun navigateToFallAlert() {
        runOnUiThread {
            wakeAndUnlockScreen()
            pendingRoute.value = ElderPalRoutes.FALL_ALERT
        }
    }

    private fun wakeAndUnlockScreen() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.action == com.example.elderpal.data.FallDetectionService.ACTION_FALL_ALERT) {
            pendingRoute.value = ElderPalRoutes.FALL_ALERT
            return
        }
        if (intent.getStringExtra("destination") == "reminder") {
            val medId = intent.getStringExtra("medicine_id") ?: ""
            val medName = intent.getStringExtra("medicine_name") ?: "Medicine"
            val dose = intent.getStringExtra("dose") ?: ""
            val scheduledTime = intent.getStringExtra("scheduled_time") ?: ""

            pendingRoute.value = "${ElderPalRoutes.REMINDER}?medicineId=$medId&medicineName=$medName&dose=$dose&time=$scheduledTime"
        }
    }
}
