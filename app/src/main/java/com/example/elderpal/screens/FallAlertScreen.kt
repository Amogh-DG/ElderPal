// Placeholder screen for fall detection countdown and emergency alert.
package com.example.elderpal.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun FallAlertScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    PlaceholderScreen(
        title = "Fall Alert",
        onBack = onBack,
        modifier = modifier
    )
}
