package com.istiak.auraplayer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.istiak.auraplayer.ui.theme.DangerRed
import com.istiak.auraplayer.ui.theme.PlayerDrawerBg
import com.istiak.auraplayer.ui.theme.PlayerHudGlassBg
import com.istiak.auraplayer.ui.theme.PlayerHudGlassBorder
import com.istiak.auraplayer.ui.theme.PlayerSeekPeriwinkle

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

@Composable
fun RightSidePlayerDrawer(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (visible) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = slideInHorizontally(initialOffsetX = { it }),
                exit = slideOutHorizontally(targetOffsetX = { it }),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Box(
                    modifier = Modifier
                        .width(304.dp)
                        .fillMaxHeight()
                        .background(PlayerDrawerBg)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                        .padding(16.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun AudioTrackDrawerContent(
    tracks: List<AudioTrackUiModel>,
    isAudioDisabled: Boolean,
    useSwAudioDecoder: Boolean,
    audioSyncOffsetMs: Int,
    onSelectTrack: (AudioTrackUiModel) -> Unit,
    onSelectDefaultAudio: () -> Unit = {},
    onDisableAudio: () -> Unit,
    onToggleSwAudioDecoder: (Boolean) -> Unit,
    onAdjustAudioSyncMs: (Int) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag("audio_track_drawer")
    ) {
        DrawerHeader(title = "Audio Track", onClose = onClose)
        Spacer(modifier = Modifier.height(10.dp))

        if (tracks.isEmpty()) {
            val defaultSelected = !isAudioDisabled
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (defaultSelected) PlayerSeekPeriwinkle.copy(alpha = 0.2f) else Color.Transparent)
                    .clickable { onSelectDefaultAudio() }
                    .padding(vertical = 10.dp, horizontal = 8.dp)
                    .testTag("drawer_audio_default")
            ) {
                RadioButton(
                    selected = defaultSelected,
                    onClick = onSelectDefaultAudio,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = PlayerSeekPeriwinkle,
                        unselectedColor = Color.White.copy(alpha = 0.6f)
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Default Stream Audio",
                        color = if (defaultSelected) PlayerSeekPeriwinkle else Color.White,
                        fontSize = 14.sp,
                        fontWeight = if (defaultSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                    Text(
                        text = "Primary container audio track",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 11.sp
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
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) PlayerSeekPeriwinkle.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { onSelectTrack(track) }
                        .padding(vertical = 10.dp, horizontal = 8.dp)
                ) {
                    RadioButton(
                        selected = selected,
                        onClick = { onSelectTrack(track) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = PlayerSeekPeriwinkle,
                            unselectedColor = Color.White.copy(alpha = 0.6f)
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.label,
                            color = if (selected) PlayerSeekPeriwinkle else Color.White,
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (track.detail.isNotEmpty()) {
                            Text(
                                text = track.detail,
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (isAudioDisabled) PlayerSeekPeriwinkle.copy(alpha = 0.2f) else Color.Transparent)
                .clickable { onDisableAudio() }
                .padding(vertical = 8.dp, horizontal = 8.dp)
                .testTag("drawer_audio_disable")
        ) {
            RadioButton(
                selected = isAudioDisabled,
                onClick = onDisableAudio,
                colors = RadioButtonDefaults.colors(
                    selectedColor = PlayerSeekPeriwinkle,
                    unselectedColor = Color.White.copy(alpha = 0.6f)
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Disable",
                color = if (isAudioDisabled) PlayerSeekPeriwinkle else Color.White,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleSwAudioDecoder(!useSwAudioDecoder) }
                .padding(vertical = 4.dp)
                .testTag("drawer_sw_audio_checkbox")
        ) {
            Checkbox(
                checked = useSwAudioDecoder,
                onCheckedChange = onToggleSwAudioDecoder,
                colors = CheckboxDefaults.colors(
                    checkedColor = PlayerSeekPeriwinkle,
                    uncheckedColor = Color.White.copy(alpha = 0.7f),
                    checkmarkColor = Color.Black
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = "Use SW audio decoder",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "FFmpeg software decoder for EAC3 / AC3 / DTS / TrueHD",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Audio Synchronization",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        SyncStepControlRow(
            valueText = "${if (audioSyncOffsetMs > 0) "+" else ""}$audioSyncOffsetMs ms",
            onDecrement = { onAdjustAudioSyncMs(audioSyncOffsetMs - 50) },
            onIncrement = { onAdjustAudioSyncMs(audioSyncOffsetMs + 50) },
            onReset = { onAdjustAudioSyncMs(0) }
        )
    }
}

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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag("subtitle_drawer")
    ) {
        DrawerHeader(title = "Subtitle", onClose = onClose)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(PlayerSeekPeriwinkle.copy(alpha = 0.18f))
                .clickable(onClick = onOpenExternalSubtitle)
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .testTag("drawer_open_subtitle_file")
        ) {
            Icon(
                imageVector = Icons.Filled.FolderOpen,
                contentDescription = null,
                tint = PlayerSeekPeriwinkle,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Open External (.srt / .ass / .vtt)",
                color = PlayerSeekPeriwinkle,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF2563EB).copy(alpha = 0.24f))
                .border(1.dp, PlayerSeekPeriwinkle.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                .clickable(onClick = onSearchOnlineSubtitles)
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .testTag("drawer_search_online_subtitle")
        ) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = null,
                tint = PlayerSeekPeriwinkle,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Search Online (.srt)",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        tracks.forEach { track ->
            val selected = areSubtitlesEnabled && track.isSelected
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) PlayerSeekPeriwinkle.copy(alpha = 0.2f) else Color.Transparent)
                    .clickable { onSelectTrack(track) }
                    .padding(vertical = 8.dp, horizontal = 6.dp)
            ) {
                RadioButton(
                    selected = selected,
                    onClick = { onSelectTrack(track) },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = PlayerSeekPeriwinkle,
                        unselectedColor = Color.White.copy(alpha = 0.6f)
                    )
                )
                Text(
                    text = if (track.isExternal) "${track.label} [External]" else track.label,
                    color = if (selected) PlayerSeekPeriwinkle else Color.White,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (!areSubtitlesEnabled) PlayerSeekPeriwinkle.copy(alpha = 0.2f) else Color.Transparent)
                .clickable(onClick = onDisableSubtitles)
                .padding(vertical = 6.dp, horizontal = 6.dp)
                .testTag("drawer_subtitle_disable")
        ) {
            RadioButton(
                selected = !areSubtitlesEnabled,
                onClick = onDisableSubtitles,
                colors = RadioButtonDefaults.colors(
                    selectedColor = PlayerSeekPeriwinkle,
                    unselectedColor = Color.White.copy(alpha = 0.6f)
                )
            )
            Text(
                text = "Disable Subtitles",
                color = if (!areSubtitlesEnabled) PlayerSeekPeriwinkle else Color.White,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
        Spacer(modifier = Modifier.height(10.dp))

        Text("Synchronization", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        val secVal = subtitleSyncOffsetMs / 1000.0
        SyncStepControlRow(
            valueText = String.format(java.util.Locale.US, "%+.1fs", secVal),
            onDecrement = { onAdjustSubtitleSyncMs(subtitleSyncOffsetMs - 500L) },
            onIncrement = { onAdjustSubtitleSyncMs(subtitleSyncOffsetMs + 500L) },
            onReset = { onAdjustSubtitleSyncMs(0L) }
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text("Subtitle Speed", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        SyncStepControlRow(
            valueText = String.format(java.util.Locale.US, "%.2fx", subtitleSpeed),
            onDecrement = { onAdjustSubtitleSpeed((subtitleSpeed - 0.05f).coerceAtLeast(0.5f)) },
            onIncrement = { onAdjustSubtitleSpeed((subtitleSpeed + 0.05f).coerceAtMost(2.0f)) },
            onReset = { onAdjustSubtitleSpeed(1.0f) }
        )

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
        Spacer(modifier = Modifier.height(10.dp))

        Text("Customization", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Font Size (${styleConfig.fontSizeSp.toInt()}sp)",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 12.sp
        )
        Slider(
            value = styleConfig.fontSizeSp,
            onValueChange = { onUpdateStyleConfig(styleConfig.copy(fontSizeSp = it)) },
            valueRange = 12f..32f,
            colors = SliderDefaults.colors(
                thumbColor = PlayerSeekPeriwinkle,
                activeTrackColor = PlayerSeekPeriwinkle
            )
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Bold Text", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
            Switch(
                checked = styleConfig.isBold,
                onCheckedChange = { onUpdateStyleConfig(styleConfig.copy(isBold = it)) },
                colors = SwitchDefaults.colors(checkedThumbColor = PlayerSeekPeriwinkle)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Shadow Background", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
            Switch(
                checked = styleConfig.hasBackground,
                onCheckedChange = { onUpdateStyleConfig(styleConfig.copy(hasBackground = it)) },
                colors = SwitchDefaults.colors(checkedThumbColor = PlayerSeekPeriwinkle)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text("Text Color", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            color = if (isSelected) PlayerSeekPeriwinkle else Color.White.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                        .clickable { onUpdateStyleConfig(styleConfig.copy(textColorArgb = colorArgb)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Bottom Margin (${(styleConfig.bottomMarginFraction * 100).toInt()}%)",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 12.sp
        )
        Slider(
            value = styleConfig.bottomMarginFraction,
            onValueChange = { onUpdateStyleConfig(styleConfig.copy(bottomMarginFraction = it)) },
            valueRange = 0.02f..0.28f,
            colors = SliderDefaults.colors(
                thumbColor = PlayerSeekPeriwinkle,
                activeTrackColor = PlayerSeekPeriwinkle
            )
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
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
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

@Composable
private fun SyncStepControlRow(
    valueText: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        IconButton(onClick = onDecrement, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.Remove, contentDescription = "Decrease", tint = Color.White)
        }
        Text(
            text = valueText,
            color = PlayerSeekPeriwinkle,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onIncrement, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Add, contentDescription = "Increase", tint = Color.White)
            }
            IconButton(onClick = onReset, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = "Reset",
                    tint = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun FloatingPlaybackSpeedBar(
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(PlayerDrawerBg)
            .border(1.dp, PlayerHudGlassBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("floating_speed_bar")
    ) {
        IconButton(
            onClick = { onSpeedChange((speed - 0.1f).coerceAtLeast(0.25f)) },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Filled.Remove, contentDescription = "Slower", tint = Color.White)
        }

        Text(
            text = String.format(java.util.Locale.US, "%.2fx", speed),
            color = PlayerSeekPeriwinkle,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(48.dp),
            textAlign = TextAlign.Center
        )

        Slider(
            value = speed,
            onValueChange = {
                val rounded = (it * 20).toInt() / 20f
                onSpeedChange(rounded.coerceIn(0.25f, 4.0f))
            },
            valueRange = 0.25f..4.0f,
            colors = SliderDefaults.colors(
                thumbColor = PlayerSeekPeriwinkle,
                activeTrackColor = PlayerSeekPeriwinkle,
                inactiveTrackColor = Color.White.copy(alpha = 0.25f)
            ),
            modifier = Modifier.weight(1f)
        )

        IconButton(
            onClick = { onSpeedChange((speed + 0.1f).coerceAtMost(4.0f)) },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Faster", tint = Color.White)
        }

        IconButton(
            onClick = { onSpeedChange(1.0f) },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = "Reset 1.0x", tint = Color.White.copy(alpha = 0.85f))
        }

        IconButton(
            onClick = onClose,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Close Speed Bar", tint = Color.White.copy(alpha = 0.85f))
        }
    }
}

@Composable
fun DecoderSelectorDialog(
    activeDecoder: DecoderPriority,
    onSelectDecoder: (DecoderPriority) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1B1C24),
        titleContentColor = Color.White,
        textContentColor = Color.White.copy(alpha = 0.85f),
        title = { Text("Decoder Mode", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    DecoderPriority.HW to "Hardware MediaCodec video & audio",
                    DecoderPriority.HW_PLUS to "Hardware video + FFmpeg SW audio (EAC3 / AC3 / DTS / TrueHD)",
                    DecoderPriority.SW to "Prefer FFmpeg / software decoding for maximum compatibility"
                ).forEach { (mode, desc) ->
                    val selected = activeDecoder == mode
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) PlayerSeekPeriwinkle.copy(alpha = 0.22f) else Color.Transparent)
                            .clickable {
                                onSelectDecoder(mode)
                                onDismiss()
                            }
                            .padding(horizontal = 10.dp, vertical = 10.dp)
                            .testTag("decoder_option_${mode.name.lowercase()}")
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mode.label,
                                color = if (selected) PlayerSeekPeriwinkle else Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = desc,
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 11.sp
                            )
                        }
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
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = PlayerSeekPeriwinkle)
            }
        }
    )
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
