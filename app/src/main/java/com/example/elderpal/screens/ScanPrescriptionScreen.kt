// Screen for scanning a doctor's prescription using CameraX and Gemini API to extract medicines and schedules.
package com.example.elderpal.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.elderpal.BuildConfig
import com.example.elderpal.R
import com.example.elderpal.ai.MedicineLabelChecker
import com.example.elderpal.data.DoseReminderScheduler
import com.example.elderpal.data.Medicine
import com.example.elderpal.data.MedicineRepository
import com.example.elderpal.data.PermissionManager
import com.example.elderpal.ui.theme.BackgroundWarmCream
import com.example.elderpal.ui.theme.BorderCard
import com.example.elderpal.ui.theme.BorderInput
import com.example.elderpal.ui.theme.CameraDark
import com.example.elderpal.ui.theme.DangerRed
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
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.UUID

// Stages of the prescription scan workflow
private enum class ScanStage {
    CAMERA,
    LOADING,
    REVIEW,
    ERROR
}

// Editable in-memory model for each scanned medicine item
data class ScannedMedicine(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var dose: String,
    var timesPerDay: Int,
    var doseTimes: List<String>,
    var takesWithFood: Boolean,
    var isLowConfidence: Boolean = false
)

private sealed interface ScanResult {
    data class Success(val medicines: List<ScannedMedicine>) : ScanResult
    data class Error(val message: String) : ScanResult
}

// Sensible default clock times based on dose frequency per day
private fun defaultTimesFor(timesPerDay: Int): List<String> {
    return when (timesPerDay) {
        1 -> listOf("8:00 AM")
        2 -> listOf("8:00 AM", "8:00 PM")
        3 -> listOf("8:00 AM", "2:00 PM", "8:00 PM")
        4 -> listOf("8:00 AM", "12:00 PM", "4:00 PM", "8:00 PM")
        else -> listOf("8:00 AM")
    }
}

