package com.istiak.auraplayer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.istiak.auraplayer.ui.components.ScrollAwareFixedHeader
import com.istiak.auraplayer.ui.components.recordFrostedBackdrop
import com.istiak.auraplayer.ui.components.rememberScrollAwareGlassLayout
import com.istiak.auraplayer.ui.theme.LocalExtendedPalette

private val PsCobaltActive = Color(0xFF2563EB)
private val PsActiveHighlightLight = Color(0xFFE3E6FA)
private val PsPeriwinkleTint = Color(0xFFD6D9F8)
private val PsSlateHeaderLight = Color(0xFF434A6E)

/**
 * Player & Decoder Sub-Screen (`PlayerSettingsScreen.kt`):
 * - Frozen `ScrollAwareFixedHeader`: `← Player & Decoder`
 * - Section `| Video & Orientation`: Orientation (8-option modal dialog), Default Decoder Mode (`HW | HW+ | SW`),
 *   Default Playback Speed (`0.5x | 1x | 1.25x | 1.5x | 2x`).
 * - Section `| Playback Behavior`: Default Resume Playback, Background Audio / PiP,
 *   Autoplay next video (unified), +85s Skip Intro Button, Auto-skip intro, Auto-skip outro,
 *   and Remember display brightness (`Off`, `Across all videos`, `Per video`).
 */
