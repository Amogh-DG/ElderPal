// Displays the ElderPal Home screen with the next scheduled medicine, quick actions, and an SOS button.
package com.example.elderpal.screens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch
import com.example.elderpal.R
import com.example.elderpal.data.FallDetectionService
import com.example.elderpal.data.MedicineRepository
import com.example.elderpal.data.PermissionManager
import com.example.elderpal.ui.theme.BackgroundWarmCream
import com.example.elderpal.ui.theme.BorderCard
import com.example.elderpal.ui.theme.BorderInput
import com.example.elderpal.ui.theme.DangerRed
import com.example.elderpal.ui.theme.DangerRedShadow
import com.example.elderpal.ui.theme.ElderPalShapes
import com.example.elderpal.ui.theme.ElderPalTheme
import com.example.elderpal.ui.theme.ElderPalTypography
import com.example.elderpal.ui.theme.SuccessGreen
import com.example.elderpal.ui.theme.SurfaceWhite
import com.example.elderpal.ui.theme.TealPrimary
import com.example.elderpal.ui.theme.TealPrimaryTint
import com.example.elderpal.ui.theme.TextPrimary
import com.example.elderpal.ui.theme.TextSecondary
import com.example.elderpal.ui.theme.WarningAmberText

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onScheduleClick: () -> Unit = {},
    onScanPrescriptionClick: () -> Unit = {},
    onCheckMedicineClick: () -> Unit = {},
    onEmergencyContactsClick: () -> Unit = {},
    onSosClick: () -> Unit = {},
    onLogoClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { MedicineRepository(context) }
    val fallPrefs = remember { com.example.elderpal.data.FallDetectionPreferences(context) }
    val coroutineScope = rememberCoroutineScope()

    val todayDoses by repository.todayDoses.collectAsState(initial = emptyList())
    val nextDose = remember(todayDoses) { repository.findNextDose(todayDoses) }
    val isFallDetectionEnabled by fallPrefs.isFallDetectionEnabled.collectAsState(initial = false)

    // Ensure the background service state matches the saved preference on app launch/reopen
    LaunchedEffect(isFallDetectionEnabled) {
        if (isFallDetectionEnabled && !FallDetectionService.isRunning()) {
            FallDetectionService.start(context)
        } else if (!isFallDetectionEnabled && FallDetectionService.isRunning()) {
            FallDetectionService.stop(context)
        }
    }

    var showMotionExplainer by remember { mutableStateOf(false) }

    if (showMotionExplainer) {
        MotionPermissionExplainer(
            onAllowClick = {
                showMotionExplainer = false
                FallDetectionService.start(context)
                coroutineScope.launch {
                    fallPrefs.setFallDetectionEnabled(true)
                }
            },
            onDismissClick = {
                showMotionExplainer = false
                PermissionManager.setPermissionRefused(context, PermissionManager.KEY_MOTION_REFUSED, true)
            }
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
        // App header with heart badge and greeting
        HomeHeader(onLogoClick = onLogoClick)

        // Card showing next medicine due from stored data - tap opens Schedule
        if (todayDoses.isEmpty()) {
            NextMedicineCard(
                chipText = "Schedule",
                medicineName = "No medicine scheduled",
                dose = "Tap to view schedule or add medicine",
                scheduledTime = "--:--",
                onClick = onScheduleClick
            )
        } else if (todayDoses.all { it.status == com.example.elderpal.data.DoseStatus.TAKEN }) {
            NextMedicineCard(
                chipText = "All done",
                medicineName = "All doses taken today",
                dose = "Next schedule begins tomorrow",
                scheduledTime = "Completed",
                onClick = onScheduleClick
            )
        } else if (nextDose != null) {
            val doseDesc = "${nextDose.dose}${if (nextDose.takesWithFood) ", with food" else ""}"
            NextMedicineCard(
                chipText = "Next medicine due",
                medicineName = nextDose.medicineName,
                dose = doseDesc,
                scheduledTime = nextDose.time,
                onClick = onScheduleClick
            )
        }

        // Action buttons
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HomeActionButton(
                label = "Scan Prescription",
                icon = painterResource(id = R.drawable.ic_prescription),
                onClick = onScanPrescriptionClick
            )

            HomeActionButton(
                label = "Check Medicine",
                icon = painterResource(id = R.drawable.ic_pill),
                onClick = onCheckMedicineClick
            )

            HomeActionButton(
                label = "Emergency Contacts",
                icon = painterResource(id = R.drawable.ic_contacts),
                onClick = onEmergencyContactsClick
            )
        }

        // Fall detection card with on/off switch
        FallDetectionCard(
            isEnabled = isFallDetectionEnabled,
            onToggle = { enable ->
                if (enable) {
                    val alreadyRefused = PermissionManager.isPermissionRefused(context, PermissionManager.KEY_MOTION_REFUSED)
                    val firstRunDone = PermissionManager.isFirstRunCompleted(context)
                    if (!firstRunDone && !alreadyRefused) {
                        showMotionExplainer = true
                    } else {
                        FallDetectionService.start(context)
                        coroutineScope.launch {
                            fallPrefs.setFallDetectionEnabled(true)
                        }
                    }
                } else {
                    FallDetectionService.stop(context)
                    coroutineScope.launch {
                        fallPrefs.setFallDetectionEnabled(false)
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Large prominent SOS emergency button
        SosEmergencyBar(
            onClick = onSosClick
        )
    }
}

@Composable
private fun HomeHeader(
    onLogoClick: () -> Unit = {}
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(ElderPalShapes.logo)
                    .background(TealPrimary)
                    .clickable(
                        onClick = onLogoClick,
                        role = Role.Button
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_heart),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = stringResource(id = R.string.app_name),
                style = ElderPalTypography.display
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Good afternoon. Here is what is next.",
            style = ElderPalTypography.greeting
        )
    }
}

@Composable
private fun NextMedicineCard(
    chipText: String = "Next medicine due",
    medicineName: String,
    dose: String,
    scheduledTime: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
                role = Role.Button
            )
            .semantics {
                role = Role.Button
                contentDescription = "$chipText: $medicineName, $dose at $scheduledTime. Tap to open Schedule."
            },
        shape = ElderPalShapes.card,
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(2.dp, TealPrimary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Pill status chip
            Surface(
                shape = ElderPalShapes.pill,
                color = TealPrimaryTint,
                border = BorderStroke(1.5.dp, TealPrimary)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_clock),
                        contentDescription = null,
                        tint = TealPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = chipText,
                        style = ElderPalTypography.label.copy(
                            fontSize = 18.sp,
                            color = TealPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Medicine Name
            Text(
                text = medicineName,
                style = ElderPalTypography.heading2
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Dose details
            Text(
                text = dose,
                style = ElderPalTypography.body
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Due Time row with prominent clock icon
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_clock),
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(34.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = scheduledTime,
                    style = ElderPalTypography.big
                )
            }
        }
    }
}

