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
 * Cache-aware model for extracting video thumbnails reliably across large 4K/HEVC `.mkv` files,
 * portrait camera recordings, and non-indexed/custom folders.
 */
data class VideoThumbnailKey(
    val uri: Uri,
    val filePath: String? = null,
    val durationMs: Long = 0L,
    val dateModifiedSec: Long = 0L,
    val isVaultItem: Boolean = false,
    val width: Int = 0,
    val height: Int = 0,
    val orientationDegrees: Int = 0
) {
    /**
     * Bumped to `_v2` so legacy low-res (`320x180`) cached thumbnails are automatically
     * replaced by crisp aspect-preserving frames.
     */
    val stableDiskKey: String
        get() {
            val base = filePath?.takeIf { it.isNotBlank() } ?: uri.toString()
            return "${base}_${dateModifiedSec}_v2"
        }
}

/**
 * Two-tier (Memory `LruCache` + Disk `cacheDir/video_thumbs`) video thumbnail cache and
 * multi-stage keyframe extractor optimized for large 4K/HEVC/MKV and portrait 1080x1920 files.
 */
object ThumbnailCacheManager {
    internal const val LANDSCAPE_TARGET_WIDTH = 640
    internal const val LANDSCAPE_TARGET_HEIGHT = 360
    internal const val PORTRAIT_TARGET_WIDTH = 540
    internal const val PORTRAIT_TARGET_HEIGHT = 960
    internal const val FALLBACK_MAX_DIMENSION = 640
    private const val MAX_BITMAP_LONG_EDGE = 960
    private const val MIN_BITMAP_SHORT_EDGE = 540
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

    /**
     * Computes aspect-preserving target extraction dimensions with a minimum of `540px`
     * on the shorter side (`540 × 960` for portrait videos, `640 × 360` for landscape videos,
     * or `640 × 640` when dimensions are unknown).
     */
    fun computeTargetDimensions(
        rawWidth: Int,
        rawHeight: Int,
        rotationDegrees: Int = 0
    ): Pair<Int, Int> {
        val normalizedRotation = ((rotationDegrees % 360) + 360) % 360
        val swapAxes = normalizedRotation == 90 || normalizedRotation == 270
        val visualWidth = if (swapAxes) rawHeight else rawWidth
        val visualHeight = if (swapAxes) rawWidth else rawHeight

        if (visualWidth <= 0 || visualHeight <= 0) {
            return FALLBACK_MAX_DIMENSION to FALLBACK_MAX_DIMENSION
        }
        return if (visualHeight > visualWidth) {
            PORTRAIT_TARGET_WIDTH to PORTRAIT_TARGET_HEIGHT
        } else {
            LANDSCAPE_TARGET_WIDTH to LANDSCAPE_TARGET_HEIGHT
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

        // Stage 1: MediaMetadataRetriever nearest-keyframe aspect-aware scaled extraction
        var extracted = extractViaMediaMetadataRetriever(context, key)

        // Stage 2: ThumbnailUtils / ContentResolver.loadThumbnail fallback at 640x640
        if (extracted == null) {
            extracted = extractViaPlatformThumbnailFallback(context, key)
        }

        if (extracted != null && !extracted.isRecycled) {
            memoryCache.put(cacheKey, extracted)
            if (diskFile != null) {
                runCatching {
                    FileOutputStream(diskFile).use { out ->
                        extracted.compress(Bitmap.CompressFormat.JPEG, 90, out)
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

            val metaWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull()
                ?.takeIf { it > 0 }
                ?: key.width
            val metaHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull()
                ?.takeIf { it > 0 }
                ?: key.height
            val metaRotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                ?.toIntOrNull()
                ?: key.orientationDegrees

            val (targetWidth, targetHeight) = computeTargetDimensions(
                rawWidth = metaWidth,
                rawHeight = metaHeight,
                rotationDegrees = metaRotation
            )

            // 10% of duration in microseconds (or 3_000_000 us fallback)
            val targetTimeUs = if (resolvedDurationMs > 0L) {
                (resolvedDurationMs * 100L).coerceAtLeast(1_000_000L)
            } else {
                DEFAULT_SEEK_US
            }

            extractScaledSyncFrame(retriever, targetTimeUs, targetWidth, targetHeight)
                ?: extractScaledSyncFrame(retriever, 0L, targetWidth, targetHeight)
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun extractScaledSyncFrame(
        retriever: MediaMetadataRetriever,
        timeUs: Long,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            val scaled = runCatching {
                retriever.getScaledFrameAtTime(
                    timeUs,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    targetWidth,
                    targetHeight
                )
            }.getOrNull()
            if (scaled != null) return scaled
        }

        val raw = runCatching {
            retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        }.getOrNull() ?: return null

        return scaleDownBitmapPreservingAspect(raw)
    }

    @Suppress("DEPRECATION")
    private fun extractViaPlatformThumbnailFallback(
        context: Context,
        key: VideoThumbnailKey
    ): Bitmap? {
        val localFile = key.filePath?.takeIf { it.isNotBlank() }?.let { File(it) }
        val fallbackSize = Size(FALLBACK_MAX_DIMENSION, FALLBACK_MAX_DIMENSION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (localFile != null && localFile.exists()) {
                val fromFile = runCatching {
                    ThumbnailUtils.createVideoThumbnail(
                        localFile,
                        fallbackSize,
                        null
                    )
                }.getOrNull()
                if (fromFile != null) return scaleDownBitmapPreservingAspect(fromFile)
            }
            if (!key.isVaultItem) {
                val fromResolver = runCatching {
                    context.contentResolver.loadThumbnail(
                        key.uri,
                        fallbackSize,
                        null
                    )
                }.getOrNull()
                if (fromResolver != null) return scaleDownBitmapPreservingAspect(fromResolver)
            }
        } else if (localFile != null && localFile.exists()) {
            val legacyThumb = runCatching {
                ThumbnailUtils.createVideoThumbnail(
                    localFile.absolutePath,
                    MediaStore.Images.Thumbnails.MINI_KIND
                )
            }.getOrNull()
            if (legacyThumb != null) return scaleDownBitmapPreservingAspect(legacyThumb)
        }
        return null
    }

    private fun scaleDownBitmapPreservingAspect(bitmap: Bitmap): Bitmap {
        val w = bitmap.width.coerceAtLeast(1)
        val h = bitmap.height.coerceAtLeast(1)
        val shortEdge = minOf(w, h)
        val longEdge = maxOf(w, h)
        if (shortEdge <= MIN_BITMAP_SHORT_EDGE && longEdge <= MAX_BITMAP_LONG_EDGE) {
            return bitmap
        }
        val ratio = maxOf(
            MIN_BITMAP_SHORT_EDGE.toFloat() / shortEdge.toFloat(),
            MAX_BITMAP_LONG_EDGE.toFloat() / longEdge.toFloat()
        ).coerceAtMost(1f)
        if (ratio >= 0.99f) return bitmap

        val targetW = (w * ratio).toInt().coerceAtLeast(1)
        val targetH = (h * ratio).toInt().coerceAtLeast(1)
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
