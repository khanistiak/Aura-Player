package com.istiak.auraplayer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.istiak.auraplayer.ui.theme.LocalExtendedPalette
import com.istiak.auraplayer.ui.theme.MxBlueAccent

data class CodecDiagnosticItem(
    val id: String,
    val displayTitle: String,
    val codecName: String,
    val mimeTypes: List<String>,
    val isHardwareAccelerated: Boolean,
    val isSoftwareOnly: Boolean,
    val isVideo: Boolean,
    val isBundledExtension: Boolean = false,
    val maxResolutionLabel: String,
    val maxFpsLabel: String,
    val bitrateRangeLabel: String,
    val minResolutionOrSampleRateLabel: String,
    val alignmentOrChannelsLabel: String,
    val maxConcurrentInstances: String,
    val hardwareFeatures: List<String>,
    val colorFormats: List<String>,
    val supportedProfiles: List<String>,
    val supportsHdr10OrDolbyVision: Boolean
)

data class CoreCodecSummary(
    val title: String,
    val mimePrefix: String,
    val primaryCodec: CodecDiagnosticItem?
)

enum class CodecFilterTab {
    ALL,
    HARDWARE,
    SOFTWARE,
    VIDEO,
    AUDIO
}

private fun friendlyCodecTitle(mimeType: String, isVideo: Boolean, codecName: String): String {
    val mime = mimeType.lowercase()
    return when {
        mime.contains("hevc") -> "H.265 / HEVC"
        mime.contains("avc") -> "H.264 / AVC"
        mime.contains("av01") -> "AV1"
        mime.contains("vp9") -> "VP9"
        mime.contains("vp8") -> "VP8"
        mime.contains("dolby-vision") -> "Dolby Vision"
        mime.contains("mp4v-es") -> "MPEG-4 Visual"
        mime.contains("3gpp") -> "H.263"
        mime.contains("mpeg2") -> "MPEG-2 Video"
        mime.contains("eac3-joc") -> "Dolby Digital Plus Atmos (E-AC-3 JOC)"
        mime.contains("eac3") -> "Dolby Digital Plus (E-AC-3)"
        mime.contains("ac3") -> "Dolby Digital (AC-3)"
        mime.contains("ac4") -> "Dolby AC-4"
        mime.contains("vnd.dts.hd") -> "DTS-HD Master Audio"
        mime.contains("vnd.dts") -> "DTS Audio"
        mime.contains("true-hd") -> "Dolby TrueHD"
        mime.contains("mlp") -> "MLP Lossless"
        mime.contains("mp4a-latm") -> "AAC Audio"
        mime.contains("opus") -> "Opus Audio"
        mime.contains("vorbis") -> "Vorbis Audio"
        mime.contains("flac") -> "FLAC Lossless"
        mime.contains("mpeg") && !isVideo -> "MP3 / MPEG Audio"
        mime.contains("raw") -> "PCM / Raw Audio"
        mime.contains("amr-wb") -> "AMR Wideband"
        mime.contains("amr-nb") -> "AMR Narrowband"
        else -> codecName.substringAfterLast('.').uppercase()
    }
}

private fun mapColorFormatName(format: Int): String {
    @Suppress("DEPRECATION")
    return when (format) {
        MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface -> "Surface"
        MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible -> "YUV420Flexible"
        MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar -> "YUV420Planar"
        MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar -> "YUV420SemiPlanar"
        MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar -> "YUV420Packed"
        MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar -> "YUV420PackedSemi"
        MediaCodecInfo.CodecCapabilities.COLOR_FormatYUVP010 -> "10-bit P010"
        MediaCodecInfo.CodecCapabilities.COLOR_FormatRGBAFlexible -> "RGBAFlexible"
        else -> "0x${format.toString(16).uppercase()}"
    }
}

