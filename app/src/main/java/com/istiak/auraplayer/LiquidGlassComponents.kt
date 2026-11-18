package com.istiak.auraplayer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.istiak.auraplayer.ui.components.FrostedGlassSurface
import com.istiak.auraplayer.ui.components.LocalFrostedBackdropState
import com.istiak.auraplayer.ui.components.ScrollAwareFixedHeader
import com.istiak.auraplayer.ui.components.ScrollAwareGlassLayoutState
import com.istiak.auraplayer.ui.components.crystalOuterShadow
import com.istiak.auraplayer.ui.theme.LocalExtendedPalette
import com.istiak.auraplayer.ui.theme.MxBlueAccent
import kotlinx.coroutines.delay

enum class LibraryQuickFilter(val label: String) {
    ALL("All"),
    UNWATCHED("Unwatched"),
    HIGH_RES("4K / HD"),
    SUBTITLED("Subtitled"),
    LARGE("Large (>1 GB)")
}

private const val ONE_GIGABYTE_BYTES = 1_073_741_824L

fun matchesQuickFilter(
    video: VideoItem,
    filter: LibraryQuickFilter,
    watchedVideoIds: Set<Long>,
    seenBaselineEpochSec: Long,
    showNewLabel: Boolean = true,
    newLabelDaysThreshold: Int = 7
): Boolean = when (filter) {
    LibraryQuickFilter.ALL -> true
    LibraryQuickFilter.UNWATCHED -> video.isConsideredNew(
        watchedVideoIds = watchedVideoIds,
        seenBaselineEpochSec = seenBaselineEpochSec,
        showNewLabel = showNewLabel,
        newLabelDaysThreshold = newLabelDaysThreshold
    )
    LibraryQuickFilter.HIGH_RES -> {
        val (w, h) = video.effectiveVisualDimensions()
        maxOf(w, h) >= 1280 || minOf(w, h) >= 720 ||
            video.resolutionTag in setOf("4K", "1440P", "1080P", "720P")
    }
    LibraryQuickFilter.SUBTITLED -> video.hasSubtitles
    LibraryQuickFilter.LARGE -> video.sizeBytes >= ONE_GIGABYTE_BYTES
}

@Composable
fun LiquidGlassBackdrop(
    isDark: Boolean,
    isTopHeader: Boolean,
    modifier: Modifier = Modifier
) {
    FrostedGlassSurface(
        shape = RectangleShape,
        blurRadius = if (isTopHeader) 18.dp else 16.dp,
        featherBottomEdge = isTopHeader,
        glassProgressOverride = 1f,
        modifier = modifier
    ) {}
}

