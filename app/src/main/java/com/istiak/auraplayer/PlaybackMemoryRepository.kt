package com.istiak.auraplayer

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject

private val Context.playbackMemoryDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "aura_playback_memory"
)

data class VideoPlaybackMemory(
    val videoId: Long,
    val videoUri: String,
    val resumePositionMs: Long = 0L,
    val selectedAudioGroupIndex: Int = -1,
    val selectedAudioTrackIndex: Int = -1,
    val selectedSubtitleGroupIndex: Int = -1,
    val selectedSubtitleTrackIndex: Int = -1,
    val areSubtitlesEnabled: Boolean = true,
    val subtitleSyncOffsetMs: Long = 0L,
    val scaleMode: PlayerScaleMode = PlayerScaleMode.FIT,
    val externalSubtitlePath: String? = null,
    val updatedAtMs: Long = System.currentTimeMillis()
)

/**
 * Persists smart per-video playback memory in Jetpack DataStore:
 * - Exact resume timestamp (`resumePositionMs`)
 * - Selected audio track (`selectedAudioGroupIndex`, `selectedAudioTrackIndex`)
 * - Selected subtitle track (`selectedSubtitleGroupIndex`, `selectedSubtitleTrackIndex`, `areSubtitlesEnabled`)
 * - Subtitle synchronization offset (`subtitleSyncOffsetMs`)
 * - Aspect ratio / scale mode (`scaleMode`)
 * - Downloaded or external subtitle file path (`externalSubtitlePath`)
 */
class PlaybackMemoryRepository(private val context: Context) {

    private fun keyForVideo(videoId: Long, videoUri: String): Preferences.Key<String> {
        val stableId = if (videoId > 0L) "vid_$videoId" else "uri_${videoUri.hashCode()}"
        return stringPreferencesKey("memory_$stableId")
    }

    fun observeMemory(videoId: Long, videoUri: String): Flow<VideoPlaybackMemory?> {
        val prefKey = keyForVideo(videoId, videoUri)
        return context.playbackMemoryDataStore.data.map { prefs ->
            prefs[prefKey]?.let { rawJson -> parseJson(videoId, videoUri, rawJson) }
        }
    }

    suspend fun getMemory(videoId: Long, videoUri: String): VideoPlaybackMemory? =
        withContext(Dispatchers.IO) {
            val prefKey = keyForVideo(videoId, videoUri)
            val prefs = context.playbackMemoryDataStore.data.first()
            prefs[prefKey]?.let { rawJson -> parseJson(videoId, videoUri, rawJson) }
        }

    suspend fun saveMemory(memory: VideoPlaybackMemory) = withContext(Dispatchers.IO) {
        val prefKey = keyForVideo(memory.videoId, memory.videoUri)
        val json = JSONObject().apply {
            put("videoId", memory.videoId)
            put("videoUri", memory.videoUri)
            put("resumePositionMs", memory.resumePositionMs)
            put("selectedAudioGroupIndex", memory.selectedAudioGroupIndex)
            put("selectedAudioTrackIndex", memory.selectedAudioTrackIndex)
            put("selectedSubtitleGroupIndex", memory.selectedSubtitleGroupIndex)
            put("selectedSubtitleTrackIndex", memory.selectedSubtitleTrackIndex)
            put("areSubtitlesEnabled", memory.areSubtitlesEnabled)
            put("subtitleSyncOffsetMs", memory.subtitleSyncOffsetMs)
            put("scaleMode", memory.scaleMode.name)
            put("externalSubtitlePath", memory.externalSubtitlePath ?: "")
            put("updatedAtMs", System.currentTimeMillis())
        }.toString()

        context.playbackMemoryDataStore.edit { prefs ->
            prefs[prefKey] = json
        }
    }

    private fun parseJson(
        fallbackId: Long,
        fallbackUri: String,
        rawJson: String
    ): VideoPlaybackMemory? {
        return try {
            val obj = JSONObject(rawJson)
            val scaleName = obj.optString("scaleMode", PlayerScaleMode.FIT.name)
            val resolvedScale = runCatching { PlayerScaleMode.valueOf(scaleName) }
                .getOrDefault(PlayerScaleMode.FIT)
            val extSub = obj.optString("externalSubtitlePath", "").takeIf { it.isNotBlank() }
            VideoPlaybackMemory(
                videoId = obj.optLong("videoId", fallbackId),
                videoUri = obj.optString("videoUri", fallbackUri),
                resumePositionMs = obj.optLong("resumePositionMs", 0L),
                selectedAudioGroupIndex = obj.optInt("selectedAudioGroupIndex", -1),
                selectedAudioTrackIndex = obj.optInt("selectedAudioTrackIndex", -1),
                selectedSubtitleGroupIndex = obj.optInt("selectedSubtitleGroupIndex", -1),
                selectedSubtitleTrackIndex = obj.optInt("selectedSubtitleTrackIndex", -1),
                areSubtitlesEnabled = obj.optBoolean("areSubtitlesEnabled", true),
                subtitleSyncOffsetMs = obj.optLong("subtitleSyncOffsetMs", 0L),
                scaleMode = resolvedScale,
                externalSubtitlePath = extSub,
                updatedAtMs = obj.optLong("updatedAtMs", 0L)
            )
        } catch (_: Exception) {
            null
        }
    }
}
