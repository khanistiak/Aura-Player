package com.istiak.auraplayer.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

enum class AppThemePalette(val displayName: String) {
    AURORA("Aurora"),
    DYNAMIC("Dynamic"),
    CATPPUCCIN("Catppuccin"),
    CLOUDFLARE("Cloudflare"),
    COTTON_CANDY("Cotton Candy"),
    DOOM("Doom"),
    EMERALD_GREEN("Emerald Green"),
    AMBER("Amber"),
    CORAL("Coral"),
    SLATE("Slate"),
    DRACULA("Dracula"),
    MOCHA("Mocha"),
    STRAWBERRY("Strawberry"),
    TIDAL("Tidal"),
    NORD("Nord"),
    ROSE_PINE("Rosé Pine"),
    TAKO("Tako");

    companion object {
        fun fromStored(value: String?): AppThemePalette {
            return entries.firstOrNull { it.name == value } ?: AURORA
        }
    }
}

@Immutable
data class ThemePaletteTokens(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outlineVariant: Color,
    val ambientTopLeft: Color,
    val ambientTopRight: Color,
    val ambientBase: Color,
    val sheetBackground: Color,
    val brandTitle: Color,
    val sectionHeader: Color
) {
    fun toColorScheme(isDark: Boolean): ColorScheme {
        return if (isDark) {
            darkColorScheme(
                primary = primary,
                onPrimary = onPrimary,
                primaryContainer = primaryContainer,
                onPrimaryContainer = onPrimaryContainer,
                secondary = secondary,
                onSecondary = onSecondary,
                secondaryContainer = secondaryContainer,
                onSecondaryContainer = onSecondaryContainer,
                background = background,
                onBackground = onSurface,
                surface = surface,
                onSurface = onSurface,
                surfaceVariant = surfaceVariant,
                onSurfaceVariant = onSurfaceVariant,
                outlineVariant = outlineVariant,
                error = DangerRed,
                onError = Color.White
            )
        } else {
            lightColorScheme(
                primary = primary,
                onPrimary = onPrimary,
                primaryContainer = primaryContainer,
                onPrimaryContainer = onPrimaryContainer,
                secondary = secondary,
                onSecondary = onSecondary,
                secondaryContainer = secondaryContainer,
                onSecondaryContainer = onSecondaryContainer,
                background = background,
                onBackground = onSurface,
                surface = surface,
                onSurface = onSurface,
                surfaceVariant = surfaceVariant,
                onSurfaceVariant = onSurfaceVariant,
                outlineVariant = outlineVariant,
                error = DangerRed,
                onError = Color.White
            )
        }
    }
}

fun resolvePaletteTokens(
    palette: AppThemePalette,
    isDark: Boolean,
    context: Context? = null
): ThemePaletteTokens {
    if (palette == AppThemePalette.DYNAMIC &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        context != null
    ) {
        val scheme = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        return ThemePaletteTokens(
            primary = scheme.primary,
            onPrimary = scheme.onPrimary,
            primaryContainer = scheme.primaryContainer,
            onPrimaryContainer = scheme.onPrimaryContainer,
            secondary = scheme.secondary,
            onSecondary = scheme.onSecondary,
            secondaryContainer = scheme.secondaryContainer,
            onSecondaryContainer = scheme.onSecondaryContainer,
            background = scheme.background,
            surface = scheme.surface,
            surfaceVariant = scheme.surfaceVariant,
            onSurface = scheme.onSurface,
            onSurfaceVariant = scheme.onSurfaceVariant,
            outlineVariant = scheme.outlineVariant,
            ambientTopLeft = blendColor(scheme.background, scheme.primaryContainer, if (isDark) 0.34f else 0.52f),
            ambientTopRight = blendColor(scheme.background, scheme.secondaryContainer, if (isDark) 0.32f else 0.48f),
            ambientBase = scheme.background,
            sheetBackground = if (isDark) blendColor(scheme.surface, Color.White, 0.04f) else Color.White,
            brandTitle = if (isDark) scheme.primary else blendColor(scheme.primary, scheme.onSurface, 0.30f),
            sectionHeader = scheme.primary
        )
    }

    return if (isDark) darkTokensFor(palette) else lightTokensFor(palette)
}

private fun blendColor(a: Color, b: Color, ratioB: Float): Color {
    val t = ratioB.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * t,
        green = a.green + (b.green - a.green) * t,
        blue = a.blue + (b.blue - a.blue) * t,
        alpha = a.alpha + (b.alpha - a.alpha) * t
    )
}

