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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Pinch
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import com.istiak.auraplayer.ui.components.MinimalSingleChoiceDialog
import com.istiak.auraplayer.ui.components.ScrollAwareFixedHeader
import com.istiak.auraplayer.ui.components.recordFrostedBackdrop
import com.istiak.auraplayer.ui.components.rememberScrollAwareGlassLayout
import com.istiak.auraplayer.ui.theme.LocalExtendedPalette
import java.util.Locale
import kotlin.math.roundToInt

private val GsCobaltActive = Color(0xFF2563EB)
private val GsActiveHighlightLight = Color(0xFFE3E6FA)
private val GsIconBgLight = Color(0xFFEEF2FF)
private val GsSlateHeaderLight = Color(0xFF434A6E)
private val GsDividerLight = Color(0xFFE8ECF8)

private enum class GesturePickerTarget {
    DOUBLE_TAP_LEFT,
    DOUBLE_TAP_CENTER,
    DOUBLE_TAP_RIGHT,
    MEDIA_PREVIOUS,
    MEDIA_PLAY_PAUSE,
    MEDIA_NEXT
}

/**
 * Dedicated Gestures & Controls Sub-Screen (`GestureSettingsScreen.kt`):
 * - Frozen `ScrollAwareFixedHeader`: `← Gestures & Controls`
 * - Section `| Swipe & Speed`: 13 uniform-surface gesture switches (state indicated solely by
 *   the Aura Blue `Switch`), Horizontal swipe sensitivity slider (`10-100`), Hold for multi-x
 *   speed slider (`1.5x-3.0x`).
 * - Section `| Double Tap & Subtitles`: Seek duration, Seek Area Width slider (`20%-45%`),
 *   Left/Center/Right tap actions, single tap center toggle, hold and drag subtitles toggle.
 * - Section `| Hardware Media Controls`: Previous, Play/Pause, Next action rows using the
 *   compact `MinimalSingleChoiceDialog`.
 */
