package com.istiak.auraplayer

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.istiak.auraplayer.ui.theme.DangerRed
import com.istiak.auraplayer.ui.theme.PlayerHudGlassBorder
import com.istiak.auraplayer.ui.theme.PlayerSeekPeriwinkle
import java.util.Locale
import kotlin.math.roundToInt

sealed class SleepTimerSelection {
    data object Off : SleepTimerSelection()
    data class Minutes(val minutes: Int) : SleepTimerSelection()
    data object EndOfVideo : SleepTimerSelection()
}

enum class PlayerDrawerType {
    NONE,
    AUDIO,
    SUBTITLE,
    EQUALIZER,
    MORE_OPTIONS
}

enum class StereoMode(val label: String) {
    STEREO("Stereo"),
    MONO("Mono"),
    REVERSE_STEREO("Reverse Stereo");

    fun next(): StereoMode {
        val all = entries
        return all[(ordinal + 1) % all.size]
    }
}

data class AudioTrackUiModel(
    val id: String,
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val detail: String,
    val isSelected: Boolean
)

data class SubtitleTrackUiModel(
    val id: String,
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val isExternal: Boolean,
    val isSelected: Boolean
)

data class SubtitleStyleConfig(
    val fontSizeSp: Float = 18f,
    val isBold: Boolean = false,
    val textColorArgb: Int = android.graphics.Color.WHITE,
    val hasBackground: Boolean = true,
    val bottomMarginFraction: Float = 0.08f
)

enum class EqPreset(val label: String, val bandsDb: List<Float>) {
    CUSTOM("Custom", listOf(0f, 0f, 0f, 0f, 0f)),
    NORMAL("Normal", listOf(0f, 0f, 0f, 0f, 0f)),
    CLASSICAL("Classical", listOf(4f, 3f, -1f, 3f, 4f)),
    DANCE("Dance", listOf(5f, 2f, 0f, 3f, 5f))
}

private val MxSkyBlueActive = Color(0xFF38BDF8)
private val MxSoftBlueAccent = Color(0xFF60A5FA)
private val MxDrawerDividerColor = Color.White.copy(alpha = 0.16f)
private val MxTextDropShadow = Shadow(
    color = Color.Black.copy(alpha = 0.75f),
    offset = Offset(0f, 1.5f),
    blurRadius = 3.5f
)

/**
 * MX Player-Style Translucent Right-Side Overlay Drawer Host:
 * - Full-height right-aligned drawer (`340.dp` in landscape / `290.dp` in portrait)
 * - Smooth horizontal gradient backdrop (`Color.Black.copy(alpha = 0.0f)` -> `0.62f` -> `Color(0xFF0B0F19).copy(alpha = 0.86f)`)
 *   so the playing video remains softly visible beneath crisp white text.
 * - Slide-in (`240ms FastOutSlowInEasing`) & slide-out (`210ms FastOutSlowInEasing`) + fade animations.
 * - Tapping anywhere on the left video area outside the drawer smoothly slides the drawer out to the right.
 */
