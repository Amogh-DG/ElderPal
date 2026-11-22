// Configures the global Material 3 theme and color scheme for ElderPal.
package com.example.elderpal.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    primaryContainer = TealPrimaryTint,
    onPrimaryContainer = TealPrimary,
    secondary = TealPrimary,
    onSecondary = Color.White,
    secondaryContainer = TealPrimaryTint,
    onSecondaryContainer = TealPrimary,
    background = BackgroundWarmCream,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = BackgroundWarmCream,
    onSurfaceVariant = TextSecondary,
    outline = BorderCard,
    error = DangerRed,
    onError = Color.White,
    errorContainer = DangerRedTint,
    onErrorContainer = DangerRed
)

@Composable
fun ElderPalTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
