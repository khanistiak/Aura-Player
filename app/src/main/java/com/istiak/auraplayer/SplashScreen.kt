package com.istiak.auraplayer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.istiak.auraplayer.ui.theme.LocalExtendedPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Animated Liquid Glass Splash Overlay (`SplashScreen.kt`):
 * - Displays over AuraPlayer's signature ambient gradient (#DCEEFF -> #ECE2FF -> #F8F9FE in Light Mode;
 *   #0D1017 -> #161B2E in Dark Mode).
 * - Center Logo Reveal: Renders the minimal Liquid Glass play emblem inside a soft frosted glass container
 *   (96.dp, 28.dp squircle corners, subtle ambient blue-violet glow) with a smooth spring scale-in
 *   (0.82f -> 1.0f) and alpha fade-in (350ms).
 * - Brand Typography: "Aura Player" (24.sp, FontWeight.Bold, tight tracking) and subtitle
 *   "LIQUID GLASS MEDIA ENGINE" (10.sp, letterSpacing = 2.sp, muted slate).
 * - Auto-Transition to Home: Holds for 900ms total while VideoRepository pre-warms the media index in the
 *   background, then triggers onSplashFinished() to cross-fade (300ms) into DirectoryBrowserScreen.
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark

    val scaleAnim = remember { Animatable(0.82f) }
    val alphaAnim = remember { Animatable(0f) }
    val textAlphaAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            alphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            )
        }
        launch {
            scaleAnim.animateTo(
                targetValue = 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        launch {
            delay(80L)
            textAlphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            )
        }
        delay(900L)
        onSplashFinished()
    }

    val splashGradient = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0D1017),
                Color(0xFF161B2E)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFDCEEFF),
                Color(0xFFECE2FF),
                Color(0xFFF8F9FE)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(splashGradient)
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Subtle ambient blue-violet radial glow behind the center emblem
        Canvas(
            modifier = Modifier
                .size(240.dp)
                .alpha(alphaAnim.value)
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF6366F1).copy(alpha = if (isDark) 0.28f else 0.20f),
                        Color(0xFFA855F7).copy(alpha = if (isDark) 0.14f else 0.10f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.minDimension * 0.5f
                ),
                radius = size.minDimension * 0.5f,
                center = center
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .scale(scaleAnim.value)
                    .alpha(alphaAnim.value)
            ) {
                MinimalLiquidGlassLogo(
                    size = 96.dp,
                    cornerRadius = 28.dp,
                    modifier = Modifier.testTag("splash_logo_emblem")
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(textAlphaAnim.value)
            ) {
                Text(
                    text = "Aura Player",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF1E2438)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "LIQUID GLASS MEDIA ENGINE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
            }
        }
    }
}

/**
 * Reusable Minimal Liquid Glass Play Logo rendered in both `SplashScreen.kt` and `AboutScreen.kt`:
 * - Deep obsidian-slate radial gradient (#181C28 center to #0D1017 edges)
 * - Subtle translucent frosted glass rounded-squircle rim (strokeWidth = 1.5dp, Color.White.copy(alpha = 0.25f))
 *   with a top specular sheen
 * - Centered rounded triangular Play Emblem rendered with a luminous sky-blue to periwinkle-violet gradient
 *   (#60A5FA -> #6366F1 -> #A855F7), inner beveled glass highlight stroke (#93C5FD), and subtle inner
 *   triangular depth layer.
 */
