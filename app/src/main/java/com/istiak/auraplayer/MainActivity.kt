package com.istiak.auraplayer

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Rational
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.rememberGraphicsLayer
import coil.Coil
import coil.annotation.ExperimentalCoilApi
import com.istiak.auraplayer.ui.components.FrostedBackdropState
import com.istiak.auraplayer.ui.components.LocalFrostedBackdropState
import com.istiak.auraplayer.ui.theme.AuraPlayerTheme
import com.istiak.auraplayer.ui.theme.LocalExtendedPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class ActiveOverlayScreen {
    NONE,
    VAULT,
    SETTINGS,
    APPEARANCE_SETTINGS,
    FOLDER_SETTINGS,
    PLAYER_SETTINGS,
    GESTURE_SETTINGS,
    PRIVATE_FOLDER_SETTINGS,
    ABOUT,
    CODEC_DIAGNOSTICS,
    MEDIA_INFO
}

enum class FolderPickerMode {
    MOVE,
    COPY
}

/**
 * MainActivity extends FragmentActivity to support androidx.biometric.BiometricPrompt
 * alongside Jetpack Compose, DataStore Settings, hierarchical MediaStore Tree View, and Media3 ExoPlayer.
 */
class MainActivity : FragmentActivity() {

    private var shouldEnterPipOnUserLeave: Boolean = false
    private val showUpdateSheetRequestState = mutableStateOf(false)

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (shouldEnterPipOnUserLeave && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_SHOW_UPDATE_SHEET, false)) {
            showUpdateSheetRequestState.value = true
            intent.removeExtra(EXTRA_SHOW_UPDATE_SHEET)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_AuraPlayer_Starting)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (intent?.getBooleanExtra(EXTRA_SHOW_UPDATE_SHEET, false) == true) {
            showUpdateSheetRequestState.value = true
            intent?.removeExtra(EXTRA_SHOW_UPDATE_SHEET)
        }

        val externalVideoUri: Uri? = if (intent?.action == Intent.ACTION_VIEW) {
            intent.data
        } else {
            null
        }

        setContent {
            val settingsStore = remember { SettingsDataStore(applicationContext) }
            val settings by settingsStore.settingsFlow.collectAsState(initial = AppSettingsState())
            val openUpdateSheetFromNotification by showUpdateSheetRequestState

            val useDarkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            val contentGraphicsLayer = rememberGraphicsLayer()
            val backgroundGraphicsLayer = rememberGraphicsLayer()
            val frostedBackdropState = remember(contentGraphicsLayer, backgroundGraphicsLayer) {
                FrostedBackdropState(
                    contentGraphicsLayer = contentGraphicsLayer,
                    backgroundGraphicsLayer = backgroundGraphicsLayer
                )
            }

            CompositionLocalProvider(LocalFrostedBackdropState provides frostedBackdropState) {
                AuraPlayerTheme(
                    darkTheme = useDarkTheme
                ) {
                    AuraPlayerApp(
                        activity = this,
                        settings = settings,
                        settingsStore = settingsStore,
                        initialExternalVideoUri = externalVideoUri,
                        openUpdateSheetFromNotification = openUpdateSheetFromNotification,
                        onConsumeUpdateSheetNotificationIntent = {
                            showUpdateSheetRequestState.value = false
                        },
                        onUpdatePipEligibility = { eligible -> shouldEnterPipOnUserLeave = eligible }
                    )
                }
            }
        }
    }
}

