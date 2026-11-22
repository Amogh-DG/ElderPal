// Defines component corner shapes and radiuses according to ElderPal design specs.
package com.example.elderpal.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object ElderPalShapes {
    val card = RoundedCornerShape(24.dp)
    val button = RoundedCornerShape(20.dp)
    val input = RoundedCornerShape(16.dp)
    val pill = RoundedCornerShape(50.dp)
    val logo = RoundedCornerShape(16.dp)
    val sosBar = RoundedCornerShape(28.dp)
}

val Shapes = Shapes(
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp)
)
