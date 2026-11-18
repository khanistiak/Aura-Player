package com.istiak.auraplayer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Gradient
import androidx.compose.material.icons.filled.HdrOn
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LensBlur
import androidx.compose.material.icons.filled.LineWeight
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.istiak.auraplayer.ui.components.FrostedBackdropState
import com.istiak.auraplayer.ui.components.FrostedGlassSurface
import com.istiak.auraplayer.ui.components.MinimalSingleChoiceDialog
import com.istiak.auraplayer.ui.components.ScrollAwareFixedHeader
import com.istiak.auraplayer.ui.components.crystalOuterShadow
import com.istiak.auraplayer.ui.components.recordFrostedBackdrop
import com.istiak.auraplayer.ui.components.rememberScrollAwareGlassLayout
import com.istiak.auraplayer.ui.theme.LocalExtendedPalette
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Real-time Liquid Glass Customization Settings Screen (`LiquidGlassSettingsScreen.kt`):
 * - Styled using Aura Player's native design system:
 *   - `ScrollAwareFixedHeader`: `← Liquid glass`
 *   - Unified `RoundedCornerShape(20.dp)` surface cards with `RoundedCornerShape(12.dp)` icon badges
 *     (`MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)` with `MaterialTheme.colorScheme.primary` icons)
 *   - Uniform transparent row backgrounds (zero ON/OFF ash background shift)
 *   - `MaterialTheme.colorScheme.primary` switches and sliders
 * - Sticky Live Interactive Preview Card (`RoundedCornerShape(20.dp)`) over a pastel checkerboard
 *   containing the interactive **Aura Player** Liquid Glass preview pill (Play button, loop icon,
 *   and live Switch) driven in real time by `LocalLiquidGlassConfig.current`.
 * - Full Sections: `| General`, `| Glass material`, `| Optics`, `| Color`, `| Highlights`,
 *   plus centered `↻ Reset to default`.
 */
