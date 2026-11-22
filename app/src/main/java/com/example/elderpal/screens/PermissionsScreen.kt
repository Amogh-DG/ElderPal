// Permissions management screen displaying current device permission statuses and links to system settings.
package com.example.elderpal.screens

import android.Manifest
import android.os.Build
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.elderpal.R
import com.example.elderpal.data.PermissionManager
import com.example.elderpal.ui.theme.BackgroundWarmCream
import com.example.elderpal.ui.theme.BorderCard
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

@Composable
fun PermissionsScreen(
    onBack: () -> Unit,
    onDemoToolsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Live permission states
    var hasNotif by remember { mutableStateOf(PermissionManager.hasNotificationPermission(context)) }
    var hasCamera by remember { mutableStateOf(PermissionManager.hasCameraPermission(context)) }
    var hasSms by remember { mutableStateOf(PermissionManager.hasSmsPermission(context)) }
    var hasLocation by remember { mutableStateOf(PermissionManager.hasLocationPermission(context)) }
    var hasCall by remember { mutableStateOf(PermissionManager.hasPhoneCallPermission(context)) }
    var hasMotion by remember { mutableStateOf(PermissionManager.hasMotionSensor(context)) }
    var isBatteryUnrestricted by remember { mutableStateOf(PermissionManager.isBatteryUnrestricted(context)) }
    var canUseLockScreen by remember { mutableStateOf(PermissionManager.canUseFullScreenIntent(context)) }

    fun refreshPermissions() {
        hasNotif = PermissionManager.hasNotificationPermission(context)
        hasCamera = PermissionManager.hasCameraPermission(context)
        hasSms = PermissionManager.hasSmsPermission(context)
        hasLocation = PermissionManager.hasLocationPermission(context)
        hasCall = PermissionManager.hasPhoneCallPermission(context)
        hasMotion = PermissionManager.hasMotionSensor(context)
        isBatteryUnrestricted = PermissionManager.isBatteryUnrestricted(context)
        canUseLockScreen = PermissionManager.canUseFullScreenIntent(context)
    }

    // Refresh automatically when returning from Android System Settings
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        refreshPermissions()
    }

    // Permission Launchers
    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { refreshPermissions() }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { refreshPermissions() }

    val smsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { refreshPermissions() }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { refreshPermissions() }

    val callLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { refreshPermissions() }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundWarmCream)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Back Button matching app design
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

        Text(
            text = "Permissions",
            style = ElderPalTypography.display.copy(fontSize = 32.sp),
            color = TextPrimary
        )

        Text(
            text = "Manage safety permissions and phone settings. Tapping any item will request access or open phone settings.",
            style = ElderPalTypography.body.copy(fontSize = 18.sp),
            color = TextSecondary
        )

        // 1. Reminders (Notifications)
        PermissionStatusCard(
            title = "Reminders",
            description = "Sends notifications when your medicine is due.",
            iconRes = R.drawable.ic_bell,
            isAllowed = hasNotif,
            onClick = {
                if (!hasNotif) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        PermissionManager.openAppSettings(context)
                    }
                }
            }
        )

        // 2. Camera
        PermissionStatusCard(
            title = "Camera",
            description = "Reads prescription labels and checks your medicine.",
            iconRes = R.drawable.ic_camera,
            isAllowed = hasCamera,
            onClick = {
                if (!hasCamera) {
                    cameraLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        )

        // 3. Text messages (SMS)
        PermissionStatusCard(
            title = "Text messages",
            description = "Sends emergency alert SMS to family contacts.",
            iconRes = R.drawable.ic_chat,
            isAllowed = hasSms,
            onClick = {
                if (!hasSms) {
                    smsLauncher.launch(Manifest.permission.SEND_SMS)
                }
            }
        )

        // 4. Location
        PermissionStatusCard(
            title = "Location",
            description = "Includes your location map link in emergency alerts.",
            iconRes = R.drawable.ic_location,
            isAllowed = hasLocation,
            onClick = {
                if (!hasLocation) {
                    locationLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            }
        )

        // 5. Phone calls
        PermissionStatusCard(
            title = "Phone calls",
            description = "Calls your primary emergency contact when SOS or fall occurs.",
            iconRes = R.drawable.ic_phone,
            isAllowed = hasCall,
            onClick = {
                if (!hasCall) {
                    callLauncher.launch(Manifest.permission.CALL_PHONE)
                }
            }
        )

        // 6. Motion sensors
        PermissionStatusCard(
            title = "Motion sensors",
            description = "Uses phone accelerometer to detect suspected falls.",
            iconRes = R.drawable.ic_motion,
            isAllowed = hasMotion,
            onClick = {
                // Accelerometer is built-in hardware sensor
            }
        )

        // Section header for Advanced System Settings
        Text(
            text = "System settings",
            style = ElderPalTypography.heading2.copy(fontSize = 24.sp),
            color = TextPrimary,
            modifier = Modifier.padding(top = 8.dp)
        )

        // 7. Unrestricted battery use
        PermissionStatusCard(
            title = "Unrestricted battery use",
            description = "Stops Android from sleeping background fall detection and medicine reminders.",
            iconRes = R.drawable.ic_clock,
            isAllowed = isBatteryUnrestricted,
            onClick = {
                PermissionManager.openBatterySettings(context)
            }
        )

        // 8. Lock screen alerts
        PermissionStatusCard(
            title = "Lock screen alerts",
            description = "Wakes the phone and displays emergency alerts even when the screen is locked.",
            iconRes = R.drawable.ic_warning,
            isAllowed = canUseLockScreen,
            onClick = {
                PermissionManager.openLockScreenAlertSettings(context)
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Demo tools link button (Judges only)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(ElderPalShapes.button)
                .background(SurfaceWhite)
                .border(2.dp, BorderCard, ElderPalShapes.button)
                .clickable(onClick = onDemoToolsClick, role = Role.Button)
                .semantics {
                    role = Role.Button
                    contentDescription = "Open demo tools for judges"
                },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_warning),
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Demo tools (judges only)",
                    style = ElderPalTypography.title.copy(color = TextSecondary, fontSize = 18.sp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PermissionStatusCard(
    title: String,
    description: String,
    iconRes: Int,
    isAllowed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick, role = Role.Button),
        shape = ElderPalShapes.card,
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(2.dp, BorderCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isAllowed) TealPrimaryTint else WarningAmberTint),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = if (isAllowed) TealPrimary else WarningAmberText,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = title,
                    style = ElderPalTypography.title.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )

                // Status badge (Allowed or Not allowed)
                Surface(
                    shape = CircleShape,
                    color = if (isAllowed) SuccessTint else WarningAmberTint,
                    border = BorderStroke(2.dp, if (isAllowed) SuccessGreen else WarningAmberText)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = if (isAllowed) R.drawable.ic_check else R.drawable.ic_warning),
                            contentDescription = null,
                            tint = if (isAllowed) SuccessGreen else WarningAmberText,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isAllowed) "Allowed" else "Not allowed",
                            style = ElderPalTypography.small.copy(
                                color = if (isAllowed) SuccessGreen else WarningAmberText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                    }
                }
            }

            Text(
                text = description,
                style = ElderPalTypography.body.copy(fontSize = 17.sp, lineHeight = 24.sp),
                color = TextSecondary
            )
        }
    }
}
