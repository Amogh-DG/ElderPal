// Screen for confirming and dispatching an emergency SOS alert via SMS with location and initiating a call.
package com.example.elderpal.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.elderpal.R
import com.example.elderpal.data.AlertDispatchReport
import com.example.elderpal.data.ContactAlertStatus
import com.example.elderpal.data.ContactsRepository
import com.example.elderpal.data.EmergencyAlertManager
import com.example.elderpal.data.PhoneCallStatus
import com.example.elderpal.data.SmsDeliveryStatus
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class SosStep {
    CONFIRMATION,
    EXPLAIN_LOCATION,
    EXPLAIN_SMS,
    EXPLAIN_CALL,
    SENDING,
    SENT
}

@Composable
fun SosScreen(
    onBack: () -> Unit,
    onNavigateToContacts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val contactsRepository = remember { ContactsRepository(context) }
    val medicineRepository = remember { com.example.elderpal.data.MedicineRepository(context) }
    val alertManager = remember { EmergencyAlertManager(context) }
    val coroutineScope = rememberCoroutineScope()

    val contacts by contactsRepository.contacts.collectAsState(initial = emptyList())

    var currentStep by remember { mutableStateOf(SosStep.CONFIRMATION) }
    var isSending by remember { mutableStateOf(false) }
    var alertReport by remember { mutableStateOf<AlertDispatchReport?>(null) }

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasCallPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun triggerEmergencyDispatch() {
        currentStep = SosStep.SENDING
        coroutineScope.launch(Dispatchers.IO) {
            val location = alertManager.getLastKnownLocation()
            val lastTakenDose = medicineRepository.getLastTakenDoseToday()
            val report = alertManager.dispatchSosAlert(contacts, location, lastTakenDose)
            withContext(Dispatchers.Main) {
                alertReport = report
                currentStep = SosStep.SENT
                isSending = false
            }
        }
    }

    fun proceedToNextPermissionAfterSms() {
        if (!hasCallPermission()) {
            currentStep = SosStep.EXPLAIN_CALL
        } else {
            triggerEmergencyDispatch()
        }
    }

    fun proceedToNextPermissionAfterLocation() {
        if (!hasSmsPermission()) {
            currentStep = SosStep.EXPLAIN_SMS
        } else {
            proceedToNextPermissionAfterSms()
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        proceedToNextPermissionAfterLocation()
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        proceedToNextPermissionAfterSms()
    }

    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        triggerEmergencyDispatch()
    }

    // Permission explainer dialogs
    when (currentStep) {
        SosStep.EXPLAIN_LOCATION -> {
            LocationPermissionExplainer(
                onAllowClick = {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                onDismissClick = {
                    proceedToNextPermissionAfterLocation()
                }
            )
        }
        SosStep.EXPLAIN_SMS -> {
            SmsPermissionExplainer(
                onAllowClick = {
                    smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                },
                onDismissClick = {
                    proceedToNextPermissionAfterSms()
                }
            )
        }
        SosStep.EXPLAIN_CALL -> {
            PhoneCallPermissionExplainer(
                onAllowClick = {
                    callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                },
                onDismissClick = {
                    triggerEmergencyDispatch()
                }
            )
        }
        else -> Unit
    }

    // Main screen views depending on current step
    when (currentStep) {
        SosStep.CONFIRMATION,
        SosStep.EXPLAIN_LOCATION,
        SosStep.EXPLAIN_SMS,
        SosStep.EXPLAIN_CALL -> {
            SosConfirmationView(
                hasContacts = contacts.isNotEmpty(),
                isSending = isSending,
                onSendClick = {
                    if (isSending) return@SosConfirmationView
                    isSending = true
                    if (contacts.isEmpty()) {
                        isSending = false
                        return@SosConfirmationView
                    }
                    if (!hasLocationPermission()) {
                        currentStep = SosStep.EXPLAIN_LOCATION
                    } else {
                        proceedToNextPermissionAfterLocation()
                    }
                },
                onCancelClick = onBack,
                onAddContactsClick = onNavigateToContacts,
                modifier = modifier
            )
        }
        SosStep.SENDING -> {
            SosSendingView(modifier = modifier)
        }
        SosStep.SENT -> {
            alertReport?.let { report ->
                EmergencyAlertSentView(
                    report = report,
                    onCallAgainClick = {
                        val primary = report.primaryContact
                        if (primary != null) {
                            val normalized = EmergencyAlertManager.normalizePhoneNumber(primary.phoneNumber)
                            if (hasCallPermission()) {
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

// Confirmation state matching design/sos.png
@Composable
private fun SosConfirmationView(
    hasContacts: Boolean,
    isSending: Boolean,
    onSendClick: () -> Unit,
    onCancelClick: () -> Unit,
    onAddContactsClick: () -> Unit,
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
        Spacer(modifier = Modifier.height(20.dp))

        // Center content: SOS badge, headline, and subtext
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            // Concentric circular SOS badge
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(DangerRedTint),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(DangerRed),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_sos_beacon),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(60.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Send emergency\nalert to your family?",
                style = ElderPalTypography.display.copy(fontSize = 32.sp, lineHeight = 40.sp),
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "They will get a text with your\nlocation.",
                style = ElderPalTypography.body.copy(fontSize = 20.sp, lineHeight = 30.sp),
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            // Warning if no contacts have been saved yet
            if (!hasContacts) {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ElderPalShapes.card,
                    colors = CardDefaults.cardColors(containerColor = DangerRedTint),
                    border = BorderStroke(2.dp, DangerRed)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No emergency contacts saved yet.",
                            style = ElderPalTypography.title.copy(
                                fontSize = 20.sp,
                                color = DangerRed,
                                fontWeight = FontWeight.Bold
                            ),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Please add a family contact so ElderPal knows who to alert.",
                            style = ElderPalTypography.body.copy(
                                fontSize = 18.sp,
                                color = TextPrimary
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Bottom Action Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!hasContacts) {
                // Button to jump to contacts screen
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_add),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Add emergency contact",
                            style = ElderPalTypography.title.copy(
                                color = Color.White,
                                fontSize = 22.sp
                            )
                        )
                    }
                }
            } else {
                // Extra Large "Send alert" button (112dp tall) matching design/sos.png
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(112.dp)
                        .clip(ElderPalShapes.button)
                        .background(if (isSending) DangerRed.copy(alpha = 0.6f) else DangerRed)
                        .clickable(
                            enabled = !isSending,
                            onClick = onSendClick,
                            role = Role.Button
                        )
                        .semantics {
                            role = Role.Button
                            contentDescription = "Send emergency alert to your family"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_sos_beacon),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Send alert",
                            style = ElderPalTypography.display.copy(
                                color = Color.White,
                                fontSize = 32.sp
                            )
                        )
                    }
                }
            }

            // "Cancel" button (72dp tall) matching design/sos.png
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(ElderPalShapes.button)
                    .background(SurfaceWhite)
                    .border(3.dp, BorderInput, ElderPalShapes.button)
                    .clickable(
                        onClick = onCancelClick,
                        role = Role.Button
                    )
                    .semantics {
                        role = Role.Button
                        contentDescription = "Cancel alert"
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Cancel",
                        style = ElderPalTypography.title.copy(
                            color = TextPrimary,
                            fontSize = 24.sp
                        )
                    )
                }
            }
        }
    }
}

// Intermediate active dispatch state based on check-medicine-loading.png
@Composable
private fun SosSendingView(modifier: Modifier = Modifier) {
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
                .clip(CircleShape)
                .background(TealPrimaryTint)
                .border(3.dp, TealPrimary, CircleShape),
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
            text = "Alerting your family...",
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