@Composable
fun LiquidGlassSettingsScreen(
    config: LiquidGlassConfig = LocalLiquidGlassConfig.current,
    onUpdateEnabled: (Boolean) -> Unit,
    onUpdateHeaderLiquidGlassEnabled: (Boolean) -> Unit = {},
    onUpdateHdrModeEnabled: (Boolean) -> Unit,
    onUpdateFrostPercent: (Float) -> Unit,
    onUpdateBackdropOpacity: (Float) -> Unit,
    onUpdateRefractionEdgeWidth: (Float) -> Unit,
    onUpdateRefractionStrength: (Float) -> Unit,
    onUpdateDepthEffect: (Boolean) -> Unit,
    onUpdateChromaticAberration: (Boolean) -> Unit,
    onUpdateVibrancyEnabled: (Boolean) -> Unit = {},
    onUpdateSaturation: (Float) -> Unit = {},
    onUpdateBrightness: (Float) -> Unit = {},
    onUpdateContrast: (Float) -> Unit = {},
    onUpdateHighlightStyle: (HighlightStyle) -> Unit,
    onUpdateHighlightStrength: (Float) -> Unit,
    onUpdateHighlightWidth: (Float) -> Unit,
    onUpdateHighlightSoftness: (Float) -> Unit,
    onResetToDefault: () -> Unit,
    onShowCapsuleToast: (String) -> Unit = {},
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val liveConfig = LocalLiquidGlassConfig.current
    val effectiveConfig = if (config != LiquidGlassConfig.Default) config else liveConfig
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    var showHighlightStyleDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val navBottomDp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val scrollLayout = rememberScrollAwareGlassLayout(
        listState = listState,
        defaultHeaderHeight = 64.dp,
        extraTopGap = 4.dp,
        bottomPadding = navBottomDp + 24.dp,
        horizontalPadding = 12.dp
    )

    val dividerColor = if (isDark) Color.White.copy(alpha = 0.08f) else palette.outlineBorder.copy(alpha = 0.72f)
    val controlsEnabled = effectiveConfig.enabled

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("liquid_glass_settings_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(scrollLayout.measuredHeaderHeightDp + 2.dp))

            // Sticky Live Interactive Preview Card
            LiquidGlassStickyPreviewSection(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Scrollable Material 3 Uniform Settings Sections
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = 4.dp,
                    bottom = navBottomDp + 28.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .recordFrostedBackdrop(
                        backdropState = scrollLayout.backdropState,
                        scrollableState = listState,
                        contentLayerOverride = scrollLayout.contentGraphicsLayer
                    )
                    .testTag("liquid_glass_settings_list")
            ) {
                // | General
                item(key = "lg_sec_general") {
                    LgSectionCard(title = "| General") {
                        LgUniformSwitchRow(
                            icon = Icons.Filled.AutoAwesome,
                            title = "Liquid glass",
                            subtitle = "Use translucent, refractive surfaces for navigation and headers",
                            checked = effectiveConfig.enabled,
                            onCheckedChange = onUpdateEnabled,
                            testTag = "lg_toggle_enabled"
                        )

                        HorizontalDivider(
                            thickness = 1.dp,
                            color = dividerColor,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )

                        LgUniformSwitchRow(
                            icon = Icons.Filled.VerticalAlignTop,
                            title = "Header liquid glass",
                            subtitle = "Apply refractive Liquid Glass effect to top headers while scrolling",
                            checked = effectiveConfig.headerLiquidGlassEnabled,
                            enabled = controlsEnabled,
                            onCheckedChange = onUpdateHeaderLiquidGlassEnabled,
                            testTag = "lg_toggle_header_liquid_glass"
                        )

                        HorizontalDivider(
                            thickness = 1.dp,
                            color = dividerColor,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )

                        LgUniformSwitchRow(
                            icon = Icons.Filled.HdrOn,
                            title = "Liquid glass in HDR mode",
                            subtitle = "Keep live glass on player controls while HDR output is on",
                            checked = effectiveConfig.hdrModeEnabled,
                            enabled = controlsEnabled,
                            onCheckedChange = onUpdateHdrModeEnabled,
                            testTag = "lg_toggle_hdr_mode"
                        )
                    }
                }

                // | Glass material
                item(key = "lg_sec_material") {
                    LgSectionCard(title = "| Glass material") {
                        LgSliderRow(
                            icon = Icons.Filled.BlurOn,
                            title = "Frost",
                            value = effectiveConfig.frostPercent,
                            valueRange = 0f..2f,
                            enabled = controlsEnabled,
                            onValueChange = onUpdateFrostPercent,
                            testTag = "lg_slider_frost"
                        )
                    }
                }

                // | Optics
                item(key = "lg_sec_optics") {
                    LgSectionCard(title = "| Optics") {
                        LgSliderRow(
                            icon = Icons.Filled.Opacity,
                            title = "Backdrop opacity",
                            value = effectiveConfig.backdropOpacity,
                            valueRange = 0f..1f,
                            enabled = controlsEnabled,
                            onValueChange = onUpdateBackdropOpacity,
                            testTag = "lg_slider_backdrop_opacity"
                        )

                        HorizontalDivider(
                            thickness = 1.dp,
                            color = dividerColor,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )

                        LgSliderRow(
                            icon = Icons.Filled.CenterFocusStrong,
                            title = "Refraction edge width",
                            value = effectiveConfig.refractionEdgeWidth,
                            valueRange = 0f..2f,
                            enabled = controlsEnabled,
                            onValueChange = onUpdateRefractionEdgeWidth,
                            testTag = "lg_slider_refraction_edge_width"
                        )

                        HorizontalDivider(
                            thickness = 1.dp,
                            color = dividerColor,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )

                        LgSliderRow(
                            icon = Icons.Filled.LensBlur,
                            title = "Refraction strength",
                            value = effectiveConfig.refractionStrength,
                            valueRange = 0f..2f,
                            enabled = controlsEnabled,
                            onValueChange = onUpdateRefractionStrength,
                            testTag = "lg_slider_refraction_strength"
                        )

                        HorizontalDivider(
                            thickness = 1.dp,
                            color = dividerColor,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )

                        LgUniformSwitchRow(
                            icon = Icons.Filled.Layers,
                            title = "Depth effect",
                            subtitle = "Add subtle multi-layer inner parallax and recessed shadow depth",
                            checked = effectiveConfig.depthEffect,
                            enabled = controlsEnabled,
                            onCheckedChange = onUpdateDepthEffect,
                            testTag = "lg_toggle_depth_effect"
                        )

                        HorizontalDivider(
                            thickness = 1.dp,
                            color = dividerColor,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )

                        LgUniformSwitchRow(
                            icon = Icons.Filled.ColorLens,
                            title = "Chromatic aberration",
                            subtitle = "Subtle RGB spectral dispersion fringing along refracting glass rims",
                            checked = effectiveConfig.chromaticAberration,
                            enabled = controlsEnabled,
                            onCheckedChange = onUpdateChromaticAberration,
                            testTag = "lg_toggle_chromatic_aberration"
                        )
                    }
                }

                // | Color
                item(key = "lg_sec_color") {
                    LgSectionCard(title = "| Color") {
                        LgUniformSwitchRow(
                            icon = Icons.Filled.AutoFixHigh,
                            title = "Vibrancy",
                            subtitle = "Enhance underlying color richness through the glass",
                            checked = effectiveConfig.vibrancyEnabled,
                            enabled = controlsEnabled,
                            onCheckedChange = onUpdateVibrancyEnabled,
                            testTag = "lg_toggle_vibrancy"
                        )

                        HorizontalDivider(
                            thickness = 1.dp,
                            color = dividerColor,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )

                        LgSliderRow(
                            icon = Icons.Filled.InvertColors,
                            title = "Saturation",
                            value = effectiveConfig.saturation,
                            valueRange = 0f..2f,
                            enabled = controlsEnabled,
                            onValueChange = onUpdateSaturation,
                            testTag = "lg_slider_saturation"
                        )

                        HorizontalDivider(
                            thickness = 1.dp,
                            color = dividerColor,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )

                        LgSliderRow(
                            icon = Icons.Filled.Brightness6,
                            title = "Brightness",
                            value = effectiveConfig.brightness,
                            valueRange = -0.50f..0.50f,
                            signedPercent = true,
                            enabled = controlsEnabled,
                            onValueChange = onUpdateBrightness,
                            testTag = "lg_slider_brightness"
                        )

                        HorizontalDivider(
                            thickness = 1.dp,
                            color = dividerColor,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )

                        LgSliderRow(
                            icon = Icons.Filled.Contrast,
                            title = "Contrast",
                            value = effectiveConfig.contrast,
                            valueRange = 0.50f..1.50f,
                            enabled = controlsEnabled,
                            onValueChange = onUpdateContrast,
                            testTag = "lg_slider_contrast"
                        )
                    }
                }

                // | Highlights
                item(key = "lg_sec_highlights") {
                    LgSectionCard(title = "| Highlights") {
                        LgSelectionRow(
                            icon = Icons.Filled.Style,
                            title = "Highlight style",
                            currentOption = effectiveConfig.highlightStyle.label,
                            enabled = controlsEnabled,
                            onClick = { showHighlightStyleDialog = true },
                            testTag = "lg_row_highlight_style"
                        )

                        HorizontalDivider(
                            thickness = 1.dp,
                            color = dividerColor,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )

                        LgSliderRow(
                            icon = Icons.Filled.BrightnessHigh,
                            title = "Highlight strength",
                            value = effectiveConfig.highlightStrength,
                            valueRange = 0f..2f,
                            enabled = controlsEnabled,
                            onValueChange = onUpdateHighlightStrength,
                            testTag = "lg_slider_highlight_strength"
                        )

                        HorizontalDivider(
                            thickness = 1.dp,
                            color = dividerColor,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )

                        LgSliderRow(
                            icon = Icons.Filled.LineWeight,
                            title = "Highlight width",
                            value = effectiveConfig.highlightWidth,
                            valueRange = 0f..2f,
                            enabled = controlsEnabled,
                            onValueChange = onUpdateHighlightWidth,
                            testTag = "lg_slider_highlight_width"
                        )

                        HorizontalDivider(
                            thickness = 1.dp,
                            color = dividerColor,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )

                        LgSliderRow(
                            icon = Icons.Filled.Gradient,
                            title = "Highlight softness",
                            value = effectiveConfig.highlightSoftness,
                            valueRange = 0f..2f,
                            enabled = controlsEnabled,
                            onValueChange = onUpdateHighlightSoftness,
                            testTag = "lg_slider_highlight_softness"
                        )
                    }
                }

                // Bottom Reset Action
                item(key = "lg_reset_default") {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        TextButton(
                            onClick = {
                                onResetToDefault()
                                onShowCapsuleToast("Liquid glass reset to defaults")
                            },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("lg_reset_to_default_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Reset to default",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Frozen Liquid Glass Top Header: <- Liquid glass
        ScrollAwareFixedHeader(
            layoutState = scrollLayout,
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("liquid_glass_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = palette.brandTitleColor
                    )
                }
                Text(
                    text = "Liquid glass",
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.brandTitleColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showHighlightStyleDialog) {
        MinimalSingleChoiceDialog(
            title = "Highlight style",
            options = HighlightStyle.entries.map { it.label },
            selected = effectiveConfig.highlightStyle.label,
            onSelect = { chosenLabel ->
                showHighlightStyleDialog = false
                val selectedStyle = HighlightStyle.entries.firstOrNull { it.label == chosenLabel }
                    ?: HighlightStyle.COMPONENT_DEFAULT
                onUpdateHighlightStyle(selectedStyle)
            },
            onDismiss = { showHighlightStyleDialog = false },
            dialogTestTag = "highlight_style_picker_dialog"
        )
    }
}

@Composable
private fun LiquidGlassStickyPreviewSection(
    modifier: Modifier = Modifier
) {
    val liveConfig = LocalLiquidGlassConfig.current
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val primaryAccent = MaterialTheme.colorScheme.primary
    val frostNorm = liveConfig.frostPercent.coerceIn(0f, 1f)

    var previewSwitchChecked by remember { mutableStateOf(true) }
    var previewLoopActive by remember { mutableStateOf(false) }
    var previewIsPlaying by remember { mutableStateOf(false) }

    val previewLayer = rememberGraphicsLayer()
    val previewBackdropState = remember(previewLayer) {
        FrostedBackdropState(
            graphicsLayer = previewLayer,
            backgroundGraphicsLayer = null
        )
    }

    val previewShape = RoundedCornerShape(20.dp)
    val frameBorder = if (isDark) {
        Color.White.copy(alpha = 0.18f)
    } else {
        palette.outlineBorder.copy(alpha = 0.85f)
    }
    // Rich, high-saturation vibrant checkerboard colors with zero dulling white wash
    val lightVividTiles = remember {
        listOf(
            Color(0xFF6366F1), // Vivid Periwinkle Blue
            Color(0xFFEC4899), // Vibrant Rose-Pink
            Color(0xFF818CF8), // Luminous Indigo-Periwinkle
            Color(0xFFF472B6), // Bright Flamingo Pink
            Color(0xFF06B6D4), // Vivid Cyan Accent
            Color(0xFF8B5CF6)  // Rich Violet Accent
        )
    }
    val darkVividTiles = remember {
        listOf(
            Color(0xFF3730A3), // Rich Indigo
            Color(0xFF86198F), // Deep Vibrant Magenta
            Color(0xFF4338CA), // Saturated Royal Indigo
            Color(0xFF9D174D), // Rich Crimson-Rose
            Color(0xFF0E7490), // Deep Cyan
            Color(0xFF5B21B6)  // Deep Violet
        )
    }

    Column(modifier = modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(142.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = previewShape,
                    ambientColor = primaryAccent.copy(alpha = if (isDark) 0.28f else 0.14f),
                    spotColor = primaryAccent.copy(alpha = if (isDark) 0.30f else 0.16f)
                )
                .clip(previewShape)
                .border(1.dp, frameBorder, previewShape)
                .testTag("liquid_glass_live_preview_card")
        ) {
            // Recorded High-Contrast Vibrant Checkerboard Backdrop
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .recordFrostedBackdrop(
                        backdropState = previewBackdropState,
                        contentLayer = previewLayer
                    )
            ) {
                val cellSize = 24.dp.toPx().coerceAtLeast(16f)
                val cols = ceil(size.width / cellSize).toInt() + 1
                val rows = ceil(size.height / cellSize).toInt() + 1
                val activeTiles = if (isDark) darkVividTiles else lightVividTiles

                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        val isEven = (r + c) % 2 == 0
                        val tileColor = if (isEven) {
                            if ((r + c / 2) % 3 == 0) activeTiles[2] else activeTiles[0]
                        } else {
                            when ((r * 3 + c) % 4) {
                                0 -> activeTiles[1]
                                1 -> activeTiles[3]
                                2 -> activeTiles[4]
                                else -> activeTiles[5]
                            }
                        }
                        drawRect(
                            color = tileColor,
                            topLeft = Offset(c * cellSize, r * cellSize),
                            size = Size(cellSize + 0.5f, cellSize + 0.5f)
                        )
                    }
                }
            }

            // Live Interactive Aura Player Pill + Circular Play Button driven in real time by LocalLiquidGlassConfig
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                modifier = Modifier.padding(horizontal = 14.dp)
            ) {
                val pillBorder = if (isDark) {
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.38f),
                            Color.White.copy(alpha = 0.20f),
                            primaryAccent.copy(alpha = 0.28f)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.95f),
                            Color(0xFF94A3B8).copy(alpha = 0.35f),
                            primaryAccent.copy(alpha = 0.30f)
                        )
                    )
                }

                FrostedGlassSurface(
                    backdropState = previewBackdropState,
                    contentLayerOverride = previewLayer,
                    configOverride = liveConfig,
                    shape = CircleShape,
                    blurRadius = 28.dp,
                    featherBottomEdge = false,
                    glassProgressOverride = 1f,
                    border = BorderStroke(1.25.dp, pillBorder),
                    baseBackgroundColor = null,
                    specularOverlayBrush = null,
                    modifier = Modifier
                        .height(52.dp)
                        .crystalOuterShadow(
                            shape = CircleShape,
                            color = if (isDark) Color.Black.copy(alpha = 0.42f) else Color(0x280F172A),
                            blurSpread = 12.dp,
                            offsetY = 4.dp
                        )
                        .testTag("liquid_glass_preview_pill")
                ) {
                    val pillTextColor = if (liveConfig.enabled && frostNorm < 0.35f && !isDark) {
                        Color(0xFF0F172A)
                    } else {
                        palette.primaryText
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(start = 16.dp, end = 10.dp, top = 6.dp, bottom = 6.dp)
                    ) {
                        Text(
                            text = "Aura Player",
                            color = pillTextColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (previewLoopActive) {
                                        primaryAccent.copy(alpha = 0.18f)
                                    } else {
                                        Color.Transparent
                                    }
                                )
                                .clickable { previewLoopActive = !previewLoopActive }
                                .testTag("liquid_glass_preview_loop_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Repeat,
                                contentDescription = "Toggle Preview Loop",
                                tint = if (previewLoopActive) primaryAccent else pillTextColor.copy(alpha = 0.88f),
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        Switch(
                            checked = previewSwitchChecked,
                            onCheckedChange = { previewSwitchChecked = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = primaryAccent,
                                uncheckedThumbColor = palette.secondaryText,
                                uncheckedTrackColor = palette.neutralChipBg
                            ),
                            modifier = Modifier.testTag("liquid_glass_preview_switch")
                        )
                    }
                }

                // Circular Liquid Glass Play Button
                val playBorder = Brush.linearGradient(
                    0.0f to Color.White.copy(alpha = 0.72f),
                    0.50f to primaryAccent.copy(alpha = 0.48f),
                    1.0f to Color.White.copy(alpha = 0.25f)
                )
                val playFillColor = if (liveConfig.enabled) {
                    primaryAccent.copy(alpha = 0.55f + 0.25f * frostNorm)
                } else {
                    primaryAccent
                }
                FrostedGlassSurface(
                    backdropState = previewBackdropState,
                    contentLayerOverride = previewLayer,
                    configOverride = liveConfig,
                    shape = CircleShape,
                    blurRadius = 28.dp,
                    featherBottomEdge = false,
                    glassProgressOverride = 1f,
                    border = if (liveConfig.enabled) BorderStroke(1.25.dp, playBorder) else null,
                    baseBackgroundColor = playFillColor,
                    specularOverlayBrush = null,
                    modifier = Modifier
                        .size(52.dp)
                        .crystalOuterShadow(
                            shape = CircleShape,
                            color = primaryAccent.copy(alpha = if (isDark) 0.45f else 0.30f),
                            blurSpread = 12.dp,
                            offsetY = 4.dp
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button
                        ) {
                            previewIsPlaying = !previewIsPlaying
                        }
                        .testTag("liquid_glass_preview_play_button")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.matchParentSize()
                    ) {
                        Icon(
                            imageVector = if (previewIsPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Preview Play",
                            tint = Color.White.copy(alpha = 0.95f),
                            modifier = Modifier.size(25.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Experimental Pill Badge + Helper Caption styled with Aura Player's primary accent
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(primaryAccent.copy(alpha = if (isDark) 0.20f else 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .testTag("liquid_glass_experimental_badge")
            ) {
                Text(
                    text = "Live preview",
                    color = primaryAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "Changes update headers and navigation glass in real time.",
                color = palette.secondaryText,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun LgSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val primary = MaterialTheme.colorScheme.primary
    val headerColor = if (isDark) primary else palette.sectionHeaderBlue
    val cardShape = RoundedCornerShape(20.dp)
    val cardBg = if (isDark) palette.sheetBackground.copy(alpha = 0.84f) else Color.White.copy(alpha = 0.86f)
    val borderBrush = if (isDark) {
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.20f), primary.copy(alpha = 0.16f)))
    } else {
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.94f), palette.outlineBorder.copy(alpha = 0.78f)))
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = headerColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp,
            modifier = Modifier.padding(start = 6.dp, top = 4.dp, bottom = 6.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 4.dp,
                    shape = cardShape,
                    ambientColor = primary.copy(alpha = if (isDark) 0.22f else 0.06f),
                    spotColor = primary.copy(alpha = if (isDark) 0.26f else 0.08f)
                )
                .clip(cardShape)
                .background(cardBg)
                .border(1.dp, borderBrush, cardShape)
                .padding(vertical = 4.dp),
            content = content
        )
    }
}

