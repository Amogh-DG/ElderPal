// Dialog explaining why SMS permissions are needed before launching the Android system prompt.
package com.example.elderpal.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.elderpal.R
import com.example.elderpal.ui.theme.BackgroundWarmCream
import com.example.elderpal.ui.theme.ElderPalShapes
import com.example.elderpal.ui.theme.ElderPalTypography
import com.example.elderpal.ui.theme.TealPrimary
import com.example.elderpal.ui.theme.TealPrimaryTint
import com.example.elderpal.ui.theme.TextPrimary
import com.example.elderpal.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsPermissionExplainer(
    onAllowClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismissClick
    ) {
        Surface(
            shape = ElderPalShapes.card,
            color = BackgroundWarmCream,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Circular badge with chat icon matching permissions-sms.png
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(TealPrimaryTint)
                        .border(3.dp, TealPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_chat),
                        contentDescription = null,
                        tint = TealPrimary,
                        modifier = Modifier.size(56.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Allow text messages",
                    style = ElderPalTypography.heading2,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "ElderPal sends a text to your family if you press SOS or if you fall.",
                    style = ElderPalTypography.body,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // "Allow" primary button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(ElderPalShapes.button)
                        .background(TealPrimary)
                        .clickable(
                            onClick = onAllowClick,
                            role = Role.Button
                        )
                        .semantics {
                            role = Role.Button
                            contentDescription = "Allow text messages"
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

                Spacer(modifier = Modifier.height(16.dp))

                // "Not now" quiet dismiss button
                Text(
                    text = "Not now",
                    style = ElderPalTypography.title.copy(
                        color = TextSecondary,
                        fontSize = 20.sp,
                        textDecoration = TextDecoration.Underline
                    ),
                    modifier = Modifier
                        .clickable(
                            onClick = onDismissClick,
                            role = Role.Button
                        )
                        .padding(8.dp)
                )
            }
        }
    }
}