@Composable
fun GestureSettingsScreen(
    settings: AppSettingsState,
    onUpdateHapticFeedback: (Boolean) -> Unit,
    onUpdateConfirmBeforeLeaving: (Boolean) -> Unit,
    onUpdateBrightnessGestures: (Boolean) -> Unit,
    onUpdateVolumeGestures: (Boolean) -> Unit,
    onUpdatePinchToZoom: (Boolean) -> Unit,
    onUpdatePinchToZoomSubtitles: (Boolean) -> Unit,
    onUpdateTwoFingerSpeed: (Boolean) -> Unit,
    onUpdateSwipeSubtitlesToSeek: (Boolean) -> Unit,
    onUpdateInvertSwipeSubtitles: (Boolean) -> Unit,
    onUpdateHorizontalSwipeToSeek: (Boolean) -> Unit,
    onUpdateShowSeekbarHidden: (Boolean) -> Unit,
    onUpdateSwipeUpCenterPlaylist: (Boolean) -> Unit,
    onUpdateHorizontalSwipeSensitivity: (Int) -> Unit,
    onUpdateHoldForSpeedMultiplier: (Float) -> Unit,
    onUpdateDoubleTapSeekSeconds: (Int) -> Unit,
    onUpdateDoubleTapSeekAreaWidth: (Int) -> Unit,
    onUpdateDoubleTapLeftAction: (String) -> Unit,
    onUpdateDoubleTapCenterAction: (String) -> Unit,
    onUpdateDoubleTapRightAction: (String) -> Unit,
    onUpdateSingleTapCenterGesture: (Boolean) -> Unit,
    onUpdateHoldAndDragSubtitles: (Boolean) -> Unit,
    onUpdateMediaPreviousAction: (String) -> Unit,
    onUpdateMediaPlayPauseAction: (String) -> Unit,
    onUpdateMediaNextAction: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val palette = LocalExtendedPalette.current
    var activePicker by remember { mutableStateOf<GesturePickerTarget?>(null) }

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
            .testTag("gesture_settings_screen")
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
            // Section | Swipe & Speed
            item(key = "sec_swipe_speed") {
                GsFrostedSection(title = "| Swipe & Speed") {
                    GsHighlightSwitchRow(
                        icon = Icons.Filled.Vibration,
                        title = "Haptic feedback",
                        subtitle = "Subtle tactile pulse on gesture and scrub milestones",
                        checked = settings.hapticFeedbackEnabled,
                        onCheckedChange = onUpdateHapticFeedback
                    )
                    GsHighlightSwitchRow(
                        icon = Icons.Filled.Brightness6,
                        title = "Brightness swipe",
                        subtitle = "Vertical swipe on left half adjusts screen brightness",
                        checked = settings.brightnessSwipeEnabled,
                        onCheckedChange = onUpdateBrightnessGestures
                    )
                    GsHighlightSwitchRow(
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        title = "Volume swipe + 200% boost",
                        subtitle = "Vertical swipe on right half adjusts volume & 200% boost",
                        checked = settings.volumeSwipeEnabled,
                        onCheckedChange = onUpdateVolumeGestures
                    )
                    GsHighlightSwitchRow(
                        icon = Icons.Filled.Pinch,
                        title = "Pinch to zoom",
                        subtitle = "Two-finger pinch scales video canvas smoothly",
                        checked = settings.pinchToZoomEnabled,
                        onCheckedChange = onUpdatePinchToZoom
                    )
                    GsHighlightSwitchRow(
                        icon = Icons.Filled.Subtitles,
                        title = "Pinch to zoom subtitles",
                        subtitle = "Pinch over captions to resize subtitle text",
                        checked = settings.pinchToZoomSubtitlesEnabled,
                        onCheckedChange = onUpdatePinchToZoomSubtitles
                    )
                    GsHighlightSwitchRow(
                        icon = Icons.Filled.Swipe,
                        title = "Horizontal swipe to seek",
                        subtitle = "Drag horizontally anywhere to scrub playback position",
                        checked = settings.horizontalSeekSwipeEnabled,
                        onCheckedChange = onUpdateHorizontalSwipeToSeek
                    )
                    // Horizontal swipe sensitivity slider (10-100)
                    val sensBand = when {
                        settings.horizontalSwipeSensitivity < 35 -> "Low"
                        settings.horizontalSwipeSensitivity > 70 -> "High"
                        else -> "Medium"
                    }
                    GsSliderRow(
                        icon = Icons.Filled.Swipe,
                        title = "Horizontal swipe sensitivity",
                        valueLabel = "${settings.horizontalSwipeSensitivity}/100 ($sensBand)",
                        value = settings.horizontalSwipeSensitivity.toFloat(),
                        valueRange = 10f..100f,
                        steps = 0,
                        onValueChange = { onUpdateHorizontalSwipeSensitivity(it.roundToInt()) }
                    )
                    GsHighlightSwitchRow(
                        icon = Icons.Filled.LinearScale,
                        title = "Show seekbar when controls are hidden",
                        subtitle = "Keep a slim wavy progress bar visible at bottom edge",
                        checked = settings.showSeekbarWhenControlsHidden,
                        onCheckedChange = onUpdateShowSeekbarHidden
                    )
                    GsHighlightSwitchRow(
                        icon = Icons.Filled.Speed,
                        title = "Two-finger swipe for speed",
                        subtitle = "Adjust playback speed in 0.25x steps",
                        checked = settings.twoFingerSwipeForSpeedEnabled,
                        onCheckedChange = onUpdateTwoFingerSpeed
                    )
                    // Hold for multi-x speed slider (1.5x-3.0x)
                    val snappedSpeed = ((settings.holdForSpeedMultiplier * 4f).roundToInt() / 4f)
                        .coerceIn(1.5f, 3.0f)
                    GsSliderRow(
                        icon = Icons.Filled.FastForward,
                        title = "Hold for multi-x speed",
                        valueLabel = String.format(Locale.US, "%.2fx", snappedSpeed),
                        value = snappedSpeed,
                        valueRange = 1.5f..3.0f,
                        steps = 5,
                        onValueChange = { raw ->
                            val snapped = ((raw * 4f).roundToInt() / 4f).coerceIn(1.5f, 3.0f)
                            onUpdateHoldForSpeedMultiplier(snapped)
                        }
                    )
                    GsHighlightSwitchRow(
                        icon = Icons.Filled.Subtitles,
                        title = "Swipe subtitles to seek",
                        subtitle = "Jump cleanly to the previous or next subtitle cue",
                        checked = settings.swipeSubtitlesToSeekEnabled,
                        onCheckedChange = onUpdateSwipeSubtitlesToSeek
                    )
                    GsHighlightSwitchRow(
                        icon = Icons.Filled.Subtitles,
                        title = "Invert swipe subtitles direction",
                        subtitle = "Reverse forward/backward subtitle cue swipe direction",
                        checked = settings.invertSwipeSubtitlesDirection,
                        onCheckedChange = onUpdateInvertSwipeSubtitles
                    )
                    GsHighlightSwitchRow(
                        icon = Icons.Filled.PlaylistPlay,
                        title = "Swipe up in center for playlist",
                        subtitle = "Open folder queue directly from center swipe up",
                        checked = settings.swipeUpCenterForPlaylistEnabled,
                        onCheckedChange = onUpdateSwipeUpCenterPlaylist
                    )
                    GsHighlightSwitchRow(
                        icon = Icons.Filled.WarningAmber,
                        title = "Confirm before leaving playback",
                        subtitle = "Prompt before closing an active video",
                        checked = settings.confirmBeforeLeavingPlayback,
                        onCheckedChange = onUpdateConfirmBeforeLeaving,
                        showDivider = false
                    )
                }
            }

            // Section | Double Tap & Subtitles
            item(key = "sec_double_tap") {
                GsFrostedSection(title = "| Double Tap & Subtitles") {
                    val seekOptions = listOf(5, 10, 15, 30)
                    GsInlinePillChoiceRow(
                        icon = Icons.Filled.FastForward,
                        title = "Double tap seek duration",
                        options = seekOptions.map { "${it}s" },
                        selectedIndex = seekOptions.indexOf(settings.doubleTapSeekSeconds).coerceAtLeast(1),
                        onSelectIndex = { idx -> onUpdateDoubleTapSeekSeconds(seekOptions[idx]) },
                        testTagPrefix = "gs_double_tap_duration"
                    )

                    GsSliderRow(
                        icon = Icons.Filled.TouchApp,
                        title = "Double tap seek area width",
                        valueLabel = "${settings.doubleTapSeekAreaWidthPercent.coerceIn(20, 45)}%",
                        value = settings.doubleTapSeekAreaWidthPercent.toFloat().coerceIn(20f, 45f),
                        valueRange = 20f..45f,
                        steps = 4,
                        onValueChange = { onUpdateDoubleTapSeekAreaWidth(it.roundToInt().coerceIn(20, 45)) }
                    )

                    GsActionRow(
                        icon = Icons.Filled.RadioButtonChecked,
                        title = "Double tap (left)",
                        valueText = settings.doubleTapLeftAction,
                        onClick = { activePicker = GesturePickerTarget.DOUBLE_TAP_LEFT }
                    )
                    GsActionRow(
                        icon = Icons.Filled.RadioButtonChecked,
                        title = "Double tap (center)",
                        valueText = settings.doubleTapCenterAction,
                        onClick = { activePicker = GesturePickerTarget.DOUBLE_TAP_CENTER }
                    )
                    GsActionRow(
                        icon = Icons.Filled.RadioButtonChecked,
                        title = "Double tap (right)",
                        valueText = settings.doubleTapRightAction,
                        onClick = { activePicker = GesturePickerTarget.DOUBLE_TAP_RIGHT }
                    )
                    GsHighlightSwitchRow(
                        icon = Icons.Filled.TouchApp,
                        title = "Use single tap for center gesture",
                        subtitle = "Trigger center Play/Pause with a single tap",
                        checked = settings.useSingleTapForCenterGesture,
                        onCheckedChange = onUpdateSingleTapCenterGesture
                    )
                    GsHighlightSwitchRow(
                        icon = Icons.Filled.DragIndicator,
                        title = "Hold and drag subtitles",
                        subtitle = "Reposition captions vertically by dragging",
                        checked = settings.holdAndDragSubtitlesEnabled,
                        onCheckedChange = onUpdateHoldAndDragSubtitles,
                        showDivider = false
                    )
                }
            }

            // Section | Hardware Media Controls
            item(key = "sec_media_controls") {
                GsFrostedSection(title = "| Hardware Media Controls") {
                    GsActionRow(
                        icon = Icons.Filled.SkipPrevious,
                        title = "Previous",
                        valueText = settings.mediaControlPreviousAction,
                        onClick = { activePicker = GesturePickerTarget.MEDIA_PREVIOUS }
                    )
                    GsActionRow(
                        icon = Icons.Filled.Pause,
                        title = "Play/Pause",
                        valueText = settings.mediaControlPlayPauseAction,
                        onClick = { activePicker = GesturePickerTarget.MEDIA_PLAY_PAUSE }
                    )
                    GsActionRow(
                        icon = Icons.Filled.SkipNext,
                        title = "Next",
                        valueText = settings.mediaControlNextAction,
                        onClick = { activePicker = GesturePickerTarget.MEDIA_NEXT },
                        showDivider = false
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
                        .testTag("gesture_settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = palette.brandTitleColor
                    )
                }
                Text(
                    text = "Gestures & Controls",
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.brandTitleColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    activePicker?.let { target ->
        val (title, options, selected) = when (target) {
            GesturePickerTarget.DOUBLE_TAP_LEFT -> Triple(
                "Double tap (left)",
                listOf("Seek", "Play/Pause", "Previous", "None"),
                settings.doubleTapLeftAction
            )
            GesturePickerTarget.DOUBLE_TAP_CENTER -> Triple(
                "Double tap (center)",
                listOf("Play/Pause", "Seek", "None"),
                settings.doubleTapCenterAction
            )
            GesturePickerTarget.DOUBLE_TAP_RIGHT -> Triple(
                "Double tap (right)",
                listOf("Seek", "Play/Pause", "Next", "None"),
                settings.doubleTapRightAction
            )
            GesturePickerTarget.MEDIA_PREVIOUS -> Triple(
                "Previous button action",
                listOf("Previous video", "Restart / Previous", "Seek back"),
                settings.mediaControlPreviousAction
            )
            GesturePickerTarget.MEDIA_PLAY_PAUSE -> Triple(
                "Play/Pause button action",
                listOf("Toggle play/pause", "Pause & mute"),
                settings.mediaControlPlayPauseAction
            )
            GesturePickerTarget.MEDIA_NEXT -> Triple(
                "Next button action",
                listOf("Next video", "Seek forward"),
                settings.mediaControlNextAction
            )
        }

        GsOptionModal(
            title = title,
            options = options,
            selected = selected,
            onSelect = { chosen ->
                when (target) {
                    GesturePickerTarget.DOUBLE_TAP_LEFT -> onUpdateDoubleTapLeftAction(chosen)
                    GesturePickerTarget.DOUBLE_TAP_CENTER -> onUpdateDoubleTapCenterAction(chosen)
                    GesturePickerTarget.DOUBLE_TAP_RIGHT -> onUpdateDoubleTapRightAction(chosen)
                    GesturePickerTarget.MEDIA_PREVIOUS -> onUpdateMediaPreviousAction(chosen)
                    GesturePickerTarget.MEDIA_PLAY_PAUSE -> onUpdateMediaPlayPauseAction(chosen)
                    GesturePickerTarget.MEDIA_NEXT -> onUpdateMediaNextAction(chosen)
                }
                activePicker = null
            },
            onDismiss = { activePicker = null }
        )
    }
}

@Composable
private fun GsFrostedSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val headerColor = if (isDark) Color(0xFFB7C5E8) else GsSlateHeaderLight
    val cardShape = RoundedCornerShape(16.dp)
    val cardBg = if (isDark) Color(0xFF161B2E).copy(alpha = 0.80f) else Color.White.copy(alpha = 0.84f)
    val borderBrush = if (isDark) {
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color(0xFF60A5FA).copy(alpha = 0.14f)))
    } else {
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.94f), Color(0xFFD6D9F8).copy(alpha = 0.72f)))
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
                    ambientColor = Color(0xFF434A6E).copy(alpha = if (isDark) 0.28f else 0.06f),
                    spotColor = Color(0xFF2563EB).copy(alpha = if (isDark) 0.28f else 0.08f)
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
private fun GsIconBadge(icon: ImageVector) {
    val isDark = LocalExtendedPalette.current.isDark
    val bg = if (isDark) GsCobaltActive.copy(alpha = 0.18f) else GsIconBgLight
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDark) Color(0xFF60A5FA) else GsCobaltActive,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun GsHighlightSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showDivider: Boolean = true
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 2.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            GsIconBadge(icon = icon)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = palette.primaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    color = palette.secondaryText,
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    modifier = Modifier.scale(0.85f),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = GsCobaltActive,
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
                color = if (isDark) Color.White.copy(alpha = 0.08f) else GsDividerLight,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

