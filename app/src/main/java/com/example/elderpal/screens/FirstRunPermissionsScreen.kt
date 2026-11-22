// First-run onboarding flow presenting system permission explainers sequentially on initial launch.
package com.example.elderpal.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.elderpal.R
import com.example.elderpal.data.PermissionManager
import com.example.elderpal.ui.theme.BackgroundWarmCream
import com.example.elderpal.ui.theme.BorderInput
import com.example.elderpal.ui.theme.ElderPalShapes
import com.example.elderpal.ui.theme.ElderPalTypography
import com.example.elderpal.ui.theme.TealPrimary
import com.example.elderpal.ui.theme.TealPrimaryTint
import com.example.elderpal.ui.theme.TextPrimary
import com.example.elderpal.ui.theme.TextSecondary

private const val TOTAL_STEPS = 6

@Composable
fun FirstRunPermissionsScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(1) }

    fun finishFirstRun() {
        PermissionManager.setFirstRunCompleted(context, true)
        onComplete()
    }

    fun nextStep() {
        if (currentStep < TOTAL_STEPS) {
            currentStep += 1
        } else {
            finishFirstRun()
        }
    }

    // Permission Launchers
    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            PermissionManager.setPermissionRefused(context, PermissionManager.KEY_NOTIF_REFUSED, true)
        }
        nextStep()
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            PermissionManager.setPermissionRefused(context, PermissionManager.KEY_CAMERA_REFUSED, true)
        }
        nextStep()
    }

    val smsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            PermissionManager.setPermissionRefused(context, PermissionManager.KEY_SMS_REFUSED, true)
        }
        nextStep()
    }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val anyGranted = results.values.any { it }
        if (!anyGranted) {
            PermissionManager.setPermissionRefused(context, PermissionManager.KEY_LOCATION_REFUSED, true)
        }
        nextStep()
    }

    val callLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            PermissionManager.setPermissionRefused(context, PermissionManager.KEY_CALL_REFUSED, true)
        }
        nextStep()
    }

    // Current step metadata
    val stepTitle = when (currentStep) {
        1 -> "Allow reminders"
        2 -> "Allow the camera"
        3 -> "Allow text messages"
        4 -> "Allow your location"
        5 -> "Allow phone calls"
        6 -> "Allow motion sensors"
        else -> ""
    }

    val stepDescription = when (currentStep) {
        1 -> "ElderPal reminds you when it is time to take your medicine."
        2 -> "ElderPal uses the camera to read your prescription and check your medicine."
        3 -> "ElderPal sends a text to your family if you press SOS or if you fall."
        4 -> "ElderPal shares where you are with your family only when you need help."
        5 -> "ElderPal can call your family contact directly if you need emergency help."
        6 -> "ElderPal notices if you fall, using your phone's motion sensors."
        else -> ""
    }

    val stepIcon = when (currentStep) {
        1 -> R.drawable.ic_bell
        2 -> R.drawable.ic_camera
        3 -> R.drawable.ic_chat
        4 -> R.drawable.ic_location
        5 -> R.drawable.ic_phone
        6 -> R.drawable.ic_motion
        else -> R.drawable.ic_bell
    }

    fun handleAllow() {
        when (currentStep) {
            1 -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    nextStep()
                }
            }
            2 -> cameraLauncher.launch(Manifest.permission.CAMERA)
            3 -> smsLauncher.launch(Manifest.permission.SEND_SMS)
            4 -> locationLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            5 -> callLauncher.launch(Manifest.permission.CALL_PHONE)
            6 -> {
                // Motion accelerometer does not require a runtime permission prompt on Android
                nextStep()
            }
        }
    }

    fun handleNotNow() {
        when (currentStep) {
            1 -> PermissionManager.setPermissionRefused(context, PermissionManager.KEY_NOTIF_REFUSED, true)
            2 -> PermissionManager.setPermissionRefused(context, PermissionManager.KEY_CAMERA_REFUSED, true)
            3 -> PermissionManager.setPermissionRefused(context, PermissionManager.KEY_SMS_REFUSED, true)
            4 -> PermissionManager.setPermissionRefused(context, PermissionManager.KEY_LOCATION_REFUSED, true)
            5 -> PermissionManager.setPermissionRefused(context, PermissionManager.KEY_CALL_REFUSED, true)
            6 -> PermissionManager.setPermissionRefused(context, PermissionManager.KEY_MOTION_REFUSED, true)
        }
        nextStep()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundWarmCream)
            .padding(horizontal = 20.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top: Step Counter & Dot Indicators matching design/permissions.png
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Step $currentStep of $TOTAL_STEPS",
                style = ElderPalTypography.title.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 6 Step indicator dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..TOTAL_STEPS) {
                    if (i == currentStep) {
                        // Active filled dot
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(TealPrimary)
                        )
                    } else {
                        // Inactive hollow dot
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .border(2.dp, TealPrimary, CircleShape)
                        )
                    }
                }
            }
        }

        // Center: Circular Icon Badge, Title, and Description
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(TealPrimaryTint)
                    .border(3.5.dp, TealPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = stepIcon),
                    contentDescription = null,
                    tint = TealPrimary,
                    modifier = Modifier.size(68.dp)
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            Text(
                text = stepTitle,
                style = ElderPalTypography.display.copy(fontSize = 32.sp),
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stepDescription,
                style = ElderPalTypography.body.copy(fontSize = 20.sp, lineHeight = 30.sp),
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }

        // Bottom: Large "Allow" button & "Not now" button
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(ElderPalShapes.button)
                    .background(TealPrimary)
                    .clickable(
                        onClick = { handleAllow() },
                        role = Role.Button
                    )
                    .semantics {
                        role = Role.Button
                        contentDescription = stepTitle
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Allow",
                    style = ElderPalTypography.title.copy(
                        color = Color.White,
                        fontSize = 24.sp
                    )
                )
            }

            Text(
                text = "Not now",
                style = ElderPalTypography.title.copy(
                    color = TextSecondary,
                    fontSize = 20.sp,
                    textDecoration = TextDecoration.Underline
                ),
                modifier = Modifier
                    .clickable(
                        onClick = { handleNotNow() },
                        role = Role.Button
                    )
                    .padding(8.dp)
                    .semantics {
                        role = Role.Button
                        contentDescription = "Skip $stepTitle for now"
                    }
            )
        }
    }
}