@Composable
fun AuraPlayerApp(
    activity: FragmentActivity,
    settings: AppSettingsState,
    settingsStore: SettingsDataStore,
    initialExternalVideoUri: Uri? = null,
    openUpdateSheetFromNotification: Boolean = false,
    onConsumeUpdateSheetNotificationIntent: () -> Unit = {},
    onUpdatePipEligibility: (Boolean) -> Unit = {}
) {
    val palette = LocalExtendedPalette.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    val videoRepository = remember(activity) { VideoRepository(activity.applicationContext) }
    val vaultManager = remember(activity) { PrivateVaultManager(activity.applicationContext) }
    val playlistRepository = remember(activity) { PlaylistRepository(activity.applicationContext) }
    val appUpdateManager = remember(activity) { AppUpdateManager(activity.applicationContext) }
    val playlists by playlistRepository.playlistsFlow.collectAsState(initial = emptyList())
    var startupUpdateRelease by remember { mutableStateOf<ReleaseUpdateInfo?>(null) }
    var showUpdateBottomSheet by rememberSaveable { mutableStateOf(false) }
    var hasCompletedStartupUpdateCheck by rememberSaveable { mutableStateOf(false) }

    val requiredPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    fun isAllFilesAccessGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching { Environment.isExternalStorageManager() }.getOrDefault(false)
        } else {
            true
        }
    }

    fun checkEffectiveMediaPermission(): Boolean {
        val standardGranted = ContextCompat.checkSelfPermission(
            activity,
            requiredPermission
        ) == PackageManager.PERMISSION_GRANTED
        val allFilesGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            isAllFilesAccessGranted()
        } else {
            false
        }
        return standardGranted || allFilesGranted
    }

    var hasMediaPermission by remember {
        mutableStateOf(checkEffectiveMediaPermission())
    }
    var hasAllFilesAccess by remember {
        mutableStateOf(isAllFilesAccessGranted())
    }
    var showAllFilesAccessDialog by rememberSaveable { mutableStateOf(false) }
    var hasPromptedAllFilesOnce by rememberSaveable { mutableStateOf(false) }
    var pendingStartupAllFilesReturn by rememberSaveable { mutableStateOf(false) }

    var showSplash by rememberSaveable { mutableStateOf(initialExternalVideoUri == null) }
    var selectedTab by rememberSaveable { mutableStateOf(MainNavTab.HOME) }
    var currentTreePath by rememberSaveable { mutableStateOf("") }
    var selectedFlatFolderPath by rememberSaveable { mutableStateOf<String?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showSortOptionsSheet by rememberSaveable { mutableStateOf(false) }
    var overlayScreen by rememberSaveable { mutableStateOf(ActiveOverlayScreen.NONE) }
    var privateFolderReturnScreen by rememberSaveable { mutableStateOf(ActiveOverlayScreen.SETTINGS) }
    var mediaInfoTargetVideo by remember { mutableStateOf<VideoItem?>(null) }

    var allVideos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var vaultVideos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var mountedStorageVolumes by remember { mutableStateOf(discoverMountedStorageVolumes(activity)) }
    var selectedStorageVolumeId by rememberSaveable { mutableStateOf(PRIMARY_STORAGE_VOLUME_ID) }
    var isScanning by remember { mutableStateOf(false) }
    var hasAutoSeededOnce by rememberSaveable { mutableStateOf(false) }
    var pendingRenameRequest by remember { mutableStateOf<Pair<VideoItem, String>?>(null) }
    var pendingVaultMoveVideos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var vaultToastEvent by remember { mutableStateOf<VaultToastEvent?>(null) }

    // Multi-Select & File Action Dialog State
    var selectedVideoIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var folderPickerMode by remember { mutableStateOf<FolderPickerMode?>(null) }
    var extraCreatedFolders by remember { mutableStateOf<Set<String>>(emptySet()) }
    var multiRenameTargetVideo by remember { mutableStateOf<VideoItem?>(null) }
    var showAddToPlaylistSheet by remember { mutableStateOf(false) }

    // Vault security state
    var isVaultUnlocked by rememberSaveable { mutableStateOf(false) }
    var showFallbackPinSheet by rememberSaveable { mutableStateOf(false) }
    var vaultAuthError by remember { mutableStateOf<String?>(null) }

    val currentSettings by rememberUpdatedState(settings)

    // Currently playing video & playlist
    var pendingExternalVideoUri by remember { mutableStateOf(initialExternalVideoUri) }
    var activePlayingVideo by remember { mutableStateOf<VideoItem?>(null) }
    var activePlaylist by remember { mutableStateOf<List<VideoItem>>(emptyList()) }

    LaunchedEffect(pendingExternalVideoUri) {
        val extUri = pendingExternalVideoUri ?: return@LaunchedEffect
        val stub = VideoItem(
            id = -1L,
            uri = extUri,
            title = extUri.lastPathSegment ?: "External Video",
            displayName = extUri.lastPathSegment ?: "video.mp4",
            durationMs = 0L,
            sizeBytes = 0L,
            width = 0,
            height = 0,
            folderName = "External",
            dateAddedSec = System.currentTimeMillis() / 1000L
        )
        val resolved = videoRepository.resolveDimensionsBeforePlayback(stub)
        activity.requestedOrientation = resolveTargetActivityOrientation(
            mode = currentSettings.videoOrientationMode,
            width = resolved.width,
            height = resolved.height,
            rotationDegrees = resolved.rotationDegrees
        )
        activePlaylist = listOf(resolved)
        activePlayingVideo = resolved
        pendingExternalVideoUri = null
    }

    fun launchPlayer(video: VideoItem, playlist: List<VideoItem>) {
        val needsResolution = video.width <= 0 || video.height <= 0 ||
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            video.isVaultItem
        if (!needsResolution) {
            activity.requestedOrientation = resolveTargetActivityOrientation(
                mode = currentSettings.videoOrientationMode,
                width = video.width,
                height = video.height,
                rotationDegrees = video.rotationDegrees
            )
            activePlaylist = playlist
            activePlayingVideo = video
        } else {
            coroutineScope.launch {
                val resolved = videoRepository.resolveDimensionsBeforePlayback(video)
                val updatedPlaylist = playlist.map { item ->
                    if (item.id == resolved.id && item.uri == resolved.uri) resolved else item
                }
                activity.requestedOrientation = resolveTargetActivityOrientation(
                    mode = currentSettings.videoOrientationMode,
                    width = resolved.width,
                    height = resolved.height,
                    rotationDegrees = resolved.rotationDegrees
                )
                activePlaylist = updatedPlaylist
                activePlayingVideo = resolved
            }
        }
    }

    fun markAllVideosAsSeen() {
        val baselineEpochSec = System.currentTimeMillis() / 1000L
        val currentIds = allVideos.map { it.id }
        allVideos = allVideos.map { it.copy(isNew = false) }
        coroutineScope.launch {
            settingsStore.markAllVideosAsSeen(currentIds, baselineEpochSec)
        }
        vaultToastEvent = VaultToastEvent(
            message = "All videos marked as seen",
            type = VaultToastType.INFO
        )
    }

    fun effectiveShowHiddenForCurrentView(): Boolean {
        return settings.showHiddenFolders && when (settings.viewMode) {
            LibraryViewMode.LIBRARY -> settings.hiddenFolderScanLibrary
            LibraryViewMode.TREE -> settings.hiddenFolderScanTree
            LibraryViewMode.FOLDER -> settings.hiddenFolderScanFolders
        }
    }

    fun refreshAllVideos(autoSeedIfEmpty: Boolean = false) {
        coroutineScope.launch {
            isScanning = true
            val showHiddenEffective = effectiveShowHiddenForCurrentView()
            var scanned = videoRepository.scanDeviceVideos(
                sortField = settings.sortField,
                sortAscending = settings.sortAscending,
                searchQuery = searchQuery,
                showHiddenFolders = showHiddenEffective,
                cachedDurationsMs = settings.cachedDurationsMs,
                scanDotFolders = settings.scanDotFolders,
                recognizeNoMedia = settings.recognizeNoMedia,
                markerFiles = settings.markerFiles,
                minVideoDurationSec = settings.minVideoDurationSec,
                minFileSizeMb = settings.minFileSizeMb,
                blacklistedFolders = settings.blacklistedFolders,
                whitelistedFolders = settings.whitelistedFolders,
                whitelistEnabled = settings.whitelistEnabled,
                watchedVideoIds = currentSettings.watchedVideoIds,
                seenBaselineEpochSec = currentSettings.seenBaselineEpochSec
            )
            if (scanned.isEmpty() && autoSeedIfEmpty && !hasAutoSeededOnce) {
                hasAutoSeededOnce = true
                val seededCount = videoRepository.generateSampleVideosInMediaStore()
                if (seededCount > 0) {
                    scanned = videoRepository.scanDeviceVideos(
                        sortField = settings.sortField,
                        sortAscending = settings.sortAscending,
                        searchQuery = searchQuery,
                        showHiddenFolders = showHiddenEffective,
                        cachedDurationsMs = settings.cachedDurationsMs,
                        scanDotFolders = settings.scanDotFolders,
                        recognizeNoMedia = settings.recognizeNoMedia,
                        markerFiles = settings.markerFiles,
                        minVideoDurationSec = settings.minVideoDurationSec,
                        minFileSizeMb = settings.minFileSizeMb,
                        blacklistedFolders = settings.blacklistedFolders,
                        whitelistedFolders = settings.whitelistedFolders,
                        whitelistEnabled = settings.whitelistEnabled,
                        watchedVideoIds = currentSettings.watchedVideoIds,
                        seenBaselineEpochSec = currentSettings.seenBaselineEpochSec
                    )
                }
            }
            mountedStorageVolumes = discoverMountedStorageVolumes(activity)
            if (mountedStorageVolumes.none { it.id.equals(selectedStorageVolumeId, ignoreCase = true) }) {
                selectedStorageVolumeId = mountedStorageVolumes.firstOrNull()?.id ?: PRIMARY_STORAGE_VOLUME_ID
            }
            allVideos = scanned
            vaultVideos = vaultManager.getVaultVideos()
            isScanning = false

            // Asynchronously extract exact durations & dimensions via MediaMetadataRetriever when missing
            if (scanned.any { it.durationMs <= 0L || it.width <= 0 || it.height <= 0 }) {
                val enriched = videoRepository.resolveMissingDurations(scanned) { newlyResolved ->
                    settingsStore.putCachedDurations(newlyResolved)
                }
                allVideos = enriched
            }
        }
    }

    fun destroySecureFolder() {
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                runCatching {
                    vaultManager.vaultDir.listFiles()?.forEach { it.deleteRecursively() }
                    File(vaultManager.vaultDir, ".nomedia").createNewFile()
                }
                runCatching {
                    File(activity.filesDir, "vault_thumbs").deleteRecursively()
                }
                runCatching {
                    File(activity.filesDir, "vault_meta_cache.json").delete()
                }
                activity.getSharedPreferences("aura_vault_security", android.content.Context.MODE_PRIVATE)
                    .edit()
                    .remove("vault_fallback_pin")
                    .apply()
            }
            settingsStore.setVaultSecurityQuestion("", "")
            settingsStore.setHideSecureFolderSettings(false)
            vaultVideos = emptyList()
            isVaultUnlocked = false
            showFallbackPinSheet = false
            vaultToastEvent = VaultToastEvent(
                message = "Secure Folder destroyed",
                type = VaultToastType.INFO
            )
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    fun clearMetadataCache() {
        coroutineScope.launch {
            settingsStore.clearCachedDurations()
            withContext(Dispatchers.IO) {
                runCatching {
                    val loader = Coil.imageLoader(activity.applicationContext)
                    loader.memoryCache?.clear()
                    loader.diskCache?.clear()
                }
                runCatching {
                    File(activity.cacheDir, "image_cache").deleteRecursively()
                }
            }
            refreshAllVideos(autoSeedIfEmpty = false)
            vaultToastEvent = VaultToastEvent(
                message = "Metadata & thumbnail cache cleared",
                type = VaultToastType.INFO
            )
        }
    }

    val currentRefreshAllVideos by rememberUpdatedState { autoSeed: Boolean ->
        refreshAllVideos(autoSeedIfEmpty = autoSeed)
    }

    // Auto-lock Vault when app is minimized and sync storage permissions on resume
    DisposableEffect(lifecycleOwner, settings.autoLockVaultOnMinimize) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && settings.autoLockVaultOnMinimize) {
                isVaultUnlocked = false
                showFallbackPinSheet = false
            } else if (event == Lifecycle.Event.ON_RESUME) {
                val wasAllFilesGranted = hasAllFilesAccess
                val nowAllFilesGranted = isAllFilesAccessGranted()
                hasAllFilesAccess = nowAllFilesGranted
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && nowAllFilesGranted) {
                    showAllFilesAccessDialog = false
                    pendingStartupAllFilesReturn = false
                    val wasMediaGranted = hasMediaPermission
                    hasMediaPermission = true
                    if (!wasAllFilesGranted && wasMediaGranted) {
                        currentRefreshAllVideos(searchQuery.isBlank())
                    }
                } else {
                    val nowMediaGranted = checkEffectiveMediaPermission()
                    if (nowMediaGranted != hasMediaPermission) {
                        hasMediaPermission = nowMediaGranted
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val deleteRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Updated device media library")
            }
        }
        refreshAllVideos(autoSeedIfEmpty = false)
    }

    val renameRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val pending = pendingRenameRequest
        pendingRenameRequest = null
        if (result.resultCode == Activity.RESULT_OK && pending != null) {
            coroutineScope.launch {
                when (val res = videoRepository.renameVideo(pending.first, pending.second)) {
                    is RenameVideoResult.Success -> {
                        refreshAllVideos(autoSeedIfEmpty = false)
                        snackbarHostState.showSnackbar("Renamed video")
                    }
                    is RenameVideoResult.Error -> {
                        snackbarHostState.showSnackbar(res.message)
                    }
                    is RenameVideoResult.RequiresWritePermission -> {
                        snackbarHostState.showSnackbar("Write permission required to rename")
                    }
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val effectiveGranted = granted || checkEffectiveMediaPermission()
        val wasGranted = hasMediaPermission
        hasMediaPermission = effectiveGranted
        if (effectiveGranted && wasGranted) {
            refreshAllVideos(autoSeedIfEmpty = true)
        }
    }

    LaunchedEffect(activePlayingVideo, settings.backgroundAudioOrPipOnHome) {
        onUpdatePipEligibility(activePlayingVideo != null && settings.backgroundAudioOrPipOnHome)
    }

    LaunchedEffect(
        hasMediaPermission,
        settings.sortField,
        settings.sortAscending,
        settings.showHiddenFolders,
        settings.hiddenFolderScanLibrary,
        settings.hiddenFolderScanTree,
        settings.hiddenFolderScanFolders,
        settings.viewMode,
        settings.scanDotFolders,
        settings.recognizeNoMedia,
        settings.markerFiles,
        settings.minVideoDurationSec,
        settings.minFileSizeMb,
        settings.blacklistedFolders,
        settings.whitelistedFolders,
        settings.whitelistEnabled,
        searchQuery
    ) {
        vaultVideos = vaultManager.getVaultVideos()
        if (hasMediaPermission) {
            refreshAllVideos(autoSeedIfEmpty = searchQuery.isBlank())
        }
    }

    val startupAllFilesAccessLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val nowAllFilesGranted = isAllFilesAccessGranted()
        hasAllFilesAccess = nowAllFilesGranted
        val wasWaitingForStartupReturn = pendingStartupAllFilesReturn
        pendingStartupAllFilesReturn = false
        if (nowAllFilesGranted) {
            showAllFilesAccessDialog = false
            val wasMediaGranted = hasMediaPermission
            hasMediaPermission = true
            if (wasMediaGranted && allVideos.isEmpty() && !isScanning) {
                refreshAllVideos(autoSeedIfEmpty = searchQuery.isBlank())
            }
        } else {
            val effectiveGranted = checkEffectiveMediaPermission()
            hasMediaPermission = effectiveGranted
            if (wasWaitingForStartupReturn && !effectiveGranted) {
                permissionLauncher.launch(requiredPermission)
            }
        }
    }

    fun dismissAllFilesDialogAndFallbackToMediaPermission() {
        showAllFilesAccessDialog = false
        pendingStartupAllFilesReturn = false
        val effectiveGranted = checkEffectiveMediaPermission()
        hasMediaPermission = effectiveGranted
        if (!effectiveGranted) {
            permissionLauncher.launch(requiredPermission)
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val release = startupUpdateRelease
            if (release != null) {
                coroutineScope.launch {
                    val lastNotified = settings.lastNotifiedUpdateTag
                    if (!lastNotified.equals(release.tagName, ignoreCase = true)) {
                        if (appUpdateManager.postSystemUpdateNotification(release)) {
                            settingsStore.setLastNotifiedUpdateTag(release.tagName)
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(showSplash) {
        if (!showSplash) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val allFilesGranted = isAllFilesAccessGranted()
                hasAllFilesAccess = allFilesGranted
                if (!allFilesGranted) {
                    if (!hasPromptedAllFilesOnce) {
                        hasPromptedAllFilesOnce = true
                        showAllFilesAccessDialog = true
                    }
                } else {
                    hasMediaPermission = true
                }
            } else {
                if (!hasMediaPermission) {
                    permissionLauncher.launch(requiredPermission)
                }
            }
        }
    }

    // Automatic startup check on Dispatchers.IO after storage permissions settle
    LaunchedEffect(Unit) {
        snapshotFlow { !showSplash && !showAllFilesAccessDialog && hasMediaPermission }
            .first { it }
        if (!hasCompletedStartupUpdateCheck) {
            hasCompletedStartupUpdateCheck = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val notifGranted = ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!notifGranted) {
                    runCatching {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
            val release = appUpdateManager.checkOnStartupAndNotifyIfNeeded(settingsStore)
            if (release != null) {
                startupUpdateRelease = release
                showUpdateBottomSheet = true
            }
        }
    }

    // Handle tapping the system update notification ("show_update_sheet" = true)
    LaunchedEffect(openUpdateSheetFromNotification) {
        if (openUpdateSheetFromNotification) {
            onConsumeUpdateSheetNotificationIntent()
            val release = startupUpdateRelease ?: when (val res = appUpdateManager.checkForUpdates()) {
                is UpdateCheckResult.UpdateAvailable -> res.info
                else -> null
            }
            if (release != null) {
                startupUpdateRelease = release
                showUpdateBottomSheet = true
            }
        }
    }

    fun triggerVaultAuthentication() {
        if (!settings.biometricVaultLockEnabled) {
            isVaultUnlocked = true
            return
        }
        vaultAuthError = null
        BiometricAuth.authenticate(
            activity = activity,
            onSuccess = {
                isVaultUnlocked = true
                showFallbackPinSheet = false
                vaultAuthError = null
            },
            onFallbackPinRequired = {
                showFallbackPinSheet = true
            },
            onError = { errMsg ->
                vaultAuthError = errMsg
            }
        )
    }

    fun deletePublicVideos(videosToDelete: List<VideoItem>) {
        if (videosToDelete.isEmpty()) return
        coroutineScope.launch {
            var directDeletedCount = 0
            val pendingUris = mutableListOf<Uri>()
            for (video in videosToDelete) {
                var deleted = false
                try {
                    deleted = activity.contentResolver.delete(video.uri, null, null) > 0
                } catch (_: SecurityException) {
                }
                if (deleted) directDeletedCount++ else pendingUris.add(video.uri)
            }

            if (pendingUris.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val pi: PendingIntent = MediaStore.createDeleteRequest(
                    activity.contentResolver,
                    pendingUris
                )
                deleteRequestLauncher.launch(
                    IntentSenderRequest.Builder(pi.intentSender).build()
                )
            } else if (directDeletedCount > 0) {
                refreshAllVideos(autoSeedIfEmpty = false)
                val msg = if (directDeletedCount == 1) "Deleted \"${videosToDelete.first().displayName}\""
                else "Deleted $directDeletedCount videos"
                snackbarHostState.showSnackbar(msg)
            }
        }
    }

    fun deletePublicVideo(video: VideoItem) {
        deletePublicVideos(listOf(video))
    }

    fun renamePublicVideo(video: VideoItem, newBaseName: String) {
        coroutineScope.launch {
            when (val res = videoRepository.renameVideo(video, newBaseName)) {
                is RenameVideoResult.Success -> {
                    refreshAllVideos(autoSeedIfEmpty = false)
                    snackbarHostState.showSnackbar("Renamed video")
                }
                is RenameVideoResult.RequiresWritePermission -> {
                    pendingRenameRequest = video to newBaseName
                    renameRequestLauncher.launch(
                        IntentSenderRequest.Builder(res.intentSender).build()
                    )
                }
                is RenameVideoResult.Error -> {
                    snackbarHostState.showSnackbar(res.message)
                }
            }
        }
    }

    val allFilesAccessLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        hasAllFilesAccess = vaultManager.hasAllFilesAccessPermission()
        if (hasAllFilesAccess) {
            showAllFilesAccessDialog = false
        }
        val pending = pendingVaultMoveVideos
        pendingVaultMoveVideos = emptyList()
        if (pending.isNotEmpty()) {
            if (hasAllFilesAccess) {
                coroutineScope.launch {
                    if (pending.size == 1) {
                        val target = pending.first()
                        when (val res = vaultManager.lockVideo(target)) {
                            is MoveToVaultResult.Completed -> {
                                allVideos = allVideos.filterNot { it.id == target.id }
                                vaultVideos = vaultManager.getVaultVideos()
                                refreshAllVideos(autoSeedIfEmpty = false)
                                vaultToastEvent = VaultToastEvent(
                                    message = "Locked in Private Folder",
                                    type = VaultToastType.LOCK
                                )
                            }
                            is MoveToVaultResult.RequiresAllFilesAccess -> {
                                snackbarHostState.showSnackbar("All files access permission is required to lock videos.")
                            }
                            is MoveToVaultResult.Error -> {
                                snackbarHostState.showSnackbar(res.message)
                            }
                        }
                    } else {
                        when (val res = vaultManager.moveVideosToVault(pending)) {
                            is BatchMoveToVaultResult.Completed -> {
                                allVideos = allVideos.filterNot { it.id in res.movedVideoIds }
                                vaultVideos = vaultManager.getVaultVideos()
                                refreshAllVideos(autoSeedIfEmpty = false)
                                val msg = if (res.movedCount <= 1) {
                                    "Locked in Private Folder"
                                } else {
                                    "Locked ${res.movedCount} videos in Private Folder"
                                }
                                vaultToastEvent = VaultToastEvent(
                                    message = msg,
                                    type = VaultToastType.LOCK
                                )
                            }
                            is BatchMoveToVaultResult.RequiresAllFilesAccess -> {
                                snackbarHostState.showSnackbar("All files access permission is required to lock videos.")
                            }
                            is BatchMoveToVaultResult.Error -> {
                                snackbarHostState.showSnackbar(res.message)
                            }
                        }
                    }
                }
            } else {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Grant All files access to lock videos instantly.")
                }
            }
        }
    }

    fun moveVideoToVault(video: VideoItem) {
        coroutineScope.launch {
            when (val res = vaultManager.lockVideo(video)) {
                is MoveToVaultResult.Completed -> {
                    allVideos = allVideos.filterNot { it.id == video.id }
                    vaultVideos = vaultManager.getVaultVideos()
                    refreshAllVideos(autoSeedIfEmpty = false)
                    vaultToastEvent = VaultToastEvent(
                        message = "Locked in Private Folder",
                        type = VaultToastType.LOCK
                    )
                }
                is MoveToVaultResult.RequiresAllFilesAccess -> {
                    pendingVaultMoveVideos = listOf(video)
                    allFilesAccessLauncher.launch(res.settingsIntent)
                }
                is MoveToVaultResult.Error -> {
                    snackbarHostState.showSnackbar(res.message)
                }
            }
        }
    }

    fun moveVideosToVaultBatch(videosToMove: List<VideoItem>) {
        if (videosToMove.isEmpty()) return
        coroutineScope.launch {
            when (val res = vaultManager.moveVideosToVault(videosToMove)) {
                is BatchMoveToVaultResult.Completed -> {
                    allVideos = allVideos.filterNot { it.id in res.movedVideoIds }
                    vaultVideos = vaultManager.getVaultVideos()
                    refreshAllVideos(autoSeedIfEmpty = false)
                    val msg = if (res.movedCount <= 1) {
                        "Locked in Private Folder"
                    } else {
                        "Locked ${res.movedCount} videos in Private Folder"
                    }
                    vaultToastEvent = VaultToastEvent(
                        message = msg,
                        type = VaultToastType.LOCK
                    )
                }
                is BatchMoveToVaultResult.RequiresAllFilesAccess -> {
                    pendingVaultMoveVideos = videosToMove
                    allFilesAccessLauncher.launch(res.settingsIntent)
                }
                is BatchMoveToVaultResult.Error -> {
                    snackbarHostState.showSnackbar(res.message)
                }
            }
        }
    }

    fun shareVideos(videosToShare: List<VideoItem>) {
        if (videosToShare.isEmpty()) return
        if (videosToShare.size == 1) {
            val vid = videosToShare.first()
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = vid.mimeType.ifBlank { "video/*" }
                putExtra(Intent.EXTRA_STREAM, vid.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            activity.startActivity(Intent.createChooser(shareIntent, "Share Video"))
        } else {
            val uris = ArrayList<Uri>(videosToShare.map { it.uri })
            val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "video/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            activity.startActivity(Intent.createChooser(shareIntent, "Share ${videosToShare.size} Videos"))
        }
    }

    val folders = remember(allVideos, settings.sortField, settings.sortAscending) {
        videoRepository.groupVideosByFolder(
            videos = allVideos,
            sortField = settings.sortField,
            sortAscending = settings.sortAscending
        )
    }

    val treeVideos = remember(allVideos, selectedStorageVolumeId, mountedStorageVolumes) {
        filterVideosByStorageVolume(allVideos, selectedStorageVolumeId, mountedStorageVolumes)
    }

    val directoryRoot = remember(treeVideos, settings.sortField, settings.sortAscending) {
        DirectoryTreeBuilder.buildTree(
            videos = treeVideos,
            sortField = settings.sortField,
            sortAscending = settings.sortAscending
        )
    }

    val selectedVideos = remember(allVideos, selectedVideoIds) {
        allVideos.filter { it.id in selectedVideoIds }
    }

    val availableStorageDirs = remember(allVideos, extraCreatedFolders) {
        videoRepository.collectAvailableStorageDirectories(allVideos, extraCreatedFolders)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Crossfade(
            targetState = when {
                pendingExternalVideoUri != null -> "PREPARING_EXTERNAL"
                activePlayingVideo != null -> "PLAYER"
                overlayScreen == ActiveOverlayScreen.MEDIA_INFO && mediaInfoTargetVideo != null -> "MEDIA_INFO"
                overlayScreen == ActiveOverlayScreen.CODEC_DIAGNOSTICS -> "CODECS"
                overlayScreen == ActiveOverlayScreen.APPEARANCE_SETTINGS -> "APPEARANCE_SETTINGS"
                overlayScreen == ActiveOverlayScreen.PRIVATE_FOLDER_SETTINGS -> "PRIVATE_FOLDER_SETTINGS"
                overlayScreen == ActiveOverlayScreen.FOLDER_SETTINGS -> "FOLDER_SETTINGS"
                overlayScreen == ActiveOverlayScreen.PLAYER_SETTINGS -> "PLAYER_SETTINGS"
                overlayScreen == ActiveOverlayScreen.GESTURE_SETTINGS -> "GESTURE_SETTINGS"
                overlayScreen == ActiveOverlayScreen.ABOUT -> "ABOUT"
                overlayScreen == ActiveOverlayScreen.SETTINGS -> "SETTINGS"
                overlayScreen == ActiveOverlayScreen.VAULT -> "VAULT"
                else -> "MAIN"
            },
            label = "root_navigation"
        ) { state ->
        when (state) {
            "PREPARING_EXTERNAL" -> {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black))
            }
            "PLAYER" -> {
                activePlayingVideo?.let { playing ->
                    VideoPlayerScreen(
                        initialVideo = playing,
                        playlist = activePlaylist,
                        settings = settings,
                        onSavePlaybackProgress = { vid, posMs ->
                            coroutineScope.launch {
                                settingsStore.recordVideoPlayback(vid.id, vid.uri.toString(), posMs)
                            }
                        },
                        onOpenSettingsFromPlayer = {
                            overlayScreen = ActiveOverlayScreen.SETTINGS
                        },
                        onShareVideo = { vid ->
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = vid.mimeType.ifBlank { "video/*" }
                                putExtra(Intent.EXTRA_STREAM, vid.uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            activity.startActivity(Intent.createChooser(shareIntent, "Share Video"))
                        },
                        onRenameVideoFromPlayer = { vid, newBase ->
                            renamePublicVideo(vid, newBase)
                        },
                        onDeleteVideoFromPlayer = { vid ->
                            deletePublicVideo(vid)
                        },
                        onClosePlayer = {
                            activePlayingVideo = null
                        }
                    )
                }
            }
            "MEDIA_INFO" -> {
                mediaInfoTargetVideo?.let { target ->
                    PastelMeshBackground(modifier = Modifier.fillMaxSize()) {
                        Scaffold(
                            containerColor = Color.Transparent,
                            contentWindowInsets = WindowInsets(0, 0, 0, 0),
                            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                        ) { paddingValues ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues)
                            ) {
                                MediaInfoScreen(
                                    video = target,
                                    onBack = {
                                        overlayScreen = ActiveOverlayScreen.NONE
                                        mediaInfoTargetVideo = null
                                    },
                                    onShowMessage = { msg ->
                                        coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                )
                            }
                        }
                    }
                }
            }
            "CODECS" -> {
                PastelMeshBackground(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                    ) { paddingValues ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                        ) {
                            CodecDiagnosticsScreen(
                                onBack = { overlayScreen = ActiveOverlayScreen.SETTINGS },
                                onShowMessage = { msg ->
                                    coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                                }
                            )
                        }
                    }
                }
            }
            "PRIVATE_FOLDER_SETTINGS" -> {
                PastelMeshBackground(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                    ) { paddingValues ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                        ) {
                            PrivateFolderSettingsScreen(
                                settings = settings,
                                onUpdateBiometricUnlock = {
                                    coroutineScope.launch { settingsStore.setBiometricVaultLockEnabled(it) }
                                },
                                onUpdateHideSecureFolderSettings = { hidden ->
                                    coroutineScope.launch { settingsStore.setHideSecureFolderSettings(hidden) }
                                },
                                onSaveSecurityQuestion = { q, a ->
                                    coroutineScope.launch { settingsStore.setVaultSecurityQuestion(q, a) }
                                },
                                onDestroySecureFolder = { destroySecureFolder() },
                                onBack = { overlayScreen = privateFolderReturnScreen },
                                onShowToast = { msg ->
                                    vaultToastEvent = VaultToastEvent(msg, VaultToastType.INFO)
                                }
                            )
                        }
                    }
                }
            }
            "FOLDER_SETTINGS" -> {
                PastelMeshBackground(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                    ) { paddingValues ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                        ) {
                            FolderSettingsScreen(
                                settings = settings,
                                availableFolders = availableStorageDirs,
                                onUpdateShowHiddenFolders = {
                                    coroutineScope.launch { settingsStore.setShowHiddenFolders(it) }
                                },
                                onUpdateHiddenScanTargets = { lib, tree, foldersFlag ->
                                    coroutineScope.launch {
                                        settingsStore.setHiddenFolderScanTarget(lib, tree, foldersFlag)
                                    }
                                },
                                onUpdateRecognizeNoMedia = {
                                    coroutineScope.launch { settingsStore.setRecognizeNoMedia(it) }
                                },
                                onUpdateScanDotFolders = {
                                    coroutineScope.launch { settingsStore.setScanDotFolders(it) }
                                },
                                onUpdateMarkerFiles = {
                                    coroutineScope.launch { settingsStore.setMarkerFiles(it) }
                                },
                                onUpdateMinVideoDurationSec = {
                                    coroutineScope.launch { settingsStore.setMinVideoDurationSec(it) }
                                },
                                onUpdateMinFileSizeMb = {
                                    coroutineScope.launch { settingsStore.setMinFileSizeMb(it) }
                                },
                                onUpdateBlacklistedFolders = {
                                    coroutineScope.launch { settingsStore.setBlacklistedFolders(it) }
                                },
                                onUpdateWhitelistedFolders = { foldersSet, enabled ->
                                    coroutineScope.launch {
                                        settingsStore.setWhitelistedFolders(foldersSet, enabled)
                                    }
                                },
                                onForceRescanLibrary = {
                                    coroutineScope.launch {
                                        videoRepository.triggerMediaScannerOnPrimaryStorage()
                                        refreshAllVideos(autoSeedIfEmpty = true)
                                    }
                                },
                                onClearMetadataCache = { clearMetadataCache() },
                                onMarkAllAsSeen = { markAllVideosAsSeen() },
                                onBack = { overlayScreen = ActiveOverlayScreen.SETTINGS },
                                onShowCapsuleToast = { msg ->
                                    vaultToastEvent = VaultToastEvent(msg, VaultToastType.RESCAN)
                                }
                            )
                        }
                    }
                }
            }
            "GESTURE_SETTINGS" -> {
                PastelMeshBackground(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                    ) { paddingValues ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                        ) {
                            GestureSettingsScreen(
                                settings = settings,
                                onUpdateHapticFeedback = { coroutineScope.launch { settingsStore.setHapticFeedbackEnabled(it) } },
                                onUpdateConfirmBeforeLeaving = { coroutineScope.launch { settingsStore.setConfirmBeforeLeavingPlayback(it) } },
                                onUpdateBrightnessGestures = { coroutineScope.launch { settingsStore.setBrightnessSwipeEnabled(it) } },
                                onUpdateVolumeGestures = { coroutineScope.launch { settingsStore.setVolumeSwipeEnabled(it) } },
                                onUpdatePinchToZoom = { coroutineScope.launch { settingsStore.setPinchToZoomEnabled(it) } },
                                onUpdatePinchToZoomSubtitles = { coroutineScope.launch { settingsStore.setPinchToZoomSubtitlesEnabled(it) } },
                                onUpdateTwoFingerSpeed = { coroutineScope.launch { settingsStore.setTwoFingerSwipeForSpeedEnabled(it) } },
                                onUpdateSwipeSubtitlesToSeek = { coroutineScope.launch { settingsStore.setSwipeSubtitlesToSeekEnabled(it) } },
                                onUpdateInvertSwipeSubtitles = { coroutineScope.launch { settingsStore.setInvertSwipeSubtitlesDirection(it) } },
                                onUpdateHorizontalSwipeToSeek = { coroutineScope.launch { settingsStore.setHorizontalSeekSwipeEnabled(it) } },
                                onUpdateShowSeekbarHidden = { coroutineScope.launch { settingsStore.setShowSeekbarWhenControlsHidden(it) } },
                                onUpdateSwipeUpCenterPlaylist = { coroutineScope.launch { settingsStore.setSwipeUpCenterForPlaylistEnabled(it) } },
                                onUpdateHorizontalSwipeSensitivity = { coroutineScope.launch { settingsStore.setHorizontalSwipeSensitivity(it) } },
                                onUpdateHoldForSpeedMultiplier = { coroutineScope.launch { settingsStore.setHoldForSpeedMultiplier(it) } },
                                onUpdateDoubleTapSeekSeconds = { coroutineScope.launch { settingsStore.setDoubleTapSeekSeconds(it) } },
                                onUpdateDoubleTapSeekAreaWidth = { coroutineScope.launch { settingsStore.setDoubleTapSeekAreaWidthPercent(it) } },
                                onUpdateDoubleTapLeftAction = { coroutineScope.launch { settingsStore.setDoubleTapLeftAction(it) } },
                                onUpdateDoubleTapCenterAction = { coroutineScope.launch { settingsStore.setDoubleTapCenterAction(it) } },
                                onUpdateDoubleTapRightAction = { coroutineScope.launch { settingsStore.setDoubleTapRightAction(it) } },
                                onUpdateSingleTapCenterGesture = { coroutineScope.launch { settingsStore.setUseSingleTapForCenterGesture(it) } },
                                onUpdateHoldAndDragSubtitles = { coroutineScope.launch { settingsStore.setHoldAndDragSubtitlesEnabled(it) } },
                                onUpdateMediaPreviousAction = { coroutineScope.launch { settingsStore.setMediaControlPreviousAction(it) } },
                                onUpdateMediaPlayPauseAction = { coroutineScope.launch { settingsStore.setMediaControlPlayPauseAction(it) } },
                                onUpdateMediaNextAction = { coroutineScope.launch { settingsStore.setMediaControlNextAction(it) } },
                                onBack = { overlayScreen = ActiveOverlayScreen.SETTINGS }
                            )
                        }
                    }
                }
            }
            "ABOUT" -> {
                PastelMeshBackground(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                    ) { paddingValues ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                        ) {
                            AboutScreen(
                                onBack = { overlayScreen = ActiveOverlayScreen.SETTINGS },
                                onShowCapsuleToast = { msg ->
                                    vaultToastEvent = VaultToastEvent(msg, VaultToastType.INFO)
                                }
                            )
                        }
                    }
                }
            }
            "APPEARANCE_SETTINGS" -> {
                PastelMeshBackground(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                    ) { paddingValues ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                        ) {
                            AppearanceSettingsScreen(
                                settings = settings,
                                onUpdateThemeMode = {
                                    coroutineScope.launch { settingsStore.setThemeMode(it) }
                                },
                                onUpdateAppDisguiseMode = { mode ->
                                    coroutineScope.launch { settingsStore.setAppDisguiseMode(mode) }
                                },
                                onShowCapsuleToast = { msg ->
                                    vaultToastEvent = VaultToastEvent(msg, VaultToastType.INFO)
                                },
                                onBack = { overlayScreen = ActiveOverlayScreen.SETTINGS }
                            )
                        }
                    }
                }
            }
            "PLAYER_SETTINGS" -> {
                PastelMeshBackground(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                    ) { paddingValues ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                        ) {
                            PlayerSettingsScreen(
                                settings = settings,
                                onUpdateVideoOrientationMode = {
                                    coroutineScope.launch { settingsStore.setVideoOrientationMode(it) }
                                },
                                onUpdateDecoderPriority = {
                                    coroutineScope.launch { settingsStore.setDecoderPriority(it) }
                                },
                                onUpdateDefaultPlaybackSpeed = {
                                    coroutineScope.launch { settingsStore.setDefaultPlaybackSpeed(it) }
                                },
                                onUpdateResumePlaybackMode = {
                                    coroutineScope.launch { settingsStore.setResumePlaybackMode(it) }
                                },
                                onUpdateBackgroundAudioOrPip = {
                                    coroutineScope.launch { settingsStore.setBackgroundAudioOrPipOnHome(it) }
                                },
                                onUpdateAutoPlayNextEpisode = {
                                    coroutineScope.launch { settingsStore.setAutoPlayNextEpisode(it) }
                                },
                                onUpdateShowSkipIntro = {
                                    coroutineScope.launch { settingsStore.setShowSkipIntroButton(it) }
                                },
                                onBack = { overlayScreen = ActiveOverlayScreen.SETTINGS }
                            )
                        }
                    }
                }
            }
            "SETTINGS" -> {
                PastelMeshBackground(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                    ) { paddingValues ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                        ) {
                            SettingsScreen(
                                settings = settings,
                                onOpenAppearanceSettings = { overlayScreen = ActiveOverlayScreen.APPEARANCE_SETTINGS },
                                onOpenFolderSettings = { overlayScreen = ActiveOverlayScreen.FOLDER_SETTINGS },
                                onOpenPlayerSettings = { overlayScreen = ActiveOverlayScreen.PLAYER_SETTINGS },
                                onOpenGestureSettings = { overlayScreen = ActiveOverlayScreen.GESTURE_SETTINGS },
                                onOpenPrivateFolderSettings = {
                                    privateFolderReturnScreen = ActiveOverlayScreen.SETTINGS
                                    overlayScreen = ActiveOverlayScreen.PRIVATE_FOLDER_SETTINGS
                                },
                                onOpenCodecDiagnostics = { overlayScreen = ActiveOverlayScreen.CODEC_DIAGNOSTICS },
                                onOpenAbout = { overlayScreen = ActiveOverlayScreen.ABOUT },
                                onBack = { overlayScreen = ActiveOverlayScreen.NONE }
                            )
                        }
                    }
                }
            }
            "VAULT" -> {
                Scaffold(
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        VaultScreen(
                            isUnlocked = isVaultUnlocked || !settings.biometricVaultLockEnabled,
                            showFallbackPinSheet = showFallbackPinSheet,
                            vaultVideos = vaultVideos,
                            authErrorMessage = vaultAuthError,
                            onRequestBiometricUnlock = { triggerVaultAuthentication() },
                            onFallbackPinAuthenticated = {
                                isVaultUnlocked = true
                                showFallbackPinSheet = false
                                vaultAuthError = null
                            },
                            onDismissFallbackPin = { showFallbackPinSheet = false },
                            onLockVault = {
                                isVaultUnlocked = false
                                showFallbackPinSheet = false
                            },
                            onOpenSettings = { overlayScreen = ActiveOverlayScreen.SETTINGS },
                            onPlayVaultVideo = { vid, list ->
                                launchPlayer(vid, list)
                            },
                            onRestoreToGallery = { vaultVid ->
                                coroutineScope.launch {
                                    val ok = vaultManager.unlockVideo(vaultVid)
                                    if (ok) {
                                        vaultVideos = vaultVideos.filterNot { it.id == vaultVid.id }
                                        refreshAllVideos(autoSeedIfEmpty = false)
                                        vaultToastEvent = VaultToastEvent(
                                            message = "Restored to ${vaultVid.folderName}",
                                            type = VaultToastType.RESTORE
                                        )
                                    } else {
                                        snackbarHostState.showSnackbar("Unable to restore video from Private Vault")
                                    }
                                }
                            },
                            onDeletePermanently = { vaultVid ->
                                coroutineScope.launch {
                                    if (vaultManager.deleteVaultVideoPermanently(vaultVid)) {
                                        vaultVideos = vaultManager.getVaultVideos()
                                        snackbarHostState.showSnackbar("Deleted from Private Vault")
                                    }
                                }
                            },
                            isSecureFolderSettingsHidden = settings.hideSecureFolderSettings,
                            onOpenPrivateFolderSettings = {
                                privateFolderReturnScreen = ActiveOverlayScreen.VAULT
                                overlayScreen = ActiveOverlayScreen.PRIVATE_FOLDER_SETTINGS
                            },
                            onUnhideSecureFolderSettings = {
                                coroutineScope.launch {
                                    settingsStore.setHideSecureFolderSettings(false)
                                    vaultToastEvent = VaultToastEvent(
                                        message = "Secure Folder Settings restored",
                                        type = VaultToastType.INFO
                                    )
                                }
                            },
                            onBack = { overlayScreen = ActiveOverlayScreen.NONE }
                        )
                    }
                }
            }
            else -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    PastelMeshBackground(modifier = Modifier.fillMaxSize()) {
                        Scaffold(
                            containerColor = Color.Transparent,
                            contentWindowInsets = WindowInsets(0, 0, 0, 0),
                            snackbarHost = {
                                SnackbarHost(
                                    hostState = snackbarHostState,
                                    modifier = Modifier.padding(bottom = 86.dp)
                                )
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                            BackHandler(enabled = selectedVideoIds.isEmpty() && selectedTab != MainNavTab.HOME) {
                                selectedTab = MainNavTab.HOME
                            }

                            HomeScreen(
                                selectedTab = selectedTab,
                                videos = allVideos,
                                folders = folders,
                                directoryRoot = directoryRoot,
                                currentTreePath = currentTreePath,
                                selectedFlatFolderPath = selectedFlatFolderPath,
                                settings = settings,
                                searchQuery = searchQuery,
                                hasMediaPermission = hasMediaPermission,
                                isLoading = isScanning,
                                selectedIds = selectedVideoIds,
                                onToggleVideoSelection = { video ->
                                    selectedVideoIds = if (video.id in selectedVideoIds) {
                                        selectedVideoIds - video.id
                                    } else {
                                        selectedVideoIds + video.id
                                    }
                                },
                                onToggleFolderSelection = { folderVideos ->
                                    val folderVideoIds = folderVideos.map { it.id }.toSet()
                                    if (folderVideoIds.isNotEmpty()) {
                                        val allSelected = folderVideoIds.all { it in selectedVideoIds }
                                        selectedVideoIds = if (allSelected) {
                                            selectedVideoIds - folderVideoIds
                                        } else {
                                            selectedVideoIds + folderVideoIds
                                        }
                                    }
                                },
                                onClearSelection = { selectedVideoIds = emptySet() },
                                onSelectAllInView = { viewVideos ->
                                    selectedVideoIds = selectedVideoIds + viewVideos.map { it.id }
                                },
                                onInvertSelectionInView = { viewVideos ->
                                    val viewIds = viewVideos.map { it.id }.toSet()
                                    val keptOutside = selectedVideoIds - viewIds
                                    val invertedInView = viewIds - selectedVideoIds
                                    selectedVideoIds = keptOutside + invertedInView
                                },
                                onPlaySelected = {
                                    if (selectedVideos.isNotEmpty()) {
                                        val batch = selectedVideos
                                        selectedVideoIds = emptySet()
                                        launchPlayer(batch.first(), batch)
                                    }
                                },
                                onOpenMediaInfo = { target ->
                                    mediaInfoTargetVideo = target
                                    overlayScreen = ActiveOverlayScreen.MEDIA_INFO
                                },
                                onShareSelected = {
                                    shareVideos(selectedVideos)
                                },
                                onRequestPermission = { permissionLauncher.launch(requiredPermission) },
                                onSearchQueryChange = { searchQuery = it },
                                onOpenSortSheet = { showSortOptionsSheet = true },
                                onOpenVault = {
                                    overlayScreen = ActiveOverlayScreen.VAULT
                                    if (!isVaultUnlocked && settings.biometricVaultLockEnabled) {
                                        triggerVaultAuthentication()
                                    }
                                },
                                onOpenSettings = { overlayScreen = ActiveOverlayScreen.SETTINGS },
                                onMarkAllAsSeen = { markAllVideosAsSeen() },
                                onNavigateToTreePath = { path -> currentTreePath = path },
                                onNavigateUpTree = {
                                    currentTreePath = DirectoryTreeBuilder.getParentPath(currentTreePath)
                                },
                                onSelectFlatFolder = { path -> selectedFlatFolderPath = path },
                                onPlayVideo = { vid, list ->
                                    launchPlayer(vid, list)
                                },
                                onResumeLastPlayed = {
                                    val lastUri = settings.lastPlayedVideoUri
                                    val target = allVideos.find { it.uri.toString() == lastUri }
                                        ?: allVideos.firstOrNull()
                                    if (target != null) {
                                        launchPlayer(target, allVideos)
                                    }
                                },
                                onMoveToVault = { moveVideoToVault(it) },
                                onRenameVideo = { vid, newBase -> renamePublicVideo(vid, newBase) },
                                onDeleteVideo = { deletePublicVideo(it) },
                                onGenerateSampleVideos = {
                                    coroutineScope.launch {
                                        isScanning = true
                                        val count = videoRepository.generateSampleVideosInMediaStore()
                                        refreshAllVideos(autoSeedIfEmpty = false)
                                        if (count > 0) {
                                            snackbarHostState.showSnackbar("Loaded $count sample videos")
                                        }
                                    }
                                },
                                onRefreshLibrary = {
                                    refreshAllVideos(autoSeedIfEmpty = false)
                                },
                                onVideosUpdatedFromPullRefresh = { refreshedList ->
                                    mountedStorageVolumes = discoverMountedStorageVolumes(activity)
                                    allVideos = refreshedList
                                    coroutineScope.launch {
                                        vaultVideos = vaultManager.getVaultVideos()
                                    }
                                },
                                mountedStorageVolumes = mountedStorageVolumes,
                                selectedStorageVolumeId = selectedStorageVolumeId,
                                onSelectStorageVolume = { volId ->
                                    selectedStorageVolumeId = volId
                                    currentTreePath = ""
                                }
                            )

                            // Innermost root BackHandler for multi-select priority
                            BackHandler(enabled = selectedVideoIds.isNotEmpty()) {
                                selectedVideoIds = emptySet()
                            }

                            // Swap bottom bar: FloatingSelectionActionDock during multi-select, else LiquidGlassBottomNavPill
                            if (selectedVideoIds.isNotEmpty()) {
                                FloatingSelectionActionDock(
                                    selectedCount = selectedVideoIds.size,
                                    onMoveClick = { folderPickerMode = FolderPickerMode.MOVE },
                                    onCopyClick = { folderPickerMode = FolderPickerMode.COPY },
                                    onRenameClick = {
                                        val single = selectedVideos.firstOrNull()
                                        if (single != null && selectedVideos.size == 1) {
                                            multiRenameTargetVideo = single
                                        }
                                    },
                                    onAddToPlaylistClick = { showAddToPlaylistSheet = true },
                                    onMoveToVaultClick = {
                                        val batch = selectedVideos
                                        selectedVideoIds = emptySet()
                                        moveVideosToVaultBatch(batch)
                                    },
                                    onDeleteClick = {
                                        val batch = selectedVideos
                                        selectedVideoIds = emptySet()
                                        deletePublicVideos(batch)
                                    },
                                    modifier = Modifier.align(Alignment.BottomCenter)
                                )
                            } else {
                                LiquidGlassBottomNavPill(
                                    selectedTab = selectedTab,
                                    onSelectTab = { tab ->
                                        if (tab == MainNavTab.HOME && selectedTab == MainNavTab.HOME) {
                                            currentTreePath = ""
                                            selectedFlatFolderPath = null
                                        }
                                        selectedTab = tab
                                        if (tab != MainNavTab.HOME) {
                                            currentTreePath = ""
                                            selectedFlatFolderPath = null
                                        }
                                    },
                                    modifier = Modifier.align(Alignment.BottomCenter)
                                )
                            }
                        }
                    }

                    if (showSortOptionsSheet) {
                        SortOptionsDialog(
                            settings = settings,
                            onApplyOptions = { sortField, sortAsc, viewMode, layoutMode, fields, lengthOverThumb, showHidden, recognizeNoMedia ->
                                val viewModeChanged = viewMode != settings.viewMode
                                coroutineScope.launch {
                                    settingsStore.applySortViewOptions(
                                        sortField = sortField,
                                        sortAscending = sortAsc,
                                        viewMode = viewMode,
                                        layoutMode = layoutMode,
                                        enabledFields = fields,
                                        displayLengthOverThumbnail = lengthOverThumb,
                                        showHiddenFolders = showHidden,
                                        recognizeNoMedia = recognizeNoMedia
                                    )
                                }
                                if (viewModeChanged) {
                                    currentTreePath = ""
                                    selectedFlatFolderPath = null
                                }
                            },
                            onDismiss = { showSortOptionsSheet = false }
                        )
                    }

                    if (folderPickerMode != null) {
                        val mode = folderPickerMode!!
                        val targetVideos = selectedVideos
                        SelectFolderDialog(
                            actionLabel = if (mode == FolderPickerMode.MOVE) "Move" else "Copy",
                            allDirectories = availableStorageDirs,
                            sourceRelativePaths = if (mode == FolderPickerMode.MOVE) {
                                targetVideos.map { it.relativePath }.toSet()
                            } else {
                                emptySet()
                            },
                            onCreateFolder = { createdPath ->
                                extraCreatedFolders = extraCreatedFolders + createdPath
                            },
                            onSelectFolder = { destinationPath ->
                                folderPickerMode = null
                                selectedVideoIds = emptySet()
                                coroutineScope.launch {
                                    val result = if (mode == FolderPickerMode.MOVE) {
                                        videoRepository.moveVideosToFolder(targetVideos, destinationPath)
                                    } else {
                                        videoRepository.copyVideosToFolder(targetVideos, destinationPath)
                                    }
                                    when (result) {
                                        is BatchFileOperationResult.Completed -> {
                                            refreshAllVideos(autoSeedIfEmpty = false)
                                            val verb = if (mode == FolderPickerMode.MOVE) "Moved" else "Copied"
                                            snackbarHostState.showSnackbar(
                                                "$verb ${result.processedCount} video(s) to $destinationPath"
                                            )
                                        }
                                        is BatchFileOperationResult.RequiresDeleteConfirmation -> {
                                            deleteRequestLauncher.launch(
                                                IntentSenderRequest.Builder(result.deleteIntentSender).build()
                                            )
                                        }
                                        is BatchFileOperationResult.Error -> {
                                            snackbarHostState.showSnackbar(result.message)
                                        }
                                    }
                                }
                            },
                            onDismiss = { folderPickerMode = null }
                        )
                    }

                    if (multiRenameTargetVideo != null) {
                        val target = multiRenameTargetVideo!!
                        RenameFileDialog(
                            video = target,
                            onDismiss = { multiRenameTargetVideo = null },
                            onConfirmRename = { newBaseName ->
                                multiRenameTargetVideo = null
                                selectedVideoIds = emptySet()
                                renamePublicVideo(target, newBaseName)
                            }
                        )
                    }

                    if (showAddToPlaylistSheet) {
                        val targetIds = selectedVideoIds.toList()
                        AddToPlaylistSheet(
                            videoCount = targetIds.size,
                            playlists = playlists,
                            onCreatePlaylist = { name ->
                                coroutineScope.launch {
                                    playlistRepository.createPlaylist(name)
                                }
                            },
                            onAddVideosToPlaylists = { chosenPlaylistIds ->
                                showAddToPlaylistSheet = false
                                selectedVideoIds = emptySet()
                                coroutineScope.launch {
                                    playlistRepository.addVideosToPlaylists(chosenPlaylistIds, targetIds)
                                    snackbarHostState.showSnackbar(
                                        "Added ${targetIds.size} video(s) to ${chosenPlaylistIds.size} playlist(s)"
                                    )
                                }
                            },
                            onDismiss = { showAddToPlaylistSheet = false }
                        )
                    }
                    }
                }
            }
        }
    }

        // 1-second floating Capsule Toast Pill for Private Vault Lock & Restore
        VaultNotificationPill(
            event = vaultToastEvent,
            onDismiss = { vaultToastEvent = null },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        if (showAllFilesAccessDialog) {
            AlertDialog(
                onDismissRequest = { dismissAllFilesDialogAndFallbackToMediaPermission() },
                shape = RoundedCornerShape(24.dp),
                containerColor = palette.sheetBackground,
                titleContentColor = palette.primaryText,
                textContentColor = palette.secondaryText,
                icon = {
                    Icon(
                        imageVector = Icons.Filled.FolderOpen,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(30.dp)
                    )
                },
                title = {
                    Text(
                        text = "Allow All Files Access",
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "All files access is required for instant Private Vault locking and folder operations (Move, Copy, Rename) without system prompts.",
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showAllFilesAccessDialog = false
                            pendingStartupAllFilesReturn = true
                            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                Intent(
                                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                    Uri.parse("package:${activity.packageName}")
                                )
                            } else {
                                vaultManager.createManageAllFilesAccessIntent()
                            }
                            startupAllFilesAccessLauncher.launch(intent)
                        },
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2563EB),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("grant_all_files_access_button")
                    ) {
                        Text(
                            text = "Grant Permission",
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { dismissAllFilesDialogAndFallbackToMediaPermission() },
                        modifier = Modifier.testTag("dismiss_all_files_access_button")
                    ) {
                        Text("Not Now", color = palette.secondaryText)
                    }
                },
                modifier = Modifier.testTag("all_files_access_dialog")
            )
        }

        if (showUpdateBottomSheet && startupUpdateRelease != null) {
            UpdateBottomSheet(
                release = startupUpdateRelease!!,
                updateManager = appUpdateManager,
                onDismiss = { showUpdateBottomSheet = false }
            )
        }

        AnimatedVisibility(
            visible = showSplash,
            enter = fadeIn(animationSpec = tween(durationMillis = 300)),
            exit = fadeOut(animationSpec = tween(durationMillis = 300)) +
                scaleOut(targetScale = 1.04f, animationSpec = tween(durationMillis = 300))
        ) {
            SplashScreen(onSplashFinished = { showSplash = false })
        }
    }
}
