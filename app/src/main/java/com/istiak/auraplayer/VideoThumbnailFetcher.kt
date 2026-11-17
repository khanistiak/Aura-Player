package com.istiak.auraplayer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Cache-aware model for extracting video thumbnails reliably across large 4K/HEVC `.mkv` files
 * and non-indexed/custom folders.
 */
data class VideoThumbnailKey(
    val uri: Uri,
    val filePath: String? = null,
    val durationMs: Long = 0L,
    val dateModifiedSec: Long = 0L,
    val isVaultItem: Boolean = false
) {
    val stableDiskKey: String
        get() {
            val base = filePath?.takeIf { it.isNotBlank() } ?: uri.toString()
            return "${base}_${dateModifiedSec}"
        }
}

/**
 * Two-tier (Memory `LruCache` + Disk `cacheDir/video_thumbs`) video thumbnail cache and
 * multi-stage keyframe extractor optimized for large 4K/HEVC/MKV files.
 */
object ThumbnailCacheManager {
    private const val THUMB_WIDTH = 320
    private const val THUMB_HEIGHT = 180
    private const val DISK_CACHE_DIR = "video_thumbs"
    private const val DEFAULT_SEEK_US = 3_000_000L

    private val memoryCache: LruCache<String, Bitmap> by lazy {
        val maxKb = (Runtime.getRuntime().maxMemory() / 1024L).toInt()
        val cacheSizeKb = (maxKb / 8).coerceIn(8 * 1024, 32 * 1024)
        object : LruCache<String, Bitmap>(cacheSizeKb) {
            override fun sizeOf(key: String, value: Bitmap): Int {
                return (value.allocationByteCount / 1024).coerceAtLeast(1)
            }
        }
    }

    fun clearMemoryCache() {
        memoryCache.evictAll()
    }

    fun getDiskCacheDir(context: Context): File {
        return File(context.cacheDir, DISK_CACHE_DIR).apply {
            if (!exists()) mkdirs()
        }
    }

    private fun hashKey(rawKey: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(rawKey.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun getDiskCacheFile(context: Context, cacheKey: String): File {
        return File(getDiskCacheDir(context), "${hashKey(cacheKey)}.jpg")
    }

    suspend fun getOrExtractThumbnail(
        context: Context,
        key: VideoThumbnailKey
    ): Bitmap? = withContext(Dispatchers.IO) {
        val cacheKey = key.stableDiskKey

        // Stage 0a: Fast memory LruCache hit
        memoryCache.get(cacheKey)?.let { cached ->
            if (!cached.isRecycled) return@withContext cached
        }

        // Stage 0b: Disk cache hit (skipped for vault items so private previews stay isolated)
        val diskFile = if (!key.isVaultItem) getDiskCacheFile(context, cacheKey) else null
        if (diskFile != null && diskFile.exists() && diskFile.length() > 0L) {
            val decoded = runCatching { BitmapFactory.decodeFile(diskFile.absolutePath) }.getOrNull()
            if (decoded != null && !decoded.isRecycled) {
                memoryCache.put(cacheKey, decoded)
                return@withContext decoded
            }
        }

        // Stage 1: MediaMetadataRetriever nearest-keyframe scaled extraction
        var extracted = extractViaMediaMetadataRetriever(context, key)

        // Stage 2: ThumbnailUtils / ContentResolver.loadThumbnail fallback (handles tricky MKV/HEVC profiles)
        if (extracted == null) {
            extracted = extractViaPlatformThumbnailFallback(context, key)
        }

        if (extracted != null && !extracted.isRecycled) {
            memoryCache.put(cacheKey, extracted)
            if (diskFile != null) {
                runCatching {
                    FileOutputStream(diskFile).use { out ->
                        extracted.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    }
                }
            }
        }
        extracted
    }

    private fun extractViaMediaMetadataRetriever(
        context: Context,
        key: VideoThumbnailKey
    ): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            val localFile = key.filePath?.takeIf { it.isNotBlank() }?.let { File(it) }
            val configured = if (localFile != null && localFile.exists() && localFile.canRead()) {
                runCatching {
                    retriever.setDataSource(localFile.absolutePath)
                    true
                }.getOrDefault(false)
            } else {
                false
            }
            if (!configured) {
                retriever.setDataSource(context, key.uri)
            }

            val resolvedDurationMs = if (key.durationMs > 0L) {
                key.durationMs
            } else {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull()
                    ?: 0L
            }

            // 10% of duration in microseconds (or 3_000_000 us fallback)
            val targetTimeUs = if (resolvedDurationMs > 0L) {
                (resolvedDurationMs * 100L).coerceAtLeast(1_000_000L)
            } else {
                DEFAULT_SEEK_US
            }

            extractScaledSyncFrame(retriever, targetTimeUs)
                ?: extractScaledSyncFrame(retriever, 0L)
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun extractScaledSyncFrame(
        retriever: MediaMetadataRetriever,
        timeUs: Long
    ): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            val scaled = runCatching {
                retriever.getScaledFrameAtTime(
                    timeUs,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    THUMB_WIDTH,
                    THUMB_HEIGHT
                )
            }.getOrNull()
            if (scaled != null) return scaled
        }

        val raw = runCatching {
            retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        }.getOrNull() ?: return null

        return scaleDownBitmapIfNeeded(raw)
    }