@Composable
private fun LgIconBadge(icon: ImageVector) {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = primary,
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable
private fun LgUniformSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
    enabled: Boolean = true
) {
    val palette = LocalExtendedPalette.current
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .alpha(if (enabled) 1f else 0.48f)
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag(testTag)
    ) {
        LgIconBadge(icon = icon)
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 10.dp)
        ) {
            Text(
                text = title,
                color = palette.primaryText,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = palette.secondaryText,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = onPrimary,
                checkedTrackColor = primary,
                uncheckedThumbColor = palette.secondaryText,
                uncheckedTrackColor = palette.neutralChipBg
            )
        )
    }
}

@Composable
private fun LgSliderRow(
    icon: ImageVector,
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    testTag: String,
    signedPercent: Boolean = false
) {
    val palette = LocalExtendedPalette.current
    val primary = MaterialTheme.colorScheme.primary
    val percentInt = (value.coerceIn(valueRange.start, valueRange.endInclusive) * 100f).roundToInt()
    val formattedValue = if (signedPercent && percentInt > 0) "+$percentInt%" else "$percentInt%"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.48f)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            LgIconBadge(icon = icon)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                color = palette.primaryText,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(primary.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = formattedValue,
                    color = primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("${testTag}_value")
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = { raw ->
                val snapped = (raw * 100f).roundToInt() / 100f
                onValueChange(snapped.coerceIn(valueRange.start, valueRange.endInclusive))
            },
            valueRange = valueRange,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = primary,
                activeTrackColor = primary,
                inactiveTrackColor = primary.copy(alpha = 0.22f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 46.dp)
                .testTag(testTag)
        )
    }
}

@Composable
private fun LgSelectionRow(
    icon: ImageVector,
    title: String,
    currentOption: String,
    enabled: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val palette = LocalExtendedPalette.current
    val primary = MaterialTheme.colorScheme.primary

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .alpha(if (enabled) 1f else 0.48f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag(testTag)
    ) {
        LgIconBadge(icon = icon)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = palette.primaryText,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = currentOption,
                color = primary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.Filled.Tune,
            contentDescription = null,
            tint = palette.secondaryText,
            modifier = Modifier.size(18.dp)
        )
    }
}
