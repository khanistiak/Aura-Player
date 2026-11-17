package com.istiak.auraplayer

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "aura_player_settings")

enum class ThemeMode(val label: String, val fullLabel: String = label) {
    SYSTEM("System", "System"),
    LIGHT("Light", "Light"),
    DARK("Dark", "Dark")
}

enum class DecoderPriority(val label: String, val badgeText: String) {
    HW("HW decoder", "HW"),
    HW_PLUS("HW+ decoder", "HW+"),
    SW("SW decoder", "SW");

    companion object {
        fun fromStored(value: String?): DecoderPriority {
            return when (value) {
                "HW" -> HW
                "HW_PLUS" -> HW_PLUS
                "SW", "SOFTWARE" -> SW
                else -> HW_PLUS
            }
        }
    }
}

enum class ResumePlaybackMode(val label: String) {
    ASK("Ask"),
    RESUME("Resume"),
    START_OVER("Start Over");

    companion object {
        fun fromStored(value: String?, legacyDefaultResume: Boolean? = null): ResumePlaybackMode {
            val parsed = value?.let { stored -> entries.firstOrNull { it.name == stored } }
            if (parsed != null) return parsed
            return when (legacyDefaultResume) {
                true -> RESUME
                false -> START_OVER
                null -> RESUME
            }
        }
    }
}

enum class VideoOrientationMode(val label: String) {
    FREE("Free"),
    VIDEO("Video"),
    PORTRAIT("Portrait"),
    REVERSE_PORTRAIT("Reverse portrait"),
    SENSOR_PORTRAIT("Sensor portrait"),
    LANDSCAPE("Landscape"),
    REVERSE_LANDSCAPE("Reverse landscape"),
    SENSOR_LANDSCAPE("Sensor landscape");

    companion object {
        fun fromStored(value: String?): VideoOrientationMode {
            return entries.firstOrNull { it.name == value } ?: VIDEO
        }
    }
}

enum class SortField(val label: String) {
    TITLE("Title"),
    DATE("Date"),
    PLAYED_TIME("Played time"),
    STATUS("Status"),
    LENGTH("Length"),
    SIZE("Size"),
    RESOLUTION("Resolution"),
    PATH("Path"),
    FRAME_RATE("Frame rate"),
    TYPE("Type")
}

enum class LibraryViewMode(val label: String) {
    FOLDER("All folders"),
    TREE("Folders"),
    LIBRARY("Files")
}

enum class LibraryLayoutMode(val label: String) {
    LIST("List"),
    GRID("Grid")
}

enum class DisplayField(val label: String, val defaultEnabled: Boolean) {
    THUMBNAILS("Thumbnail", true),
    DURATION("Length", true),
    EXTENSION("File extension", false),
    PLAYED_TIME("Played time", false),
    RESOLUTION("Resolution", true),
    FRAMERATE("Frame rate", false),
    PATH("Path", false),
    SIZE("Size", true),
    DATE("Date", false),
    TOTAL_MEDIA("Total Media", true),
    FOLDER_SIZE("Folder Size", true),
    TOTAL_DURATION("Total Duration", true),
    CODEC_SUPPORT("Codec support", false),
    SUBTITLE("Subtitle", false),
    PROGRESS_BAR("Progress Bar", true);

    companion object {
        val defaultSet: Set<DisplayField> = entries.filter { it.defaultEnabled }.toSet()
    }
}