private fun lightTokensFor(palette: AppThemePalette): ThemePaletteTokens = when (palette) {
    AppThemePalette.AURORA, AppThemePalette.DYNAMIC -> ThemePaletteTokens(
        primary = Color(0xFF2563EB),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFDDE6FF),
        onPrimaryContainer = Color(0xFF1E3A8A),
        secondary = PeriwinkleSecondary,
        onSecondary = OnPeriwinkleText,
        secondaryContainer = Color(0xFFE3E6FA),
        onSecondaryContainer = Color(0xFF1E2B4D),
        background = Color(0xFFEAF0FF),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = NeutralChipBgLight,
        onSurface = PrimaryTextLight,
        onSurfaceVariant = SecondaryTextLight,
        outlineVariant = Color(0xFFDCE4FA),
        ambientTopLeft = Color(0xFFDFE7FF),
        ambientTopRight = Color(0xFFEBF0FF),
        ambientBase = Color(0xFFF3F6FF),
        sheetBackground = SheetSurfaceLight,
        brandTitle = SlateIndigoPrimary,
        sectionHeader = RoyalBlueAccent
    )

    AppThemePalette.CATPPUCCIN -> ThemePaletteTokens(
        primary = Color(0xFF7287FD),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE0E5FF),
        onPrimaryContainer = Color(0xFF333F8C),
        secondary = Color(0xFF8839EF),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFEAE2FF),
        onSecondaryContainer = Color(0xFF42276E),
        background = Color(0xFFEFF1F5),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFE6E9EF),
        onSurface = Color(0xFF4C4F69),
        onSurfaceVariant = Color(0xFF6C6F85),
        outlineVariant = Color(0xFFDCE0E8),
        ambientTopLeft = Color(0xFFDCE4FF),
        ambientTopRight = Color(0xFFEFE2FF),
        ambientBase = Color(0xFFEFF1F5),
        sheetBackground = Color(0xFFFFFFFF),
        brandTitle = Color(0xFF4C4F69),
        sectionHeader = Color(0xFF7287FD)
    )

    AppThemePalette.CLOUDFLARE -> ThemePaletteTokens(
        primary = Color(0xFFF38020),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFE8D4),
        onPrimaryContainer = Color(0xFF8A3D00),
        secondary = Color(0xFFD97706),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFFFF1DB),
        onSecondaryContainer = Color(0xFF78350F),
        background = Color(0xFFFFFBF5),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFF7EFE6),
        onSurface = Color(0xFF29211A),
        onSurfaceVariant = Color(0xFF6E5C4E),
        outlineVariant = Color(0xFFF2DFCC),
        ambientTopLeft = Color(0xFFFFEDD9),
        ambientTopRight = Color(0xFFFFF3CC),
        ambientBase = Color(0xFFFFFBF5),
        sheetBackground = Color(0xFFFFFFFF),
        brandTitle = Color(0xFF4A2B12),
        sectionHeader = Color(0xFFD96B14)
    )

    AppThemePalette.COTTON_CANDY -> ThemePaletteTokens(
        primary = Color(0xFF9D4EDD),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFF3E3FF),
        onPrimaryContainer = Color(0xFF5A189A),
        secondary = Color(0xFFEC4899),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFFCE7F3),
        onSecondaryContainer = Color(0xFF831843),
        background = Color(0xFFFDF6FA),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFF6EAF4),
        onSurface = Color(0xFF2B1933),
        onSurfaceVariant = Color(0xFF6E5278),
        outlineVariant = Color(0xFFEBD9F2),
        ambientTopLeft = Color(0xFFFDE2F3),
        ambientTopRight = Color(0xFFEDE4FF),
        ambientBase = Color(0xFFFDF6FA),
        sheetBackground = Color(0xFFFFFFFF),
        brandTitle = Color(0xFF4E236D),
        sectionHeader = Color(0xFF9D4EDD)
    )

    AppThemePalette.DOOM -> ThemePaletteTokens(
        primary = Color(0xFFDC2626),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFEE2E2),
        onPrimaryContainer = Color(0xFF7F1D1D),
        secondary = Color(0xFFB91C1C),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFFCE8E8),
        onSecondaryContainer = Color(0xFF691818),
        background = Color(0xFFFDF6F6),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFF6EAEA),
        onSurface = Color(0xFF261414),
        onSurfaceVariant = Color(0xFF6B4C4C),
        outlineVariant = Color(0xFFF2D6D6),
        ambientTopLeft = Color(0xFFFFE2E2),
        ambientTopRight = Color(0xFFFFE8DD),
        ambientBase = Color(0xFFFDF6F6),
        sheetBackground = Color(0xFFFFFFFF),
        brandTitle = Color(0xFF5E1717),
        sectionHeader = Color(0xFFDC2626)
    )

    AppThemePalette.EMERALD_GREEN -> ThemePaletteTokens(
        primary = Color(0xFF15803D),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFDCFCE7),
        onPrimaryContainer = Color(0xFF14532D),
        secondary = Color(0xFF059669),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE0F7EC),
        onSecondaryContainer = Color(0xFF064E3B),
        background = Color(0xFFF4FBF6),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFE8F4EC),
        onSurface = Color(0xFF132619),
        onSurfaceVariant = Color(0xFF4A6653),
        outlineVariant = Color(0xFFD0EAD8),
        ambientTopLeft = Color(0xFFDCFCE7),
        ambientTopRight = Color(0xFFE0F7F0),
        ambientBase = Color(0xFFF4FBF6),
        sheetBackground = Color(0xFFFFFFFF),
        brandTitle = Color(0xFF144B2A),
        sectionHeader = Color(0xFF15803D)
    )

    AppThemePalette.AMBER -> ThemePaletteTokens(
        primary = Color(0xFFB45309),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFEF3C7),
        onPrimaryContainer = Color(0xFF78350F),
        secondary = Color(0xFFD97706),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFFEF5D6),
        onSecondaryContainer = Color(0xFF6B320E),
        background = Color(0xFFFFFDF5),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFF7F1E1),
        onSurface = Color(0xFF281F10),
        onSurfaceVariant = Color(0xFF6C5B42),
        outlineVariant = Color(0xFFF2E2BE),
        ambientTopLeft = Color(0xFFFEF3C7),
        ambientTopRight = Color(0xFFFFEDD5),
        ambientBase = Color(0xFFFFFDF5),
        sheetBackground = Color(0xFFFFFFFF),
        brandTitle = Color(0xFF5C310C),
        sectionHeader = Color(0xFFB45309)
    )

    AppThemePalette.CORAL -> ThemePaletteTokens(
        primary = Color(0xFFC2410C),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFEDD5),
        onPrimaryContainer = Color(0xFF7C2D12),
        secondary = Color(0xFFE11D48),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFFFE4E6),
        onSecondaryContainer = Color(0xFF881337),
        background = Color(0xFFFFF7F5),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFF8ECE8),
        onSurface = Color(0xFF2A1815),
        onSurfaceVariant = Color(0xFF6E504B),
        outlineVariant = Color(0xFFF3DAD3),
        ambientTopLeft = Color(0xFFFFE8E0),
        ambientTopRight = Color(0xFFFFECE5),
        ambientBase = Color(0xFFFFF7F5),
        sheetBackground = Color(0xFFFFFFFF),
        brandTitle = Color(0xFF5D2614),
        sectionHeader = Color(0xFFC2410C)
    )

    AppThemePalette.SLATE -> ThemePaletteTokens(
        primary = Color(0xFF334155),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE2E8F0),
        onPrimaryContainer = Color(0xFF0F172A),
        secondary = Color(0xFF475569),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE8EEF5),
        onSecondaryContainer = Color(0xFF1E293B),
        background = Color(0xFFF8FAFC),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFEBF0F5),
        onSurface = Color(0xFF0F172A),
        onSurfaceVariant = Color(0xFF526075),
        outlineVariant = Color(0xFFD8E1EC),
        ambientTopLeft = Color(0xFFE2E8F0),
        ambientTopRight = Color(0xFFECF1F7),
        ambientBase = Color(0xFFF8FAFC),
        sheetBackground = Color(0xFFFFFFFF),
        brandTitle = Color(0xFF1E293B),
        sectionHeader = Color(0xFF334155)
    )

    AppThemePalette.DRACULA -> ThemePaletteTokens(
        primary = Color(0xFF6272A4),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE4E7F5),
        onPrimaryContainer = Color(0xFF282A36),
        secondary = Color(0xFF7C3AED),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFEDE8FA),
        onSecondaryContainer = Color(0xFF3B1F70),
        background = Color(0xFFF7F7FC),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFECEEF6),
        onSurface = Color(0xFF282A36),
        onSurfaceVariant = Color(0xFF5A6078),
        outlineVariant = Color(0xFFDADFF0),
        ambientTopLeft = Color(0xFFE3E7F7),
        ambientTopRight = Color(0xFFEFE4FA),
        ambientBase = Color(0xFFF7F7FC),
        sheetBackground = Color(0xFFFFFFFF),
        brandTitle = Color(0xFF343B59),
        sectionHeader = Color(0xFF6272A4)
    )

    AppThemePalette.MOCHA -> ThemePaletteTokens(
        primary = Color(0xFF6F4E37),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFEFE4DA),
        onPrimaryContainer = Color(0xFF3E2723),
        secondary = Color(0xFF8D6E63),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFF3EAE3),
        onSecondaryContainer = Color(0xFF4E342E),
        background = Color(0xFFFAF6F2),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFF1E9E2),
        onSurface = Color(0xFF291F1A),
        onSurfaceVariant = Color(0xFF69564C),
        outlineVariant = Color(0xFFE8DBD0),
        ambientTopLeft = Color(0xFFF3E8DE),
        ambientTopRight = Color(0xFFF5ECE2),
        ambientBase = Color(0xFFFAF6F2),
        sheetBackground = Color(0xFFFFFFFF),
        brandTitle = Color(0xFF4B3222),
        sectionHeader = Color(0xFF6F4E37)
    )

    AppThemePalette.STRAWBERRY -> ThemePaletteTokens(
        primary = Color(0xFFBE123C),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFE4E6),
        onPrimaryContainer = Color(0xFF881337),
        secondary = Color(0xFFE11D48),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFFCE7F3),
        onSecondaryContainer = Color(0xFF831843),
        background = Color(0xFFFFF5F7),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFF9E8EC),
        onSurface = Color(0xFF2B131A),
        onSurfaceVariant = Color(0xFF704854),
        outlineVariant = Color(0xFFF5D3DB),
        ambientTopLeft = Color(0xFFFFE0E9),
        ambientTopRight = Color(0xFFFCE7F3),
        ambientBase = Color(0xFFFFF5F7),
        sheetBackground = Color(0xFFFFFFFF),
        brandTitle = Color(0xFF6B1028),
        sectionHeader = Color(0xFFBE123C)
    )

    AppThemePalette.TIDAL -> ThemePaletteTokens(
        primary = Color(0xFF0F766E),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFCCFBF1),
        onPrimaryContainer = Color(0xFF134E4A),
        secondary = Color(0xFF0D9488),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFD8FAF3),
        onSecondaryContainer = Color(0xFF115E59),
        background = Color(0xFFF2FBF9),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFE5F3F0),
        onSurface = Color(0xFF112624),
        onSurfaceVariant = Color(0xFF476662),
        outlineVariant = Color(0xFFCDE9E4),
        ambientTopLeft = Color(0xFFD2F7EF),
        ambientTopRight = Color(0xFFDDF6FA),
        ambientBase = Color(0xFFF2FBF9),
        sheetBackground = Color(0xFFFFFFFF),
        brandTitle = Color(0xFF114B47),
        sectionHeader = Color(0xFF0F766E)
    )

    AppThemePalette.NORD -> ThemePaletteTokens(
        primary = Color(0xFF3B6E8C),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD8E4EE),
        onPrimaryContainer = Color(0xFF2E3440),
        secondary = Color(0xFF5E81AC),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE1E8F2),
        onSecondaryContainer = Color(0xFF2E3440),
        background = Color(0xFFECEFF4),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFE5E9F0),
        onSurface = Color(0xFF2E3440),
        onSurfaceVariant = Color(0xFF4C566A),
        outlineVariant = Color(0xFFD8DEE9),
        ambientTopLeft = Color(0xFFDAE5F0),
        ambientTopRight = Color(0xFFE2E9F2),
        ambientBase = Color(0xFFECEFF4),
        sheetBackground = Color(0xFFFFFFFF),
        brandTitle = Color(0xFF2E3440),
        sectionHeader = Color(0xFF3B6E8C)
    )

    AppThemePalette.ROSE_PINE -> ThemePaletteTokens(
        primary = Color(0xFF907AA9),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFEFE6F5),
        onPrimaryContainer = Color(0xFF575279),
        secondary = Color(0xFFD7827E),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFF7E7E6),
        onSecondaryContainer = Color(0xFF6B3E3B),
        background = Color(0xFFFAF4ED),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFF2E9E1),
        onSurface = Color(0xFF575279),
        onSurfaceVariant = Color(0xFF797593),
        outlineVariant = Color(0xFFE8DDD5),
        ambientTopLeft = Color(0xFFF2E9E4),
        ambientTopRight = Color(0xFFEFE6F4),
        ambientBase = Color(0xFFFAF4ED),
        sheetBackground = Color(0xFFFFFFFF),
        brandTitle = Color(0xFF575279),
        sectionHeader = Color(0xFF907AA9)
    )

    AppThemePalette.TAKO -> ThemePaletteTokens(
        primary = Color(0xFF3F6212),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE5F2CE),
        onPrimaryContainer = Color(0xFF1A2E05),
        secondary = Color(0xFF65A30D),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFECF6D8),
        onSecondaryContainer = Color(0xFF2D460B),
        background = Color(0xFFF7FAF2),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFEAF2DF),
        onSurface = Color(0xFF1B2412),
        onSurfaceVariant = Color(0xFF516140),
        outlineVariant = Color(0xFFD8E6C5),
        ambientTopLeft = Color(0xFFE6F2D1),
        ambientTopRight = Color(0xFFECF4DA),
        ambientBase = Color(0xFFF7FAF2),
        sheetBackground = Color(0xFFFFFFFF),
        brandTitle = Color(0xFF29400B),
        sectionHeader = Color(0xFF3F6212)
    )
}

