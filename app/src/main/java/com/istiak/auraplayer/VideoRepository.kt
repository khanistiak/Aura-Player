package com.istiak.auraplayer

import android.app.PendingIntent
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.content.pm.ActivityInfo
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.MediaStore
import androidx.annotation.RawRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

data class VideoItem(
    val id: Long,
    val uri: Uri,
    val title: String,
    val displayName: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val folderName: String,
    val relativePath: String = "Movies/$folderName",
    val frameRate: Float = 24f,
    val videoCodec: String = "HEVC / H.265",
    val audioTrackInfo: String = "AAC Stereo (Default)",
    val subtitleInfo: String = "English [SRT]",
    val hasSubtitles: Boolean = true,
    val dateAddedSec: Long,
    val mimeType: String = "video/mp4",
    val isVaultItem: Boolean = false,
    val vaultFilePath: String? = null,
    val vaultThumbnailPath: String? = null,
    val rotationDegrees: Int = 0,
    val dateModifiedSec: Long = dateAddedSec,
    val isNew: Boolean = false,
    val storageVolumeId: String = PRIMARY_STORAGE_VOLUME_ID,
    val filePath: String? = null
) {
    fun effectiveVisualDimensions(): Pair<Int, Int> {
        val normalizedRot = ((rotationDegrees % 360) + 360) % 360
        val isRotated90or270 = (normalizedRot == 90 || normalizedRot == 270)
        val effectiveWidth = if (isRotated90or270) height else width
        val effectiveHeight = if (isRotated90or270) width else height
        return effectiveWidth to effectiveHeight
    }

    val isPortraitVideo: Boolean
        get() {
            val (effectiveWidth, effectiveHeight) = effectiveVisualDimensions()
            return effectiveHeight > effectiveWidth
        }

    fun isConsideredNew(watchedVideoIds: Set<Long>, seenBaselineEpochSec: Long = 0L): Boolean {
        if (id in watchedVideoIds) return false
        if (seenBaselineEpochSec <= 0L) return true
        val latestTimestampSec = maxOf(
            normalizeEpochSeconds(dateAddedSec),
            normalizeEpochSeconds(dateModifiedSec)
        )
        return latestTimestampSec > seenBaselineEpochSec
    }

    fun isConsideredNew(
        watchedVideoIds: Set<Long>,
        seenBaselineEpochSec: Long,
        showNewLabel: Boolean,
        newLabelDaysThreshold: Int,
        referenceTimeMs: Long = System.currentTimeMillis()
    ): Boolean {
        if (!showNewLabel) return false
        if (!isConsideredNew(watchedVideoIds, seenBaselineEpochSec)) return false
        val latestTimestampSec = maxOf(
            normalizeEpochSeconds(dateAddedSec),
            normalizeEpochSeconds(dateModifiedSec)
        )
        if (latestTimestampSec <= 0L) return false
        val thresholdMs = TimeUnit.DAYS.toMillis(newLabelDaysThreshold.coerceIn(1, 30).toLong())
        val itemTimestampMs = latestTimestampSec * 1000L
        return itemTimestampMs >= (referenceTimeMs - thresholdMs)
    }

    val resolutionTag: String
        get() {
            val maxDim = maxOf(width, height)
            val minDim = minOf(width, height)
            return when {
                maxDim >= 3840 || minDim >= 2160 || title.contains("4K", ignoreCase = true) -> "4K"
                maxDim >= 2560 || minDim >= 1440 -> "1440P"
                maxDim >= 1920 || minDim >= 1080 || title.contains("1080", ignoreCase = true) -> "1080P"
                maxDim >= 1280 || minDim >= 720 -> "720P"
                else -> "HD"
            }
        }

    val verticalPixelsLabel: String
        get() {
            val minDim = minOf(width, height)
            val maxDim = maxOf(width, height)
            return when {
                maxDim >= 3840 || minDim >= 2160 || title.contains("4K", ignoreCase = true) || displayName.contains("2160p", ignoreCase = true) -> "2160p"
                maxDim >= 2560 || minDim >= 1440 -> "1440p"
                maxDim >= 1920 || minDim >= 1080 || title.contains("1080", ignoreCase = true) || displayName.contains("1080p", ignoreCase = true) -> "1080p"
                maxDim >= 1280 || minDim >= 720 -> "720p"
                else -> "${minDim.coerceAtLeast(480)}p"
            }
        }

    val formattedFramerate: String
        get() = if (frameRate % 1.0f == 0f) {
            frameRate.toInt().toString()
        } else {
            String.format(Locale.US, "%.2f", frameRate)
        }

    val resolutionFramerateLabel: String
        get() = "$verticalPixelsLabel@$formattedFramerate"

    val extensionLabel: String
        get() = displayName.substringAfterLast('.', "mp4").uppercase(Locale.US)

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            return sdf.format(Date(dateAddedSec * 1000L))
        }

    val isHdr: Boolean
        get() = title.contains("HDR", ignoreCase = true) ||
            displayName.contains("HDR", ignoreCase = true) ||
            maxOf(width, height) >= 3840

    val formattedDuration: String
        get() = formatDurationMillis(durationMs)

    val formattedSize: String
        get() = formatFileSize(sizeBytes)

    val formattedResolution: String
        get() {
            val (ew, eh) = effectiveVisualDimensions()
            return if (ew > 0 && eh > 0) "${ew}×${eh}" else resolutionTag
        }
}

data class VideoFolder(
    val name: String,
    val path: String,
    val videos: List<VideoItem>,
    val subfolders: List<String> = emptyList()
) {
    val videoCount: Int get() = videos.size
    val totalSizeBytes: Long get() = videos.sumOf { it.sizeBytes }
    val totalDurationMs: Long get() = videos.sumOf { it.durationMs }
    val formattedTotalSize: String get() = formatFileSize(totalSizeBytes)
    val formattedTotalDuration: String get() = formatFolderDurationCompact(totalDurationMs)
    val previewVideo: VideoItem? get() = videos.firstOrNull()
    val has4K: Boolean get() = videos.any { it.resolutionTag == "4K" }
}

