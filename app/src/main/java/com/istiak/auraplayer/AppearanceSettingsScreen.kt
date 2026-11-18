package com.istiak.auraplayer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.istiak.auraplayer.ui.components.ScrollAwareFixedHeader
import com.istiak.auraplayer.ui.components.recordFrostedBackdrop
import com.istiak.auraplayer.ui.components.rememberScrollAwareGlassLayout
import com.istiak.auraplayer.ui.theme.AppThemePalette
import com.istiak.auraplayer.ui.theme.LocalExtendedPalette
import com.istiak.auraplayer.ui.theme.resolvePaletteTokens
import kotlin.math.roundToInt

/**
 * Material 3 Expressive Appearance Sub-Screen (`AppearanceSettingsScreen.kt`):
 * - Frozen `ScrollAwareFixedHeader`: `← Appearance`
 * - `| Theme` Section: Expandable Theme card showing `"<Mode> · <Palette>"` subtitle,
 *   connected segmented selector (`Dark` | `Light` | `System`), and horizontal `App Theme`
 *   mini phone-card carousel of all 17 curated Light & Dark palettes.
 * - `| File Browser` Section: Uniform card rows for `Show full names`,
 *   `Show label for new unplayed videos`, `Days threshold for new video label` slider (`1`..`30`),
 *   and `Show video thumbnails`.
 * - `| App Identity` Section: Preserves the existing `App Disguise` launcher switcher.
 */
