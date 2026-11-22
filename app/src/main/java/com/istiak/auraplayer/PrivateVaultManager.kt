package com.istiak.auraplayer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

enum class AppDisguiseMode(val label: String, val aliasSuffix: String?) {
    DEFAULT("Default (Aura)", null),
    CALCULATOR("Calculator", ".AliasCalculator"),
    NOTES("Notes", ".AliasNotes");

    companion object {
        fun fromStored(value: String?): AppDisguiseMode {
            return entries.firstOrNull { it.name == value } ?: DEFAULT
        }
    }
}

/**
 * Represents a single mapping line in `/storage/emulated/0/AuraPlayer/.private/vault.kv`
 * formatted as `<private_path>><original_path>`:
 * `/storage/emulated/0/AuraPlayer/.private/86023BFA3A99145B0DAF51D03D007712.private>/storage/emulated/0/Movies/SampleVideo.mp4`
 */
data class VaultEntry(
    val privateFilePath: String,
    val originalAbsolutePath: String,
    val hexId: String = File(privateFilePath).nameWithoutExtension
) {
    val privateFile: File get() = File(privateFilePath)
    val originalFile: File get() = File(originalAbsolutePath)
    val originalFileName: String get() = originalFile.name.ifBlank { "Video.mp4" }
    val originalTitle: String get() = originalFileName.substringBeforeLast('.', originalFileName)
    val originalParentFolder: String
        get() = originalFile.parentFile?.name?.takeIf { it.isNotBlank() } ?: "Movies"
    val sizeBytes: Long get() = privateFile.length()

    val stableId: Long
        get() = abs(privateFilePath.hashCode().toLong()).coerceAtLeast(1L)

    fun toKvLine(): String = "${privateFilePath}>${originalAbsolutePath}"
}

sealed class MoveToVaultResult {
    data class Completed(val vaultItem: VideoItem) : MoveToVaultResult()
    data class RequiresAllFilesAccess(val settingsIntent: Intent) : MoveToVaultResult()
    data class Error(val message: String) : MoveToVaultResult()
}

sealed class BatchMoveToVaultResult {
    data class Completed(val movedCount: Int, val movedVideoIds: Set<Long> = emptySet()) : BatchMoveToVaultResult()
    data class RequiresAllFilesAccess(val settingsIntent: Intent) : BatchMoveToVaultResult()
    data class Error(val message: String) : BatchMoveToVaultResult()
}

/**
 * Instant Zero-Copy Private Vault Manager (`PrivateVaultManager.kt`):
 *
 * 1. **Vault Directory Structure (`/storage/emulated/0/AuraPlayer/.private/`):**
 *    - Root vault directory resolved via `File(Environment.getExternalStorageDirectory(), "AuraPlayer/.private")`.
 *    - Automatically creates the directory and places an empty `.nomedia` file inside
 *      (`File(vaultDir, ".nomedia").createNewFile()`) so Android's `MediaScanner` and Gallery apps ignore the folder.
 *    - Mapping index file inside the same folder: `/storage/emulated/0/AuraPlayer/.private/vault.kv`.
 *
 * 2. **Exact `.kv` Mapping Format (`<private_path>><original_path>`):**
 *    - Stores one mapping per line separated by `>`:
 *      `/storage/emulated/0/AuraPlayer/.private/86023BFA3A99145B0DAF51D03D007712.private>/storage/emulated/0/Movies/SampleVideo.mp4`
 *    - Generates `<32-CHAR-UPPERCASE-HEX-ID>` via `UUID.randomUUID().toString().replace("-", "").uppercase()`
 *      and appends `.private` as the filename extension.
 *
 * 3. **Instant Zero-Copy Lock & Restore (`File.renameTo()`):**
 *    - Relocates files across `/storage/emulated/0/` in under 10 milliseconds by updating the filesystem
 *      inode table directly via `sourceFile.renameTo(destFile)` (with `Files.move(..., REPLACE_EXISTING)` fallback)
 *      with zero byte-copying, zero encryption overhead, and zero `MediaStore.createDeleteRequest()` popups.
 */
