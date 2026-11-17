package com.istiak.auraplayer

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class PlaylistItem(
    val id: String,
    val name: String,
    val videoIds: List<Long> = emptyList()
) {
    val count: Int get() = videoIds.size
}

/**
 * File-backed Playlist Repository persisting all playlists (including the default `Favorites`
 * playlist) to `context.filesDir/playlists.json` with shared in-process reactive updates.
 */
class PlaylistRepository(private val context: Context) {

    companion object {
        const val FAVORITES_ID = "favorites"
        private const val FILE_NAME = "playlists.json"

        private val fileFlows = ConcurrentHashMap<String, MutableStateFlow<List<PlaylistItem>>>()
        private val fileMutexes = ConcurrentHashMap<String, Mutex>()

        private fun getMutex(path: String): Mutex =
            fileMutexes.computeIfAbsent(path) { Mutex() }
    }

    private val playlistsFile: File
        get() = File(context.applicationContext.filesDir, FILE_NAME)

    private val stateFlow: MutableStateFlow<List<PlaylistItem>>
        get() {
            val file = playlistsFile
            return fileFlows.computeIfAbsent(file.absolutePath) {
                MutableStateFlow(readOrInitPlaylistsSync(file))
            }
        }

    val playlistsFlow: Flow<List<PlaylistItem>>
        get() = stateFlow.asStateFlow()

    fun getCurrentPlaylists(): List<PlaylistItem> = stateFlow.value

    suspend fun reloadFromDisk(): List<PlaylistItem> = withContext(Dispatchers.IO) {
        val file = playlistsFile
        getMutex(file.absolutePath).withLock {
            val loaded = readOrInitPlaylistsSync(file)
            stateFlow.value = loaded
            loaded
        }
    }

    suspend fun createPlaylist(name: String): PlaylistItem = withContext(Dispatchers.IO) {
        val cleanName = name.trim().ifEmpty { "New Playlist" }
        val file = playlistsFile
        getMutex(file.absolutePath).withLock {
            val current = readOrInitPlaylistsSync(file).toMutableList()
            val existing = current.find { it.name.equals(cleanName, ignoreCase = true) }
            if (existing != null) {
                stateFlow.value = current
                return@withLock existing
            }
            val newPlaylist = PlaylistItem(
                id = "pl_${UUID.randomUUID().toString().take(8)}",
                name = cleanName,
                videoIds = emptyList()
            )
            current.add(newPlaylist)
            writePlaylistsSync(file, current)
            stateFlow.value = current
            newPlaylist
        }
    }

    suspend fun renamePlaylist(playlistId: String, newName: String) = withContext(Dispatchers.IO) {
        val cleanName = newName.trim()
        if (cleanName.isEmpty() || playlistId == FAVORITES_ID) return@withContext
        val file = playlistsFile
        getMutex(file.absolutePath).withLock {
            val updated = readOrInitPlaylistsSync(file).map { playlist ->
                if (playlist.id == playlistId) playlist.copy(name = cleanName) else playlist
            }
            writePlaylistsSync(file, updated)
            stateFlow.value = updated
        }
    }

    suspend fun deletePlaylist(playlistId: String) = withContext(Dispatchers.IO) {
        if (playlistId == FAVORITES_ID) return@withContext
        val file = playlistsFile
        getMutex(file.absolutePath).withLock {
            val updated = readOrInitPlaylistsSync(file).filterNot { it.id == playlistId }
            writePlaylistsSync(file, updated)
            stateFlow.value = updated
        }
    }

    suspend fun addVideosToPlaylists(playlistIds: Set<String>, videoIds: List<Long>) = withContext(Dispatchers.IO) {
        if (playlistIds.isEmpty() || videoIds.isEmpty()) return@withContext
        val file = playlistsFile
        getMutex(file.absolutePath).withLock {
            val updated = readOrInitPlaylistsSync(file).map { playlist ->
                if (playlist.id in playlistIds) {
                    val merged = (playlist.videoIds + videoIds).distinct()
                    playlist.copy(videoIds = merged)
                } else {
                    playlist
                }
            }
            writePlaylistsSync(file, updated)
            stateFlow.value = updated
        }
    }

    suspend fun removeVideoFromPlaylist(playlistId: String, videoId: Long) = withContext(Dispatchers.IO) {
        val file = playlistsFile
        getMutex(file.absolutePath).withLock {
            val updated = readOrInitPlaylistsSync(file).map { playlist ->
                if (playlist.id == playlistId) {
                    playlist.copy(videoIds = playlist.videoIds.filterNot { it == videoId })
                } else {
                    playlist
                }
            }
            writePlaylistsSync(file, updated)
            stateFlow.value = updated
        }
    }

    private fun readOrInitPlaylistsSync(file: File): List<PlaylistItem> {
        val rawText = if (file.exists()) {
            runCatching { file.readText() }.getOrNull()
        } else {
            null
        }
        val parsed = parsePlaylists(rawText)
        if (!file.exists()) {
            writePlaylistsSync(file, parsed)
        }
        return parsed
    }

    private fun writePlaylistsSync(file: File, playlists: List<PlaylistItem>) {
        runCatching {
            file.parentFile?.mkdirs()
            val serialized = serializePlaylists(playlists)
            file.writeText(serialized)
        }
    }

    private fun parsePlaylists(jsonStr: String?): List<PlaylistItem> {
        val list = mutableListOf<PlaylistItem>()
        if (!jsonStr.isNullOrBlank()) {
            try {
                val arr = JSONArray(jsonStr)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val id = obj.optString("id", "").ifBlank { continue }
                    val name = obj.optString("name", "Playlist")
                    val idsArr = obj.optJSONArray("videoIds")
                    val videoIds = mutableListOf<Long>()
                    if (idsArr != null) {
                        for (j in 0 until idsArr.length()) {
                            videoIds.add(idsArr.getLong(j))
                        }
                    }
                    list.add(PlaylistItem(id = id, name = name, videoIds = videoIds.distinct()))
                }
            } catch (_: Exception) {
            }
        }
        if (list.none { it.id == FAVORITES_ID }) {
            list.add(0, PlaylistItem(id = FAVORITES_ID, name = "Favorites", videoIds = emptyList()))
        }
        return list
    }

    private fun serializePlaylists(playlists: List<PlaylistItem>): String {
        val arr = JSONArray()
        for (item in playlists) {
            val idsArr = JSONArray()
            item.videoIds.forEach { idsArr.put(it) }
            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("videoIds", idsArr)
            }
            arr.put(obj)
        }
        return arr.toString(2)
    }
}
