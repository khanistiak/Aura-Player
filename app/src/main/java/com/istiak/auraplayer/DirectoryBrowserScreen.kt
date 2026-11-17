package com.istiak.auraplayer

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.BackEventCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.istiak.auraplayer.ui.components.ScrollAwareFixedHeader
import com.istiak.auraplayer.ui.components.recordFrostedBackdrop
import com.istiak.auraplayer.ui.components.rememberScrollAwareGlassLayout
import com.istiak.auraplayer.ui.theme.CoralBadgeDark
import com.istiak.auraplayer.ui.theme.DangerRed
import com.istiak.auraplayer.ui.theme.FolderBadgeCoral
import com.istiak.auraplayer.ui.theme.LocalExtendedPalette
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Hierarchical Tree View, Flat Folder View, and Library View browser (`DirectoryBrowserScreen.kt`):
 * - Uses `rememberScrollAwareGlassLayout(listState = listState, defaultHeaderHeight = 68.dp, extraTopGap = 4.dp)`
 *   to measure the Top Header dynamically, ramp glass opacity from `0f` (when idle at scroll top) to `1f`
 *   (over `48.dp`), and record the scrolling `LazyColumn` / `LazyVerticalGrid` via
 *   `.recordFrostedBackdrop(scrollLayout.backdropState, listState, scrollLayout.contentGraphicsLayer)`.
 * - Renders the Top Header (including title/back arrow, top-right icons, and merged breadcrumb bar when inside
 *   a subfolder) inside `ScrollAwareFixedHeader(layoutState = scrollLayout) { ... }`.
 * - Wrapped inside Material 3's `PullToRefreshBox` with `PullToRefreshDefaults.Indicator` offset
 *   below the measured header height (`scrollLayout.measuredHeaderHeightDp`).
 * - Supports Android 14+ `PredictiveBackHandler` so swiping back from a subfolder smoothly scales
 *   (`1f -> 0.92f`) and rounds corners to preview the parent directory underneath.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectoryBrowserScreen(
    selectedTab: MainNavTab,
    currentTreePath: String,
    directoryRoot: DirectoryNode,
    flatFolders: List<VideoFolder>,
    selectedFlatFolderPath: String?,
    allVideos: List<VideoItem>,
    settings: AppSettingsState,
    imageLoader: ImageLoader,
    selectedIds: Set<Long> = emptySet(),
    onToggleVideoSelection: (VideoItem) -> Unit = {},
    onToggleFolderSelection: (List<VideoItem>) -> Unit = {},
    onNavigateToTreePath: (String) -> Unit,
    onNavigateUpTree: () -> Unit,
    onSelectFlatFolder: (String?) -> Unit,
    onPlayVideo: (VideoItem, List<VideoItem>) -> Unit,
    onOpenVideoMenu: (VideoItem) -> Unit,
    topTitle: String = "Videos",
    isDrilledDown: Boolean = false,
    showSearchBar: Boolean = false,
    searchQuery: String = "",
    selectedQuickFilter: LibraryQuickFilter = LibraryQuickFilter.ALL,
    onSelectQuickFilter: (LibraryQuickFilter) -> Unit = {},
    @Suppress("UNUSED_PARAMETER") playlistsCount: Int = 1,
    @Suppress("UNUSED_PARAMETER") onOpenPlaylistsBrowser: () -> Unit = {},
    onToggleSearch: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    onOpenVault: () -> Unit = {},
    onOpenSortSheet: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onMarkAllAsSeen: () -> Unit = {},
    currentViewVideos: List<VideoItem> = allVideos,
    onClearSelection: () -> Unit = {},
    onSelectAllInView: (List<VideoItem>) -> Unit = {},
    onInvertSelectionInView: (List<VideoItem>) -> Unit = {},
    onPlaySelected: () -> Unit = {},
    onOpenMediaInfo: (VideoItem) -> Unit = {},
    onShareSelected: () -> Unit = {},
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onScannedVideosUpdated: (List<VideoItem>) -> Unit = {},
    mountedStorageVolumes: List<StorageVolumeOption> = emptyList(),
    selectedStorageVolumeId: String = PRIMARY_STORAGE_VOLUME_ID,
    onSelectStorageVolume: (String) -> Unit = {},
    bottomContentPadding: Dp = 112.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val isMultiSelectMode = selectedIds.isNotEmpty()
    val isTreeMode = selectedTab == MainNavTab.HOME && settings.viewMode == LibraryViewMode.TREE
    val isFolderMode = selectedTab == MainNavTab.HOME && settings.viewMode == LibraryViewMode.FOLDER
    val isLibraryMode = selectedTab == MainNavTab.ALL_VIDEOS || settings.viewMode == LibraryViewMode.LIBRARY
    val isGridLayout = settings.layoutMode == LibraryLayoutMode.GRID

    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val scrollLayout = rememberScrollAwareGlassLayout(
        listState = listState,
        gridState = gridState,
        useGridState = isGridLayout,
        defaultHeaderHeight = 68.dp,
        extraTopGap = 6.dp,
        bottomPadding = bottomContentPadding,
        horizontalPadding = if (isGridLayout) 16.dp else 0.dp
    )

    val pullRefreshState = rememberPullToRefreshState()
    var internalRefreshing by remember { mutableStateOf(false) }
    var showLibraryUpdatedPill by remember { mutableStateOf(false) }
    var libraryUpdatedMessage by remember { mutableStateOf("Library updated") }
    val effectiveRefreshing = isRefreshing || internalRefreshing

    // Predictive Back Gesture Animation (Android 14+): smoothly scales (1.0f -> 0.92f) + fades
    // Disabled during multi-select so back gestures clear selection first before navigating up.
    var predictiveBackProgress by remember { mutableFloatStateOf(0f) }
    val canNavigateBackInBrowser = !isMultiSelectMode &&
        ((isTreeMode && currentTreePath.isNotEmpty()) ||
            (isFolderMode && selectedFlatFolderPath != null))

    PredictiveBackHandler(enabled = canNavigateBackInBrowser) { progress: Flow<BackEventCompat> ->
        try {
            progress.collect { backEvent ->
                predictiveBackProgress = backEvent.progress.coerceIn(0f, 1f)
            }
            predictiveBackProgress = 0f
            if (isTreeMode && currentTreePath.isNotEmpty()) {
                onNavigateUpTree()
            } else if (isFolderMode && selectedFlatFolderPath != null) {
                onSelectFlatFolder(null)
            }
        } catch (_: CancellationException) {
            predictiveBackProgress = 0f
        }
    }

    // Innermost BackHandler: always exit multi-select mode first without leaving the current folder.
    BackHandler(enabled = isMultiSelectMode) {
        predictiveBackProgress = 0f
        onClearSelection()
    }

    LaunchedEffect(showLibraryUpdatedPill) {
        if (showLibraryUpdatedPill) {
            delay(2200L)
            showLibraryUpdatedPill = false
        }
    }

    val trimmedQuery = searchQuery.trim()
    val isFilterActive = trimmedQuery.isNotEmpty() || selectedQuickFilter != LibraryQuickFilter.ALL

    fun videoMatchesActiveFilters(video: VideoItem, matchFolderNameOnAll: Boolean = false): Boolean {
        if (!matchesQuickFilter(video, selectedQuickFilter, settings.watchedVideoIds, settings.seenBaselineEpochSec)) {
            return false
        }
        if (trimmedQuery.isEmpty()) return true
        return video.title.contains(trimmedQuery, ignoreCase = true) ||
            video.displayName.contains(trimmedQuery, ignoreCase = true) ||
            (matchFolderNameOnAll && (
                video.folderName.contains(trimmedQuery, ignoreCase = true) ||
                    video.relativePath.contains(trimmedQuery, ignoreCase = true)
                ))
    }

    val filteredLibraryVideos = remember(
        allVideos,
        trimmedQuery,
        selectedQuickFilter,
        settings.watchedVideoIds,
        settings.seenBaselineEpochSec
    ) {
        if (!isFilterActive) allVideos
        else allVideos.filter { videoMatchesActiveFilters(it, matchFolderNameOnAll = true) }
    }

    val filteredFlatFolders = remember(
        flatFolders,
        trimmedQuery,
        selectedQuickFilter,
        settings.watchedVideoIds,
        settings.seenBaselineEpochSec
    ) {
        if (!isFilterActive) flatFolders
        else flatFolders.mapNotNull { folder ->
            val folderNameMatch = selectedQuickFilter == LibraryQuickFilter.ALL &&
                trimmedQuery.isNotEmpty() &&
                folder.name.contains(trimmedQuery, ignoreCase = true)
            val matchingVideos = if (folderNameMatch) {
                folder.videos
            } else {
                folder.videos.filter { videoMatchesActiveFilters(it, matchFolderNameOnAll = false) }
            }
            if (matchingVideos.isNotEmpty()) folder.copy(videos = matchingVideos) else null
        }
    }

    val rawActiveFlatFolder = remember(flatFolders, selectedFlatFolderPath) {
        if (selectedFlatFolderPath == null) null
        else flatFolders.find { it.path == selectedFlatFolderPath || it.name == selectedFlatFolderPath }
    }
    val activeFlatFolder = remember(
        rawActiveFlatFolder,
        trimmedQuery,
        selectedQuickFilter,
        settings.watchedVideoIds,
        settings.seenBaselineEpochSec
    ) {
        rawActiveFlatFolder?.let { folder ->
            if (!isFilterActive) folder
            else folder.copy(videos = folder.videos.filter { videoMatchesActiveFilters(it, matchFolderNameOnAll = false) })
        }
    }

    val filteredTreeRoot = remember(
        directoryRoot,
        trimmedQuery,
        selectedQuickFilter,
        settings.watchedVideoIds,
        settings.seenBaselineEpochSec,
        settings.sortField,
        settings.sortAscending
    ) {
        if (!isFilterActive) {
            directoryRoot
        } else {
            val volumeVideos = directoryRoot.allRecursiveVideos
            val matchingVideos = volumeVideos.filter {
                videoMatchesActiveFilters(
                    it,
                    matchFolderNameOnAll = selectedQuickFilter == LibraryQuickFilter.ALL
                )
            }
            DirectoryTreeBuilder.buildTree(
                videos = matchingVideos,
                sortField = settings.sortField,
                sortAscending = settings.sortAscending
            )
        }
    }

    val currentNode = remember(filteredTreeRoot, currentTreePath) {
        DirectoryTreeBuilder.getNodeAtPath(filteredTreeRoot, currentTreePath)
    }

    val predictiveScale = 1f - (0.08f * predictiveBackProgress) // 1.0f -> 0.92f
    val predictiveAlpha = 1f - (0.18f * predictiveBackProgress)
    val predictiveCornerRadius = (24f * predictiveBackProgress).dp
    val headerHeight = scrollLayout.measuredHeaderHeightDp
    val headerHeightPx = with(density) { headerHeight.toPx() }
    val fadeFeatherPx = with(density) { 18.dp.toPx() }

    Box(modifier = modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = effectiveRefreshing,
            onRefresh = {
                onRefresh()
                coroutineScope.launch {
                    internalRefreshing = true
                    val previousIds = allVideos.map { it.id }.toSet()
                    val scanned = withContext(Dispatchers.IO) {
                        val repo = VideoRepository(context.applicationContext)
                        val vaultMgr = PrivateVaultManager(context.applicationContext)
                        val storageDirs = arrayOf(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)?.absolutePath,
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)?.absolutePath,
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.absolutePath,
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)?.absolutePath
                        ).filterNotNull().toTypedArray()
                        if (storageDirs.isNotEmpty()) {
                            runCatching {
                                MediaScannerConnection.scanFile(context.applicationContext, storageDirs, null, null)
                            }
                        }
                        val lockedPaths = vaultMgr.getLockedOriginalPaths()
                        val showHiddenEffective = settings.showHiddenFolders && when (settings.viewMode) {
                            LibraryViewMode.LIBRARY -> settings.hiddenFolderScanLibrary
                            LibraryViewMode.TREE -> settings.hiddenFolderScanTree
                            LibraryViewMode.FOLDER -> settings.hiddenFolderScanFolders
                        }
                        repo.scanMediaAndRefreshLibrary(
                            sortField = settings.sortField,
                            sortAscending = settings.sortAscending,
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
                            watchedVideoIds = settings.watchedVideoIds,
                            seenBaselineEpochSec = settings.seenBaselineEpochSec
                        ).filterNot { item ->
                            val itemPath = "${Environment.getExternalStorageDirectory()?.absolutePath ?: "/storage/emulated/0"}/${item.relativePath.trim('/')}/${item.displayName}"
                            itemPath in lockedPaths
                        }
                    }
                    val newCount = scanned.count { it.id !in previousIds }
                    onScannedVideosUpdated(scanned)
                    internalRefreshing = false
                    libraryUpdatedMessage = if (newCount > 0) {
                        "Library updated (+$newCount new)"
                    } else {
                        "Library updated"
                    }
                    showLibraryUpdatedPill = true
                }
            },
            state = pullRefreshState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullRefreshState,
                    isRefreshing = effectiveRefreshing,
                    containerColor = Color.White,
                    color = Color(0xFF2563EB),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = headerHeight)
                        .testTag("pull_to_refresh_indicator")
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = predictiveScale
                    scaleY = predictiveScale
                    alpha = predictiveAlpha
                    shape = RoundedCornerShape(predictiveCornerRadius)
                    clip = predictiveBackProgress > 0.01f
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    drawContent()
                    if (headerHeightPx > 0f && size.height > headerHeightPx + fadeFeatherPx) {
                        val maskEndY = (headerHeightPx + fadeFeatherPx).coerceAtMost(size.height)
                        val solidStop = (headerHeightPx / maskEndY).coerceIn(0f, 0.92f)
                        drawRect(
                            brush = Brush.verticalGradient(
                                0.0f to Color.Transparent,
                                solidStop to Color.Transparent,
                                1.0f to Color.Black,
                                startY = 0f,
                                endY = maskEndY
                            ),
                            size = Size(size.width, maskEndY),
                            blendMode = BlendMode.DstIn
                        )
                    }
                }
                .testTag("directory_pull_to_refresh_box")
        ) {
            when {
                isLibraryMode -> {
                    if (isGridLayout) {
                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Adaptive(minSize = 165.dp),
                            contentPadding = scrollLayout.contentPadding,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .recordFrostedBackdrop(
                                    backdropState = scrollLayout.backdropState,
                                    scrollableState = gridState,
                                    contentLayerOverride = scrollLayout.contentGraphicsLayer
                                )
                                .testTag("videos_grid")
                        ) {
                            if (filteredLibraryVideos.isEmpty() && isFilterActive) {
                                item(span = { GridItemSpan(maxLineSpan) }, key = "empty_filter_library_grid") {
                                    FilteredEmptyNotice()
                                }
                            }
                            items(filteredLibraryVideos, key = { "${it.id}_${it.uri}" }) { video ->
                                val isSelected = video.id in selectedIds
                                DirectoryVideoGridCard(
                                    video = video,
                                    settings = settings,
                                    imageLoader = imageLoader,
                                    isSelected = isSelected,
                                    onPlay = {
                                        if (isMultiSelectMode) onToggleVideoSelection(video)
                                        else onPlayVideo(video, filteredLibraryVideos)
                                    },
                                    onLongPress = { onToggleVideoSelection(video) },
                                    onOpenMenu = {
                                        if (isMultiSelectMode) onToggleVideoSelection(video)
                                        else onOpenVideoMenu(video)
                                    }
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            contentPadding = scrollLayout.contentPadding,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .recordFrostedBackdrop(
                                    backdropState = scrollLayout.backdropState,
                                    scrollableState = listState,
                                    contentLayerOverride = scrollLayout.contentGraphicsLayer
                                )
                                .testTag("videos_list")
                        ) {
                            if (filteredLibraryVideos.isEmpty() && isFilterActive) {
                                item(key = "empty_filter_library_list") {
                                    FilteredEmptyNotice()
                                }
                            }
                            items(filteredLibraryVideos, key = { "${it.id}_${it.uri}" }) { video ->
                                val isSelected = video.id in selectedIds
                                DirectoryVideoRow(
                                    video = video,
                                    settings = settings,
                                    imageLoader = imageLoader,
                                    isSelected = isSelected,
                                    onPlay = {
                                        if (isMultiSelectMode) onToggleVideoSelection(video)
                                        else onPlayVideo(video, filteredLibraryVideos)
                                    },
                                    onLongPress = { onToggleVideoSelection(video) },
                                    onOpenActionSheet = {
                                        if (isMultiSelectMode) onToggleVideoSelection(video)
                                        else onOpenVideoMenu(video)
                                    }
                                )
                            }
                        }
                    }
                }

                isFolderMode -> {
                    if (activeFlatFolder != null) {
                        if (isGridLayout) {
                            LazyVerticalGrid(
                                state = gridState,
                                columns = GridCells.Adaptive(minSize = 165.dp),
                                contentPadding = scrollLayout.contentPadding,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .recordFrostedBackdrop(
                                        backdropState = scrollLayout.backdropState,
                                        scrollableState = gridState,
                                        contentLayerOverride = scrollLayout.contentGraphicsLayer
                                    )
                                    .testTag("flat_folder_videos_grid")
                            ) {
                                if (activeFlatFolder.videos.isEmpty() && isFilterActive) {
                                    item(span = { GridItemSpan(maxLineSpan) }, key = "empty_filter_flat_grid") {
                                        FilteredEmptyNotice()
                                    }
                                }
                                items(activeFlatFolder.videos, key = { "${it.id}_${it.uri}" }) { video ->
                                    val isSelected = video.id in selectedIds
                                    DirectoryVideoGridCard(
                                        video = video,
                                        settings = settings,
                                        imageLoader = imageLoader,
                                        isSelected = isSelected,
                                        onPlay = {
                                            if (isMultiSelectMode) onToggleVideoSelection(video)
                                            else onPlayVideo(video, activeFlatFolder.videos)
                                        },
                                        onLongPress = { onToggleVideoSelection(video) },
                                        onOpenMenu = {
                                            if (isMultiSelectMode) onToggleVideoSelection(video)
                                            else onOpenVideoMenu(video)
                                        }
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                contentPadding = scrollLayout.contentPadding,
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .recordFrostedBackdrop(
                                        backdropState = scrollLayout.backdropState,
                                        scrollableState = listState,
                                        contentLayerOverride = scrollLayout.contentGraphicsLayer
                                    )
                                    .testTag("flat_folder_videos_list")
                            ) {
                                if (activeFlatFolder.videos.isEmpty() && isFilterActive) {
                                    item(key = "empty_filter_flat_list") {
                                        FilteredEmptyNotice()
                                    }
                                }
                                items(activeFlatFolder.videos, key = { "${it.id}_${it.uri}" }) { video ->
                                    val isSelected = video.id in selectedIds
                                    DirectoryVideoRow(
                                        video = video,
                                        settings = settings,
                                        imageLoader = imageLoader,
                                        isSelected = isSelected,
                                        onPlay = {
                                            if (isMultiSelectMode) onToggleVideoSelection(video)
                                            else onPlayVideo(video, activeFlatFolder.videos)
                                        },
                                        onLongPress = { onToggleVideoSelection(video) },
                                        onOpenActionSheet = {
                                            if (isMultiSelectMode) onToggleVideoSelection(video)
                                            else onOpenVideoMenu(video)
                                        }
                                    )
                                }
                            }
                        }
                    } else if (isGridLayout) {
                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Adaptive(minSize = 155.dp),
                            contentPadding = scrollLayout.contentPadding,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .recordFrostedBackdrop(
                                    backdropState = scrollLayout.backdropState,
                                    scrollableState = gridState,
                                    contentLayerOverride = scrollLayout.contentGraphicsLayer
                                )
                                .testTag("folders_grid")
                        ) {
                            if (filteredFlatFolders.isEmpty() && isFilterActive) {
                                item(span = { GridItemSpan(maxLineSpan) }, key = "empty_filter_folder_grid") {
                                    FilteredEmptyNotice()
                                }
                            }
                            items(filteredFlatFolders, key = { it.path }) { folder ->
                                val isFolderSelected = folder.videos.isNotEmpty() &&
                                    folder.videos.all { it.id in selectedIds }
                                FlatFolderGridItem(
                                    folder = folder,
                                    unwatchedCount = folder.videos.count {
                                        it.isConsideredNew(settings.watchedVideoIds, settings.seenBaselineEpochSec)
                                    },
                                    isSelected = isFolderSelected,
                                    onClick = {
                                        if (isMultiSelectMode) onToggleFolderSelection(folder.videos)
                                        else onSelectFlatFolder(folder.path)
                                    },
                                    onLongPress = { onToggleFolderSelection(folder.videos) }
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            contentPadding = scrollLayout.contentPadding,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .recordFrostedBackdrop(
                                    backdropState = scrollLayout.backdropState,
                                    scrollableState = listState,
                                    contentLayerOverride = scrollLayout.contentGraphicsLayer
                                )
                                .testTag("folders_list")
                        ) {
                            if (filteredFlatFolders.isEmpty() && isFilterActive) {
                                item(key = "empty_filter_folder_list") {
                                    FilteredEmptyNotice()
                                }
                            }
                            items(filteredFlatFolders, key = { it.path }) { folder ->
                                val isFolderSelected = folder.videos.isNotEmpty() &&
                                    folder.videos.all { it.id in selectedIds }
                                FlatFolderRowItem(
                                    folder = folder,
                                    settings = settings,
                                    isSelected = isFolderSelected,
                                    onClick = {
                                        if (isMultiSelectMode) onToggleFolderSelection(folder.videos)
                                        else onSelectFlatFolder(folder.path)
                                    },
                                    onLongPress = { onToggleFolderSelection(folder.videos) },
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }

                else -> {
                    // Hierarchical Tree View (Default)
                    val isAtRoot = currentTreePath.isEmpty()
                    val subFolders = currentNode.childFolders
                    val directVideos = currentNode.directVideos

                    if (isGridLayout) {
                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Adaptive(minSize = 160.dp),
                            contentPadding = scrollLayout.contentPadding,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .recordFrostedBackdrop(
                                    backdropState = scrollLayout.backdropState,
                                    scrollableState = gridState,
                                    contentLayerOverride = scrollLayout.contentGraphicsLayer
                                )
                                .testTag(if (isAtRoot) "tree_root_grid" else "tree_directory_grid")
                        ) {
                            if (isAtRoot && mountedStorageVolumes.size > 1) {
                                item(span = { GridItemSpan(maxLineSpan) }, key = "storage_volume_switcher") {
                                    FrostedStorageVolumeSwitcherRow(
                                        volumes = mountedStorageVolumes,
                                        selectedVolumeId = selectedStorageVolumeId,
                                        onSelectVolume = onSelectStorageVolume,
                                        modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                                    )
                                }
                            }

                            if (subFolders.isEmpty() && directVideos.isEmpty() && isFilterActive) {
                                item(span = { GridItemSpan(maxLineSpan) }, key = "empty_filter_tree_grid") {
                                    FilteredEmptyNotice()
                                }
                            }

                            if (!isAtRoot && subFolders.isNotEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }, key = "header_folders") {
                                    SectionHeaderLabel(
                                        title = "Folders",
                                        color = palette.sectionHeaderBlue,
                                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                                    )
                                }
                            }

                            items(subFolders, key = { "folder_${it.fullPath}" }) { node ->
                                val isFolderSelected = node.allRecursiveVideos.isNotEmpty() &&
                                    node.allRecursiveVideos.all { it.id in selectedIds }
                                DirectoryFolderGridItem(
                                    node = node,
                                    watchedVideoIds = settings.watchedVideoIds,
                                    seenBaselineEpochSec = settings.seenBaselineEpochSec,
                                    isSelected = isFolderSelected,
                                    onClick = {
                                        if (isMultiSelectMode) onToggleFolderSelection(node.allRecursiveVideos)
                                        else onNavigateToTreePath(node.fullPath)
                                    },
                                    onLongPress = { onToggleFolderSelection(node.allRecursiveVideos) }
                                )
                            }

                            if (!isAtRoot && directVideos.isNotEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }, key = "header_videos") {
                                    SectionHeaderLabel(
                                        title = "Videos",
                                        color = palette.sectionHeaderBlue,
                                        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                                    )
                                }
                            }

                            items(directVideos, key = { "video_${it.id}_${it.uri}" }) { video ->
                                val isSelected = video.id in selectedIds
                                DirectoryVideoGridCard(
                                    video = video,
                                    settings = settings,
                                    imageLoader = imageLoader,
                                    isSelected = isSelected,
                                    onPlay = {
                                        if (isMultiSelectMode) onToggleVideoSelection(video)
                                        else onPlayVideo(video, directVideos)
                                    },
                                    onLongPress = { onToggleVideoSelection(video) },
                                    onOpenMenu = {
                                        if (isMultiSelectMode) onToggleVideoSelection(video)
                                        else onOpenVideoMenu(video)
                                    }
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            contentPadding = scrollLayout.contentPadding,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .recordFrostedBackdrop(
                                    backdropState = scrollLayout.backdropState,
                                    scrollableState = listState,
                                    contentLayerOverride = scrollLayout.contentGraphicsLayer
                                )
                                .testTag(if (isAtRoot) "tree_root_list" else "tree_directory_list")
                        ) {
                            if (isAtRoot && mountedStorageVolumes.size > 1) {
                                item(key = "storage_volume_switcher") {
                                    FrostedStorageVolumeSwitcherRow(
                                        volumes = mountedStorageVolumes,
                                        selectedVolumeId = selectedStorageVolumeId,
                                        onSelectVolume = onSelectStorageVolume,
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            if (subFolders.isEmpty() && directVideos.isEmpty() && isFilterActive) {
                                item(key = "empty_filter_tree_list") {
                                    FilteredEmptyNotice()
                                }
                            }

                            if (!isAtRoot && subFolders.isNotEmpty()) {
                                item(key = "section_folders") {
                                    SectionHeaderLabel(
                                        title = "Folders",
                                        color = palette.sectionHeaderBlue,
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            items(subFolders, key = { "folder_${it.fullPath}" }) { node ->
                                val isFolderSelected = node.allRecursiveVideos.isNotEmpty() &&
                                    node.allRecursiveVideos.all { it.id in selectedIds }
                                DirectoryFolderRowItem(
                                    node = node,
                                    settings = settings,
                                    isSelected = isFolderSelected,
                                    onClick = {
                                        if (isMultiSelectMode) onToggleFolderSelection(node.allRecursiveVideos)
                                        else onNavigateToTreePath(node.fullPath)
                                    },
                                    onLongPress = { onToggleFolderSelection(node.allRecursiveVideos) },
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                            }

                            if (directVideos.isNotEmpty()) {
                                if (!isAtRoot) {
                                    item(key = "section_videos") {
                                        SectionHeaderLabel(
                                            title = "Videos",
                                            color = palette.sectionHeaderBlue,
                                            modifier = Modifier.padding(
                                                start = 18.dp,
                                                end = 18.dp,
                                                top = if (subFolders.isNotEmpty()) 12.dp else 6.dp,
                                                bottom = 4.dp
                                            )
                                        )
                                    }
                                }
                                items(directVideos, key = { "video_${it.id}_${it.uri}" }) { video ->
                                    val isSelected = video.id in selectedIds
                                    DirectoryVideoRow(
                                        video = video,
                                        settings = settings,
                                        imageLoader = imageLoader,
                                        isSelected = isSelected,
                                        onPlay = {
                                            if (isMultiSelectMode) onToggleVideoSelection(video)
                                            else onPlayVideo(video, directVideos)
                                        },
                                        onLongPress = { onToggleVideoSelection(video) },
                                        onOpenActionSheet = {
                                            if (isMultiSelectMode) onToggleVideoSelection(video)
                                            else onOpenVideoMenu(video)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Subtle "Library updated" feedback pill below the Feathered Liquid Glass top header
            AnimatedVisibility(
                visible = showLibraryUpdatedPill,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = headerHeight + 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .shadow(8.dp, RoundedCornerShape(999.dp))
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.94f))
                        .border(1.dp, Color(0xFF2563EB).copy(alpha = 0.25f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                        .testTag("library_updated_pill")
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = libraryUpdatedMessage,
                        color = Color(0xFF1E293B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Fixed Top Header: ContextualSelectionTopBar in Multi-Select Mode, else ScrollAwareFixedHeader
        if (isMultiSelectMode) {
            ContextualSelectionTopBar(
                selectedCount = selectedIds.size,
                totalCount = currentViewVideos.size.coerceAtLeast(selectedIds.size),
                onClearSelection = onClearSelection,
                onSelectAll = { onSelectAllInView(currentViewVideos) },
                onInvertSelection = { onInvertSelectionInView(currentViewVideos) },
                onPlaySelected = onPlaySelected,
                onShowMediaInfo = {
                    val singleId = selectedIds.firstOrNull()
                    val singleVideo = allVideos.find { it.id == singleId }
                    if (singleVideo != null) {
                        onOpenMediaInfo(singleVideo)
                    }
                },
                onShareSelected = onShareSelected,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .onSizeChanged { size ->
                        if (size.height > 0 && scrollLayout.headerHeightPxState.intValue != size.height) {
                            scrollLayout.headerHeightPxState.intValue = size.height
                        }
                    }
            )
        } else {
            val headerScrimBgColor = MaterialTheme.colorScheme.background
            val solidFrostedHeaderScrim = remember(headerScrimBgColor, isDark) {
                Brush.verticalGradient(
                    0.0f to headerScrimBgColor.copy(alpha = 0.96f),
                    0.72f to headerScrimBgColor.copy(alpha = 0.92f),
                    1.0f to headerScrimBgColor.copy(alpha = 0.88f)
                )
            }
            val hairlineDividerColor = if (isDark) {
                Color.White.copy(alpha = 0.12f)
            } else {
                Color(0xFFCBD5E1).copy(alpha = 0.65f)
            }

            ScrollAwareFixedHeader(
                layoutState = scrollLayout,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .background(solidFrostedHeaderScrim)
                    .onSizeChanged { size ->
                        if (size.height > 0 && scrollLayout.headerHeightPxState.intValue != size.height) {
                            scrollLayout.headerHeightPxState.intValue = size.height
                        }
                    }
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    LiquidGlassHeaderContent(
                        title = topTitle,
                        isDrilledDown = isDrilledDown,
                        currentPath = if (isTreeMode) currentTreePath else "",
                        showSearchBar = showSearchBar,
                        searchQuery = searchQuery,
                        onNavigateBack = {
                            if (isTreeMode && currentTreePath.isNotEmpty()) onNavigateUpTree()
                            else onSelectFlatFolder(null)
                        },
                        onNavigateToPath = onNavigateToTreePath,
                        onOpenVault = onOpenVault,
                        onToggleSearch = onToggleSearch,
                        onSearchQueryChange = onSearchQueryChange,
                        onOpenSortDialog = onOpenSortSheet,
                        onOpenSettings = onOpenSettings,
                        onMarkAllAsSeen = onMarkAllAsSeen,
                        selectedQuickFilter = selectedQuickFilter,
                        onSelectQuickFilter = onSelectQuickFilter
                    )
                    HorizontalDivider(
                        thickness = 0.8.dp,
                        color = hairlineDividerColor
                    )
                }
            }
        }
    }
}

@Composable
private fun FilteredEmptyNotice() {
    val palette = LocalExtendedPalette.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 48.dp)
            .testTag("filtered_empty_notice")
    ) {
        Icon(
            imageVector = Icons.Filled.SearchOff,
            contentDescription = null,
            tint = palette.secondaryText,
            modifier = Modifier.size(44.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "No matching videos",
            color = palette.primaryText,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Try clearing the search text or switching quick filters.",
            color = palette.secondaryText,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun FrostedStorageVolumeSwitcherRow(
    volumes: List<StorageVolumeOption>,
    selectedVolumeId: String,
    onSelectVolume: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    var expanded by remember { mutableStateOf(false) }
    val activeVolume = volumes.firstOrNull { it.id.equals(selectedVolumeId, ignoreCase = true) }
        ?: volumes.firstOrNull()
        ?: return

    val activeIcon = when (activeVolume.kind) {
        StorageVolumeKind.INTERNAL -> Icons.Filled.PhoneAndroid
        StorageVolumeKind.SD_CARD -> Icons.Filled.SdStorage
        StorageVolumeKind.USB_OTG -> Icons.Filled.Usb
    }

    val pillBg = if (isDark) Color(0xFF1A2238).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.82f)
    val pillBorder = if (isDark) Color.White.copy(alpha = 0.16f) else Color(0xFFD6D9F8).copy(alpha = 0.90f)

    Box(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .shadow(
                    elevation = 2.dp,
                    shape = RoundedCornerShape(999.dp),
                    ambientColor = Color(0xFF2563EB).copy(alpha = 0.08f),
                    spotColor = Color(0xFF434A6E).copy(alpha = 0.10f)
                )
                .clip(RoundedCornerShape(999.dp))
                .background(pillBg)
                .border(1.dp, pillBorder, RoundedCornerShape(999.dp))
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .testTag("storage_volume_switcher_pill")
        ) {
            Icon(
                imageVector = activeIcon,
                contentDescription = null,
                tint = if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = activeVolume.label,
                color = palette.primaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = "Switch Storage Volume",
                tint = palette.secondaryText,
                modifier = Modifier.size(17.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(palette.sheetBackground)
        ) {
            volumes.forEach { volume ->
                val isSelected = volume.id == activeVolume.id
                val volIcon = when (volume.kind) {
                    StorageVolumeKind.INTERNAL -> Icons.Filled.PhoneAndroid
                    StorageVolumeKind.SD_CARD -> Icons.Filled.SdStorage
                    StorageVolumeKind.USB_OTG -> Icons.Filled.Usb
                }
                DropdownMenuItem(
                    text = {
                        Text(
                            text = volume.label,
                            color = if (isSelected) palette.brandTitleColor else palette.primaryText,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.5.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = volIcon,
                            contentDescription = null,
                            tint = if (isSelected) palette.brandTitleColor else palette.secondaryText,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Selected",
                                tint = palette.brandTitleColor,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelectVolume(volume.id)
                    },
                    modifier = Modifier.testTag("storage_volume_option_${volume.id}")
                )
            }
        }
    }
}

@Composable
private fun SectionHeaderLabel(
    title: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        color = color,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.2.sp,
        modifier = modifier.testTag("section_header_${title.lowercase()}")
    )
}

/**
 * Custom Multi-Layer Premium Folder Icon (`54.dp × 46.dp` default) harmonizing with
 * AuraPlayer's sky-blue & periwinkle-lavender palette:
 * 1. Deep periwinkle-indigo Back Tab Layer (`#4F6EF7` -> `#6366F1`).
 * 2. Crisp `2.dp` white translucent Inner Document / Media-Peek Strip peeking above the flap.
 * 3. Rich sky-blue to royal-periwinkle Front Glass Flap (`#60A5FA` -> `#3B82F6` -> `#4F46E5`)
 *    with a `1.dp` specular white top highlight curve and subtle `0.18f` play-glyph watermark.
 */
@Composable
private fun PremiumFolderIcon(
    modifier: Modifier = Modifier,
    width: Dp = 54.dp,
    height: Dp = 46.dp
) {
    Canvas(modifier = modifier.size(width = width, height = height)) {
        val w = size.width
        val h = size.height
        val cornerPx = 7.dp.toPx().coerceAtMost(h * 0.2f)

        // Subtle soft ground shadow under the folder
        drawRoundRect(
            color = Color(0xFF1E3A8A).copy(alpha = 0.16f),
            topLeft = Offset(x = w * 0.04f, y = h * 0.28f),
            size = Size(width = w * 0.92f, height = h * 0.72f),
            cornerRadius = CornerRadius(cornerPx, cornerPx)
        )

        // 1. Back Tab Layer (#4F6EF7 -> #6366F1)
        val backTabTop = h * 0.02f
        val backBodyTop = h * 0.18f
        val backBottom = h * 0.94f
        val tabEndX = w * 0.43f
        val shoulderX = w * 0.54f

        val backPath = Path().apply {
            moveTo(0f, backTabTop + cornerPx)
            quadraticTo(0f, backTabTop, cornerPx, backTabTop)
            lineTo(tabEndX - cornerPx * 0.5f, backTabTop)
            cubicTo(
                tabEndX + cornerPx * 0.35f, backTabTop,
                shoulderX - cornerPx * 0.35f, backBodyTop,
                shoulderX + cornerPx * 0.4f, backBodyTop
            )
            lineTo(w - cornerPx, backBodyTop)
            quadraticTo(w, backBodyTop, w, backBodyTop + cornerPx)
            lineTo(w, backBottom - cornerPx)
            quadraticTo(w, backBottom, w - cornerPx, backBottom)
            lineTo(cornerPx, backBottom)
            quadraticTo(0f, backBottom, 0f, backBottom - cornerPx)
            close()
        }
        drawPath(
            path = backPath,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF4F6EF7), Color(0xFF6366F1)),
                start = Offset.Zero,
                end = Offset(w, h * 0.9f)
            )
        )

        // 2. Subtle Inner Document / Media-Peek Strip (2.dp white translucent edge)
        val peekHeight = 2.dp.toPx()
        val peekTop = h * 0.225f
        val peekInset = w * 0.07f
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.86f),
                    Color.White.copy(alpha = 0.50f)
                ),
                startY = peekTop,
                endY = peekTop + peekHeight * 2.2f
            ),
            topLeft = Offset(x = peekInset, y = peekTop),
            size = Size(width = w - peekInset * 2f, height = peekHeight * 2.4f),
            cornerRadius = CornerRadius(peekHeight, peekHeight)
        )

        // 3. Front Glass Flap (#60A5FA -> #3B82F6 -> #4F46E5)
        val flapTop = h * 0.27f
        val flapHeight = h * 0.70f
        val flapCorner = 7.5.dp.toPx().coerceAtMost(flapHeight * 0.28f)

        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF60A5FA),
                    Color(0xFF3B82F6),
                    Color(0xFF4F46E5)
                ),
                startY = flapTop,
                endY = flapTop + flapHeight
            ),
            topLeft = Offset(x = 0f, y = flapTop),
            size = Size(width = w, height = flapHeight),
            cornerRadius = CornerRadius(flapCorner, flapCorner)
        )

        // Subtle inner luminous edge on front flap
        drawRoundRect(
            color = Color.White.copy(alpha = 0.20f),
            topLeft = Offset(x = 0.5.dp.toPx(), y = flapTop + 0.5.dp.toPx()),
            size = Size(width = w - 1.dp.toPx(), height = flapHeight - 1.dp.toPx()),
            cornerRadius = CornerRadius(flapCorner, flapCorner),
            style = Stroke(width = 0.75.dp.toPx())
        )

        // 1.dp Specular white top highlight curve on front flap
        val specStroke = 1.dp.toPx()
        val specY = flapTop + specStroke * 0.7f
        val specularPath = Path().apply {
            moveTo(flapCorner * 0.45f, specY + flapCorner * 0.28f)
            quadraticTo(flapCorner * 0.7f, specY, flapCorner * 1.15f, specY)
            lineTo(w - flapCorner * 1.15f, specY)
            quadraticTo(w - flapCorner * 0.7f, specY, w - flapCorner * 0.45f, specY + flapCorner * 0.28f)
        }
        drawPath(
            path = specularPath,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.22f),
                    Color.White.copy(alpha = 0.85f),
                    Color.White.copy(alpha = 0.22f)
                )
            ),
            style = Stroke(width = specStroke, cap = StrokeCap.Round)
        )

        // Subtle inner play-glyph watermark at 0.18f alpha centered in front flap
        val cx = w * 0.50f
        val cy = flapTop + flapHeight * 0.52f
        val triRadius = flapHeight * 0.21f
        val playPath = Path().apply {
            moveTo(cx - triRadius * 0.55f, cy - triRadius * 0.85f)
            lineTo(cx + triRadius * 0.95f, cy)
            lineTo(cx - triRadius * 0.55f, cy + triRadius * 0.85f)
            close()
        }
        drawPath(
            path = playPath,
            color = Color.White.copy(alpha = 0.18f),
            style = Stroke(
                width = 1.5.dp.toPx(),
                join = StrokeJoin.Round,
                cap = StrokeCap.Round
            )
        )
        drawPath(
            path = playPath,
            color = Color.White.copy(alpha = 0.18f)
        )
    }
}

@Composable
private fun UnwatchedCountPillBadge(
    count: Int,
    modifier: Modifier = Modifier
) {
    if (count <= 0) return
    val badgeBrush = remember {
        Brush.linearGradient(
            colors = listOf(Color(0xFFFF5E62), Color(0xFFEF4444))
        )
    }
    val label = if (count > 99) "99+" else count.toString()
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 22.dp, minHeight = 22.dp)
            .shadow(3.dp, RoundedCornerShape(999.dp))
            .clip(RoundedCornerShape(999.dp))
            .background(badgeBrush)
            .border(1.5.dp, Color.White, RoundedCornerShape(999.dp))
            .padding(horizontal = if (count > 9) 5.5.dp else 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum")
        )
    }
}

@Composable
private fun buildFolderInlineMetadata(
    countSegments: List<String>,
    tailSegments: List<String>,
    isDark: Boolean
): AnnotatedString {
    val baseColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF5A6382)
    val countHighlightColor = if (isDark) {
        Color(0xFF60A5FA).copy(alpha = 0.92f)
    } else {
        Color(0xFF2563EB).copy(alpha = 0.85f)
    }
    val separatorColor = baseColor.copy(alpha = 0.55f)

    return remember(countSegments, tailSegments, isDark) {
        buildAnnotatedString {
            var hasEmitted = false
            countSegments.forEach { segment ->
                if (hasEmitted) {
                    withStyle(SpanStyle(color = separatorColor, fontFeatureSettings = "tnum")) {
                        append("  •  ")
                    }
                }
                withStyle(
                    SpanStyle(
                        color = countHighlightColor,
                        fontWeight = FontWeight.SemiBold,
                        fontFeatureSettings = "tnum"
                    )
                ) {
                    append(segment)
                }
                hasEmitted = true
            }
            tailSegments.forEach { segment ->
                if (hasEmitted) {
                    withStyle(SpanStyle(color = separatorColor, fontFeatureSettings = "tnum")) {
                        append("  •  ")
                    }
                }
                withStyle(
                    SpanStyle(
                        color = baseColor,
                        fontWeight = FontWeight.Medium,
                        fontFeatureSettings = "tnum"
                    )
                ) {
                    append(segment)
                }
                hasEmitted = true
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FrostedFolderCardShell(
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    innerPaddingHorizontal: Dp = 14.dp,
    innerPaddingVertical: Dp = 12.dp,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val palette = LocalExtendedPalette.current
    val cardShape = remember { RoundedCornerShape(20.dp) }

    val cardBg = when {
        isSelected && isDark -> palette.periwinkleBadgeBg.copy(alpha = 0.78f)
        isSelected -> Color(0xFFE3E6FA).copy(alpha = 0.90f)
        isDark -> Color(0xFF161B2E).copy(alpha = 0.72f)
        else -> Color.White.copy(alpha = 0.72f)
    }
    val borderBrush = remember(isSelected, isDark) {
        when {
            isSelected && isDark -> Brush.verticalGradient(
                listOf(Color(0xFF818CF8).copy(alpha = 0.65f), Color(0xFF6366F1).copy(alpha = 0.45f))
            )
            isSelected -> Brush.verticalGradient(
                listOf(Color(0xFF6366F1).copy(alpha = 0.65f), Color(0xFF4F46E5).copy(alpha = 0.42f))
            )
            isDark -> Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.14f))
            )
            else -> Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.90f), Color(0xFFDCE4FA).copy(alpha = 0.55f))
            )
        }
    }
    val shadowAmbient = if (isDark) Color(0x33000000) else Color(0x142563EB)

    Box(
        modifier = modifier
            .shadow(
                elevation = 2.dp,
                shape = cardShape,
                ambientColor = shadowAmbient,
                spotColor = shadowAmbient
            )
            .clip(cardShape)
            .background(cardBg)
            .border(width = 1.dp, brush = borderBrush, shape = cardShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    triggerLongPressHaptic(context, hapticFeedback)
                    onLongPress()
                }
            )
            .padding(horizontal = innerPaddingHorizontal, vertical = innerPaddingVertical)
    ) {
        content()
    }
}

@Composable
private fun CoralNewBadge(modifier: Modifier = Modifier) {
    val coralBrush = remember {
        Brush.linearGradient(listOf(FolderBadgeCoral, CoralBadgeDark))
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(coralBrush)
            .padding(horizontal = 5.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "NEW",
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp
        )
    }
}

@Composable
private fun SelectionCheckmarkBadge(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    iconSize: Dp = 15.dp
) {
    val badgeBrush = remember {
        Brush.linearGradient(
            listOf(Color(0xFF6366F1), Color(0xFF4F46E5))
        )
    }
    Box(
        modifier = modifier
            .size(size)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(badgeBrush)
            .border(1.5.dp, Color.White.copy(alpha = 0.88f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = "Selected",
            tint = Color.White,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Suppress("DEPRECATION")
private fun triggerLongPressHaptic(context: Context, hapticFeedback: HapticFeedback) {
    runCatching {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    runCatching {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (vibrator?.hasVibrator() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(15L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrator.vibrate(15L)
            }
        }
    }
}

@Composable
fun DirectoryFolderRowItem(
    node: DirectoryNode,
    settings: AppSettingsState,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val unwatchedCount = node.unwatchedVideoCount(settings.watchedVideoIds, settings.seenBaselineEpochSec)

    val countSegments = remember(node, settings.enabledFields) {
        buildList {
            if (DisplayField.TOTAL_MEDIA in settings.enabledFields) {
                if (node.directVideoCount > 0) {
                    add("${node.directVideoCount} ${if (node.directVideoCount == 1) "video" else "videos"}")
                }
                if (node.immediateFolderCount > 0) {
                    add("${node.immediateFolderCount} ${if (node.immediateFolderCount == 1) "folder" else "folders"}")
                }
                if (isEmpty() && node.recursiveVideoCount > 0) {
                    add("${node.recursiveVideoCount} ${if (node.recursiveVideoCount == 1) "video" else "videos"}")
                }
            }
        }
    }
    val tailSegments = remember(node, settings.enabledFields) {
        buildList {
            if (DisplayField.FOLDER_SIZE in settings.enabledFields) {
                add(node.formattedRecursiveSize)
            }
            if (DisplayField.TOTAL_DURATION in settings.enabledFields && node.recursiveDurationMs > 0L) {
                add(node.formattedRecursiveDuration)
            }
        }
    }
    val metadataLine = buildFolderInlineMetadata(
        countSegments = countSegments,
        tailSegments = tailSegments,
        isDark = isDark
    )

    FrostedFolderCardShell(
        isSelected = isSelected,
        isDark = isDark,
        onClick = onClick,
        onLongPress = onLongPress,
        modifier = modifier
            .fillMaxWidth()
            .testTag("tree_folder_row_${node.name}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier.size(width = 60.dp, height = 50.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                PremiumFolderIcon(
                    width = 54.dp,
                    height = 46.dp
                )

                if (isSelected) {
                    SelectionCheckmarkBadge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-1).dp)
                    )
                } else if (unwatchedCount > 0) {
                    UnwatchedCountPillBadge(
                        count = unwatchedCount,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-1).dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = node.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (metadataLine.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = metadataLine,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF5A6382).copy(alpha = 0.72f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun DirectoryFolderGridItem(
    node: DirectoryNode,
    watchedVideoIds: Set<Long>,
    seenBaselineEpochSec: Long = 0L,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {}
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val unwatchedCount = node.unwatchedVideoCount(watchedVideoIds, seenBaselineEpochSec)
    val countSegments = remember(node) {
        buildList {
            if (node.directVideoCount > 0) {
                add("${node.directVideoCount} ${if (node.directVideoCount == 1) "video" else "videos"}")
            }
            if (node.immediateFolderCount > 0) {
                add("${node.immediateFolderCount} ${if (node.immediateFolderCount == 1) "folder" else "folders"}")
            }
            if (isEmpty() && node.recursiveVideoCount > 0) {
                add("${node.recursiveVideoCount} ${if (node.recursiveVideoCount == 1) "video" else "videos"}")
            }
        }
    }
    val tailSegments = remember(node) { listOf(node.formattedRecursiveSize) }
    val metadataLine = buildFolderInlineMetadata(
        countSegments = countSegments,
        tailSegments = tailSegments,
        isDark = isDark
    )

    FrostedFolderCardShell(
        isSelected = isSelected,
        isDark = isDark,
        onClick = onClick,
        onLongPress = onLongPress,
        innerPaddingHorizontal = 12.dp,
        innerPaddingVertical = 14.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tree_folder_grid_${node.name}")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier.size(width = 66.dp, height = 56.dp),
                contentAlignment = Alignment.Center
            ) {
                PremiumFolderIcon(
                    width = 60.dp,
                    height = 50.dp
                )
                if (isSelected) {
                    SelectionCheckmarkBadge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-1).dp)
                    )
                } else if (unwatchedCount > 0) {
                    UnwatchedCountPillBadge(
                        count = unwatchedCount,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-1).dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = node.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = palette.primaryText,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (metadataLine.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = metadataLine,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun FlatFolderRowItem(
    folder: VideoFolder,
    settings: AppSettingsState,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val unwatchedCount = folder.videos.count {
        it.isConsideredNew(settings.watchedVideoIds, settings.seenBaselineEpochSec)
    }

    val countSegments = remember(folder, settings.enabledFields) {
        buildList {
            if (DisplayField.TOTAL_MEDIA in settings.enabledFields) {
                add("${folder.videoCount} ${if (folder.videoCount == 1) "video" else "videos"}")
            }
        }
    }
    val tailSegments = remember(folder, settings.enabledFields) {
        buildList {
            if (DisplayField.FOLDER_SIZE in settings.enabledFields) {
                add(folder.formattedTotalSize)
            }
            if (DisplayField.TOTAL_DURATION in settings.enabledFields && folder.totalDurationMs > 0L) {
                add(folder.formattedTotalDuration)
            }
        }
    }
    val metadataLine = buildFolderInlineMetadata(
        countSegments = countSegments,
        tailSegments = tailSegments,
        isDark = isDark
    )

    FrostedFolderCardShell(
        isSelected = isSelected,
        isDark = isDark,
        onClick = onClick,
        onLongPress = onLongPress,
        modifier = modifier
            .fillMaxWidth()
            .testTag("flat_folder_row_${folder.name}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier.size(width = 60.dp, height = 50.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                PremiumFolderIcon(
                    width = 54.dp,
                    height = 46.dp
                )
                if (isSelected) {
                    SelectionCheckmarkBadge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-1).dp)
                    )
                } else if (unwatchedCount > 0) {
                    UnwatchedCountPillBadge(
                        count = unwatchedCount,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-1).dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (metadataLine.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = metadataLine,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF5A6382).copy(alpha = 0.72f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun FlatFolderGridItem(
    folder: VideoFolder,
    unwatchedCount: Int,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {}
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val countSegments = remember(folder) {
        listOf("${folder.videoCount} ${if (folder.videoCount == 1) "video" else "videos"}")
    }
    val tailSegments = remember(folder) { listOf(folder.formattedTotalSize) }
    val metadataLine = buildFolderInlineMetadata(
        countSegments = countSegments,
        tailSegments = tailSegments,
        isDark = isDark
    )

    FrostedFolderCardShell(
        isSelected = isSelected,
        isDark = isDark,
        onClick = onClick,
        onLongPress = onLongPress,
        innerPaddingHorizontal = 12.dp,
        innerPaddingVertical = 14.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("flat_folder_grid_${folder.name}")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier.size(width = 66.dp, height = 56.dp),
                contentAlignment = Alignment.Center
            ) {
                PremiumFolderIcon(
                    width = 60.dp,
                    height = 50.dp
                )
                if (isSelected) {
                    SelectionCheckmarkBadge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-1).dp)
                    )
                } else if (unwatchedCount > 0) {
                    UnwatchedCountPillBadge(
                        count = unwatchedCount,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-1).dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = folder.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = palette.primaryText,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = metadataLine,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun DirectoryVideoRow(
    video: VideoItem,
    settings: AppSettingsState,
    imageLoader: ImageLoader,
    isSelected: Boolean = false,
    onPlay: () -> Unit,
    onLongPress: () -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onOpenActionSheet: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val palette = LocalExtendedPalette.current
    val rowBg = if (isSelected) {
        if (palette.isDark) palette.periwinkleBadgeBg.copy(alpha = 0.65f) else Color(0xFFE3E6FA)
    } else Color.Transparent
    val isUnwatched = video.isConsideredNew(settings.watchedVideoIds, settings.seenBaselineEpochSec)
    val savedPosMs = settings.playbackPositionsMs[video.id] ?: 0L
    val progressFraction = if (video.durationMs > 0 && savedPosMs > 0) {
        (savedPosMs.toFloat() / video.durationMs.toFloat()).coerceIn(0.05f, 1.0f)
    } else {
        0f
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(rowBg)
            .combinedClickable(
                onClick = onPlay,
                onLongClick = {
                    triggerLongPressHaptic(context, hapticFeedback)
                    onLongPress()
                }
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("video_row_${video.id}")
    ) {
        if (DisplayField.THUMBNAILS in settings.enabledFields) {
            Box(
                modifier = Modifier
                    .width(128.dp)
                    .height(76.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(palette.neutralChipBg)
            ) {
                val thumbnailKey = remember(
                    video.id,
                    video.uri,
                    video.filePath,
                    video.vaultFilePath,
                    video.durationMs,
                    video.dateModifiedSec,
                    video.isVaultItem,
                    video.width,
                    video.height,
                    video.rotationDegrees
                ) {
                    VideoThumbnailKey(
                        uri = video.uri,
                        filePath = video.filePath ?: video.vaultFilePath,
                        durationMs = video.durationMs,
                        dateModifiedSec = video.dateModifiedSec,
                        isVaultItem = video.isVaultItem,
                        width = video.width,
                        height = video.height,
                        orientationDegrees = video.rotationDegrees
                    )
                }
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(thumbnailKey)
                        .build(),
                    imageLoader = imageLoader,
                    contentDescription = video.displayName,
                    contentScale = ContentScale.Crop,
                    filterQuality = FilterQuality.High,
                    modifier = Modifier.fillMaxSize()
                )

                if (isUnwatched) {
                    CoralNewBadge(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(5.dp)
                    )
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color(0xFF4F46E5).copy(alpha = 0.30f))
                    )
                    SelectionCheckmarkBadge(
                        size = 32.dp,
                        iconSize = 19.dp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                if (DisplayField.DURATION in settings.enabledFields && settings.displayLengthOverThumbnail) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(5.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.76f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = video.formattedDuration,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (DisplayField.PROGRESS_BAR in settings.enabledFields && progressFraction > 0f) {
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        color = palette.brandTitleColor,
                        trackColor = Color.Black.copy(alpha = 0.35f),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(3.5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            val shownName = if (DisplayField.EXTENSION in settings.enabledFields) {
                video.displayName
            } else {
                video.title
            }
            Text(
                text = shownName,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
                color = palette.primaryText,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (DisplayField.SUBTITLE in settings.enabledFields && video.hasSubtitles) {
                    SrtSubtitleBadge(text = "SRT")
                }
                if (DisplayField.DURATION in settings.enabledFields && !settings.displayLengthOverThumbnail) {
                    MetadataChip(text = video.formattedDuration)
                }
                if (DisplayField.PLAYED_TIME in settings.enabledFields && savedPosMs > 0L) {
                    MetadataChip(text = "Played ${formatDurationMillis(savedPosMs)}")
                }
                if (DisplayField.RESOLUTION in settings.enabledFields && DisplayField.FRAMERATE in settings.enabledFields) {
                    MetadataChip(text = video.resolutionFramerateLabel)
                } else if (DisplayField.RESOLUTION in settings.enabledFields) {
                    MetadataChip(text = video.verticalPixelsLabel)
                } else if (DisplayField.FRAMERATE in settings.enabledFields) {
                    MetadataChip(text = "${video.formattedFramerate}fps")
                }
                if (DisplayField.SIZE in settings.enabledFields) {
                    MetadataChip(text = video.formattedSize)
                }
                if (DisplayField.EXTENSION in settings.enabledFields) {
                    MetadataChip(text = video.extensionLabel)
                }
                if (DisplayField.DATE in settings.enabledFields) {
                    MetadataChip(text = video.formattedDate)
                }
                if (DisplayField.CODEC_SUPPORT in settings.enabledFields) {
                    MetadataChip(text = video.videoCodec, isAccent = true)
                }
                if (DisplayField.PATH in settings.enabledFields) {
                    MetadataChip(text = video.relativePath)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun DirectoryVideoGridCard(
    video: VideoItem,
    settings: AppSettingsState,
    imageLoader: ImageLoader,
    isSelected: Boolean = false,
    onPlay: () -> Unit,
    onLongPress: () -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onOpenMenu: () -> Unit = {}
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val palette = LocalExtendedPalette.current
    val cardBg = if (isSelected) {
        if (palette.isDark) palette.periwinkleBadgeBg.copy(alpha = 0.65f) else Color(0xFFE3E6FA)
    } else Color.Transparent
    val isUnwatched = video.isConsideredNew(settings.watchedVideoIds, settings.seenBaselineEpochSec)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .combinedClickable(
                onClick = onPlay,
                onLongClick = {
                    triggerLongPressHaptic(context, hapticFeedback)
                    onLongPress()
                }
            )
            .padding(4.dp)
            .testTag("video_grid_card_${video.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(104.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.neutralChipBg)
        ) {
            val thumbnailKey = remember(
                video.id,
                video.uri,
                video.filePath,
                video.vaultFilePath,
                video.durationMs,
                video.dateModifiedSec,
                video.isVaultItem,
                video.width,
                video.height,
                video.rotationDegrees
            ) {
                VideoThumbnailKey(
                    uri = video.uri,
                    filePath = video.filePath ?: video.vaultFilePath,
                    durationMs = video.durationMs,
                    dateModifiedSec = video.dateModifiedSec,
                    isVaultItem = video.isVaultItem,
                    width = video.width,
                    height = video.height,
                    orientationDegrees = video.rotationDegrees
                )
            }
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(thumbnailKey)
                    .build(),
                imageLoader = imageLoader,
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.High,
                modifier = Modifier.fillMaxSize()
            )
            if (isUnwatched) {
                CoralNewBadge(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                )
            }
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color(0xFF4F46E5).copy(alpha = 0.30f))
                )
                SelectionCheckmarkBadge(
                    size = 34.dp,
                    iconSize = 20.dp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            if (DisplayField.DURATION in settings.enabledFields && settings.displayLengthOverThumbnail) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.76f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = video.formattedDuration,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = video.displayName,
            color = palette.primaryText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (DisplayField.SUBTITLE in settings.enabledFields && video.hasSubtitles) {
                SrtSubtitleBadge("SRT")
            }
            MetadataChip(text = video.verticalPixelsLabel)
            MetadataChip(text = video.formattedSize)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoOverflowBottomSheet(
    video: VideoItem,
    onDismiss: () -> Unit,
    onAddToPlaylist: () -> Unit = {},
    onMoveToVault: () -> Unit,
    onShowMediaInfo: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val palette = LocalExtendedPalette.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = palette.sheetBackground,
        contentColor = palette.primaryText,
        modifier = Modifier.testTag("video_overflow_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = video.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = palette.primaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (video.hasSubtitles) {
                    SrtSubtitleBadge("SRT")
                }
                MetadataChip(text = video.formattedSize)
                MetadataChip(text = video.resolutionFramerateLabel)
                MetadataChip(text = video.videoCodec, isAccent = true)
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = palette.neutralChipBg)

            // Action 0: Add to Playlist
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAddToPlaylist)
                    .padding(vertical = 14.dp)
                    .testTag("sheet_add_to_playlist_${video.id}")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                    contentDescription = null,
                    tint = palette.brandTitleColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Add to Playlist",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.primaryText
                    )
                    Text(
                        text = "Save to Favorites or a custom playlist",
                        fontSize = 12.sp,
                        color = palette.secondaryText
                    )
                }
            }

            // Action 1: Move to Private Vault
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onMoveToVault)
                    .padding(vertical = 14.dp)
                    .testTag("sheet_move_to_vault_${video.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = palette.brandTitleColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Move to Private Vault",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.primaryText
                    )
                    Text(
                        text = "Instant zero-copy move to /storage/emulated/0/AuraPlayer/.private/",
                        fontSize = 12.sp,
                        color = palette.secondaryText
                    )
                }
            }

            // Action 2: Media Info / Codecs
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onShowMediaInfo)
                    .padding(vertical = 14.dp)
                    .testTag("sheet_media_info_${video.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = palette.brandTitleColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Media Info / Codecs",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.primaryText
                    )
                    Text(
                        text = "Inspect video stream, resolution, framerate, and decoder",
                        fontSize = 12.sp,
                        color = palette.secondaryText
                    )
                }
            }

            // Action 3: Rename
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onRename)
                    .padding(vertical = 14.dp)
                    .testTag("sheet_rename_video_${video.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.DriveFileRenameOutline,
                    contentDescription = null,
                    tint = palette.brandTitleColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Rename",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.primaryText
                    )
                    Text(
                        text = "Edit display title and file name in MediaStore",
                        fontSize = 12.sp,
                        color = palette.secondaryText
                    )
                }
            }

            // Action 4: Delete
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onDelete)
                    .padding(vertical = 14.dp)
                    .testTag("sheet_delete_video_${video.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = null,
                    tint = DangerRed,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Delete",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = DangerRed
                    )
                    Text(
                        text = "Remove video file from device storage",
                        fontSize = 12.sp,
                        color = palette.secondaryText
                    )
                }
            }
        }
    }
}

@Composable
fun RenameVideoDialog(
    video: VideoItem,
    onDismiss: () -> Unit,
    onConfirmRename: (String) -> Unit
) {
    val palette = LocalExtendedPalette.current
    val initialBase = remember(video.id) {
        video.displayName.substringBeforeLast('.')
    }
    var newName by remember(video.id) { mutableStateOf(initialBase) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.sheetBackground,
        titleContentColor = palette.primaryText,
        textContentColor = palette.secondaryText,
        title = {
            Text(
                text = "Rename Video",
                style = MaterialTheme.typography.titleLarge,
                color = palette.brandTitleColor
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Enter a new filename (extension .${video.extensionLabel.lowercase()} preserved):",
                    fontSize = 13.sp,
                    color = palette.secondaryText
                )
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    label = { Text("File name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rename_video_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmRename(newName) },
                enabled = newName.trim().isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = palette.brandTitleColor),
                modifier = Modifier.testTag("rename_video_confirm_button")
            ) {
                Text("Save", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = palette.secondaryText)
            }
        }
    )
}

@Composable
fun VideoCodecInfoDialog(
    video: VideoItem,
    onDismiss: () -> Unit
) {
    val palette = LocalExtendedPalette.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.sheetBackground,
        titleContentColor = palette.primaryText,
        textContentColor = palette.secondaryText,
        title = {
            Text(
                text = "Media & Codec Info",
                style = MaterialTheme.typography.titleLarge,
                color = palette.brandTitleColor
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("File: ${video.displayName}", color = palette.primaryText, fontWeight = FontWeight.Medium)
                Text("Path: /storage/emulated/0/${video.relativePath}")
                Text("Resolution: ${video.formattedResolution} (${video.resolutionFramerateLabel})")
                Text("Video Decoder: ${video.videoCodec}")
                Text("Audio Stream: ${video.audioTrackInfo}")
                Text("Subtitle Track: ${video.subtitleInfo}")
                Text("Duration: ${video.formattedDuration}")
                Text("Size: ${video.formattedSize} (${video.mimeType})")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = palette.brandTitleColor)
            }
        }
    )
}
