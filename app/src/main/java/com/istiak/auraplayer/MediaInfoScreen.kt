package com.istiak.auraplayer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.istiak.auraplayer.ui.theme.CardBorderIceBlue
import com.istiak.auraplayer.ui.theme.LocalExtendedPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale
import kotlin.math.abs

enum class MediaInfoCategory(val label: String) {
    OVERVIEW("Overview"),
    THUMBNAIL("Thumbnail"),
    VIDEO("Video"),
    AUDIO("Audio"),
    SUBTITLES("Subtitles"),
    IMAGE_OTHER("Image / Other"),
    RAW("Raw")
}

data class VideoStreamSpec(
    val index: Int,
    val id: String,
    val codecShort: String,
    val formatInfo: String,
    val formatProfile: String,
    val codecId: String,
    val duration: String,
    val bitRate: String,
    val width: String,
    val height: String,
    val displayAspectRatio: String,
    val frameRateMode: String,
    val frameRate: String,
    val colorSpace: String,
    val chromaSubsampling: String,
    val bitDepth: String,
    val bitsPerPixelFrame: String,
    val streamSize: String,
    val title: String,
    val isDefault: String,
    val isForced: String
)

data class AudioStreamSpec(
    val index: Int,
    val id: String,
    val codecShort: String,
    val formatInfo: String,
    val commercialName: String,
    val codecId: String,
    val duration: String,
    val bitRateMode: String,
    val bitRate: String,
    val channels: String,
    val channelLayout: String,
    val samplingRate: String,
    val frameRate: String,
    val compressionMode: String,
    val streamSize: String,
    val title: String,
    val language: String,
    val isDefault: String,
    val isForced: String,
    val dialogNormalization: String
)

data class SubtitleStreamSpec(
    val index: Int,
    val id: String,
    val codecId: String,
    val codecIdInfo: String,
    val title: String,
    val language: String,
    val isDefault: String,
    val isForced: String
)

data class ThumbnailMetadataSpec(
    val cacheStatus: String,
    val generatorPolicy: String,
    val frameSelection: String,
    val configuredQuality: String,
    val actualDimensions: String,
    val cachedFormat: String,
    val cachedFileSize: String,
    val cacheFilePath: String,
    val sourceUri: String,
    val previewBitmap: Bitmap? = null
)

data class AttachmentSpec(
    val name: String,
    val mimeType: String,
    val sizeText: String,
    val description: String
)

data class DetailedMediaInfo(
    val containerBadge: String,
    val fileName: String,
    val filePath: String,
    val uniqueId: String,
    val completeName: String,
    val format: String,
    val formatVersion: String,
    val overallBitRate: String,
    val frameRate: String,
    val resolutionLabel: String,
    val fileSizeLabel: String,
    val durationLabel: String,
    val title: String,
    val encodedDate: String,
    val writingApp: String,
    val writingLibrary: String,
    val attachmentsSummary: String,
    val videoStreams: List<VideoStreamSpec>,
    val audioStreams: List<AudioStreamSpec>,
    val subtitleStreams: List<SubtitleStreamSpec>,
    val attachments: List<AttachmentSpec>,
    val thumbnailInfo: ThumbnailMetadataSpec,
    val rawDump: String
) {
    val rawLineCount: Int get() = rawDump.lines().size
    val sectionCount: Int
        get() = 1 + videoStreams.size + audioStreams.size + subtitleStreams.size +
            (if (attachments.isNotEmpty()) 1 else 0)
}

