package com.istiak.auraplayer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.istiak.auraplayer.ui.theme.LocalExtendedPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Animated Liquid Glass Splash Overlay (`SplashScreen.kt`):
 * - Displays over Aura Player's ambient gradient (#EEF3FF in Light Mode / #0B101E in Dark Mode).
 * - Animates the exact `R.drawable.aura_logo` (`108.dp`) with a smooth spring scale-in
 *   (0.82f -> 1.0f) and alpha fade-in (350ms), accompanied by "Aura Player" (`24.sp`, bold)
 *   and "LIQUID GLASS MEDIA ENGINE" (`10.sp`, `letterSpacing = 2.sp`).
 * - Holds for `900ms` while `VideoRepository` pre-warms the media index in the background,
 *   then triggers `onSplashFinished()` so `MainActivity` smoothly cross-fades (300ms)
 *   into `DirectoryBrowserScreen`.
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
                Color(0xFF0B101E),
                Color(0xFF101830),
                Color(0xFF0B101E)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFDCEEFF),
                Color(0xFFEEF3FF),
                Color(0xFFF6F8FF)
            )
        )
    }

    val logoShape = RoundedCornerShape(30.dp)
    val logoGlassBg = if (isDark) {
        Color(0xFF131B33).copy(alpha = 0.72f)
    } else {
        Color.White.copy(alpha = 0.76f)
    }
    val logoGlassBorder = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.24f),
                Color(0xFF38BDF8).copy(alpha = 0.22f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.96f),
                Color(0xFFD6E4FF).copy(alpha = 0.85f)
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
        // Soft ambient cerulean-blue radial glow behind the center logo
        Canvas(
            modifier = Modifier
                .size(250.dp)
                .alpha(alphaAnim.value)
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF2563EB).copy(alpha = if (isDark) 0.30f else 0.20f),
                        Color(0xFF0EA5E9).copy(alpha = if (isDark) 0.14f else 0.10f),
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
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .scale(scaleAnim.value)
                    .alpha(alphaAnim.value)
                    .size(124.dp)
                    .shadow(
                        elevation = 18.dp,
                        shape = logoShape,
                        ambientColor = Color(0xFF2563EB).copy(alpha = if (isDark) 0.45f else 0.20f),
                        spotColor = Color(0xFF0EA5E9).copy(alpha = if (isDark) 0.40f else 0.18f)
                    )
                    .clip(logoShape)
                    .background(logoGlassBg)
                    .border(1.dp, logoGlassBorder, logoShape)
                    .padding(8.dp)
                    .testTag("splash_logo_emblem")
            ) {
                Image(
                    painter = painterResource(id = R.drawable.aura_logo),
                    contentDescription = "Aura Player Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(108.dp)
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