private fun mapProfileName(mime: String, profile: Int): String {
    val m = mime.lowercase()
    return when {
        m.contains("hevc") -> when (profile) {
            MediaCodecInfo.CodecProfileLevel.HEVCProfileMain -> "Main"
            MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10 -> "Main 10"
            MediaCodecInfo.CodecProfileLevel.HEVCProfileMainStill -> "Main Still"
            MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10 -> "Main 10 HDR10"
            MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10Plus -> "Main 10 HDR10+"
            else -> "Profile $profile"
        }
        m.contains("avc") -> when (profile) {
            MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline -> "Baseline"
            MediaCodecInfo.CodecProfileLevel.AVCProfileConstrainedBaseline -> "Constrained Baseline"
            MediaCodecInfo.CodecProfileLevel.AVCProfileMain -> "Main"
            MediaCodecInfo.CodecProfileLevel.AVCProfileExtended -> "Extended"
            MediaCodecInfo.CodecProfileLevel.AVCProfileHigh -> "High"
            MediaCodecInfo.CodecProfileLevel.AVCProfileConstrainedHigh -> "Constrained High"
            MediaCodecInfo.CodecProfileLevel.AVCProfileHigh10 -> "High 10 (10-bit)"
            MediaCodecInfo.CodecProfileLevel.AVCProfileHigh422 -> "High 4:2:2"
            MediaCodecInfo.CodecProfileLevel.AVCProfileHigh444 -> "High 4:4:4"
            else -> "Profile $profile"
        }
        m.contains("av01") -> when (profile) {
            MediaCodecInfo.CodecProfileLevel.AV1ProfileMain8 -> "Main 8-bit"
            MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10 -> "Main 10"
            MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10HDR10 -> "Main 10 HDR10"
            MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10HDR10Plus -> "Main 10 HDR10+"
            else -> "Profile $profile"
        }
        m.contains("vp9") -> when (profile) {
            MediaCodecInfo.CodecProfileLevel.VP9Profile0 -> "Profile 0 (8-bit)"
            MediaCodecInfo.CodecProfileLevel.VP9Profile1 -> "Profile 1"
            MediaCodecInfo.CodecProfileLevel.VP9Profile2 -> "Profile 2 (10-bit)"
            MediaCodecInfo.CodecProfileLevel.VP9Profile2HDR -> "Profile 2 HDR"
            MediaCodecInfo.CodecProfileLevel.VP9Profile2HDR10Plus -> "Profile 2 HDR10+"
            MediaCodecInfo.CodecProfileLevel.VP9Profile3 -> "Profile 3"
            else -> "Profile $profile"
        }
        m.contains("mp4a") -> when (profile) {
            MediaCodecInfo.CodecProfileLevel.AACObjectMain -> "AAC Main"
            MediaCodecInfo.CodecProfileLevel.AACObjectLC -> "AAC-LC"
            MediaCodecInfo.CodecProfileLevel.AACObjectHE -> "HE-AAC"
            MediaCodecInfo.CodecProfileLevel.AACObjectHE_PS -> "HE-AACv2"
            MediaCodecInfo.CodecProfileLevel.AACObjectLD -> "AAC-LD"
            MediaCodecInfo.CodecProfileLevel.AACObjectELD -> "AAC-ELD"
            MediaCodecInfo.CodecProfileLevel.AACObjectXHE -> "xHE-AAC"
            else -> "Profile $profile"
        }
        else -> "Profile $profile"
    }
}