@Composable
fun AppearanceSettingsScreen(
    settings: AppSettingsState,
    onUpdateThemeMode: (ThemeMode) -> Unit,
    onUpdateThemePalette: (AppThemePalette) -> Unit = {},
    onUpdateShowFullNames: (Boolean) -> Unit = {},
    onUpdateShowNewVideoLabel: (Boolean) -> Unit = {},
    onUpdateNewVideoLabelDaysThreshold: (Int) -> Unit = {},
    onUpdateShowVideoThumbnails: (Boolean) -> Unit = {},
    onUpdateAppDisguiseMode: (AppDisguiseMode) -> Unit,
    onShowCapsuleToast: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val systemIsDark = isSystemInDarkTheme()
    val previewIsDark = when (settings.themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemIsDark
    }

    var themeCardExpanded by remember { mutableStateOf(true) }
    var sliderDaysDraft by remember(settings.newVideoLabelDaysThreshold) {
        mutableFloatStateOf(settings.newVideoLabelDaysThreshold.coerceIn(1, 30).toFloat())
    }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("appearance_settings_screen")
    ) {
        LazyColumn(
            state = listState,
            contentPadding = scrollLayout.contentPadding,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .recordFrostedBackdrop(
                    backdropState = scrollLayout.backdropState,
                    scrollableState = listState,
                    contentLayerOverride = scrollLayout.contentGraphicsLayer
                )
        ) {
            item(key = "sec_theme") {
                AsFrostedSection(title = "| Theme") {
                    // Expandable Theme Header Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp)
                            .clickable { themeCardExpanded = !themeCardExpanded }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("appearance_theme_expand_row")
                    ) {
                        AsIconBadge(icon = Icons.Filled.Palette)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Theme",
                                color = palette.primaryText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${settings.themeMode.label} · ${settings.appThemePalette.displayName}",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("appearance_theme_subtitle")
                            )
                        }
                        Icon(
                            imageVector = if (themeCardExpanded) {
                                Icons.Filled.KeyboardArrowUp
                            } else {
                                Icons.Filled.KeyboardArrowDown
                            },
                            contentDescription = if (themeCardExpanded) "Collapse Theme" else "Expand Theme",
                            tint = palette.secondaryText,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = themeCardExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            HorizontalDivider(
                                thickness = 1.dp,
                                color = dividerColor,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Connected Segmented Mode Selector: Dark | Light | System
                            ThemeModeSegmentedSelector(
                                selectedMode = settings.themeMode,
                                onSelectMode = onUpdateThemeMode,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "App Theme",
                                color = palette.primaryText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Horizontal App Theme Carousel (17 curated palettes)
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 14.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("app_theme_carousel")
                            ) {
                                items(AppThemePalette.entries, key = { it.name }) { paletteOption ->
                                    val isSelected = settings.appThemePalette == paletteOption
                                    val previewTokens = remember(paletteOption, previewIsDark, context) {
                                        resolvePaletteTokens(
                                            palette = paletteOption,
                                            isDark = previewIsDark,
                                            context = context
                                        )
                                    }
                                    ThemePalettePreviewCard(
                                        paletteOption = paletteOption,
                                        tokens = previewTokens,
                                        isSelected = isSelected,
                                        onClick = { onUpdateThemePalette(paletteOption) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item(key = "sec_file_browser") {
                AsFrostedSection(title = "| File Browser") {
                    // 1. Show full names (Toggle, default false)
                    AsUniformSwitchRow(
                        icon = Icons.Filled.Title,
                        title = "Show full names",
                        subtitle = "Display complete folder and video names without truncation",
                        checked = settings.showFullNames,
                        onCheckedChange = onUpdateShowFullNames,
                        testTag = "appearance_toggle_show_full_names"
                    )

                    HorizontalDivider(
                        thickness = 1.dp,
                        color = dividerColor,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )

                    // 2. Show label for new unplayed videos (Toggle, default true)
                    AsUniformSwitchRow(
                        icon = Icons.Filled.NewReleases,
                        title = "Show label for new unplayed videos",
                        subtitle = "Mark recently added unplayed videos with a NEW badge",
                        checked = settings.showNewVideoLabel,
                        onCheckedChange = onUpdateShowNewVideoLabel,
                        testTag = "appearance_toggle_show_new_label"
                    )

                    HorizontalDivider(
                        thickness = 1.dp,
                        color = dividerColor,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )

                    // 3. Days threshold for new video label (Slider 1..30, default 7)
                    val displayedDays = sliderDaysDraft.roundToInt().coerceIn(1, 30)
                    AsDaysThresholdSliderRow(
                        icon = Icons.Filled.CalendarToday,
                        daysThreshold = displayedDays,
                        sliderValue = sliderDaysDraft,
                        enabled = settings.showNewVideoLabel,
                        onValueChange = { raw ->
                            sliderDaysDraft = raw.coerceIn(1f, 30f)
                        },
                        onValueChangeFinished = {
                            onUpdateNewVideoLabelDaysThreshold(sliderDaysDraft.roundToInt().coerceIn(1, 30))
                        },
                        testTag = "appearance_slider_new_days_threshold"
                    )

                    HorizontalDivider(
                        thickness = 1.dp,
                        color = dividerColor,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )

                    // 4. Show video thumbnails (Toggle, default true)
                    AsUniformSwitchRow(
                        icon = Icons.Filled.Image,
                        title = "Show video thumbnails",
                        subtitle = "Display extracted video preview frames in lists and grids",
                        checked = settings.showVideoThumbnails,
                        onCheckedChange = onUpdateShowVideoThumbnails,
                        testTag = "appearance_toggle_show_thumbnails"
                    )
                }
            }

            item(key = "sec_app_identity") {
                AsFrostedSection(title = "| App Identity") {
                    AsStackedSegmentedRow(
                        icon = Icons.Filled.Calculate,
                        title = "App Disguise",
                        subtitle = "Switch launcher icon and app name on your home screen",
                        options = AppDisguiseMode.entries.map { it.label },
                        selectedIndex = AppDisguiseMode.entries.indexOf(settings.appDisguiseMode).coerceAtLeast(0),
                        onSelectIndex = { idx ->
                            val chosen = AppDisguiseMode.entries[idx]
                            PrivateVaultManager.applyAppDisguise(context, chosen)
                            onUpdateAppDisguiseMode(chosen)
                            onShowCapsuleToast("App Disguise set to ${chosen.label}")
                        },
                        testTagPrefix = "app_disguise_option"
                    )
                }
            }
        }

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
                        .testTag("appearance_settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = palette.brandTitleColor
                    )
                }
                Text(
                    text = "Appearance",
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.brandTitleColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ThemeModeSegmentedSelector(
    selectedMode: ThemeMode,
    onSelectMode: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer

    val orderedModes = listOf(
        Triple(ThemeMode.DARK, "Dark", Icons.Filled.DarkMode),
        Triple(ThemeMode.LIGHT, "Light", Icons.Filled.LightMode),
        Triple(ThemeMode.SYSTEM, "System", Icons.Filled.BrightnessAuto)
    )
    val trackBg = if (isDark) {
        Color.White.copy(alpha = 0.06f)
    } else {
        palette.neutralChipBg.copy(alpha = 0.72f)
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(trackBg)
            .border(
                width = 1.dp,
                color = if (isDark) Color.White.copy(alpha = 0.10f) else palette.outlineBorder.copy(alpha = 0.65f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(4.dp)
            .testTag("theme_mode_segmented_selector")
    ) {
        orderedModes.forEach { (mode, label, icon) ->
            val isSelected = selectedMode == mode
            val segmentBg = if (isSelected) primaryContainer else Color.Transparent
            val contentColor = if (isSelected) onPrimaryContainer else palette.secondaryText
            val iconTint = if (isSelected) primary else palette.secondaryText

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(segmentBg)
                    .then(
                        if (isSelected) {
                            Modifier.border(
                                width = 1.dp,
                                color = primary.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onSelectMode(mode) }
                    .padding(horizontal = 8.dp)
                    .testTag("theme_mode_option_${mode.name.lowercase()}")
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = label,
                    color = contentColor,
                    fontSize = 12.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun ThemePalettePreviewCard(
    paletteOption: AppThemePalette,
    tokens: com.istiak.auraplayer.ui.theme.ThemePaletteTokens,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val activePalette = LocalExtendedPalette.current
    val previewShape = RoundedCornerShape(16.dp)
    val borderColor = if (isSelected) tokens.primary else tokens.outlineVariant.copy(alpha = 0.85f)
    val borderWidth = if (isSelected) 2.dp else 1.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(78.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp)
            .testTag("theme_palette_${paletteOption.name.lowercase()}")
    ) {
        Box(
            modifier = Modifier
                .size(width = 76.dp, height = 116.dp)
                .shadow(
                    elevation = if (isSelected) 6.dp else 2.dp,
                    shape = previewShape,
                    ambientColor = tokens.primary.copy(alpha = if (isSelected) 0.25f else 0.08f),
                    spotColor = tokens.primary.copy(alpha = if (isSelected) 0.28f else 0.10f)
                )
                .clip(previewShape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            tokens.ambientTopLeft,
                            tokens.ambientTopRight,
                            tokens.ambientBase
                        )
                    )
                )
                .border(borderWidth, borderColor, previewShape)
                .padding(7.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxSize()
            ) {
                // Simulated Top Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.width(if (isSelected) 20.dp else 2.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(7.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(tokens.brandTitle.copy(alpha = 0.72f))
                    )
                }

                // Simulated Middle Card with Accent Pill + Dot
                Column(
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(tokens.surface.copy(alpha = 0.92f))
                        .border(0.75.dp, tokens.outlineVariant.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
                        .padding(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .width(26.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(tokens.onSurface.copy(alpha = 0.45f))
                        )
                        // Mini Toggle Track + Thumb
                        Box(
                            contentAlignment = Alignment.CenterEnd,
                            modifier = Modifier
                                .size(width = 18.dp, height = 10.dp)
                                .clip(CircleShape)
                                .background(tokens.primary)
                                .padding(1.5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(tokens.onPrimary)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.72f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(tokens.primaryContainer)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(tokens.surfaceVariant)
                    )
                }

                // Simulated Bottom Bar Capsule + Play Dot
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(14.dp)
                            .clip(CircleShape)
                            .background(tokens.surface.copy(alpha = 0.88f))
                            .border(0.75.dp, tokens.outlineVariant.copy(alpha = 0.70f), CircleShape)
                            .padding(horizontal = 3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(tokens.primary.copy(alpha = 0.25f))
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(tokens.onSurfaceVariant.copy(alpha = 0.40f))
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(tokens.primary)
                    )
                }
            }

            if (isSelected) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(tokens.primary)
                        .border(1.5.dp, Color.White, CircleShape)
                        .testTag("theme_palette_selected_badge")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Selected",
                        tint = tokens.onPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = paletteOption.displayName,
            color = if (isSelected) MaterialTheme.colorScheme.primary else activePalette.primaryText,
            fontSize = 11.5.sp,
            lineHeight = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AsFrostedSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val primary = MaterialTheme.colorScheme.primary
    val headerColor = if (isDark) primary else palette.sectionHeaderBlue
    val cardShape = RoundedCornerShape(18.dp)
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
private fun AsIconBadge(icon: ImageVector) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val primary = MaterialTheme.colorScheme.primary
    val containerBg = if (isDark) primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.primaryContainer
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(containerBg),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = primary,
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Uniform Switch Row: background stays completely transparent regardless of [checked] state
 * (zero ON/OFF ash background shift).
 */
@Composable
private fun AsUniformSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    val palette = LocalExtendedPalette.current
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag(testTag)
    ) {
        AsIconBadge(icon = icon)
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
private fun AsDaysThresholdSliderRow(
    icon: ImageVector,
    daysThreshold: Int,
    sliderValue: Float,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    testTag: String
) {
    val palette = LocalExtendedPalette.current
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.45f)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            AsIconBadge(icon = icon)
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = "Days threshold for new video label",
                    color = palette.primaryText,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Videos added within the last $daysThreshold days without playback history will be marked as new",
                    color = palette.secondaryText,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.testTag("appearance_days_threshold_subtitle")
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(primaryContainer)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${daysThreshold}d",
                    color = onPrimaryContainer,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("appearance_days_threshold_badge")
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Slider(
            value = sliderValue.coerceIn(1f, 30f),
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = 1f..30f,
            steps = 28,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = primary,
                activeTrackColor = primary,
                inactiveTrackColor = primary.copy(alpha = 0.22f),
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 42.dp)
                .testTag(testTag)
        )
    }
}

@Composable
private fun AsStackedSegmentedRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    options: List<String>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    testTagPrefix: String
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val inactiveBg = if (isDark) Color.White.copy(alpha = 0.08f) else palette.neutralChipBg
    val inactiveBorder = if (isDark) Color.White.copy(alpha = 0.12f) else palette.outlineBorder

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsIconBadge(icon = icon)
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
                    text = subtitle,
                    color = palette.secondaryText,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 46.dp)
        ) {
            options.forEachIndexed { idx, label ->
                val isSelected = idx == selectedIndex
                val chipShape = RoundedCornerShape(999.dp)
                Box(
                    modifier = Modifier
                        .clip(chipShape)
                        .background(if (isSelected) primary else inactiveBg)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) primary.copy(alpha = 0.85f) else inactiveBorder,
                            shape = chipShape
                        )
                        .clickable { onSelectIndex(idx) }
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                        .testTag("${testTagPrefix}_$idx"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) onPrimary else palette.primaryText.copy(alpha = 0.86f),
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        softWrap = false,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
