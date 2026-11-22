// Reusable alert sent confirmation screen matching design/fall-alert-sent.png for both SOS and fall detection.
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.elderpal.R
import com.example.elderpal.data.AlertDispatchReport
import com.example.elderpal.data.ContactAlertStatus
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

@Composable
fun EmergencyAlertSentView(
    report: AlertDispatchReport,
    onCallAgainClick: () -> Unit,
    onOkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Large circular green checkmark badge matching fall-alert-sent.png
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(SuccessGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_check),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
            }

            Text(
                text = "Alert sent to family",
                style = ElderPalTypography.display.copy(fontSize = 32.sp),
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Please stay where you are. Help is on\nthe way.",
                style = ElderPalTypography.body.copy(fontSize = 20.sp, lineHeight = 30.sp),
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // List of notified contacts matching fall-alert-sent.png
            report.contactStatuses.forEach { contactStatus ->
                NotifiedContactCard(contactStatus = contactStatus)
            }

            // Call status summary card
            val primaryName = report.primaryContact?.name ?: "Family"
            val callStatusText = when (report.callStatus) {
                PhoneCallStatus.PLACED -> "Called $primaryName"
                PhoneCallStatus.DIALER_OPENED -> "Opened dialer for $primaryName"
                PhoneCallStatus.FAILED -> "Call could not be placed"
                PhoneCallStatus.NONE -> "No primary contact set"
            }

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
                        painter = painterResource(id = R.drawable.ic_phone),
                        contentDescription = null,
                        tint = TealPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = callStatusText,
                        style = ElderPalTypography.body.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Bottom Action Buttons matching fall-alert-sent.png
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val primary = report.primaryContact
            if (primary != null) {
                // "Call [Name]" primary button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(ElderPalShapes.button)
                        .background(TealPrimary)
                        .clickable(
                            onClick = onCallAgainClick,
                            role = Role.Button
                        )
                        .semantics {
                            role = Role.Button
                            contentDescription = "Call ${primary.name}"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_phone),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Call ${primary.name}",
                            style = ElderPalTypography.title.copy(
                                color = Color.White,
                                fontSize = 24.sp
                            )
                        )
                    }
                }
            }

            // "I'm OK now" secondary button matching fall-alert-sent.png
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(ElderPalShapes.button)
                    .background(SurfaceWhite)
                    .border(3.dp, BorderInput, ElderPalShapes.button)
                    .clickable(
                        onClick = onOkClick,
                        role = Role.Button
                    )
                    .semantics {
                        role = Role.Button
                        contentDescription = "I am okay now"
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "I'm OK now",
                    style = ElderPalTypography.title.copy(
                        color = TextPrimary,
                        fontSize = 24.sp
                    )
                )
            }
        }
    }
}

// Single contact delivery card matching the cards in fall-alert-sent.png
@Composable
fun NotifiedContactCard(
    contactStatus: ContactAlertStatus,
    modifier: Modifier = Modifier
) {
    val contact = contactStatus.contact
    val initial = contact.name.trim().firstOrNull()?.uppercase() ?: "C"

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = ElderPalShapes.card,
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(2.dp, BorderCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circle avatar with first letter
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(TealPrimaryTint)
                    .border(2.5.dp, TealPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    style = ElderPalTypography.title.copy(
                        color = TealPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Contact Name and Relationship
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = ElderPalTypography.title.copy(fontSize = 22.sp),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = contact.relationship.ifBlank { "Contact" },
                    style = ElderPalTypography.body.copy(fontSize = 18.sp),
                    color = TextSecondary
                )
            }

            // Delivery Status Badge matching fall-alert-sent.png ("Told", "In app", or "Failed")
            when (contactStatus.smsStatus) {
                SmsDeliveryStatus.SENT -> {
                    Surface(
                        shape = CircleShape,
                        color = SuccessTint,
                        border = BorderStroke(2.dp, SuccessGreen)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_check),
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Told",
                                style = ElderPalTypography.small.copy(
                                    color = SuccessGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            )
                        }
                    }
                }
                SmsDeliveryStatus.OPENED_IN_APP -> {
                    Surface(
                        shape = CircleShape,
                        color = TealPrimaryTint,
                        border = BorderStroke(2.dp, TealPrimary)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_chat),
                                contentDescription = null,
                                tint = TealPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "In app",
                                style = ElderPalTypography.small.copy(
                                    color = TealPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            )
                        }
                    }
                }
                SmsDeliveryStatus.FAILED -> {
                    Surface(
                        shape = CircleShape,
                        color = DangerRedTint,
                        border = BorderStroke(2.dp, DangerRed)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_warning),
                                contentDescription = null,
                                tint = DangerRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Failed",
                                style = ElderPalTypography.small.copy(
                                    color = DangerRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