// Checks for active internet connectivity
private fun isDeviceOnline(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
           caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

@Composable
fun ScanPrescriptionScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit = onBack,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { MedicineRepository(context) }

    var scanStage by remember { mutableStateOf(ScanStage.CAMERA) }
    var errorMessage by remember { mutableStateOf("") }
    var medicines by remember { mutableStateOf<List<ScannedMedicine>>(emptyList()) }
    var editingMedicine by remember { mutableStateOf<ScannedMedicine?>(null) }
    var activeScanJob by remember { mutableStateOf<Job?>(null) }

    // In-memory photo state only (never written to disk)
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Camera permission states
    var hasCameraPermission by remember {
        mutableStateOf(PermissionManager.hasCameraPermission(context))
    }
    var showPermissionExplainer by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            PermissionManager.setPermissionRefused(context, PermissionManager.KEY_CAMERA_REFUSED, true)
        }
    }

    val cameraController = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
        }
    }

    LaunchedEffect(hasCameraPermission) {
        if (!hasCameraPermission) {
            if (!PermissionManager.isPermissionRefused(context, PermissionManager.KEY_CAMERA_REFUSED)) {
                showPermissionExplainer = true
            }
        } else {
            cameraController.bindToLifecycle(lifecycleOwner)
        }
    }

    // Clean up in-memory bitmap on screen dispose
    DisposableEffect(Unit) {
        onDispose {
            activeScanJob?.cancel()
            capturedBitmap?.recycle()
            capturedBitmap = null
        }
    }

    if (showPermissionExplainer) {
        CameraPermissionExplainer(
            onAllowClick = {
                showPermissionExplainer = false
                permissionLauncher.launch(Manifest.permission.CAMERA)
            },
            onDismissClick = {
                showPermissionExplainer = false
                PermissionManager.setPermissionRefused(context, PermissionManager.KEY_CAMERA_REFUSED, true)
            }
        )
    }

    when (scanStage) {
        ScanStage.CAMERA -> {
            CameraScanView(
                cameraController = cameraController,
                hasCameraPermission = hasCameraPermission,
                onBack = onBack,
                onEnableCameraClick = { showPermissionExplainer = true },
                onCapturePhoto = { bitmap ->
                    capturedBitmap = bitmap
                    scanStage = ScanStage.LOADING
                    activeScanJob = coroutineScope.launch {
                        val result = performPrescriptionScan(context, bitmap)
                        when (result) {
                            is ScanResult.Success -> {
                                medicines = result.medicines
                                scanStage = ScanStage.REVIEW
                            }
                            is ScanResult.Error -> {
                                errorMessage = result.message
                                scanStage = ScanStage.ERROR
                            }
                        }
                    }
                },
                modifier = modifier
            )
        }

        ScanStage.LOADING -> {
            LoadingScanView(
                onCancel = {
                    activeScanJob?.cancel()
                    activeScanJob = null
                    capturedBitmap?.recycle()
                    capturedBitmap = null
                    scanStage = ScanStage.CAMERA
                },
                modifier = modifier
            )
        }

        ScanStage.ERROR -> {
            ErrorScanView(
                message = errorMessage,
                onTryAgain = {
                    capturedBitmap?.recycle()
                    capturedBitmap = null
                    scanStage = ScanStage.CAMERA
                },
                onGoBack = onBack,
                modifier = modifier
            )
        }

        ScanStage.REVIEW -> {
            ReviewScanView(
                medicines = medicines,
                onBack = {
                    // Discard and return to camera
                    capturedBitmap?.recycle()
                    capturedBitmap = null
                    scanStage = ScanStage.CAMERA
                },
                onEditClick = { med ->
                    editingMedicine = med.copy(doseTimes = med.doseTimes.toList())
                },
                onDoseChangedInline = { med, newDose ->
                    medicines = medicines.map { if (it.id == med.id) it.copy(dose = newDose) else it }
                },
                onLooksRight = {
                    val valid = medicines.filter { it.name.trim().isNotBlank() }
                    if (valid.isNotEmpty()) {
                        coroutineScope.launch {
                            for (item in valid) {
                                val resolvedDose = item.dose.ifBlank { "1 tablet" }.trim()
                                val newMed = Medicine(
                                    name = item.name.trim(),
                                    dose = resolvedDose,
                                    timesPerDay = item.timesPerDay,
                                    doseTimes = if (item.doseTimes.isNotEmpty()) item.doseTimes else defaultTimesFor(item.timesPerDay),
                                    takesWithFood = item.takesWithFood
                                )
                                repository.saveMedicine(newMed)
                            }
                            DoseReminderScheduler.rescheduleAll(context)
                            capturedBitmap?.recycle()
                            capturedBitmap = null
                            onSaved()
                        }
                    }
                },
                modifier = modifier
            )
        }
    }

    // Modal dialog for editing a single medicine entry
    editingMedicine?.let { target ->
        EditScannedMedicineDialog(
            initialMedicine = target,
            onDismiss = { editingMedicine = null },
            onSave = { updated ->
                medicines = medicines.map { if (it.id == updated.id) updated else it }
                editingMedicine = null
            },
            onDelete = { deletedId ->
                medicines = medicines.filterNot { it.id == deletedId }
                editingMedicine = null
            }
        )
    }
}

