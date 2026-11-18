package com.istiak.auraplayer.ui.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.istiak.auraplayer.ui.components.FrostedBackdropState
import com.istiak.auraplayer.ui.components.LocalFrostedBackdropState
import com.istiak.auraplayer.ui.components.recordFrostedBackground

data class ExtendedMaterialPalette(
    val isDark: Boolean,
    val themePalette: AppThemePalette = AppThemePalette.AURORA,
    val appBackground: Color,
    val sheetBackground: Color,
    val brandTitleColor: Color,
    val sectionHeaderBlue: Color,
    val accentPrimary: Color = RoyalBlueAccent,
    val periwinkleBadgeBg: Color,
    val periwinkleBadgeText: Color,
    val neutralChipBg: Color,
    val neutralChipText: Color,
    val srtBadgeBg: Color,
    val srtBadgeText: Color,
    val folderIconTint: Color,
    val folderCountBadgeBg: Color,
    val folderCountBadgeText: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val bottomNavCapsuleBg: Color,
    val bottomNavActivePillBg: Color,
    val ambientTopLeft: Color = Color(0xFFEAF3FB),
    val ambientTopRight: Color = Color(0xFFF3F6FC),
    val ambientBase: Color = Color(0xFFF7F8FD),
    val outlineBorder: Color = Color(0xFFDCE4FA)
)

val LocalExtendedPalette = staticCompositionLocalOf {
    ExtendedMaterialPalette(
        isDark = false,
        themePalette = AppThemePalette.AURORA,
        appBackground = Color(0xFFF3F6FC),
        sheetBackground = SheetSurfaceLight,
        brandTitleColor = SlateIndigoPrimary,
        sectionHeaderBlue = RoyalBlueAccent,
        accentPrimary = RoyalBlueAccent,
        periwinkleBadgeBg = PeriwinkleSecondary,
        periwinkleBadgeText = OnPeriwinkleText,
        neutralChipBg = NeutralChipBgLight,
        neutralChipText = NeutralChipTextLight,
        srtBadgeBg = SrtBadgeBgLight,
        srtBadgeText = SrtBadgeTextLight,
        folderIconTint = FolderIconGray,
        folderCountBadgeBg = PeriwinkleSecondary,
        folderCountBadgeText = OnPeriwinkleText,
        primaryText = PrimaryTextLight,
        secondaryText = SecondaryTextLight,
        bottomNavCapsuleBg = BottomNavCapsuleLight,
        bottomNavActivePillBg = BottomNavActivePillLight,
        ambientTopLeft = Color(0xFFEAF3FB),
        ambientTopRight = Color(0xFFF3F6FC),
        ambientBase = Color(0xFFF7F8FD),
        outlineBorder = Color(0xFFDCE4FA)
    )
}

val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private val LightAmbientBaseTop = Color(0xFFEAF3FB)
private val LightAmbientBaseMid = Color(0xFFF3F6FC)
private val LightAmbientBaseBottom = Color(0xFFF7F8FD)
private val LightDefaultTopLeftGlow = Color(0xFFC7E7F9).copy(alpha = 0.78f)
private val LightDefaultTopRightGlow = Color(0xFFE8D5FA).copy(alpha = 0.72f)

private val DarkAmbientBaseTop = Color(0xFF0A1124)
private val DarkAmbientBaseMid = Color(0xFF0E162D)
private val DarkAmbientBaseBottom = Color(0xFF111A34)
private val DarkDefaultTopLeftGlow = Color(0xFF0C4A6E).copy(alpha = 0.36f)
private val DarkDefaultTopRightGlow = Color(0xFF3B0764).copy(alpha = 0.30f)

private data class AmbientMeshSpec(
    val baseTop: Color,
    val baseMid: Color,
    val baseBottom: Color,
    val midStopFraction: Float,
    val topLeftGlow: Color,
    val topRightGlow: Color
)

private fun resolveAmbientMeshSpec(
    palette: AppThemePalette,
    tokens: ThemePaletteTokens,
    isDark: Boolean,
    context: Context
): AmbientMeshSpec {
    val baseTop = if (isDark) DarkAmbientBaseTop else LightAmbientBaseTop
    val baseMid = if (isDark) DarkAmbientBaseMid else LightAmbientBaseMid
    val baseBottom = if (isDark) DarkAmbientBaseBottom else LightAmbientBaseBottom
    val midStopFraction = if (isDark) 0.40f else 0.38f

    if (palette == AppThemePalette.AURORA) {
        return AmbientMeshSpec(
            baseTop = baseTop,
            baseMid = baseMid,
            baseBottom = baseBottom,
            midStopFraction = midStopFraction,
            topLeftGlow = if (isDark) DarkDefaultTopLeftGlow else LightDefaultTopLeftGlow,
            topRightGlow = if (isDark) DarkDefaultTopRightGlow else LightDefaultTopRightGlow
        )
    }

    val dynamicTertiaryContainer = if (
        palette == AppThemePalette.DYNAMIC &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    ) {
        val scheme = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        scheme.tertiaryContainer
    } else {
        null
    }

    val customPrimaryGlowSource = tokens.primaryContainer
    val customTertiaryGlowSource = dynamicTertiaryContainer ?: tokens.ambientTopRight

    return AmbientMeshSpec(
        baseTop = baseTop,
        baseMid = baseMid,
        baseBottom = baseBottom,
        midStopFraction = midStopFraction,
        topLeftGlow = customPrimaryGlowSource.copy(alpha = if (isDark) 0.36f else 0.72f),
        topRightGlow = customTertiaryGlowSource.copy(alpha = if (isDark) 0.30f else 0.68f)
    )
}

