package com.example.ui.splash

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.ui.splash.SplashConstants.FADE_OUT_DURATION_MS
import com.example.ui.splash.SplashConstants.LINE_DURATION_MS
import com.example.ui.splash.SplashConstants.STATIC_ACCESSIBILITY_MS
import com.example.ui.splash.SplashConstants.TEXT_DURATION_MS
import com.example.ui.splash.SplashConstants.TRIANGLE_DURATION_MS
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val EaseOutCubic = CubicBezierEasing(0.215f, 0.610f, 0.355f, 1.000f)

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val context = LocalContext.current

    // Check system accessibility "reduce motion" setting
    val animationScale = try {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1.0f
        )
    } catch (e: Exception) {
        1.0f
    }
    val isReduceMotionEnabled = animationScale == 0f

    // Animated values
    val leftTriangleSlide = remember { Animatable(0f) }
    val rightTriangleSlide = remember { Animatable(0f) }
    val lineExpandScale = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val textTranslateUpDp = remember { Animatable(12f) }
    val overallAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        if (isReduceMotionEnabled) {
            // Static mode for accessibility: show full logo for 800ms then transition
            leftTriangleSlide.snapTo(1f)
            rightTriangleSlide.snapTo(1f)
            lineExpandScale.snapTo(1f)
            textAlpha.snapTo(1f)
            textTranslateUpDp.snapTo(0f)
            delay(STATIC_ACCESSIBILITY_MS)
            onSplashFinished()
            return@LaunchedEffect
        }

        // Timeline Sequence (Total ~2200ms)
        // 1. 0–700 ms: Triangles slide in & fade in
        coroutineScope {
            launch {
                leftTriangleSlide.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = TRIANGLE_DURATION_MS.toInt(),
                        easing = EaseOutCubic
                    )
                )
            }
            launch {
                rightTriangleSlide.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = TRIANGLE_DURATION_MS.toInt(),
                        easing = EaseOutCubic
                    )
                )
            }
        }

        // 2. 600–1200 ms: Line expands horizontally from center
        delay(100L) // (600ms boundary from 0ms)
        lineExpandScale.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = LINE_DURATION_MS.toInt(),
                easing = FastOutSlowInEasing
            )
        )

        // 3. 1100–1600 ms: Text fade in + translate up 12dp
        coroutineScope {
            launch {
                textAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = TEXT_DURATION_MS.toInt(),
                        easing = LinearEasing
                    )
                )
            }
            launch {
                textTranslateUpDp.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = TEXT_DURATION_MS.toInt(),
                        easing = EaseOutCubic
                    )
                )
            }
        }

        // 4. 1600–2200 ms: Hold & then Fade-out 300ms
        delay(300L) // Hold until 1900ms
        overallAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = FADE_OUT_DURATION_MS.toInt(),
                easing = LinearEasing
            )
        )

        // Ensure safe single navigation to Home
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SplashConstants.BrandBlue)
            .testTag("splash_screen_view")
    ) {
        LogoCanvas(
            leftTriangleSlide = leftTriangleSlide.value,
            leftTriangleAlpha = leftTriangleSlide.value,
            rightTriangleSlide = rightTriangleSlide.value,
            rightTriangleAlpha = rightTriangleSlide.value,
            lineExpandScale = lineExpandScale.value,
            lineAlpha = lineExpandScale.value,
            textAlpha = textAlpha.value,
            textTranslateUpDp = textTranslateUpDp.value,
            overallAlpha = overallAlpha.value,
            modifier = Modifier.fillMaxSize()
        )
    }
}
