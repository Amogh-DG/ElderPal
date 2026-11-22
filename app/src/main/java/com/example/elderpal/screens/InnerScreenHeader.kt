// Reusable header component for inner screens with a back button and an SOS emergency chip.
package com.example.elderpal.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import com.example.elderpal.R
import com.example.elderpal.ui.theme.BorderCard
import com.example.elderpal.ui.theme.DangerRed
import com.example.elderpal.ui.theme.ElderPalShapes
import com.example.elderpal.ui.theme.ElderPalTypography
import com.example.elderpal.ui.theme.SurfaceWhite
import com.example.elderpal.ui.theme.TextPrimary

@Composable
fun InnerScreenHeader(
    onBack: () -> Unit,
    onSosClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back Button (< Back)
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

        // SOS chip (64 x 112 dp, top right)
        Box(
            modifier = Modifier
                .height(64.dp)
                .width(112.dp)
                .clip(ElderPalShapes.button)
                .background(DangerRed)
                .clickable(
                    onClick = onSosClick,
                    role = Role.Button
                )
                .semantics {
                    role = Role.Button
                    contentDescription = "SOS Emergency help"
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
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SOS",
                    style = ElderPalTypography.title.copy(color = Color.White)
                )
            }
        }
    }
}
