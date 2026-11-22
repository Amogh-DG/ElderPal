// Sets up font families and typography styles for high readability according to ElderPal design specs.
package com.example.elderpal.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.elderpal.R

// Atkinson Hyperlegible font family (open license from Braille Institute via Google Fonts)
val AtkinsonHyperlegible = FontFamily(
    Font(R.font.atkinson_hyperlegible_regular, FontWeight.Normal),
    Font(R.font.atkinson_hyperlegible_bold, FontWeight.Bold)
)

// Patrick Hand handwritten font family for warm greetings (SIL Open Font License via Google Fonts)
val HandwrittenGreeting = FontFamily(
    Font(R.font.patrick_hand_regular, FontWeight.Normal)
)

// Explicit typography styles matching design.md specifications
object ElderPalTypography {
    val display = TextStyle(
        fontFamily = AtkinsonHyperlegible,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        color = TextPrimary
    )

    val big = TextStyle(
        fontFamily = AtkinsonHyperlegible,
        fontWeight = FontWeight.Bold,
        fontSize = 42.sp,
        lineHeight = 50.sp,
        color = TextPrimary
    )

    val heading2 = TextStyle(
        fontFamily = AtkinsonHyperlegible,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        color = TextPrimary
    )

    val title = TextStyle(
        fontFamily = AtkinsonHyperlegible,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        color = TextPrimary
    )

    val body = TextStyle(
        fontFamily = AtkinsonHyperlegible,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 30.sp,
        color = TextPrimary
    )

    val label = TextStyle(
        fontFamily = AtkinsonHyperlegible,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        color = TextPrimary
    )

    val small = TextStyle(
        fontFamily = AtkinsonHyperlegible,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        color = TextSecondary
    )

    val greeting = TextStyle(
        fontFamily = HandwrittenGreeting,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        color = TextSecondary
    )
}

// Material 3 Typography integration
val Typography = Typography(
    displayLarge = ElderPalTypography.big,
    displayMedium = ElderPalTypography.display,
    headlineMedium = ElderPalTypography.heading2,
    titleLarge = ElderPalTypography.title,
    bodyLarge = ElderPalTypography.body,
    bodyMedium = ElderPalTypography.body,
    labelLarge = ElderPalTypography.label,
    bodySmall = ElderPalTypography.small,
    labelSmall = ElderPalTypography.small
)