/**
 * Sharp Foreground Content for the Top Header (Title / Back Arrow, inline Vault / Search / Sort /
 * Settings / More icons, lightweight clickable Breadcrumb Row when inside a subfolder, and
 * collapsible Search Input).
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun LiquidGlassHeaderContent(
    title: String,
    isDrilledDown: Boolean,
    currentPath: String,
    showSearchBar: Boolean,
    searchQuery: String,
    onNavigateBack: () -> Unit,
    onNavigateToPath: (String) -> Unit,
    onOpenVault: () -> Unit,
    onToggleSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onOpenSortDialog: () -> Unit,
    onOpenSettings: () -> Unit,
    onMarkAllAsSeen: () -> Unit = {},
    selectedQuickFilter: LibraryQuickFilter = LibraryQuickFilter.ALL,
    onSelectQuickFilter: (LibraryQuickFilter) -> Unit = {},
    showFullNames: Boolean = false,
    modifier: Modifier = Modifier
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val topSpecularRim = if (isDark) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.85f)
    var showOverflowMenu by remember { mutableStateOf(false) }
    val cleanPath = currentPath.trim('/')
    var isPathExpanded by remember(cleanPath) { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(showSearchBar) {
        if (showSearchBar) {
            delay(60L)
            runCatching { searchFocusRequester.requestFocus() }
        }
    }

    val liveBreadcrumbs = remember(cleanPath) {
        if (cleanPath.isNotEmpty()) DirectoryTreeBuilder.getBreadcrumbs(cleanPath) else emptyList()
    }
    // Preserve last non-empty breadcrumb list across AnimatedVisibility exit transitions so
    // returning from a subfolder to Home never calls .first()/.last() on an empty list.
    var cachedBreadcrumbs by remember { mutableStateOf(liveBreadcrumbs) }
    if (liveBreadcrumbs.isNotEmpty()) {
        cachedBreadcrumbs = liveBreadcrumbs
    }
    val breadcrumbs = if (liveBreadcrumbs.isNotEmpty()) liveBreadcrumbs else cachedBreadcrumbs

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("liquid_glass_top_header")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 54.dp)
                .padding(start = if (isDrilledDown) 6.dp else 18.dp, end = 6.dp, top = 4.dp, bottom = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
            ) {
                if (isDrilledDown) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("directory_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = palette.brandTitleColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(2.dp))
                }

                Text(
                    text = title,
                    fontSize = if (isDrilledDown) 19.5.sp else 23.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.brandTitleColor,
                    maxLines = if (isDrilledDown && showFullNames) Int.MAX_VALUE else 1,
                    overflow = if (isDrilledDown && showFullNames) TextOverflow.Clip else TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (!isDrilledDown) {
                                Modifier.combinedClickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {},
                                    onLongClick = onOpenVault
                                )
                            } else {
                                Modifier
                            }
                        )
                )
            }

            // Clean inline action icons (Private Vault, Search, Sort AZ, Settings, More) with balanced spacing
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                IconButton(
                    onClick = onOpenVault,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("top_vault_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FolderSpecial,
                        contentDescription = "Private Vault",
                        tint = palette.brandTitleColor.copy(alpha = 0.88f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onToggleSearch,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("top_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = palette.brandTitleColor,
                        modifier = Modifier.size(21.dp)
                    )
                }

                IconButton(
                    onClick = onOpenSortDialog,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("top_sort_options_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.SortByAlpha,
                        contentDescription = "Sort & View Options",
                        tint = palette.brandTitleColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("top_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = palette.brandTitleColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box {
                    IconButton(
                        onClick = { showOverflowMenu = true },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("top_overflow_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "More options",
                            tint = palette.brandTitleColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false },
                        containerColor = palette.sheetBackground,
                        modifier = Modifier.testTag("top_overflow_dropdown")
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Mark all as seen",
                                    color = palette.primaryText,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.DoneAll,
                                    contentDescription = null,
                                    tint = palette.brandTitleColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                showOverflowMenu = false
                                onMarkAllAsSeen()
                            },
                            modifier = Modifier.testTag("menu_mark_all_as_seen")
                        )
                    }
                }
            }
        }

        // Lightweight Breadcrumb Row rendered inside the lower half of the same Liquid Glass header pill
        AnimatedVisibility(visible = cleanPath.isNotEmpty() && liveBreadcrumbs.isNotEmpty()) {
            if (breadcrumbs.isNotEmpty()) {
                val activeChipBg = MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.22f else 0.12f)
                val activeChipText = if (isDark) palette.brandTitleColor else MaterialTheme.colorScheme.primary

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isPathExpanded = !isPathExpanded }
                        .animateContentSize(animationSpec = tween(durationMillis = 200))
                        .padding(start = 18.dp, end = 16.dp, top = 0.dp, bottom = 10.dp)
                        .testTag("top_header_breadcrumb_bar")
                ) {
                    if (!isPathExpanded) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val visibleAncestors = if (breadcrumbs.size > 3) {
                                listOfNotNull(breadcrumbs.firstOrNull(), "…" to "") +
                                    breadcrumbs.takeLast(2).dropLast(1)
                            } else {
                                breadcrumbs.dropLast(1)
                            }

                            visibleAncestors.forEachIndexed { idx, (label, path) ->
                                if (idx > 0) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = palette.secondaryText.copy(alpha = 0.68f),
                                        modifier = Modifier
                                            .padding(horizontal = 2.dp)
                                            .size(15.dp)
                                    )
                                }
                                val originalIndex = breadcrumbs.indexOfFirst {
                                    it.second == path && it.first == label
                                }
                                Box(
                                    modifier = Modifier
                                        .widthIn(max = 112.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .then(
                                            if (path.isNotEmpty() || label == "Videos") {
                                                Modifier.clickable { onNavigateToPath(path) }
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                        .then(
                                            if (originalIndex >= 0) Modifier.testTag("breadcrumb_segment_$originalIndex")
                                            else Modifier
                                        ),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = label,
                                        color = palette.secondaryText,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (breadcrumbs.size > 1) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = palette.secondaryText.copy(alpha = 0.68f),
                                    modifier = Modifier
                                        .padding(horizontal = 2.dp)
                                        .size(15.dp)
                                )
                            }

                            val lastPair = breadcrumbs.lastOrNull()
                            if (lastPair != null) {
                                val lastIndex = breadcrumbs.lastIndex
                                Box(
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(activeChipBg)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                        .testTag("breadcrumb_segment_$lastIndex"),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = lastPair.first,
                                        color = activeChipText,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (breadcrumbs.size > 2) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowDown,
                                    contentDescription = "Expand full folder path",
                                    tint = palette.secondaryText.copy(alpha = 0.72f),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("top_header_breadcrumb_expanded")
                        ) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                breadcrumbs.forEachIndexed { index, (label, path) ->
                                    val isLast = index == breadcrumbs.lastIndex
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (index > 0) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                contentDescription = null,
                                                tint = palette.secondaryText.copy(alpha = 0.68f),
                                                modifier = Modifier
                                                    .padding(horizontal = 2.dp)
                                                    .size(15.dp)
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isLast) activeChipBg else Color.Transparent)
                                                .clickable {
                                                    if (isLast) {
                                                        isPathExpanded = false
                                                    } else {
                                                        onNavigateToPath(path)
                                                    }
                                                }
                                                .padding(horizontal = 7.dp, vertical = 3.dp)
                                                .testTag("breadcrumb_segment_$index")
                                        ) {
                                            Text(
                                                text = label,
                                                color = if (isLast) activeChipText else palette.secondaryText,
                                                fontSize = 12.5.sp,
                                                lineHeight = 16.sp,
                                                fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowUp,
                                contentDescription = "Collapse folder path",
                                tint = palette.secondaryText.copy(alpha = 0.80f),
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(15.dp)
                            )
                        }
                    }
                }
            }
        }

        // Collapsible Search Input & Quick-Filter Chips inside the Top Header
        AnimatedVisibility(visible = showSearchBar) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 6.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isDark) Color.White.copy(alpha = 0.10f)
                            else Color.White.copy(alpha = 0.62f)
                        )
                        .border(
                            1.dp,
                            Brush.verticalGradient(listOf(topSpecularRim, Color.White.copy(alpha = 0.20f))),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = palette.secondaryText,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search videos or folders…",
                                    color = palette.secondaryText,
                                    fontSize = 14.sp
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = palette.primaryText,
                                    fontSize = 14.sp
                                ),
                                cursorBrush = SolidColor(palette.brandTitleColor),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(searchFocusRequester)
                                    .testTag("search_videos_input")
                            )
                        }
                        IconButton(
                            onClick = {
                                if (searchQuery.isNotEmpty()) {
                                    onSearchQueryChange("")
                                } else {
                                    onToggleSearch()
                                }
                            },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("clear_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = if (searchQuery.isNotEmpty()) "Clear search" else "Close search",
                                tint = palette.secondaryText,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Horizontal Quick-Filter Chips
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, bottom = 4.dp)
                        .testTag("quick_filter_chips_row")
                ) {
                    LibraryQuickFilter.entries.forEach { filter ->
                        val selected = selectedQuickFilter == filter
                        val primaryAccent = MaterialTheme.colorScheme.primary
                        val chipBg = when {
                            selected && isDark -> primaryAccent.copy(alpha = 0.24f)
                            selected -> primaryAccent.copy(alpha = 0.16f)
                            isDark -> Color.White.copy(alpha = 0.08f)
                            else -> Color.White.copy(alpha = 0.62f)
                        }
                        val chipBorder = when {
                            selected -> primaryAccent.copy(alpha = 0.62f)
                            isDark -> Color.White.copy(alpha = 0.14f)
                            else -> Color.White.copy(alpha = 0.85f)
                        }
                        val chipText = when {
                            selected -> primaryAccent
                            else -> palette.secondaryText
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(chipBg)
                                .border(1.dp, chipBorder, RoundedCornerShape(999.dp))
                                .clickable { onSelectQuickFilter(filter) }
                                .padding(horizontal = 13.dp, vertical = 7.dp)
                                .testTag("quick_filter_chip_${filter.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = filter.label,
                                color = chipText,
                                fontSize = 12.5.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Scroll-Aware, Bottom-Feathered Liquid Crystal Top Header wrapping [LiquidGlassHeaderContent]:
 * - Anchors full-width at `Alignment.TopCenter` with `RoundedCornerShape(0.dp)`, `border = null`,
 *   `blurRadius = 10.dp`, `featherBottomEdge = true`, and `statusBarsPadding()`.
 * - At rest (`glassProgress == 0f`), renders 100% seamless and borderless over the ambient background.
 * - On scroll, ramps in `GraphicsLayer` backdrop blur (`BlurEffect` with `TileMode.Clamp`) and dissolves
 *   softly at its bottom edge via `CompositingStrategy.Offscreen` + `BlendMode.DstIn`.
 */
