package com.istiak.auraplayer

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.istiak.auraplayer.ui.components.FrostedBackdropState
import com.istiak.auraplayer.ui.components.LocalFrostedBackdropState
import com.istiak.auraplayer.ui.components.recordFrostedBackground
import com.istiak.auraplayer.ui.theme.AmbientLilacLavenderTopRight
import com.istiak.auraplayer.ui.theme.AmbientNavyTop
import com.istiak.auraplayer.ui.theme.AmbientObsidianBottom
import com.istiak.auraplayer.ui.theme.AmbientSkyBlueTop
import com.istiak.auraplayer.ui.theme.AmbientSoftOffWhiteBase
import com.istiak.auraplayer.ui.theme.AmbientVioletTopRight
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
            content = content
        )
    }
}

/**
 * Full-Bleed Pastel Sky-Blue to Soft-Lavender Ambient Background Canvas:
 * - Light Mode: top-left soft icy sky-blue bloom (`#DCEEFF`), top-right soft pastel
 *   lilac-lavender bloom (`#ECE2FF`), blending smoothly into airy soft off-white (`#F8F9FE`)
 *   across the middle and bottom of the screen.
 * - Dark Mode: deep obsidian-indigo gradient canvas (`#0B0E1A` base with `#172546` top-left
 *   and `#281D45` top-right blooms).
 * - Attaches `.recordFrostedBackground(backdropState)` to the ambient `Canvas` so
 *   `ScrollAwareFixedHeader` and `FrostedGlassSurface` refract both the background gradient
 *   and scrolling thumbnails accurately.
 */
@Composable
fun PastelMeshBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val existingBackdrop = LocalFrostedBackdropState.current
    val fallbackContentLayer = rememberGraphicsLayer()
    val fallbackBackgroundLayer = rememberGraphicsLayer()
    val backdropState = existingBackdrop ?: remember(fallbackContentLayer, fallbackBackgroundLayer) {
        FrostedBackdropState(
            contentGraphicsLayer = fallbackContentLayer,
            backgroundGraphicsLayer = fallbackBackgroundLayer
        )
    }

    val baseColor = if (isDark) AmbientObsidianBottom else AmbientSoftOffWhiteBase
    val topLeftBloom = if (isDark) AmbientNavyTop else AmbientSkyBlueTop
    val topRightBloom = if (isDark) AmbientVioletTopRight else AmbientLilacLavenderTopRight

    CompositionLocalProvider(LocalFrostedBackdropState provides backdropState) {
        Box(
            modifier = modifier.fillMaxSize()
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .recordFrostedBackground(backdropState)
            ) {
                // Base off-white (#F8F9FE) / deep obsidian canvas
                drawRect(color = baseColor)

                val maxDim = size.maxDimension.coerceAtLeast(1f)
                // Top-left soft icy sky-blue bloom (#DCEEFF)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            topLeftBloom,
                            topLeftBloom.copy(alpha = 0.65f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.12f, size.height * 0.06f),
                        radius = maxDim * 0.68f
                    )
                )

                // Top-right soft pastel lilac-lavender bloom (#ECE2FF)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            topRightBloom,
                            topRightBloom.copy(alpha = 0.62f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.90f, size.height * 0.09f),
                        radius = maxDim * 0.64f
                    )
                )

                // Smooth vertical transition into airy soft off-white (#F8F9FE) across middle & bottom
                drawRect(
                    brush = Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.36f to baseColor.copy(alpha = 0.45f),
                        0.68f to baseColor.copy(alpha = 0.92f),
                        1.0f to baseColor
                    )
                )
            }

            content()
        }
    }
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