@Composable
private fun GsSliderRow(
    icon: ImageVector,
    title: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    showDivider: Boolean = true
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark

    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GsIconBadge(icon = icon)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    color = palette.primaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (isDark) GsCobaltActive.copy(alpha = 0.25f) else GsActiveHighlightLight)
                        .padding(horizontal = 9.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = valueLabel,
                        color = if (isDark) Color(0xFFBFDBFE) else GsCobaltActive,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                colors = SliderDefaults.colors(
                    thumbColor = GsCobaltActive,
                    activeTrackColor = GsCobaltActive,
                    inactiveTrackColor = if (isDark) Color.White.copy(alpha = 0.16f) else GsActiveHighlightLight
                ),
                modifier = Modifier.padding(start = 42.dp)
            )
        }
        if (showDivider) {
            HorizontalDivider(
                thickness = 1.dp,
                color = if (isDark) Color.White.copy(alpha = 0.08f) else GsDividerLight,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

@Composable
private fun GsInlinePillChoiceRow(
    icon: ImageVector,
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    testTagPrefix: String,
    showDivider: Boolean = true
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val inactiveBg = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F4FC)
    val inactiveBorder = if (isDark) Color.White.copy(alpha = 0.12f) else GsDividerLight

    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GsIconBadge(icon = icon)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    color = palette.primaryText,
                    fontSize = 14.sp,
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
                            .background(if (isSelected) GsCobaltActive else inactiveBg)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color(0xFFD6D9F8).copy(alpha = 0.7f) else inactiveBorder,
                                shape = chipShape
                            )
                            .clickable { onSelectIndex(idx) }
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                            .testTag("${testTagPrefix}_$idx"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else palette.primaryText.copy(alpha = 0.86f),
                            fontSize = 12.sp,
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
                color = if (isDark) Color.White.copy(alpha = 0.08f) else GsDividerLight,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

@Composable
private fun GsActionRow(
    icon: ImageVector,
    title: String,
    valueText: String,
    onClick: () -> Unit,
    showDivider: Boolean = true
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            GsIconBadge(icon = icon)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                color = palette.primaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isDark) GsCobaltActive.copy(alpha = 0.24f) else GsActiveHighlightLight)
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(
                    text = valueText,
                    color = if (isDark) Color(0xFFBFDBFE) else GsCobaltActive,
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
                color = if (isDark) Color.White.copy(alpha = 0.08f) else GsDividerLight,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

@Composable
private fun GsOptionModal(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    MinimalSingleChoiceDialog(
        title = title,
        options = options,
        selected = selected,
        onSelect = onSelect,
        onDismiss = onDismiss
    )
}