data class AppSettingsState(
    // Appearance & Library
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val showHiddenFolders: Boolean = false,
    val hiddenFolderScanLibrary: Boolean = true,
    val hiddenFolderScanTree: Boolean = true,
    val hiddenFolderScanFolders: Boolean = true,
    val recognizeNoMedia: Boolean = true,
    val scanDotFolders: Boolean = false,
    val markerFiles: Set<String> = setOf(".nomedia"),
    val minVideoDurationSec: Int = 0,
    val minFileSizeMb: Int = 0,
    val blacklistedFolders: Set<String> = emptySet(),
    val whitelistedFolders: Set<String> = emptySet(),
    val whitelistEnabled: Boolean = false,
    val displayLengthOverThumbnail: Boolean = true,
    val resumePlaybackMode: ResumePlaybackMode = ResumePlaybackMode.RESUME,

    // Playback & Decoder
    val videoOrientationMode: VideoOrientationMode = VideoOrientationMode.VIDEO,
    val autoRotateToVideoOrientation: Boolean = true,
    val decoderPriority: DecoderPriority = DecoderPriority.HW_PLUS,
    val backgroundAudioOrPipOnHome: Boolean = false,
    val autoPlayNextEpisode: Boolean = true,
    val showSkipIntroButton: Boolean = true,
    val doubleTapSeekSeconds: Int = 10,
    val defaultPlaybackSpeed: Float = 1.0f,

    // Gestures & Controls
    val hapticFeedbackEnabled: Boolean = true,
    val confirmBeforeLeavingPlayback: Boolean = false,
    val brightnessSwipeEnabled: Boolean = true,
    val volumeSwipeEnabled: Boolean = true,
    val horizontalSeekSwipeEnabled: Boolean = true,
    val pinchToZoomEnabled: Boolean = true,
    val pinchToZoomSubtitlesEnabled: Boolean = true,
    val twoFingerSwipeForSpeedEnabled: Boolean = true,
    val swipeSubtitlesToSeekEnabled: Boolean = true,
    val invertSwipeSubtitlesDirection: Boolean = false,
    val showSeekbarWhenControlsHidden: Boolean = false,
    val swipeUpCenterForPlaylistEnabled: Boolean = true,
    val horizontalSwipeSensitivity: Int = 50,
    val holdForSpeedMultiplier: Float = 2.0f,
    val doubleTapSeekAreaWidthPercent: Int = 35,
    val doubleTapLeftAction: String = "Seek",
    val doubleTapCenterAction: String = "Play/Pause",
    val doubleTapRightAction: String = "Seek",
    val useSingleTapForCenterGesture: Boolean = false,
    val holdAndDragSubtitlesEnabled: Boolean = true,
    val mediaControlPreviousAction: String = "Previous video",
    val mediaControlPlayPauseAction: String = "Toggle play/pause",
    val mediaControlNextAction: String = "Next video",

    // Privacy & Vault
    val biometricVaultLockEnabled: Boolean = true,
    val autoLockVaultOnMinimize: Boolean = true,
    val appDisguiseMode: AppDisguiseMode = AppDisguiseMode.DEFAULT,
    val hideSecureFolderSettings: Boolean = false,
    val vaultSecurityQuestion: String = "",
    val vaultSecurityAnswer: String = "",

    // Sort & View Options
    val sortField: SortField = SortField.TITLE,
    val sortAscending: Boolean = true,
    val viewMode: LibraryViewMode = LibraryViewMode.TREE,
    val layoutMode: LibraryLayoutMode = LibraryLayoutMode.LIST,
    val enabledFields: Set<DisplayField> = DisplayField.defaultSet,

    // Playback & Duration Tracking
    val lastPlayedVideoUri: String? = null,
    val watchedVideoIds: Set<Long> = emptySet(),
    val seenBaselineEpochSec: Long = 0L,
    val playbackPositionsMs: Map<Long, Long> = emptyMap(),
    val cachedDurationsMs: Map<Long, Long> = emptyMap(),
    val lastNotifiedUpdateTag: String = ""
) {
    val defaultResumePlayback: Boolean
        get() = resumePlaybackMode == ResumePlaybackMode.RESUME
}

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SHOW_HIDDEN_FOLDERS = booleanPreferencesKey("show_hidden_folders")
        val HIDDEN_SCAN_LIBRARY = booleanPreferencesKey("hidden_scan_library")
        val HIDDEN_SCAN_TREE = booleanPreferencesKey("hidden_scan_tree")
        val HIDDEN_SCAN_FOLDERS = booleanPreferencesKey("hidden_scan_folders")
        val RECOGNIZE_NOMEDIA = booleanPreferencesKey("recognize_nomedia")
        val SCAN_DOT_FOLDERS = booleanPreferencesKey("scan_dot_folders")
        val MARKER_FILES = stringSetPreferencesKey("marker_files")
        val MIN_VIDEO_DURATION_SEC = intPreferencesKey("min_video_duration_sec")
        val MIN_FILE_SIZE_MB = intPreferencesKey("min_file_size_mb")
        val BLACKLISTED_FOLDERS = stringSetPreferencesKey("blacklisted_folders")
        val WHITELISTED_FOLDERS = stringSetPreferencesKey("whitelisted_folders")
        val WHITELIST_ENABLED = booleanPreferencesKey("whitelist_enabled")
        val DISPLAY_LENGTH_OVER_THUMB = booleanPreferencesKey("display_length_over_thumb")
        val DEFAULT_RESUME_PLAYBACK = booleanPreferencesKey("default_resume_playback")
        val RESUME_PLAYBACK_MODE = stringPreferencesKey("resume_playback_mode")

        val VIDEO_ORIENTATION_MODE = stringPreferencesKey("video_orientation_mode")
        val AUTO_ROTATE_TO_VIDEO = booleanPreferencesKey("auto_rotate_to_video")
        val DECODER_PRIORITY = stringPreferencesKey("decoder_priority")
        val BG_AUDIO_OR_PIP = booleanPreferencesKey("bg_audio_or_pip")
        val AUTO_PLAY_NEXT_EPISODE = booleanPreferencesKey("auto_play_next_episode")
        val SHOW_SKIP_INTRO_BUTTON = booleanPreferencesKey("show_skip_intro_button")
        val DOUBLE_TAP_SEEK_SEC = intPreferencesKey("double_tap_seek_sec")
        val DEFAULT_PLAYBACK_SPEED = floatPreferencesKey("default_playback_speed")

        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val CONFIRM_BEFORE_LEAVING = booleanPreferencesKey("confirm_before_leaving")
        val BRIGHTNESS_SWIPE = booleanPreferencesKey("brightness_swipe")
        val VOLUME_SWIPE = booleanPreferencesKey("volume_swipe")
        val HORIZONTAL_SEEK_SWIPE = booleanPreferencesKey("horizontal_seek_swipe")
        val PINCH_TO_ZOOM = booleanPreferencesKey("pinch_to_zoom")
        val PINCH_TO_ZOOM_SUBTITLES = booleanPreferencesKey("pinch_to_zoom_subtitles")
        val TWO_FINGER_SWIPE_SPEED = booleanPreferencesKey("two_finger_swipe_speed")
        val SWIPE_SUBTITLES_TO_SEEK = booleanPreferencesKey("swipe_subtitles_to_seek")
        val INVERT_SWIPE_SUBTITLES = booleanPreferencesKey("invert_swipe_subtitles")
        val SHOW_SEEKBAR_HIDDEN = booleanPreferencesKey("show_seekbar_hidden")
        val SWIPE_UP_CENTER_PLAYLIST = booleanPreferencesKey("swipe_up_center_playlist")
        val HORIZONTAL_SWIPE_SENSITIVITY = intPreferencesKey("horizontal_swipe_sensitivity")
        val HOLD_FOR_SPEED_MULTIPLIER = floatPreferencesKey("hold_for_speed_multiplier")
        val DOUBLE_TAP_AREA_WIDTH = intPreferencesKey("double_tap_area_width")
        val DOUBLE_TAP_LEFT_ACTION = stringPreferencesKey("double_tap_left_action")
        val DOUBLE_TAP_CENTER_ACTION = stringPreferencesKey("double_tap_center_action")
        val DOUBLE_TAP_RIGHT_ACTION = stringPreferencesKey("double_tap_right_action")
        val SINGLE_TAP_CENTER_GESTURE = booleanPreferencesKey("single_tap_center_gesture")
        val HOLD_DRAG_SUBTITLES = booleanPreferencesKey("hold_drag_subtitles")
        val MEDIA_CTRL_PREVIOUS = stringPreferencesKey("media_ctrl_previous")
        val MEDIA_CTRL_PLAY_PAUSE = stringPreferencesKey("media_ctrl_play_pause")
        val MEDIA_CTRL_NEXT = stringPreferencesKey("media_ctrl_next")

        val BIOMETRIC_VAULT_LOCK = booleanPreferencesKey("biometric_vault_lock")
        val AUTO_LOCK_VAULT_MINIMIZE = booleanPreferencesKey("auto_lock_vault_minimize")
        val APP_DISGUISE_MODE = stringPreferencesKey("app_disguise_mode")
        val HIDE_SECURE_FOLDER_SETTINGS = booleanPreferencesKey("hide_secure_folder_settings")
        val VAULT_SECURITY_QUESTION = stringPreferencesKey("vault_security_question")
        val VAULT_SECURITY_ANSWER = stringPreferencesKey("vault_security_answer")

        val SORT_FIELD = stringPreferencesKey("sort_field")
        val SORT_ASCENDING = booleanPreferencesKey("sort_ascending")
        val VIEW_MODE = stringPreferencesKey("view_mode")
        val LAYOUT_MODE = stringPreferencesKey("layout_mode")
        val ENABLED_FIELDS = stringSetPreferencesKey("enabled_fields")

        val LAST_PLAYED_URI = stringPreferencesKey("last_played_uri")
        val WATCHED_VIDEO_IDS = stringSetPreferencesKey("watched_video_ids")
        val SEEN_BASELINE_EPOCH_SEC = longPreferencesKey("seen_baseline_epoch_sec")
        val PLAYBACK_POSITIONS = stringSetPreferencesKey("playback_positions")
        val CACHED_DURATIONS = stringSetPreferencesKey("cached_durations")
        val LAST_NOTIFIED_UPDATE_TAG = stringPreferencesKey("last_notified_update_tag")
    }

    val settingsFlow: Flow<AppSettingsState> = context.appDataStore.data.map { prefs ->
        val themeMode = prefs[Keys.THEME_MODE]?.let {
            runCatching { ThemeMode.valueOf(it) }.getOrNull()
        } ?: ThemeMode.LIGHT

        val decoderPriority = DecoderPriority.fromStored(prefs[Keys.DECODER_PRIORITY])
        val appDisguiseMode = AppDisguiseMode.fromStored(prefs[Keys.APP_DISGUISE_MODE])
        val legacyAutoRotate = prefs[Keys.AUTO_ROTATE_TO_VIDEO] ?: true
        val videoOrientationMode = prefs[Keys.VIDEO_ORIENTATION_MODE]?.let {
            VideoOrientationMode.fromStored(it)
        } ?: if (legacyAutoRotate) VideoOrientationMode.VIDEO else VideoOrientationMode.PORTRAIT

        val sortField = prefs[Keys.SORT_FIELD]?.let {
            runCatching { SortField.valueOf(it) }.getOrNull()
        } ?: SortField.TITLE

        val viewMode = prefs[Keys.VIEW_MODE]?.let {
            runCatching { LibraryViewMode.valueOf(it) }.getOrNull()
        } ?: LibraryViewMode.TREE

        val layoutMode = prefs[Keys.LAYOUT_MODE]?.let {
            runCatching { LibraryLayoutMode.valueOf(it) }.getOrNull()
        } ?: LibraryLayoutMode.LIST

        val enabledFields = prefs[Keys.ENABLED_FIELDS]?.mapNotNull { name ->
            runCatching { DisplayField.valueOf(name) }.getOrNull()
        }?.toSet() ?: DisplayField.defaultSet

        val watchedIds = prefs[Keys.WATCHED_VIDEO_IDS]?.mapNotNull { it.toLongOrNull() }?.toSet()
            ?: emptySet()

        val positionsMap = parseLongPairMap(prefs[Keys.PLAYBACK_POSITIONS])
        val cachedDurationsMap = parseLongPairMap(prefs[Keys.CACHED_DURATIONS])

        val storedMarkers = prefs[Keys.MARKER_FILES]
        val markerFiles = if (storedMarkers == null) setOf(".nomedia") else storedMarkers

        AppSettingsState(
            themeMode = themeMode,
            showHiddenFolders = prefs[Keys.SHOW_HIDDEN_FOLDERS] ?: false,
            hiddenFolderScanLibrary = prefs[Keys.HIDDEN_SCAN_LIBRARY] ?: true,
            hiddenFolderScanTree = prefs[Keys.HIDDEN_SCAN_TREE] ?: true,
            hiddenFolderScanFolders = prefs[Keys.HIDDEN_SCAN_FOLDERS] ?: true,
            recognizeNoMedia = prefs[Keys.RECOGNIZE_NOMEDIA] ?: true,
            scanDotFolders = prefs[Keys.SCAN_DOT_FOLDERS] ?: false,
            markerFiles = markerFiles,
            minVideoDurationSec = prefs[Keys.MIN_VIDEO_DURATION_SEC] ?: 0,
            minFileSizeMb = prefs[Keys.MIN_FILE_SIZE_MB] ?: 0,
            blacklistedFolders = prefs[Keys.BLACKLISTED_FOLDERS] ?: emptySet(),
            whitelistedFolders = prefs[Keys.WHITELISTED_FOLDERS] ?: emptySet(),
            whitelistEnabled = prefs[Keys.WHITELIST_ENABLED] ?: false,
            displayLengthOverThumbnail = prefs[Keys.DISPLAY_LENGTH_OVER_THUMB] ?: true,
            resumePlaybackMode = ResumePlaybackMode.fromStored(
                value = prefs[Keys.RESUME_PLAYBACK_MODE],
                legacyDefaultResume = prefs[Keys.DEFAULT_RESUME_PLAYBACK]
            ),
            videoOrientationMode = videoOrientationMode,
            autoRotateToVideoOrientation = videoOrientationMode == VideoOrientationMode.VIDEO ||
                videoOrientationMode == VideoOrientationMode.FREE,
            decoderPriority = decoderPriority,
            backgroundAudioOrPipOnHome = prefs[Keys.BG_AUDIO_OR_PIP] ?: false,
            autoPlayNextEpisode = prefs[Keys.AUTO_PLAY_NEXT_EPISODE] ?: true,
            showSkipIntroButton = prefs[Keys.SHOW_SKIP_INTRO_BUTTON] ?: true,
            doubleTapSeekSeconds = prefs[Keys.DOUBLE_TAP_SEEK_SEC] ?: 10,
            defaultPlaybackSpeed = prefs[Keys.DEFAULT_PLAYBACK_SPEED] ?: 1.0f,
            hapticFeedbackEnabled = prefs[Keys.HAPTIC_FEEDBACK] ?: true,
            confirmBeforeLeavingPlayback = prefs[Keys.CONFIRM_BEFORE_LEAVING] ?: false,
            brightnessSwipeEnabled = prefs[Keys.BRIGHTNESS_SWIPE] ?: true,
            volumeSwipeEnabled = prefs[Keys.VOLUME_SWIPE] ?: true,
            horizontalSeekSwipeEnabled = prefs[Keys.HORIZONTAL_SEEK_SWIPE] ?: true,
            pinchToZoomEnabled = prefs[Keys.PINCH_TO_ZOOM] ?: true,
            pinchToZoomSubtitlesEnabled = prefs[Keys.PINCH_TO_ZOOM_SUBTITLES] ?: true,
            twoFingerSwipeForSpeedEnabled = prefs[Keys.TWO_FINGER_SWIPE_SPEED] ?: true,
            swipeSubtitlesToSeekEnabled = prefs[Keys.SWIPE_SUBTITLES_TO_SEEK] ?: true,
            invertSwipeSubtitlesDirection = prefs[Keys.INVERT_SWIPE_SUBTITLES] ?: false,
            showSeekbarWhenControlsHidden = prefs[Keys.SHOW_SEEKBAR_HIDDEN] ?: false,
            swipeUpCenterForPlaylistEnabled = prefs[Keys.SWIPE_UP_CENTER_PLAYLIST] ?: true,
            horizontalSwipeSensitivity = (prefs[Keys.HORIZONTAL_SWIPE_SENSITIVITY] ?: 50).coerceIn(10, 100),
            holdForSpeedMultiplier = (prefs[Keys.HOLD_FOR_SPEED_MULTIPLIER] ?: 2.0f).coerceIn(1.5f, 3.0f),
            doubleTapSeekAreaWidthPercent = (prefs[Keys.DOUBLE_TAP_AREA_WIDTH] ?: 35).coerceIn(15, 45),
            doubleTapLeftAction = prefs[Keys.DOUBLE_TAP_LEFT_ACTION] ?: "Seek",
            doubleTapCenterAction = prefs[Keys.DOUBLE_TAP_CENTER_ACTION] ?: "Play/Pause",
            doubleTapRightAction = prefs[Keys.DOUBLE_TAP_RIGHT_ACTION] ?: "Seek",
            useSingleTapForCenterGesture = prefs[Keys.SINGLE_TAP_CENTER_GESTURE] ?: false,
            holdAndDragSubtitlesEnabled = prefs[Keys.HOLD_DRAG_SUBTITLES] ?: true,
            mediaControlPreviousAction = prefs[Keys.MEDIA_CTRL_PREVIOUS] ?: "Previous video",
            mediaControlPlayPauseAction = prefs[Keys.MEDIA_CTRL_PLAY_PAUSE] ?: "Toggle play/pause",
            mediaControlNextAction = prefs[Keys.MEDIA_CTRL_NEXT] ?: "Next video",
            biometricVaultLockEnabled = prefs[Keys.BIOMETRIC_VAULT_LOCK] ?: true,
            autoLockVaultOnMinimize = prefs[Keys.AUTO_LOCK_VAULT_MINIMIZE] ?: true,
            appDisguiseMode = appDisguiseMode,
            hideSecureFolderSettings = prefs[Keys.HIDE_SECURE_FOLDER_SETTINGS] ?: false,
            vaultSecurityQuestion = prefs[Keys.VAULT_SECURITY_QUESTION] ?: "",
            vaultSecurityAnswer = prefs[Keys.VAULT_SECURITY_ANSWER] ?: "",
            sortField = sortField,
            sortAscending = prefs[Keys.SORT_ASCENDING] ?: true,
            viewMode = viewMode,
            layoutMode = layoutMode,
            enabledFields = enabledFields,
            lastPlayedVideoUri = prefs[Keys.LAST_PLAYED_URI],
            watchedVideoIds = watchedIds,
            seenBaselineEpochSec = prefs[Keys.SEEN_BASELINE_EPOCH_SEC] ?: 0L,
            playbackPositionsMs = positionsMap,
            cachedDurationsMs = cachedDurationsMap,
            lastNotifiedUpdateTag = prefs[Keys.LAST_NOTIFIED_UPDATE_TAG] ?: ""
        )
    }

    private fun parseLongPairMap(entries: Set<String>?): Map<Long, Long> {
        val map = mutableMapOf<Long, Long>()
        entries?.forEach { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                val id = parts[0].toLongOrNull()
                val value = parts[1].toLongOrNull()
                if (id != null && value != null) {
                    map[id] = value
                }
            }
        }
        return map
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.appDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setShowHiddenFolders(show: Boolean) {
        context.appDataStore.edit { it[Keys.SHOW_HIDDEN_FOLDERS] = show }
    }

    suspend fun setHiddenFolderScanTarget(library: Boolean, tree: Boolean, folders: Boolean) {
        context.appDataStore.edit {
            it[Keys.HIDDEN_SCAN_LIBRARY] = library
            it[Keys.HIDDEN_SCAN_TREE] = tree
            it[Keys.HIDDEN_SCAN_FOLDERS] = folders
        }
    }

    suspend fun setRecognizeNoMedia(enabled: Boolean) {
        context.appDataStore.edit { it[Keys.RECOGNIZE_NOMEDIA] = enabled }
    }

    suspend fun setScanDotFolders(enabled: Boolean) {
        context.appDataStore.edit { it[Keys.SCAN_DOT_FOLDERS] = enabled }
    }

    suspend fun setMarkerFiles(markers: Set<String>) {
        context.appDataStore.edit { it[Keys.MARKER_FILES] = markers }
    }

    suspend fun setMinVideoDurationSec(seconds: Int) {
        context.appDataStore.edit { it[Keys.MIN_VIDEO_DURATION_SEC] = seconds }
    }

    suspend fun setMinFileSizeMb(mb: Int) {
        context.appDataStore.edit { it[Keys.MIN_FILE_SIZE_MB] = mb }
    }

    suspend fun setBlacklistedFolders(folders: Set<String>) {
        context.appDataStore.edit { it[Keys.BLACKLISTED_FOLDERS] = folders }
    }

    suspend fun setWhitelistedFolders(folders: Set<String>, enabled: Boolean) {
        context.appDataStore.edit {
            it[Keys.WHITELISTED_FOLDERS] = folders
            it[Keys.WHITELIST_ENABLED] = enabled
        }
    }

    suspend fun setResumePlaybackMode(mode: ResumePlaybackMode) {
        context.appDataStore.edit {
            it[Keys.RESUME_PLAYBACK_MODE] = mode.name
            it[Keys.DEFAULT_RESUME_PLAYBACK] = (mode == ResumePlaybackMode.RESUME)
        }
    }

    suspend fun setVideoOrientationMode(mode: VideoOrientationMode) {
        context.appDataStore.edit {
            it[Keys.VIDEO_ORIENTATION_MODE] = mode.name
            it[Keys.AUTO_ROTATE_TO_VIDEO] = (mode == VideoOrientationMode.VIDEO || mode == VideoOrientationMode.FREE)
        }
    }

    suspend fun setAutoRotateToVideoOrientation(enabled: Boolean) {
        context.appDataStore.edit {
            it[Keys.AUTO_ROTATE_TO_VIDEO] = enabled
            it[Keys.VIDEO_ORIENTATION_MODE] = if (enabled) {
                VideoOrientationMode.VIDEO.name
            } else {
                VideoOrientationMode.PORTRAIT.name
            }
        }
    }

    suspend fun setDecoderPriority(priority: DecoderPriority) {
        context.appDataStore.edit { it[Keys.DECODER_PRIORITY] = priority.name }
    }

    suspend fun setBackgroundAudioOrPipOnHome(enabled: Boolean) {
        context.appDataStore.edit { it[Keys.BG_AUDIO_OR_PIP] = enabled }
    }

    suspend fun setAutoPlayNextEpisode(enabled: Boolean) {
        context.appDataStore.edit { it[Keys.AUTO_PLAY_NEXT_EPISODE] = enabled }
    }

    suspend fun setShowSkipIntroButton(enabled: Boolean) {
        context.appDataStore.edit { it[Keys.SHOW_SKIP_INTRO_BUTTON] = enabled }
    }

    suspend fun setDoubleTapSeekSeconds(seconds: Int) {
        context.appDataStore.edit { it[Keys.DOUBLE_TAP_SEEK_SEC] = seconds }
    }

    suspend fun setDefaultPlaybackSpeed(speed: Float) {
        context.appDataStore.edit { it[Keys.DEFAULT_PLAYBACK_SPEED] = speed }
    }

    suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        context.appDataStore.edit { it[Keys.HAPTIC_FEEDBACK] = enabled }
    }

    suspend fun setConfirmBeforeLeavingPlayback(enabled: Boolean) {
        context.appDataStore.edit { it[Keys.CONFIRM_BEFORE_LEAVING] = enabled }
    }

    suspend fun setBrightnessSwipeEnabled(enabled: Boolean) {
        context.appDataStore.edit { it[Keys.BRIGHTNESS_SWIPE] = enabled }
    }

    suspend fun setVolumeSwipeEnabled(enabled: Boolean) {
        context.appDataStore.edit { it[Keys.VOLUME_SWIPE] = enabled }
    }

    suspend fun setHorizontalSeekSwipeEnabled(enabled: Boolean) {
        context.appDataStore.edit { it[Keys.HORIZONTAL_SEEK_SWIPE] = enabled }
    }

    suspend fun setPinchToZoomEnabled(enabled: Boolean) {
        context.appDataStore.edit { it[Keys.PINCH_TO_ZOOM] = enabled }
    }

    suspend fun setPinchToZoomSubtitlesEnabled(enabled: Boolean) {
        context.appDataStore.edit { it[Keys.PINCH_TO_ZOOM_SUBTITLES] = enabled }
    }

    suspend fun setTwoFingerSwipeForSpeedEnabled(enabled: Boolean) {
        context.appDataStore.edit { it[Keys.TWO_FINGER_SWIPE_SPEED] = enabled }
    }

    suspend fun setSwipeSubtitlesToSeekEnabled(enabled: Boolean) {
        context.appDataStore.edit { it[Keys.SWIPE_SUBTITLES_TO_SEEK] = enabled }
    }

    suspend fun setInvertSwipeSubtitlesDirection(enabled: Boolean) {
        context.appDataStore.edit { it[Keys.INVERT_SWIPE_SUBTITLES] = enabled }
    }

    suspend fun setShowSeekbarWhenControlsHidden(enabled: Boolean) {
        context.appDataStore.edit { it[Keys.SHOW_SEEKBAR_HIDDEN] = enabled }
    }

    suspend fun setSwipeUpCenterForPlaylistEnabled(enabled: Boolean) {
        context.appDataStore.edit { it[Keys.SWIPE_UP_CENTER_PLAYLIST] = enabled }
    }

    suspend fun setHorizontalSwipeSensitivity(sensitivity: Int) {
        context.appDataStore.edit { it[Keys.HORIZONTAL_SWIPE_SENSITIVITY] = sensitivity.coerceIn(10, 100) }
    }

    suspend fun setHoldForSpeedMultiplier(multiplier: Float) {
        context.appDataStore.edit { it[Keys.HOLD_FOR_SPEED_MULTIPLIER] = multiplier.coerceIn(1.5f, 3.0f) }
    }

    suspend fun setDoubleTapSeekAreaWidthPercent(percent: Int) {
        context.appDataStore.edit { it[Keys.DOUBLE_TAP_AREA_WIDTH] = percent.coerceIn(15, 45) }
    }

    suspend fun setDoubleTapLeftAction(action: String) {
        context.appDataStore.edit { it[Keys.DOUBLE_TAP_LEFT_ACTION] = action }
    }

    suspend fun setDoubleTapCenterAction(action: String) {
        context.appDataStore.edit { it[Keys.DOUBLE_TAP_CENTER_ACTION] = action }
    }

    suspend fun setDoubleTapRightAction(action: String) {
        context.appDataStore.edit { it[Keys.DOUBLE_TAP_RIGHT_ACTION] = action }
    }

    suspend fun setUseSingleTapForCenterGesture(enabled: Boolean) {
        context.appDataStore.edit { it[Keys.SINGLE_TAP_CENTER_GESTURE] = enabled }
    }

    suspend fun setHoldAndDragSubtitlesEnabled(enabled: Boolean) {
        context.appDataStore.edit { it[Keys.HOLD_DRAG_SUBTITLES] = enabled }
    }

    suspend fun setMediaControlPreviousAction(action: String) {
        context.appDataStore.edit { it[Keys.MEDIA_CTRL_PREVIOUS] = action }
    }

    suspend fun setMediaControlPlayPauseAction(action: String) {
        context.appDataStore.edit { it[Keys.MEDIA_CTRL_PLAY_PAUSE] = action }
    }

    suspend fun setMediaControlNextAction(action: String) {
        context.appDataStore.edit { it[Keys.MEDIA_CTRL_NEXT] = action }
    }

    suspend fun setBiometricVaultLockEnabled(enabled: Boolean) {
        context.appDataStore.edit { it[Keys.BIOMETRIC_VAULT_LOCK] = enabled }
    }

    suspend fun setAutoLockVaultOnMinimize(enabled: Boolean) {
        context.appDataStore.edit { it[Keys.AUTO_LOCK_VAULT_MINIMIZE] = enabled }
    }

    suspend fun setAppDisguiseMode(mode: AppDisguiseMode) {
        context.appDataStore.edit { it[Keys.APP_DISGUISE_MODE] = mode.name }
    }

    suspend fun setHideSecureFolderSettings(hidden: Boolean) {
        context.appDataStore.edit { it[Keys.HIDE_SECURE_FOLDER_SETTINGS] = hidden }
    }

    suspend fun setVaultSecurityQuestion(question: String, answer: String) {
        context.appDataStore.edit {
            it[Keys.VAULT_SECURITY_QUESTION] = question.trim()
            it[Keys.VAULT_SECURITY_ANSWER] = answer.trim()
        }
    }

    suspend fun setSortField(field: SortField) {
        context.appDataStore.edit { it[Keys.SORT_FIELD] = field.name }
    }

    suspend fun setSortAscending(ascending: Boolean) {
        context.appDataStore.edit { it[Keys.SORT_ASCENDING] = ascending }
    }

    suspend fun setViewMode(mode: LibraryViewMode) {
        context.appDataStore.edit { it[Keys.VIEW_MODE] = mode.name }
    }

    suspend fun setLayoutMode(mode: LibraryLayoutMode) {
        context.appDataStore.edit { it[Keys.LAYOUT_MODE] = mode.name }
    }

    suspend fun toggleDisplayField(field: DisplayField) {
        context.appDataStore.edit { prefs ->
            val current = prefs[Keys.ENABLED_FIELDS]?.mapNotNull {
                runCatching { DisplayField.valueOf(it) }.getOrNull()
            }?.toMutableSet() ?: DisplayField.defaultSet.toMutableSet()

            if (current.contains(field)) {
                current.remove(field)
            } else {
                current.add(field)
            }
            prefs[Keys.ENABLED_FIELDS] = current.map { it.name }.toSet()
        }
    }

    suspend fun applySortViewOptions(
        sortField: SortField,
        sortAscending: Boolean,
        viewMode: LibraryViewMode,
        layoutMode: LibraryLayoutMode,
        enabledFields: Set<DisplayField>,
        displayLengthOverThumbnail: Boolean,
        showHiddenFolders: Boolean,
        recognizeNoMedia: Boolean
    ) {
        context.appDataStore.edit { prefs ->
            prefs[Keys.SORT_FIELD] = sortField.name
            prefs[Keys.SORT_ASCENDING] = sortAscending
            prefs[Keys.VIEW_MODE] = viewMode.name
            prefs[Keys.LAYOUT_MODE] = layoutMode.name
            prefs[Keys.ENABLED_FIELDS] = enabledFields.map { it.name }.toSet()
            prefs[Keys.DISPLAY_LENGTH_OVER_THUMB] = displayLengthOverThumbnail
            prefs[Keys.SHOW_HIDDEN_FOLDERS] = showHiddenFolders
            prefs[Keys.RECOGNIZE_NOMEDIA] = recognizeNoMedia
        }
    }

    suspend fun putCachedDurations(newDurations: Map<Long, Long>) {
        if (newDurations.isEmpty()) return
        context.appDataStore.edit { prefs ->
            val current = parseLongPairMap(prefs[Keys.CACHED_DURATIONS]).toMutableMap()
            current.putAll(newDurations)
            prefs[Keys.CACHED_DURATIONS] = current.entries.map { "${it.key}:${it.value}" }.toSet()
        }
    }

    suspend fun clearCachedDurations() {
        context.appDataStore.edit { prefs ->
            prefs.remove(Keys.CACHED_DURATIONS)
        }
    }

    suspend fun recordVideoPlayback(videoId: Long, videoUri: String, positionMs: Long) {
        context.appDataStore.edit { prefs ->
            prefs[Keys.LAST_PLAYED_URI] = videoUri

            val watched = prefs[Keys.WATCHED_VIDEO_IDS]?.toMutableSet() ?: mutableSetOf()
            watched.add(videoId.toString())
            prefs[Keys.WATCHED_VIDEO_IDS] = watched

            val posEntries = prefs[Keys.PLAYBACK_POSITIONS]?.toMutableSet() ?: mutableSetOf()
            posEntries.removeAll { it.startsWith("$videoId:") }
            posEntries.add("$videoId:$positionMs")
            prefs[Keys.PLAYBACK_POSITIONS] = posEntries
        }
    }

    suspend fun markAllVideosAsSeen(
        allVideoIds: Collection<Long>,
        baselineEpochSec: Long = System.currentTimeMillis() / 1000L
    ) {
        context.appDataStore.edit { prefs ->
            prefs[Keys.SEEN_BASELINE_EPOCH_SEC] = baselineEpochSec
            val watched = prefs[Keys.WATCHED_VIDEO_IDS]?.toMutableSet() ?: mutableSetOf()
            allVideoIds.forEach { watched.add(it.toString()) }
            prefs[Keys.WATCHED_VIDEO_IDS] = watched
        }
    }

    suspend fun setLastNotifiedUpdateTag(tagName: String) {
        context.appDataStore.edit { prefs ->
            prefs[Keys.LAST_NOTIFIED_UPDATE_TAG] = tagName.trim()
        }
    }
}