@Composable
fun PlayerSettingsScreen(
    settings: AppSettingsState,
    onUpdateVideoOrientationMode: (VideoOrientationMode) -> Unit,
    onUpdateDecoderPriority: (DecoderPriority) -> Unit,
    onUpdateDefaultPlaybackSpeed: (Float) -> Unit,
    onUpdateResumePlaybackMode: (ResumePlaybackMode) -> Unit,
    onUpdateBackgroundAudioOrPip: (Boolean) -> Unit,
    onUpdateAutoPlayNextEpisode: (Boolean) -> Unit,
    onUpdateAutoSkipIntro: (Boolean) -> Unit = {},
    onUpdateAutoSkipOutro: (Boolean) -> Unit = {},
    onUpdateBrightnessMemoryMode: (BrightnessMemoryMode) -> Unit = {},
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val palette = LocalExtendedPalette.current
    var showOrientationDialog by remember { mutableStateOf(false) }
    var showBrightnessModeDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val navBottomDp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val scrollLayout = rememberScrollAwareGlassLayout(
        listState = listState,
        defaultHeaderHeight = 64.dp,
        extraTopGap = 4.dp,
        bottomPadding = navBottomDp + 24.dp,
        horizontalPadding = 12.dp
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("player_settings_screen")
    ) {
        LazyColumn(
            state = listState,
            contentPadding = scrollLayout.contentPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .recordFrostedBackdrop(
                    backdropState = scrollLayout.backdropState,
                    scrollableState = listState,
                    contentLayerOverride = scrollLayout.contentGraphicsLayer
                )
        ) {
            // Section | Video & Orientation
            item(key = "sec_video_orientation") {
                PsFrostedSection(title = "| Video & Orientation") {
                    PsActionRow(
                        icon = Icons.Filled.ScreenRotation,
                        title = "Orientation",
                        badgeText = settings.videoOrientationMode.label,
                        onClick = { showOrientationDialog = true },
                        testTag = "settings_orientation_row"
                    )

                    PsInlineChoiceRow(
                        icon = Icons.Filled.Memory,
                        title = "Default Decoder Mode",
                        options = DecoderPriority.entries.map { it.badgeText },
                        selectedIndex = DecoderPriority.entries.indexOf(settings.decoderPriority).coerceAtLeast(0),
                        onSelectIndex = { idx -> onUpdateDecoderPriority(DecoderPriority.entries[idx]) },
                        testTagPrefix = "ps_decoder_option"
                    )

                    val speedOptions = listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f)
                    val speedLabels = listOf("0.5x", "1x", "1.25x", "1.5x", "2x")
                    PsCompactChoiceRow(
                        icon = Icons.Filled.Speed,
                        title = "Default Playback Speed",
                        options = speedLabels,
                        selectedIndex = speedOptions.indexOfFirst { it == settings.defaultPlaybackSpeed }.coerceAtLeast(1),
                        showDivider = false,
                        onSelectIndex = { idx -> onUpdateDefaultPlaybackSpeed(speedOptions[idx]) },
                        testTagPrefix = "ps_speed_option"
                    )
                }
            }

            // Section | Playback Behavior
            item(key = "sec_playback_behavior") {
                PsFrostedSection(title = "| Playback Behavior") {
                    PsCompactChoiceRow(
                        icon = Icons.Filled.PlayCircleOutline,
                        title = "Default Resume Playback",
                        options = ResumePlaybackMode.entries.map { it.label },
                        selectedIndex = ResumePlaybackMode.entries.indexOf(settings.resumePlaybackMode).coerceAtLeast(0),
                        onSelectIndex = { idx -> onUpdateResumePlaybackMode(ResumePlaybackMode.entries[idx]) },
                        testTagPrefix = "ps_resume_mode"
                    )

                    PsSwitchRow(
                        icon = Icons.Filled.Headset,
                        title = "Background Audio / PiP",
                        subtitle = "Enter Picture-in-Picture or keep audio running when leaving",
                        checked = settings.backgroundAudioOrPipOnHome,
                        onCheckedChange = onUpdateBackgroundAudioOrPip,
                        testTag = "switch_bg_pip"
                    )

                    PsSwitchRow(
                        icon = Icons.Filled.FastForward,
                        title = "Auto-skip intro",
                        subtitle = "Show interactive Skip Intro popup when opening markers are detected",
                        checked = settings.autoSkipIntro,
                        onCheckedChange = onUpdateAutoSkipIntro,
                        testTag = "switch_auto_skip_intro"
                    )

                    PsSwitchRow(
                        icon = Icons.Filled.SkipNext,
                        title = "Auto-skip outro",
                        subtitle = "Show interactive Skip Outro popup when ending markers are detected",
                        checked = settings.autoSkipOutro,
                        onCheckedChange = onUpdateAutoSkipOutro,
                        testTag = "switch_auto_skip_outro"
                    )

                    PsSwitchRow(
                        icon = Icons.Filled.SkipNext,
                        title = "Autoplay next video",
                        subtitle = "Show interactive Next Video popup when current video ends",
                        checked = settings.autoPlayNextEpisode,
                        onCheckedChange = onUpdateAutoPlayNextEpisode,
                        testTag = "switch_autoplay_next_episode"
                    )

                    PsActionRow(
                        icon = Icons.Filled.Brightness6,
                        title = "Remember display brightness",
                        subtitle = settings.brightnessMemoryMode.rowSubtitle,
                        badgeText = settings.brightnessMemoryMode.label,
                        onClick = { showBrightnessModeDialog = true },
                        showDivider = false,
                        testTag = "settings_remember_brightness_row"
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
                        .testTag("player_settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = palette.brandTitleColor
                    )
                }
                Text(
                    text = "Player & Decoder",
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.brandTitleColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showOrientationDialog) {
        VideoOrientationSelectorDialog(
            selectedMode = settings.videoOrientationMode,
            onSelectMode = { mode ->
                onUpdateVideoOrientationMode(mode)
                showOrientationDialog = false
            },
            onDismiss = { showOrientationDialog = false }
        )
    }

    if (showBrightnessModeDialog) {
        BrightnessMemoryModeDialog(
            selectedMode = settings.brightnessMemoryMode,
            onSelectMode = { mode ->
                onUpdateBrightnessMemoryMode(mode)
                showBrightnessModeDialog = false
            },
            onDismiss = { showBrightnessModeDialog = false }
        )
    }
}

@Composable
private fun BrightnessMemoryModeDialog(
    selectedMode: BrightnessMemoryMode,
    onSelectMode: (BrightnessMemoryMode) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val auraBlue = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
    val containerColor = if (isDark) Color(0xFF111827) else Color(0xFFF8FAFF)
    val hairlineBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    val selectedRowFill = if (isDark) {
        Color(0xFF60A5FA).copy(alpha = 0.16f)
    } else {
        Color(0xFF2563EB).copy(alpha = 0.10f)
    }
    val selectedRowBorder = Color(0xFF2563EB).copy(alpha = 0.45f)
    val cardShape = RoundedCornerShape(24.dp)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .widthIn(max = 380.dp)
                .fillMaxWidth()
                .shadow(16.dp, cardShape)
                .clip(cardShape)
                .background(containerColor)
                .border(1.dp, hairlineBorderColor, cardShape)
                .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 12.dp)
                .testTag("brightness_memory_mode_dialog")
        ) {
            Text(
                text = "Remember display brightness",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                BrightnessMemoryMode.entries.forEach { mode ->
                    val isSelected = mode == selectedMode
                    val rowShape = RoundedCornerShape(12.dp)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 46.dp)
                            .clip(rowShape)
                            .background(if (isSelected) selectedRowFill else Color.Transparent)
                            .then(
                                if (isSelected) {
                                    Modifier.border(1.dp, selectedRowBorder, rowShape)
                                } else {
                                    Modifier
                                }
                            )
                            .clickable { onSelectMode(mode) }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .testTag("brightness_mode_option_${mode.name.lowercase()}")
                    ) {
                        CompositionLocalProvider(
                            LocalMinimumInteractiveComponentSize provides Dp.Unspecified
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onSelectMode(mode) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = auraBlue,
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mode.label,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                            )
                            Text(
                                text = mode.description,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("brightness_mode_cancel_button")
                ) {
                    Text(
                        text = "Cancel",
                        color = auraBlue,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoOrientationSelectorDialog(
    selectedMode: VideoOrientationMode,
    onSelectMode: (VideoOrientationMode) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val dialogShape = RoundedCornerShape(24.dp)
    val containerBg = if (isDark) Color(0xFF161B2E) else Color(0xFFF6F7FF)
    val headerBg = if (isDark) Color(0xFF232B47) else PsActiveHighlightLight
    val activeCardBg = if (isDark) PsCobaltActive.copy(alpha = 0.24f) else PsActiveHighlightLight
    val inactiveCardBg = if (isDark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.85f)
    val cardBorder = if (isDark) Color.White.copy(alpha = 0.14f) else PsPeriwinkleTint.copy(alpha = 0.7f)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .clip(dialogShape)
                .background(containerBg)
                .border(1.dp, cardBorder, dialogShape)
                .testTag("orientation_selector_dialog")
        ) {
            // Soft periwinkle-lavender header banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBg)
                    .padding(horizontal = 22.dp, vertical = 18.dp)
            ) {
                Text(
                    text = "Orientation",
                    color = if (isDark) Color.White else PsSlateHeaderLight,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                VideoOrientationMode.entries.forEach { mode ->
                    val isSelected = mode == selectedMode
                    val itemShape = RoundedCornerShape(14.dp)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(itemShape)
                            .background(if (isSelected) activeCardBg else inactiveCardBg)
                            .border(
                                width = if (isSelected) 1.2.dp else 1.dp,
                                color = if (isSelected) PsCobaltActive.copy(alpha = 0.55f) else cardBorder,
                                shape = itemShape
                            )
                            .clickable { onSelectMode(mode) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("orientation_option_${mode.name.lowercase()}")
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelectMode(mode) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = PsCobaltActive,
                                unselectedColor = palette.secondaryText
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = mode.label,
                            color = palette.primaryText,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // Bottom-right pill-shaped Cancel button (#434A6E fill, white text)
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 4.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PsSlateHeaderLight,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("orientation_cancel_button")
                ) {
                    Text(
                        text = "Cancel",
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PsFrostedSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val primary = MaterialTheme.colorScheme.primary
    val headerColor = if (isDark) primary else palette.sectionHeaderBlue
    val cardShape = RoundedCornerShape(16.dp)
    val cardBg = if (isDark) palette.sheetBackground.copy(alpha = 0.84f) else Color.White.copy(alpha = 0.86f)
    val borderBrush = if (isDark) {
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), primary.copy(alpha = 0.16f)))
    } else {
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.94f), palette.outlineBorder.copy(alpha = 0.72f)))
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
                    ambientColor = primary.copy(alpha = if (isDark) 0.28f else 0.06f),
                    spotColor = primary.copy(alpha = if (isDark) 0.28f else 0.08f)
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
private fun PsIconBadge(icon: ImageVector) {
    val isDark = LocalExtendedPalette.current.isDark
    val primary = MaterialTheme.colorScheme.primary
    val containerBg = if (isDark) primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.primaryContainer
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(9.dp))
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

@Composable
private fun PsActionRow(
    icon: ImageVector,
    title: String,
    badgeText: String,
    onClick: () -> Unit,
    testTag: String,
    subtitle: String? = null,
    showDivider: Boolean = true
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val dividerColor = if (isDark) Color.White.copy(alpha = 0.08f) else palette.outlineBorder.copy(alpha = 0.72f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag(testTag)
        ) {
            PsIconBadge(icon = icon)
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
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = palette.secondaryText,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isDark) primary.copy(alpha = 0.22f) else primaryContainer)
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(
                    text = badgeText,
                    color = if (isDark) Color(0xFFDBEAFE) else onPrimaryContainer,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = palette.secondaryText,
                modifier = Modifier.size(18.dp)
            )
        }
        if (showDivider) {
            HorizontalDivider(
                thickness = 1.dp,
                color = dividerColor,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

@Composable
private fun PsInlineChoiceRow(
    icon: ImageVector,
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    testTagPrefix: String? = null,
    showDivider: Boolean = true
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val dividerColor = if (isDark) Color.White.copy(alpha = 0.08f) else palette.outlineBorder.copy(alpha = 0.72f)
    val inactiveBg = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F4FC)
    val inactiveBorder = if (isDark) Color.White.copy(alpha = 0.12f) else palette.outlineBorder.copy(alpha = 0.72f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            PsIconBadge(icon = icon)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                color = palette.primaryText,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
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
                                color = if (isSelected) primary.copy(alpha = 0.75f) else inactiveBorder,
                                shape = chipShape
                            )
                            .clickable { onSelectIndex(idx) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .then(
                                if (testTagPrefix != null) Modifier.testTag("${testTagPrefix}_$idx")
                                else Modifier
                            ),
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
        if (showDivider) {
            HorizontalDivider(
                thickness = 1.dp,
                color = dividerColor,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

@Composable
private fun PsCompactChoiceRow(
    icon: ImageVector,
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    testTagPrefix: String? = null,
    showDivider: Boolean = true
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val dividerColor = if (isDark) Color.White.copy(alpha = 0.08f) else palette.outlineBorder.copy(alpha = 0.72f)
    val inactiveBg = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F4FC)
    val inactiveBorder = if (isDark) Color.White.copy(alpha = 0.12f) else palette.outlineBorder.copy(alpha = 0.72f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PsIconBadge(icon = icon)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    color = palette.primaryText,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 44.dp)
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
                                color = if (isSelected) primary.copy(alpha = 0.75f) else inactiveBorder,
                                shape = chipShape
                            )
                            .clickable { onSelectIndex(idx) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .then(
                                if (testTagPrefix != null) Modifier.testTag("${testTagPrefix}_$idx")
                                else Modifier
                            ),
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
        if (showDivider) {
            HorizontalDivider(
                thickness = 1.dp,
                color = dividerColor,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

@Composable
private fun PsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
    showDivider: Boolean = true
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val dividerColor = if (isDark) Color.White.copy(alpha = 0.08f) else palette.outlineBorder.copy(alpha = 0.72f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag(testTag)
        ) {
            PsIconBadge(icon = icon)
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
            Spacer(modifier = Modifier.width(8.dp))
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    modifier = Modifier.scale(0.86f),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = onPrimary,
                        checkedTrackColor = primary,
                        checkedBorderColor = Color.Transparent,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
                        uncheckedBorderColor = Color.Transparent
                    )
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                thickness = 1.dp,
                color = dividerColor,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}