    @Suppress("DEPRECATION")
    private fun extractViaPlatformThumbnailFallback(
        context: Context,
        key: VideoThumbnailKey
    ): Bitmap? {
        val localFile = key.filePath?.takeIf { it.isNotBlank() }?.let { File(it) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (localFile != null && localFile.exists()) {
                val fromFile = runCatching {
                    ThumbnailUtils.createVideoThumbnail(
                        localFile,
                        Size(THUMB_WIDTH, THUMB_HEIGHT),
                        null
                    )
                }.getOrNull()
                if (fromFile != null) return scaleDownBitmapIfNeeded(fromFile)
            }
            if (!key.isVaultItem) {
                val fromResolver = runCatching {
                    context.contentResolver.loadThumbnail(
                        key.uri,
                        Size(THUMB_WIDTH, THUMB_HEIGHT),
                        null
                    )
                }.getOrNull()
                if (fromResolver != null) return scaleDownBitmapIfNeeded(fromResolver)
            }
        } else if (localFile != null && localFile.exists()) {
            val legacyThumb = runCatching {
                ThumbnailUtils.createVideoThumbnail(
                    localFile.absolutePath,
                    MediaStore.Images.Thumbnails.MINI_KIND
                )
            }.getOrNull()
            if (legacyThumb != null) return scaleDownBitmapIfNeeded(legacyThumb)
        }
        return null
    }

    private fun scaleDownBitmapIfNeeded(bitmap: Bitmap): Bitmap {
        if (bitmap.width <= THUMB_WIDTH && bitmap.height <= THUMB_HEIGHT) return bitmap
        val ratio = minOf(
            THUMB_WIDTH.toFloat() / bitmap.width.coerceAtLeast(1),
            THUMB_HEIGHT.toFloat() / bitmap.height.coerceAtLeast(1)
        ).coerceAtMost(1f)
        val targetW = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val targetH = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
        if (scaled != bitmap) {
            bitmap.recycle()
        }
        return scaled
    }
}

/**
 * Custom Coil [Fetcher] that resolves [VideoThumbnailKey] requests via [ThumbnailCacheManager].
 */
class VideoThumbnailFetcher(
    private val context: Context,
    private val data: VideoThumbnailKey
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val bitmap = ThumbnailCacheManager.getOrExtractThumbnail(context, data) ?: return null
        return DrawableResult(
            drawable = BitmapDrawable(context.resources, bitmap),
            isSampled = true,
            dataSource = DataSource.DISK
        )
    }

    class Factory(private val context: Context) : Fetcher.Factory<VideoThumbnailKey> {
        override fun create(
            data: VideoThumbnailKey,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher {
            return VideoThumbnailFetcher(context.applicationContext, data)
        }
    }
}