class PrivateVaultManager(private val context: Context) {

    /**
     * Root vault directory: `/storage/emulated/0/AuraPlayer/.private/`
     * via `File(Environment.getExternalStorageDirectory(), "AuraPlayer/.private")`.
     */
    val vaultDir: File
        get() {
            val externalRoot = runCatching { Environment.getExternalStorageDirectory() }.getOrNull()
                ?: context.filesDir
            return File(externalRoot, "AuraPlayer/.private").apply {
                if (!exists()) {
                    mkdirs()
                }
                val noMedia = File(this, ".nomedia")
                if (!noMedia.exists()) {
                    try {
                        noMedia.createNewFile()
                    } catch (_: Exception) {
                    }
                }
            }
        }

    /**
     * Mapping index file inside the vault directory:
     * `/storage/emulated/0/AuraPlayer/.private/vault.kv`
     */
    val vaultKvFile: File
        get() = File(vaultDir, "vault.kv")

    /**
     * Isolated internal thumbnail cache directory (`File(context.filesDir, "vault_thumbs")`)
     * for locked vault videos so private thumbnails never touch shared/public image caches.
     */
    val vaultThumbsDir: File
        get() = File(context.filesDir, "vault_thumbs").apply {
            if (!exists()) {
                mkdirs()
            }
            val noMedia = File(this, ".nomedia")
            if (!noMedia.exists()) {
                try {
                    noMedia.createNewFile()
                } catch (_: Exception) {
                }
            }
        }

    /**
     * Auxiliary sidecar cache (`context.filesDir/vault_meta_cache.json`) for fast duration/dimension lookup
     * so `VaultScreen` does not need to re-run `MediaMetadataRetriever` on every frame.
     */
    private val metadataCacheFile: File
        get() = File(context.filesDir, "vault_meta_cache.json")

    /**
     * Generates a `<32-CHAR-UPPERCASE-HEX-ID>` using
     * `UUID.randomUUID().toString().replace("-", "").uppercase()`.
     */
    fun generateUpperHexId(): String {
        return UUID.randomUUID().toString().replace("-", "").uppercase()
    }

