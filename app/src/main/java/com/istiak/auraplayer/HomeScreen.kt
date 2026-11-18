package com.istiak.auraplayer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import com.istiak.auraplayer.ui.theme.LocalExtendedPalette

@Composable
fun rememberVideoImageLoader(): ImageLoader {
    val context = LocalContext.current
    return remember(context.applicationContext) {
        ImageLoader.Builder(context.applicationContext)
            .components {
                add(VideoThumbnailFetcher.Factory(context.applicationContext))
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(false)
            .build()
    }
}

/**
 * Edge-to-Edge Scroll-Beneath Home Screen (`HomeScreen.kt`):
 * - Full-bleed Box layered over `PastelMeshBackground` so `DirectoryBrowserScreen`
 *   fills the entire screen (`Modifier.fillMaxSize()`) and visibly scrolls underneath both the
 *   feathered `ScrollAwareFixedHeader` (with integrated breadcrumb trail) and `FrostedGlassSurface`
 *   Bottom Navigation Capsule.
 * - Floating Resume-Play FAB styled as a vibrant royal-blue gradient circle (`#3B82F6` -> `#2563EB`)
 *   with a soft blue glow shadow.
 */
@Composable
fun HomeScreen(
    selectedTab: MainNavTab,
    videos: List<VideoItem>,
    folders: List<VideoFolder>,
    directoryRoot: DirectoryNode,
    currentTreePath: String,
    selectedFlatFolderPath: String?,
    settings: AppSettingsState,
    playbackProgress: PlaybackProgressSnapshot = PlaybackProgressSnapshot(),
    searchQuery: String,
    hasMediaPermission: Boolean,
    isLoading: Boolean,
    isRefreshing: Boolean = false,
    onRefreshLibrary: () -> Unit = {},
    onVideosUpdatedFromPullRefresh: (List<VideoItem>) -> Unit = {},
    selectedIds: Set<Long> = emptySet(),
    onToggleVideoSelection: (VideoItem) -> Unit = {},
    onToggleFolderSelection: (List<VideoItem>) -> Unit = {},
    onClearSelection: () -> Unit = {},
    onSelectAllInView: (List<VideoItem>) -> Unit = {},
    onInvertSelectionInView: (List<VideoItem>) -> Unit = {},
    onPlaySelected: () -> Unit = {},
    onOpenMediaInfo: (VideoItem) -> Unit = {},
    onShareSelected: () -> Unit = {},
    onRequestPermission: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onOpenSortSheet: () -> Unit,
    onOpenVault: () -> Unit,
    onOpenSettings: () -> Unit,
    onMarkAllAsSeen: () -> Unit = {},
    onNavigateToTreePath: (String) -> Unit,
    onNavigateUpTree: () -> Unit,
    onSelectFlatFolder: (String?) -> Unit,
    onPlayVideo: (VideoItem, List<VideoItem>) -> Unit,
    onResumeLastPlayed: () -> Unit,
    onAddVideoToPlaylist: (VideoItem) -> Unit = {},
    onMoveToVault: (VideoItem) -> Unit,
    onRenameVideo: (VideoItem, String) -> Unit,
    onDeleteVideo: (VideoItem) -> Unit,
    onGenerateSampleVideos: () -> Unit,
    mountedStorageVolumes: List<StorageVolumeOption> = emptyList(),
    selectedStorageVolumeId: String = PRIMARY_STORAGE_VOLUME_ID,
    onSelectStorageVolume: (String) -> Unit = {},
    selectedQuickFilter: LibraryQuickFilter = LibraryQuickFilter.ALL,
    onSelectQuickFilter: (LibraryQuickFilter) -> Unit = {},
    playlistsCount: Int = 1,
    onOpenPlaylistsBrowser: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalExtendedPalette.current
    val imageLoader = rememberVideoImageLoader()
    var showSearchBar by remember { mutableStateOf(searchQuery.isNotEmpty()) }
    var actionSheetVideo by remember { mutableStateOf<VideoItem?>(null) }
    var renameTargetVideo by remember { mutableStateOf<VideoItem?>(null) }

    val isMultiSelectMode = selectedIds.isNotEmpty()
    val isDrilledInTree = selectedTab == MainNavTab.HOME &&
        settings.viewMode == LibraryViewMode.TREE &&
        currentTreePath.isNotEmpty()
    val isDrilledInFlatFolder = selectedTab == MainNavTab.HOME &&
        settings.viewMode == LibraryViewMode.FOLDER &&
        selectedFlatFolderPath != null
    val isDrilledDown = isDrilledInTree || isDrilledInFlatFolder

    val currentViewVideos = remember(
        selectedTab,
        settings.viewMode,
        directoryRoot,
        currentTreePath,
        folders,
        selectedFlatFolderPath,
        videos
    ) {
        when {
            selectedTab == MainNavTab.ALL_VIDEOS || settings.viewMode == LibraryViewMode.LIBRARY -> videos
            settings.viewMode == LibraryViewMode.FOLDER && selectedFlatFolderPath != null -> {
                folders.find { it.path == selectedFlatFolderPath || it.name == selectedFlatFolderPath }?.videos ?: videos
            }
            settings.viewMode == LibraryViewMode.TREE && currentTreePath.isNotEmpty() -> {
                val node = DirectoryTreeBuilder.getNodeAtPath(directoryRoot, currentTreePath)
                node.allRecursiveVideos.ifEmpty { videos }
            }
            else -> videos
        }
    }

    val cleanTreePath = currentTreePath.trim('/')
    val topTitle = when {
        selectedTab == MainNavTab.ALL_VIDEOS -> "All Videos"
        isDrilledInTree -> cleanTreePath.substringAfterLast('/').ifBlank { "Videos" }
        isDrilledInFlatFolder -> selectedFlatFolderPath?.trim('/')?.substringAfterLast('/')?.ifBlank { "Folder" } ?: "Folder"
        else -> "Videos"
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val fallbackTopContentPadding = statusBarTop + 72.dp +
        (if (isDrilledInTree) 36.dp else 0.dp) +
        (if (showSearchBar) 54.dp else 0.dp)
    val bottomContentPadding = navBarBottom + 124.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen")
    ) {
        when {
            !hasMediaPermission -> {
                PermissionRequiredSection(
                    onRequestPermission = onRequestPermission,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = fallbackTopContentPadding, bottom = bottomContentPadding)
                )
                LiquidGlassTopHeader(
                    title = topTitle,
                    isDrilledDown = isDrilledDown,
                    currentPath = if (isDrilledInTree) currentTreePath else "",
                    showSearchBar = showSearchBar,
                    searchQuery = searchQuery,
                    onNavigateBack = {
                        if (isDrilledInTree) onNavigateUpTree()
                        else onSelectFlatFolder(null)
                    },
                    onNavigateToPath = onNavigateToTreePath,
                    onOpenVault = onOpenVault,
                    onToggleSearch = {
                        showSearchBar = !showSearchBar
                        if (!showSearchBar) {
                            onSearchQueryChange("")
                            onSelectQuickFilter(LibraryQuickFilter.ALL)
                        }
                    },
                    onSearchQueryChange = onSearchQueryChange,
                    onOpenSortDialog = onOpenSortSheet,
                    onOpenSettings = onOpenSettings,
                    onMarkAllAsSeen = onMarkAllAsSeen,
                    selectedQuickFilter = selectedQuickFilter,
                    onSelectQuickFilter = onSelectQuickFilter,
                    showFullNames = settings.showFullNames,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
            isLoading && videos.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = palette.brandTitleColor)
                }
                LiquidGlassTopHeader(
                    title = topTitle,
                    isDrilledDown = isDrilledDown,
                    currentPath = if (isDrilledInTree) currentTreePath else "",
                    showSearchBar = showSearchBar,
                    searchQuery = searchQuery,
                    onNavigateBack = {
                        if (isDrilledInTree) onNavigateUpTree()
                        else onSelectFlatFolder(null)
                    },
                    onNavigateToPath = onNavigateToTreePath,
                    onOpenVault = onOpenVault,
                    onToggleSearch = {
                        showSearchBar = !showSearchBar
                        if (!showSearchBar) {
                            onSearchQueryChange("")
                            onSelectQuickFilter(LibraryQuickFilter.ALL)
                        }
                    },
                    onSearchQueryChange = onSearchQueryChange,
                    onOpenSortDialog = onOpenSortSheet,
                    onOpenSettings = onOpenSettings,
                    onMarkAllAsSeen = onMarkAllAsSeen,
                    selectedQuickFilter = selectedQuickFilter,
                    onSelectQuickFilter = onSelectQuickFilter,
                    showFullNames = settings.showFullNames,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
            videos.isEmpty() -> {
                EmptyLibrarySection(
                    onGenerateSampleVideos = onGenerateSampleVideos,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = fallbackTopContentPadding, bottom = bottomContentPadding)
                )
                LiquidGlassTopHeader(
                    title = topTitle,
                    isDrilledDown = isDrilledDown,
                    currentPath = if (isDrilledInTree) currentTreePath else "",
                    showSearchBar = showSearchBar,
                    searchQuery = searchQuery,
                    onNavigateBack = {
                        if (isDrilledInTree) onNavigateUpTree()
                        else onSelectFlatFolder(null)
                    },
                    onNavigateToPath = onNavigateToTreePath,
                    onOpenVault = onOpenVault,
                    onToggleSearch = {
                        showSearchBar = !showSearchBar
                        if (!showSearchBar) {
                            onSearchQueryChange("")
                            onSelectQuickFilter(LibraryQuickFilter.ALL)
                        }
                    },
                    onSearchQueryChange = onSearchQueryChange,
                    onOpenSortDialog = onOpenSortSheet,
                    onOpenSettings = onOpenSettings,
                    onMarkAllAsSeen = onMarkAllAsSeen,
                    selectedQuickFilter = selectedQuickFilter,
                    onSelectQuickFilter = onSelectQuickFilter,
                    showFullNames = settings.showFullNames,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
            else -> {
                DirectoryBrowserScreen(
                    selectedTab = selectedTab,
                    currentTreePath = currentTreePath,
                    directoryRoot = directoryRoot,
                    flatFolders = folders,
                    selectedFlatFolderPath = selectedFlatFolderPath,
                    allVideos = videos,
                    settings = settings,
                    imageLoader = imageLoader,
                    playbackProgress = playbackProgress,
                    selectedIds = selectedIds,
                    onToggleVideoSelection = onToggleVideoSelection,
                    onToggleFolderSelection = onToggleFolderSelection,
                    onNavigateToTreePath = onNavigateToTreePath,
                    onNavigateUpTree = onNavigateUpTree,
                    onSelectFlatFolder = onSelectFlatFolder,
                    onPlayVideo = onPlayVideo,
                    onOpenVideoMenu = { actionSheetVideo = it },
                    topTitle = topTitle,
                    isDrilledDown = isDrilledDown,
                    showSearchBar = showSearchBar,
                    searchQuery = searchQuery,
                    selectedQuickFilter = selectedQuickFilter,
                    onSelectQuickFilter = onSelectQuickFilter,
                    playlistsCount = playlistsCount,
                    onOpenPlaylistsBrowser = onOpenPlaylistsBrowser,
                    onToggleSearch = {
                        showSearchBar = !showSearchBar
                        if (!showSearchBar) {
                            onSearchQueryChange("")
                            onSelectQuickFilter(LibraryQuickFilter.ALL)
                        }
                    },
                    onSearchQueryChange = onSearchQueryChange,
                    onOpenVault = onOpenVault,
                    onOpenSortSheet = onOpenSortSheet,
                    onOpenSettings = onOpenSettings,
                    onMarkAllAsSeen = onMarkAllAsSeen,
                    currentViewVideos = currentViewVideos,
                    onClearSelection = onClearSelection,
                    onSelectAllInView = onSelectAllInView,
                    onInvertSelectionInView = onInvertSelectionInView,
                    onPlaySelected = onPlaySelected,
                    onOpenMediaInfo = onOpenMediaInfo,
                    onShareSelected = onShareSelected,
                    isRefreshing = isRefreshing,
                    onRefresh = onRefreshLibrary,
                    onScannedVideosUpdated = onVideosUpdatedFromPullRefresh,
                    mountedStorageVolumes = mountedStorageVolumes,
                    selectedStorageVolumeId = selectedStorageVolumeId,
                    onSelectStorageVolume = onSelectStorageVolume,
                    bottomContentPadding = bottomContentPadding,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (actionSheetVideo != null) {
        val targetVideo = actionSheetVideo!!
        VideoOverflowBottomSheet(
            video = targetVideo,
            onDismiss = { actionSheetVideo = null },
            onAddToPlaylist = {
                actionSheetVideo = null
                onAddVideoToPlaylist(targetVideo)
            },
            onMoveToVault = {
                actionSheetVideo = null
                onMoveToVault(targetVideo)
            },
            onShowMediaInfo = {
                actionSheetVideo = null
                onOpenMediaInfo(targetVideo)
            },
            onRename = {
                actionSheetVideo = null
                renameTargetVideo = targetVideo
            },
            onDelete = {
                actionSheetVideo = null
                onDeleteVideo(targetVideo)
            }
        )
    }

    if (renameTargetVideo != null) {
        val targetVideo = renameTargetVideo!!
        RenameFileDialog(
            video = targetVideo,
            onDismiss = { renameTargetVideo = null },
            onConfirmRename = { newBaseName ->
                renameTargetVideo = null
                onRenameVideo(targetVideo, newBaseName)
            }
        )
    }
}

@Composable
private fun PermissionRequiredSection(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalExtendedPalette.current
    Box(
        modifier = modifier.padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.VideoFile,
                contentDescription = null,
                tint = palette.brandTitleColor,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Media Permission Required",
                style = MaterialTheme.typography.titleLarge,
                color = palette.primaryText
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Allow video access to scan folders, 4K streams, and manage your Private Vault.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.secondaryText
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.brandTitleColor
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("grant_permission_button")
            ) {
                Text("Grant Video Access", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun EmptyLibrarySection(
    onGenerateSampleVideos: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalExtendedPalette.current
    Box(
        modifier = modifier.padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = null,
                tint = palette.folderIconTint,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No Videos Found",
                style = MaterialTheme.typography.titleLarge,
                color = palette.primaryText
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Populate MediaStore with sample 4K & 1080p videos to explore directory trees and test playback.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.secondaryText
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onGenerateSampleVideos,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.periwinkleBadgeBg,
                    contentColor = palette.periwinkleBadgeText
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("empty_generate_samples_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.AddCircleOutline,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Load Sample Videos", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
