package com.istiak.auraplayer.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

data class ExtendedMaterialPalette(
    val isDark: Boolean,
    val appBackground: Color,
    val sheetBackground: Color,
    val brandTitleColor: Color,
    val sectionHeaderBlue: Color,
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
    val bottomNavActivePillBg: Color
)

val LocalExtendedPalette = staticCompositionLocalOf {
    ExtendedMaterialPalette(
        isDark = false,
        appBackground = LavenderSurfaceBg,
        sheetBackground = SheetSurfaceLight,
        brandTitleColor = SlateIndigoPrimary,
        sectionHeaderBlue = RoyalBlueAccent,
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
        bottomNavActivePillBg = BottomNavActivePillLight
    )
}

private val LightColorScheme = lightColorScheme(
    primary = SlateIndigoPrimary,
    onPrimary = Color.White,
    primaryContainer = PeriwinkleSecondary,
    onPrimaryContainer = OnPeriwinkleText,
    secondary = PeriwinkleSecondary,
    onSecondary = OnPeriwinkleText,
    secondaryContainer = PeriwinkleSecondary,
    onSecondaryContainer = OnPeriwinkleText,
    background = LavenderSurfaceBg,
    onBackground = PrimaryTextLight,
    surface = LavenderSurfaceBg,
    onSurface = PrimaryTextLight,
    surfaceVariant = NeutralChipBgLight,
    onSurfaceVariant = NeutralChipTextLight,
    error = DangerRed,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = SlateIndigoDarkPrimary,
    onPrimary = DarkSurfaceBg,
    primaryContainer = PeriwinkleDarkContainer,
    onPrimaryContainer = OnPeriwinkleDarkText,
    secondary = PeriwinkleDarkContainer,
    onSecondary = OnPeriwinkleDarkText,
    secondaryContainer = PeriwinkleDarkContainer,
    onSecondaryContainer = OnPeriwinkleDarkText,
    background = DarkSurfaceBg,
    onBackground = PrimaryTextDark,
    surface = DarkSurfaceBg,
    onSurface = PrimaryTextDark,
    surfaceVariant = NeutralChipBgDark,
    onSurfaceVariant = NeutralChipTextDark,
    error = DangerRed,
    onError = Color.White
)

val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AuraPlayerTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val extendedPalette = if (darkTheme) {
        ExtendedMaterialPalette(
            isDark = true,
            appBackground = DarkSurfaceBg,
            sheetBackground = SheetSurfaceDark,
            brandTitleColor = SlateIndigoDarkPrimary,
            sectionHeaderBlue = RoyalBlueDarkAccent,
            periwinkleBadgeBg = PeriwinkleDarkContainer,
            periwinkleBadgeText = OnPeriwinkleDarkText,
            neutralChipBg = NeutralChipBgDark,
            neutralChipText = NeutralChipTextDark,
            srtBadgeBg = SrtBadgeBgDark,
            srtBadgeText = SrtBadgeTextDark,
            folderIconTint = FolderIconGrayDark,
            folderCountBadgeBg = PeriwinkleDarkContainer,
            folderCountBadgeText = OnPeriwinkleDarkText,
            primaryText = PrimaryTextDark,
            secondaryText = SecondaryTextDark,
            bottomNavCapsuleBg = BottomNavCapsuleDark,
            bottomNavActivePillBg = BottomNavActivePillDark
        )
    } else {
        ExtendedMaterialPalette(
            isDark = false,
            appBackground = LavenderSurfaceBg,
            sheetBackground = SheetSurfaceLight,
            brandTitleColor = SlateIndigoPrimary,
            sectionHeaderBlue = RoyalBlueAccent,
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
            bottomNavActivePillBg = BottomNavActivePillLight
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

    val existingBackdrop = com.istiak.auraplayer.ui.components.LocalFrostedBackdropState.current
    val contentGraphicsLayer = androidx.compose.ui.graphics.rememberGraphicsLayer()
    val backgroundGraphicsLayer = androidx.compose.ui.graphics.rememberGraphicsLayer()
    val frostedBackdropState = existingBackdrop ?: androidx.compose.runtime.remember(
        contentGraphicsLayer,
        backgroundGraphicsLayer
    ) {
        com.istiak.auraplayer.ui.components.FrostedBackdropState(
            contentGraphicsLayer = contentGraphicsLayer,
            backgroundGraphicsLayer = backgroundGraphicsLayer
        )
    }

    CompositionLocalProvider(
        LocalExtendedPalette provides extendedPalette,
        com.istiak.auraplayer.ui.components.LocalFrostedBackdropState provides frostedBackdropState
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    AuraPlayerTheme(darkTheme = darkTheme, content = content)
}