@Composable
private fun HomeActionButton(
    label: String,
    icon: Painter,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(ElderPalShapes.button)
            .background(TealPrimaryTint)
            .border(3.dp, TealPrimary, ElderPalShapes.button)
            .clickable(
                onClick = onClick,
                role = Role.Button
            )
            .semantics {
                role = Role.Button
                contentDescription = label
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = TealPrimary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = label,
                style = ElderPalTypography.title.copy(color = TealPrimary)
            )
        }
    }
}

@Composable
private fun SosEmergencyBar(
    onClick: () -> Unit
) {
    // 3D tactile button with bottom shadow layer (#7F1712) and red fill (#B3261E)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .clip(ElderPalShapes.sosBar)
            .background(DangerRedShadow)
            .clickable(
                onClick = onClick,
                role = Role.Button
            )
            .semantics {
                role = Role.Button
                contentDescription = "SOS Emergency help"
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(106.dp)
                .clip(ElderPalShapes.sosBar)
                .background(DangerRed)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular white badge with siren icon
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_sos_beacon),
                        contentDescription = null,
                        tint = DangerRed,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "SOS",
                        style = ElderPalTypography.big.copy(
                            color = Color.White,
                            fontSize = 42.sp
                        )
                    )
                    Text(
                        text = "Emergency help",
                        style = ElderPalTypography.label.copy(
                            color = Color.White,
                            fontSize = 20.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun FallDetectionCard(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isEnabled) TealPrimaryTint else Color(0xFFF0EBE1)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_motion),
                        contentDescription = null,
                        tint = if (isEnabled) TealPrimary else TextSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Fall detection",
                        style = ElderPalTypography.title.copy(fontSize = 22.sp),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isEnabled) "Active in background" else "Turned off",
                        style = ElderPalTypography.small.copy(fontSize = 16.sp),
                        color = if (isEnabled) SuccessGreen else TextSecondary
                    )
                }

                androidx.compose.material3.Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    colors = androidx.compose.material3.SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = TealPrimary,
                        uncheckedThumbColor = SurfaceWhite,
                        uncheckedTrackColor = BorderInput
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun HomeScreenPreview() {
    ElderPalTheme {
        HomeScreen()
    }
}
