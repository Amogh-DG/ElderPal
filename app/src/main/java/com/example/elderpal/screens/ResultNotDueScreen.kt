// Displays the notice when a verified medicine is on the schedule but not due yet.
package com.example.elderpal.screens

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.elderpal.R
import com.example.elderpal.ui.theme.BackgroundWarmCream
import com.example.elderpal.ui.theme.BorderCard
import com.example.elderpal.ui.theme.ElderPalShapes
import com.example.elderpal.ui.theme.ElderPalTypography
import com.example.elderpal.ui.theme.SurfaceWhite
import com.example.elderpal.ui.theme.TealPrimary
import com.example.elderpal.ui.theme.TextPrimary
import com.example.elderpal.ui.theme.TextSecondary
import com.example.elderpal.ui.theme.WarningAmberFill
import com.example.elderpal.ui.theme.WarningAmberText
import com.example.elderpal.ui.theme.WarningAmberTint

@Composable
fun ResultNotDueScreen(
    medicineName: String,
    dose: String,
    scheduledTime: String,
    onBack: () -> Unit,
    onWaitClick: () -> Unit,
    onTakeAnywayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundWarmCream)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Back button
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

        // Center card matching design/result-not-due.png
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = ElderPalShapes.card,
            colors = CardDefaults.cardColors(containerColor = WarningAmberTint),
            border = androidx.compose.foundation.BorderStroke(3.dp, WarningAmberText)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Large circular amber clock badge
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(WarningAmberFill),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_clock),
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(64.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Not due yet",
                    style = ElderPalTypography.display.copy(
                        color = WarningAmberText,
                        fontSize = 32.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                val timeDueText = if (scheduledTime.isNotBlank()) {
                    "This one isn't due until $scheduledTime."
                } else {
                    "This medicine is not due yet."
                }

                Text(
                    text = timeDueText,
                    style = ElderPalTypography.body.copy(fontSize = 20.sp),
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Inner white badge showing medicine name and dose
                Surface(
                    shape = ElderPalShapes.input,
                    color = SurfaceWhite,
                    border = androidx.compose.foundation.BorderStroke(2.dp, BorderCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_pill),
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "$medicineName · $dose",
                            style = ElderPalTypography.title.copy(
                                color = TextPrimary,
                                fontSize = 22.sp
                            )
                        )
                    }
                }
            }
        }

        // Bottom Actions: OK I will wait & Take anyway
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // "OK, I will wait" primary button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(ElderPalShapes.button)
                    .background(TealPrimary)
                    .clickable(
                        onClick = onWaitClick,
                        role = Role.Button
                    )
                    .semantics {
                        role = Role.Button
                        contentDescription = "OK, I will wait"
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
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "OK, I will wait",
                        style = ElderPalTypography.title.copy(
                            color = Color.White,
                            fontSize = 24.sp
                        )
                    )
                }
            }

            // "Take anyway" secondary button (amber border)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(ElderPalShapes.button)
                    .background(SurfaceWhite)
                    .border(3.dp, WarningAmberText, ElderPalShapes.button)
                    .clickable(
                        onClick = onTakeAnywayClick,
                        role = Role.Button
                    )
                    .semantics {
                        role = Role.Button
                        contentDescription = "Take anyway"
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Take anyway",
                    style = ElderPalTypography.title.copy(
                        color = WarningAmberText,
                        fontSize = 24.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "ElderPal is a helper, not medical advice.",
                style = ElderPalTypography.small.copy(color = TextSecondary),
                textAlign = TextAlign.Center
            )
        }
    }
}
