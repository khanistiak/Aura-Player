package com.istiak.auraplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.istiak.auraplayer.ui.components.FrostedBackdropState
import com.istiak.auraplayer.ui.components.LocalFrostedBackdropState
import com.istiak.auraplayer.ui.theme.AppThemePalette
import com.istiak.auraplayer.ui.theme.AuraPlayerTheme
import com.istiak.auraplayer.ui.theme.LocalExtendedPalette

/**
 * Bottom Navigation Bar destinations:
 * Contains ONLY two public tabs (Home and All Videos). Private Vault is hidden from
 * the bottom navigation bar and accessed discreetly via the top-bar Vault icon.
 */
enum class MainNavTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_tab_home"),
    ALL_VIDEOS("All Videos", Icons.Filled.VideoLibrary, Icons.Outlined.VideoLibrary, "nav_tab_all_videos")
}

@Composable
fun MaterialYouTheme(
    darkTheme: Boolean = false,
    themePalette: AppThemePalette = AppThemePalette.AURORA,
    content: @Composable () -> Unit
) {
    val contentGraphicsLayer = rememberGraphicsLayer()
    val backgroundGraphicsLayer = rememberGraphicsLayer()
    val backdropState = remember(contentGraphicsLayer, backgroundGraphicsLayer) {
        FrostedBackdropState(
            contentGraphicsLayer = contentGraphicsLayer,
            backgroundGraphicsLayer = backgroundGraphicsLayer
        )
    }
    CompositionLocalProvider(LocalFrostedBackdropState provides backdropState) {
        AuraPlayerTheme(
            darkTheme = darkTheme,
            themePalette = themePalette,
            content = content
        )
    }
}

/**
 * Transparent screen container that preserves the single unified root theme background painted once
 * in [AuraPlayerTheme], preventing any background color or tint shift during screen navigation transitions.
 */
@Composable
fun PastelMeshBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent),
        content = content
    )
}

/**
 * Refined Neutral Metadata Chip (#EEF3FA with #475569 text in Light Mode) or Accent Chip.
 */
@Composable
fun MetadataChip(
    text: String,
    modifier: Modifier = Modifier,
    isAccent: Boolean = false
) {
    val palette = LocalExtendedPalette.current
    val bgColor = if (isAccent) palette.periwinkleBadgeBg else palette.neutralChipBg
    val textColor = if (isAccent) palette.periwinkleBadgeText else palette.neutralChipText

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(7.dp))
            .background(bgColor)
            .padding(horizontal = 7.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = if (isAccent) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

/**
 * Refined Green SRT Subtitle Badge (#E8F5E9 with #2E7D32 text in Light Mode).
 */
@Composable
fun SrtSubtitleBadge(
    text: String = "SRT",
    modifier: Modifier = Modifier
) {
    val palette = LocalExtendedPalette.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(palette.srtBadgeBg)
            .padding(horizontal = 6.dp, vertical = 2.5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = palette.srtBadgeText,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.3.sp
        )
    }
}
