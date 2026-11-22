// Placeholder screen indicating that the prescription scanning feature is coming later.
package com.example.elderpal.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ScanPrescriptionScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    PlaceholderScreen(
        title = "Scan Prescription",
        subtitle = "Coming later",
        onBack = onBack,
        modifier = modifier
    )
}
