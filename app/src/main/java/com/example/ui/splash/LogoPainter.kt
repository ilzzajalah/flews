package com.example.ui.splash

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.splash.SplashConstants.CANVAS_SIZE
import com.example.ui.splash.SplashConstants.LOGO_WIDTH_PERCENT

@Composable
fun LogoCanvas(
    leftTriangleSlide: Float, // 0.0f (far left) to 1.0f (in position)
    leftTriangleAlpha: Float,
    rightTriangleSlide: Float, // 0.0f (far right) to 1.0f (in position)
    rightTriangleAlpha: Float,
    lineExpandScale: Float, // 0.0f to 1.0f (horizontal scale from center)
    lineAlpha: Float,
    textAlpha: Float,
    textTranslateUpDp: Float, // e.g. 12dp down to 0dp
    overallAlpha: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val screenW = size.width
        val screenH = size.height

        // Logo target width = 52% screen width
        val logoScale = (screenW * LOGO_WIDTH_PERCENT) / CANVAS_SIZE
        val logoWidthPx = CANVAS_SIZE * logoScale
        val logoHeightPx = CANVAS_SIZE * logoScale

        // Center on screen
        val originX = (screenW - logoWidthPx) / 2f
        val originY = (screenH - logoHeightPx) / 2f

        translate(originX, originY) {
            scale(logoScale, logoScale, pivot = Offset.Zero) {
                // Global fade-out modifier
                val effectiveLeftAlpha = (leftTriangleAlpha * overallAlpha).coerceIn(0f, 1f)
                val effectiveRightAlpha = (rightTriangleAlpha * overallAlpha).coerceIn(0f, 1f)
                val effectiveLineAlpha = (lineAlpha * overallAlpha).coerceIn(0f, 1f)
                val effectiveTextAlpha = (textAlpha * overallAlpha).coerceIn(0f, 1f)

                // 1. Left Triangle (pointing left)
                // Normal position: (922,638), (316,1018), (922,1398)
                val leftSlideOffsetX = (1f - leftTriangleSlide) * -1200f
                translate(leftSlideOffsetX, 0f) {
                    val leftPath = Path().apply {
                        moveTo(922f, 638f)
                        lineTo(316f, 1018f)
                        lineTo(922f, 1398f)
                        close()
                    }
                    drawPath(
                        path = leftPath,
                        color = Color.White.copy(alpha = effectiveLeftAlpha)
                    )
                }

                // 2. Right Triangle (pointing right)
                // Normal position: (1012,384), (1618,764), (1012,1144)
                val rightSlideOffsetX = (1f - rightTriangleSlide) * 1200f
                translate(rightSlideOffsetX, 0f) {
                    val rightPath = Path().apply {
                        moveTo(1012f, 384f)
                        lineTo(1618f, 764f)
                        lineTo(1012f, 1144f)
                        close()
                    }
                    drawPath(
                        path = rightPath,
                        color = Color.White.copy(alpha = effectiveRightAlpha)
                    )
                }

                // 3. Thin Horizontal Line below
                // From (569,1574) to (1362,1574)
                // Scale horizontally from center (965.5, 1574)
                val lineCenterX = (569f + 1362f) / 2f
                val lineY = 1574f
                val strokeWidthPx = (3f / logoScale).coerceAtLeast(3f) // Ensure min 1.5 dp

                scale(
                    scaleX = lineExpandScale,
                    scaleY = 1f,
                    pivot = Offset(lineCenterX, lineY)
                ) {
                    drawLine(
                        color = Color.White.copy(alpha = effectiveLineAlpha),
                        start = Offset(569f, lineY),
                        end = Offset(1362f, lineY),
                        strokeWidth = strokeWidthPx,
                        cap = StrokeCap.Round
                    )
                }

                // 4. Text "Flews Motion"
                // Rendered below line with fade-in + translate up
                if (effectiveTextAlpha > 0.01f) {
                    val textTranslateCanvas = textTranslateUpDp / logoScale
                    val textY = 1780f + textTranslateCanvas

                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        this.alpha = (effectiveTextAlpha * 255).toInt().coerceIn(0, 255)
                        textSize = 100f // 22sp in normalized canvas units
                        isAntiAlias = true
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.create(
                            android.graphics.Typeface.DEFAULT,
                            600,
                            false
                        )
                        letterSpacing = 0.08f
                    }

                    drawContext.canvas.nativeCanvas.drawText(
                        SplashConstants.BRAND_NAME,
                        lineCenterX,
                        textY,
                        paint
                    )
                }
            }
        }
    }
}
