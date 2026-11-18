package com.istiak.auraplayer

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.onGloballyPositioned
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
import com.istiak.auraplayer.ui.components.ExpressivePullToRefreshBox
import com.istiak.auraplayer.ui.components.ScrollAwareFixedHeader
import com.istiak.auraplayer.ui.components.recordFrostedBackdrop
import com.istiak.auraplayer.ui.components.rememberScrollAwareGlassLayout
import com.istiak.auraplayer.ui.theme.DangerRed
import com.istiak.auraplayer.ui.theme.LocalExtendedPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Hierarchical Tree View, Flat Folder View, and Library View browser (`DirectoryBrowserScreen.kt`):
 * - Uses `rememberScrollAwareGlassLayout(listState = listState, defaultHeaderHeight = 68.dp, extraTopGap = 0.dp)`
 *   to measure the Top Header dynamically via `onGloballyPositioned`, ramp glass opacity from `0f` (when idle at scroll top) to `1f`
 *   (over `48.dp`), and record the scrolling `LazyColumn` / `LazyVerticalGrid` via
 *   `.recordFrostedBackdrop(scrollLayout.backdropState, listState, scrollLayout.contentGraphicsLayer)`.
 * - Renders 100% of the Top Header (status bar insets, title/back arrow, top-right icons, and
 *   integrated breadcrumb bar when inside a subfolder) inside `ScrollAwareFixedHeader(layoutState = scrollLayout) { ... }`
 *   so the Liquid Glass blur, tint, rim refraction, and specular catchlight extend cut-to-cut to the bottom edge.
 * - Wrapped inside `ExpressivePullToRefreshBox` with downward list displacement physics and an Aura Blue
 *   Material 3 Expressive morphing polygon indicator positioned below `scrollLayout.measuredHeaderHeightDp`.
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
    playbackProgress: PlaybackProgressSnapshot = PlaybackProgressSnapshot(),
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
    bottomContentPadding: Dp = 124.dp,
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
        defaultHeaderHeight = 68.dp,
        extraTopGap = 0.dp,
        bottomNavClearance = 124.dp,
        gridState = gridState,
        useGridState = isGridLayout,
        horizontalPadding = if (isGridLayout) 16.dp else 0.dp
    )

    var internalRefreshing by remember { mutableStateOf(false) }
    var showLibraryUpdatedPill by remember { mutableStateOf(false) }
    var libraryUpdatedMessage by remember { mutableStateOf("Library updated") }
    val effectiveRefreshing = isRefreshing || internalRefreshing

    val cleanTreePath = currentTreePath.trim('/')
    val canNavigateBackInBrowser = !isMultiSelectMode &&
        ((isTreeMode && cleanTreePath.isNotEmpty()) ||
            (isFolderMode && selectedFlatFolderPath != null))

    // Reset list/grid scroll position safely when switching folders or tabs so transitioning from a
    // subfolder back to root never leaves LazyListState pointing at an out-of-range item index.
    LaunchedEffect(cleanTreePath, selectedFlatFolderPath, selectedTab) {
        runCatching { listState.scrollToItem(0) }
        runCatching { gridState.scrollToItem(0) }
    }

    // Enabled strictly when inside a subfolder (currentTreePath != rootPath / selectedFlatFolderPath != null)
    BackHandler(enabled = canNavigateBackInBrowser) {
        if (isTreeMode && cleanTreePath.isNotEmpty()) {
            onNavigateUpTree()
        } else if (isFolderMode && selectedFlatFolderPath != null) {
            onSelectFlatFolder(null)
        }
    }

    // Innermost BackHandler: always exit multi-select mode first without leaving the current folder.
    BackHandler(enabled = isMultiSelectMode) {
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
        if (!matchesQuickFilter(
                video = video,
                filter = selectedQuickFilter,
                watchedVideoIds = settings.watchedVideoIds,
                seenBaselineEpochSec = settings.seenBaselineEpochSec,
                showNewLabel = settings.showNewVideoLabel,
                newLabelDaysThreshold = settings.newVideoLabelDaysThreshold
            )
        ) {
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
        settings.seenBaselineEpochSec,
        settings.showNewVideoLabel,
        settings.newVideoLabelDaysThreshold
    ) {
        if (!isFilterActive) allVideos
        else allVideos.filter { videoMatchesActiveFilters(it, matchFolderNameOnAll = true) }
    }

    val filteredFlatFolders = remember(
        flatFolders,
        trimmedQuery,
        selectedQuickFilter,
        settings.watchedVideoIds,
        settings.seenBaselineEpochSec,
        settings.showNewVideoLabel,
        settings.newVideoLabelDaysThreshold
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
        settings.seenBaselineEpochSec,
        settings.showNewVideoLabel,
        settings.newVideoLabelDaysThreshold
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
        settings.showNewVideoLabel,
        settings.newVideoLabelDaysThreshold,
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

    val measuredHeaderTopDp = scrollLayout.measuredHeaderHeightDp
    val listContentPadding = scrollLayout.contentPadding

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        ExpressivePullToRefreshBox(
            isRefreshing = effectiveRefreshing,
            onRefresh = {
                internalRefreshing = true
                onRefresh()
                coroutineScope.launch {
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
            headerTopPadding = measuredHeaderTopDp,
            isAtTop = {
                if (isGridLayout) {
                    gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0
                } else {
                    listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                }
            },
            indicatorTestTag = "pull_to_refresh_indicator",
            modifier = Modifier
                .fillMaxSize()
                .testTag("directory_pull_to_refresh_box")
        ) {
            when {
                isLibraryMode -> {
                    AllVideosScreen(
                        videos = filteredLibraryVideos,
                        isFilterActive = isFilterActive,
                        isGridLayout = isGridLayout,
                        listState = listState,
                        gridState = gridState,
                        scrollLayout = scrollLayout,
                        listContentPadding = listContentPadding,
                        settings = settings,
                        imageLoader = imageLoader,
                        playbackProgress = playbackProgress,
                        selectedIds = selectedIds,
                        isMultiSelectMode = isMultiSelectMode,
                        onToggleVideoSelection = onToggleVideoSelection,
                        onPlayVideo = onPlayVideo,
                        onOpenVideoMenu = onOpenVideoMenu
                    )
                }

                isFolderMode -> {
                    if (activeFlatFolder != null) {
                        if (isGridLayout) {
                            LazyVerticalGrid(
                                state = gridState,
                                columns = GridCells.Adaptive(minSize = 165.dp),
                                contentPadding = listContentPadding,
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
                                    item(
                                        span = { GridItemSpan(maxLineSpan) },
                                        key = "empty_filter_flat_grid",
                                        contentType = "empty_notice"
                                    ) {
                                        FilteredEmptyNotice()
                                    }
                                }
                                items(
                                    items = activeFlatFolder.videos,
                                    key = { it.id },
                                    contentType = { "video_item" }
                                ) { video ->
                                    val isSelected = video.id in selectedIds
                                    val onPlayCallback = remember(video, activeFlatFolder.videos, isMultiSelectMode, onToggleVideoSelection, onPlayVideo) {
                                        {
                                            if (isMultiSelectMode) onToggleVideoSelection(video)
                                            else onPlayVideo(video, activeFlatFolder.videos)
                                        }
                                    }
                                    val onLongPressCallback = remember(video, onToggleVideoSelection) {
                                        { onToggleVideoSelection(video) }
                                    }
                                    val onMenuCallback = remember(video, isMultiSelectMode, onToggleVideoSelection, onOpenVideoMenu) {
                                        {
                                            if (isMultiSelectMode) onToggleVideoSelection(video)
                                            else onOpenVideoMenu(video)
                                        }
                                    }
                                    DirectoryVideoGridCard(
                                        video = video,
                                        settings = settings,
                                        imageLoader = imageLoader,
                                        playbackProgress = playbackProgress,
                                        isSelected = isSelected,
                                        onPlay = onPlayCallback,
                                        onLongPress = onLongPressCallback,
                                        onOpenMenu = onMenuCallback
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
                                        scrollLayout.backdropState,
                                        listState,
                                        scrollLayout.contentGraphicsLayer
                                    )
                                    .testTag("flat_folder_videos_list")
                            ) {
                                if (activeFlatFolder.videos.isEmpty() && isFilterActive) {
                                    item(
                                        key = "empty_filter_flat_list",
                                        contentType = "empty_notice"
                                    ) {
                                        FilteredEmptyNotice()
                                    }
                                }
                                items(
                                    items = activeFlatFolder.videos,
                                    key = { it.id },
                                    contentType = { "video_item" }
                                ) { video ->
                                    val isSelected = video.id in selectedIds
                                    val onPlayCallback = remember(video, activeFlatFolder.videos, isMultiSelectMode, onToggleVideoSelection, onPlayVideo) {
                                        {
                                            if (isMultiSelectMode) onToggleVideoSelection(video)
                                            else onPlayVideo(video, activeFlatFolder.videos)
                                        }
                                    }
                                    val onLongPressCallback = remember(video, onToggleVideoSelection) {
                                        { onToggleVideoSelection(video) }
                                    }
                                    val onActionSheetCallback = remember(video, isMultiSelectMode, onToggleVideoSelection, onOpenVideoMenu) {
                                        {
                                            if (isMultiSelectMode) onToggleVideoSelection(video)
                                            else onOpenVideoMenu(video)
                                        }
                                    }
                                    VideoItemCard(
                                        video = video,
                                        settings = settings,
                                        imageLoader = imageLoader,
                                        playbackProgress = playbackProgress,
                                        isSelected = isSelected,
                                        onPlay = onPlayCallback,
                                        onLongPress = onLongPressCallback,
                                        onOpenActionSheet = onActionSheetCallback
                                    )
                                }
                            }
                        }
                    } else if (isGridLayout) {
                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Adaptive(minSize = 155.dp),
                            contentPadding = listContentPadding,
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
                                item(
                                    span = { GridItemSpan(maxLineSpan) },
                                    key = "empty_filter_folder_grid",
                                    contentType = "empty_notice"
                                ) {
                                    FilteredEmptyNotice()
                                }
                            }
                            items(
                                items = filteredFlatFolders,
                                key = { it.path },
                                contentType = { "folder_item" }
                            ) { folder ->
                                val isFolderSelected = folder.videos.isNotEmpty() &&
                                    folder.videos.all { it.id in selectedIds }
                                val onClickCallback = remember(folder, isMultiSelectMode, onToggleFolderSelection, onSelectFlatFolder) {
                                    {
                                        if (isMultiSelectMode) onToggleFolderSelection(folder.videos)
                                        else onSelectFlatFolder(folder.path)
                                    }
                                }
                                val onLongPressCallback = remember(folder, onToggleFolderSelection) {
                                    { onToggleFolderSelection(folder.videos) }
                                }
                                FlatFolderGridItem(
                                    folder = folder,
                                    unwatchedCount = folder.videos.count {
                                        it.isConsideredNew(
                                            watchedVideoIds = settings.watchedVideoIds,
                                            seenBaselineEpochSec = settings.seenBaselineEpochSec,
                                            showNewLabel = settings.showNewVideoLabel,
                                            newLabelDaysThreshold = settings.newVideoLabelDaysThreshold
                                        )
                                    },
                                    showFullNames = settings.showFullNames,
                                    isSelected = isFolderSelected,
                                    onClick = onClickCallback,
                                    onLongPress = onLongPressCallback
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
                                    scrollLayout.backdropState,
                                    listState,
                                    scrollLayout.contentGraphicsLayer
                                )
                                .testTag("folders_list")
                        ) {
                            if (filteredFlatFolders.isEmpty() && isFilterActive) {
                                item(
                                    key = "empty_filter_folder_list",
                                    contentType = "empty_notice"
                                ) {
                                    FilteredEmptyNotice()
                                }
                            }
                            items(
                                items = filteredFlatFolders,
                                key = { it.path },
                                contentType = { "folder_item" }
                            ) { folder ->
                                val isFolderSelected = folder.videos.isNotEmpty() &&
                                    folder.videos.all { it.id in selectedIds }
                                val onClickCallback = remember(folder, isMultiSelectMode, onToggleFolderSelection, onSelectFlatFolder) {
                                    {
                                        if (isMultiSelectMode) onToggleFolderSelection(folder.videos)
                                        else onSelectFlatFolder(folder.path)
                                    }
                                }
                                val onLongPressCallback = remember(folder, onToggleFolderSelection) {
                                    { onToggleFolderSelection(folder.videos) }
                                }
                                FlatFolderRowItem(
                                    folder = folder,
                                    settings = settings,
                                    isSelected = isFolderSelected,
                                    onClick = onClickCallback,
                                    onLongPress = onLongPressCallback,
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
                            contentPadding = listContentPadding,
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
                                item(
                                    span = { GridItemSpan(maxLineSpan) },
                                    key = "storage_volume_switcher",
                                    contentType = "volume_switcher"
                                ) {
                                    FrostedStorageVolumeSwitcherRow(
                                        volumes = mountedStorageVolumes,
                                        selectedVolumeId = selectedStorageVolumeId,
                                        onSelectVolume = onSelectStorageVolume,
                                        modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                                    )
                                }
                            }

                            if (subFolders.isEmpty() && directVideos.isEmpty() && isFilterActive) {
                                item(
                                    span = { GridItemSpan(maxLineSpan) },
                                    key = "empty_filter_tree_grid",
                                    contentType = "empty_notice"
                                ) {
                                    FilteredEmptyNotice()
                                }
                            }

                            if (!isAtRoot && subFolders.isNotEmpty()) {
                                item(
                                    span = { GridItemSpan(maxLineSpan) },
                                    key = "header_folders",
                                    contentType = "section_header"
                                ) {
                                    SectionHeaderLabel(
                                        title = "Folders",
                                        color = palette.sectionHeaderBlue,
                                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                                    )
                                }
                            }

                            items(
                                items = subFolders,
                                key = { "folder_${it.fullPath}" },
                                contentType = { "folder_item" }
                            ) { node ->
                                val isFolderSelected = node.allRecursiveVideos.isNotEmpty() &&
                                    node.allRecursiveVideos.all { it.id in selectedIds }
                                val onClickCallback = remember(node, isMultiSelectMode, onToggleFolderSelection, onNavigateToTreePath) {
                                    {
                                        if (isMultiSelectMode) onToggleFolderSelection(node.allRecursiveVideos)
                                        else onNavigateToTreePath(node.fullPath)
                                    }
                                }
                                val onLongPressCallback = remember(node, onToggleFolderSelection) {
                                    { onToggleFolderSelection(node.allRecursiveVideos) }
                                }
                                DirectoryFolderGridItem(
                                    node = node,
                                    settings = settings,
                                    isSelected = isFolderSelected,
                                    onClick = onClickCallback,
                                    onLongPress = onLongPressCallback
                                )
                            }

                            if (!isAtRoot && directVideos.isNotEmpty()) {
                                item(
                                    span = { GridItemSpan(maxLineSpan) },
                                    key = "header_videos",
                                    contentType = "section_header"
                                ) {
                                    SectionHeaderLabel(
                                        title = "Videos",
                                        color = palette.sectionHeaderBlue,
                                        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                                    )
                                }
                            }

                            items(
                                items = directVideos,
                                key = { it.id },
                                contentType = { "video_item" }
                            ) { video ->
                                val isSelected = video.id in selectedIds
                                val onPlayCallback = remember(video, directVideos, isMultiSelectMode, onToggleVideoSelection, onPlayVideo) {
                                    {
                                        if (isMultiSelectMode) onToggleVideoSelection(video)
                                        else onPlayVideo(video, directVideos)
                                    }
                                }
                                val onLongPressCallback = remember(video, onToggleVideoSelection) {
                                    { onToggleVideoSelection(video) }
                                }
                                val onMenuCallback = remember(video, isMultiSelectMode, onToggleVideoSelection, onOpenVideoMenu) {
                                    {
                                        if (isMultiSelectMode) onToggleVideoSelection(video)
                                        else onOpenVideoMenu(video)
                                    }
                                }
                                DirectoryVideoGridCard(
                                    video = video,
                                    settings = settings,
                                    imageLoader = imageLoader,
                                    playbackProgress = playbackProgress,
                                    isSelected = isSelected,
                                    onPlay = onPlayCallback,
                                    onLongPress = onLongPressCallback,
                                    onOpenMenu = onMenuCallback
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
                                    scrollLayout.backdropState,
                                    listState,
                                    scrollLayout.contentGraphicsLayer
                                )
                                .testTag(if (isAtRoot) "tree_root_list" else "tree_directory_list")
                        ) {
                            if (isAtRoot && mountedStorageVolumes.size > 1) {
                                item(
                                    key = "storage_volume_switcher",
                                    contentType = "volume_switcher"
                                ) {
                                    FrostedStorageVolumeSwitcherRow(
                                        volumes = mountedStorageVolumes,
                                        selectedVolumeId = selectedStorageVolumeId,
                                        onSelectVolume = onSelectStorageVolume,
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            if (subFolders.isEmpty() && directVideos.isEmpty() && isFilterActive) {
                                item(
                                    key = "empty_filter_tree_list",
                                    contentType = "empty_notice"
                                ) {
                                    FilteredEmptyNotice()
                                }
                            }

                            if (!isAtRoot && subFolders.isNotEmpty()) {
                                item(
                                    key = "section_folders",
                                    contentType = "section_header"
                                ) {
                                    SectionHeaderLabel(
                                        title = "Folders",
                                        color = palette.sectionHeaderBlue,
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            items(
                                items = subFolders,
                                key = { "folder_${it.fullPath}" },
                                contentType = { "folder_item" }
                            ) { node ->
                                val isFolderSelected = node.allRecursiveVideos.isNotEmpty() &&
                                    node.allRecursiveVideos.all { it.id in selectedIds }
                                val onClickCallback = remember(node, isMultiSelectMode, onToggleFolderSelection, onNavigateToTreePath) {
                                    {
                                        if (isMultiSelectMode) onToggleFolderSelection(node.allRecursiveVideos)
                                        else onNavigateToTreePath(node.fullPath)
                                    }
                                }
                                val onLongPressCallback = remember(node, onToggleFolderSelection) {
                                    { onToggleFolderSelection(node.allRecursiveVideos) }
                                }
                                DirectoryFolderRowItem(
                                    node = node,
                                    settings = settings,
                                    isSelected = isFolderSelected,
                                    onClick = onClickCallback,
                                    onLongPress = onLongPressCallback,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                            }

                            if (directVideos.isNotEmpty()) {
                                if (!isAtRoot) {
                                    item(
                                        key = "section_videos",
                                        contentType = "section_header"
                                    ) {
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
                                items(
                                    items = directVideos,
                                    key = { it.id },
                                    contentType = { "video_item" }
                                ) { video ->
                                    val isSelected = video.id in selectedIds
                                    val onPlayCallback = remember(video, directVideos, isMultiSelectMode, onToggleVideoSelection, onPlayVideo) {
                                        {
                                            if (isMultiSelectMode) onToggleVideoSelection(video)
                                            else onPlayVideo(video, directVideos)
                                        }
                                    }
                                    val onLongPressCallback = remember(video, onToggleVideoSelection) {
                                        { onToggleVideoSelection(video) }
                                    }
                                    val onActionSheetCallback = remember(video, isMultiSelectMode, onToggleVideoSelection, onOpenVideoMenu) {
                                        {
                                            if (isMultiSelectMode) onToggleVideoSelection(video)
                                            else onOpenVideoMenu(video)
                                        }
                                    }
                                    VideoItemCard(
                                        video = video,
                                        settings = settings,
                                        imageLoader = imageLoader,
                                        playbackProgress = playbackProgress,
                                        isSelected = isSelected,
                                        onPlay = onPlayCallback,
                                        onLongPress = onLongPressCallback,
                                        onOpenActionSheet = onActionSheetCallback
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Subtle "Library updated" feedback pill below the floating top header pill
        androidx.compose.animation.AnimatedVisibility(
            visible = showLibraryUpdatedPill,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = measuredHeaderTopDp + 10.dp)
        ) {
            val primaryAccent = MaterialTheme.colorScheme.primary
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .shadow(8.dp, RoundedCornerShape(999.dp))
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isDark) palette.sheetBackground.copy(alpha = 0.94f) else Color.White.copy(alpha = 0.94f))
                    .border(1.dp, primaryAccent.copy(alpha = 0.30f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 14.dp, vertical = 7.dp)
                    .testTag("library_updated_pill")
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = primaryAccent,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = libraryUpdatedMessage,
                    color = palette.primaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Scroll-aware, cut-to-cut Liquid Glass top header overlaid at TopCenter
        // covering status bar, title/actions, and breadcrumbs with bottom-edge refraction & reflection.
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
                    .onGloballyPositioned { coordinates ->
                        val h = coordinates.size.height
                        if (h > 0 && scrollLayout.headerHeightPxState.intValue != h) {
                            scrollLayout.headerHeightPxState.intValue = h
                        }
                    }
            )
        } else {
            ScrollAwareFixedHeader(
                layoutState = scrollLayout,
                blurRadius = 28.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
            ) {
                LiquidGlassHeaderContent(
                    title = topTitle,
                    isDrilledDown = isDrilledDown,
                    currentPath = if (isTreeMode) cleanTreePath else "",
                    showSearchBar = showSearchBar,
                    searchQuery = searchQuery,
                    onNavigateBack = {
                        if (isTreeMode && cleanTreePath.isNotEmpty()) onNavigateUpTree()
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
                    onSelectQuickFilter = onSelectQuickFilter,
                    showFullNames = settings.showFullNames,
                    modifier = Modifier.fillMaxWidth()
                )
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

    val primaryAccent = MaterialTheme.colorScheme.primary
    val pillBg = if (isDark) palette.sheetBackground.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.82f)
    val pillBorder = if (isDark) Color.White.copy(alpha = 0.16f) else palette.outlineBorder.copy(alpha = 0.90f)

    Box(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .shadow(
                    elevation = 2.dp,
                    shape = RoundedCornerShape(999.dp),
                    ambientColor = primaryAccent.copy(alpha = 0.08f),
                    spotColor = primaryAccent.copy(alpha = 0.10f)
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
                tint = primaryAccent,
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
    RenameFileDialog(
        video = video,
        onDismiss = onDismiss,
        onConfirmRename = onConfirmRename
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
