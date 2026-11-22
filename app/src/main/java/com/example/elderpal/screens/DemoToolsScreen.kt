// Demo tools screen for hackathon judges: load fake data, simulate a fall, and reset everything.
// Not reachable from the main Home design - only via the Permissions screen footer link.
package com.example.elderpal.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.elderpal.R
import com.example.elderpal.data.DemoDataManager
import com.example.elderpal.data.EmergencyAlertManager
import com.example.elderpal.data.FallDetectionService
import com.example.elderpal.data.FallDetector
import com.example.elderpal.ui.theme.BackgroundWarmCream
import com.example.elderpal.ui.theme.BorderCard
import com.example.elderpal.ui.theme.BorderInput
import com.example.elderpal.ui.theme.DangerRed
import com.example.elderpal.ui.theme.DangerRedTint
import com.example.elderpal.ui.theme.ElderPalShapes
import com.example.elderpal.ui.theme.ElderPalTypography
import com.example.elderpal.ui.theme.SuccessGreen
import com.example.elderpal.ui.theme.SuccessTint
import com.example.elderpal.ui.theme.SurfaceWhite
import com.example.elderpal.ui.theme.TealPrimary
import com.example.elderpal.ui.theme.TealPrimaryTint
import com.example.elderpal.ui.theme.TextPrimary
import com.example.elderpal.ui.theme.TextSecondary
import com.example.elderpal.ui.theme.WarningAmberText
import com.example.elderpal.ui.theme.WarningAmberTint
import kotlinx.coroutines.launch

// States the screen can be in after an action completes
private enum class DemoAction { NONE, LOADED, RESET }

