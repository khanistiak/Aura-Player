package com.istiak.auraplayer

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

private val Context.playbackProgressDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "aura_playback_progress"
)

data class VideoPlaybackProgress(
    val videoUri: String,
    val filePath: String? = null,
    val videoId: Long = -1L,
    val savedPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val updatedAtMs: Long = 0L
) {
    val watchProgressFraction: Float
        get() {
            if (totalDurationMs <= 0L || savedPositionMs <= 0L) return 0f
            val raw = (savedPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
            return if (raw >= 0.95f) 1.0f else raw
        }

    val isCompleted: Boolean
        get() = watchProgressFraction >= 1.0f
}

data class PlaybackProgressSnapshot(
    val lastPlayedVideoUri: String? = null,
    val lastPlayedFilePath: String? = null,
    val lastPlayedVideoId: Long = -1L,
    val progressByKey: Map<String, VideoPlaybackProgress> = emptyMap()
) {
    fun findProgress(video: VideoItem): VideoPlaybackProgress? {
        val canonicalPath = video.effectiveCanonicalPath()
        if (canonicalPath != null) {
            progressByKey[PlaybackProgressStore.canonicalStorageKey(video.uri.toString(), canonicalPath)]
                ?.let { return it }
            progressByKey.values.firstOrNull { it.filePath == canonicalPath }
                ?.let { return it }
        }
        val uriString = video.uri.toString()
        progressByKey[PlaybackProgressStore.uriStorageKey(uriString)]?.let { return it }
        progressByKey.values.firstOrNull { it.videoUri == uriString }?.let { return it }
        if (video.id > 0L) {
            progressByKey.values.firstOrNull { it.videoId == video.id }?.let { return it }
        }
        return null
    }

    fun isLastPlayed(video: VideoItem): Boolean {
        val uriMatches = !lastPlayedVideoUri.isNullOrBlank() &&
            lastPlayedVideoUri == video.uri.toString()
        if (uriMatches) return true

        val canonicalPath = video.effectiveCanonicalPath()
        if (!lastPlayedFilePath.isNullOrBlank() && canonicalPath != null && lastPlayedFilePath == canonicalPath) {
            return true
        }
        return lastPlayedVideoId > 0L && video.id > 0L && lastPlayedVideoId == video.id
    }

    fun savedPositionMsFor(video: VideoItem, fallbackSavedPosMs: Long = 0L): Long {
        val tracked = findProgress(video)?.savedPositionMs
        return tracked ?: fallbackSavedPosMs
    }

    fun watchProgressFractionFor(video: VideoItem, fallbackSavedPosMs: Long = 0L): Float {
        val entry = findProgress(video)
        val effectivePosMs = entry?.savedPositionMs ?: fallbackSavedPosMs
        val effectiveDurationMs = (entry?.totalDurationMs ?: 0L).takeIf { it > 0L } ?: video.durationMs
        if (effectiveDurationMs <= 0L || effectivePosMs <= 0L) return 0f
        val raw = (effectivePosMs.toFloat() / effectiveDurationMs.toFloat()).coerceIn(0f, 1f)
        return if (raw >= 0.95f) 1.0f else raw
    }

    fun hasStarted(video: VideoItem): Boolean {
        return findProgress(video) != null
    }

    fun findLastPlayedVideo(videos: List<VideoItem>): VideoItem? {
        if (videos.isEmpty()) return null
        if (!lastPlayedVideoUri.isNullOrBlank()) {
            videos.firstOrNull { it.uri.toString() == lastPlayedVideoUri }?.let { return it }
        }
        if (!lastPlayedFilePath.isNullOrBlank()) {
            videos.firstOrNull { it.effectiveCanonicalPath() == lastPlayedFilePath }?.let { return it }
        }
        if (lastPlayedVideoId > 0L) {
            videos.firstOrNull { it.id == lastPlayedVideoId }?.let { return it }
        }
        return null
    }
}

fun VideoItem.effectiveCanonicalPath(): String? {
    val raw = filePath?.trim()?.takeIf { it.isNotEmpty() }
        ?: vaultFilePath?.trim()?.takeIf { it.isNotEmpty() }
        ?: uri.path?.takeIf { uri.scheme == "file" && it.isNotBlank() }
    return raw?.let { candidate ->
        runCatching { File(candidate).canonicalPath }.getOrElse { candidate }
    }
}

/**
 * Jetpack DataStore repository tracking:
 * - The single most recently played video (`lastPlayedVideoUri`, `lastPlayedFilePath`, `lastPlayedVideoId`)
 * - Per-video `savedPositionMs` and `totalDurationMs` keyed by canonical file path / URI so
 *   watch progress bars and Aura Blue last-played highlights stay synchronized across Home and All Videos.
 */
class PlaybackProgressStore(private val context: Context) {

    private object Keys {
        val LAST_PLAYED_URI = stringPreferencesKey("last_played_video_uri")
        val LAST_PLAYED_PATH = stringPreferencesKey("last_played_file_path")
        val LAST_PLAYED_ID = longPreferencesKey("last_played_video_id")
        val PROGRESS_MAP_JSON = stringPreferencesKey("video_progress_map_json")
    }

    val progressFlow: Flow<PlaybackProgressSnapshot> = context.playbackProgressDataStore.data.map { prefs ->
        PlaybackProgressSnapshot(
            lastPlayedVideoUri = prefs[Keys.LAST_PLAYED_URI]?.takeIf { it.isNotBlank() },
            lastPlayedFilePath = prefs[Keys.LAST_PLAYED_PATH]?.takeIf { it.isNotBlank() },
            lastPlayedVideoId = prefs[Keys.LAST_PLAYED_ID] ?: -1L,
            progressByKey = parseProgressJson(prefs[Keys.PROGRESS_MAP_JSON])
        )
    }

    suspend fun getSnapshot(): PlaybackProgressSnapshot = withContext(Dispatchers.IO) {
        progressFlow.first()
    }

    suspend fun saveProgress(
        video: VideoItem,
        savedPositionMs: Long,
        totalDurationMs: Long
    ) = withContext(Dispatchers.IO) {
        val uriString = video.uri.toString()
        val canonicalPath = video.effectiveCanonicalPath()
        val effectiveDurationMs = totalDurationMs.takeIf { it > 0L } ?: video.durationMs.coerceAtLeast(0L)
        val rawFraction = if (effectiveDurationMs > 0L && savedPositionMs > 0L) {
            (savedPositionMs.toFloat() / effectiveDurationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
        val normalizedPosMs = if (rawFraction >= 0.95f && effectiveDurationMs > 0L) {
            effectiveDurationMs
        } else {
            savedPositionMs.coerceAtLeast(0L).let { pos ->
                if (effectiveDurationMs > 0L) pos.coerceAtMost(effectiveDurationMs) else pos
            }
        }
        val nowMs = System.currentTimeMillis()
        val primaryKey = canonicalStorageKey(uriString, canonicalPath)

        context.playbackProgressDataStore.edit { prefs ->
            prefs[Keys.LAST_PLAYED_URI] = uriString
            if (canonicalPath != null) {
                prefs[Keys.LAST_PLAYED_PATH] = canonicalPath
            } else {
                prefs.remove(Keys.LAST_PLAYED_PATH)
            }
            prefs[Keys.LAST_PLAYED_ID] = video.id

            val currentMap = parseProgressJson(prefs[Keys.PROGRESS_MAP_JSON]).toMutableMap()
            if (canonicalPath != null) {
                currentMap.remove(uriStorageKey(uriString))
            }
            currentMap[primaryKey] = VideoPlaybackProgress(
                videoUri = uriString,
                filePath = canonicalPath,
                videoId = video.id,
                savedPositionMs = normalizedPosMs,
                totalDurationMs = effectiveDurationMs,
                updatedAtMs = nowMs
            )
            prefs[Keys.PROGRESS_MAP_JSON] = serializeProgressJson(currentMap)
        }
    }

    private fun parseProgressJson(rawJson: String?): Map<String, VideoPlaybackProgress> {
        if (rawJson.isNullOrBlank()) return emptyMap()
        return runCatching {
            val root = JSONObject(rawJson)
            val result = LinkedHashMap<String, VideoPlaybackProgress>(root.length())
            val keys = root.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val obj = root.optJSONObject(key) ?: continue
                val videoUri = obj.optString("videoUri", "")
                if (videoUri.isBlank()) continue
                result[key] = VideoPlaybackProgress(
                    videoUri = videoUri,
                    filePath = obj.optString("filePath", "").takeIf { it.isNotBlank() },
                    videoId = obj.optLong("videoId", -1L),
                    savedPositionMs = obj.optLong("savedPositionMs", 0L),
                    totalDurationMs = obj.optLong("totalDurationMs", 0L),
                    updatedAtMs = obj.optLong("updatedAtMs", 0L)
                )
            }
            result.toMap()
        }.getOrDefault(emptyMap())
    }

    private fun serializeProgressJson(map: Map<String, VideoPlaybackProgress>): String {
        val root = JSONObject()
        map.forEach { (key, entry) ->
            val obj = JSONObject().apply {
                put("videoUri", entry.videoUri)
                put("filePath", entry.filePath ?: "")
                put("videoId", entry.videoId)
                put("savedPositionMs", entry.savedPositionMs)
                put("totalDurationMs", entry.totalDurationMs)
                put("updatedAtMs", entry.updatedAtMs)
            }
            root.put(key, obj)
        }
        return root.toString()
    }

    companion object {
        fun uriStorageKey(videoUri: String): String = "uri:$videoUri"

        fun canonicalStorageKey(videoUri: String, canonicalFilePath: String?): String {
            return if (!canonicalFilePath.isNullOrBlank()) {
                "path:$canonicalFilePath"
            } else {
                uriStorageKey(videoUri)
            }
        }
    }
}
