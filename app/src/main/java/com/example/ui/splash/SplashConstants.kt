package com.example.ui.splash

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object SplashConstants {
    val BrandBlue = Color(0xFF004AAD)
    val LogoWhite = Color.White

    const val CANVAS_SIZE = 2000f
    const val LOGO_WIDTH_PERCENT = 0.52f

    // Timeline Durations (Total ~2200 ms)
    const val TOTAL_DURATION_MS = 2200L
    const val STATIC_ACCESSIBILITY_MS = 800L

    // Interval 1: 0–700 ms (Triangles slide in + fade in)
    const val TRIANGLE_START_MS = 0L
    const val TRIANGLE_DURATION_MS = 700L

    // Interval 2: 600–1200 ms (Horizontal line expands from center)
    const val LINE_START_MS = 600L
    const val LINE_DURATION_MS = 600L

    // Interval 3: 1100–1600 ms (Text fade in + translate up 12dp)
    const val TEXT_START_MS = 1100L
    const val TEXT_DURATION_MS = 500L

    // Interval 4: 1600–2200 ms (Hold & Fade-out 300 ms)
    const val FADE_OUT_START_MS = 1600L
    const val FADE_OUT_DURATION_MS = 300L

    // Text specs
    const val BRAND_NAME = "Flews Motion"
    val TEXT_SIZE = 22.sp
    val TEXT_TRANSLATE_OFFSET_DP = 12.dp
    const val TEXT_LETTER_SPACING = 1.5f
}
