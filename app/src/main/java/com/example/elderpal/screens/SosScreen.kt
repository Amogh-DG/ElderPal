// Placeholder screen for the emergency SOS confirmation and trigger.
package com.example.elderpal.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SosScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    PlaceholderScreen(
        title = "SOS",
        onBack = onBack,
        modifier = modifier
    )
}