// ── 1. Camera View ────────────────────────────────────────────────────────────
@Composable
private fun CameraScanView(
    cameraController: LifecycleCameraController,
    hasCameraPermission: Boolean,
    onBack: () -> Unit,
    onEnableCameraClick: () -> Unit,
    onCapturePhoto: (Bitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CameraDark)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top section: Back button & Title matching design/scan-prescription.png
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .height(60.dp)
                    .clip(ElderPalShapes.button)
                    .background(Color.Transparent)
                    .border(2.dp, Color.White, ElderPalShapes.button)
                    .clickable(onClick = onBack, role = Role.Button)
                    .semantics {
                        role = Role.Button
                        contentDescription = "Go back"
                    }
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_back),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Back",
                        style = ElderPalTypography.title.copy(color = Color.White, fontSize = 22.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Put your prescription\ninside the box",
                style = ElderPalTypography.display.copy(
                    color = Color.White,
                    fontSize = 32.sp,
                    lineHeight = 38.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        // Center Viewfinder matching prescription aspect ratio
        if (!hasCameraPermission) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.82f),
                shape = ElderPalShapes.card,
                color = SurfaceWhite
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_camera),
                        contentDescription = null,
                        tint = TealPrimary,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Camera access needed",
                        style = ElderPalTypography.heading2,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ElderPal needs camera access to read and scan your prescription.",
                        style = ElderPalTypography.body,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clip(ElderPalShapes.button)
                            .background(TealPrimary)
                            .clickable(onClick = onEnableCameraClick, role = Role.Button),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Enable camera",
                            style = ElderPalTypography.title.copy(color = Color.White, fontSize = 20.sp)
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.82f)
                    .clip(ElderPalShapes.card)
                    .background(Color(0xFF2B2520)),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            controller = cameraController
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Four corner framing guides
                PrescriptionFramingBracketsOverlay()
            }
        }

        // Caption text below viewfinder
        Text(
            text = "Keep it flat and in good light.",
            style = ElderPalTypography.body.copy(color = Color.White, fontSize = 20.sp),
            textAlign = TextAlign.Center
        )

        // Bottom Controls: Take photo button
        if (hasCameraPermission) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clip(CircleShape)
                        .border(5.dp, Color.White, CircleShape)
                        .padding(6.dp)
                        .clickable(
                            onClick = {
                                cameraController.takePicture(
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(image: ImageProxy) {
                                            val original = image.toBitmap()
                                            val rotation = image.imageInfo.rotationDegrees
                                            val matrix = Matrix().apply {
                                                postRotate(rotation.toFloat())
                                            }
                                            val rotated = Bitmap.createBitmap(
                                                original,
                                                0,
                                                0,
                                                original.width,
                                                original.height,
                                                matrix,
                                                true
                                            )
                                            image.close()
                                            onCapturePhoto(rotated)
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            // Handle gracefully
                                        }
                                    }
                                )
                            },
                            role = Role.Button
                        )
                        .semantics {
                            role = Role.Button
                            contentDescription = "Take photo of prescription"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Take photo",
                    style = ElderPalTypography.label.copy(
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        } else {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// ── 2. Loading View ───────────────────────────────────────────────────────────
@Composable
private fun LoadingScanView(
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundWarmCream)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // Center circular loading indicator matching design/scan-prescription-loading.png
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(190.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.fillMaxSize(),
                    color = TealPrimary,
                    trackColor = TealPrimaryTint,
                    strokeWidth = 12.dp,
                    strokeCap = StrokeCap.Round
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_prescription),
                    contentDescription = null,
                    tint = TealPrimary,
                    modifier = Modifier.size(68.dp)
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            Text(
                text = "Reading your\nprescription",
                style = ElderPalTypography.display.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 38.sp
                ),
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "This takes a few seconds. Please\nkeep the app open.",
                style = ElderPalTypography.body.copy(
                    fontSize = 20.sp,
                    lineHeight = 28.sp
                ),
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }

        // Cancel button at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(ElderPalShapes.button)
                .background(SurfaceWhite)
                .border(2.5.dp, BorderCard, ElderPalShapes.button)
                .clickable(onClick = onCancel, role = Role.Button)
                .semantics {
                    role = Role.Button
                    contentDescription = "Cancel prescription scanning"
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Cancel",
                style = ElderPalTypography.title.copy(
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

// ── 3. Error View ─────────────────────────────────────────────────────────────
@Composable
private fun ErrorScanView(
    message: String,
    onTryAgain: () -> Unit,
    onGoBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundWarmCream)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(30.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(WarningAmberTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_warning),
                    contentDescription = null,
                    tint = WarningAmberText,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Couldn't read prescription",
                style = ElderPalTypography.display.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = message,
                style = ElderPalTypography.body.copy(
                    fontSize = 20.sp,
                    lineHeight = 28.sp
                ),
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
                    .clickable(onClick = onTryAgain, role = Role.Button)
                    .semantics {
                        role = Role.Button
                        contentDescription = "Try scanning again"
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Try again",
                    style = ElderPalTypography.title.copy(color = Color.White, fontSize = 22.sp)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(ElderPalShapes.button)
                    .background(SurfaceWhite)
                    .border(2.5.dp, BorderCard, ElderPalShapes.button)
                    .clickable(onClick = onGoBack, role = Role.Button)
                    .semantics {
                        role = Role.Button
                        contentDescription = "Go back"
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Back",
                    style = ElderPalTypography.title.copy(color = TextPrimary, fontSize = 22.sp)
                )
            }
        }
    }
}

// ── 4. Review View ────────────────────────────────────────────────────────────
@Composable
private fun ReviewScanView(
    medicines: List<ScannedMedicine>,
    onBack: () -> Unit,
    onEditClick: (ScannedMedicine) -> Unit,
    onDoseChangedInline: (ScannedMedicine, String) -> Unit,
    onLooksRight: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val hasLowConfidence = medicines.any { it.isLowConfidence }

    val subtitle = when {
        medicines.isEmpty() -> "No medicines detected."
        hasLowConfidence && medicines.size == 1 -> "I found 1 medicine. Please check it."
        hasLowConfidence -> "I found ${medicines.size} medicines. Please check the highlighted one."
        medicines.size == 1 -> "I found 1 medicine. Tap to change it."
        else -> "I found ${medicines.size} medicines. Tap one to change it."
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundWarmCream)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Back button matching design/scan-prescription-review.png
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
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Back",
                    style = ElderPalTypography.title.copy(color = TextPrimary, fontSize = 20.sp)
                )
            }
        }

        // Title and Subtitle
        Text(
            text = "Is this right?",
            style = ElderPalTypography.display.copy(
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            ),
            color = TextPrimary
        )

        Text(
            text = subtitle,
            style = ElderPalTypography.body.copy(
                fontSize = 20.sp,
                lineHeight = 28.sp
            ),
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Cards list
        medicines.forEach { item ->
            if (item.isLowConfidence) {
                // Low-confidence state matching design/scan-prescription-low-confidence.png
                LowConfidenceMedicineCard(
                    item = item,
                    onEditClick = { onEditClick(item) },
                    onDoseChange = { newDose -> onDoseChangedInline(item, newDose) }
                )
            } else {
                // Normal review card matching design/scan-prescription-review.png
                NormalMedicineCard(
                    item = item,
                    onEditClick = { onEditClick(item) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // "Looks right" primary button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(ElderPalShapes.button)
                .background(SuccessGreen)
                .clickable(
                    enabled = medicines.any { it.name.trim().isNotBlank() },
                    onClick = onLooksRight,
                    role = Role.Button
                )
                .semantics {
                    role = Role.Button
                    contentDescription = "Looks right - save all confirmed medicines"
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
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Looks right",
                    style = ElderPalTypography.title.copy(
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ── Normal Card ───────────────────────────────────────────────────────────────
@Composable
private fun NormalMedicineCard(
    item: ScannedMedicine,
    onEditClick: () -> Unit
) {
    val frequencyLabel = when (item.timesPerDay) {
        1 -> "once a day"
        2 -> "twice a day"
        3 -> "3 times a day"
        4 -> "4 times a day"
        else -> "${item.timesPerDay} times a day"
    }
    val foodLabel = if (item.takesWithFood) " · with food" else ""
    val description = "${item.dose.ifBlank { "1 tablet" }} · $frequencyLabel$foodLabel"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEditClick, role = Role.Button),
        shape = ElderPalShapes.card,
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(2.dp, BorderCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
            ) {
                Text(
                    text = item.name.ifBlank { "Unlabelled medicine" },
                    style = ElderPalTypography.title.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = ElderPalTypography.body.copy(fontSize = 18.sp),
                    color = TextSecondary
                )
            }

            // Edit button circle
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(TealPrimaryTint)
                    .clickable(onClick = onEditClick, role = Role.Button)
                    .semantics {
                        role = Role.Button
                        contentDescription = "Edit ${item.name}"
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_edit),
                    contentDescription = null,
                    tint = TealPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// ── Low-Confidence Card ───────────────────────────────────────────────────────
@Composable
private fun LowConfidenceMedicineCard(
    item: ScannedMedicine,
    onEditClick: () -> Unit,
    onDoseChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ElderPalShapes.card,
        colors = CardDefaults.cardColors(containerColor = WarningAmberTint),
        border = BorderStroke(2.5.dp, WarningAmberText)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // "Please check" pill badge
            Surface(
                shape = CircleShape,
                color = SurfaceWhite,
                border = BorderStroke(1.5.dp, WarningAmberText)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_warning),
                        contentDescription = null,
                        tint = WarningAmberText,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Please check",
                        style = ElderPalTypography.small.copy(
                            color = WarningAmberText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.name.ifBlank { "Uncertain medicine" },
                    style = ElderPalTypography.title.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )

                // Edit button circle
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SurfaceWhite)
                        .border(1.5.dp, WarningAmberText, CircleShape)
                        .clickable(onClick = onEditClick, role = Role.Button)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Edit medicine details"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_edit),
                        contentDescription = null,
                        tint = WarningAmberText,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Text(
                text = "I couldn't read this part. Please check it.",
                style = ElderPalTypography.body.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = WarningAmberText
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Inline Dose field matching design/scan-prescription-low-confidence.png
            Text(
                text = "Dose",
                style = ElderPalTypography.title.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = TextPrimary
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceWhite)
                    .border(2.dp, WarningAmberText, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = item.dose,
                    onValueChange = onDoseChange,
                    textStyle = ElderPalTypography.body.copy(
                        fontSize = 20.sp,
                        color = TextPrimary
                    ),
                    cursorBrush = SolidColor(TealPrimary),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        if (item.dose.isBlank()) {
                            Text(
                                text = "Type the dose",
                                style = ElderPalTypography.body.copy(
                                    fontSize = 18.sp,
                                    color = TextSecondary
                                )
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }
    }
}

// ── Edit Dialog ───────────────────────────────────────────────────────────────
@Composable
private fun EditScannedMedicineDialog(
    initialMedicine: ScannedMedicine,
    onDismiss: () -> Unit,
    onSave: (ScannedMedicine) -> Unit,
    onDelete: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialMedicine.name) }
    var dose by remember { mutableStateOf(initialMedicine.dose) }
    var timesPerDay by remember { mutableIntStateOf(initialMedicine.timesPerDay) }
    var takesWithFood by remember { mutableStateOf(initialMedicine.takesWithFood) }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit medicine",
                style = ElderPalTypography.title.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Name input
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Medicine name",
                        style = ElderPalTypography.label.copy(fontSize = 17.sp),
                        color = TextPrimary
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .clip(ElderPalShapes.input)
                            .background(SurfaceWhite)
                            .border(2.dp, BorderInput, ElderPalShapes.input)
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = name,
                            onValueChange = { name = it },
                            textStyle = ElderPalTypography.body.copy(fontSize = 18.sp, color = TextPrimary),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Dose input
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Dose",
                        style = ElderPalTypography.label.copy(fontSize = 17.sp),
                        color = TextPrimary
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .clip(ElderPalShapes.input)
                            .background(SurfaceWhite)
                            .border(2.dp, BorderInput, ElderPalShapes.input)
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = dose,
                            onValueChange = { dose = it },
                            textStyle = ElderPalTypography.body.copy(fontSize = 18.sp, color = TextPrimary),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { inner ->
                                if (dose.isBlank()) {
                                    Text(
                                        text = "e.g. 1 tablet or 500 mg",
                                        style = ElderPalTypography.body.copy(fontSize = 17.sp, color = TextSecondary)
                                    )
                                }
                                inner()
                            }
                        )
                    }
                }

                // Times per day buttons
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "How many times a day?",
                        style = ElderPalTypography.label.copy(fontSize = 17.sp),
                        color = TextPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        (1..4).forEach { count ->
                            val isSelected = timesPerDay == count
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .clip(ElderPalShapes.button)
                                    .background(if (isSelected) TealPrimary else SurfaceWhite)
                                    .border(2.dp, if (isSelected) TealPrimary else BorderInput, ElderPalShapes.button)
                                    .clickable(onClick = { timesPerDay = count }, role = Role.Button),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$count",
                                    style = ElderPalTypography.title.copy(
                                        color = if (isSelected) Color.White else TextPrimary,
                                        fontSize = 20.sp
                                    )
                                )
                            }
                        }
                    }
                }

                // Food instructions
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Food instructions",
                        style = ElderPalTypography.label.copy(fontSize = 17.sp),
                        color = TextPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(ElderPalShapes.button)
                                .background(if (takesWithFood) TealPrimary else SurfaceWhite)
                                .border(2.dp, if (takesWithFood) TealPrimary else BorderInput, ElderPalShapes.button)
                                .clickable(onClick = { takesWithFood = true }, role = Role.Button),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "With food",
                                style = ElderPalTypography.title.copy(
                                    color = if (takesWithFood) Color.White else TextPrimary,
                                    fontSize = 18.sp
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(ElderPalShapes.button)
                                .background(if (!takesWithFood) TealPrimary else SurfaceWhite)
                                .border(2.dp, if (!takesWithFood) TealPrimary else BorderInput, ElderPalShapes.button)
                                .clickable(onClick = { takesWithFood = false }, role = Role.Button),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No food",
                                style = ElderPalTypography.title.copy(
                                    color = if (!takesWithFood) Color.White else TextPrimary,
                                    fontSize = 18.sp
                                )
                            )
                        }
                    }
                }

                // Default dose times preview
                val calculatedTimes = defaultTimesFor(timesPerDay)
                Text(
                    text = "Default dose times: ${calculatedTimes.joinToString(", ")}",
                    style = ElderPalTypography.small.copy(fontSize = 15.sp),
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Delete / Remove button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = { onDelete(initialMedicine.id) }, role = Role.Button)
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_delete),
                        contentDescription = null,
                        tint = DangerRed,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Remove this medicine",
                        style = ElderPalTypography.body.copy(
                            color = DangerRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        initialMedicine.copy(
                            name = name.trim(),
                            dose = dose.ifBlank { "1 tablet" }.trim(),
                            timesPerDay = timesPerDay,
                            doseTimes = defaultTimesFor(timesPerDay),
                            takesWithFood = takesWithFood,
                            isLowConfidence = false
                        )
                    )
                }
            ) {
                Text(
                    text = "Done",
                    style = ElderPalTypography.title.copy(fontSize = 20.sp),
                    color = TealPrimary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    style = ElderPalTypography.title.copy(fontSize = 18.sp),
                    color = TextSecondary
                )
            }
        },
        containerColor = SurfaceWhite,
        shape = ElderPalShapes.card
    )
}

