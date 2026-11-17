package com.istiak.auraplayer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.istiak.auraplayer.ui.components.FrostedGlassSurface
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
    seenBaselineEpochSec: Long
): Boolean = when (filter) {
    LibraryQuickFilter.ALL -> true
    LibraryQuickFilter.UNWATCHED -> video.isConsideredNew(watchedVideoIds, seenBaselineEpochSec)
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
        blurRadius = if (isTopHeader) 10.dp else 10.dp,
        featherBottomEdge = isTopHeader,
        glassProgressOverride = 1f,
        modifier = modifier
    ) {}
}

/**
 * Sharp Foreground Content for the Top Header (Title / Back Arrow, Vault / Search / Sort / Settings
 * icons, expandable Breadcrumb Location Bar when inside a subfolder, and collapsible Search Input).
 *
 * When `isDrilledDown == true` (inside a subfolder), low-frequency actions (`Sort`, `Settings`,
 * and `Private Vault`) move into the top-right `⋮` overflow menu so the folder title (`Modifier.weight(1f)`)
 * gets maximum horizontal width without premature truncation.
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
    modifier: Modifier = Modifier
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val topSpecularRim = if (isDark) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.85f)
    var showOverflowMenu by remember { mutableStateOf(false) }
    var isPathExpanded by remember(currentPath) { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(showSearchBar) {
        if (showSearchBar) {
            delay(60L)
            runCatching { searchFocusRequester.requestFocus() }
        }
    }

    val breadcrumbs = remember(currentPath) {
        if (currentPath.isNotEmpty()) DirectoryTreeBuilder.getBreadcrumbs(currentPath) else emptyList()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(bottom = 8.dp)
            .testTag("liquid_glass_top_header")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(start = if (isDrilledDown) 4.dp else 16.dp, end = 4.dp)
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
                            .size(40.dp)
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
                    fontSize = if (isDrilledDown) 19.sp else 23.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.brandTitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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

            // Right-Side Icon Cluster:
            // At root: Private Vault, Search, Sort, Settings, More (with tight 2.dp spacing)
            // Inside a subfolder (isDrilledDown): Search + More (⋮) so long folder names have maximum room
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (!isDrilledDown) {
                    IconButton(
                        onClick = onOpenVault,
                        modifier = Modifier
                            .size(40.dp)
                            .padding(horizontal = 2.dp)
                            .testTag("top_vault_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FolderSpecial,
                            contentDescription = "Private Vault",
                            tint = palette.brandTitleColor.copy(alpha = 0.85f),
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onToggleSearch,
                    modifier = Modifier
                        .size(40.dp)
                        .padding(horizontal = 2.dp)
                        .testTag("top_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = palette.brandTitleColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (!isDrilledDown) {
                    IconButton(
                        onClick = onOpenSortDialog,
                        modifier = Modifier
                            .size(40.dp)
                            .padding(horizontal = 2.dp)
                            .testTag("top_sort_options_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SortByAlpha,
                            contentDescription = "Sort & View Options",
                            tint = palette.brandTitleColor,
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(40.dp)
                            .padding(horizontal = 2.dp)
                            .testTag("top_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings",
                            tint = palette.brandTitleColor,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }

                Box {
                    IconButton(
                        onClick = { showOverflowMenu = true },
                        modifier = Modifier
                            .size(40.dp)
                            .padding(horizontal = 2.dp)
                            .testTag("top_overflow_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "More options",
                            tint = palette.brandTitleColor,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false },
                        containerColor = palette.sheetBackground,
                        modifier = Modifier.testTag("top_overflow_dropdown")
                    ) {
                        if (isDrilledDown) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Sort & View Options",
                                        color = palette.primaryText,
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.SortByAlpha,
                                        contentDescription = null,
                                        tint = palette.brandTitleColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    onOpenSortDialog()
                                },
                                modifier = Modifier.testTag("menu_sort_options")
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Private Vault",
                                        color = palette.primaryText,
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.FolderSpecial,
                                        contentDescription = null,
                                        tint = palette.brandTitleColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    onOpenVault()
                                },
                                modifier = Modifier.testTag("menu_private_vault")
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Settings",
                                        color = palette.primaryText,
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Settings,
                                        contentDescription = null,
                                        tint = palette.brandTitleColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    onOpenSettings()
                                },
                                modifier = Modifier.testTag("menu_settings")
                            )
                        }
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

        // Expandable Breadcrumb Location Bar inside Top Header (when inside a subfolder)
        // Compact single-line with ellipsis by default; tap to smoothly expand into a multi-line Liquid Glass path card
        AnimatedVisibility(visible = currentPath.isNotEmpty() && breadcrumbs.isNotEmpty()) {
            val breadcrumbCardShape = RoundedCornerShape(if (isPathExpanded) 14.dp else 10.dp)
            val breadcrumbGlassBg = when {
                isPathExpanded && isDark -> Color(0xFF161F35).copy(alpha = 0.72f)
                isPathExpanded -> Color.White.copy(alpha = 0.76f)
                isDark -> Color(0xFF161F35).copy(alpha = 0.42f)
                else -> Color.White.copy(alpha = 0.52f)
            }
            val breadcrumbBorderBrush = Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = if (isDark) 0.30f else 0.90f),
                    Color(0xFF93C5FD).copy(alpha = if (isDark) 0.22f else 0.45f),
                    Color.White.copy(alpha = if (isDark) 0.12f else 0.55f)
                )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 6.dp)
                    .clip(breadcrumbCardShape)
                    .background(breadcrumbGlassBg)
                    .border(1.dp, breadcrumbBorderBrush, breadcrumbCardShape)
                    .clickable { isPathExpanded = !isPathExpanded }
                    .animateContentSize(animationSpec = tween(durationMillis = 220))
                    .padding(
                        horizontal = 10.dp,
                        vertical = if (isPathExpanded) 8.dp else 4.dp
                    )
                    .testTag("top_header_breadcrumb_bar")
            ) {
                if (!isPathExpanded) {
                    // Compact Single-Line Ellipsis View: keeps parent context + current folder cleanly on-screen
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val visibleAncestors = if (breadcrumbs.size > 3) {
                            listOf(breadcrumbs.first(), "…" to "") + breadcrumbs.takeLast(2).dropLast(1)
                        } else {
                            breadcrumbs.dropLast(1)
                        }

                        visibleAncestors.forEachIndexed { idx, (label, path) ->
                            if (idx > 0) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = palette.secondaryText.copy(alpha = 0.72f),
                                    modifier = Modifier
                                        .padding(horizontal = 1.dp)
                                        .size(15.dp)
                                )
                            }
                            val originalIndex = breadcrumbs.indexOfFirst { it.second == path && it.first == label }
                            Box(
                                modifier = Modifier
                                    .widthIn(max = 96.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .then(
                                        if (path.isNotEmpty() || label == "Videos") {
                                            Modifier.clickable { onNavigateToPath(path) }
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
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
                                tint = palette.secondaryText.copy(alpha = 0.72f),
                                modifier = Modifier
                                    .padding(horizontal = 1.dp)
                                    .size(15.dp)
                            )
                        }

                        val (lastLabel, _) = breadcrumbs.last()
                        val lastIndex = breadcrumbs.lastIndex
                        Box(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .clip(RoundedCornerShape(7.dp))
                                .background(MxBlueAccent.copy(alpha = if (isDark) 0.22f else 0.12f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                .testTag("breadcrumb_segment_$lastIndex"),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = lastLabel,
                                color = if (isDark) palette.brandTitleColor else MxBlueAccent,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Expand full folder path",
                            tint = palette.secondaryText.copy(alpha = 0.80f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    // Expanded Multi-Line Liquid Glass Path Card: shows complete untruncated folder hierarchy
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("top_header_breadcrumb_expanded")
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                breadcrumbs.forEachIndexed { index, (label, path) ->
                                    val isLast = index == breadcrumbs.lastIndex
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (index > 0) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                contentDescription = null,
                                                tint = palette.secondaryText.copy(alpha = 0.75f),
                                                modifier = Modifier
                                                    .padding(horizontal = 2.dp)
                                                    .size(15.dp)
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isLast) MxBlueAccent.copy(alpha = if (isDark) 0.24f else 0.13f)
                                                    else Color.Transparent
                                                )
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
                                                color = if (isLast) {
                                                    if (isDark) palette.brandTitleColor else MxBlueAccent
                                                } else {
                                                    palette.secondaryText
                                                },
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
                                tint = palette.secondaryText.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(16.dp)
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
                        val chipBg = when {
                            selected && isDark -> Color(0xFF283554).copy(alpha = 0.90f)
                            selected -> MxBlueAccent.copy(alpha = 0.16f)
                            isDark -> Color.White.copy(alpha = 0.08f)
                            else -> Color.White.copy(alpha = 0.62f)
                        }
                        val chipBorder = when {
                            selected && isDark -> Color(0xFF60A5FA).copy(alpha = 0.72f)
                            selected -> MxBlueAccent.copy(alpha = 0.55f)
                            isDark -> Color.White.copy(alpha = 0.14f)
                            else -> Color.White.copy(alpha = 0.85f)
                        }
                        val chipText = when {
                            selected && isDark -> Color(0xFF93C5FD)
                            selected -> MxBlueAccent
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
 * Feathered Liquid Glass Top Header bar wrapping [LiquidGlassHeaderContent] inside
 * [ScrollAwareFixedHeader] (or [FrostedGlassSurface] with `featherBottomEdge = true` when
 * no scroll layout is bound).
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
    layoutState: ScrollAwareGlassLayoutState? = null,
    modifier: Modifier = Modifier
) {
    if (layoutState != null) {
        ScrollAwareFixedHeader(
            layoutState = layoutState,
            modifier = modifier
        ) {
            LiquidGlassHeaderContent(
                title = title,
                isDrilledDown = isDrilledDown,
                currentPath = currentPath,
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
                onSelectQuickFilter = onSelectQuickFilter
            )
        }
    } else {
        FrostedGlassSurface(
            shape = RectangleShape,
            blurRadius = 10.dp,
            border = null,
            featherBottomEdge = true,
            glassProgressOverride = 0f,
            modifier = modifier.fillMaxWidth()
        ) {
            LiquidGlassHeaderContent(
                title = title,
                isDrilledDown = isDrilledDown,
                currentPath = currentPath,
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
                onSelectQuickFilter = onSelectQuickFilter
            )
        }
    }
}

/**
 * Elevated Liquid Crystal Floating Bottom Navigation Capsule (`Home` | `All Videos`) + inline
 * Reversed Aura-Blue Quick-Play Button (`56.dp`):
 * - Positioned higher off the bottom edge (`Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 18.dp)`).
 * - Uses low blur (`9.dp`) and high-translucency crystal glass tint (`Color.White.copy(alpha = 0.28f)` in Light Mode,
 *   `Color(0xFF141C2E).copy(alpha = 0.34f)` in Dark Mode) with a `1.2.dp` multi-stop specular crystal rim so
 *   background video cards and colors refract vividly through the capsule.
 * - Inline quick-play button uses a vibrant Aura Blue gradient (`#2563EB` -> `#1D4ED8`) with a `1.dp`
 *   luminous top-edge highlight and frosted translucent white (`Color.White.copy(alpha = 0.92f)`) play icon.
 */
