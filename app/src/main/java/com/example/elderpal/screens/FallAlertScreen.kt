// Full-screen fall alert countdown screen with audible chime, vibration, cancel button, and emergency dispatch.
package com.example.elderpal.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.elderpal.R
import com.example.elderpal.data.AlertDispatchReport
import com.example.elderpal.data.AlertType
import com.example.elderpal.data.ContactsRepository
import com.example.elderpal.data.EmergencyAlertManager
import com.example.elderpal.data.FallDetectionService
import com.example.elderpal.data.MedicineRepository
import com.example.elderpal.ui.theme.BackgroundWarmCream
import com.example.elderpal.ui.theme.DangerRed
import com.example.elderpal.ui.theme.DangerRedTint
import com.example.elderpal.ui.theme.ElderPalShapes
import com.example.elderpal.ui.theme.ElderPalTypography
import com.example.elderpal.ui.theme.SuccessGreen
import com.example.elderpal.ui.theme.SurfaceWhite
import com.example.elderpal.ui.theme.TealPrimary
import com.example.elderpal.ui.theme.TealPrimaryTint
import com.example.elderpal.ui.theme.TextPrimary
import com.example.elderpal.ui.theme.TextSecondary
import com.example.elderpal.ui.theme.WarningAmberText
import com.example.elderpal.ui.theme.WarningAmberTint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class FallAlertStep {
    COUNTDOWN,
    DISPATCHING,
    SENT
}

@Composable
fun FallAlertScreen(
    onBack: () -> Unit,
    onNavigateToContacts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val contactsRepository = remember { ContactsRepository(context) }
    val medicineRepository = remember { MedicineRepository(context) }
    val alertManager = remember { EmergencyAlertManager(context) }
    val coroutineScope = rememberCoroutineScope()

    val contacts by contactsRepository.contacts.collectAsState(initial = emptyList())

    var currentStep by remember { mutableStateOf(FallAlertStep.COUNTDOWN) }
    var secondsRemaining by remember { mutableIntStateOf(10) }
    var alertReport by remember { mutableStateOf<AlertDispatchReport?>(null) }
    var isCancelled by remember { mutableStateOf(false) }

    // Setup vibrator and sound chime
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    val ringtone = remember {
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        RingtoneManager.getRingtone(context, soundUri)
    }

    fun stopSoundAndVibration() {
        try {
            vibrator?.cancel()
            if (ringtone?.isPlaying == true) {
                ringtone.stop()
            }
        } catch (e: Exception) {
            // Ignore teardown errors
        }
    }

    fun executeFallAlertDispatch() {
        stopSoundAndVibration()
        currentStep = FallAlertStep.DISPATCHING

        coroutineScope.launch(Dispatchers.IO) {
            val location = alertManager.getLastKnownLocation()
            val lastDose = medicineRepository.getLastTakenDoseToday()
            val report = alertManager.dispatchEmergencyAlert(
                contacts = contacts,
                location = location,
                lastTakenDose = lastDose,
                alertType = AlertType.FALL_DETECTED
            )

            withContext(Dispatchers.Main) {
                alertReport = report
                currentStep = FallAlertStep.SENT
                FallDetectionService.activeDetector?.isAlertActive = false
            }
        }
    }

    // Lifecycle cleanup for sound and vibration
    DisposableEffect(Unit) {
        onDispose {
            stopSoundAndVibration()
        }
    }

    // 10-Second Countdown Timer with sound & vibration
    LaunchedEffect(currentStep, contacts) {
        if (currentStep == FallAlertStep.COUNTDOWN && contacts.isNotEmpty() && !isCancelled) {
            // Start repeating vibration pulse (vibrate 400ms, pause 600ms)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 600), 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 400, 600), 0)
                }
            } catch (e: Exception) {
                // Ignore vibration failure
            }

            while (secondsRemaining > 0 && !isCancelled) {
                try {
                    if (ringtone?.isPlaying == false) {
                        ringtone.play()
                    }
                } catch (e: Exception) {
                    // Ignore sound failure
                }

                delay(1000L)
                if (!isCancelled) {
                    secondsRemaining -= 1
                }
            }

            if (secondsRemaining == 0 && !isCancelled) {
                executeFallAlertDispatch()
            }
        }
    }

    // Render screen according to current step
    when (currentStep) {
        FallAlertStep.COUNTDOWN -> {
            if (contacts.isEmpty()) {
                // Requirement 5: No contacts saved guard
                FallNoContactsView(
                    onAddContactsClick = onNavigateToContacts,
                    onCancelClick = onBack,
                    modifier = modifier
                )
            } else {
                // Main countdown view matching design/fall-alert.png
                FallCountdownView(
                    secondsRemaining = secondsRemaining,
                    onCancelClick = {
                        isCancelled = true
                        stopSoundAndVibration()
                        FallDetectionService.activeDetector?.notifyCancelled()
                        onBack()
                    },
                    onNeedHelpClick = {
                        isCancelled = true
                        executeFallAlertDispatch()
                    },
                    modifier = modifier
                )
            }
        }
        FallAlertStep.DISPATCHING -> {
            // Loading / dispatching state
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(BackgroundWarmCream)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(TealPrimaryTint),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = TealPrimary,
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(64.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Sending fall alert...",
                    style = ElderPalTypography.heading2,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Sending your location and calling your emergency contact.",
                    style = ElderPalTypography.body,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
        FallAlertStep.SENT -> {
            alertReport?.let { report ->
                EmergencyAlertSentView(
                    report = report,
                    onCallAgainClick = {
                        val primary = report.primaryContact
                        if (primary != null) {
                            val normalized = EmergencyAlertManager.normalizePhoneNumber(primary.phoneNumber)
                            val hasCallPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CALL_PHONE
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasCallPermission) {
                                alertManager.placeDirectPhoneCall(normalized)
                            } else {
                                alertManager.openDialerFallback(normalized)
                            }
                        }
                    },
                    onOkClick = onBack,
                    modifier = modifier
                )
            }
        }
    }
}