private fun bundledFfmpegSoftwareDecoders(): List<CodecDiagnosticItem> {
    val ffmpegAvailable = runCatching {
        val cls = Class.forName("androidx.media3.decoder.ffmpeg.FfmpegLibrary")
        val method = cls.getMethod("isAvailable")
        method.invoke(null) as? Boolean ?: true
    }.getOrDefault(true)

    val statusSuffix = if (ffmpegAvailable) "Media3 FFmpeg" else "FFmpeg Fallback"

    return listOf(
        CodecDiagnosticItem(
            id = "ffmpeg_eac3",
            displayTitle = "Dolby Digital Plus 5.1 (E-AC-3)",
            codecName = "ffmpeg.audio.eac3 ($statusSuffix)",
            mimeTypes = listOf("audio/eac3", "audio/eac3-joc"),
            isHardwareAccelerated = false,
            isSoftwareOnly = true,
            isVideo = false,
            isBundledExtension = true,
            maxResolutionLabel = "Audio / Stream",
            maxFpsLabel = "N/A",
            bitrateRangeLabel = "32 kbps – 6144 kbps",
            minResolutionOrSampleRateLabel = "32 – 48 kHz",
            alignmentOrChannelsLabel = "Up to 8 ch (5.1 / 7.1)",
            maxConcurrentInstances = "Unlimited (CPU)",
            hardwareFeatures = listOf("FFmpeg Extension", "PREFER Mode", "5.1 Downmix"),
            colorFormats = emptyList(),
            supportedProfiles = listOf("E-AC-3", "E-AC-3 JOC (Atmos core)"),
            supportsHdr10OrDolbyVision = false
        ),
        CodecDiagnosticItem(
            id = "ffmpeg_ac3",
            displayTitle = "Dolby Digital (AC-3)",
            codecName = "ffmpeg.audio.ac3 ($statusSuffix)",
            mimeTypes = listOf("audio/ac3"),
            isHardwareAccelerated = false,
            isSoftwareOnly = true,
            isVideo = false,
            isBundledExtension = true,
            maxResolutionLabel = "Audio / Stream",
            maxFpsLabel = "N/A",
            bitrateRangeLabel = "32 kbps – 640 kbps",
            minResolutionOrSampleRateLabel = "32 – 48 kHz",
            alignmentOrChannelsLabel = "Up to 6 ch (5.1)",
            maxConcurrentInstances = "Unlimited (CPU)",
            hardwareFeatures = listOf("FFmpeg Extension", "PREFER Mode", "Stereo Downmix"),
            colorFormats = emptyList(),
            supportedProfiles = listOf("AC-3 Main"),
            supportsHdr10OrDolbyVision = false
        ),
        CodecDiagnosticItem(
            id = "ffmpeg_dts",
            displayTitle = "DTS Coherent Acoustics",
            codecName = "ffmpeg.audio.dts ($statusSuffix)",
            mimeTypes = listOf("audio/vnd.dts"),
            isHardwareAccelerated = false,
            isSoftwareOnly = true,
            isVideo = false,
            isBundledExtension = true,
            maxResolutionLabel = "Audio / Stream",
            maxFpsLabel = "N/A",
            bitrateRangeLabel = "128 kbps – 1536 kbps",
            minResolutionOrSampleRateLabel = "44.1 – 96 kHz",
            alignmentOrChannelsLabel = "Up to 6 ch (5.1)",
            maxConcurrentInstances = "Unlimited (CPU)",
            hardwareFeatures = listOf("FFmpeg Extension", "PREFER Mode"),
            colorFormats = emptyList(),
            supportedProfiles = listOf("DTS Core", "DTS-ES"),
            supportsHdr10OrDolbyVision = false
        ),
        CodecDiagnosticItem(
            id = "ffmpeg_dtshd",
            displayTitle = "DTS-HD High Resolution / Master Audio",
            codecName = "ffmpeg.audio.dtshd ($statusSuffix)",
            mimeTypes = listOf("audio/vnd.dts.hd"),
            isHardwareAccelerated = false,
            isSoftwareOnly = true,
            isVideo = false,
            isBundledExtension = true,
            maxResolutionLabel = "Audio / Stream",
            maxFpsLabel = "N/A",
            bitrateRangeLabel = "Up to 24.5 Mbps",
            minResolutionOrSampleRateLabel = "48 – 192 kHz",
            alignmentOrChannelsLabel = "Up to 8 ch (7.1)",
            maxConcurrentInstances = "Unlimited (CPU)",
            hardwareFeatures = listOf("FFmpeg Extension", "PREFER Mode"),
            colorFormats = emptyList(),
            supportedProfiles = listOf("DTS-HD Core + Extension"),
            supportsHdr10OrDolbyVision = false
        ),
        CodecDiagnosticItem(
            id = "ffmpeg_truehd",
            displayTitle = "Dolby TrueHD",
            codecName = "ffmpeg.audio.truehd ($statusSuffix)",
            mimeTypes = listOf("audio/true-hd"),
            isHardwareAccelerated = false,
            isSoftwareOnly = true,
            isVideo = false,
            isBundledExtension = true,
            maxResolutionLabel = "Audio / Stream",
            maxFpsLabel = "N/A",
            bitrateRangeLabel = "Lossless VBR (up to 18 Mbps)",
            minResolutionOrSampleRateLabel = "48 – 192 kHz",
            alignmentOrChannelsLabel = "Up to 8 ch (7.1)",
            maxConcurrentInstances = "Unlimited (CPU)",
            hardwareFeatures = listOf("FFmpeg Extension", "PREFER Mode"),
            colorFormats = emptyList(),
            supportedProfiles = listOf("TrueHD Lossless"),
            supportsHdr10OrDolbyVision = false
        ),
        CodecDiagnosticItem(
            id = "ffmpeg_mlp",
            displayTitle = "MLP (Meridian Lossless Packing)",
            codecName = "ffmpeg.audio.mlp ($statusSuffix)",
            mimeTypes = listOf("audio/mlp"),
            isHardwareAccelerated = false,
            isSoftwareOnly = true,
            isVideo = false,
            isBundledExtension = true,
            maxResolutionLabel = "Audio / Stream",
            maxFpsLabel = "N/A",
            bitrateRangeLabel = "Lossless VBR",
            minResolutionOrSampleRateLabel = "44.1 – 192 kHz",
            alignmentOrChannelsLabel = "Up to 8 ch",
            maxConcurrentInstances = "Unlimited (CPU)",
            hardwareFeatures = listOf("FFmpeg Extension", "PREFER Mode"),
            colorFormats = emptyList(),
            supportedProfiles = listOf("MLP Lossless"),
            supportsHdr10OrDolbyVision = false
        )
    )
}

