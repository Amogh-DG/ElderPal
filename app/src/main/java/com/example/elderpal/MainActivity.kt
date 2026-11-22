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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize high-priority medicine notification channel
        DoseReminderScheduler.createNotificationChannel(applicationContext)

        checkIntentForReminder(intent)

        setContent {
            ElderPalTheme {
                val navController = rememberNavController()

                LaunchedEffect(pendingRoute.value) {
                    val route = pendingRoute.value
                    if (route != null) {
                        navController.navigate(route)
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        checkIntentForReminder(intent)
    }

    private fun checkIntentForReminder(intent: Intent?) {
        if (intent?.getStringExtra("destination") == "reminder") {
            val medId = intent.getStringExtra("medicine_id") ?: ""
            val medName = intent.getStringExtra("medicine_name") ?: "Medicine"
            val dose = intent.getStringExtra("dose") ?: ""
            val scheduledTime = intent.getStringExtra("scheduled_time") ?: ""

            pendingRoute.value = "${ElderPalRoutes.REMINDER}?medicineId=$medId&medicineName=$medName&dose=$dose&time=$scheduledTime"
        }
    }
}
