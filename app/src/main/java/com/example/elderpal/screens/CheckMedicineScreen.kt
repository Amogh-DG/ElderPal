// Placeholder screen for photographing and checking a medicine label with AI.
package com.example.elderpal.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun CheckMedicineScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    PlaceholderScreen(
        title = "Check Medicine",
        onBack = onBack,
        modifier = modifier
    )
}