enum class VideoSortOption(val label: String) {
    DATE("Date Added"),
    NAME("Name (A–Z)"),
    SIZE("File Size")
}

sealed class RenameVideoResult {
    data object Success : RenameVideoResult()
    data class RequiresWritePermission(val intentSender: IntentSender) : RenameVideoResult()
    data class Error(val message: String) : RenameVideoResult()
}

sealed class BatchFileOperationResult {
    data class Completed(val processedCount: Int) : BatchFileOperationResult()
    data class RequiresDeleteConfirmation(
        val processedCount: Int,
        val deleteIntentSender: IntentSender
    ) : BatchFileOperationResult()
    data class Error(val message: String) : BatchFileOperationResult()
}

fun formatDurationMillis(durationMs: Long): String {
    if (durationMs <= 0L) return "00:00"
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

fun formatFolderDurationCompact(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        else -> String.format(Locale.US, "%.0f KB", kb)
    }
}

internal fun normalizeEpochSeconds(timestamp: Long): Long {
    return if (timestamp > 10_000_000_000L) timestamp / 1000L else timestamp
}

fun resolveTargetActivityOrientation(
    mode: VideoOrientationMode,
    width: Int,
    height: Int,
    rotationDegrees: Int = 0
): Int {
    val normalizedRot = ((rotationDegrees % 360) + 360) % 360
    val isRotated90or270 = (normalizedRot == 90 || normalizedRot == 270)
    val effectiveWidth = if (isRotated90or270) height else width
    val effectiveHeight = if (isRotated90or270) width else height
    val isPortraitVideo = effectiveHeight > effectiveWidth

    return when (mode) {
        VideoOrientationMode.FREE -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        VideoOrientationMode.VIDEO -> {
            if (effectiveWidth > 0 && effectiveHeight > 0) {
                if (isPortraitVideo) {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                }
            } else {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
        VideoOrientationMode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        VideoOrientationMode.REVERSE_PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
        VideoOrientationMode.SENSOR_PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        VideoOrientationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        VideoOrientationMode.REVERSE_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
        VideoOrientationMode.SENSOR_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }
}

const val PRIMARY_STORAGE_VOLUME_ID = "primary"

enum class StorageVolumeKind {
    INTERNAL,
    SD_CARD,
    USB_OTG
}

data class StorageVolumeOption(
    val id: String,
    val label: String,
    val rootPath: String,
    val kind: StorageVolumeKind
)

fun discoverMountedStorageVolumes(context: Context): List<StorageVolumeOption> {
    val optionsByRoot = linkedMapOf<String, StorageVolumeOption>()
    val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager

    val externalAppRoots = runCatching {
        context.getExternalFilesDirs(null)
            ?.mapNotNull { dir ->
                dir?.absolutePath?.substringBefore("/Android/data/")?.trimEnd('/')?.takeIf { it.isNotBlank() }
            }
            ?: emptyList()
    }.getOrDefault(emptyList())

    fun classifyVolume(
        rootPath: String,
        isPrimary: Boolean,
        isRemovable: Boolean,
        rawDescription: String?,
        uuid: String?
    ): StorageVolumeOption {
        val desc = rawDescription?.trim().orEmpty()
        val combined = "$desc $rootPath".lowercase(Locale.US)
        return when {
            isPrimary || !isRemovable || rootPath.startsWith("/storage/emulated") -> {
                StorageVolumeOption(
                    id = PRIMARY_STORAGE_VOLUME_ID,
                    label = "Internal Storage",
                    rootPath = rootPath.ifBlank { "/storage/emulated/0" },
                    kind = StorageVolumeKind.INTERNAL
                )
            }
            combined.contains("usb") || combined.contains("otg") || combined.contains("mass storage") -> {
                val volId = uuid?.takeIf { it.isNotBlank() }?.lowercase(Locale.US)
                    ?: rootPath.substringAfterLast('/').lowercase(Locale.US).ifBlank { "usb_otg" }
                StorageVolumeOption(
                    id = volId,
                    label = if (desc.isNotBlank() && !desc.equals("usb", ignoreCase = true)) "USB OTG ($desc)" else "USB OTG",
                    rootPath = rootPath,
                    kind = StorageVolumeKind.USB_OTG
                )
            }
            else -> {
                val volId = uuid?.takeIf { it.isNotBlank() }?.lowercase(Locale.US)
                    ?: rootPath.substringAfterLast('/').lowercase(Locale.US).ifBlank { "sd_card" }
                StorageVolumeOption(
                    id = volId,
                    label = if (desc.isNotBlank() && !desc.equals("sd card", ignoreCase = true)) "SD Card ($desc)" else "SD Card",
                    rootPath = rootPath,
                    kind = StorageVolumeKind.SD_CARD
                )
            }
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && storageManager != null) {
        val volumes = runCatching { storageManager.storageVolumes }.getOrDefault(emptyList())
        for (vol in volumes) {
            val state = vol.state
            if (state != Environment.MEDIA_MOUNTED && state != Environment.MEDIA_MOUNTED_READ_ONLY) {
                continue
            }
            val dirPath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                vol.directory?.absolutePath?.trimEnd('/')
            } else {
                null
            }
            val uuid = vol.uuid?.trim()
            val matchedAppRoot = when {
                !dirPath.isNullOrBlank() -> dirPath
                vol.isPrimary -> externalAppRoots.firstOrNull { it.startsWith("/storage/emulated") }
                    ?: Environment.getExternalStorageDirectory()?.absolutePath?.trimEnd('/')
                !uuid.isNullOrBlank() -> externalAppRoots.firstOrNull { it.contains(uuid, ignoreCase = true) }
                    ?: "/storage/$uuid"
                else -> externalAppRoots.firstOrNull { !it.startsWith("/storage/emulated") }
            }
            val resolvedRoot = matchedAppRoot?.takeIf { it.isNotBlank() } ?: continue
            val option = classifyVolume(
                rootPath = resolvedRoot,
                isPrimary = vol.isPrimary,
                isRemovable = vol.isRemovable,
                rawDescription = runCatching { vol.getDescription(context) }.getOrNull(),
                uuid = uuid
            )
            optionsByRoot.putIfAbsent(option.rootPath, option)
        }
    }

    // Also inspect getExternalFilesDirs(null) for any removable OTG/SD roots not yet registered
    for (rootPath in externalAppRoots) {
        if (optionsByRoot.keys.any { rootPath.equals(it, ignoreCase = true) }) continue
        val isPrimary = rootPath.startsWith("/storage/emulated")
        val option = classifyVolume(
            rootPath = rootPath,
            isPrimary = isPrimary,
            isRemovable = !isPrimary,
            rawDescription = null,
            uuid = rootPath.substringAfterLast('/').takeIf { !isPrimary }
        )
        optionsByRoot.putIfAbsent(option.rootPath, option)
    }

    if (optionsByRoot.values.none { it.kind == StorageVolumeKind.INTERNAL }) {
        val defaultPrimaryRoot = Environment.getExternalStorageDirectory()?.absolutePath?.trimEnd('/')
            ?: "/storage/emulated/0"
        optionsByRoot[defaultPrimaryRoot] = StorageVolumeOption(
            id = PRIMARY_STORAGE_VOLUME_ID,
            label = "Internal Storage",
            rootPath = defaultPrimaryRoot,
            kind = StorageVolumeKind.INTERNAL
        )
    }

    return optionsByRoot.values.sortedWith(
        compareBy<StorageVolumeOption> {
            when (it.kind) {
                StorageVolumeKind.INTERNAL -> 0
                StorageVolumeKind.SD_CARD -> 1
                StorageVolumeKind.USB_OTG -> 2
            }
        }.thenBy { it.label.lowercase(Locale.US) }
    )
}

internal fun resolveStorageVolumeId(
    dataPath: String?,
    mediaStoreVolumeName: String?,
    mountedVolumes: List<StorageVolumeOption>
): String {
    if (!dataPath.isNullOrBlank()) {
        val matched = mountedVolumes.firstOrNull { vol ->
            vol.rootPath.isNotBlank() && dataPath.startsWith("${vol.rootPath}/", ignoreCase = true)
        }
        if (matched != null) return matched.id
        if (dataPath.startsWith("/storage/emulated/", ignoreCase = true) || dataPath.startsWith("/sdcard/", ignoreCase = true)) {
            return PRIMARY_STORAGE_VOLUME_ID
        }
        val segment = dataPath.removePrefix("/storage/").substringBefore('/').trim()
        if (segment.isNotBlank()) return segment.lowercase(Locale.US)
    }
    val volName = mediaStoreVolumeName?.trim().orEmpty()
    if (volName.isNotBlank() && !volName.equals(MediaStore.VOLUME_EXTERNAL_PRIMARY, ignoreCase = true)) {
        val matchedById = mountedVolumes.firstOrNull { it.id.equals(volName, ignoreCase = true) }
        return matchedById?.id ?: volName.lowercase(Locale.US)
    }
    return PRIMARY_STORAGE_VOLUME_ID
}

fun filterVideosByStorageVolume(
    videos: List<VideoItem>,
    selectedVolumeId: String?,
    mountedVolumes: List<StorageVolumeOption>
): List<VideoItem> {
    if (mountedVolumes.size <= 1 || selectedVolumeId.isNullOrBlank()) return videos
    val validIds = mountedVolumes.map { it.id }.toSet()
    val effectiveId = if (selectedVolumeId in validIds) selectedVolumeId else PRIMARY_STORAGE_VOLUME_ID
    return videos.filter { it.storageVolumeId.equals(effectiveId, ignoreCase = true) }
}

class VideoRepository(private val context: Context) {

    /**
     * Triggers `MediaScannerConnection.scanFile()` across primary external storage directories
     * (`Movies`, `DCIM`, `Download`, `Pictures`, `Documents`) on `Dispatchers.IO` so newly added
     * or modified video files are indexed by Android's MediaStore before re-querying.
     */
    suspend fun triggerMediaScannerOnPrimaryStorage(): Int = withContext(Dispatchers.IO) {
        val targetDirs = listOf(
            Environment.DIRECTORY_MOVIES,
            Environment.DIRECTORY_DCIM,
            Environment.DIRECTORY_DOWNLOADS,
            Environment.DIRECTORY_PICTURES,
            Environment.DIRECTORY_DOCUMENTS
        ).mapNotNull { type ->
            runCatching { Environment.getExternalStoragePublicDirectory(type) }.getOrNull()
        }.filter { it.exists() }

        val pathsToScan = linkedSetOf<String>()
        for (dir in targetDirs) {
            pathsToScan.add(dir.absolutePath)
            runCatching {
                dir.walkTopDown().maxDepth(2).forEach { child ->
                    if (child.isFile && isVideoFileName(child.name)) {
                        pathsToScan.add(child.absolutePath)
                    } else if (child.isDirectory && !child.name.startsWith(".")) {
                        pathsToScan.add(child.absolutePath)
                    }
                }
            }
        }

        if (pathsToScan.isEmpty()) {
            runCatching {
                Environment.getExternalStorageDirectory()?.absolutePath?.let { pathsToScan.add(it) }
            }
        }

        val pathArray = pathsToScan.toTypedArray()
        if (pathArray.isEmpty()) return@withContext 0

        withTimeoutOrNull(1800L) {
            suspendCancellableCoroutine { continuation ->
                val remaining = AtomicInteger(pathArray.size)
                try {
                    MediaScannerConnection.scanFile(
                        context,
                        pathArray,
                        null
                    ) { _, _ ->
                        if (remaining.decrementAndGet() <= 0 && continuation.isActive) {
                            continuation.resume(pathArray.size)
                        }
                    }
                } catch (_: Exception) {
                    if (continuation.isActive) {
                        continuation.resume(0)
                    }
                }
            }
        } ?: pathArray.size
    }

    private fun isVideoFileName(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase(Locale.US)
        return ext in setOf("mp4", "mkv", "webm", "avi", "mov", "m4v", "ts", "3gp", "flv")
    }

    /**
     * Pull-to-refresh helper: invokes `MediaScannerConnection.scanFile()` on primary storage
     * directories and then re-queries `scanDeviceVideos()` on `Dispatchers.IO`.
     */
    suspend fun scanMediaAndRefreshLibrary(
        sortField: SortField = SortField.TITLE,
        sortAscending: Boolean = true,
        searchQuery: String = "",
        showHiddenFolders: Boolean = false,
        cachedDurationsMs: Map<Long, Long> = emptyMap(),
        scanDotFolders: Boolean = false,
        recognizeNoMedia: Boolean = true,
        markerFiles: Set<String> = setOf(".nomedia"),
        minVideoDurationSec: Int = 0,
        minFileSizeMb: Int = 0,
        blacklistedFolders: Set<String> = emptySet(),
        whitelistedFolders: Set<String> = emptySet(),
        whitelistEnabled: Boolean = false,
        watchedVideoIds: Set<Long> = emptySet(),
        seenBaselineEpochSec: Long = 0L
    ): List<VideoItem> = withContext(Dispatchers.IO) {
        triggerMediaScannerOnPrimaryStorage()
        scanDeviceVideos(
            sortField = sortField,
            sortAscending = sortAscending,
            searchQuery = searchQuery,
            showHiddenFolders = showHiddenFolders,
            cachedDurationsMs = cachedDurationsMs,
            scanDotFolders = scanDotFolders,
            recognizeNoMedia = recognizeNoMedia,
            markerFiles = markerFiles,
            minVideoDurationSec = minVideoDurationSec,
            minFileSizeMb = minFileSizeMb,
            blacklistedFolders = blacklistedFolders,
            whitelistedFolders = whitelistedFolders,
            whitelistEnabled = whitelistEnabled,
            watchedVideoIds = watchedVideoIds,
            seenBaselineEpochSec = seenBaselineEpochSec
        )
    }

    /**
     * Queries MediaStore.Video.Media to fetch all local device videos
     * including RELATIVE_PATH (Android 10+) and DATA path for hierarchical Tree View.
     * Uses cachedDurationsMs for items where MediaStore reports <= 0L duration.
     */
    @Suppress("DEPRECATION")
    suspend fun scanDeviceVideos(
        sortField: SortField = SortField.TITLE,
        sortAscending: Boolean = true,
        searchQuery: String = "",
        showHiddenFolders: Boolean = false,
        cachedDurationsMs: Map<Long, Long> = emptyMap(),
        scanDotFolders: Boolean = false,
        recognizeNoMedia: Boolean = true,
        markerFiles: Set<String> = setOf(".nomedia"),
        minVideoDurationSec: Int = 0,
        minFileSizeMb: Int = 0,
        blacklistedFolders: Set<String> = emptySet(),
        whitelistedFolders: Set<String> = emptySet(),
        whitelistEnabled: Boolean = false,
        watchedVideoIds: Set<Long> = emptySet(),
        seenBaselineEpochSec: Long = 0L
    ): List<VideoItem> = withContext(Dispatchers.IO) {
        val videoList = mutableListOf<VideoItem>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projectionList = mutableListOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projectionList.add(MediaStore.Video.Media.RELATIVE_PATH)
            projectionList.add(MediaStore.Video.Media.ORIENTATION)
            projectionList.add(MediaStore.Video.Media.VOLUME_NAME)
        }

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        val lockedVaultPaths = PrivateVaultManager(context).getLockedOriginalPaths()
        val mountedVolumes = discoverMountedStorageVolumes(context)

        try {
            context.contentResolver.query(
                collection,
                projectionList.toTypedArray(),
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                val displayNameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val dateModifiedCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                val dataCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                val relPathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH)
                } else {
                    -1
                }
                val orientationCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Video.Media.ORIENTATION)
                } else {
                    -1
                }
                val volumeNameCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Video.Media.VOLUME_NAME)
                } else {
                    -1
                }

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val rawTitle = cursor.getString(titleCol) ?: "Untitled Video"
                    val displayName = cursor.getString(displayNameCol) ?: "$rawTitle.mp4"
                    val mediaStoreDuration = cursor.getLong(durationCol)
                    val duration = when {
                        mediaStoreDuration > 0L -> mediaStoreDuration
                        (cachedDurationsMs[id] ?: 0L) > 0L -> cachedDurationsMs[id] ?: 0L
                        else -> 0L
                    }
                    val size = cursor.getLong(sizeCol).coerceAtLeast(0L)
                    val width = cursor.getInt(widthCol).coerceAtLeast(0)
                    val height = cursor.getInt(heightCol).coerceAtLeast(0)
                    val bucket = cursor.getString(bucketCol)?.takeIf { it.isNotBlank() } ?: "Others"
                    val dateAdded = cursor.getLong(dateCol)
                    val dateModified = if (dateModifiedCol >= 0) cursor.getLong(dateModifiedCol) else dateAdded
                    val rotationDegrees = if (orientationCol >= 0) cursor.getInt(orientationCol) else 0
                    val mediaVolumeName = if (volumeNameCol >= 0) cursor.getString(volumeNameCol) else null
                    val mimeType = cursor.getString(mimeCol) ?: "video/mp4"
                    val dataPath = if (dataCol >= 0) cursor.getString(dataCol) else null
                    val rawRelPath = if (relPathCol >= 0) cursor.getString(relPathCol) else null

                    if (!dataPath.isNullOrBlank() &&
                        (dataPath in lockedVaultPaths ||
                            dataPath.contains("/.private/") ||
                            dataPath.endsWith(".private", ignoreCase = true) ||
                            dataPath.endsWith(".enc", ignoreCase = true))
                    ) {
                        continue
                    }

                    val normalizedPath = DirectoryTreeBuilder.normalizeDirectoryPath(
                        relativePath = rawRelPath,
                        dataPath = dataPath,
                        fallbackBucket = bucket
                    )

                    if (!showHiddenFolders && !scanDotFolders && normalizedPath.split('/').any { it.startsWith(".") }) {
                        continue
                    }

                    if (!showHiddenFolders && recognizeNoMedia && !dataPath.isNullOrBlank() && markerFiles.isNotEmpty()) {
                        val parentDir = File(dataPath).parentFile
                        if (parentDir != null && markerFiles.any { marker -> File(parentDir, marker).exists() }) {
                            continue
                        }
                    }

                    if (minVideoDurationSec > 0 && duration > 0L && duration < minVideoDurationSec * 1000L) {
                        continue
                    }

                    if (minFileSizeMb > 0 && size < minFileSizeMb * 1024L * 1024L) {
                        continue
                    }

                    val normLower = normalizedPath.trim('/').lowercase(Locale.US)
                    if (blacklistedFolders.any { bl ->
                            val bLower = bl.trim('/').lowercase(Locale.US)
                            bLower.isNotEmpty() && (normLower == bLower || normLower.startsWith("$bLower/"))
                        }
                    ) {
                        continue
                    }

                    if (whitelistEnabled && whitelistedFolders.isNotEmpty()) {
                        val allowed = whitelistedFolders.any { wl ->
                            val wLower = wl.trim('/').lowercase(Locale.US)
                            wLower.isNotEmpty() && (normLower == wLower || normLower.startsWith("$wLower/"))
                        }
                        if (!allowed) continue
                    }

                    val resolvedWidth = width
                    val resolvedHeight = height
                    val is4k = maxOf(resolvedWidth, resolvedHeight) >= 3840 ||
                        rawTitle.contains("4K", ignoreCase = true) ||
                        displayName.contains("2160p", ignoreCase = true)
                    val fps = when {
                        rawTitle.contains("23.98") || displayName.contains("23.98") -> 23.98f
                        rawTitle.contains("60") || displayName.contains("60fps") -> 60f
                        is4k -> 24f
                        else -> 23.98f
                    }
                    val codec = when {
                        displayName.contains("AV1", ignoreCase = true) -> "AV1 Main (HW+)"
                        is4k || displayName.contains("x265", ignoreCase = true) || displayName.contains("HEVC", ignoreCase = true) ->
                            "HEVC 10-bit (HW+)"
                        else -> "H.264 / AVC (HW+)"
                    }

                    // Detect sidecar .srt/.vtt/.ass file or embedded subtitle stream marker
                    val sidecarSrtExists = if (!dataPath.isNullOrBlank()) {
                        val base = dataPath.substringBeforeLast('.')
                        File("$base.srt").exists() ||
                            File("$base.vtt").exists() ||
                            File("$base.ass").exists() ||
                            File("$base.sub").exists()
                    } else {
                        false
                    }
                    val hasSrt = sidecarSrtExists ||
                        displayName.contains("BluRay", ignoreCase = true) ||
                        displayName.contains("HDR", ignoreCase = true) ||
                        displayName.contains("WEB-DL", ignoreCase = true) ||
                        displayName.contains("HEVC", ignoreCase = true)

                    val latestTimestampSec = maxOf(
                        normalizeEpochSeconds(dateAdded),
                        normalizeEpochSeconds(dateModified)
                    )
                    val isNewVideo = (id !in watchedVideoIds) &&
                        (seenBaselineEpochSec <= 0L || latestTimestampSec > seenBaselineEpochSec)

                    val contentUri = ContentUris.withAppendedId(collection, id)
                    val volumeId = resolveStorageVolumeId(dataPath, mediaVolumeName, mountedVolumes)
                    if (size > 0L) {
                        videoList.add(
                            VideoItem(
                                id = id,
                                uri = contentUri,
                                title = rawTitle,
                                displayName = displayName,
                                durationMs = duration,
                                sizeBytes = size,
                                width = resolvedWidth,
                                height = resolvedHeight,
                                folderName = bucket,
                                relativePath = normalizedPath,
                                frameRate = fps,
                                videoCodec = codec,
                                hasSubtitles = hasSrt,
                                subtitleInfo = if (hasSrt) "English [SRT]" else "None",
                                dateAddedSec = dateAdded,
                                mimeType = mimeType,
                                rotationDegrees = rotationDegrees,
                                dateModifiedSec = dateModified,
                                isNew = isNewVideo,
                                storageVolumeId = volumeId,
                                filePath = dataPath
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        filterAndSortVideos(videoList, sortField, sortAscending, searchQuery)
    }

    /**
     * Asynchronously extracts exact durations (and dimensions if missing) using MediaMetadataRetriever
     * on Dispatchers.IO for any videos where MediaStore.Video.Media.DURATION returned <= 0L,
     * and invokes onResolved with newly extracted (id -> durationMs) pairs for DataStore caching.
     */
    suspend fun resolveMissingDurations(
        videos: List<VideoItem>,
        onResolved: suspend (Map<Long, Long>) -> Unit
    ): List<VideoItem> = withContext(Dispatchers.IO) {
        val missing = videos.filter { it.durationMs <= 0L || it.width <= 0 || it.height <= 0 }
        if (missing.isEmpty()) return@withContext videos

        val extractedDurations = mutableMapOf<Long, Long>()
        val updatedVideos = videos.map { item ->
            if (item.durationMs > 0L && item.width > 0 && item.height > 0) {
                item
            } else {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, item.uri)
                    val durMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull() ?: 0L
                    val retW = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                        ?.toIntOrNull() ?: item.width
                    val retH = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                        ?.toIntOrNull() ?: item.height
                    val retRot = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                        ?.toIntOrNull() ?: item.rotationDegrees
                    if (durMs > 0L && item.durationMs <= 0L) {
                        extractedDurations[item.id] = durMs
                    }
                    item.copy(
                        durationMs = if (durMs > 0L) durMs else item.durationMs,
                        width = if (retW > 0) retW else item.width,
                        height = if (retH > 0) retH else item.height,
                        rotationDegrees = retRot
                    )
                } catch (_: Exception) {
                    item
                } finally {
                    runCatching { retriever.release() }
                }
            }
        }

        if (extractedDurations.isNotEmpty()) {
            onResolved(extractedDurations)
        }
        updatedVideos
    }

    /**
     * Ensures valid video width, height, and rotationDegrees before launching VideoPlayerScreen
     * so portrait videos never flash horizontally while Media3 initializes.
     */
    suspend fun resolveDimensionsBeforePlayback(video: VideoItem): VideoItem = withContext(Dispatchers.IO) {
        val needsLookup = video.width <= 0 || video.height <= 0 ||
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            video.isVaultItem
        if (!needsLookup) return@withContext video

        val retriever = MediaMetadataRetriever()
        try {
            if (video.isVaultItem && !video.vaultFilePath.isNullOrBlank()) {
                retriever.setDataSource(video.vaultFilePath)
            } else {
                retriever.setDataSource(context, video.uri)
            }
            val retW = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull() ?: video.width
            val retH = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull() ?: video.height
            val retRot = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                ?.toIntOrNull() ?: video.rotationDegrees
            val durMs = if (video.durationMs <= 0L) {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: video.durationMs
            } else {
                video.durationMs
            }
            video.copy(
                durationMs = durMs,
                width = if (retW > 0) retW else video.width,
                height = if (retH > 0) retH else video.height,
                rotationDegrees = retRot
            )
        } catch (_: Exception) {
            video
        } finally {
            runCatching { retriever.release() }
        }
    }

    suspend fun renameVideo(video: VideoItem, newBaseName: String): RenameVideoResult = withContext(Dispatchers.IO) {
        val cleaned = newBaseName.trim().replace("/", "_")
        if (cleaned.isEmpty()) {
            return@withContext RenameVideoResult.Error("Filename cannot be empty.")
        }
        val ext = video.displayName.substringAfterLast('.', "mp4")
        val targetDisplayName = if (cleaned.endsWith(".$ext", ignoreCase = true)) {
            cleaned
        } else {
            "$cleaned.$ext"
        }

        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, targetDisplayName)
            put(MediaStore.Video.Media.TITLE, cleaned.substringBeforeLast('.'))
        }

        return@withContext try {
            val updated = context.contentResolver.update(video.uri, values, null, null)
            if (updated > 0) {
                RenameVideoResult.Success
            } else {
                RenameVideoResult.Error("Unable to rename video.")
            }
        } catch (_: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val pi: PendingIntent = MediaStore.createWriteRequest(
                    context.contentResolver,
                    listOf(video.uri)
                )
                RenameVideoResult.RequiresWritePermission(pi.intentSender)
            } else {
                RenameVideoResult.Error("Permission denied to rename this file.")
            }
        } catch (e: Exception) {
            RenameVideoResult.Error(e.localizedMessage ?: "Rename failed.")
        }
    }

    /**
     * Copies selected videos to targetRelativePath (e.g. "Movies/Action" or "Pictures").
     */
    suspend fun copyVideosToFolder(
        videos: List<VideoItem>,
        targetRelativePath: String
    ): BatchFileOperationResult = withContext(Dispatchers.IO) {
        val cleanTarget = targetRelativePath.trim().trim('/')
        if (cleanTarget.isEmpty()) {
            return@withContext BatchFileOperationResult.Error("Select a destination folder.")
        }
        var copiedCount = 0
        for (video in videos) {
            val inserted = insertVideoCopyToMediaStore(video, cleanTarget)
            if (inserted != null) {
                copiedCount++
            }
        }
        if (copiedCount == 0) {
            BatchFileOperationResult.Error("Failed to copy selected video(s).")
        } else {
            BatchFileOperationResult.Completed(copiedCount)
        }
    }

    /**
     * Moves selected videos to targetRelativePath: copies into destination MediaStore folder,
     * then deletes source originals directly or collects a batch MediaStore.createDeleteRequest.
     */
    suspend fun moveVideosToFolder(
        videos: List<VideoItem>,
        targetRelativePath: String
    ): BatchFileOperationResult = withContext(Dispatchers.IO) {
        val cleanTarget = targetRelativePath.trim().trim('/')
        if (cleanTarget.isEmpty()) {
            return@withContext BatchFileOperationResult.Error("Select a destination folder.")
        }
        var copiedCount = 0
        val pendingDeleteUris = mutableListOf<Uri>()

        for (video in videos) {
            if (video.relativePath.trim('/') == cleanTarget) continue
            val inserted = insertVideoCopyToMediaStore(video, cleanTarget)
            if (inserted != null) {
                copiedCount++
                var deleted = false
                try {
                    deleted = context.contentResolver.delete(video.uri, null, null) > 0
                } catch (_: SecurityException) {
                }
                if (!deleted) {
                    pendingDeleteUris.add(video.uri)
                }
            }
        }

        if (copiedCount == 0) {
            return@withContext BatchFileOperationResult.Error("No videos could be moved to $cleanTarget.")
        }

        if (pendingDeleteUris.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val pi: PendingIntent = MediaStore.createDeleteRequest(
                context.contentResolver,
                pendingDeleteUris
            )
            return@withContext BatchFileOperationResult.RequiresDeleteConfirmation(
                processedCount = copiedCount,
                deleteIntentSender = pi.intentSender
            )
        }

        BatchFileOperationResult.Completed(copiedCount)
    }

    private fun insertVideoCopyToMediaStore(video: VideoItem, cleanRelativePath: String): Uri? {
        return try {
            val resolver = context.contentResolver
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

            val baseName = video.displayName.substringBeforeLast('.')
            val ext = video.displayName.substringAfterLast('.', "mp4")
            val uniqueDisplayName = "$baseName.${ext}"

            val values = ContentValues().apply {
                put(MediaStore.Video.Media.TITLE, baseName)
                put(MediaStore.Video.Media.DISPLAY_NAME, uniqueDisplayName)
                put(MediaStore.Video.Media.MIME_TYPE, video.mimeType.ifBlank { "video/mp4" })
                if (video.durationMs > 0L) put(MediaStore.Video.Media.DURATION, video.durationMs)
                if (video.width > 0) put(MediaStore.Video.Media.WIDTH, video.width)
                if (video.height > 0) put(MediaStore.Video.Media.HEIGHT, video.height)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, cleanRelativePath)
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }

            val destUri = resolver.insert(collection, values) ?: return null
            val input = resolver.openInputStream(video.uri) ?: run {
                runCatching { resolver.delete(destUri, null, null) }
                return null
            }
            input.use { inStream ->
                resolver.openOutputStream(destUri)?.use { outStream ->
                    inStream.copyTo(outStream, bufferSize = 64 * 1024)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val publishValues = ContentValues().apply {
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                }
                resolver.update(destUri, publishValues, null, null)
            }
            destUri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Collects known relative directory paths under /storage/emulated/0 for SelectFolderDialog,
     * combining standard top-level Android storage folders, paths from scanned videos,
     * accessible physical directories, and user-created folders.
     */
    fun collectAvailableStorageDirectories(
        allVideos: List<VideoItem>,
        extraCreatedFolders: Set<String>
    ): List<String> {
        val paths = linkedSetOf(
            "Android",
            "DCIM",
            "DCIM/Camera",
            "Documents",
            "Download",
            "Movies",
            "Music",
            "Pictures"
        )
        allVideos.forEach { video ->
            val rel = video.relativePath.trim('/')
            if (rel.isNotEmpty()) {
                paths.add(rel)
            }
        }
        extraCreatedFolders.forEach { folder ->
            val rel = folder.trim('/')
            if (rel.isNotEmpty()) {
                paths.add(rel)
            }
        }
        runCatching {
            val root = Environment.getExternalStorageDirectory()
            root?.listFiles()?.forEach { child ->
                if (child.isDirectory && !child.name.startsWith(".")) {
                    paths.add(child.name)
                }
            }
        }
        val expanded = linkedSetOf<String>()
        for (p in paths) {
            val segments = p.split('/').filter { it.isNotBlank() }
            val sb = StringBuilder()
            for (seg in segments) {
                if (sb.isNotEmpty()) sb.append('/')
                sb.append(seg)
                expanded.add(sb.toString())
            }
        }
        return expanded.sortedBy { it.lowercase(Locale.US) }
    }

    fun filterAndSortVideos(
        videos: List<VideoItem>,
        sortOption: VideoSortOption,
        searchQuery: String
    ): List<VideoItem> {
        val sortField = when (sortOption) {
            VideoSortOption.DATE -> SortField.DATE
            VideoSortOption.NAME -> SortField.TITLE
            VideoSortOption.SIZE -> SortField.SIZE
        }
        val ascending = sortOption == VideoSortOption.NAME
        return filterAndSortVideos(videos, sortField, ascending, searchQuery)
    }

    fun filterAndSortVideos(
        videos: List<VideoItem>,
        sortField: SortField,
        sortAscending: Boolean,
        searchQuery: String
    ): List<VideoItem> {
        val filtered = if (searchQuery.isBlank()) {
            videos
        } else {
            val q = searchQuery.trim().lowercase(Locale.US)
            videos.filter {
                it.title.lowercase(Locale.US).contains(q) ||
                    it.displayName.lowercase(Locale.US).contains(q) ||
                    it.folderName.lowercase(Locale.US).contains(q) ||
                    it.relativePath.lowercase(Locale.US).contains(q) ||
                    it.resolutionTag.lowercase(Locale.US).contains(q)
            }
        }

        val comparator = when (sortField) {
            SortField.TITLE -> compareBy<VideoItem> { it.displayName.lowercase(Locale.US) }
            SortField.DATE -> compareBy { it.dateAddedSec }
            SortField.PLAYED_TIME -> compareBy { it.dateAddedSec }
            SortField.STATUS -> compareBy { it.hasSubtitles }
            SortField.LENGTH -> compareBy { it.durationMs }
            SortField.SIZE -> compareBy { it.sizeBytes }
            SortField.RESOLUTION -> compareBy { it.width.toLong() * it.height.toLong() }
            SortField.PATH -> compareBy { it.relativePath.lowercase(Locale.US) }
            SortField.FRAME_RATE -> compareBy { it.frameRate }
            SortField.TYPE -> compareBy { it.extensionLabel }
        }
        return if (sortAscending) filtered.sortedWith(comparator) else filtered.sortedWith(comparator.reversed())
    }

    fun groupVideosByFolder(
        videos: List<VideoItem>,
        sortField: SortField = SortField.TITLE,
        sortAscending: Boolean = true
    ): List<VideoFolder> {
        val grouped = videos.groupBy { it.relativePath }
        val folders = grouped.map { (relPath, items) ->
            val leafName = relPath.substringAfterLast('/', items.firstOrNull()?.folderName ?: "Others")
            VideoFolder(
                name = leafName,
                path = relPath,
                videos = items
            )
        }

        val comparator = when (sortField) {
            SortField.PATH -> compareBy<VideoFolder> { it.path.lowercase(Locale.US) }
            SortField.DATE, SortField.PLAYED_TIME -> compareBy { f -> f.videos.maxOfOrNull { it.dateAddedSec } ?: 0L }
            SortField.LENGTH -> compareBy { it.totalDurationMs }
            SortField.SIZE -> compareBy { it.totalSizeBytes }
            else -> compareBy { it.name.lowercase(Locale.US) }
        }
        return if (sortAscending) folders.sortedWith(comparator) else folders.sortedWith(comparator.reversed())
    }

    /**
     * Copies bundled H.264/AAC MP4 sample videos from res/raw/ into hierarchical MediaStore paths
     * (Movies, Movies/Cinema 4K, Pictures/Cox's Bazar Tour 2024, Pictures/Screenshots, DCIM/Camera)
     * so Tree View immediately showcases multi-level root folders, subfolders, and direct videos.
     */
    suspend fun generateSampleVideosInMediaStore(): Int = withContext(Dispatchers.IO) {
        val existingNames = scanDeviceVideos().map { it.displayName }.toSet()

        val sampleSpecs = listOf(
            SampleVideoSpec(
                rawResId = R.raw.sample_cyberpunk_4k,
                title = "Cyberpunk.Neon.Horizon.2026.2160p.UHD.BluRay.x265.10bit.HDR",
                displayName = "Cyberpunk.Neon.Horizon.2026.2160p.UHD.BluRay.x265.10bit.HDR.mp4",
                relativeDir = "${Environment.DIRECTORY_MOVIES}/Cinema 4K",
                width = 3840,
                height = 2160,
                durationSec = 8
            ),
            SampleVideoSpec(
                rawResId = R.raw.sample_obsidian_reel,
                title = "Open.Source.Cinema.Reel.2160p.WEB-DL.AV1.HDR10",
                displayName = "Open.Source.Cinema.Reel.2160p.WEB-DL.AV1.HDR10.mp4",
                relativeDir = Environment.DIRECTORY_MOVIES,
                width = 3840,
                height = 2160,
                durationSec = 6
            ),
            SampleVideoSpec(
                rawResId = R.raw.sample_alpine_glacier,
                title = "Coxs.Bazar.Sunset.Drone.1080p.BluRay.x264.23.98fps",
                displayName = "Coxs.Bazar.Sunset.Drone.1080p.BluRay.x264.23.98fps.mp4",
                relativeDir = "${Environment.DIRECTORY_PICTURES}/Cox's Bazar Tour 2024",
                width = 1920,
                height = 1080,
                durationSec = 7
            ),
            SampleVideoSpec(
                rawResId = R.raw.sample_aurora_borealis,
                title = "ScreenRecording.UI.Walkthrough.1080p.HEVC.10bit",
                displayName = "ScreenRecording.UI.Walkthrough.1080p.HEVC.10bit.mp4",
                relativeDir = "${Environment.DIRECTORY_PICTURES}/Screenshots",
                width = 1920,
                height = 1080,
                durationSec = 6
            ),
            SampleVideoSpec(
                rawResId = R.raw.sample_studio_acoustic,
                title = "VID_20261115_Studio_Session_1080p_60fps",
                displayName = "VID_20261115_Studio_Session_1080p_60fps.mp4",
                relativeDir = "${Environment.DIRECTORY_DCIM}/Camera",
                width = 1920,
                height = 1080,
                durationSec = 5
            )
        )

        var createdCount = 0
        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        for (spec in sampleSpecs) {
            if (spec.displayName in existingNames) continue
            try {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.TITLE, spec.title)
                    put(MediaStore.Video.Media.DISPLAY_NAME, spec.displayName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.DURATION, spec.durationSec * 1000L)
                    put(MediaStore.Video.Media.WIDTH, spec.width)
                    put(MediaStore.Video.Media.HEIGHT, spec.height)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Video.Media.RELATIVE_PATH, spec.relativeDir)
                        put(MediaStore.Video.Media.IS_PENDING, 1)
                    }
                }

                val insertedUri = resolver.insert(collection, values)
                if (insertedUri != null) {
                    resolver.openOutputStream(insertedUri)?.use { out ->
                        context.resources.openRawResource(spec.rawResId).use { input ->
                            input.copyTo(out, bufferSize = 64 * 1024)
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val publishValues = ContentValues().apply {
                            put(MediaStore.Video.Media.IS_PENDING, 0)
                            put(MediaStore.Video.Media.WIDTH, spec.width)
                            put(MediaStore.Video.Media.HEIGHT, spec.height)
                        }
                        resolver.update(insertedUri, publishValues, null, null)
                    }
                    createdCount++
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        createdCount
    }

    private data class SampleVideoSpec(
        @RawRes val rawResId: Int,
        val title: String,
        val displayName: String,
        val relativeDir: String,
        val width: Int,
        val height: Int,
        val durationSec: Int
    )
}
