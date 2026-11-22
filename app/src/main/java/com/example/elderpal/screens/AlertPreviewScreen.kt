// Displays a preview of what emergency contacts receive when an alert is dispatched, matching design/alert-preview.png.
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.elderpal.R
import com.example.elderpal.data.AlertMessageBuilder
import com.example.elderpal.data.AlertType
import com.example.elderpal.data.MedicineRepository
import com.example.elderpal.ui.theme.BackgroundWarmCream
import com.example.elderpal.ui.theme.BorderCard
import com.example.elderpal.ui.theme.ElderPalShapes
import com.example.elderpal.ui.theme.ElderPalTypography
import com.example.elderpal.ui.theme.SurfaceWhite
import com.example.elderpal.ui.theme.TealPrimary
import com.example.elderpal.ui.theme.TealPrimaryTint
import com.example.elderpal.ui.theme.TextPrimary
import com.example.elderpal.ui.theme.TextSecondary
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AlertPreviewScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val medicineRepository = remember { MedicineRepository(context) }
    val lastTakenDose by medicineRepository.lastTakenDoseToday.collectAsState(initial = null)

    val sampleTime = remember { LocalTime.now() }
    val sampleTimeFormatted = remember(sampleTime) {
        sampleTime.format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))
    }

    // Build the preview message using the shared alert message builder and real stored dose data
    val previewMessageText = remember(lastTakenDose, sampleTime) {
        AlertMessageBuilder.buildTextWithoutLocation(
            alertType = AlertType.FALL_DETECTED,
            currentTime = sampleTime,
            lastTakenDose = lastTakenDose
        )
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundWarmCream)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Back Button matching design/alert-preview.png
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .height(64.dp)
                        .clip(ElderPalShapes.button)
                        .background(SurfaceWhite)
                        .border(2.dp, BorderCard, ElderPalShapes.button)
                        .clickable(
                            onClick = onBack,
                            role = Role.Button
                        )
                        .semantics {
                            role = Role.Button
                            contentDescription = "Go back to contacts"
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
                            tint = TextPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Back",
                            style = ElderPalTypography.title.copy(color = TextPrimary)
                        )
                    }
                }
            }

            // Headline & Subtitle matching design/alert-preview.png
            Text(
                text = "What your family\nsees",
                style = ElderPalTypography.display.copy(fontSize = 32.sp, lineHeight = 40.sp),
                color = TextPrimary
            )

            Text(
                text = "This is an example of the text they\nget.",
                style = ElderPalTypography.body.copy(fontSize = 20.sp, lineHeight = 30.sp),
                color = TextSecondary
            )

            // Chat Message Container Card matching design/alert-preview.png
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = ElderPalShapes.card,
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0EBE1)),
                border = BorderStroke(2.dp, BorderCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Sender info header (Avatar + "ElderPal")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(TealPrimaryTint)
                                .border(2.5.dp, TealPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "E",
                                style = ElderPalTypography.title.copy(
                                    color = TealPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Text(
                            text = "ElderPal",
                            style = ElderPalTypography.title.copy(fontSize = 24.sp),
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Chat Bubble 1: Alert text and recent medicine details
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = SurfaceWhite,
                        border = BorderStroke(1.5.dp, BorderCard)
                    ) {
                        Text(
                            text = previewMessageText,
                            style = ElderPalTypography.body.copy(
                                fontSize = 20.sp,
                                lineHeight = 28.sp
                            ),
                            color = TextPrimary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Chat Bubble 2: Interactive map link preview (example)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = SurfaceWhite,
                        border = BorderStroke(1.5.dp, BorderCard)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_location),
                                contentDescription = null,
                                tint = TealPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Location: tap to open the map",
                                    style = ElderPalTypography.title.copy(
                                        fontSize = 20.sp,
                                        color = TealPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "maps.google.com/?q=... (example link)",
                                    style = ElderPalTypography.small.copy(
                                        fontSize = 16.sp,
                                        color = TextSecondary
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Timestamp
                    Text(
                        text = sampleTimeFormatted,
                        style = ElderPalTypography.small.copy(fontSize = 18.sp, color = TextSecondary),
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }
            }

            // Informational Card matching design/alert-preview.png
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = ElderPalShapes.card,
                colors = CardDefaults.cardColors(containerColor = TealPrimaryTint),
                border = BorderStroke(2.dp, TealPrimary)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_chat),
                        contentDescription = null,
                        tint = TealPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Sent by text message to every contact on your list.",
                        style = ElderPalTypography.body.copy(fontSize = 20.sp, lineHeight = 28.sp),
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Bottom "Back" Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .height(72.dp)
                .clip(ElderPalShapes.button)
                .background(TealPrimary)
                .clickable(
                    onClick = onBack,
                    role = Role.Button
                )
                .semantics {
                    role = Role.Button
                    contentDescription = "Return to contacts"
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Back",
                style = ElderPalTypography.title.copy(
                    color = Color.White,
                    fontSize = 24.sp
                )
            )
        }
    }
}
