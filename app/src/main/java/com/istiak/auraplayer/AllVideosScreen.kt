package com.istiak.auraplayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import com.istiak.auraplayer.ui.components.ScrollAwareGlassLayoutState
import com.istiak.auraplayer.ui.components.recordFrostedBackdrop
import com.istiak.auraplayer.ui.theme.LocalExtendedPalette

/**
 * High-FPS All Videos Library View (`AllVideosScreen.kt`):
 * - Uses stable `key = { it.id }` and `contentType = { "video_item" }` on every `LazyColumn` and
 *   `LazyVerticalGrid` item for zero-jank 60/120fps scrolling.
 * - Remembers per-row click/long-press/action-sheet callbacks so scrolling rows skip unnecessary
 *   recompositions.
 * - Records the live backdrop via `recordFrostedBackdrop` without per-frame state mutations.
 */
@Composable
fun AllVideosScreen(
    videos: List<VideoItem>,
    isFilterActive: Boolean,
    isGridLayout: Boolean,
    listState: LazyListState,
    gridState: LazyGridState,
    scrollLayout: ScrollAwareGlassLayoutState,
    listContentPadding: PaddingValues,
    settings: AppSettingsState,
    imageLoader: ImageLoader,
    playbackProgress: PlaybackProgressSnapshot,
    selectedIds: Set<Long>,
    isMultiSelectMode: Boolean,
    onToggleVideoSelection: (VideoItem) -> Unit,
    onPlayVideo: (VideoItem, List<VideoItem>) -> Unit,
    onOpenVideoMenu: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isGridLayout) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(minSize = 165.dp),
            contentPadding = listContentPadding,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = modifier
                .fillMaxSize()
                .recordFrostedBackdrop(
                    backdropState = scrollLayout.backdropState,
                    scrollableState = gridState,
                    contentLayerOverride = scrollLayout.contentGraphicsLayer
                )
                .testTag("videos_grid")
        ) {
            if (videos.isEmpty() && isFilterActive) {
                item(
                    span = { GridItemSpan(maxLineSpan) },
                    key = "empty_filter_library_grid",
                    contentType = "empty_notice"
                ) {
                    AllVideosFilteredEmptyNotice()
                }
            }
            items(
                items = videos,
                key = { it.id },
                contentType = { "video_item" }
            ) { video ->
                val isSelected = video.id in selectedIds
                val onPlayCallback = remember(video, videos, isMultiSelectMode, onToggleVideoSelection, onPlayVideo) {
                    {
                        if (isMultiSelectMode) onToggleVideoSelection(video)
                        else onPlayVideo(video, videos)
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
            modifier = modifier
                .fillMaxSize()
                .recordFrostedBackdrop(
                    scrollLayout.backdropState,
                    listState,
                    scrollLayout.contentGraphicsLayer
                )
                .testTag("videos_list")
        ) {
            if (videos.isEmpty() && isFilterActive) {
                item(
                    key = "empty_filter_library_list",
                    contentType = "empty_notice"
                ) {
                    AllVideosFilteredEmptyNotice()
                }
            }
            items(
                items = videos,
                key = { it.id },
                contentType = { "video_item" }
            ) { video ->
                val isSelected = video.id in selectedIds
                val onPlayCallback = remember(video, videos, isMultiSelectMode, onToggleVideoSelection, onPlayVideo) {
                    {
                        if (isMultiSelectMode) onToggleVideoSelection(video)
                        else onPlayVideo(video, videos)
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

@Composable
internal fun AllVideosFilteredEmptyNotice() {
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