@Composable
fun LiquidGlassTopHeader(
    title: String,
    isDrilledDown: Boolean,
    currentPath: String,
    showSearchBar: Boolean,
    searchQuery: String,
    onNavigateBack: () -> Unit,
    onNavigateToPath: (String) -> Unit,
    onOpenVault: () -> Unit,
    onToggleSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onOpenSortDialog: () -> Unit,
    onOpenSettings: () -> Unit,
    onMarkAllAsSeen: () -> Unit = {},
    selectedQuickFilter: LibraryQuickFilter = LibraryQuickFilter.ALL,
    onSelectQuickFilter: (LibraryQuickFilter) -> Unit = {},
    showFullNames: Boolean = false,
    layoutState: ScrollAwareGlassLayoutState? = null,
    onHeaderHeightMeasured: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cleanPath = currentPath.trim('/')
    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { size ->
                if (size.height > 0) {
                    if (layoutState != null && layoutState.headerHeightPxState.intValue != size.height) {
                        layoutState.headerHeightPxState.intValue = size.height
                    }
                    onHeaderHeightMeasured?.invoke(size.height)
                }
            }
    ) {
        if (layoutState != null) {
            ScrollAwareFixedHeader(
                layoutState = layoutState,
                blurRadius = 28.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                LiquidGlassHeaderContent(
                    title = title,
                    isDrilledDown = isDrilledDown,
                    currentPath = cleanPath,
                    showSearchBar = showSearchBar,
                    searchQuery = searchQuery,
                    onNavigateBack = onNavigateBack,
                    onNavigateToPath = onNavigateToPath,
                    onOpenVault = onOpenVault,
                    onToggleSearch = onToggleSearch,
                    onSearchQueryChange = onSearchQueryChange,
                    onOpenSortDialog = onOpenSortDialog,
                    onOpenSettings = onOpenSettings,
                    onMarkAllAsSeen = onMarkAllAsSeen,
                    selectedQuickFilter = selectedQuickFilter,
                    onSelectQuickFilter = onSelectQuickFilter,
                    showFullNames = showFullNames
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                LiquidGlassHeaderContent(
                    title = title,
                    isDrilledDown = isDrilledDown,
                    currentPath = cleanPath,
                    showSearchBar = showSearchBar,
                    searchQuery = searchQuery,
                    onNavigateBack = onNavigateBack,
                    onNavigateToPath = onNavigateToPath,
                    onOpenVault = onOpenVault,
                    onToggleSearch = onToggleSearch,
                    onSearchQueryChange = onSearchQueryChange,
                    onOpenSortDialog = onOpenSortDialog,
                    onOpenSettings = onOpenSettings,
                    onMarkAllAsSeen = onMarkAllAsSeen,
                    selectedQuickFilter = selectedQuickFilter,
                    onSelectQuickFilter = onSelectQuickFilter,
                    showFullNames = showFullNames
                )
            }
        }
    }
}

