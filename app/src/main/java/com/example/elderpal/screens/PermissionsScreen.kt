// Placeholder screen for explaining and requesting required device permissions.
package com.example.elderpal.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun PermissionsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    PlaceholderScreen(
        title = "Permissions",
        onBack = onBack,
        modifier = modifier
    )
}