// ── Framing overlay ───────────────────────────────────────────────────────────
@Composable
private fun PrescriptionFramingBracketsOverlay(
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        val stroke = 4.dp.toPx()
        val length = 44.dp.toPx()
        val radius = 18.dp.toPx()
        val strokeColor = Color.White

        // Top-Left corner bracket
        drawPath(
            path = Path().apply {
                moveTo(0f, length)
                lineTo(0f, radius)
                quadraticTo(0f, 0f, radius, 0f)
                lineTo(length, 0f)
            },
            color = strokeColor,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Top-Right corner bracket
        drawPath(
            path = Path().apply {
                moveTo(size.width - length, 0f)
                lineTo(size.width - radius, 0f)
                quadraticTo(size.width, 0f, size.width, radius)
                lineTo(size.width, length)
            },
            color = strokeColor,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Bottom-Left corner bracket
        drawPath(
            path = Path().apply {
                moveTo(0f, size.height - length)
                lineTo(0f, size.height - radius)
                quadraticTo(0f, size.height, radius, size.height)
                lineTo(length, size.height)
            },
            color = strokeColor,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Bottom-Right corner bracket
        drawPath(
            path = Path().apply {
                moveTo(size.width - length, size.height)
                lineTo(size.width - radius, size.height)
                quadraticTo(size.width, size.height, size.width, size.height - radius)
                lineTo(size.width, size.height - length)
            },
            color = strokeColor,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

// ── Gemini API call & Retry Logic ─────────────────────────────────────────────
private suspend fun performPrescriptionScan(
    context: Context,
    bitmap: Bitmap
): ScanResult = withContext(Dispatchers.IO) {
    val apiKey = BuildConfig.GEMINI_API_KEY.trim()
    if (apiKey.isBlank() || apiKey.startsWith("YOUR_", ignoreCase = true) || apiKey.startsWith("your_", ignoreCase = true)) {
        return@withContext ScanResult.Error("API key not configured in local.properties")
    }

    if (!isDeviceOnline(context)) {
        return@withContext ScanResult.Error(
            "No internet connection. Please connect to Wi-Fi or mobile data and try again."
        )
    }

    val prompt = buildString {
        appendLine("You are an assistive health helper for an elderly person.")
        appendLine("Read the doctor's prescription shown in the photo and extract the list of prescribed medicines.")
        appendLine("Read ONLY legible printed text. Never guess medicine names, doses, or instructions from unreadable scribbles or blurry text.")
        appendLine()
        appendLine("Respond with ONLY a raw JSON object (no markdown, no backticks, no code fences) with a single key \"medicines\" containing an array of objects:")
        appendLine("{")
        appendLine("  \"medicines\": [")
        appendLine("    {")
        appendLine("      \"name\": string (the drug or medicine name, e.g. \"Metformin\", or \"\" if unreadable),")
        appendLine("      \"dose\": string (the dosage, e.g. \"500 mg\" or \"1 tablet\", or \"\" if unreadable),")
        appendLine("      \"times_per_day\": integer (1, 2, 3, or 4. Default to 1 if unspecified),")
        appendLine("      \"takes_with_food\": boolean (true if instructions indicate taking with meals or food, false otherwise),")
        appendLine("      \"is_confident\": boolean (true if the medicine name and dose are clearly legible; false if handwriting or printed text is cut off, smudged, blurry, or ambiguous)")
        appendLine("    }")
        appendLine("  ]")
        appendLine("}")
        appendLine()
        appendLine("Rules:")
        appendLine("1. Extract every distinct medicine line item found on the prescription.")
        appendLine("2. If a medicine line is partly illegible or you are unsure about the name or dose, set \"is_confident\": false.")
        appendLine("3. If the image is blank, blurry, not a prescription, or completely unreadable, return {\"medicines\": []}.")
    }

    val generativeModel = GenerativeModel(
        modelName = MedicineLabelChecker.GEMINI_MODEL_NAME,
        apiKey = apiKey,
        generationConfig = generationConfig {
            responseMimeType = "application/json"
            temperature = 0.1f
        }
    )

    val inputContent = content {
        image(bitmap)
        text(prompt)
    }

    val retryablePatterns = listOf("429", "503", "rate", "quota", "overload", "unavailable")

    repeat(MedicineLabelChecker.MAX_RETRIES) { attempt ->
        try {
            val response = withTimeoutOrNull(25_000L) {
                generativeModel.generateContent(inputContent)
            }

            if (response == null || response.text.isNullOrBlank()) {
                throw IOException("Empty response")
            }

            val cleanJson = response.text!!.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val array = if (cleanJson.startsWith("[")) {
                JSONArray(cleanJson)
            } else {
                val jsonObject = JSONObject(cleanJson)
                jsonObject.optJSONArray("medicines") ?: jsonObject.optJSONArray("prescriptions") ?: JSONArray()
            }

            val extractedMedicines = mutableListOf<ScannedMedicine>()
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val name = item.optString("name", "").trim()
                val dose = item.optString("dose", "").trim()
                val times = item.optInt("times_per_day", 1).coerceIn(1, 4)
                val withFood = item.optBoolean("takes_with_food", false)
                val isConfident = item.optBoolean("is_confident", true)

                val isLowConf = !isConfident || name.isBlank() || dose.isBlank()
                extractedMedicines.add(
                    ScannedMedicine(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        dose = dose,
                        timesPerDay = times,
                        doseTimes = defaultTimesFor(times),
                        takesWithFood = withFood,
                        isLowConfidence = isLowConf
                    )
                )
            }

            if (extractedMedicines.isEmpty()) {
                return@withContext ScanResult.Error(
                    "The prescription could not be read clearly. Please keep it flat in good light and try again, or add medicines by hand."
                )
            }

            return@withContext ScanResult.Success(extractedMedicines)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            val msg = (e.message ?: "").lowercase()
            val isRetryable = retryablePatterns.any { msg.contains(it) }

            if (isRetryable && attempt < MedicineLabelChecker.MAX_RETRIES - 1) {
                delay(MedicineLabelChecker.RETRY_DELAYS_MS[attempt])
                if (!isDeviceOnline(context)) {
                    return@withContext ScanResult.Error(
                        "No internet connection. Please connect to Wi-Fi or mobile data and try again."
                    )
                }
            } else {
                if (isRetryable) {
                    return@withContext ScanResult.Error(
                        "The prescription reader is busy right now. Please try again in a moment."
                    )
                } else {
                    return@withContext ScanResult.Error(
                        "The prescription could not be read clearly. Please keep it flat in good light and try again, or add medicines by hand."
                    )
                }
            }
        }
    }

    return@withContext ScanResult.Error(
        "The prescription reader is busy right now. Please try again in a moment."
    )
}