/**
 * Elevated Liquid Crystal Floating Bottom Navigation Capsule (`Home` | `All Videos`) + inline
 * Circular Quick-Resume Play Button (`56.dp`):
 * - Driven by the exact same [FrostedGlassSurface] optical pipeline (`blurRadius = 28.dp`, `glassProgressOverride = 1f`)
 *   and [LocalLiquidGlassConfig] as the top header and live preview card.
 * - When `config.enabled == false`: renders a clean standard frosted Material capsule
 *   (`MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f)` with `14.dp` blur and hairline
 *   `outlineVariant.copy(alpha = 0.35f)` border) and a solid `MaterialTheme.colorScheme.primary` Play circle.
 */
@Composable
fun LiquidGlassBottomNavPill(
    selectedTab: MainNavTab,
    onSelectTab: (MainNavTab) -> Unit,
    showResumeButton: Boolean = false,
    onResumeLastPlayed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val glassConfig = LocalLiquidGlassConfig.current
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val primaryAccent = MaterialTheme.colorScheme.primary
    val frostNorm = glassConfig.frostPercent.coerceIn(0f, 1f)

    val capsuleShape = CircleShape
    val liquidCrystalBorder = if (isDark) {
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.28f),
                Color.White.copy(alpha = 0.18f),
                Color.White.copy(alpha = 0.10f)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.95f),
                Color(0xFF94A3B8).copy(alpha = 0.35f),
                primaryAccent.copy(alpha = 0.30f)
            )
        )
    }
    val pillOuterShadowColor = if (isDark) {
        Color.Black.copy(alpha = 0.45f)
    } else {
        Color(0x280F172A)
    }
    val activePillBg = MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.26f else 0.16f)
    val activePillBorder = MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
    val tabSlotWidth = 118.dp
    val selectedTabIndex = MainNavTab.entries.indexOf(selectedTab).coerceAtLeast(0)
    val indicatorOffsetX by animateDpAsState(
        targetValue = if (selectedTabIndex == 0) 0.dp else tabSlotWidth,
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = 420f
        ),
        label = "TabIndicatorSlide"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 26.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FrostedGlassSurface(
            shape = capsuleShape,
            blurRadius = 28.dp,
            featherBottomEdge = false,
            border = BorderStroke(1.25.dp, liquidCrystalBorder),
            glassProgressOverride = 1f,
            baseBackgroundColor = null,
            specularOverlayBrush = null,
            modifier = Modifier
                .height(56.dp)
                .crystalOuterShadow(
                    shape = CircleShape,
                    color = pillOuterShadowColor,
                    blurSpread = 14.dp,
                    offsetY = 4.dp
                )
                .testTag("floating_bottom_nav_pill")
        ) {
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
            ) {
                // Single shared active indicator capsule sliding smoothly behind the tab items
                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffsetX)
                        .width(tabSlotWidth)
                        .height(44.dp)
                        .clip(RoundedCornerShape(50))
                        .background(activePillBg)
                        .border(1.dp, activePillBorder, RoundedCornerShape(50))
                        .testTag("bottom_nav_sliding_indicator")
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    MainNavTab.entries.forEach { tab ->
                        val isSelected = selectedTab == tab
                        val animatedIconTint by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            animationSpec = spring(dampingRatio = 0.85f, stiffness = 450f),
                            label = "TabIconTint_${tab.name}"
                        )
                        val animatedTextColor by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            animationSpec = spring(dampingRatio = 0.85f, stiffness = 450f),
                            label = "TabTextColor_${tab.name}"
                        )
                        Box(
                            modifier = Modifier
                                .width(tabSlotWidth)
                                .height(44.dp)
                                .clip(RoundedCornerShape(50))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onSelectTab(tab) }
                                .padding(horizontal = 12.dp)
                                .testTag(tab.testTag),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title,
                                    tint = animatedIconTint,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(7.dp))
                                Text(
                                    text = tab.title,
                                    color = animatedTextColor,
                                    fontSize = 13.5.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showResumeButton) {
            val playInteractionSource = remember { MutableInteractionSource() }
            val isPressed by playInteractionSource.collectIsPressedAsState()
            val pressScale by animateFloatAsState(
                targetValue = if (isPressed) 0.92f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "liquid_play_press_scale"
            )

            val topEdgeHighlightBorder = remember(primaryAccent, isDark) {
                Brush.linearGradient(
                    0.0f to Color.White.copy(alpha = if (isDark) 0.56f else 0.72f),
                    0.50f to primaryAccent.copy(alpha = 0.45f),
                    1.0f to Color.White.copy(alpha = 0.24f)
                )
            }
            val frostedIconHalo = remember {
                Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.22f),
                        Color.Transparent
                    )
                )
            }
            val playFillColor = if (glassConfig.enabled) {
                primaryAccent.copy(alpha = 0.55f + 0.25f * frostNorm)
            } else {
                primaryAccent
            }

            FrostedGlassSurface(
                shape = CircleShape,
                blurRadius = 28.dp,
                featherBottomEdge = false,
                border = if (glassConfig.enabled) BorderStroke(1.25.dp, topEdgeHighlightBorder) else null,
                glassProgressOverride = 1f,
                baseBackgroundColor = playFillColor,
                specularOverlayBrush = null,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = pressScale
                        scaleY = pressScale
                    }
                    .size(56.dp)
                    .crystalOuterShadow(
                        shape = CircleShape,
                        color = primaryAccent.copy(alpha = if (isDark) 0.50f else 0.34f),
                        blurSpread = 14.dp,
                        offsetY = 4.dp
                    )
                    .clickable(
                        interactionSource = playInteractionSource,
                        indication = null,
                        role = Role.Button,
                        onClick = onResumeLastPlayed
                    )
                    .testTag("resume_play_fab")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.matchParentSize()
                ) {
                    if (glassConfig.enabled) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(frostedIconHalo)
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Resume Last Played Video",
                        tint = Color.White.copy(alpha = 0.95f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}