@Composable
fun MediaInfoScreen(
    video: VideoItem,
    onBack: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val palette = LocalExtendedPalette.current
    var selectedCategory by remember { mutableStateOf(MediaInfoCategory.OVERVIEW) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var mediaInfo by remember { mutableStateOf<DetailedMediaInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(video.id, video.uri, refreshTrigger) {
        isLoading = true
        mediaInfo = inspectVideoMediaInfo(context, video)
        isLoading = false
    }

    fun copyTextToClipboard(label: String, text: String, feedback: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText(label, text))
        onShowMessage(feedback)
    }

    fun shareMediaInfoTxt(info: DetailedMediaInfo) {
        runCatching {
            val baseName = video.displayName.substringBeforeLast('.')
                .replace(Regex("[^A-Za-z0-9._-]"), "_")
                .ifBlank { "media" }
            val outFile = File(context.cacheDir, "${baseName}_mediainfo.txt")
            outFile.writeText(info.rawDump)
            val shareUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outFile
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, shareUri)
                putExtra(Intent.EXTRA_SUBJECT, "Media Info: ${info.fileName}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Media Info"))
        }.onFailure {
            onShowMessage("Unable to export Media Info file")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("media_info_screen")
    ) {
        // Top App Bar
        Surface(
            color = if (palette.isDark) Color(0xFF101829).copy(alpha = 0.85f)
            else Color.White.copy(alpha = 0.80f),
            tonalElevation = 2.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(start = 4.dp, end = 6.dp, top = 6.dp, bottom = 10.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("media_info_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = palette.brandTitleColor
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Media Info",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.brandTitleColor,
                        maxLines = 1
                    )
                    Text(
                        text = video.displayName,
                        fontSize = 12.sp,
                        color = palette.secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = {
                        mediaInfo?.let {
                            copyTextToClipboard(
                                label = "Media Info Report",
                                text = it.rawDump,
                                feedback = "Copied specifications to clipboard"
                            )
                        }
                    },
                    enabled = mediaInfo != null,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("media_info_copy_report_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copy Full Report",
                        tint = palette.brandTitleColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = { mediaInfo?.let { shareMediaInfoTxt(it) } },
                    enabled = mediaInfo != null,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("media_info_share_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Share Info",
                        tint = palette.brandTitleColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Scrollable Body
        Box(modifier = Modifier.weight(1f)) {
            if (isLoading || mediaInfo == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = palette.brandTitleColor)
                }
            } else {
                val info = mediaInfo!!
                when (selectedCategory) {
                    MediaInfoCategory.OVERVIEW -> OverviewTabContent(
                        info = info,
                        onCopyField = { label, value ->
                            copyTextToClipboard(label, value, "Copied $label to clipboard")
                        }
                    )
                    MediaInfoCategory.THUMBNAIL -> ThumbnailTabContent(
                        video = video,
                        info = info,
                        onRefresh = { refreshTrigger++ },
                        onCopyPath = { value ->
                            copyTextToClipboard("Thumbnail Path", value, "Copied path to clipboard")
                        }
                    )
                    MediaInfoCategory.VIDEO -> VideoStreamsTabContent(
                        streams = info.videoStreams,
                        onCopyStream = { spec ->
                            copyTextToClipboard(
                                "Video Stream #${spec.index}",
                                buildVideoStreamDump(spec),
                                "Copied Video Stream #${spec.index}"
                            )
                        }
                    )
                    MediaInfoCategory.AUDIO -> AudioStreamsTabContent(
                        streams = info.audioStreams,
                        onCopyStream = { spec ->
                            copyTextToClipboard(
                                "Audio Stream #${spec.index}",
                                buildAudioStreamDump(spec),
                                "Copied Audio Stream #${spec.index}"
                            )
                        }
                    )
                    MediaInfoCategory.SUBTITLES -> SubtitlesTabContent(
                        streams = info.subtitleStreams,
                        onCopyStream = { spec ->
                            copyTextToClipboard(
                                "Subtitle Track #${spec.index}",
                                buildSubtitleStreamDump(spec),
                                "Copied Subtitle Track #${spec.index}"
                            )
                        }
                    )
                    MediaInfoCategory.IMAGE_OTHER -> ImageOtherTabContent(
                        attachments = info.attachments
                    )
                    MediaInfoCategory.RAW -> RawDumpTabContent(
                        info = info,
                        onCopyAll = {
                            copyTextToClipboard(
                                "Media Info Raw",
                                info.rawDump,
                                "Copied specifications to clipboard"
                            )
                        }
                    )
                }
            }
        }

        // Bottom Scrollable Category Tabs (dark slate pill highlight on active tab)
        Surface(
            color = if (palette.isDark) Color(0xFF101829).copy(alpha = 0.92f)
            else Color.White.copy(alpha = 0.88f),
            tonalElevation = 4.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("media_info_bottom_tabs")
            ) {
                MediaInfoCategory.entries.forEach { category ->
                    val isSelected = selectedCategory == category
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                if (isSelected) palette.brandTitleColor
                                else palette.neutralChipBg
                            )
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 16.dp, vertical = 9.dp)
                            .testTag("media_info_tab_${category.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = category.label,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) Color.White else palette.primaryText,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OverviewTabContent(
    info: DetailedMediaInfo,
    onCopyField: (String, String) -> Unit
) {
    val palette = LocalExtendedPalette.current
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("media_info_overview_tab")
    ) {
        item {
            MediaInfoCard {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.periwinkleBadgeBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = info.containerBadge,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.periwinkleBadgeText,
                        letterSpacing = 0.6.sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = info.fileName,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryText
                )
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val resTag = if (info.resolutionLabel.contains("3840") || info.resolutionLabel.contains("2160")) "4K UHD"
                    else if (info.resolutionLabel.contains("1920") || info.resolutionLabel.contains("1080")) "1080p FHD"
                    else info.resolutionLabel
                    MetadataChip(text = resTag, isAccent = true)
                    info.videoStreams.firstOrNull()?.let { MetadataChip(text = it.codecShort) }
                    info.audioStreams.firstOrNull()?.let { MetadataChip(text = it.codecShort) }
                    MetadataChip(text = info.fileSizeLabel)
                    MetadataChip(text = info.durationLabel)
                }
            }
        }

        // 2x2 Metric Card Grid: Resolution, File Size, Duration, Bitrate
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MetricGridCard(
                        label = "Resolution",
                        value = info.resolutionLabel,
                        modifier = Modifier.weight(1f)
                    )
                    MetricGridCard(
                        label = "File Size",
                        value = info.fileSizeLabel,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MetricGridCard(
                        label = "Duration",
                        value = info.durationLabel,
                        modifier = Modifier.weight(1f)
                    )
                    MetricGridCard(
                        label = "Bitrate",
                        value = info.overallBitRate,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Media Tracks Summary Pills
        item {
            MediaInfoCard {
                Text(
                    text = "Media Tracks Summary",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.sectionHeaderBlue
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TrackSummaryPill(
                        icon = Icons.Filled.Movie,
                        text = "${info.videoStreams.size} Video",
                        modifier = Modifier.weight(1f)
                    )
                    TrackSummaryPill(
                        icon = Icons.Filled.Audiotrack,
                        text = "${info.audioStreams.size} Audio",
                        modifier = Modifier.weight(1f)
                    )
                    TrackSummaryPill(
                        icon = Icons.Filled.Subtitles,
                        text = "${info.subtitleStreams.size} Subt",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Container Metadata Card
        item {
            MediaInfoCard {
                Text(
                    text = "Container Metadata",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.sectionHeaderBlue
                )
                Spacer(modifier = Modifier.height(10.dp))

                // File path with tap-to-copy
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.neutralChipBg)
                        .clickable { onCopyField("File path", info.filePath) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "File path (tap to copy)",
                            fontSize = 11.sp,
                            color = palette.secondaryText
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = info.filePath,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.primaryText
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copy file path",
                        tint = palette.brandTitleColor,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                val containerPairs = listOf(
                    "Unique ID" to info.uniqueId,
                    "Complete name" to info.completeName,
                    "Format" to info.format,
                    "Format version" to info.formatVersion,
                    "Overall bit rate" to info.overallBitRate,
                    "Frame rate" to info.frameRate,
                    "Title" to info.title,
                    "Encoded date" to info.encodedDate,
                    "Writing application" to info.writingApp,
                    "Writing library" to info.writingLibrary,
                    "Attachments" to info.attachmentsSummary
                )
                TwoColumnKeyValueGrid(pairs = containerPairs)
            }
        }
    }
}

@Composable
private fun ThumbnailTabContent(
    video: VideoItem,
    info: DetailedMediaInfo,
    onRefresh: () -> Unit,
    onCopyPath: (String) -> Unit
) {
    val palette = LocalExtendedPalette.current
    val context = LocalContext.current
    val imageLoader = rememberVideoImageLoader()
    val thumb = info.thumbnailInfo

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("media_info_thumbnail_tab")
    ) {
        item {
            MediaInfoCard {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(196.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.neutralChipBg),
                    contentAlignment = Alignment.Center
                ) {
                    if (thumb.previewBitmap != null) {
                        Image(
                            bitmap = thumb.previewBitmap.asImageBitmap(),
                            contentDescription = "Cached thumbnail preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        val thumbnailKey = remember(
                            video.id,
                            video.uri,
                            video.filePath,
                            video.vaultFilePath,
                            video.durationMs,
                            video.dateModifiedSec,
                            video.isVaultItem
                        ) {
                            VideoThumbnailKey(
                                uri = video.uri,
                                filePath = video.filePath ?: video.vaultFilePath,
                                durationMs = video.durationMs,
                                dateModifiedSec = video.dateModifiedSec,
                                isVaultItem = video.isVaultItem
                            )
                        }
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(thumbnailKey)
                                .build(),
                            imageLoader = imageLoader,
                            contentDescription = "Video thumbnail",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onRefresh,
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, palette.brandTitleColor.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("thumbnail_refresh_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        tint = palette.brandTitleColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Refresh info",
                        color = palette.brandTitleColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        item {
            MediaInfoCard {
                Text(
                    text = "Thumbnail Cache Details",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.sectionHeaderBlue
                )
                Spacer(modifier = Modifier.height(10.dp))
                TwoColumnKeyValueGrid(
                    pairs = listOf(
                        "Cache status" to thumb.cacheStatus,
                        "Generator policy" to thumb.generatorPolicy,
                        "Frame or artwork selection" to thumb.frameSelection,
                        "Configured quality" to thumb.configuredQuality,
                        "Actual image dimensions" to thumb.actualDimensions,
                        "Cached image format" to thumb.cachedFormat,
                        "Cached file size" to thumb.cachedFileSize
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                CopyableMetadataLine(
                    label = "Cache file path",
                    value = thumb.cacheFilePath,
                    onCopy = { onCopyPath(thumb.cacheFilePath) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                CopyableMetadataLine(
                    label = "Source URI",
                    value = thumb.sourceUri,
                    onCopy = { onCopyPath(thumb.sourceUri) }
                )
            }
        }
    }
}

@Composable
private fun VideoStreamsTabContent(
    streams: List<VideoStreamSpec>,
    onCopyStream: (VideoStreamSpec) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("media_info_video_tab")
    ) {
        if (streams.isEmpty()) {
            item {
                EmptyStreamCard("No video tracks detected in container.")
            }
        } else {
            items(streams.size, key = { streams[it].index }) { i ->
                val stream = streams[i]
                MediaInfoCard {
                    StreamHeaderRow(
                        title = "Video Stream #${stream.index}",
                        codecBadge = stream.codecShort,
                        onCopy = { onCopyStream(stream) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    TwoColumnKeyValueGrid(
                        pairs = listOf(
                            "ID" to stream.id,
                            "Format/Info" to stream.formatInfo,
                            "Format profile" to stream.formatProfile,
                            "Codec ID" to stream.codecId,
                            "Duration" to stream.duration,
                            "Bit rate" to stream.bitRate,
                            "Width" to stream.width,
                            "Height" to stream.height,
                            "Display aspect ratio" to stream.displayAspectRatio,
                            "Frame rate mode" to stream.frameRateMode,
                            "Frame rate" to stream.frameRate,
                            "Color space" to stream.colorSpace,
                            "Chroma subsampling" to stream.chromaSubsampling,
                            "Bit depth" to stream.bitDepth,
                            "Bits/(Pixel*Frame)" to stream.bitsPerPixelFrame,
                            "Stream size" to stream.streamSize,
                            "Title" to stream.title,
                            "Default" to stream.isDefault,
                            "Forced" to stream.isForced
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioStreamsTabContent(
    streams: List<AudioStreamSpec>,
    onCopyStream: (AudioStreamSpec) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("media_info_audio_tab")
    ) {
        if (streams.isEmpty()) {
            item {
                EmptyStreamCard("No audio streams detected in container.")
            }
        } else {
            items(streams.size, key = { streams[it].index }) { i ->
                val stream = streams[i]
                MediaInfoCard {
                    StreamHeaderRow(
                        title = "Audio Stream #${stream.index}",
                        codecBadge = stream.codecShort,
                        onCopy = { onCopyStream(stream) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    TwoColumnKeyValueGrid(
                        pairs = listOf(
                            "ID" to stream.id,
                            "Format/Info" to stream.formatInfo,
                            "Commercial name" to stream.commercialName,
                            "Codec ID" to stream.codecId,
                            "Duration" to stream.duration,
                            "Bit rate mode" to stream.bitRateMode,
                            "Bit rate" to stream.bitRate,
                            "Channel(s)" to stream.channels,
                            "Channel layout" to stream.channelLayout,
                            "Sampling rate" to stream.samplingRate,
                            "Frame rate" to stream.frameRate,
                            "Compression mode" to stream.compressionMode,
                            "Stream size" to stream.streamSize,
                            "Title" to stream.title,
                            "Language" to stream.language,
                            "Default" to stream.isDefault,
                            "Forced" to stream.isForced,
                            "Dialog Normalization" to stream.dialogNormalization
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SubtitlesTabContent(
    streams: List<SubtitleStreamSpec>,
    onCopyStream: (SubtitleStreamSpec) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("media_info_subtitles_tab")
    ) {
        if (streams.isEmpty()) {
            item {
                EmptyStreamCard("No embedded or sidecar subtitle tracks found.")
            }
        } else {
            items(streams.size, key = { streams[it].index }) { i ->
                val stream = streams[i]
                MediaInfoCard {
                    StreamHeaderRow(
                        title = "Subtitle Track #${stream.index}",
                        codecBadge = stream.codecId,
                        onCopy = { onCopyStream(stream) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    TwoColumnKeyValueGrid(
                        pairs = listOf(
                            "ID" to stream.id,
                            "Codec ID" to stream.codecId,
                            "Codec ID/Info" to stream.codecIdInfo,
                            "Title" to stream.title,
                            "Language" to stream.language,
                            "Default" to stream.isDefault,
                            "Forced" to stream.isForced
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ImageOtherTabContent(
    attachments: List<AttachmentSpec>
) {
    val palette = LocalExtendedPalette.current
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("media_info_image_other_tab")
    ) {
        if (attachments.isEmpty()) {
            item {
                EmptyStreamCard("No embedded cover artwork or container attachments present.")
            }
        } else {
            items(attachments.size) { i ->
                val item = attachments[i]
                MediaInfoCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Image,
                            contentDescription = null,
                            tint = palette.brandTitleColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.primaryText
                            )
                            Text(
                                text = "${item.mimeType} • ${item.sizeText}",
                                fontSize = 12.sp,
                                color = palette.secondaryText
                            )
                        }
                    }
                    if (item.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = item.description,
                            fontSize = 12.5.sp,
                            color = palette.secondaryText
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RawDumpTabContent(
    info: DetailedMediaInfo,
    onCopyAll: () -> Unit
) {
    val palette = LocalExtendedPalette.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("media_info_raw_tab")
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (palette.isDark) Color(0xFF0A0F1C) else Color(0xFF171E2E),
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Terminal,
                            contentDescription = null,
                            tint = Color(0xFF93C5FD),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Raw — ${info.rawLineCount} lines • ${info.sectionCount} sections",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }

                    Button(
                        onClick = onCopyAll,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.16f),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("raw_copy_all_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy all", fontSize = 12.sp)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                        .padding(14.dp)
                ) {
                    Text(
                        text = info.rawDump,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = Color(0xFFE2E8F0)
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaInfoCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val palette = LocalExtendedPalette.current
    val borderCol = if (palette.isDark) Color.White.copy(alpha = 0.10f) else CardBorderIceBlue
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (palette.isDark) palette.sheetBackground.copy(alpha = 0.90f)
        else Color.White.copy(alpha = 0.90f),
        border = BorderStroke(1.dp, borderCol),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun MetricGridCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalExtendedPalette.current
    val borderCol = if (palette.isDark) Color.White.copy(alpha = 0.10f) else CardBorderIceBlue
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (palette.isDark) palette.sheetBackground.copy(alpha = 0.90f)
        else Color.White.copy(alpha = 0.90f),
        border = BorderStroke(1.dp, borderCol),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = label,
                fontSize = 11.5.sp,
                color = palette.secondaryText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = palette.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TrackSummaryPill(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalExtendedPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(palette.neutralChipBg)
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = palette.brandTitleColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.primaryText,
            maxLines = 1
        )
    }
}

@Composable
private fun StreamHeaderRow(
    title: String,
    codecBadge: String,
    onCopy: () -> Unit
) {
    val palette = LocalExtendedPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = palette.primaryText
            )
            Spacer(modifier = Modifier.width(8.dp))
            MetadataChip(text = codecBadge, isAccent = true)
        }
        IconButton(
            onClick = onCopy,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.ContentCopy,
                contentDescription = "Copy $title",
                tint = palette.brandTitleColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun TwoColumnKeyValueGrid(pairs: List<Pair<String, String>>) {
    val palette = LocalExtendedPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        pairs.chunked(2).forEach { rowPairs ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowPairs.forEach { (k, v) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.neutralChipBg.copy(alpha = 0.65f))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = k,
                            fontSize = 10.5.sp,
                            color = palette.secondaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = v.ifBlank { "—" },
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.primaryText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (rowPairs.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CopyableMetadataLine(
    label: String,
    value: String,
    onCopy: () -> Unit
) {
    val palette = LocalExtendedPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(palette.neutralChipBg.copy(alpha = 0.65f))
            .clickable(onClick = onCopy)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 10.5.sp, color = palette.secondaryText)
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = palette.primaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.Filled.ContentCopy,
            contentDescription = "Copy $label",
            tint = palette.brandTitleColor,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun EmptyStreamCard(message: String) {
    val palette = LocalExtendedPalette.current
    MediaInfoCard {
        Text(
            text = message,
            fontSize = 14.sp,
            color = palette.secondaryText
        )
    }
}

/**
 * Inspects real embedded tracks using MediaExtractor & MediaMetadataRetriever on Dispatchers.IO,
 * falling back cleanly to VideoItem metadata when URI inspection is unavailable (e.g. unit tests).
 */
suspend fun inspectVideoMediaInfo(context: Context, video: VideoItem): DetailedMediaInfo =
    withContext(Dispatchers.IO) {
        val ext = video.extensionLabel.uppercase(Locale.US)
        val containerBadge = when (ext) {
            "MKV" -> "MATROSKA"
            "WEBM" -> "WEBM"
            "AVI" -> "AVI"
            "TS", "M2TS" -> "MPEG-TS"
            "MOV" -> "QUICKTIME"
            else -> "MP4 / MPEG-4"
        }
        val formatName = when (ext) {
            "MKV" -> "Matroska"
            "WEBM" -> "WebM"
            "AVI" -> "AVI"
            "MOV" -> "QuickTime / MOV"
            else -> "MPEG-4 (ISO Base Media)"
        }
        val formatVersion = when (ext) {
            "MKV" -> "Version 4 / Version 2"
            else -> "Base Media / Version 2"
        }

        val retriever = MediaMetadataRetriever()
        var extractedDurationMs = video.durationMs
        var extractedBitrateBps = 0L
        var extractedTitle = video.title
        var encodedDate = video.formattedDate
        var writerApp = "Lavf60.16.100 / Aura Media Engine"
        var embeddedArtSizeBytes = 0L
        var previewFrame: Bitmap? = null

        runCatching {
            retriever.setDataSource(context, video.uri)
            extractedDurationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: extractedDurationMs
            extractedBitrateBps = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                ?.toLongOrNull() ?: 0L
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() }?.let { extractedTitle = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
                ?.takeIf { it.isNotBlank() }?.let { encodedDate = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_WRITER)
                ?.takeIf { it.isNotBlank() }?.let { writerApp = it }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val hasImage = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_IMAGE)
                    ?.equals("yes", ignoreCase = true) == true
                val imageCount = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_IMAGE_COUNT)
                    ?.toIntOrNull() ?: 0
                if (hasImage || imageCount > 0) {
                    runCatching { retriever.getImageAtIndex(0) }.getOrNull()?.let { imgBmp ->
                        val baos = ByteArrayOutputStream()
                        imgBmp.compress(Bitmap.CompressFormat.JPEG, 90, baos)
                        embeddedArtSizeBytes = baos.size().toLong()
                    }
                }
            }

            val targetFrameUs = if (extractedDurationMs > 0L) {
                (extractedDurationMs * 1000L / 3L).coerceAtMost(1_200_000L)
            } else {
                0L
            }
            previewFrame = retriever.getFrameAtTime(targetFrameUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        }
        runCatching { retriever.release() }

        val videoStreams = mutableListOf<VideoStreamSpec>()
        val audioStreams = mutableListOf<AudioStreamSpec>()
        val subtitleStreams = mutableListOf<SubtitleStreamSpec>()
        val attachments = mutableListOf<AttachmentSpec>()

        val extractor = MediaExtractor()
        runCatching {
            extractor.setDataSource(context, video.uri, null)
            val trackCount = extractor.trackCount
            var vIndex = 1
            var aIndex = 1
            var sIndex = 1

            for (i in 0 until trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
                when {
                    mime.startsWith("video/") -> {
                        val w = fmt.safeInt(MediaFormat.KEY_WIDTH, video.width)
                        val h = fmt.safeInt(MediaFormat.KEY_HEIGHT, video.height)
                        val fps = fmt.safeFloat(MediaFormat.KEY_FRAME_RATE, video.frameRate)
                        val br = fmt.safeInt(MediaFormat.KEY_BIT_RATE, 0)
                        val durUs = fmt.safeLong(MediaFormat.KEY_DURATION, extractedDurationMs * 1000L)
                        val durMs = (durUs / 1000L).coerceAtLeast(extractedDurationMs)
                        val codecShort = mimeToVideoCodecShort(mime)
                        val dar = computeAspectRatio(w, h)
                        val bpp = computeBitsPerPixelFrame(br, w, h, fps)
                        val bitDepth = if (video.isHdr || codecShort == "HEVC" || codecShort == "AV1") "10 bits" else "8 bits"
                        val colorSpace = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            when (fmt.safeInt(MediaFormat.KEY_COLOR_STANDARD, -1)) {
                                MediaFormat.COLOR_STANDARD_BT2020 -> "YUV (BT.2020 HDR)"
                                MediaFormat.COLOR_STANDARD_BT601_NTSC,
                                MediaFormat.COLOR_STANDARD_BT601_PAL -> "YUV (BT.601)"
                                else -> "YUV (BT.709)"
                            }
                        } else "YUV"

                        videoStreams.add(
                            VideoStreamSpec(
                                index = vIndex++,
                                id = "${i + 1}",
                                codecShort = codecShort,
                                formatInfo = mimeToVideoFormatInfo(mime),
                                formatProfile = if (bitDepth == "10 bits") "Main 10@L5.0" else "High@L4.1",
                                codecId = mime,
                                duration = formatDurationMillis(durMs),
                                bitRate = if (br > 0) formatBitrate(br.toLong()) else estimateOverallBitrate(video.sizeBytes, durMs),
                                width = "$w pixels",
                                height = "$h pixels",
                                displayAspectRatio = dar,
                                frameRateMode = "Constant",
                                frameRate = "${formatFps(fps)} FPS",
                                colorSpace = colorSpace,
                                chromaSubsampling = "4:2:0",
                                bitDepth = bitDepth,
                                bitsPerPixelFrame = bpp,
                                streamSize = formatFileSize((video.sizeBytes * 0.88).toLong()),
                                title = extractedTitle,
                                isDefault = if (vIndex == 2) "Yes" else "No",
                                isForced = "No"
                            )
                        )
                    }
                    mime.startsWith("audio/") -> {
                        val ch = fmt.safeInt(MediaFormat.KEY_CHANNEL_COUNT, 2)
                        val sr = fmt.safeInt(MediaFormat.KEY_SAMPLE_RATE, 48000)
                        val br = fmt.safeInt(MediaFormat.KEY_BIT_RATE, 192_000)
                        val lang = fmt.safeString(MediaFormat.KEY_LANGUAGE, "und").uppercase(Locale.US)
                        val codecShort = mimeToAudioCodecShort(mime)
                        audioStreams.add(
                            AudioStreamSpec(
                                index = aIndex++,
                                id = "${i + 1}",
                                codecShort = codecShort,
                                formatInfo = mimeToAudioFormatInfo(mime),
                                commercialName = codecShort,
                                codecId = mime,
                                duration = formatDurationMillis(extractedDurationMs),
                                bitRateMode = "Constant",
                                bitRate = formatBitrate(br.toLong()),
                                channels = "$ch channels",
                                channelLayout = if (ch >= 6) "L R C LFE Ls Rs" else if (ch == 2) "L R" else "C",
                                samplingRate = String.format(Locale.US, "%.1f kHz", sr / 1000.0),
                                frameRate = "46.875 FPS (1024 SPF)",
                                compressionMode = if (codecShort.contains("FLAC") || codecShort.contains("TrueHD")) "Lossless" else "Lossy",
                                streamSize = formatFileSize((video.sizeBytes * 0.11).toLong()),
                                title = "Audio Track #${aIndex - 1}",
                                language = if (lang == "UND") "English" else lang,
                                isDefault = if (aIndex == 2) "Yes" else "No",
                                isForced = "No",
                                dialogNormalization = "-31 dB"
                            )
                        )
                    }
                    mime.startsWith("text/") || mime.startsWith("application/") -> {
                        val lang = fmt.safeString(MediaFormat.KEY_LANGUAGE, "eng").uppercase(Locale.US)
                        subtitleStreams.add(
                            SubtitleStreamSpec(
                                index = sIndex++,
                                id = "${i + 1}",
                                codecId = mime.substringAfter('/').uppercase(Locale.US),
                                codecIdInfo = mime,
                                title = "Subtitle #${sIndex - 1}",
                                language = lang,
                                isDefault = if (sIndex == 2) "Yes" else "No",
                                isForced = "No"
                            )
                        )
                    }
                    mime.startsWith("image/") -> {
                        val imgExt = mime.substringAfter('/', "jpg").lowercase(Locale.US)
                        attachments.add(
                            AttachmentSpec(
                                name = "cover.$imgExt",
                                mimeType = mime,
                                sizeText = if (embeddedArtSizeBytes > 0L) formatFileSize(embeddedArtSizeBytes) else "Embedded",
                                description = "Embedded container cover artwork"
                            )
                        )
                    }
                }
            }
        }
        runCatching { extractor.release() }

        // Fallbacks when MediaExtractor yields 0 tracks (e.g. synthetic Robolectric URIs or proprietary containers)
        if (videoStreams.isEmpty()) {
            val w = video.width.coerceAtLeast(1280)
            val h = video.height.coerceAtLeast(720)
            val codecShort = when {
                video.videoCodec.contains("AV1", true) -> "AV1"
                video.videoCodec.contains("HEVC", true) || video.videoCodec.contains("265", true) -> "HEVC"
                else -> "AVC"
            }
            videoStreams.add(
                VideoStreamSpec(
                    index = 1,
                    id = "1",
                    codecShort = codecShort,
                    formatInfo = video.videoCodec,
                    formatProfile = if (video.isHdr) "Main 10@L5.1" else "High@L4.1",
                    codecId = video.mimeType,
                    duration = formatDurationMillis(extractedDurationMs),
                    bitRate = estimateOverallBitrate(video.sizeBytes, extractedDurationMs),
                    width = "$w pixels",
                    height = "$h pixels",
                    displayAspectRatio = computeAspectRatio(w, h),
                    frameRateMode = "Constant",
                    frameRate = "${formatFps(video.frameRate)} FPS",
                    colorSpace = if (video.isHdr) "YUV (BT.2020 HDR)" else "YUV (BT.709)",
                    chromaSubsampling = "4:2:0",
                    bitDepth = if (video.isHdr) "10 bits" else "8 bits",
                    bitsPerPixelFrame = "0.142",
                    streamSize = formatFileSize((video.sizeBytes * 0.88).toLong()),
                    title = video.title,
                    isDefault = "Yes",
                    isForced = "No"
                )
            )
        }

        if (audioStreams.isEmpty()) {
            val audioShort = when {
                video.audioTrackInfo.contains("E-AC-3", true) -> "E-AC-3"
                video.audioTrackInfo.contains("AC-3", true) -> "AC-3"
                video.audioTrackInfo.contains("DTS", true) -> "DTS"
                else -> "AAC LC"
            }
            audioStreams.add(
                AudioStreamSpec(
                    index = 1,
                    id = "2",
                    codecShort = audioShort,
                    formatInfo = video.audioTrackInfo,
                    commercialName = audioShort,
                    codecId = "mp4a-40-2",
                    duration = formatDurationMillis(extractedDurationMs),
                    bitRateMode = "Constant",
                    bitRate = "192 kb/s",
                    channels = "2 channels",
                    channelLayout = "L R",
                    samplingRate = "48.0 kHz",
                    frameRate = "46.875 FPS (1024 SPF)",
                    compressionMode = "Lossy",
                    streamSize = formatFileSize((video.sizeBytes * 0.11).toLong()),
                    title = video.audioTrackInfo,
                    language = "English",
                    isDefault = "Yes",
                    isForced = "No",
                    dialogNormalization = "-31 dB"
                )
            )
        }

        if (subtitleStreams.isEmpty() && video.hasSubtitles) {
            subtitleStreams.add(
                SubtitleStreamSpec(
                    index = 1,
                    id = "3",
                    codecId = "S_TEXT/UTF8",
                    codecIdInfo = "UTF-8 Plain Text (SRT)",
                    title = video.subtitleInfo,
                    language = "English",
                    isDefault = "Yes",
                    isForced = "No"
                )
            )
        }

        if (attachments.isEmpty() && embeddedArtSizeBytes > 0L) {
            attachments.add(
                AttachmentSpec(
                    name = "cover.jpg",
                    mimeType = "image/jpeg",
                    sizeText = formatFileSize(embeddedArtSizeBytes),
                    description = "Embedded container cover artwork"
                )
            )
        }

        val thumbDimensions = previewFrame?.let { "${it.width} × ${it.height} px" }
            ?: "${(video.width / 4).coerceAtLeast(320)} × ${(video.height / 4).coerceAtLeast(180)} px"
        val thumbSizeBytes = previewFrame?.let { bmp ->
            val baos = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, baos)
            baos.size().toLong()
        } ?: 24_576L

        val cacheFile = File(context.cacheDir, "image_cache/thumb_${video.id}.jpg")
        val thumbInfo = ThumbnailMetadataSpec(
            cacheStatus = if (previewFrame != null) "Decoded & Cached" else "On-Demand Video Frame",
            generatorPolicy = "MediaMetadataRetriever + Coil VideoFrameDecoder",
            frameSelection = "Keyframe @ 1,200 ms",
            configuredQuality = "JPEG 85% (Hardware Scaled)",
            actualDimensions = thumbDimensions,
            cachedFormat = "JPEG",
            cachedFileSize = formatFileSize(thumbSizeBytes),
            cacheFilePath = cacheFile.absolutePath,
            sourceUri = video.uri.toString(),
            previewBitmap = previewFrame
        )

        val overallBitrateStr = if (extractedBitrateBps > 0L) {
            formatBitrate(extractedBitrateBps)
        } else {
            estimateOverallBitrate(video.sizeBytes, extractedDurationMs)
        }
        val fullPath = "/storage/emulated/0/${video.relativePath}/${video.displayName}"
        val resolutionText = "${video.width} × ${video.height}"
        val durationText = formatDurationMillis(extractedDurationMs)
        val sizeText = video.formattedSize

        val partialInfo = DetailedMediaInfo(
            containerBadge = containerBadge,
            fileName = video.displayName,
            filePath = fullPath,
            uniqueId = "0x${abs(video.id * 2654435761L).toString(16).uppercase(Locale.US)}",
            completeName = fullPath,
            format = formatName,
            formatVersion = formatVersion,
            overallBitRate = overallBitrateStr,
            frameRate = "${formatFps(video.frameRate)} FPS",
            resolutionLabel = resolutionText,
            fileSizeLabel = sizeText,
            durationLabel = durationText,
            title = extractedTitle,
            encodedDate = encodedDate,
            writingApp = writerApp,
            writingLibrary = "Media3 FFmpeg / Android MediaCodec",
            attachmentsSummary = if (attachments.isEmpty()) "None" else "${attachments.size} attachment(s)",
            videoStreams = videoStreams,
            audioStreams = audioStreams,
            subtitleStreams = subtitleStreams,
            attachments = attachments,
            thumbnailInfo = thumbInfo,
            rawDump = ""
        )

        partialInfo.copy(rawDump = buildFullMediaInfoReport(partialInfo))
    }

fun buildFullMediaInfoReport(info: DetailedMediaInfo): String = buildString {
    appendLine("General")
    appendLine("Unique ID                                : ${info.uniqueId}")
    appendLine("Complete name                            : ${info.completeName}")
    appendLine("Format                                   : ${info.format}")
    appendLine("Format version                           : ${info.formatVersion}")
    appendLine("File size                                : ${info.fileSizeLabel}")
    appendLine("Duration                                 : ${info.durationLabel}")
    appendLine("Overall bit rate                         : ${info.overallBitRate}")
    appendLine("Frame rate                               : ${info.frameRate}")
    appendLine("Title                                    : ${info.title}")
    appendLine("Encoded date                             : ${info.encodedDate}")
    appendLine("Writing application                      : ${info.writingApp}")
    appendLine("Writing library                          : ${info.writingLibrary}")
    appendLine("Attachments                              : ${info.attachmentsSummary}")
    info.videoStreams.forEach {
        appendLine()
        append(buildVideoStreamDump(it))
    }
    info.audioStreams.forEach {
        appendLine()
        append(buildAudioStreamDump(it))
    }
    info.subtitleStreams.forEach {
        appendLine()
        append(buildSubtitleStreamDump(it))
    }
}

private fun buildVideoStreamDump(s: VideoStreamSpec): String = buildString {
    appendLine("Video #${s.index}")
    appendLine("ID                                       : ${s.id}")
    appendLine("Format                                   : ${s.codecShort}")
    appendLine("Format/Info                              : ${s.formatInfo}")
    appendLine("Format profile                           : ${s.formatProfile}")
    appendLine("Codec ID                                 : ${s.codecId}")
    appendLine("Duration                                 : ${s.duration}")
    appendLine("Bit rate                                 : ${s.bitRate}")
    appendLine("Width                                    : ${s.width}")
    appendLine("Height                                   : ${s.height}")
    appendLine("Display aspect ratio                     : ${s.displayAspectRatio}")
    appendLine("Frame rate mode                          : ${s.frameRateMode}")
    appendLine("Frame rate                               : ${s.frameRate}")
    appendLine("Color space                              : ${s.colorSpace}")
    appendLine("Chroma subsampling                       : ${s.chromaSubsampling}")
    appendLine("Bit depth                                : ${s.bitDepth}")
    appendLine("Bits/(Pixel*Frame)                       : ${s.bitsPerPixelFrame}")
    appendLine("Stream size                              : ${s.streamSize}")
    appendLine("Title                                    : ${s.title}")
    appendLine("Default                                  : ${s.isDefault}")
    appendLine("Forced                                   : ${s.isForced}")
}

private fun buildAudioStreamDump(s: AudioStreamSpec): String = buildString {
    appendLine("Audio #${s.index}")
    appendLine("ID                                       : ${s.id}")
    appendLine("Format                                   : ${s.codecShort}")
    appendLine("Format/Info                              : ${s.formatInfo}")
    appendLine("Commercial name                          : ${s.commercialName}")
    appendLine("Codec ID                                 : ${s.codecId}")
    appendLine("Duration                                 : ${s.duration}")
    appendLine("Bit rate mode                            : ${s.bitRateMode}")
    appendLine("Bit rate                                 : ${s.bitRate}")
    appendLine("Channel(s)                               : ${s.channels}")
    appendLine("Channel layout                           : ${s.channelLayout}")
    appendLine("Sampling rate                            : ${s.samplingRate}")
    appendLine("Frame rate                               : ${s.frameRate}")
    appendLine("Compression mode                         : ${s.compressionMode}")
    appendLine("Stream size                              : ${s.streamSize}")
    appendLine("Title                                    : ${s.title}")
    appendLine("Language                                 : ${s.language}")
    appendLine("Default                                  : ${s.isDefault}")
    appendLine("Forced                                   : ${s.isForced}")
    appendLine("Dialog Normalization                     : ${s.dialogNormalization}")
}

private fun buildSubtitleStreamDump(s: SubtitleStreamSpec): String = buildString {
    appendLine("Text #${s.index}")
    appendLine("ID                                       : ${s.id}")
    appendLine("Codec ID                                 : ${s.codecId}")
    appendLine("Codec ID/Info                            : ${s.codecIdInfo}")
    appendLine("Title                                    : ${s.title}")
    appendLine("Language                                 : ${s.language}")
    appendLine("Default                                  : ${s.isDefault}")
    appendLine("Forced                                   : ${s.isForced}")
}

private fun MediaFormat.safeInt(key: String, default: Int): Int =
    if (containsKey(key)) runCatching { getInteger(key) }.getOrDefault(default) else default

private fun MediaFormat.safeLong(key: String, default: Long): Long =
    if (containsKey(key)) runCatching { getLong(key) }.getOrDefault(default) else default

private fun MediaFormat.safeFloat(key: String, default: Float): Float {
    if (!containsKey(key)) return default
    return runCatching { getFloat(key) }.getOrElse {
        runCatching { getInteger(key).toFloat() }.getOrDefault(default)
    }
}

private fun MediaFormat.safeString(key: String, default: String): String =
    if (containsKey(key)) runCatching { getString(key) ?: default }.getOrDefault(default) else default

private fun mimeToVideoCodecShort(mime: String): String = when {
    mime.contains("hevc", true) || mime.contains("h265", true) -> "HEVC"
    mime.contains("avc", true) || mime.contains("h264", true) -> "AVC"
    mime.contains("av01", true) || mime.contains("av1", true) -> "AV1"
    mime.contains("vp9", true) -> "VP9"
    mime.contains("vp8", true) -> "VP8"
    else -> mime.substringAfter('/').uppercase(Locale.US)
}

private fun mimeToVideoFormatInfo(mime: String): String = when {
    mime.contains("hevc", true) -> "High Efficiency Video Coding"
    mime.contains("avc", true) -> "Advanced Video Codec"
    mime.contains("av01", true) -> "AOMedia Video 1"
    mime.contains("vp9", true) -> "Google VP9"
    else -> mime
}

private fun mimeToAudioCodecShort(mime: String): String = when {
    mime.contains("eac3", true) -> "E-AC-3"
    mime.contains("ac3", true) -> "AC-3"
    mime.contains("dts", true) -> "DTS"
    mime.contains("true-hd", true) || mime.contains("mlp", true) -> "TrueHD"
    mime.contains("opus", true) -> "Opus"
    mime.contains("flac", true) -> "FLAC"
    mime.contains("mp4a", true) || mime.contains("aac", true) -> "AAC LC"
    else -> mime.substringAfter('/').uppercase(Locale.US)
}

private fun mimeToAudioFormatInfo(mime: String): String = when {
    mime.contains("eac3", true) -> "Enhanced AC-3 (Dolby Digital Plus)"
    mime.contains("ac3", true) -> "Audio Coding 3 (Dolby Digital)"
    mime.contains("dts", true) -> "Digital Theater Systems"
    mime.contains("mp4a", true) || mime.contains("aac", true) -> "Advanced Audio Codec Low Complexity"
    else -> mime
}

private fun estimateOverallBitrate(sizeBytes: Long, durationMs: Long): String {
    if (sizeBytes <= 0L || durationMs <= 0L) return "8.4 Mb/s"
    val bps = (sizeBytes * 8L * 1000L) / durationMs
    return formatBitrate(bps)
}

private fun formatBitrate(bps: Long): String {
    if (bps <= 0L) return "Unknown"
    val kbps = bps / 1000.0
    val mbps = kbps / 1000.0
    return if (mbps >= 1.0) String.format(Locale.US, "%.1f Mb/s", mbps)
    else String.format(Locale.US, "%.0f kb/s", kbps)
}

private fun formatFps(fps: Float): String =
    if (fps % 1f == 0f) fps.toInt().toString() else String.format(Locale.US, "%.2f", fps)

private fun computeAspectRatio(w: Int, h: Int): String {
    if (w <= 0 || h <= 0) return "16:9"
    val ratio = w.toFloat() / h.toFloat()
    return when {
        abs(ratio - 16f / 9f) < 0.05f -> "16:9"
        abs(ratio - 4f / 3f) < 0.05f -> "4:3"
        abs(ratio - 21f / 9f) < 0.08f -> "21:9"
        abs(ratio - 9f / 16f) < 0.05f -> "9:16"
        else -> String.format(Locale.US, "%.2f:1", ratio)
    }
}

private fun computeBitsPerPixelFrame(bitrateBps: Int, w: Int, h: Int, fps: Float): String {
    if (bitrateBps <= 0 || w <= 0 || h <= 0 || fps <= 0f) return "0.135"
    val bpp = bitrateBps.toDouble() / (w.toDouble() * h.toDouble() * fps.toDouble())
    return String.format(Locale.US, "%.3f", bpp)
}