private fun darkTokensFor(palette: AppThemePalette): ThemePaletteTokens = when (palette) {
    AppThemePalette.AURORA, AppThemePalette.DYNAMIC -> ThemePaletteTokens(
        primary = Color(0xFF60A5FA),
        onPrimary = Color(0xFF0A1429),
        primaryContainer = Color(0xFF1E3A5F),
        onPrimaryContainer = Color(0xFFDBEAFE),
        secondary = PeriwinkleDarkContainer,
        onSecondary = OnPeriwinkleDarkText,
        secondaryContainer = Color(0xFF1E293B),
        onSecondaryContainer = Color(0xFFDBEAFE),
        background = Color(0xFF0B132B),
        surface = Color(0xFF161B2E),
        surfaceVariant = NeutralChipBgDark,
        onSurface = PrimaryTextDark,
        onSurfaceVariant = SecondaryTextDark,
        outlineVariant = Color(0xFF293552),
        ambientTopLeft = AmbientNavyTop,
        ambientTopRight = AmbientVioletTopRight,
        ambientBase = AmbientObsidianBottom,
        sheetBackground = SheetSurfaceDark,
        brandTitle = SlateIndigoDarkPrimary,
        sectionHeader = RoyalBlueDarkAccent
    )

    AppThemePalette.CATPPUCCIN -> ThemePaletteTokens(
        primary = Color(0xFFCBA6F7),
        onPrimary = Color(0xFF1E1E2E),
        primaryContainer = Color(0xFF3D315B),
        onPrimaryContainer = Color(0xFFF3E8FF),
        secondary = Color(0xFF89B4FA),
        onSecondary = Color(0xFF1E1E2E),
        secondaryContainer = Color(0xFF2D3856),
        onSecondaryContainer = Color(0xFFDBEAFE),
        background = Color(0xFF1E1E2E),
        surface = Color(0xFF26263A),
        surfaceVariant = Color(0xFF313244),
        onSurface = Color(0xFFCDD6F4),
        onSurfaceVariant = Color(0xFFA6ADC8),
        outlineVariant = Color(0xFF45475A),
        ambientTopLeft = Color(0xFF2B2748),
        ambientTopRight = Color(0xFF38254A),
        ambientBase = Color(0xFF1E1E2E),
        sheetBackground = Color(0xFF242437),
        brandTitle = Color(0xFFCBA6F7),
        sectionHeader = Color(0xFFCBA6F7)
    )

    AppThemePalette.CLOUDFLARE -> ThemePaletteTokens(
        primary = Color(0xFFFB923C),
        onPrimary = Color(0xFF291404),
        primaryContainer = Color(0xFF4E2B11),
        onPrimaryContainer = Color(0xFFFFEDD5),
        secondary = Color(0xFFFBBF24),
        onSecondary = Color(0xFF261A05),
        secondaryContainer = Color(0xFF423114),
        onSecondaryContainer = Color(0xFFFEF3C7),
        background = Color(0xFF171512),
        surface = Color(0xFF221E19),
        surfaceVariant = Color(0xFF2E2821),
        onSurface = Color(0xFFF5ECE3),
        onSurfaceVariant = Color(0xFFB8A99A),
        outlineVariant = Color(0xFF443A2F),
        ambientTopLeft = Color(0xFF362112),
        ambientTopRight = Color(0xFF2F2514),
        ambientBase = Color(0xFF171512),
        sheetBackground = Color(0xFF211D18),
        brandTitle = Color(0xFFFB923C),
        sectionHeader = Color(0xFFFB923C)
    )

    AppThemePalette.COTTON_CANDY -> ThemePaletteTokens(
        primary = Color(0xFFF472B6),
        onPrimary = Color(0xFF2E0D22),
        primaryContainer = Color(0xFF4E2249),
        onPrimaryContainer = Color(0xFFFCE7F3),
        secondary = Color(0xFFC084FC),
        onSecondary = Color(0xFF240F38),
        secondaryContainer = Color(0xFF3C2356),
        onSecondaryContainer = Color(0xFFF3E8FF),
        background = Color(0xFF1B121E),
        surface = Color(0xFF261A2B),
        surfaceVariant = Color(0xFF33233A),
        onSurface = Color(0xFFF6EAF8),
        onSurfaceVariant = Color(0xFFBDA6C4),
        outlineVariant = Color(0xFF473250),
        ambientTopLeft = Color(0xFF391B3A),
        ambientTopRight = Color(0xFF2B1C42),
        ambientBase = Color(0xFF1B121E),
        sheetBackground = Color(0xFF241828),
        brandTitle = Color(0xFFF472B6),
        sectionHeader = Color(0xFFF472B6)
    )

    AppThemePalette.DOOM -> ThemePaletteTokens(
        primary = Color(0xFFF87171),
        onPrimary = Color(0xFF330D0D),
        primaryContainer = Color(0xFF4F1D1D),
        onPrimaryContainer = Color(0xFFFEE2E2),
        secondary = Color(0xFFEF4444),
        onSecondary = Color(0xFF2D0A0A),
        secondaryContainer = Color(0xFF421A1A),
        onSecondaryContainer = Color(0xFFFECACA),
        background = Color(0xFF181111),
        surface = Color(0xFF241919),
        surfaceVariant = Color(0xFF312222),
        onSurface = Color(0xFFF5E9E9),
        onSurfaceVariant = Color(0xFFBA9E9E),
        outlineVariant = Color(0xFF4A3333),
        ambientTopLeft = Color(0xFF381717),
        ambientTopRight = Color(0xFF2E1A18),
        ambientBase = Color(0xFF181111),
        sheetBackground = Color(0xFF211717),
        brandTitle = Color(0xFFF87171),
        sectionHeader = Color(0xFFF87171)
    )

    AppThemePalette.EMERALD_GREEN -> ThemePaletteTokens(
        primary = Color(0xFF4ADE80),
        onPrimary = Color(0xFF0A2916),
        primaryContainer = Color(0xFF144428),
        onPrimaryContainer = Color(0xFFDCFCE7),
        secondary = Color(0xFF34D399),
        onSecondary = Color(0xFF08261B),
        secondaryContainer = Color(0xFF163A2E),
        onSecondaryContainer = Color(0xFFD1FAE5),
        background = Color(0xFF0E1A12),
        surface = Color(0xFF15251B),
        surfaceVariant = Color(0xFF1E3226),
        onSurface = Color(0xFFE7F5EC),
        onSurfaceVariant = Color(0xFF9BB8A4),
        outlineVariant = Color(0xFF2C4A38),
        ambientTopLeft = Color(0xFF13311F),
        ambientTopRight = Color(0xFF142B27),
        ambientBase = Color(0xFF0E1A12),
        sheetBackground = Color(0xFF132219),
        brandTitle = Color(0xFF4ADE80),
        sectionHeader = Color(0xFF4ADE80)
    )

    AppThemePalette.AMBER -> ThemePaletteTokens(
        primary = Color(0xFFFBBF24),
        onPrimary = Color(0xFF291D04),
        primaryContainer = Color(0xFF4A3410),
        onPrimaryContainer = Color(0xFFFEF3C7),
        secondary = Color(0xFFF59E0B),
        onSecondary = Color(0xFF261904),
        secondaryContainer = Color(0xFF3D2C10),
        onSecondaryContainer = Color(0xFFFDE68A),
        background = Color(0xFF1A150C),
        surface = Color(0xFF261F13),
        surfaceVariant = Color(0xFF332A1B),
        onSurface = Color(0xFFF7F1E5),
        onSurfaceVariant = Color(0xFFBBA98E),
        outlineVariant = Color(0xFF4A3D28),
        ambientTopLeft = Color(0xFF352710),
        ambientTopRight = Color(0xFF2E2212),
        ambientBase = Color(0xFF1A150C),
        sheetBackground = Color(0xFF231C11),
        brandTitle = Color(0xFFFBBF24),
        sectionHeader = Color(0xFFFBBF24)
    )

    AppThemePalette.CORAL -> ThemePaletteTokens(
        primary = Color(0xFFFB7185),
        onPrimary = Color(0xFF330C14),
        primaryContainer = Color(0xFF4D1E26),
        onPrimaryContainer = Color(0xFFFFE4E6),
        secondary = Color(0xFFFB923C),
        onSecondary = Color(0xFF2D1405),
        secondaryContainer = Color(0xFF472415),
        onSecondaryContainer = Color(0xFFFFEDD5),
        background = Color(0xFF1C1211),
        surface = Color(0xFF281A18),
        surfaceVariant = Color(0xFF362422),
        onSurface = Color(0xFFF7EBE9),
        onSurfaceVariant = Color(0xFFBEA09C),
        outlineVariant = Color(0xFF4C3330),
        ambientTopLeft = Color(0xFF381B1D),
        ambientTopRight = Color(0xFF311A24),
        ambientBase = Color(0xFF1C1211),
        sheetBackground = Color(0xFF241816),
        brandTitle = Color(0xFFFB7185),
        sectionHeader = Color(0xFFFB7185)
    )

    AppThemePalette.SLATE -> ThemePaletteTokens(
        primary = Color(0xFF94A3B8),
        onPrimary = Color(0xFF0F172A),
        primaryContainer = Color(0xFF28354E),
        onPrimaryContainer = Color(0xFFE2E8F0),
        secondary = Color(0xFFCBD5E1),
        onSecondary = Color(0xFF0F172A),
        secondaryContainer = Color(0xFF1E293B),
        onSecondaryContainer = Color(0xFFF1F5F9),
        background = Color(0xFF0F172A),
        surface = Color(0xFF182238),
        surfaceVariant = Color(0xFF222E48),
        onSurface = Color(0xFFF1F5F9),
        onSurfaceVariant = Color(0xFF94A3B8),
        outlineVariant = Color(0xFF334155),
        ambientTopLeft = Color(0xFF1C2942),
        ambientTopRight = Color(0xFF1E263D),
        ambientBase = Color(0xFF0F172A),
        sheetBackground = Color(0xFF161F33),
        brandTitle = Color(0xFFE2E8F0),
        sectionHeader = Color(0xFF94A3B8)
    )

    AppThemePalette.DRACULA -> ThemePaletteTokens(
        primary = Color(0xFFBD93F9),
        onPrimary = Color(0xFF1E1A2E),
        primaryContainer = Color(0xFF443B68),
        onPrimaryContainer = Color(0xFFF3E8FF),
        secondary = Color(0xFFFF79C6),
        onSecondary = Color(0xFF2D1223),
        secondaryContainer = Color(0xFF4E2A48),
        onSecondaryContainer = Color(0xFFFCE7F3),
        background = Color(0xFF282A36),
        surface = Color(0xFF313443),
        surfaceVariant = Color(0xFF3C3F52),
        onSurface = Color(0xFFF8F8F2),
        onSurfaceVariant = Color(0xFFAEB4D0),
        outlineVariant = Color(0xFF4D516A),
        ambientTopLeft = Color(0xFF34324E),
        ambientTopRight = Color(0xFF3C2F52),
        ambientBase = Color(0xFF282A36),
        sheetBackground = Color(0xFF2E3140),
        brandTitle = Color(0xFFBD93F9),
        sectionHeader = Color(0xFFBD93F9)
    )

    AppThemePalette.MOCHA -> ThemePaletteTokens(
        primary = Color(0xFFD4A373),
        onPrimary = Color(0xFF281B10),
        primaryContainer = Color(0xFF473527),
        onPrimaryContainer = Color(0xFFF7EAE0),
        secondary = Color(0xFFBC8A5F),
        onSecondary = Color(0xFF26170C),
        secondaryContainer = Color(0xFF3D2D21),
        onSecondaryContainer = Color(0xFFF3E4D7),
        background = Color(0xFF1C1613),
        surface = Color(0xFF28201B),
        surfaceVariant = Color(0xFF352A24),
        onSurface = Color(0xFFF5EDE6),
        onSurfaceVariant = Color(0xFFB8A598),
        outlineVariant = Color(0xFF4C3D34),
        ambientTopLeft = Color(0xFF33251D),
        ambientTopRight = Color(0xFF2D221A),
        ambientBase = Color(0xFF1C1613),
        sheetBackground = Color(0xFF251D18),
        brandTitle = Color(0xFFD4A373),
        sectionHeader = Color(0xFFD4A373)
    )

    AppThemePalette.STRAWBERRY -> ThemePaletteTokens(
        primary = Color(0xFFFB7185),
        onPrimary = Color(0xFF320B13),
        primaryContainer = Color(0xFF501B29),
        onPrimaryContainer = Color(0xFFFFE4E6),
        secondary = Color(0xFFF43F5E),
        onSecondary = Color(0xFF2E0810),
        secondaryContainer = Color(0xFF471824),
        onSecondaryContainer = Color(0xFFFECDD3),
        background = Color(0xFF1D1014),
        surface = Color(0xFF2A171D),
        surfaceVariant = Color(0xFF382027),
        onSurface = Color(0xFFF8EAEF),
        onSurfaceVariant = Color(0xFFC09CA6),
        outlineVariant = Color(0xFF4E2E38),
        ambientTopLeft = Color(0xFF391620),
        ambientTopRight = Color(0xFF341828),
        ambientBase = Color(0xFF1D1014),
        sheetBackground = Color(0xFF26151A),
        brandTitle = Color(0xFFFB7185),
        sectionHeader = Color(0xFFFB7185)
    )

    AppThemePalette.TIDAL -> ThemePaletteTokens(
        primary = Color(0xFF2DD4BF),
        onPrimary = Color(0xFF072623),
        primaryContainer = Color(0xFF13433E),
        onPrimaryContainer = Color(0xFFCCFBF1),
        secondary = Color(0xFF38BDF8),
        onSecondary = Color(0xFF082233),
        secondaryContainer = Color(0xFF16384B),
        onSecondaryContainer = Color(0xFFE0F2FE),
        background = Color(0xFF0C1A19),
        surface = Color(0xFF142625),
        surfaceVariant = Color(0xFF1D3332),
        onSurface = Color(0xFFE6F5F3),
        onSurfaceVariant = Color(0xFF96B8B4),
        outlineVariant = Color(0xFF2B4B49),
        ambientTopLeft = Color(0xFF103230),
        ambientTopRight = Color(0xFF112D33),
        ambientBase = Color(0xFF0C1A19),
        sheetBackground = Color(0xFF122322),
        brandTitle = Color(0xFF2DD4BF),
        sectionHeader = Color(0xFF2DD4BF)
    )

    AppThemePalette.NORD -> ThemePaletteTokens(
        primary = Color(0xFF88C0D0),
        onPrimary = Color(0xFF1D2430),
        primaryContainer = Color(0xFF2E4A59),
        onPrimaryContainer = Color(0xFFD8F1F8),
        secondary = Color(0xFF81A1C1),
        onSecondary = Color(0xFF1D2430),
        secondaryContainer = Color(0xFF38475C),
        onSecondaryContainer = Color(0xFFE5EFF8),
        background = Color(0xFF2E3440),
        surface = Color(0xFF3B4252),
        surfaceVariant = Color(0xFF434C5E),
        onSurface = Color(0xFFECEFF4),
        onSurfaceVariant = Color(0xFFD8DEE9),
        outlineVariant = Color(0xFF4C566A),
        ambientTopLeft = Color(0xFF334354),
        ambientTopRight = Color(0xFF374054),
        ambientBase = Color(0xFF2E3440),
        sheetBackground = Color(0xFF353C4A),
        brandTitle = Color(0xFF88C0D0),
        sectionHeader = Color(0xFF88C0D0)
    )

    AppThemePalette.ROSE_PINE -> ThemePaletteTokens(
        primary = Color(0xFFEBBCBA),
        onPrimary = Color(0xFF2A1920),
        primaryContainer = Color(0xFF463547),
        onPrimaryContainer = Color(0xFFFBE9E8),
        secondary = Color(0xFFC4A7E7),
        onSecondary = Color(0xFF221A33),
        secondaryContainer = Color(0xFF393052),
        onSecondaryContainer = Color(0xFFF1E8FF),
        background = Color(0xFF191724),
        surface = Color(0xFF232033),
        surfaceVariant = Color(0xFF2D2A40),
        onSurface = Color(0xFFE0DEF4),
        onSurfaceVariant = Color(0xFF908CAA),
        outlineVariant = Color(0xFF403D52),
        ambientTopLeft = Color(0xFF2B2336),
        ambientTopRight = Color(0xFF32243A),
        ambientBase = Color(0xFF191724),
        sheetBackground = Color(0xFF201D2E),
        brandTitle = Color(0xFFEBBCBA),
        sectionHeader = Color(0xFFEBBCBA)
    )

    AppThemePalette.TAKO -> ThemePaletteTokens(
        primary = Color(0xFFA3E635),
        onPrimary = Color(0xFF182605),
        primaryContainer = Color(0xFF2F4518),
        onPrimaryContainer = Color(0xFFECFCCB),
        secondary = Color(0xFF84CC16),
        onSecondary = Color(0xFF142104),
        secondaryContainer = Color(0xFF293B15),
        onSecondaryContainer = Color(0xFFD9F99D),
        background = Color(0xFF141A10),
        surface = Color(0xFF1E2618),
        surfaceVariant = Color(0xFF283321),
        onSurface = Color(0xFFEEF5E6),
        onSurfaceVariant = Color(0xFFA2B593),
        outlineVariant = Color(0xFF39492F),
        ambientTopLeft = Color(0xFF243217),
        ambientTopRight = Color(0xFF1E2C18),
        ambientBase = Color(0xFF141A10),
        sheetBackground = Color(0xFF1B2316),
        brandTitle = Color(0xFFA3E635),
        sectionHeader = Color(0xFFA3E635)
    )
}
