package com.istiak.auraplayer

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.istiak.auraplayer.ui.theme.CoralBadgeDark
import com.istiak.auraplayer.ui.theme.FolderBadgeCoral
import com.istiak.auraplayer.ui.theme.LocalExtendedPalette

private val AuraBlueTitleLight = Color(0xFF2563EB)
private val AuraBlueTitleDark = Color(0xFF60A5FA)
private val AuraBlueProgressStart = Color(0xFF2563EB)
private val AuraBlueProgressEnd = Color(0xFF3B82F6)
private val ThumbnailCornerShape = RoundedCornerShape(12.dp)

@Composable
fun VideoItemCard(
    video: VideoItem,
    settings: AppSettingsState,
    imageLoader: ImageLoader,
    playbackProgress: PlaybackProgressSnapshot = PlaybackProgressSnapshot(),
    isSelected: Boolean = false,
    onPlay: () -> Unit,
    onLongPress: () -> Unit = {},
    onOpenActionSheet: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    DirectoryVideoRow(
        video = video,
        settings = settings,
        imageLoader = imageLoader,
        playbackProgress = playbackProgress,
        isSelected = isSelected,
        onPlay = onPlay,
        onLongPress = onLongPress,
        onOpenActionSheet = onOpenActionSheet,
        modifier = modifier
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun DirectoryVideoRow(
    video: VideoItem,
    settings: AppSettingsState,
    imageLoader: ImageLoader,
    playbackProgress: PlaybackProgressSnapshot = PlaybackProgressSnapshot(),
    isSelected: Boolean = false,
    onPlay: () -> Unit,
    onLongPress: () -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onOpenActionSheet: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val palette = LocalExtendedPalette.current
    val primaryAccent = MaterialTheme.colorScheme.primary
    val rowBg = if (isSelected) {
        if (palette.isDark) palette.periwinkleBadgeBg.copy(alpha = 0.65f)
        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
    } else Color.Transparent

    val legacyPosMs = settings.playbackPositionsMs[video.id] ?: 0L
    val savedPosMs = playbackProgress.savedPositionMsFor(video, legacyPosMs)
    val watchProgressFraction = playbackProgress.watchProgressFractionFor(video, legacyPosMs)
    val isLastPlayed = playbackProgress.isLastPlayed(video) ||
        (!settings.lastPlayedVideoUri.isNullOrBlank() && settings.lastPlayedVideoUri == video.uri.toString())
    val hasStarted = playbackProgress.hasStarted(video) || savedPosMs > 0L
    val isUnwatched = !hasStarted && video.isConsideredNew(
        watchedVideoIds = settings.watchedVideoIds,
        seenBaselineEpochSec = settings.seenBaselineEpochSec,
        showNewLabel = settings.showNewVideoLabel,
        newLabelDaysThreshold = settings.newVideoLabelDaysThreshold
    )
    val titleColor = if (isLastPlayed) primaryAccent else palette.primaryText
    val showProgressOverlay = DisplayField.PROGRESS_BAR in settings.enabledFields &&
        watchProgressFraction > 0.01f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(rowBg)
            .combinedClickable(
                onClick = onPlay,
                onLongClick = {
                    triggerLongPressHaptic(context, hapticFeedback)
                    onLongPress()
                }
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("video_row_${video.id}")
    ) {
        if (DisplayField.THUMBNAILS in settings.enabledFields) {
            Box(
                modifier = Modifier
                    .size(width = 128.dp, height = 74.dp)
                    .clip(ThumbnailCornerShape)
                    .background(
                        if (settings.showVideoThumbnails) palette.neutralChipBg
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (palette.isDark) 0.35f else 0.55f)
                    )
                    .then(
                        if (isLastPlayed) {
                            Modifier.border(
                                width = 1.25.dp,
                                color = primaryAccent.copy(alpha = 0.68f),
                                shape = ThumbnailCornerShape
                            )
                        } else {
                            Modifier
                        }
                    )
            ) {
                if (settings.showVideoThumbnails) {
                    val thumbnailKey = remember(
                        video.id,
                        video.uri,
                        video.filePath,
                        video.vaultFilePath,
                        video.durationMs,
                        video.dateModifiedSec,
                        video.isVaultItem,
                        video.width,
                        video.height,
                        video.rotationDegrees
                    ) {
                        VideoThumbnailKey(
                            uri = video.uri,
                            filePath = video.filePath ?: video.vaultFilePath,
                            durationMs = video.durationMs,
                            dateModifiedSec = video.dateModifiedSec,
                            isVaultItem = video.isVaultItem,
                            width = video.width,
                            height = video.height,
                            orientationDegrees = video.rotationDegrees
                        )
                    }
                    val imageRequest = remember(context, thumbnailKey) {
                        ImageRequest.Builder(context)
                            .data(thumbnailKey)
                            .size(256, 148)
                            .memoryCacheKey(thumbnailKey.stableDiskKey)
                            .diskCacheKey(thumbnailKey.stableDiskKey)
                            .crossfade(false)
                            .build()
                    }
                    AsyncImage(
                        model = imageRequest,
                        imageLoader = imageLoader,
                        contentDescription = video.displayName,
                        contentScale = ContentScale.Crop,
                        filterQuality = FilterQuality.Medium,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("video_thumbnail_placeholder_${video.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Movie,
                            contentDescription = video.displayName,
                            tint = primaryAccent,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                if (isLastPlayed && !isSelected) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(primaryAccent.copy(alpha = 0.10f))
                    )
                }

                if (isUnwatched) {
                    CoralNewBadge(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(5.dp)
                    )
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(primaryAccent.copy(alpha = 0.30f))
                    )
                    SelectionCheckmarkBadge(
                        size = 32.dp,
                        iconSize = 19.dp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                if (DisplayField.DURATION in settings.enabledFields && settings.displayLengthOverThumbnail) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 6.dp, end = 6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.76f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = video.formattedDuration,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (showProgressOverlay) {
                    ThumbnailProgressBarOverlay(
                        progressFraction = watchProgressFraction,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .testTag("video_progress_bar_${video.id}")
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            val shownName = if (DisplayField.EXTENSION in settings.enabledFields) {
                video.displayName
            } else {
                video.title
            }
            Text(
                text = shownName,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = if (isLastPlayed) FontWeight.SemiBold else FontWeight.Medium,
                color = titleColor,
                maxLines = if (settings.showFullNames) Int.MAX_VALUE else 2,
                overflow = if (settings.showFullNames) TextOverflow.Clip else TextOverflow.Ellipsis,
                modifier = Modifier.testTag(
                    if (isLastPlayed) "last_played_video_title_${video.id}" else "video_title_${video.id}"
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (DisplayField.SUBTITLE in settings.enabledFields && video.hasSubtitles) {
                    SrtSubtitleBadge(text = "SRT")
                }
                if (DisplayField.DURATION in settings.enabledFields && !settings.displayLengthOverThumbnail) {
                    MetadataChip(text = video.formattedDuration)
                }
                if (DisplayField.PLAYED_TIME in settings.enabledFields && savedPosMs > 0L) {
                    MetadataChip(text = "Played ${formatDurationMillis(savedPosMs)}")
                }
                if (DisplayField.RESOLUTION in settings.enabledFields && DisplayField.FRAMERATE in settings.enabledFields) {
                    MetadataChip(text = video.resolutionFramerateLabel)
                } else if (DisplayField.RESOLUTION in settings.enabledFields) {
                    MetadataChip(text = video.verticalPixelsLabel)
                } else if (DisplayField.FRAMERATE in settings.enabledFields) {
                    MetadataChip(text = "${video.formattedFramerate}fps")
                }
                if (DisplayField.SIZE in settings.enabledFields) {
                    MetadataChip(text = video.formattedSize)
                }
                if (DisplayField.EXTENSION in settings.enabledFields) {
                    MetadataChip(text = video.extensionLabel)
                }
                if (DisplayField.DATE in settings.enabledFields) {
                    MetadataChip(text = video.formattedDate)
                }
                if (DisplayField.CODEC_SUPPORT in settings.enabledFields) {
                    MetadataChip(text = video.videoCodec, isAccent = true)
                }
                if (DisplayField.PATH in settings.enabledFields) {
                    MetadataChip(text = video.relativePath)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun DirectoryVideoGridCard(
    video: VideoItem,
    settings: AppSettingsState,
    imageLoader: ImageLoader,
    playbackProgress: PlaybackProgressSnapshot = PlaybackProgressSnapshot(),
    isSelected: Boolean = false,
    onPlay: () -> Unit,
    onLongPress: () -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onOpenMenu: () -> Unit = {}
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val palette = LocalExtendedPalette.current
    val primaryAccent = MaterialTheme.colorScheme.primary
    val cardBg = if (isSelected) {
        if (palette.isDark) palette.periwinkleBadgeBg.copy(alpha = 0.65f)
        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
    } else Color.Transparent

    val legacyPosMs = settings.playbackPositionsMs[video.id] ?: 0L
    val savedPosMs = playbackProgress.savedPositionMsFor(video, legacyPosMs)
    val watchProgressFraction = playbackProgress.watchProgressFractionFor(video, legacyPosMs)
    val isLastPlayed = playbackProgress.isLastPlayed(video) ||
        (!settings.lastPlayedVideoUri.isNullOrBlank() && settings.lastPlayedVideoUri == video.uri.toString())
    val hasStarted = playbackProgress.hasStarted(video) || savedPosMs > 0L
    val isUnwatched = !hasStarted && video.isConsideredNew(
        watchedVideoIds = settings.watchedVideoIds,
        seenBaselineEpochSec = settings.seenBaselineEpochSec,
        showNewLabel = settings.showNewVideoLabel,
        newLabelDaysThreshold = settings.newVideoLabelDaysThreshold
    )
    val titleColor = if (isLastPlayed) primaryAccent else palette.primaryText
    val showProgressOverlay = DisplayField.PROGRESS_BAR in settings.enabledFields &&
        watchProgressFraction > 0.01f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .combinedClickable(
                onClick = onPlay,
                onLongClick = {
                    triggerLongPressHaptic(context, hapticFeedback)
                    onLongPress()
                }
            )
            .padding(4.dp)
            .testTag("video_grid_card_${video.id}")
    ) {
        if (DisplayField.THUMBNAILS in settings.enabledFields) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
                    .clip(ThumbnailCornerShape)
                    .background(
                        if (settings.showVideoThumbnails) palette.neutralChipBg
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (palette.isDark) 0.35f else 0.55f)
                    )
                    .then(
                        if (isLastPlayed) {
                            Modifier.border(
                                width = 1.25.dp,
                                color = primaryAccent.copy(alpha = 0.68f),
                                shape = ThumbnailCornerShape
                            )
                        } else {
                            Modifier
                        }
                    )
            ) {
                if (settings.showVideoThumbnails) {
                    val thumbnailKey = remember(
                        video.id,
                        video.uri,
                        video.filePath,
                        video.vaultFilePath,
                        video.durationMs,
                        video.dateModifiedSec,
                        video.isVaultItem,
                        video.width,
                        video.height,
                        video.rotationDegrees
                    ) {
                        VideoThumbnailKey(
                            uri = video.uri,
                            filePath = video.filePath ?: video.vaultFilePath,
                            durationMs = video.durationMs,
                            dateModifiedSec = video.dateModifiedSec,
                            isVaultItem = video.isVaultItem,
                            width = video.width,
                            height = video.height,
                            orientationDegrees = video.rotationDegrees
                        )
                    }
                    val imageRequest = remember(context, thumbnailKey) {
                        ImageRequest.Builder(context)
                            .data(thumbnailKey)
                            .size(256, 148)
                            .memoryCacheKey(thumbnailKey.stableDiskKey)
                            .diskCacheKey(thumbnailKey.stableDiskKey)
                            .crossfade(false)
                            .build()
                    }
                    AsyncImage(
                        model = imageRequest,
                        imageLoader = imageLoader,
                        contentDescription = video.title,
                        contentScale = ContentScale.Crop,
                        filterQuality = FilterQuality.Medium,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("video_thumbnail_placeholder_${video.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Movie,
                            contentDescription = video.displayName,
                            tint = primaryAccent,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
                if (isLastPlayed && !isSelected) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(primaryAccent.copy(alpha = 0.10f))
                    )
                }
                if (isUnwatched) {
                    CoralNewBadge(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                    )
                }
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(primaryAccent.copy(alpha = 0.30f))
                    )
                    SelectionCheckmarkBadge(
                        size = 34.dp,
                        iconSize = 20.dp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                if (DisplayField.DURATION in settings.enabledFields && settings.displayLengthOverThumbnail) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 6.dp, end = 6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.76f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = video.formattedDuration,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                if (showProgressOverlay) {
                    ThumbnailProgressBarOverlay(
                        progressFraction = watchProgressFraction,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .testTag("video_grid_progress_bar_${video.id}")
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
        val shownGridName = if (DisplayField.EXTENSION in settings.enabledFields) {
            video.displayName
        } else {
            video.title
        }
        Text(
            text = shownGridName,
            color = titleColor,
            fontSize = 13.sp,
            fontWeight = if (isLastPlayed) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = if (settings.showFullNames) Int.MAX_VALUE else 2,
            overflow = if (settings.showFullNames) TextOverflow.Clip else TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(
                    if (isLastPlayed) "last_played_video_title_${video.id}" else "video_title_${video.id}"
                )
        )
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (DisplayField.SUBTITLE in settings.enabledFields && video.hasSubtitles) {
                SrtSubtitleBadge("SRT")
            }
            MetadataChip(text = video.verticalPixelsLabel)
            MetadataChip(text = video.formattedSize)
        }
    }
}

/**
 * Sleek 3.dp high horizontal progress bar pinned flush along the bottom edge of a video thumbnail:
 * - Track Background: `Color.Black.copy(alpha = 0.45f)`
 * - Progress Fill: Aura Blue (`#2563EB` -> `#3B82F6`) proportional to `progressFraction`
 */
@Composable
fun ThumbnailProgressBarOverlay(
    progressFraction: Float,
    modifier: Modifier = Modifier
) {
    val clamped = progressFraction.coerceIn(0f, 1f)
    val primary = MaterialTheme.colorScheme.primary
    val fillBrush = remember(primary) {
        Brush.horizontalGradient(
            listOf(primary, primary.copy(alpha = 0.82f))
        )
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(Color.Black.copy(alpha = 0.45f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(clamped)
                .background(fillBrush)
        )
    }
}

@Composable
internal fun CoralNewBadge(modifier: Modifier = Modifier) {
    val coralBrush = remember {
        Brush.linearGradient(listOf(FolderBadgeCoral, CoralBadgeDark))
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(coralBrush)
            .padding(horizontal = 5.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "NEW",
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp
        )
    }
}

@Composable
internal fun SelectionCheckmarkBadge(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    iconSize: Dp = 15.dp
) {
    val primary = MaterialTheme.colorScheme.primary
    val badgeBrush = remember(primary) {
        Brush.linearGradient(
            listOf(primary, primary.copy(alpha = 0.88f))
        )
    }
    Box(
        modifier = modifier
            .size(size)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(badgeBrush)
            .border(1.5.dp, Color.White.copy(alpha = 0.88f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = "Selected",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Suppress("DEPRECATION")
internal fun triggerLongPressHaptic(context: Context, hapticFeedback: HapticFeedback) {
    runCatching {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    runCatching {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (vibrator?.hasVibrator() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(15L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrator.vibrate(15L)
            }
        }
    }
}