fun queryDeviceCodecDiagnostics(): List<CodecDiagnosticItem> {
    val systemItems = try {
        val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
        val infos = codecList.codecInfos.filter { !it.isEncoder }
        infos.mapIndexed { idx, info ->
            val types = info.supportedTypes.toList()
            val primaryMime = types.firstOrNull().orEmpty()
            val isVideo = types.any { it.startsWith("video/", ignoreCase = true) }
            val isHw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                info.isHardwareAccelerated
            } else {
                !info.name.startsWith("OMX.google.", ignoreCase = true) &&
                    !info.name.startsWith("c2.android.", ignoreCase = true)
            }
            val isSw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                info.isSoftwareOnly
            } else {
                !isHw
            }

            var minW = Int.MAX_VALUE
            var minH = Int.MAX_VALUE
            var maxW = 0
            var maxH = 0
            var maxFps = 0
            var alignW = 0
            var alignH = 0
            var minBitrate = Int.MAX_VALUE
            var maxBitrate = 0
            var maxChannels = 0
            var sampleRatesLabel = "8 – 48 kHz"
            var maxInstances = 0
            var supportsHdr = false
            val features = linkedSetOf<String>()
            val colors = linkedSetOf<String>()
            val profiles = linkedSetOf<String>()

            for (type in types) {
                try {
                    val caps = info.getCapabilitiesForType(type)
                    if (caps.maxSupportedInstances > maxInstances) {
                        maxInstances = caps.maxSupportedInstances
                    }
                    caps.videoCapabilities?.let { vCaps ->
                        if (vCaps.supportedWidths.lower < minW) minW = vCaps.supportedWidths.lower
                        if (vCaps.supportedHeights.lower < minH) minH = vCaps.supportedHeights.lower
                        if (vCaps.supportedWidths.upper > maxW) maxW = vCaps.supportedWidths.upper
                        if (vCaps.supportedHeights.upper > maxH) maxH = vCaps.supportedHeights.upper
                        if (vCaps.supportedFrameRates.upper > maxFps) maxFps = vCaps.supportedFrameRates.upper
                        if (vCaps.widthAlignment > alignW) alignW = vCaps.widthAlignment
                        if (vCaps.heightAlignment > alignH) alignH = vCaps.heightAlignment
                        if (vCaps.bitrateRange.lower < minBitrate) minBitrate = vCaps.bitrateRange.lower
                        if (vCaps.bitrateRange.upper > maxBitrate) maxBitrate = vCaps.bitrateRange.upper
                    }
                    caps.audioCapabilities?.let { aCaps ->
                        if (aCaps.bitrateRange.lower < minBitrate) minBitrate = aCaps.bitrateRange.lower
                        if (aCaps.bitrateRange.upper > maxBitrate) maxBitrate = aCaps.bitrateRange.upper
                        if (aCaps.maxInputChannelCount > maxChannels) maxChannels = aCaps.maxInputChannelCount
                        val rates = aCaps.supportedSampleRates
                        if (rates != null && rates.isNotEmpty()) {
                            val minKhz = rates.min() / 1000.0
                            val maxKhz = rates.max() / 1000.0
                            sampleRatesLabel = if (minKhz == maxKhz) {
                                "${maxKhz.toInt()} kHz"
                            } else {
                                "${minKhz.toInt()} – ${maxKhz.toInt()} kHz"
                            }
                        } else {
                            val range = aCaps.supportedSampleRateRanges?.firstOrNull()
                            if (range != null) {
                                sampleRatesLabel = "${range.lower / 1000} – ${range.upper / 1000} kHz"
                            }
                        }
                    }

                    if (caps.isFeatureSupported(MediaCodecInfo.CodecCapabilities.FEATURE_AdaptivePlayback)) {
                        features.add("Adaptive Res")
                    }
                    if (caps.isFeatureSupported("low-latency")) {
                        features.add("Low Latency")
                    }
                    if (caps.isFeatureSupported(MediaCodecInfo.CodecCapabilities.FEATURE_TunneledPlayback)) {
                        features.add("Tunneled")
                    }
                    if (caps.isFeatureSupported(MediaCodecInfo.CodecCapabilities.FEATURE_SecurePlayback)) {
                        features.add("Secure DRM")
                    }

                    caps.colorFormats?.take(6)?.forEach { fmt ->
                        colors.add(mapColorFormatName(fmt))
                    }

                    val profileLevels = caps.profileLevels ?: emptyArray()
                    profileLevels.forEach { pl ->
                        profiles.add(mapProfileName(type, pl.profile))
                        if (pl.profile == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10 ||
                            pl.profile == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10 ||
                            pl.profile == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10Plus ||
                            pl.profile == MediaCodecInfo.CodecProfileLevel.VP9Profile2 ||
                            pl.profile == MediaCodecInfo.CodecProfileLevel.VP9Profile2HDR ||
                            pl.profile == MediaCodecInfo.CodecProfileLevel.VP9Profile2HDR10Plus ||
                            pl.profile == MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10 ||
                            pl.profile == MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10HDR10 ||
                            pl.profile == MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10HDR10Plus ||
                            type.contains("dolby-vision", ignoreCase = true)
                        ) {
                            supportsHdr = true
                        }
                    }
                } catch (_: Exception) {}
            }

            val bitrateLabel = if (maxBitrate > 0 && minBitrate != Int.MAX_VALUE) {
                val minK = (minBitrate / 1000).coerceAtLeast(1)
                val maxLabel = if (maxBitrate >= 1_000_000) {
                    "${maxBitrate / 1_000_000} Mbps"
                } else {
                    "${(maxBitrate / 1000).coerceAtLeast(1)} kbps"
                }
                "$minK kbps – $maxLabel"
            } else {
                "Adaptive Stream"
            }

            CodecDiagnosticItem(
                id = "${info.name}_$idx",
                displayTitle = friendlyCodecTitle(primaryMime, isVideo, info.name),
                codecName = info.name,
                mimeTypes = types,
                isHardwareAccelerated = isHw,
                isSoftwareOnly = isSw,
                isVideo = isVideo,
                isBundledExtension = false,
                maxResolutionLabel = if (maxW > 0 && maxH > 0) "${maxW}x${maxH}" else "Audio / Stream",
                maxFpsLabel = if (maxFps > 0) "${maxFps}fps" else "N/A",
                bitrateRangeLabel = bitrateLabel,
                minResolutionOrSampleRateLabel = if (isVideo) {
                    if (minW != Int.MAX_VALUE && minH != Int.MAX_VALUE) "${minW}x${minH}" else "64x64"
                } else {
                    sampleRatesLabel
                },
                alignmentOrChannelsLabel = if (isVideo) {
                    if (alignW > 0 && alignH > 0) "${alignW}x${alignH}" else "2x2"
                } else {
                    if (maxChannels > 0) "$maxChannels ch" else "2 ch"
                },
                maxConcurrentInstances = if (maxInstances > 0) maxInstances.toString() else "16",
                hardwareFeatures = features.toList().ifEmpty { listOf(if (isHw) "VPU Accelerated" else "CPU Fallback") },
                colorFormats = colors.toList(),
                supportedProfiles = profiles.toList().take(8),
                supportsHdr10OrDolbyVision = supportsHdr
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }

    return (systemItems + bundledFfmpegSoftwareDecoders()).sortedWith(
        compareByDescending<CodecDiagnosticItem> { it.isVideo }
            .thenByDescending { it.isHardwareAccelerated }
            .thenByDescending { it.isBundledExtension }
            .thenBy { it.displayTitle }
            .thenBy { it.codecName }
    )
}

