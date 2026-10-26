package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = FlewsPrimary,
    onPrimary = Color.White,
    primaryContainer = FlewsSurfaceHigh,
    onPrimaryContainer = FlewsTextPrimary,
    inversePrimary = FlewsPrimaryLight,
    secondary = FlewsPrimaryLight,
    onSecondary = Color.White,
    secondaryContainer = FlewsSurfaceHigh,
    onSecondaryContainer = FlewsTextPrimary,
    tertiary = FlewsKeyframeActive,
    onTertiary = Color.White,
    background = FlewsBackground,
    onBackground = FlewsTextPrimary,
    surface = FlewsSurface,
    onSurface = FlewsTextPrimary,
    surfaceVariant = FlewsSurfaceHigh,
    onSurfaceVariant = FlewsTextSecondary,
    outline = FlewsBorder,
    outlineVariant = FlewsBorder,
    error = FlewsAccentRed,
    onError = Color.White
)

val FlewsShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp)
)

@Composable
fun FlewsMotionTheme(
    content: @Composable () -> Unit
) {
    // Flews Motion strictly enforces Dark Mode as specified in design constraints
    MaterialTheme(
        colorScheme = DarkColorScheme,
        shapes = FlewsShapes,
        typography = Typography,
        content = content
    )
}