@Composable
fun LiquidGlassBottomNavPill(
    selectedTab: MainNavTab,
    onSelectTab: (MainNavTab) -> Unit,
    showResumeButton: Boolean = false,
    onResumeLastPlayed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark

    val capsuleShape = RoundedCornerShape(999.dp)
    val liquidCrystalBorder = Brush.linearGradient(
        0.0f to Color.White.copy(alpha = 0.90f),
        0.50f to Color(0xFF93C5FD).copy(alpha = 0.35f),
        1.0f to Color.White.copy(alpha = 0.45f)
    )
    val glassBaseBg = if (isDark) {
        Color(0xFF141C2E).copy(alpha = 0.34f)
    } else {
        Color.White.copy(alpha = 0.28f)
    }
    val specularSheenBrush = if (isDark) {
        Brush.verticalGradient(
            0.0f to Color.White.copy(alpha = 0.22f),
            0.38f to Color.White.copy(alpha = 0.04f),
            1.0f to Color(0xFF60A5FA).copy(alpha = 0.12f)
        )
    } else {
        Brush.verticalGradient(
            0.0f to Color.White.copy(alpha = 0.46f),
            0.38f to Color.White.copy(alpha = 0.06f),
            1.0f to Color(0xFF93C5FD).copy(alpha = 0.14f)
        )
    }
    val activePillBg = Color(0xFF2563EB).copy(alpha = if (isDark) 0.24f else 0.14f)
    val activePillBorder = Brush.linearGradient(
        0.0f to Color.White.copy(alpha = if (isDark) 0.55f else 0.85f),
        0.55f to Color(0xFF93C5FD).copy(alpha = 0.45f),
        1.0f to Color(0xFF2563EB).copy(alpha = 0.28f)
    )
    val activeTint = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FrostedGlassSurface(
            shape = capsuleShape,
            blurRadius = 9.dp,
            border = BorderStroke(1.2.dp, liquidCrystalBorder),
            glassProgressOverride = 1f,
            baseBackgroundColor = glassBaseBg,
            specularOverlayBrush = specularSheenBrush,
            modifier = Modifier
                .height(56.dp)
                .crystalOuterShadow(
                    shape = capsuleShape,
                    isDark = isDark,
                    elevation = 16.dp
                )
                .testTag("floating_bottom_nav_pill")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
            ) {
                MainNavTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .widthIn(min = 108.dp)
                            .clip(capsuleShape)
                            .background(if (isSelected) activePillBg else Color.Transparent)
                            .then(
                                if (isSelected) {
                                    Modifier.border(1.dp, activePillBorder, capsuleShape)
                                } else {
                                    Modifier
                                }
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onSelectTab(tab) }
                            .padding(horizontal = 16.dp)
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
                                tint = if (isSelected) activeTint else palette.primaryText.copy(alpha = 0.78f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = tab.title,
                                color = if (isSelected) activeTint else palette.primaryText.copy(alpha = 0.82f),
                                fontSize = 13.5.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                maxLines = 1
                            )
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

            val auraBlueGradient = remember {
                Brush.linearGradient(
                    colors = listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
                )
            }
            val topEdgeHighlightBorder = remember {
                Brush.verticalGradient(
                    0.0f to Color.White.copy(alpha = 0.40f),
                    0.50f to Color(0xFF93C5FD).copy(alpha = 0.24f),
                    1.0f to Color.White.copy(alpha = 0.14f)
                )
            }
            val topInnerGloss = remember {
                Brush.verticalGradient(
                    0.0f to Color.White.copy(alpha = 0.24f),
                    0.45f to Color.Transparent
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

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = pressScale
                        scaleY = pressScale
                    }
                    .size(56.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = CircleShape,
                        ambientColor = Color(0xFF2563EB).copy(alpha = if (isDark) 0.55f else 0.36f),
                        spotColor = Color(0xFF1D4ED8).copy(alpha = if (isDark) 0.65f else 0.44f)
                    )
                    .clip(CircleShape)
                    .background(auraBlueGradient)
                    .background(topInnerGloss)
                    .border(1.dp, topEdgeHighlightBorder, CircleShape)
                    .clickable(
                        interactionSource = playInteractionSource,
                        indication = null,
                        role = Role.Button,
                        onClick = onResumeLastPlayed
                    )
                    .testTag("resume_play_fab")
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(frostedIconHalo)
                )
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Resume Last Played Video",
                    tint = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
