// Placeholder screen for managing emergency contacts.
package com.example.elderpal.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ContactsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    PlaceholderScreen(
        title = "Emergency Contacts",
        onBack = onBack,
        modifier = modifier
    )
}
