// Displays today's medicine schedule with real statuses (Upcoming, Taken, Missed) and manual logging.
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.elderpal.R
import com.example.elderpal.data.DoseStatus
import com.example.elderpal.data.MedicineRepository
import com.example.elderpal.data.ScheduledDose
import com.example.elderpal.ui.theme.BackgroundWarmCream
import com.example.elderpal.ui.theme.BorderCard
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
import kotlinx.coroutines.launch
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.example.elderpal.data.DoseReminderScheduler

@Composable
fun ScheduleScreen(
    onBack: () -> Unit,
    onAddMedicineClick: () -> Unit,
    onSosClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { MedicineRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    val doses by repository.todayDoses.collectAsState(initial = emptyList())

    val todayFormatted = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))
    }

    var showPermissionExplainer by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            DoseReminderScheduler.rescheduleAll(context)
        }
    }

    // Ensure notification channel exists and initial check
    LaunchedEffect(Unit) {
        DoseReminderScheduler.createNotificationChannel(context)
    }

    // Find the next upcoming dose to highlight it with the primary teal border
    val nextDose = remember(doses) { repository.findNextDose(doses) }

    if (showPermissionExplainer) {
        NotificationPermissionExplainer(
            onAllowClick = {
                showPermissionExplainer = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onDismissClick = {
                showPermissionExplainer = false
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundWarmCream)
            .padding(16.dp)
    ) {
        // Inner screen top navigation (Back and SOS)
        InnerScreenHeader(
            onBack = onBack,
            onSosClick = onSosClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Screen title and today's date
        Text(
            text = "Today's medicine",
            style = ElderPalTypography.display,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Date and test reminder trigger button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = todayFormatted,
                style = ElderPalTypography.body.copy(color = TextSecondary)
            )

            // Debug button to test notification reminder within 10 seconds
            Surface(
                shape = ElderPalShapes.input,
                color = TealPrimaryTint,
                border = BorderStroke(1.5.dp, TealPrimary),
                modifier = Modifier.clickable {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) {
                        showPermissionExplainer = true
                    } else {
                        val testMedName = doses.firstOrNull()?.medicineName ?: "Blood Pressure pill"
                        val testDose = doses.firstOrNull()?.dose ?: "1 tablet"
                        DoseReminderScheduler.triggerTestReminder(context, testMedName, testDose)
                        Toast.makeText(context, "Test reminder will fire in 10s!", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_bell),
                        contentDescription = null,
                        tint = TealPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Test 10s",
                        style = ElderPalTypography.label.copy(fontSize = 14.sp, color = TealPrimary)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content area: either empty state or list of today's doses
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (doses.isEmpty()) {
                ScheduleEmptyState(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(doses) { _, doseItem ->
                        val isNext = doseItem == nextDose
                        ScheduledDoseCard(
                            dose = doseItem,
                            isHighlighted = isNext,
                            onMarkTaken = {
                                coroutineScope.launch {
                                    repository.markDoseTaken(
                                        medicineId = doseItem.medicineId,
                                        medicineName = doseItem.medicineName,
                                        scheduledTime = doseItem.time
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom "+ Add medicine" button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(ElderPalShapes.button)
                .background(TealPrimary)
                .clickable(
                    onClick = onAddMedicineClick,
                    role = Role.Button
                )
                .semantics {
                    role = Role.Button
                    contentDescription = "Add medicine"
                },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Add medicine",
                    style = ElderPalTypography.title.copy(color = Color.White)
                )
            }
        }
    }
}

@Composable
private fun ScheduledDoseCard(
    dose: ScheduledDose,
    isHighlighted: Boolean,
    onMarkTaken: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ElderPalShapes.card,
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(
            width = 2.dp,
            color = if (isHighlighted) TealPrimary else BorderCard
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Top row: Clock time and Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dose.time,
                    style = ElderPalTypography.heading2,
                    color = TextPrimary
                )

                DoseStatusChip(status = dose.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom line: Medicine name, dose amount, food instruction
            val foodSuffix = if (dose.takesWithFood) ", with food" else ""
            Text(
                text = "${dose.medicineName} · ${dose.dose}$foodSuffix",
                style = ElderPalTypography.body.copy(
                    fontSize = 20.sp,
                    color = TextPrimary
                )
            )

            // Button to mark dose as taken when not yet taken
            if (dose.status != DoseStatus.TAKEN) {
                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(ElderPalShapes.input)
                        .background(TealPrimaryTint)
                        .border(2.dp, TealPrimary, ElderPalShapes.input)
                        .clickable(
                            onClick = onMarkTaken,
                            role = Role.Button
                        )
                        .semantics {
                            role = Role.Button
                            contentDescription = "Mark ${dose.medicineName} dose at ${dose.time} as taken"
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
                            tint = TealPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Mark as taken",
                            style = ElderPalTypography.label.copy(
                                fontSize = 18.sp,
                                color = TealPrimary
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DoseStatusChip(status: DoseStatus) {
    val (bgColor, borderColor, textColor, iconRes, text) = when (status) {
        DoseStatus.UPCOMING -> Quintuple(
            TealPrimaryTint,
            TealPrimary,
            TealPrimary,
            R.drawable.ic_clock,
            "Upcoming"
        )
        DoseStatus.TAKEN -> Quintuple(
            SuccessTint,
            SuccessGreen,
            SuccessGreen,
            R.drawable.ic_check,
            "Taken"
        )
        DoseStatus.MISSED -> Quintuple(
            DangerRedTint,
            DangerRed,
            DangerRed,
            R.drawable.ic_close,
            "Missed"
        )
    }

    Surface(
        shape = ElderPalShapes.pill,
        color = bgColor,
        border = BorderStroke(2.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = ElderPalTypography.label.copy(
                    fontSize = 18.sp,
                    color = textColor
                )
            )
        }
    }
}

@Composable
private fun ScheduleEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Large circular badge with medicine capsule icon
        Box(
            modifier = Modifier
                .size(130.dp)
                .clip(CircleShape)
                .background(TealPrimaryTint)
                .border(3.dp, TealPrimary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_pill),
                contentDescription = null,
                tint = TealPrimary,
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "No medicine for today",
            style = ElderPalTypography.heading2,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Add your medicine and I will remind you at the right time.",
            style = ElderPalTypography.body.copy(color = TextSecondary),
            textAlign = TextAlign.Center
        )
    }
}

private data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