/**
 * Supported Codecs & Decoder Diagnostics Screen (`CodecDiagnosticsScreen.kt`):
 * - Back arrow + bold "Supported Codecs" + Device Model & Android API subtitle
 * - Elevated "Hardware vs. Software Decoding" summary card with Hardware / Software / Video tiles
 * - Core Video Codecs Status cards (H.264 / AVC, H.265 / HEVC, AV1, VP9) with 10-Bit HDR & Hardware badges
 * - Rounded Search bar + All / Hardware / Software / Video / Audio filter chips
 * - Expandable detailed decoder cards (including MediaCodecList + bundled FFmpeg audio decoders)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CodecDiagnosticsScreen(
    onBack: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val cardBg = if (isDark) Color(0xFF161F33) else Color.White
    val cardBorder = if (isDark) Color.White.copy(alpha = 0.10f) else Color(0xFFE5ECF6)

    val allCodecs = remember { queryDeviceCodecDiagnostics() }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(CodecFilterTab.ALL) }
    var expandedCodecIds by remember { mutableStateOf(setOf<String>()) }

    val hwCount = remember(allCodecs) { allCodecs.count { it.isHardwareAccelerated } }
    val swCount = remember(allCodecs) { allCodecs.count { !it.isHardwareAccelerated } }
    val videoCount = remember(allCodecs) { allCodecs.count { it.isVideo } }
    val audioCount = remember(allCodecs) { allCodecs.count { !it.isVideo } }

    val coreVideoCodecs = remember(allCodecs) {
        listOf(
            "H.264 / AVC" to "video/avc",
            "H.265 / HEVC" to "video/hevc",
            "AV1" to "video/av01",
            "VP9" to "video/x-vnd.on2.vp9"
        ).map { (label, mime) ->
            val primary = allCodecs
                .filter { it.mimeTypes.any { m -> m.equals(mime, ignoreCase = true) } }
                .sortedByDescending { it.isHardwareAccelerated }
                .firstOrNull()
            CoreCodecSummary(title = label, mimePrefix = mime, primaryCodec = primary)
        }
    }

    val filteredCodecs = remember(allCodecs, selectedFilter, searchQuery) {
        val byTab = when (selectedFilter) {
            CodecFilterTab.ALL -> allCodecs
            CodecFilterTab.HARDWARE -> allCodecs.filter { it.isHardwareAccelerated }
            CodecFilterTab.SOFTWARE -> allCodecs.filter { !it.isHardwareAccelerated }
            CodecFilterTab.VIDEO -> allCodecs.filter { it.isVideo }
            CodecFilterTab.AUDIO -> allCodecs.filter { !it.isVideo }
        }
        val q = searchQuery.trim()
        if (q.isEmpty()) byTab
        else byTab.filter { item ->
            item.displayTitle.contains(q, ignoreCase = true) ||
                item.codecName.contains(q, ignoreCase = true) ||
                item.mimeTypes.any { it.contains(q, ignoreCase = true) }
        }
    }

    val deviceModelLabel = remember {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val model = Build.MODEL
        val modelText = if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model"
        "$modelText • Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("codec_diagnostics_screen")
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, end = 10.dp, top = 8.dp, bottom = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("codec_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = palette.brandTitleColor
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Supported Codecs",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = deviceModelLabel,
                        fontSize = 12.sp,
                        color = palette.secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = {
                    val report = buildString {
                        appendLine("Aura Player Supported Codecs ($deviceModelLabel)")
                        appendLine("$hwCount Hardware • $swCount Software Decoders ($videoCount Video, $audioCount Audio)")
                        appendLine("---")
                        allCodecs.forEach { c ->
                            val hwTag = if (c.isHardwareAccelerated) "HW" else "SW"
                            val kind = if (c.isVideo) "VIDEO" else "AUDIO"
                            val limit = if (c.isVideo) "${c.maxResolutionLabel} @ ${c.maxFpsLabel}" else c.bitrateRangeLabel
                            appendLine("${c.displayTitle} [$kind/$hwTag] ${c.codecName} -> ${c.mimeTypes.joinToString()} ($limit)")
                        }
                    }
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Supported Codecs", report))
                    onShowMessage("Copied ${allCodecs.size} decoder specs to clipboard")
                },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("copy_diagnostics_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = "Copy Diagnostics",
                    tint = palette.brandTitleColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("codec_list")
        ) {
            // 1. Summary Card ("Hardware vs. Software Decoding")
            item(key = "summary_card") {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = cardBg,
                    tonalElevation = 2.dp,
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, cardBorder, RoundedCornerShape(18.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Hardware vs. Software Decoding",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryText
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$hwCount Hardware • $swCount Software Decoders",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MxBlueAccent
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SummaryMetricTile(
                                label = "Hardware",
                                value = hwCount.toString(),
                                accentColor = MxBlueAccent,
                                isDark = isDark,
                                modifier = Modifier.weight(1f)
                            )
                            SummaryMetricTile(
                                label = "Software",
                                value = swCount.toString(),
                                accentColor = Color(0xFF7C3AED),
                                isDark = isDark,
                                modifier = Modifier.weight(1f)
                            )
                            SummaryMetricTile(
                                label = "Video",
                                value = videoCount.toString(),
                                accentColor = Color(0xFF059669),
                                isDark = isDark,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Hardware decoders offload decoding to the device VPU/GPU for smooth 4K HDR playback, while bundled FFmpeg software decoders run on the CPU to play surround formats (EAC3, AC3, DTS, TrueHD) when hardware support is absent.",
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp,
                            color = palette.secondaryText
                        )
                    }
                }
            }

            // 2. Core Video Codecs Status Section (4 clean cards)
            item(key = "core_video_section_title") {
                Text(
                    text = "Core Video Codecs",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryText,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(coreVideoCodecs, key = { "core_${it.mimePrefix}" }) { core ->
                val primary = core.primaryCodec
                val isHw = primary?.isHardwareAccelerated == true
                val hasHdr = primary?.supportsHdr10OrDolbyVision == true

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cardBg,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = core.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.primaryText
                                )
                                if (hasHdr) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFFEF3C7))
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "10-Bit HDR",
                                            color = Color(0xFFB45309),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isHw) Color(0xFF1E293B)
                                        else Color(0xFF475569)
                                    )
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (primary == null) "Unavailable" else if (isHw) "Hardware" else "Software",
                                    color = Color.White,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = primary?.codecName ?: "No system decoder reported",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = palette.secondaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        val limitText = if (primary != null && primary.maxResolutionLabel != "Audio / Stream") {
                            "Max Limit: ${primary.maxResolutionLabel} @ ${primary.maxFpsLabel}"
                        } else {
                            "Max Limit: 1920x1080 @ 60fps"
                        }
                        Text(
                            text = limitText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MxBlueAccent
                        )
                    }
                }
            }

            // 3. Search & Filter Chips
            item(key = "search_and_filters") {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        text = "All Registered Decoders",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryText,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Rounded search bar
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = cardBg,
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 11.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = palette.secondaryText,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search codec name, format, or MIME...",
                                        color = palette.secondaryText,
                                        fontSize = 13.5.sp
                                    )
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        color = palette.primaryText,
                                        fontSize = 13.5.sp
                                    ),
                                    cursorBrush = SolidColor(MxBlueAccent),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("codec_search_input")
                                )
                            }
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Clear search",
                                        tint = palette.secondaryText,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val filterLabels = listOf(
                        CodecFilterTab.ALL to "All (${allCodecs.size})",
                        CodecFilterTab.HARDWARE to "Hardware ($hwCount)",
                        CodecFilterTab.SOFTWARE to "Software ($swCount)",
                        CodecFilterTab.VIDEO to "Video ($videoCount)",
                        CodecFilterTab.AUDIO to "Audio ($audioCount)"
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(filterLabels) { (tab, label) ->
                            val isSelected = selectedFilter == tab
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(
                                        if (isSelected) MxBlueAccent
                                        else if (isDark) Color(0xFF1E293B) else Color.White
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) MxBlueAccent else cardBorder,
                                        shape = RoundedCornerShape(999.dp)
                                    )
                                    .clickable { selectedFilter = tab }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                                    .testTag("codec_filter_${tab.name.lowercase()}")
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else palette.primaryText,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // 4. Expandable Detailed Decoder List
            items(filteredCodecs, key = { it.id }) { item ->
                val isExpanded = item.id in expandedCodecIds
                ExpandableCodecCard(
                    item = item,
                    expanded = isExpanded,
                    isDark = isDark,
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    onToggleExpand = {
                        expandedCodecIds = if (isExpanded) {
                            expandedCodecIds - item.id
                        } else {
                            expandedCodecIds + item.id
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SummaryMetricTile(
    label: String,
    value: String,
    accentColor: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val tileBg = if (isDark) accentColor.copy(alpha = 0.16f) else accentColor.copy(alpha = 0.08f)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(tileBg)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                color = accentColor,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExpandableCodecCard(
    item: CodecDiagnosticItem,
    expanded: Boolean,
    isDark: Boolean,
    cardBg: Color,
    cardBorder: Color,
    onToggleExpand: () -> Unit
) {
    val palette = LocalExtendedPalette.current
    val swPurple = Color(0xFF7C3AED)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onToggleExpand)
            .testTag("codec_card_${item.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = item.displayTitle,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.primaryText
                        )

                        // VIDEO / AUDIO badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(palette.neutralChipBg)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (item.isVideo) "VIDEO" else "AUDIO",
                                color = palette.neutralChipText,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // HW (blue) / SW (purple) badge
                        val modeColor = if (item.isHardwareAccelerated) MxBlueAccent else swPurple
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(modeColor.copy(alpha = if (isDark) 0.24f else 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (item.isHardwareAccelerated) "HW" else "SW",
                                color = if (isDark && !item.isHardwareAccelerated) Color(0xFFC4B5FD) else modeColor,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = item.codecName,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = palette.secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val summaryCapability = if (item.isVideo) {
                        "${item.maxResolutionLabel} @ ${item.maxFpsLabel}"
                    } else {
                        "${item.bitrateRangeLabel} • ${item.alignmentOrChannelsLabel}"
                    }
                    Text(
                        text = summaryCapability,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (item.isBundledExtension) swPurple else MxBlueAccent
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = palette.secondaryText,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(22.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = cardBorder)
                    Spacer(modifier = Modifier.height(10.dp))

                    CodecDetailSpecRow(
                        label = "MIME Type",
                        value = item.mimeTypes.joinToString(", ")
                    )
                    CodecDetailSpecRow(
                        label = if (item.isVideo) "Min Resolution" else "Sample Rates",
                        value = item.minResolutionOrSampleRateLabel
                    )
                    CodecDetailSpecRow(
                        label = if (item.isVideo) "Pixel Alignment" else "Max Channels",
                        value = item.alignmentOrChannelsLabel
                    )
                    CodecDetailSpecRow(
                        label = "Max Concurrent Instances",
                        value = item.maxConcurrentInstances
                    )

                    if (item.hardwareFeatures.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Hardware Features",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.secondaryText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            item.hardwareFeatures.forEach { feat ->
                                MetadataChip(text = feat, isAccent = feat == "Adaptive Res" || feat == "Low Latency")
                            }
                        }
                    }

                    if (item.colorFormats.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Supported Color Formats",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.secondaryText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            item.colorFormats.forEach { cf ->
                                MetadataChip(text = cf)
                            }
                        }
                    }

                    if (item.supportedProfiles.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Supported Profiles",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.secondaryText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            item.supportedProfiles.forEach { prof ->
                                MetadataChip(text = prof)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CodecDetailSpecRow(
    label: String,
    value: String
) {
    val palette = LocalExtendedPalette.current
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = palette.secondaryText
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = palette.primaryText
        )
    }
}