@Composable
fun RightSidePlayerDrawer(
    visible: Boolean,
    isLandscape: Boolean = true,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val drawerWidth = if (isLandscape) 340.dp else 290.dp
    val drawerGradient = remember {
        Brush.horizontalGradient(
            0.00f to Color.Black.copy(alpha = 0.0f),
            0.14f to Color.Black.copy(alpha = 0.42f),
            0.38f to Color.Black.copy(alpha = 0.62f),
            1.00f to Color(0xFF0B0F19).copy(alpha = 0.86f)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(12f)
    ) {
        // Transparent tap-outside catcher over the unobstructed video area
        if (visible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
                    .testTag("drawer_outside_scrim")
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(durationMillis = 180)),
            exit = slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(durationMillis = 210, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(durationMillis = 160)),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
        ) {
            Box(
                modifier = Modifier
                    .width(drawerWidth)
                    .fillMaxHeight()
                    .background(drawerGradient)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(start = 26.dp, end = 18.dp, top = 16.dp, bottom = 14.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun MxCustomRadioIndicator(
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(20.dp)) {
        val strokeWidthPx = 1.8.dp.toPx()
        val outerRadius = (size.minDimension - strokeWidthPx) / 2f
        drawCircle(
            color = if (selected) MxSkyBlueActive else Color.White.copy(alpha = 0.65f),
            radius = outerRadius,
            style = Stroke(width = strokeWidthPx)
        )
        if (selected) {
            drawCircle(
                color = MxSkyBlueActive,
                radius = outerRadius * 0.52f
            )
        }
    }
}

@Composable
private fun MxRoundedSquareCheckbox(
    checked: Boolean,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(19.dp)
            .clip(shape)
            .background(if (checked) MxSkyBlueActive else Color.Transparent)
            .border(
                width = 1.6.dp,
                color = if (checked) MxSkyBlueActive else Color.White.copy(alpha = 0.65f),
                shape = shape
            )
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Color(0xFF06101E),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun MxInlineStepperRow(
    label: String,
    valueText: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    decrementTag: String = "",
    incrementTag: String = "",
    badgeTag: String = ""
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            style = TextStyle(shadow = MxTextDropShadow),
            modifier = Modifier.weight(1f)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(0.7.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(8.dp))
                    .clickable(onClick = onDecrement)
                    .then(if (decrementTag.isNotEmpty()) Modifier.testTag(decrementTag) else Modifier)
            ) {
                Icon(
                    imageVector = Icons.Filled.Remove,
                    contentDescription = "Decrease $label",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .widthIn(min = 68.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.14f))
                    .border(0.7.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                    .clickable(onClick = onReset)
                    .padding(horizontal = 8.dp)
                    .then(if (badgeTag.isNotEmpty()) Modifier.testTag(badgeTag) else Modifier)
            ) {
                Text(
                    text = valueText,
                    color = Color.White,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(0.7.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(8.dp))
                    .clickable(onClick = onIncrement)
                    .then(if (incrementTag.isNotEmpty()) Modifier.testTag(incrementTag) else Modifier)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Increase $label",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * MX Player-Style Audio Track Side Panel (`PlayerDrawerType.AUDIO`):
 * - Bold `Audio Track` header (`18.sp`, `FontWeight.SemiBold`, `Color.White`)
 * - Scrollable track list with circular `#38BDF8` radio buttons + 2-line track metadata,
 *   `Disable` radio option right below tracks, and `Use SW audio decoder` square checkbox below a thin divider.
 * - Bottom action & stepper rows separated by a thin `0.6.dp` divider:
 *   `Open`, `Stereo mode`, and `Synchronization` (`-` `[ 0.00s ]` `+` stepping by `±0.10s`).
 */
@Composable
fun AudioTrackDrawerContent(
    tracks: List<AudioTrackUiModel>,
    isAudioDisabled: Boolean,
    useSwAudioDecoder: Boolean,
    audioSyncOffsetMs: Int,
    stereoMode: StereoMode = StereoMode.STEREO,
    onSelectTrack: (AudioTrackUiModel) -> Unit,
    onSelectDefaultAudio: () -> Unit = {},
    onDisableAudio: () -> Unit,
    onToggleSwAudioDecoder: (Boolean) -> Unit,
    onOpenExternalAudio: () -> Unit = {},
    onCycleStereoMode: () -> Unit = {},
    onAdjustAudioSyncMs: (Int) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("audio_track_drawer")
    ) {
        Text(
            text = "Audio Track",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            style = TextStyle(shadow = MxTextDropShadow),
            modifier = Modifier.padding(bottom = 10.dp)
        )

        // Scrollable Track List + Disable + Use SW audio decoder
        Column(
            modifier = Modifier
                .weight(1f, fill = true)
                .verticalScroll(rememberScrollState())
        ) {
            if (tracks.isEmpty()) {
                val defaultSelected = !isAudioDisabled
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onSelectDefaultAudio)
                        .padding(vertical = 9.dp, horizontal = 4.dp)
                        .testTag("drawer_audio_default")
                ) {
                    MxCustomRadioIndicator(selected = defaultSelected)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Track 1 - Primary Audio",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = if (defaultSelected) FontWeight.SemiBold else FontWeight.Normal,
                            style = TextStyle(shadow = MxTextDropShadow)
                        )
                        Text(
                            text = "AAC / Stereo / 48 kHz",
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                tracks.forEach { track ->
                    val selected = !isAudioDisabled && track.isSelected
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelectTrack(track) }
                            .padding(vertical = 9.dp, horizontal = 4.dp)
                    ) {
                        MxCustomRadioIndicator(selected = selected)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.label,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                style = TextStyle(shadow = MxTextDropShadow),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (track.detail.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = track.detail,
                                    color = Color.White.copy(alpha = 0.72f),
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Disable radio option right below tracks
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onDisableAudio)
                    .padding(vertical = 9.dp, horizontal = 4.dp)
                    .testTag("drawer_audio_disable")
            ) {
                MxCustomRadioIndicator(selected = isAudioDisabled)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Disable",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = if (isAudioDisabled) FontWeight.SemiBold else FontWeight.Normal,
                    style = TextStyle(shadow = MxTextDropShadow)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(thickness = 0.6.dp, color = MxDrawerDividerColor)
            Spacer(modifier = Modifier.height(8.dp))

            // Square checkbox option: Use SW audio decoder
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onToggleSwAudioDecoder(!useSwAudioDecoder) }
                    .padding(vertical = 9.dp, horizontal = 4.dp)
                    .testTag("drawer_sw_audio_checkbox")
            ) {
                MxRoundedSquareCheckbox(checked = useSwAudioDecoder)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Use SW audio decoder",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    style = TextStyle(shadow = MxTextDropShadow)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        HorizontalDivider(thickness = 0.6.dp, color = MxDrawerDividerColor)
        Spacer(modifier = Modifier.height(4.dp))

        // Bottom Action & Stepper Rows: Open, Stereo mode, Synchronization
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onOpenExternalAudio)
                .padding(vertical = 10.dp, horizontal = 4.dp)
                .testTag("drawer_open_audio_file")
        ) {
            Text(
                text = "Open",
                color = Color.White,
                fontSize = 14.sp,
                style = TextStyle(shadow = MxTextDropShadow)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onCycleStereoMode)
                .padding(vertical = 10.dp, horizontal = 4.dp)
                .testTag("drawer_stereo_mode_row")
        ) {
            Text(
                text = "Stereo mode",
                color = Color.White,
                fontSize = 14.sp,
                style = TextStyle(shadow = MxTextDropShadow)
            )
            Text(
                text = stereoMode.label,
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 13.sp
            )
        }

        val audioSyncSeconds = audioSyncOffsetMs / 1000.0
        val formattedAudioSync = String.format(Locale.US, "%.2fs", audioSyncSeconds)
        MxInlineStepperRow(
            label = "Synchronization",
            valueText = formattedAudioSync,
            onDecrement = { onAdjustAudioSyncMs(audioSyncOffsetMs - 100) },
            onIncrement = { onAdjustAudioSyncMs(audioSyncOffsetMs + 100) },
            onReset = { onAdjustAudioSyncMs(0) },
            decrementTag = "audio_sync_minus",
            incrementTag = "audio_sync_plus",
            badgeTag = "audio_sync_value"
        )
    }
}

/**
 * MX Player-Style Subtitle Side Panel (`PlayerDrawerType.SUBTITLE`):
 * - Bold `Subtitle` header (`18.sp`, `FontWeight.SemiBold`, `Color.White`)
 * - Scrollable track list with Rounded Square Checkboxes (`RoundedCornerShape(4.dp)`, `#38BDF8` fill when checked)
 * - Bottom Action & Stepper Rows separated by thin `0.6.dp` divider:
 *   `Open`, `Online subtitles`, `Subtitle Style / Appearance`, `Synchronization` (`±0.1s`), and `Speed` (`±0.5%`).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubtitleDrawerContent(
    tracks: List<SubtitleTrackUiModel>,
    areSubtitlesEnabled: Boolean,
    subtitleSyncOffsetMs: Long,
    subtitleSpeed: Float,
    styleConfig: SubtitleStyleConfig,
    onSelectTrack: (SubtitleTrackUiModel) -> Unit,
    onDisableSubtitles: () -> Unit,
    onOpenExternalSubtitle: () -> Unit,
    onSearchOnlineSubtitles: () -> Unit = {},
    onAdjustSubtitleSyncMs: (Long) -> Unit,
    onAdjustSubtitleSpeed: (Float) -> Unit,
    onUpdateStyleConfig: (SubtitleStyleConfig) -> Unit,
    onClose: () -> Unit
) {
    var showStyleSection by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("subtitle_drawer")
    ) {
        Text(
            text = "Subtitle",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            style = TextStyle(shadow = MxTextDropShadow),
            modifier = Modifier.padding(bottom = 10.dp)
        )

        // Scrollable Subtitle Track List
        Column(
            modifier = Modifier
                .weight(1f, fill = true)
                .verticalScroll(rememberScrollState())
        ) {
            tracks.forEach { track ->
                val checked = areSubtitlesEnabled && track.isSelected
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            if (checked) {
                                onDisableSubtitles()
                            } else {
                                onSelectTrack(track)
                            }
                        }
                        .padding(vertical = 9.dp, horizontal = 4.dp)
                ) {
                    MxRoundedSquareCheckbox(checked = checked)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (track.isExternal) "${track.label} [External]" else track.label,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal,
                        style = TextStyle(shadow = MxTextDropShadow),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onDisableSubtitles)
                    .padding(vertical = 9.dp, horizontal = 4.dp)
                    .testTag("drawer_subtitle_disable")
            ) {
                MxRoundedSquareCheckbox(checked = !areSubtitlesEnabled)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Disable",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = if (!areSubtitlesEnabled) FontWeight.SemiBold else FontWeight.Normal,
                    style = TextStyle(shadow = MxTextDropShadow)
                )
            }

            // Expandable inline Subtitle Style / Appearance customization controls
            AnimatedVisibility(
                visible = showStyleSection,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(0.6.dp, MxDrawerDividerColor, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Font Size (${styleConfig.fontSizeSp.toInt()}sp)",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                    Slider(
                        value = styleConfig.fontSizeSp,
                        onValueChange = { onUpdateStyleConfig(styleConfig.copy(fontSizeSp = it)) },
                        valueRange = 12f..32f,
                        colors = SliderDefaults.colors(
                            thumbColor = MxSkyBlueActive,
                            activeTrackColor = MxSkyBlueActive,
                            inactiveTrackColor = Color.White.copy(alpha = 0.22f)
                        )
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Bold Text", color = Color.White.copy(alpha = 0.88f), fontSize = 12.5.sp)
                        Switch(
                            checked = styleConfig.isBold,
                            onCheckedChange = { onUpdateStyleConfig(styleConfig.copy(isBold = it)) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MxSkyBlueActive
                            )
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Shadow Background", color = Color.White.copy(alpha = 0.88f), fontSize = 12.5.sp)
                        Switch(
                            checked = styleConfig.hasBackground,
                            onCheckedChange = { onUpdateStyleConfig(styleConfig.copy(hasBackground = it)) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MxSkyBlueActive
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Text Color", color = Color.White.copy(alpha = 0.82f), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(
                            android.graphics.Color.WHITE,
                            android.graphics.Color.YELLOW,
                            android.graphics.Color.CYAN,
                            android.graphics.Color.GREEN
                        ).forEach { colorArgb ->
                            val isSelected = styleConfig.textColorArgb == colorArgb
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorArgb))
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) MxSkyBlueActive else Color.White.copy(alpha = 0.4f),
                                        shape = CircleShape
                                    )
                                    .clickable { onUpdateStyleConfig(styleConfig.copy(textColorArgb = colorArgb)) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Bottom Margin (${(styleConfig.bottomMarginFraction * 100).toInt()}%)",
                        color = Color.White.copy(alpha = 0.82f),
                        fontSize = 12.sp
                    )
                    Slider(
                        value = styleConfig.bottomMarginFraction,
                        onValueChange = { onUpdateStyleConfig(styleConfig.copy(bottomMarginFraction = it)) },
                        valueRange = 0.02f..0.28f,
                        colors = SliderDefaults.colors(
                            thumbColor = MxSkyBlueActive,
                            activeTrackColor = MxSkyBlueActive,
                            inactiveTrackColor = Color.White.copy(alpha = 0.22f)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        HorizontalDivider(thickness = 0.6.dp, color = MxDrawerDividerColor)
        Spacer(modifier = Modifier.height(4.dp))

        // Bottom Action & Stepper Rows: Open, Online subtitles, Subtitle Style, Synchronization, Speed
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onOpenExternalSubtitle)
                .padding(vertical = 9.dp, horizontal = 4.dp)
                .testTag("drawer_open_subtitle_file")
        ) {
            Text(
                text = "Open",
                color = Color.White,
                fontSize = 14.sp,
                style = TextStyle(shadow = MxTextDropShadow)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onSearchOnlineSubtitles)
                .padding(vertical = 9.dp, horizontal = 4.dp)
                .testTag("drawer_search_online_subtitle")
        ) {
            Text(
                text = "Online subtitles",
                color = Color.White,
                fontSize = 14.sp,
                style = TextStyle(shadow = MxTextDropShadow)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { showStyleSection = !showStyleSection }
                .padding(vertical = 9.dp, horizontal = 4.dp)
                .testTag("drawer_subtitle_style_row")
        ) {
            Text(
                text = "Subtitle Style / Appearance",
                color = Color.White,
                fontSize = 14.sp,
                style = TextStyle(shadow = MxTextDropShadow)
            )
            Text(
                text = "${styleConfig.fontSizeSp.toInt()}sp",
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 12.5.sp
            )
        }

        val subSyncSec = subtitleSyncOffsetMs / 1000.0
        MxInlineStepperRow(
            label = "Synchronization",
            valueText = String.format(Locale.US, "%.1fs", subSyncSec),
            onDecrement = { onAdjustSubtitleSyncMs(subtitleSyncOffsetMs - 100L) },
            onIncrement = { onAdjustSubtitleSyncMs(subtitleSyncOffsetMs + 100L) },
            onReset = { onAdjustSubtitleSyncMs(0L) },
            decrementTag = "subtitle_sync_minus",
            incrementTag = "subtitle_sync_plus",
            badgeTag = "subtitle_sync_value"
        )

        val subSpeedPercent = subtitleSpeed * 100f
        MxInlineStepperRow(
            label = "Speed",
            valueText = String.format(Locale.US, "%.1f%%", subSpeedPercent),
            onDecrement = {
                val next = ((subtitleSpeed - 0.005f) * 200f).roundToInt() / 200f
                onAdjustSubtitleSpeed(next.coerceIn(0.5f, 2.0f))
            },
            onIncrement = {
                val next = ((subtitleSpeed + 0.005f) * 200f).roundToInt() / 200f
                onAdjustSubtitleSpeed(next.coerceIn(0.5f, 2.0f))
            },
            onReset = { onAdjustSubtitleSpeed(1.0f) },
            decrementTag = "subtitle_speed_minus",
            incrementTag = "subtitle_speed_plus",
            badgeTag = "subtitle_speed_value"
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EqualizerDrawerContent(
    selectedPreset: EqPreset,
    bandLevelsDb: List<Float>,
    bassBoostStrength: Float,
    onSelectPreset: (EqPreset) -> Unit,
    onBandLevelChange: (Int, Float) -> Unit,
    onBassBoostChange: (Float) -> Unit,
    onClose: () -> Unit
) {
    val freqLabels = listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag("equalizer_drawer")
    ) {
        DrawerHeader(title = "Equalizer", onClose = onClose)
        Spacer(modifier = Modifier.height(10.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EqPreset.entries.forEach { preset ->
                val selected = selectedPreset == preset
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) PlayerSeekPeriwinkle else Color.White.copy(alpha = 0.12f))
                        .clickable { onSelectPreset(preset) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = preset.label,
                        color = if (selected) Color.Black else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5-band vertical equalizer sliders
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            freqLabels.forEachIndexed { idx, label ->
                val dbVal = bandLevelsDb.getOrElse(idx) { 0f }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "${if (dbVal > 0f) "+" else ""}${dbVal.toInt()}dB",
                        color = PlayerSeekPeriwinkle,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .width(42.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Slider(
                            value = dbVal,
                            onValueChange = { onBandLevelChange(idx, it) },
                            valueRange = -12f..12f,
                            colors = SliderDefaults.colors(
                                thumbColor = PlayerSeekPeriwinkle,
                                activeTrackColor = PlayerSeekPeriwinkle,
                                inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier
                                .graphicsLayer {
                                    rotationZ = 270f
                                    transformOrigin = TransformOrigin(0f, 0f)
                                }
                                .layout { measurable, constraints ->
                                    val placeable = measurable.measure(
                                        Constraints(
                                            minWidth = constraints.minHeight,
                                            maxWidth = constraints.maxHeight,
                                            minHeight = constraints.minWidth,
                                            maxHeight = constraints.maxWidth
                                        )
                                    )
                                    layout(placeable.height, placeable.width) {
                                        placeable.place(-placeable.width, 0)
                                    }
                                }
                                .width(130.dp)
                                .height(36.dp)
                        )
                    }
                    Text(
                        text = label,
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Bass Boost", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = "${(bassBoostStrength * 100).toInt()}%",
                color = PlayerSeekPeriwinkle,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = bassBoostStrength,
            onValueChange = onBassBoostChange,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = PlayerSeekPeriwinkle,
                activeTrackColor = PlayerSeekPeriwinkle,
                inactiveTrackColor = Color.White.copy(alpha = 0.25f)
            )
        )
    }
}

@Composable
fun MoreOptionsDrawerContent(
    longPress2xEnabled: Boolean,
    sleepTimerStatusLabel: String?,
    onCycleAspectRatio: () -> Unit,
    onToggleLongPress2x: (Boolean) -> Unit,
    onOpenSleepTimer: () -> Unit,
    onShowInformation: () -> Unit,
    onShareVideo: () -> Unit,
    onRenameVideo: () -> Unit,
    onDeleteVideo: () -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag("more_options_drawer")
    ) {
        DrawerHeader(title = "Options", onClose = onClose)
        Spacer(modifier = Modifier.height(12.dp))

        // Grid of MX Player-style tool tiles (2 columns)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DrawerToolTile(
                    icon = Icons.Filled.AspectRatio,
                    label = "Aspect Ratio",
                    onClick = onCycleAspectRatio,
                    modifier = Modifier.weight(1f)
                )
                DrawerToolTile(
                    icon = Icons.Filled.FastForward,
                    label = if (longPress2xEnabled) "2.0X Hold: ON" else "2.0X Hold: OFF",
                    isHighlighted = longPress2xEnabled,
                    onClick = { onToggleLongPress2x(!longPress2xEnabled) },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DrawerToolTile(
                    icon = Icons.Filled.Info,
                    label = "Information",
                    onClick = onShowInformation,
                    modifier = Modifier.weight(1f)
                )
                DrawerToolTile(
                    icon = Icons.Filled.Timer,
                    label = sleepTimerStatusLabel ?: "Sleep Timer",
                    isHighlighted = sleepTimerStatusLabel != null,
                    onClick = onOpenSleepTimer,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DrawerToolTile(
                    icon = Icons.Filled.Share,
                    label = "Share",
                    onClick = onShareVideo,
                    modifier = Modifier.weight(1f)
                )
                DrawerToolTile(
                    icon = Icons.Filled.DriveFileRenameOutline,
                    label = "Rename",
                    onClick = onRenameVideo,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DrawerToolTile(
                    icon = Icons.Filled.DeleteOutline,
                    label = "Delete",
                    tint = DangerRed,
                    onClick = onDeleteVideo,
                    modifier = Modifier.weight(1f)
                )
                DrawerToolTile(
                    icon = Icons.Filled.Settings,
                    label = "Settings",
                    onClick = onOpenSettings,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DrawerToolTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    tint: Color = Color.White
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .height(78.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isHighlighted) PlayerSeekPeriwinkle.copy(alpha = 0.22f)
                else Color.White.copy(alpha = 0.08f)
            )
            .border(
                width = 1.dp,
                color = if (isHighlighted) PlayerSeekPeriwinkle else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isHighlighted) PlayerSeekPeriwinkle else tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = if (isHighlighted) PlayerSeekPeriwinkle else tint,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DrawerHeader(
    title: String,
    onClose: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            style = TextStyle(shadow = MxTextDropShadow)
        )
        IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Close",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Responsive Floating Playback Speed Control Bar (`0.25x` to `4.00x` in `0.05x` steps):
 * - Elevated with `zIndex(20f)` and an explicit click consumer so touches on the slider,
 *   `-`, `+`, `↻` (reset `1.0x`), or `✕` (close) never leak through to the video surface.
 */
@Composable
fun FloatingPlaybackSpeedBar(
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    fun snapSpeed(raw: Float): Float {
        return (((raw * 20f).roundToInt()) / 20f).coerceIn(0.25f, 4.0f)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .zIndex(20f)
            .widthIn(max = 460.dp)
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(elevation = 16.dp, shape = RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0F172A).copy(alpha = 0.90f))
            .border(1.dp, PlayerHudGlassBorder, RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("floating_speed_bar")
    ) {
        IconButton(
            onClick = { onSpeedChange(snapSpeed(speed - 0.05f)) },
            modifier = Modifier
                .size(40.dp)
                .testTag("speed_bar_minus")
        ) {
            Icon(Icons.Filled.Remove, contentDescription = "Slower", tint = Color.White)
        }

        Text(
            text = String.format(Locale.US, "%.2fx", speed),
            color = MxSoftBlueAccent,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .width(52.dp)
                .testTag("speed_bar_value_text"),
            textAlign = TextAlign.Center
        )

        Slider(
            value = speed.coerceIn(0.25f, 4.0f),
            onValueChange = { newVal ->
                onSpeedChange(snapSpeed(newVal))
            },
            valueRange = 0.25f..4.0f,
            colors = SliderDefaults.colors(
                thumbColor = MxSoftBlueAccent,
                activeTrackColor = MxSoftBlueAccent,
                inactiveTrackColor = Color.White.copy(alpha = 0.24f)
            ),
            modifier = Modifier
                .weight(1f)
                .testTag("speed_bar_slider")
        )

        IconButton(
            onClick = { onSpeedChange(snapSpeed(speed + 0.05f)) },
            modifier = Modifier
                .size(40.dp)
                .testTag("speed_bar_plus")
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Faster", tint = Color.White)
        }

        IconButton(
            onClick = { onSpeedChange(1.0f) },
            modifier = Modifier
                .size(40.dp)
                .testTag("speed_bar_reset")
        ) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = "Reset 1.0x",
                tint = Color.White.copy(alpha = 0.88f)
            )
        }

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .size(40.dp)
                .testTag("speed_bar_close")
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Close Speed Bar",
                tint = Color.White.copy(alpha = 0.88f)
            )
        }
    }
}

/**
 * Minimalist, Clean Liquid Glass Decoder Mode Selector (`DecoderSelectorDialog`):
 * - Compact frosted dark glass surface (`RoundedCornerShape(24.dp)`, `Color(0xFF111622).copy(alpha = 0.88f)`,
 *   subtle `1.dp` `Color.White.copy(alpha = 0.14f)` hairline border, width `340.dp`).
 * - Simple header (`Decoder Mode` in `17.sp` `FontWeight.SemiBold` and minimal borderless `✕` icon button).
 * - Borderless list rows separated only by thin `0.5.dp` hairlines (`Color.White.copy(alpha = 0.10f)`).
 * - Left monochromatic tag (`HW`, `HW+`, `SW`), center title + concise 1-line subtitle,
 *   and right minimal radio indicator (`#60A5FA` dot/ring when selected, `Color.White.copy(alpha = 0.30f)` ring when unselected).
 */
@Composable
fun DecoderSelectorDialog(
    activeDecoder: DecoderPriority,
    onSelectDecoder: (DecoderPriority) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetShape = RoundedCornerShape(24.dp)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .width(340.dp)
                .shadow(
                    elevation = 24.dp,
                    shape = sheetShape,
                    ambientColor = Color.Black.copy(alpha = 0.55f),
                    spotColor = Color.Black.copy(alpha = 0.75f)
                )
                .clip(sheetShape)
                .background(Color(0xFF111622).copy(alpha = 0.88f))
                .border(1.dp, Color.White.copy(alpha = 0.14f), sheetShape)
                .padding(vertical = 16.dp)
                .testTag("decoder_selector_dialog")
        ) {
            // Simple minimalist header: "Decoder Mode" (17.sp SemiBold) + borderless ✕ button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, bottom = 8.dp)
            ) {
                Text(
                    text = "Decoder Mode",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("decoder_dialog_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close Decoder Mode",
                        tint = Color.White.copy(alpha = 0.72f),
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = Color.White.copy(alpha = 0.10f)
            )

            val entries = DecoderPriority.entries
            entries.forEachIndexed { index, mode ->
                val isSelected = mode == activeDecoder
                val titleText = when (mode) {
                    DecoderPriority.HW -> "HW decoder"
                    DecoderPriority.HW_PLUS -> "HW+ decoder"
                    DecoderPriority.SW -> "SW decoder"
                }
                val subtitleText = when (mode) {
                    DecoderPriority.HW -> "Hardware MediaCodec video & audio"
                    DecoderPriority.HW_PLUS -> "Hardware video + FFmpeg SW audio"
                    DecoderPriority.SW -> "Full software decoding for all formats"
                }
                val itemTag = when (mode) {
                    DecoderPriority.HW -> "decoder_option_hw"
                    DecoderPriority.HW_PLUS -> "decoder_option_hw_plus"
                    DecoderPriority.SW -> "decoder_option_sw"
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSelected) Color.White.copy(alpha = 0.06f)
                            else Color.Transparent
                        )
                        .clickable {
                            onSelectDecoder(mode)
                            onDismiss()
                        }
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                        .testTag(itemTag)
                ) {
                    // Left: Clean monochromatic tag without a bright blue square fill
                    Text(
                        text = mode.badgeText,
                        color = if (isSelected) Color.White else Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(38.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    // Center: Title (15.sp Medium) + concise 1-line subtitle (12.sp, 0.60f alpha)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = titleText,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitleText,
                            color = Color.White.copy(alpha = 0.60f),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Right: Minimal radio indicator (#60A5FA soft sky-blue dot when selected, subtle 0.30f ring when unselected)
                    Canvas(modifier = Modifier.size(18.dp)) {
                        val ringStroke = 1.6.dp.toPx()
                        val radius = (size.minDimension - ringStroke) / 2f
                        drawCircle(
                            color = if (isSelected) MxSoftBlueAccent else Color.White.copy(alpha = 0.30f),
                            radius = radius,
                            style = Stroke(width = ringStroke)
                        )
                        if (isSelected) {
                            drawCircle(
                                color = MxSoftBlueAccent,
                                radius = radius * 0.52f
                            )
                        }
                    }
                }

                if (index < entries.lastIndex) {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = Color.White.copy(alpha = 0.10f),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SleepTimerDialog(
    activeSelection: SleepTimerSelection,
    onSelectOption: (SleepTimerSelection) -> Unit,
    onDismiss: () -> Unit
) {
    val options: List<Triple<SleepTimerSelection, String, String>> = listOf(
        Triple(SleepTimerSelection.Off, "Off", "off"),
        Triple(SleepTimerSelection.Minutes(15), "15 min", "15"),
        Triple(SleepTimerSelection.Minutes(30), "30 min", "30"),
        Triple(SleepTimerSelection.Minutes(45), "45 min", "45"),
        Triple(SleepTimerSelection.Minutes(60), "60 min", "60"),
        Triple(SleepTimerSelection.EndOfVideo, "End of current video", "end")
    )

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF101524).copy(alpha = 0.92f))
                .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(24.dp))
                .padding(horizontal = 18.dp, vertical = 16.dp)
                .testTag("sleep_timer_dialog"),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Timer,
                        contentDescription = null,
                        tint = PlayerSeekPeriwinkle,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Sleep Timer",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close Sleep Timer",
                        tint = Color.White.copy(alpha = 0.78f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            options.forEach { (choice, label, tagSuffix) ->
                val selected = activeSelection == choice
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (selected) PlayerSeekPeriwinkle.copy(alpha = 0.26f)
                            else Color.White.copy(alpha = 0.08f)
                        )
                        .border(
                            width = 1.dp,
                            color = if (selected) PlayerSeekPeriwinkle else Color.White.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(999.dp)
                        )
                        .clickable {
                            onSelectOption(choice)
                            onDismiss()
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag("sleep_timer_option_$tagSuffix")
                ) {
                    Text(
                        text = label,
                        color = if (selected) PlayerSeekPeriwinkle else Color.White,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    if (selected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Selected",
                            tint = PlayerSeekPeriwinkle,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerVideoInfoDialog(
    video: VideoItem,
    decoderMode: DecoderPriority,
    detectedResolution: String,
    audioSummary: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1B1C24),
        titleContentColor = Color.White,
        textContentColor = Color.White.copy(alpha = 0.85f),
        title = { Text("Stream Information", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("File: ${video.displayName}", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text("Path: /storage/emulated/0/${video.relativePath}")
                Text("Decoder: ${decoderMode.label} (${video.videoCodec})")
                Text("Video Stream: $detectedResolution (${video.resolutionFramerateLabel})")
                Text("Audio Stream: $audioSummary")
                Text("Duration: ${video.formattedDuration}")
                Text("Size: ${video.formattedSize} (${video.mimeType})")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = PlayerSeekPeriwinkle)
            }
        }
    )
}
