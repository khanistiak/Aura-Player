package com.istiak.auraplayer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
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
        blurRadius = if (isTopHeader) 10.dp else 22.dp,
        featherBottomEdge = isTopHeader,
        glassProgressOverride = 1f,
        modifier = modifier
    ) {}
}

/**
 * Sharp Foreground Content for the Top Header (Title / Back Arrow, Vault / Search / Sort / Settings
 * icons, merged Breadcrumb Bar when inside a subfolder, and collapsible Search Input).
 * Designed to be wrapped inside `ScrollAwareFixedHeader(layoutState = scrollLayout) { ... }`.
 */
@OptIn(ExperimentalFoundationApi::class)
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
    val breadcrumbScrollState = rememberScrollState()
    LaunchedEffect(currentPath) {
        if (currentPath.isNotEmpty()) {
            breadcrumbScrollState.animateScrollTo(breadcrumbScrollState.maxValue)
        }
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
                .height(60.dp)
                .padding(start = if (isDrilledDown) 4.dp else 18.dp, end = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (isDrilledDown) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("directory_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = palette.brandTitleColor
                        )
                    }
                }

                Text(
                    text = title,
                    fontSize = if (isDrilledDown) 20.sp else 23.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.brandTitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = if (!isDrilledDown) {
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
            }

            // Right-Side Icon Cluster: Vault, Search, Sort/Filter, Settings
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onOpenVault,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("top_vault_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FolderSpecial,
                        contentDescription = "Private Vault",
                        tint = palette.brandTitleColor.copy(alpha = 0.85f),
                        modifier = Modifier.size(21.dp)
                    )
                }

                IconButton(
                    onClick = onToggleSearch,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("top_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = palette.brandTitleColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = onOpenSortDialog,
                    modifier = Modifier
                        .size(48.dp)
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
                        .size(48.dp)
                        .testTag("top_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = palette.brandTitleColor,
                        modifier = Modifier.size(21.dp)
                    )
                }

                Box {
                    IconButton(
                        onClick = { showOverflowMenu = true },
                        modifier = Modifier
                            .size(48.dp)
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

        // Merged Breadcrumb Sub-Row inside Top Header (when inside a subfolder)
        AnimatedVisibility(visible = currentPath.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(breadcrumbScrollState)
                    .padding(start = 14.dp, end = 14.dp, bottom = 6.dp)
                    .testTag("top_header_breadcrumb_bar")
            ) {
                breadcrumbs.forEachIndexed { index, (label, path) ->
                    val isLast = index == breadcrumbs.lastIndex
                    if (index > 0) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = palette.secondaryText.copy(alpha = 0.75f),
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .size(16.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isLast) MxBlueAccent.copy(alpha = if (isDark) 0.22f else 0.11f)
                                else Color.Transparent
                            )
                            .clickable { onNavigateToPath(path) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("breadcrumb_segment_$index"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isLast) {
                                if (isDark) palette.brandTitleColor else MxBlueAccent
                            } else {
                                palette.secondaryText
                            },
                            fontSize = 13.sp,
                            fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Medium,
                            maxLines = 1
                        )
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
 * Unified Floating Bottom Navigation Capsule (`Home` | `All Videos`) + inline matching
 * Liquid Crystal Play/Resume button powered by [FrostedGlassSurface] so both share the exact
 * same real-time `GraphicsLayer` backdrop refraction, frosted tint, and specular rim.
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
    val liquidCrystalBorder = if (isDark) {
        Brush.linearGradient(
            0.0f to Color.White.copy(alpha = 0.85f),
            0.45f to Color(0xFF60A5FA).copy(alpha = 0.34f),
            1.0f to Color.White.copy(alpha = 0.18f)
        )
    } else {
        Brush.linearGradient(
            0.0f to Color.White.copy(alpha = 0.85f),
            0.55f to Color.White.copy(alpha = 0.42f),
            1.0f to Color.White.copy(alpha = 0.18f)
        )
    }
    val glassBaseBg = if (isDark) {
        Color(0xFF1A2236).copy(alpha = 0.58f)
    } else {
        Color.White.copy(alpha = 0.55f)
    }
    val specularSheenBrush = if (isDark) {
        Brush.verticalGradient(
            0.0f to Color.White.copy(alpha = 0.24f),
            0.48f to Color.White.copy(alpha = 0.06f),
            1.0f to Color(0xFF60A5FA).copy(alpha = 0.16f)
        )
    } else {
        Brush.verticalGradient(
            0.0f to Color.White.copy(alpha = 0.62f),
            0.48f to Color.White.copy(alpha = 0.14f),
            1.0f to Color.White.copy(alpha = 0.32f)
        )
    }
    val activePillBg = if (isDark) Color(0xFF283554).copy(alpha = 0.78f) else MxBlueAccent.copy(alpha = 0.14f)
    val activeTint = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FrostedGlassSurface(
            shape = capsuleShape,
            blurRadius = 28.dp,
            border = BorderStroke(1.dp, liquidCrystalBorder),
            glassProgressOverride = 1f,
            baseBackgroundColor = glassBaseBg,
            specularOverlayBrush = specularSheenBrush,
            modifier = Modifier
                .height(56.dp)
                .crystalOuterShadow(
                    shape = capsuleShape,
                    isDark = isDark,
                    elevation = 18.dp
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
                                tint = if (isSelected) activeTint else palette.secondaryText,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = tab.title,
                                color = if (isSelected) activeTint else palette.secondaryText,
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

            FrostedGlassSurface(
                shape = CircleShape,
                blurRadius = 28.dp,
                border = BorderStroke(1.dp, liquidCrystalBorder),
                glassProgressOverride = 1f,
                baseBackgroundColor = glassBaseBg,
                specularOverlayBrush = specularSheenBrush,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = pressScale
                        scaleY = pressScale
                    }
                    .size(56.dp)
                    .crystalOuterShadow(
                        shape = CircleShape,
                        isDark = isDark,
                        elevation = 18.dp
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
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Resume Last Played Video",
                        tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}
