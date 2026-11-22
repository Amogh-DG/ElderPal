// Displays the loading screen while Gemini verifies the medicine label, allowing cancellation.
package com.example.elderpal.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.elderpal.ai.CapturedImageHolder
import com.example.elderpal.ai.CheckResultType
import com.example.elderpal.ai.LabelCheckResult
import com.example.elderpal.ai.MedicineLabelChecker
import com.example.elderpal.ui.theme.BackgroundWarmCream
import com.example.elderpal.ui.theme.BorderInput
import com.example.elderpal.ui.theme.ElderPalShapes
import com.example.elderpal.ui.theme.ElderPalTypography
import com.example.elderpal.ui.theme.SurfaceWhite
import com.example.elderpal.ui.theme.TealPrimary
import com.example.elderpal.ui.theme.TealPrimaryTint
import com.example.elderpal.ui.theme.TextPrimary
import com.example.elderpal.ui.theme.TextSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun CheckMedicineLoadingScreen(
    onResultReady: (LabelCheckResult) -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val checker = remember { MedicineLabelChecker(context) }

    LaunchedEffect(Unit) {
        val bitmap = CapturedImageHolder.currentBitmap
        if (bitmap == null) {
            onResultReady(
                LabelCheckResult(
                    type = CheckResultType.UNSURE,
                    reason = "No label photo found. Please try again."
                )
            )
            return@LaunchedEffect
        }

        val checkJob: Job = coroutineScope.launch {
            val result = checker.checkMedicineLabel(bitmap)
            CapturedImageHolder.latestResult = result
            onResultReady(result)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundWarmCream)
            .padding(horizontal = 20.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Center: Spinner with capsule pill icon matching check-medicine-loading.png
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(170.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background tint circle
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(TealPrimaryTint)
                )

                // Indeterminate circular progress arc
                CircularProgressIndicator(
                    modifier = Modifier.size(160.dp),
                    color = TealPrimary,
                    strokeWidth = 10.dp
                )

                // Centered pill icon
                Icon(
                    painter = painterResource(id = R.drawable.ic_pill),
                    contentDescription = null,
                    tint = TealPrimary,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            Text(
                text = "Checking your\nmedicine",
                style = ElderPalTypography.heading2,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "This takes a few seconds. Please keep the label in view.",
                style = ElderPalTypography.body,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }

        // Bottom "Cancel" button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(ElderPalShapes.button)
                .background(SurfaceWhite)
                .border(3.dp, BorderInput, ElderPalShapes.button)
                .clickable(
                    onClick = {
                        CapturedImageHolder.clear()
                        onCancelClick()
                    },
                    role = Role.Button
                )
                .semantics {
                    role = Role.Button
                    contentDescription = "Cancel checking medicine"
                },
            contentAlignment = Alignment.Center
        ) {
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
