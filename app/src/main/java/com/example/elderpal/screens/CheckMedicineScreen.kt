// Screen for capturing a photo of a medicine label using CameraX with in-memory preview and review.
package com.example.elderpal.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.elderpal.R
import com.example.elderpal.ui.theme.CameraDark
import com.example.elderpal.ui.theme.ElderPalShapes
import com.example.elderpal.ui.theme.ElderPalTypography
import com.example.elderpal.ui.theme.SurfaceWhite
import com.example.elderpal.ui.theme.TealPrimary
import com.example.elderpal.ui.theme.TealPrimaryTint
import com.example.elderpal.ui.theme.TextPrimary
import com.example.elderpal.ui.theme.TextSecondary

@Composable
fun CheckMedicineScreen(
    onBack: () -> Unit,
    onUsePhotoClick: (Bitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var showPermissionExplainer by remember { mutableStateOf(false) }
    var permissionDeniedPermanently by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            permissionDeniedPermanently = true
        }
    }

    // In-memory photo state only (never written permanently to disk)
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // CameraX controller
    val cameraController = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
        }
    }

    LaunchedEffect(hasCameraPermission) {
        if (!hasCameraPermission && !permissionDeniedPermanently) {
            showPermissionExplainer = true
        } else if (hasCameraPermission) {
            cameraController.bindToLifecycle(lifecycleOwner)
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
                permissionDeniedPermanently = true
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CameraDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top section: Dark Back button and screen heading
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .height(64.dp)
                    .clip(ElderPalShapes.button)
                    .background(Color.Transparent)
                    .border(2.dp, Color.White, ElderPalShapes.button)
                    .clickable(
                        onClick = onBack,
                        role = Role.Button
                    )
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
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Back",
                        style = ElderPalTypography.title.copy(color = Color.White)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Show the medicine\nlabel",
                style = ElderPalTypography.display.copy(
                    color = Color.White,
                    fontSize = 32.sp,
                    lineHeight = 38.sp
                )
            )
        }

        // Center Viewfinder: Camera Preview or Captured Photo
        if (!hasCameraPermission) {
            CameraPermissionDeniedState(
                onEnableClick = {
                    showPermissionExplainer = true
                }
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(ElderPalShapes.card)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (capturedBitmap == null) {
                    // Live CameraX preview
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                controller = cameraController
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Four corner framing guides matching design/check-medicine.png
                    FramingBracketsOverlay()
                } else {
                    // In-memory captured photo preview
                    Image(
                        bitmap = capturedBitmap!!.asImageBitmap(),
                        contentDescription = "Captured medicine label photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        // Caption text
        Text(
            text = if (capturedBitmap == null) {
                "Hold it steady. The writing should be\neasy to see."
            } else {
                "Is the writing clear and easy to read?"
            },
            style = ElderPalTypography.body.copy(color = Color.White),
            textAlign = TextAlign.Center
        )

        // Bottom Controls: Take photo or Review actions
        if (!hasCameraPermission) {
            Spacer(modifier = Modifier.height(30.dp))
        } else if (capturedBitmap == null) {
            // Camera Shutter Button matching design/check-medicine.png
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
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
                                            capturedBitmap = rotated
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            // Handled gracefully
                                        }
                                    }
                                )
                            },
                            role = Role.Button
                        )
                        .semantics {
                            role = Role.Button
                            contentDescription = "Take photo"
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
                        fontSize = 22.sp
                    )
                )
            }
        } else {
            // Action buttons after photo is captured: Use this photo & Retake
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // "Use this photo" primary button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(ElderPalShapes.button)
                        .background(SurfaceWhite)
                        .clickable(
                            onClick = {
                                capturedBitmap?.let { onUsePhotoClick(it) }
                            },
                            role = Role.Button
                        )
                        .semantics {
                            role = Role.Button
                            contentDescription = "Use this photo"
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
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Use this photo",
                            style = ElderPalTypography.title.copy(
                                color = TealPrimary,
                                fontSize = 24.sp
                            )
                        )
                    }
                }

                // "Retake" secondary button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(ElderPalShapes.button)
                        .border(3.dp, Color.White, ElderPalShapes.button)
                        .clickable(
                            onClick = {
                                capturedBitmap = null // Discard in-memory bitmap and return to live preview
                            },
                            role = Role.Button
                        )
                        .semantics {
                            role = Role.Button
                            contentDescription = "Retake photo"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_camera),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Retake",
                            style = ElderPalTypography.title.copy(
                                color = Color.White,
                                fontSize = 22.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FramingBracketsOverlay() {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        val stroke = 3.5.dp.toPx()
        val length = 38.dp.toPx()
        val radius = 16.dp.toPx()
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

@Composable
private fun CameraPermissionDeniedState(
    onEnableClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
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
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(TealPrimaryTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_camera),
                    contentDescription = null,
                    tint = TealPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Camera access needed",
                style = ElderPalTypography.heading2,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "ElderPal needs camera access to read and check your medicine label.",
                style = ElderPalTypography.body,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(ElderPalShapes.button)
                    .background(TealPrimary)
                    .clickable(
                        onClick = onEnableClick,
                        role = Role.Button
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Enable camera",
                    style = ElderPalTypography.title.copy(
                        color = Color.White,
                        fontSize = 20.sp
                    )
                )
            }
        }
    }
}