@Composable
fun AuraPlayerTheme(
    darkTheme: Boolean = false,
    themePalette: AppThemePalette = AppThemePalette.AURORA,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val rawTokens = remember(themePalette, darkTheme, context) {
        resolvePaletteTokens(
            palette = themePalette,
            isDark = darkTheme,
            context = context
        )
    }
    val ambientSpec = remember(themePalette, rawTokens, darkTheme, context) {
        resolveAmbientMeshSpec(
            palette = themePalette,
            tokens = rawTokens,
            isDark = darkTheme,
            context = context
        )
    }

    val themeBackground = if (darkTheme) DarkAmbientBaseMid else LightAmbientBaseMid
    val tokens = remember(rawTokens, themeBackground) {
        rawTokens.copy(background = themeBackground)
    }

    val colorScheme = remember(tokens, darkTheme) {
        tokens.toColorScheme(isDark = darkTheme)
    }

    val extendedPalette = remember(
        tokens,
        darkTheme,
        themePalette,
        ambientSpec
    ) {
        ExtendedMaterialPalette(
            isDark = darkTheme,
            themePalette = themePalette,
            appBackground = tokens.background,
            sheetBackground = tokens.sheetBackground,
            brandTitleColor = tokens.brandTitle,
            sectionHeaderBlue = tokens.sectionHeader,
            accentPrimary = tokens.primary,
            periwinkleBadgeBg = tokens.primaryContainer,
            periwinkleBadgeText = tokens.onPrimaryContainer,
            neutralChipBg = tokens.surfaceVariant,
            neutralChipText = tokens.onSurfaceVariant,
            srtBadgeBg = if (darkTheme) SrtBadgeBgDark else SrtBadgeBgLight,
            srtBadgeText = if (darkTheme) SrtBadgeTextDark else SrtBadgeTextLight,
            folderIconTint = if (darkTheme) FolderIconGrayDark else FolderIconGray,
            folderCountBadgeBg = tokens.primaryContainer,
            folderCountBadgeText = tokens.onPrimaryContainer,
            primaryText = tokens.onSurface,
            secondaryText = tokens.onSurfaceVariant,
            bottomNavCapsuleBg = if (darkTheme) tokens.surface.copy(alpha = 0.72f) else BottomNavCapsuleLight,
            bottomNavActivePillBg = tokens.primary.copy(alpha = if (darkTheme) 0.24f else 0.14f),
            ambientTopLeft = ambientSpec.baseTop,
            ambientTopRight = ambientSpec.baseMid,
            ambientBase = ambientSpec.baseBottom,
            outlineBorder = tokens.outlineVariant
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    val existingBackdrop = LocalFrostedBackdropState.current
    val graphicsLayer = rememberGraphicsLayer()
    val backgroundGraphicsLayer = rememberGraphicsLayer()
    val frostedBackdropState = existingBackdrop ?: remember(
        graphicsLayer,
        backgroundGraphicsLayer
    ) {
        FrostedBackdropState(
            graphicsLayer = graphicsLayer,
            backgroundGraphicsLayer = backgroundGraphicsLayer
        )
    }

    val rootAmbientBaseBrush = remember(ambientSpec) {
        Brush.verticalGradient(
            0.0f to ambientSpec.baseTop,
            ambientSpec.midStopFraction to ambientSpec.baseMid,
            1.0f to ambientSpec.baseBottom
        )
    }

    CompositionLocalProvider(
        LocalExtendedPalette provides extendedPalette,
        LocalFrostedBackdropState provides frostedBackdropState
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(tokens.background)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .recordFrostedBackground(frostedBackdropState)
                ) {
                    drawRect(brush = rootAmbientBaseBrush)

                    val w = size.width
                    val h = size.height
                    if (w > 1f && h > 1f) {
                        val glowRadius = w * 0.85f

                        drawRect(
                            brush = Brush.radialGradient(
                                0.0f to ambientSpec.topLeftGlow,
                                0.55f to ambientSpec.topLeftGlow.copy(
                                    alpha = ambientSpec.topLeftGlow.alpha * 0.42f
                                ),
                                1.0f to Color.Transparent,
                                center = Offset(0f, 0f),
                                radius = glowRadius
                            )
                        )

                        drawRect(
                            brush = Brush.radialGradient(
                                0.0f to ambientSpec.topRightGlow,
                                0.55f to ambientSpec.topRightGlow.copy(
                                    alpha = ambientSpec.topRightGlow.alpha * 0.42f
                                ),
                                1.0f to Color.Transparent,
                                center = Offset(w, 0f),
                                radius = glowRadius
                            )
                        )
                    }
                }
                content()
            }
        }
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themePalette: AppThemePalette = AppThemePalette.AURORA,
    content: @Composable () -> Unit
) {
    AuraPlayerTheme(darkTheme = darkTheme, themePalette = themePalette, content = content)
}