// Countdown view matching design/fall-alert.png
@Composable
private fun FallCountdownView(
    secondsRemaining: Int,
    onCancelClick: () -> Unit,
    onNeedHelpClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundWarmCream)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Top content: Headline & Explanation matching design/fall-alert.png
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Are you okay?",
                style = ElderPalTypography.display.copy(fontSize = 36.sp),
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "I think you may have fallen. I will\ntell your family when the time\nruns out.",
                style = ElderPalTypography.body.copy(fontSize = 20.sp, lineHeight = 30.sp),
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Center Circular Countdown matching design/fall-alert.png
            Box(
                modifier = Modifier.size(190.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background Track and Progress Arc
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidthPx = 18.dp.toPx()
                    // Background track
                    drawCircle(
                        color = SurfaceWhite,
                        style = Stroke(width = strokeWidthPx)
                    )
                    // Warning amber progress arc
                    val sweepAngle = (secondsRemaining / 10f) * 360f
                    drawArc(
                        color = WarningAmberText,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                    )
                }

                // Number and "seconds" text inside ring
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$secondsRemaining",
                        style = ElderPalTypography.display.copy(
                            fontSize = 54.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = TextPrimary
                    )
                    Text(
                        text = "seconds",
                        style = ElderPalTypography.title.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = WarningAmberText
                    )
                }
            }
        }

        // Bottom Action Buttons matching design/fall-alert.png
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // "I'm OK, cancel" extra large button (112dp tall) in SuccessGreen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .clip(ElderPalShapes.button)
                    .background(SuccessGreen)
                    .clickable(
                        onClick = onCancelClick,
                        role = Role.Button
                    )
                    .semantics {
                        role = Role.Button
                        contentDescription = "I am okay, cancel alert"
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_check),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "I'm OK, cancel",
                        style = ElderPalTypography.display.copy(
                            color = Color.White,
                            fontSize = 28.sp
                        )
                    )
                }
            }

            // "I need help now" button (76dp tall) in DangerRed
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .clip(ElderPalShapes.button)
                    .background(DangerRed)
                    .clickable(
                        onClick = onNeedHelpClick,
                        role = Role.Button
                    )
                    .semantics {
                        role = Role.Button
                        contentDescription = "I need help now, send alert immediately"
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_warning),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "I need help now",
                        style = ElderPalTypography.title.copy(
                            color = Color.White,
                            fontSize = 24.sp
                        )
                    )
                }
            }
        }
    }
}

// Fall Alert guard view when no emergency contacts are configured
@Composable
private fun FallNoContactsView(
    onAddContactsClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundWarmCream)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(DangerRedTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_warning),
                    contentDescription = null,
                    tint = DangerRed,
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "No contacts saved",
                style = ElderPalTypography.display.copy(fontSize = 32.sp),
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "ElderPal noticed a fall, but there are no emergency contacts to alert. Please add a family contact.",
                style = ElderPalTypography.body.copy(fontSize = 20.sp, lineHeight = 30.sp),
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(ElderPalShapes.button)
                    .background(TealPrimary)
                    .clickable(
                        onClick = onAddContactsClick,
                        role = Role.Button
                    )
                    .semantics {
                        role = Role.Button
                        contentDescription = "Add emergency contact"
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Add emergency contact",
                        style = ElderPalTypography.title.copy(color = Color.White, fontSize = 22.sp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(ElderPalShapes.button)
                    .background(SurfaceWhite)
                    .clickable(
                        onClick = onCancelClick,
                        role = Role.Button
                    )
                    .semantics {
                        role = Role.Button
                        contentDescription = "Dismiss"
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Dismiss",
                    style = ElderPalTypography.title.copy(color = TextPrimary, fontSize = 22.sp)
                )
            }
        }
    }
}