@Composable
fun DemoToolsScreen(
    onBack: () -> Unit,
    onSimulateFallClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var phoneNumber by remember { mutableStateOf("") }
    var lastAction by remember { mutableStateOf(DemoAction.NONE) }
    var isBusy by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    // Warning shown when the judge leaves the phone number blank
    var showBlankPhoneWarning by remember { mutableStateOf(false) }
    var voiceCallAlsoFirstContact by remember {
        mutableStateOf(EmergencyAlertManager.isVoiceCallAlsoFirstContact(context))
    }

    // Live debug sensor state for testing on physical phone (in-memory only, never saved or logged)
    var currentG by remember { mutableFloatStateOf(1.0f) }
    var minG by remember { mutableFloatStateOf(1.0f) }
    var maxG by remember { mutableFloatStateOf(1.0f) }
    var detectorStage by remember { mutableStateOf("waiting") }
    var isServiceActive by remember { mutableStateOf(FallDetectionService.isRunning()) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Ephemeral in-memory sliding window for the last 2000ms: pairs of (timestampMs, g)
        val windowSamples = ArrayDeque<Pair<Long, Float>>()

        val updateReadout: (Float, String) -> Unit = { g, stage ->
            val now = System.currentTimeMillis()
            windowSamples.addLast(now to g)
            while (windowSamples.isNotEmpty() && (now - windowSamples.first().first > 2000L)) {
                windowSamples.removeFirst()
            }
            currentG = g
            minG = windowSamples.minOfOrNull { it.second } ?: g
            maxG = windowSamples.maxOfOrNull { it.second } ?: g
            detectorStage = stage
            isServiceActive = FallDetectionService.isRunning()
        }

        val activeDetector = FallDetectionService.activeDetector
        val localDetector: FallDetector?

        if (activeDetector != null) {
            localDetector = null
            activeDetector.onSensorSample = updateReadout
        } else {
            // Service not running: register temporary local FallDetector listener at SENSOR_DELAY_GAME
            val tempDetector = FallDetector(
                onFallDetected = {
                    detectorStage = "triggered"
                }
            )
            tempDetector.onSensorSample = updateReadout
            localDetector = tempDetector
            accelerometer?.let { sensor ->
                sensorManager.registerListener(tempDetector, sensor, SensorManager.SENSOR_DELAY_GAME)
            }
        }

        onDispose {
            if (activeDetector != null) {
                activeDetector.onSensorSample = null
            }
            if (localDetector != null) {
                sensorManager.unregisterListener(localDetector)
            }
            windowSamples.clear()
        }
    }

    // Reset confirmation dialog
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = {
                Text(
                    text = "Reset all data?",
                    style = ElderPalTypography.title.copy(fontSize = 22.sp),
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "This will delete all medicines, contacts and the dose log. It cannot be undone.",
                    style = ElderPalTypography.body.copy(fontSize = 18.sp),
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showResetConfirm = false
                    isBusy = true
                    coroutineScope.launch {
                        DemoDataManager.resetAllData(context)
                        lastAction = DemoAction.RESET
                        isBusy = false
                    }
                }) {
                    Text(
                        text = "Yes, reset",
                        color = DangerRed,
                        style = ElderPalTypography.title.copy(fontSize = 20.sp)
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text(
                        text = "Cancel",
                        color = TextSecondary,
                        style = ElderPalTypography.title.copy(fontSize = 20.sp)
                    )
                }
            },
            containerColor = SurfaceWhite,
            shape = ElderPalShapes.card
        )
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundWarmCream)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header: back button + title
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .height(56.dp)
                    .clip(ElderPalShapes.button)
                    .background(SurfaceWhite)
                    .border(2.dp, BorderCard, ElderPalShapes.button)
                    .clickable(onClick = onBack, role = Role.Button)
                    .semantics {
                        role = Role.Button
                        contentDescription = "Go back"
                    }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_back),
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Back",
                        style = ElderPalTypography.title.copy(color = TextPrimary, fontSize = 20.sp)
                    )
                }
            }
        }

        Text(
            text = "Demo tools",
            style = ElderPalTypography.heading2,
            color = TextPrimary
        )

        Text(
            text = "For judges and demos only. All data loaded here is clearly fake and labelled DEMO.",
            style = ElderPalTypography.body.copy(fontSize = 18.sp),
            color = TextSecondary
        )

        // ── Section 1: Load demo data ───────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = ElderPalShapes.card,
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            border = androidx.compose.material3.CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Load demo data",
                    style = ElderPalTypography.title.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = "Adds 3 fake medicines around the current time, one fake contact, and two logged doses so the schedule looks live.",
                    style = ElderPalTypography.body.copy(fontSize = 18.sp),
                    color = TextSecondary
                )

                // Phone number input ──────────────────────────────────────────
                Text(
                    text = "Your phone number for the demo contact",
                    style = ElderPalTypography.small.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = "SOS and fall alerts send real SMS and calls. Enter a number you own. Leave blank to use a clearly fake placeholder (alerts will fail).",
                    style = ElderPalTypography.body.copy(fontSize = 17.sp),
                    color = TextSecondary
                )
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = {
                        phoneNumber = it
                        showBlankPhoneWarning = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "e.g. +91 98765 00000",
                            style = ElderPalTypography.body.copy(fontSize = 18.sp),
                            color = TextSecondary
                        )
                    },
                    textStyle = ElderPalTypography.body.copy(fontSize = 20.sp, color = TextPrimary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPrimary,
                        unfocusedBorderColor = BorderInput
                    )
                )

                // Warning shown when number is left blank
                if (showBlankPhoneWarning) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = ElderPalShapes.card,
                        colors = CardDefaults.cardColors(containerColor = WarningAmberTint)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_warning),
                                contentDescription = null,
                                tint = WarningAmberText,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "No number entered. The demo contact was saved with a fake placeholder number. Any SOS or fall alert you send during the demo will fail to reach anyone.",
                                style = ElderPalTypography.body.copy(fontSize = 17.sp),
                                color = WarningAmberText
                            )
                        }
                    }
                }

                // Load button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(ElderPalShapes.button)
                        .background(if (isBusy) TealPrimary.copy(alpha = 0.6f) else TealPrimary)
                        .clickable(
                            enabled = !isBusy,
                            onClick = {
                                val trimmed = phoneNumber.trim()
                                if (trimmed.isBlank()) showBlankPhoneWarning = true
                                isBusy = true
                                coroutineScope.launch {
                                    DemoDataManager.loadDemoData(context, trimmed)
                                    lastAction = DemoAction.LOADED
                                    isBusy = false
                                }
                            },
                            role = Role.Button
                        )
                        .semantics {
                            role = Role.Button
                            contentDescription = "Load demo medicines and contact"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isBusy) "Loading…" else "Load demo data",
                        style = ElderPalTypography.title.copy(color = Color.White, fontSize = 22.sp)
                    )
                }
            }
        }

        // Success / feedback banners ─────────────────────────────────────────
        if (lastAction == DemoAction.LOADED) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = ElderPalShapes.card,
                colors = CardDefaults.cardColors(containerColor = SuccessTint)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_check),
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Demo data loaded. Go to Home or Schedule to see it. Reminders are scheduled for the upcoming doses.",
                        style = ElderPalTypography.body.copy(fontSize = 18.sp),
                        color = SuccessGreen
                    )
                }
            }
        }

        if (lastAction == DemoAction.RESET) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = ElderPalShapes.card,
                colors = CardDefaults.cardColors(containerColor = SuccessTint)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_check),
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "All data cleared. The app is now empty.",
                        style = ElderPalTypography.body.copy(fontSize = 18.sp),
                        color = SuccessGreen
                    )
                }
            }
        }

        // ── Voice call test switch ──────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = ElderPalShapes.card,
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            border = androidx.compose.material3.CardDefaults.outlinedCardBorder()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp)
                ) {
                    Text(
                        text = "Voice call test: also call the first contact",
                        style = ElderPalTypography.title.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Twilio voice calls normally go to secondary family contacts while the primary contact gets a direct phone call. Turn this on to also place a Twilio voice call to the primary contact when testing with a single phone number.",
                        style = ElderPalTypography.body.copy(fontSize = 17.sp),
                        color = TextSecondary
                    )
                }

                Switch(
                    checked = voiceCallAlsoFirstContact,
                    onCheckedChange = { isChecked ->
                        voiceCallAlsoFirstContact = isChecked
                        EmergencyAlertManager.setVoiceCallAlsoFirstContact(context, isChecked)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = TealPrimary
                    )
                )
            }
        }

        // ── Section 2: Simulate fall ─────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = ElderPalShapes.card,
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            border = androidx.compose.material3.CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Fall detection demo",
                    style = ElderPalTypography.title.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = "Triggers the fall alert countdown immediately without dropping the phone. Tap \"I'm OK, cancel\" on the next screen to dismiss it without sending anything.",
                    style = ElderPalTypography.body.copy(fontSize = 18.sp),
                    color = TextSecondary
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(ElderPalShapes.button)
                        .background(WarningAmberTint)
                        .border(2.dp, WarningAmberText, ElderPalShapes.button)
                        .clickable(onClick = onSimulateFallClick, role = Role.Button)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Simulate a fall to test the emergency alert flow"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_warning),
                            contentDescription = null,
                            tint = WarningAmberText,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Simulate fall (Test)",
                            style = ElderPalTypography.title.copy(
                                color = WarningAmberText,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }

        // ── Section 3: Sensor test (Live debug readout) ───────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = ElderPalShapes.card,
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            border = BorderStroke(2.dp, BorderCard)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sensor test",
                        style = ElderPalTypography.title.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    // Service state badge
                    Surface(
                        shape = CircleShape,
                        color = if (isServiceActive) SuccessTint else WarningAmberTint,
                        border = BorderStroke(1.5.dp, if (isServiceActive) SuccessGreen else WarningAmberText)
                    ) {
                        Text(
                            text = if (isServiceActive) "Service running" else "Temp test sensor",
                            style = ElderPalTypography.small.copy(
                                color = if (isServiceActive) SuccessGreen else WarningAmberText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = "Live accelerometer readout from phone sensors. Drop phone onto a bed to inspect values in real-time.",
                    style = ElderPalTypography.body.copy(fontSize = 17.sp),
                    color = TextSecondary
                )

                // Current G readout card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ElderPalShapes.card,
                    colors = CardDefaults.cardColors(containerColor = TealPrimaryTint),
                    border = BorderStroke(1.5.dp, TealPrimary)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Current acceleration",
                                style = ElderPalTypography.small.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                                color = TealPrimary
                            )
                            Text(
                                text = "1.00g = resting gravity",
                                style = ElderPalTypography.small.copy(fontSize = 14.sp),
                                color = TextSecondary
                            )
                        }

                        Text(
                            text = String.format("%.2f g", currentG),
                            style = ElderPalTypography.display.copy(
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                                color = TealPrimary
                            )
                        )
                    }
                }

                // Min and Max over last 2 seconds
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Lowest (Min) in last 2 seconds
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = ElderPalShapes.card,
                        colors = CardDefaults.cardColors(
                            containerColor = if (minG < 0.5f) SuccessTint else BackgroundWarmCream
                        ),
                        border = BorderStroke(
                            1.5.dp,
                            if (minG < 0.5f) SuccessGreen else BorderCard
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "Lowest (last 2s)",
                                style = ElderPalTypography.small.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                                color = if (minG < 0.5f) SuccessGreen else TextPrimary
                            )
                            Text(
                                text = "Free-fall: < 0.50g",
                                style = ElderPalTypography.small.copy(fontSize = 13.sp),
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format("%.2f g", minG),
                                style = ElderPalTypography.title.copy(
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (minG < 0.5f) SuccessGreen else TextPrimary
                                )
                            )
                        }
                    }

                    // Highest (Max) in last 2 seconds
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = ElderPalShapes.card,
                        colors = CardDefaults.cardColors(
                            containerColor = if (maxG > 1.8f) SuccessTint else BackgroundWarmCream
                        ),
                        border = BorderStroke(
                            1.5.dp,
                            if (maxG > 1.8f) SuccessGreen else BorderCard
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "Highest (last 2s)",
                                style = ElderPalTypography.small.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                                color = if (maxG > 1.8f) SuccessGreen else TextPrimary
                            )
                            Text(
                                text = "Impact: > 1.80g",
                                style = ElderPalTypography.small.copy(fontSize = 13.sp),
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format("%.2f g", maxG),
                                style = ElderPalTypography.title.copy(
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (maxG > 1.8f) SuccessGreen else TextPrimary
                                )
                            )
                        }
                    }
                }

                // Stage reached
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ElderPalShapes.card,
                    colors = CardDefaults.cardColors(
                        containerColor = when (detectorStage) {
                            "triggered" -> DangerRedTint
                            "inactive" -> SuccessTint
                            "impact seen", "free-fall seen" -> WarningAmberTint
                            else -> BackgroundWarmCream
                        }
                    ),
                    border = BorderStroke(
                        1.5.dp,
                        when (detectorStage) {
                            "triggered" -> DangerRed
                            "inactive" -> SuccessGreen
                            "impact seen", "free-fall seen" -> WarningAmberText
                            else -> BorderCard
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Detector stage",
                                style = ElderPalTypography.small.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = "waiting → free-fall → impact → inactive → triggered",
                                style = ElderPalTypography.small.copy(fontSize = 11.sp),
                                color = TextSecondary
                            )
                        }

                        Text(
                            text = detectorStage,
                            style = ElderPalTypography.title.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (detectorStage) {
                                    "triggered" -> DangerRed
                                    "inactive" -> SuccessGreen
                                    "impact seen", "free-fall seen" -> WarningAmberText
                                    else -> TealPrimary
                                }
                            )
                        )
                    }
                }
            }
        }

        // ── Section 4: Reset ────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = ElderPalShapes.card,
            colors = CardDefaults.cardColors(containerColor = DangerRedTint)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Reset data",
                    style = ElderPalTypography.title.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                    color = DangerRed
                )
                Text(
                    text = "Clears all medicines, contacts and the dose log and cancels all reminders. Use this to restore the app to its empty state between demos.",
                    style = ElderPalTypography.body.copy(fontSize = 18.sp),
                    color = TextPrimary
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(ElderPalShapes.button)
                        .background(DangerRed)
                        .clickable(
                            enabled = !isBusy,
                            onClick = { showResetConfirm = true },
                            role = Role.Button
                        )
                        .semantics {
                            role = Role.Button
                            contentDescription = "Reset all app data"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Reset data",
                        style = ElderPalTypography.title.copy(color = Color.White, fontSize = 22.sp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