    /**
     * Resolves the original filesystem path (e.g. `/storage/emulated/0/Movies/SampleVideo.mp4`)
     * for a MediaStore or file URI so it can be relocated and restored to its exact folder.
     */
    @Suppress("DEPRECATION")
    fun resolveOriginalAbsolutePath(video: VideoItem): String {
        if (video.uri.scheme.equals("file", ignoreCase = true)) {
            video.uri.path?.let { if (it.isNotBlank()) return it }
        }
        runCatching {
            val projection = arrayOf(MediaStore.MediaColumns.DATA)
            context.contentResolver.query(video.uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val colIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                    if (colIdx >= 0) {
                        val dataPath = cursor.getString(colIdx)
                        if (!dataPath.isNullOrBlank()) {
                            return dataPath
                        }
                    }
                }
            }
        }
        if (video.relativePath.startsWith("/")) {
            val f = File(video.relativePath)
            if (f.name.contains('.')) return f.absolutePath
        }
        val rootPath = Environment.getExternalStorageDirectory()?.absolutePath ?: "/storage/emulated/0"
        val cleanRel = video.relativePath.trim('/').ifBlank { Environment.DIRECTORY_MOVIES }
        val safeName = video.displayName.ifBlank { "${video.title.replace(" ", "_")}.mp4" }
        return "$rootPath/$cleanRel/$safeName"
    }

    fun hasAllFilesAccessPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching { Environment.isExternalStorageManager() }.getOrDefault(false)
        } else {
            true
        }
    }

    fun createManageAllFilesAccessIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    private fun isAppInternalPath(path: String): Boolean {
        val filesPath = context.filesDir.absolutePath
        val cachePath = context.cacheDir.absolutePath
        val extFilesPath = context.getExternalFilesDir(null)?.absolutePath
        return path.startsWith(filesPath) ||
            path.startsWith(cachePath) ||
            (extFilesPath != null && path.startsWith(extFilesPath))
    }

    /**
     * Parses `/storage/emulated/0/AuraPlayer/.private/vault.kv` into a list of [VaultEntry] items.
     * Each line follows `<private_path>><original_path>`.
     */
    fun readVaultKvEntries(): List<VaultEntry> {
        val kvFile = vaultKvFile
        if (!kvFile.exists()) return emptyList()
        return try {
            val entries = mutableListOf<VaultEntry>()
            val seenPrivatePaths = HashSet<String>()
            val seenOriginalPaths = HashSet<String>()
            kvFile.readLines().forEach { rawLine ->
                val line = rawLine.trim()
                if (line.isNotEmpty() && line.contains(">")) {
                    val separatorIdx = line.indexOf('>')
                    val privatePath = line.substring(0, separatorIdx).trim()
                    val originalPath = line.substring(separatorIdx + 1).trim()
                    if (
                        privatePath.isNotEmpty() &&
                        originalPath.isNotEmpty() &&
                        seenPrivatePaths.add(privatePath) &&
                        seenOriginalPaths.add(originalPath)
                    ) {
                        entries.add(
                            VaultEntry(
                                privateFilePath = privatePath,
                                originalAbsolutePath = originalPath
                            )
                        )
                    }
                }
            }
            entries
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Appends `"${privateFilePath}>${originalAbsolutePath}\n"` to `vault.kv`.
     */
    @Synchronized
    private fun appendToVaultKv(privateFilePath: String, originalAbsolutePath: String) {
        val kvFile = vaultKvFile
        kvFile.parentFile?.mkdirs()
        val existingEntries = readVaultKvEntries().filterNot {
            it.privateFilePath == privateFilePath || it.originalAbsolutePath == originalAbsolutePath
        }
        val updatedLines = buildString {
            existingEntries.forEach { entry ->
                append(entry.privateFilePath)
                append('>')
                append(entry.originalAbsolutePath)
                append('\n')
            }
            append(privateFilePath)
            append('>')
            append(originalAbsolutePath)
            append('\n')
        }
        kvFile.writeText(updatedLines)
    }

    @Synchronized
    private fun writeVaultKvEntries(entries: List<VaultEntry>) {
        val kvFile = vaultKvFile
        kvFile.parentFile?.mkdirs()
        if (entries.isEmpty()) {
            if (kvFile.exists()) {
                kvFile.writeText("")
            }
            return
        }
        val content = buildString {
            entries.forEach { entry ->
                append(entry.privateFilePath)
                append('>')
                append(entry.originalAbsolutePath)
                append('\n')
            }
        }
        kvFile.writeText(content)
    }

    fun getLockedOriginalPaths(): Set<String> {
        return readVaultKvEntries()
            .asSequence()
            .filter { it.privateFile.exists() }
            .map { it.originalAbsolutePath }
            .filter { it.isNotBlank() }
            .toSet()
    }

    /**
     * Instant zero-copy move using `sourceFile.renameTo(destFile)` with
     * `Files.move(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING)`
     * fallback if moving across different storage volumes.
     */
    private fun performInstantZeroCopyMove(sourceFile: File, destFile: File): Boolean {
        if (!sourceFile.exists()) return false
        destFile.parentFile?.mkdirs()
        val moved = sourceFile.renameTo(destFile)
        if (moved && destFile.exists()) {
            return true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nioMoved = runCatching {
                Files.move(
                    sourceFile.toPath(),
                    destFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
                true
            }.getOrDefault(false)
            if (nioMoved && destFile.exists()) {
                return true
            }
        }
        return false
    }

    @Suppress("DEPRECATION")
    private fun silentlyDeleteStaleMediaStoreEntry(originalAbsolutePath: String, videoUri: Uri? = null) {
        if (originalAbsolutePath.isNotBlank()) {
            runCatching {
                context.contentResolver.delete(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    "${MediaStore.Video.Media.DATA}=?",
                    arrayOf(originalAbsolutePath)
                )
            }
        }
        if (videoUri != null && videoUri.scheme.equals("content", ignoreCase = true)) {
            runCatching {
                context.contentResolver.delete(videoUri, null, null)
            }
        }
    }

    private fun ensureIsolatedThumbnail(hexId: String, videoFile: File): File? {
        val thumbFile = File(vaultThumbsDir, "vault_thumb_$hexId.jpg")
        if (thumbFile.exists() && thumbFile.length() > 0L) {
            return thumbFile
        }
        if (!videoFile.exists() || !videoFile.canRead()) return null
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(videoFile.absolutePath)
            val bitmap = retriever.getFrameAtTime(
                1_000_000L,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            ) ?: retriever.frameAtTime
            if (bitmap != null) {
                thumbFile.outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                bitmap.recycle()
                if (thumbFile.exists() && thumbFile.length() > 0L) thumbFile else null
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun deleteIsolatedThumbnail(hexId: String) {
        runCatching {
            val thumbFile = File(vaultThumbsDir, "vault_thumb_$hexId.jpg")
            if (thumbFile.exists()) {
                thumbFile.delete()
            }
        }
    }

    private fun extractOrResolveMetadata(
        entry: VaultEntry,
        cachedMap: MutableMap<String, CachedVaultMeta>
    ): CachedVaultMeta {
        cachedMap[entry.hexId]?.let { return it }

        var durationMs = 0L
        var width = 1920
        var height = 1080
        val privateFile = entry.privateFile
        if (privateFile.exists() && privateFile.length() > 0L) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(privateFile.absolutePath)
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
                val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    ?.toIntOrNull() ?: 0
                val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    ?.toIntOrNull() ?: 0
                if (w > 0 && h > 0) {
                    width = w
                    height = h
                }
            } catch (_: Exception) {
            } finally {
                runCatching { retriever.release() }
            }
        }

        val ext = entry.originalFileName.substringAfterLast('.', "mp4").lowercase(Locale.US)
        val mimeType = when (ext) {
            "mkv" -> "video/x-matroska"
            "webm" -> "video/webm"
            "avi" -> "video/x-msvideo"
            "mov" -> "video/quicktime"
            else -> "video/mp4"
        }
        val resolved = CachedVaultMeta(
            hexId = entry.hexId,
            durationMs = durationMs,
            width = width,
            height = height,
            mimeType = mimeType,
            hiddenAtSec = (privateFile.lastModified() / 1000L).coerceAtLeast(1L)
        )
        cachedMap[entry.hexId] = resolved
        return resolved
    }

    private fun buildVaultVideoItem(
        entry: VaultEntry,
        meta: CachedVaultMeta
    ): VideoItem {
        val privateFile = entry.privateFile
        val thumbFile = ensureIsolatedThumbnail(entry.hexId, privateFile)
        val originalFile = entry.originalFile
        val displayName = originalFile.name.ifBlank { "Video.mp4" }
        val title = displayName.substringBeforeLast('.', displayName)
        val folderLabel = entry.originalParentFolder

        return VideoItem(
            id = entry.stableId,
            uri = Uri.fromFile(privateFile),
            title = title,
            displayName = displayName,
            durationMs = meta.durationMs,
            sizeBytes = privateFile.length(),
            width = meta.width,
            height = meta.height,
            folderName = folderLabel,
            relativePath = entry.originalAbsolutePath,
            dateAddedSec = meta.hiddenAtSec,
            mimeType = meta.mimeType,
            isVaultItem = true,
            vaultFilePath = privateFile.absolutePath,
            vaultThumbnailPath = thumbFile?.absolutePath
        )
    }

    /**
     * Parses `/storage/emulated/0/AuraPlayer/.private/vault.kv` and returns all locked videos
     * with their original filename, original parent folder badge, isolated thumbnail, duration,
     * and direct `Uri.fromFile(File(entry.privateFilePath))` for instant 4K playback.
     */
    suspend fun getVaultVideos(): List<VideoItem> = withContext(Dispatchers.IO) {
        val kvEntries = readVaultKvEntries()
        if (kvEntries.isEmpty()) return@withContext emptyList()

        val cachedMap = readCachedMetaMap().toMutableMap()
        val initialCacheSize = cachedMap.size
        val validEntries = mutableListOf<VaultEntry>()
        val resultItems = mutableListOf<VideoItem>()
        var pruned = false

        for (entry in kvEntries) {
            val pFile = entry.privateFile
            if (pFile.exists() && pFile.length() > 0L) {
                validEntries.add(entry)
                val meta = extractOrResolveMetadata(entry, cachedMap)
                resultItems.add(buildVaultVideoItem(entry, meta))
            } else {
                pruned = true
            }
        }

        if (pruned) {
            writeVaultKvEntries(validEntries)
        }
        if (cachedMap.size != initialCacheSize) {
            writeCachedMetaMap(cachedMap)
        }

        resultItems.asReversed()
    }

    /**
     * Instant Lock Operation (`lockVideo(video: VideoItem)`):
     * 1. Requires `Environment.isExternalStorageManager()` (`MANAGE_EXTERNAL_STORAGE`). If not granted,
     *    opens `Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION` for `package:com.istiak.auraplayer`.
     * 2. Resolves `File(originalAbsolutePath)`, verifying it exists and is not already mapped in `vault.kv`.
     * 3. Creates destination `File(vaultDir, "${hexId}.private")` using a 32-char uppercase hex UUID.
     * 4. Instant Zero-Copy Move via `sourceFile.renameTo(destFile)` (with `Files.move` fallback).
     * 5. Appends `"${destFile.absolutePath}>${sourceFile.absolutePath}\n"` to `vault.kv`.
     * 6. Removes stale public entry silently from `MediaStore` via `contentResolver.delete(...)`.
     */
    suspend fun lockVideo(video: VideoItem): MoveToVaultResult = withContext(Dispatchers.IO) {
        try {
            val originalAbsolutePath = resolveOriginalAbsolutePath(video)
            if (!isAppInternalPath(originalAbsolutePath) && !hasAllFilesAccessPermission()) {
                val settingsIntent = createManageAllFilesAccessIntent()
                runCatching { context.startActivity(settingsIntent) }
                return@withContext MoveToVaultResult.RequiresAllFilesAccess(settingsIntent)
            }

            // Ensure vaultDir and .nomedia exist
            val targetVaultDir = vaultDir
            val existingEntries = readVaultKvEntries().filter { it.privateFile.exists() }

            // Duplicate prevention: check if originalAbsolutePath is already mapped in vault.kv
            val duplicate = existingEntries.firstOrNull {
                it.originalAbsolutePath == originalAbsolutePath ||
                    it.privateFilePath == originalAbsolutePath
            }
            if (duplicate != null) {
                val cachedMap = readCachedMetaMap().toMutableMap()
                val meta = extractOrResolveMetadata(duplicate, cachedMap)
                if (!File(originalAbsolutePath).exists()) {
                    silentlyDeleteStaleMediaStoreEntry(originalAbsolutePath, video.uri)
                }
                return@withContext MoveToVaultResult.Completed(
                    buildVaultVideoItem(duplicate, meta)
                )
            }

            val sourceFile = File(originalAbsolutePath)
            if (!sourceFile.exists()) {
                return@withContext MoveToVaultResult.Error("Video file not found at $originalAbsolutePath")
            }

            val hexId = generateUpperHexId()
            val destFile = File(targetVaultDir, "${hexId}.private")

            // Instant Zero-Copy Move
            val moved = performInstantZeroCopyMove(sourceFile, destFile)
            if (!moved || !destFile.exists()) {
                return@withContext MoveToVaultResult.Error(
                    "Unable to move video into ${targetVaultDir.absolutePath}"
                )
            }

            // Append exact `<private_path>><original_path>` line to vault.kv
            appendToVaultKv(destFile.absolutePath, sourceFile.absolutePath)

            // Cache metadata and isolated thumbnail
            val nowSec = System.currentTimeMillis() / 1000L
            val meta = CachedVaultMeta(
                hexId = hexId,
                durationMs = video.durationMs,
                width = video.width,
                height = video.height,
                mimeType = video.mimeType,
                hiddenAtSec = nowSec
            )
            val cachedMap = readCachedMetaMap().toMutableMap()
            cachedMap[hexId] = meta
            writeCachedMetaMap(cachedMap)

            ensureIsolatedThumbnail(hexId, destFile)

            // Remove stale public entry silently from MediaStore without system delete dialog
            silentlyDeleteStaleMediaStoreEntry(sourceFile.absolutePath, video.uri)

            val createdEntry = VaultEntry(
                privateFilePath = destFile.absolutePath,
                originalAbsolutePath = sourceFile.absolutePath,
                hexId = hexId
            )
            MoveToVaultResult.Completed(buildVaultVideoItem(createdEntry, meta))
        } catch (e: Exception) {
            e.printStackTrace()
            MoveToVaultResult.Error(e.localizedMessage ?: "Failed to lock video in Private Vault.")
        }
    }

    suspend fun moveVideoToVault(video: VideoItem): MoveToVaultResult = lockVideo(video)

    /**
     * Batch-locks multiple videos instantly into `/storage/emulated/0/AuraPlayer/.private/<HEX_ID>.private`.
     */
    suspend fun moveVideosToVault(videos: List<VideoItem>): BatchMoveToVaultResult = withContext(Dispatchers.IO) {
        if (videos.isEmpty()) return@withContext BatchMoveToVaultResult.Completed(0)

        val firstExternal = videos.firstOrNull { !isAppInternalPath(resolveOriginalAbsolutePath(it)) }
        if (firstExternal != null && !hasAllFilesAccessPermission()) {
            val settingsIntent = createManageAllFilesAccessIntent()
            runCatching { context.startActivity(settingsIntent) }
            return@withContext BatchMoveToVaultResult.RequiresAllFilesAccess(settingsIntent)
        }

        var movedCount = 0
        val movedIds = HashSet<Long>()

        for (video in videos) {
            when (lockVideo(video)) {
                is MoveToVaultResult.Completed -> {
                    movedCount++
                    movedIds.add(video.id)
                }
                is MoveToVaultResult.RequiresAllFilesAccess -> {
                    return@withContext BatchMoveToVaultResult.RequiresAllFilesAccess(
                        createManageAllFilesAccessIntent()
                    )
                }
                is MoveToVaultResult.Error -> {
                    // Continue with remaining videos in batch
                }
            }
        }

        if (movedCount == 0 && movedIds.isEmpty()) {
            return@withContext BatchMoveToVaultResult.Error("Unable to move selected videos to Private Vault.")
        }

        BatchMoveToVaultResult.Completed(movedCount, movedIds)
    }

    /**
     * Resolves the restore destination path. If a file already exists at `originalAbsolutePath`,
     * appends `(1)` (or `(2)`, etc.) before the extension so nothing is overwritten.
     */
    fun resolveNonOverwritingRestorePath(originalAbsolutePath: String): String {
        val candidate = File(originalAbsolutePath)
        if (!candidate.exists()) return candidate.absolutePath

        val parent = candidate.parentFile
        val fileName = candidate.name
        val dotIndex = fileName.lastIndexOf('.')
        val baseName = if (dotIndex > 0) fileName.substring(0, dotIndex) else fileName
        val extWithDot = if (dotIndex > 0) fileName.substring(dotIndex) else ""

        var index = 1
        while (true) {
            val nextName = "${baseName}($index)$extWithDot"
            val nextFile = if (parent != null) File(parent, nextName) else File(nextName)
            if (!nextFile.exists()) {
                return nextFile.absolutePath
            }
            index++
        }
    }

    /**
     * Instant Unlock / Restore Operation (`unlockVideo(entry: VaultEntry)`):
     * 1. Looks up the entry in `vault.kv` to get `privateFilePath` and `originalAbsolutePath`.
     * 2. Ensures the original parent folder exists (`File(originalAbsolutePath).parentFile?.mkdirs()`).
     * 3. If a file already exists at `originalAbsolutePath`, appends `(1)` before the extension.
     * 4. Instant Zero-Copy Restore via `File(privateFilePath).renameTo(File(targetOriginalPath))`.
     * 5. Removes that line from `vault.kv` and notifies `MediaScannerConnection.scanFile(...)`.
     */
    suspend fun unlockVideo(entry: VaultEntry): Boolean = withContext(Dispatchers.IO) {
        try {
            val allEntries = readVaultKvEntries()
            val matched = allEntries.firstOrNull {
                it.privateFilePath == entry.privateFilePath ||
                    it.originalAbsolutePath == entry.originalAbsolutePath ||
                    it.hexId == entry.hexId
            } ?: entry

            val privateFile = File(matched.privateFilePath)
            if (!privateFile.exists()) return@withContext false

            val originalAbsolutePath = matched.originalAbsolutePath
            File(originalAbsolutePath).parentFile?.mkdirs()

            val targetOriginalPath = resolveNonOverwritingRestorePath(originalAbsolutePath)
            val targetOriginalFile = File(targetOriginalPath)

            val restored = performInstantZeroCopyMove(privateFile, targetOriginalFile)
            if (!restored || !targetOriginalFile.exists()) {
                return@withContext false
            }

            // Remove restored line from vault.kv
            val remainingEntries = allEntries.filterNot {
                it.privateFilePath == matched.privateFilePath ||
                    it.originalAbsolutePath == matched.originalAbsolutePath
            }
            writeVaultKvEntries(remainingEntries)

            // Clean up isolated thumbnail and sidecar metadata cache
            deleteIsolatedThumbnail(matched.hexId)
            val cachedMap = readCachedMetaMap().toMutableMap()
            if (cachedMap.remove(matched.hexId) != null) {
                writeCachedMetaMap(cachedMap)
            }

            // Notify MediaScanner so the video reappears immediately in its original folder
            runCatching {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(targetOriginalPath),
                    null,
                    null
                )
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Convenience overload for unlocking a [VideoItem] from `VaultScreen`.
     */
    suspend fun unlockVideo(vaultVideo: VideoItem): Boolean = withContext(Dispatchers.IO) {
        val kvEntries = readVaultKvEntries()
        val matchedEntry = kvEntries.firstOrNull { entry ->
            entry.privateFilePath == vaultVideo.vaultFilePath ||
                entry.privateFilePath == vaultVideo.uri.path ||
                entry.originalAbsolutePath == vaultVideo.relativePath ||
                entry.stableId == vaultVideo.id
        } ?: VaultEntry(
            privateFilePath = vaultVideo.vaultFilePath ?: vaultVideo.uri.path ?: return@withContext false,
            originalAbsolutePath = vaultVideo.relativePath.takeIf { it.startsWith("/") }
                ?: "${Environment.getExternalStorageDirectory()?.absolutePath ?: "/storage/emulated/0"}/${Environment.DIRECTORY_MOVIES}/${vaultVideo.displayName}"
        )
        unlockVideo(matchedEntry)
    }

    suspend fun restoreVideoToGallery(vaultVideo: VideoItem): Boolean = unlockVideo(vaultVideo)

    suspend fun deleteVaultVideoPermanently(vaultVideo: VideoItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val kvEntries = readVaultKvEntries()
            val matched = kvEntries.firstOrNull { entry ->
                entry.privateFilePath == vaultVideo.vaultFilePath ||
                    entry.privateFilePath == vaultVideo.uri.path ||
                    entry.stableId == vaultVideo.id
            }
            val privatePath = matched?.privateFilePath ?: vaultVideo.vaultFilePath ?: vaultVideo.uri.path
            if (privatePath != null) {
                File(privatePath).delete()
            }
            val hexId = matched?.hexId ?: privatePath?.let { File(it).nameWithoutExtension }
            if (hexId != null) {
                deleteIsolatedThumbnail(hexId)
                val cachedMap = readCachedMetaMap().toMutableMap()
                if (cachedMap.remove(hexId) != null) {
                    writeCachedMetaMap(cachedMap)
                }
            }
            val remaining = kvEntries.filterNot { entry ->
                entry.privateFilePath == privatePath || entry.stableId == vaultVideo.id
            }
            writeVaultKvEntries(remaining)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private data class CachedVaultMeta(
        val hexId: String,
        val durationMs: Long,
        val width: Int,
        val height: Int,
        val mimeType: String,
        val hiddenAtSec: Long
    )

    private fun readCachedMetaMap(): Map<String, CachedVaultMeta> {
        val file = metadataCacheFile
        if (!file.exists()) return emptyMap()
        return try {
            val raw = file.readText()
            val decryptedJson = BiometricAuth.decryptSecretValue(raw)
            if (decryptedJson.isBlank()) return emptyMap()
            val arr = JSONArray(decryptedJson)
            val map = HashMap<String, CachedVaultMeta>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val hexId = obj.optString("hexId")
                if (hexId.isNotBlank()) {
                    map[hexId] = CachedVaultMeta(
                        hexId = hexId,
                        durationMs = obj.optLong("durationMs", 0L),
                        width = obj.optInt("width", 1920),
                        height = obj.optInt("height", 1080),
                        mimeType = obj.optString("mimeType", "video/mp4"),
                        hiddenAtSec = obj.optLong("hiddenAtSec", 1L)
                    )
                }
            }
            // Transparently upgrade legacy plaintext cache on disk to Keystore AES-256-GCM
            if (!BiometricAuth.isEncryptedPayload(raw) && BiometricAuth.isKeystoreProtected()) {
                writeCachedMetaMap(map)
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun writeCachedMetaMap(map: Map<String, CachedVaultMeta>) {
        try {
            val arr = JSONArray()
            map.values.forEach { item ->
                val obj = JSONObject().apply {
                    put("hexId", item.hexId)
                    put("durationMs", item.durationMs)
                    put("width", item.width)
                    put("height", item.height)
                    put("mimeType", item.mimeType)
                    put("hiddenAtSec", item.hiddenAtSec)
                }
                arr.put(obj)
            }
            metadataCacheFile.parentFile?.mkdirs()
            val protectedPayload = BiometricAuth.encryptSecretValue(arr.toString())
            metadataCacheFile.writeText(protectedPayload)
        } catch (_: Exception) {
        }
    }

    companion object {
        /**
         * Switches the home screen launcher icon & app name disguise using
         * `PackageManager.setComponentEnabledSetting()` with `DONT_KILL_APP`.
         */
        fun applyAppDisguise(context: Context, mode: AppDisguiseMode) {
            runCatching {
                val pm = context.packageManager
                val pkg = context.packageName
                val calcComp = ComponentName(pkg, "$pkg.AliasCalculator")
                val notesComp = ComponentName(pkg, "$pkg.AliasNotes")

                when (mode) {
                    AppDisguiseMode.DEFAULT -> {
                        pm.setComponentEnabledSetting(
                            calcComp,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP
                        )
                        pm.setComponentEnabledSetting(
                            notesComp,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP
                        )
                    }
                    AppDisguiseMode.CALCULATOR -> {
                        pm.setComponentEnabledSetting(
                            calcComp,
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                            PackageManager.DONT_KILL_APP
                        )
                        pm.setComponentEnabledSetting(
                            notesComp,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP
                        )
                    }
                    AppDisguiseMode.NOTES -> {
                        pm.setComponentEnabledSetting(
                            calcComp,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP
                        )
                        pm.setComponentEnabledSetting(
                            notesComp,
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                            PackageManager.DONT_KILL_APP
                        )
                    }
                }
            }
        }
    }
}