@Composable
fun MinimalLiquidGlassLogo(
    size: Dp = 96.dp,
    cornerRadius: Dp = 28.dp,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 16.dp,
                shape = shape,
                ambientColor = Color(0xFF6366F1).copy(alpha = 0.45f),
                spotColor = Color(0xFFA855F7).copy(alpha = 0.45f)
            )
            .clip(shape)
            .border(
                width = 1.5.dp,
                color = Color.White.copy(alpha = 0.25f),
                shape = shape
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val center = Offset(w * 0.5f, h * 0.5f)
            val cornerPx = cornerRadius.toPx()

            // 1. Deep obsidian-slate radial gradient (#181C28 center to #0D1017 edges)
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF181C28),
                        Color(0xFF121620),
                        Color(0xFF0D1017)
                    ),
                    center = center,
                    radius = w * 0.72f
                )
            )

            // 2. Luminous inner Liquid Glass orb halo (#60A5FA -> #6366F1 -> #A855F7)
            val orbRadius = w * 0.37f
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF60A5FA).copy(alpha = 0.36f),
                        Color(0xFF6366F1).copy(alpha = 0.30f),
                        Color(0xFFA855F7).copy(alpha = 0.36f)
                    ),
                    start = Offset(w * 0.2f, h * 0.2f),
                    end = Offset(w * 0.8f, h * 0.8f)
                ),
                radius = orbRadius,
                center = center
            )

            // Crisp translucent circular glass ring
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF93C5FD).copy(alpha = 0.55f),
                        Color(0xFFA855F7).copy(alpha = 0.35f)
                    ),
                    start = Offset(w * 0.2f, h * 0.2f),
                    end = Offset(w * 0.8f, h * 0.8f)
                ),
                radius = orbRadius,
                center = center,
                style = Stroke(width = 1.2.dp.toPx())
            )

            // 3. Top specular sheen across the upper squircle
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.22f),
                        Color.White.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = h * 0.48f
                ),
                topLeft = Offset(w * 0.04f, h * 0.03f),
                size = Size(w * 0.92f, h * 0.45f),
                cornerRadius = CornerRadius(cornerPx * 0.9f, cornerPx * 0.9f)
            )

            // 4. Centered rounded triangular Play Emblem (#60A5FA -> #6366F1 -> #A855F7)
            // Normalized coordinates from the 108dp vector viewport inside the 66dp safe zone
            fun mapX(vx: Float): Float = ((vx - 21f) / 66f) * w
            fun mapY(vy: Float): Float = ((vy - 21f) / 66f) * h

            val outerPlayPath = Path().apply {
                moveTo(mapX(44.5f), mapY(39.2f))
                cubicTo(
                    mapX(44.5f), mapY(36.0f),
                    mapX(48.0f), mapY(34.1f),
                    mapX(50.7f), mapY(35.8f)
                )
                lineTo(mapX(68.8f), mapY(47.0f))
                cubicTo(
                    mapX(71.4f), mapY(48.6f),
                    mapX(71.4f), mapY(52.4f),
                    mapX(68.8f), mapY(54.0f)
                )
                lineTo(mapX(50.7f), mapY(65.2f))
                cubicTo(
                    mapX(48.0f), mapY(66.9f),
                    mapX(44.5f), mapY(65.0f),
                    mapX(44.5f), mapY(61.8f)
                )
                close()
            }

            // Soft drop shadow under Play triangle
            val shadowPlayPath = Path().apply {
                moveTo(mapX(44.5f), mapY(40.4f))
                cubicTo(
                    mapX(44.5f), mapY(37.2f),
                    mapX(48.0f), mapY(35.3f),
                    mapX(50.7f), mapY(37.0f)
                )
                lineTo(mapX(68.8f), mapY(48.2f))
                cubicTo(
                    mapX(71.4f), mapY(49.8f),
                    mapX(71.4f), mapY(53.6f),
                    mapX(68.8f), mapY(55.2f)
                )
                lineTo(mapX(50.7f), mapY(66.4f))
                cubicTo(
                    mapX(48.0f), mapY(68.1f),
                    mapX(44.5f), mapY(66.2f),
                    mapX(44.5f), mapY(63.0f)
                )
                close()
            }

            drawPath(
                path = shadowPlayPath,
                color = Color(0xFF090D1A).copy(alpha = 0.42f)
            )

            // Main luminous sky-blue -> periwinkle -> violet Play gradient
            drawPath(
                path = outerPlayPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF60A5FA),
                        Color(0xFF6366F1),
                        Color(0xFFA855F7)
                    ),
                    start = Offset(mapX(44f), mapY(35f)),
                    end = Offset(mapX(71f), mapY(65f))
                )
            )

            // 5. Subtle inner triangular depth layer
            val innerDepthPath = Path().apply {
                moveTo(mapX(47.8f), mapY(42.8f))
                cubicTo(
                    mapX(47.8f), mapY(40.8f),
                    mapX(50.0f), mapY(39.6f),
                    mapX(51.7f), mapY(40.7f)
                )
                lineTo(mapX(64.5f), mapY(48.6f))
                cubicTo(
                    mapX(66.1f), mapY(49.6f),
                    mapX(66.1f), mapY(51.4f),
                    mapX(64.5f), mapY(52.4f)
                )
                lineTo(mapX(51.7f), mapY(60.3f))
                cubicTo(
                    mapX(50.0f), mapY(61.4f),
                    mapX(47.8f), mapY(60.2f),
                    mapX(47.8f), mapY(58.2f)
                )
                close()
            }

            drawPath(
                path = innerDepthPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.26f),
                        Color(0xFF60A5FA).copy(alpha = 0.12f),
                        Color(0xFF1E1B4B).copy(alpha = 0.24f)
                    ),
                    start = Offset(mapX(47f), mapY(40f)),
                    end = Offset(mapX(66f), mapY(60f))
                )
            )

            // 6. Inner beveled glass highlight stroke (#93C5FD)
            drawPath(
                path = outerPlayPath,
                color = Color(0xFF93C5FD),
                style = Stroke(
                    width = 1.4.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}
