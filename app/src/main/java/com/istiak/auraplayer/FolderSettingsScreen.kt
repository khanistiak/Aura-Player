package com.istiak.auraplayer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.istiak.auraplayer.ui.components.ScrollAwareFixedHeader
import com.istiak.auraplayer.ui.components.recordFrostedBackdrop
import com.istiak.auraplayer.ui.components.rememberScrollAwareGlassLayout
import com.istiak.auraplayer.ui.theme.LocalExtendedPalette

private val FsCobaltActive = Color(0xFF2563EB)
private val FsActiveHighlightLight = Color(0xFFE3E6FA)
private val FsIconBgLight = Color(0xFFEEF2FF)
private val FsSlateHeaderLight = Color(0xFF434A6E)
private val FsDividerLight = Color(0xFFE8ECF8)

/**
 * Folders & Media Library Settings Sub-Screen (`FolderSettingsScreen.kt`):
 * - Frozen `ScrollAwareFixedHeader`: `← Folders`
 * - Section `| Media Library`: Hidden folder scan + Library/Tree/Folders pills, Marker files &
 *   Dot folders + interactive marker chips, Filter short videos, Filter small files.
 * - Section `| Storage & Exclusions`: Blacklist & Whitelist folder pickers.
 * - Section `| Library Maintenance`: Force Rescan Library & Clear Metadata Cache.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FolderSettingsScreen(
    settings: AppSettingsState,
    availableFolders: List<String>,
    onUpdateShowHiddenFolders: (Boolean) -> Unit,
    onUpdateHiddenScanTargets: (library: Boolean, tree: Boolean, folders: Boolean) -> Unit,
    onUpdateRecognizeNoMedia: (Boolean) -> Unit,
    onUpdateScanDotFolders: (Boolean) -> Unit,
    onUpdateMarkerFiles: (Set<String>) -> Unit,
    onUpdateMinVideoDurationSec: (Int) -> Unit,
    onUpdateMinFileSizeMb: (Int) -> Unit,
    onUpdateBlacklistedFolders: (Set<String>) -> Unit,
    onUpdateWhitelistedFolders: (Set<String>, Boolean) -> Unit,
    onForceRescanLibrary: () -> Unit,
    onClearMetadataCache: () -> Unit,
    onMarkAllAsSeen: () -> Unit = {},
    onBack: () -> Unit,
    onShowCapsuleToast: (String) -> Unit
) {
    BackHandler { onBack() }

    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark

    var showAddMarkerDialog by remember { mutableStateOf(false) }
    var showDurationPicker by remember { mutableStateOf(false) }
    var showSizePicker by remember { mutableStateOf(false) }
    var showBlacklistPicker by remember { mutableStateOf(false) }
    var showWhitelistPicker by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val navBottomDp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val scrollLayout = rememberScrollAwareGlassLayout(
        listState = listState,
        defaultHeaderHeight = 64.dp,
        extraTopGap = 4.dp,
        bottomPadding = navBottomDp + 24.dp,
        horizontalPadding = 12.dp
    )

    val shortVideoLabel = formatMinDurationLabel(settings.minVideoDurationSec)
    val smallFileLabel = formatMinSizeLabel(settings.minFileSizeMb)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("folder_settings_screen")
    ) {
        LazyColumn(
            state = listState,
            contentPadding = scrollLayout.contentPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .recordFrostedBackdrop(
                    backdropState = scrollLayout.backdropState,
                    scrollableState = listState,
                    contentLayerOverride = scrollLayout.contentGraphicsLayer
                )
        ) {
            // Section | Media Library
            item(key = "sec_media_library") {
                FsFrostedSection(title = "| Media Library") {
                    // Hidden folder scan + 3 selectable pill chips: Library, Tree, Folders
                    FsSwitchRow(
                        icon = Icons.Filled.Visibility,
                        title = "Hidden folder scan",
                        subtitle = "Include hidden directories across selected library views",
                        checked = settings.showHiddenFolders,
                        onCheckedChange = onUpdateShowHiddenFolders,
                        showDivider = false,
                        testTag = "fs_switch_hidden_folder_scan"
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 56.dp, end = 12.dp, bottom = 10.dp)
                    ) {
                        Text(
                            text = "Enable in",
                            color = palette.secondaryText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        FsSelectablePillChip(
                            label = "Library",
                            selected = settings.hiddenFolderScanLibrary,
                            onClick = {
                                onUpdateHiddenScanTargets(
                                    !settings.hiddenFolderScanLibrary,
                                    settings.hiddenFolderScanTree,
                                    settings.hiddenFolderScanFolders
                                )
                            },
                            testTag = "fs_chip_library"
                        )
                        FsSelectablePillChip(
                            label = "Tree",
                            selected = settings.hiddenFolderScanTree,
                            onClick = {
                                onUpdateHiddenScanTargets(
                                    settings.hiddenFolderScanLibrary,
                                    !settings.hiddenFolderScanTree,
                                    settings.hiddenFolderScanFolders
                                )
                            },
                            testTag = "fs_chip_tree"
                        )
                        FsSelectablePillChip(
                            label = "Folders",
                            selected = settings.hiddenFolderScanFolders,
                            onClick = {
                                onUpdateHiddenScanTargets(
                                    settings.hiddenFolderScanLibrary,
                                    settings.hiddenFolderScanTree,
                                    !settings.hiddenFolderScanFolders
                                )
                            },
                            testTag = "fs_chip_folders"
                        )
                    }

                    FsDivider()

                    // Marker files & Dot folders + interactive marker tag chips
                    FsSwitchRow(
                        icon = Icons.Filled.Description,
                        title = "Marker files",
                        subtitle = "Respect marker files such as .nomedia during scanning",
                        checked = settings.recognizeNoMedia,
                        onCheckedChange = onUpdateRecognizeNoMedia,
                        showDivider = false,
                        testTag = "fs_switch_marker_files"
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 56.dp, end = 12.dp, bottom = 10.dp)
                    ) {
                        settings.markerFiles.sorted().forEach { marker ->
                            val chipShape = RoundedCornerShape(999.dp)
                            val chipBg = if (isDark) Color(0xFF283454) else FsActiveHighlightLight
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(chipShape)
                                    .background(chipBg)
                                    .border(
                                        1.dp,
                                        FsCobaltActive.copy(alpha = if (isDark) 0.45f else 0.28f),
                                        chipShape
                                    )
                                    .clickable {
                                        onUpdateMarkerFiles(settings.markerFiles - marker)
                                    }
                                    .padding(start = 10.dp, top = 5.dp, end = 8.dp, bottom = 5.dp)
                                    .testTag("fs_marker_chip_$marker")
                            ) {
                                Text(
                                    text = marker,
                                    color = if (isDark) Color(0xFFDBEAFE) else FsCobaltActive,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Remove $marker",
                                    tint = if (isDark) Color(0xFF93C5FD) else FsCobaltActive,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }

                        // '+' Add custom marker chip button
                        val addShape = RoundedCornerShape(999.dp)
                        Box(
                            modifier = Modifier
                                .clip(addShape)
                                .background(if (isDark) Color.White.copy(alpha = 0.08f) else FsIconBgLight)
                                .border(
                                    1.dp,
                                    if (isDark) Color.White.copy(alpha = 0.2f) else Color(0xFFD6D9F8),
                                    addShape
                                )
                                .clickable { showAddMarkerDialog = true }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .testTag("fs_add_marker_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Add Marker File",
                                tint = if (isDark) Color(0xFF93C5FD) else FsCobaltActive,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    FsDivider()

                    FsSwitchRow(
                        icon = Icons.Filled.FolderSpecial,
                        title = "Dot folders",
                        subtitle = "Scan folders whose names start with a dot (.)",
                        checked = settings.scanDotFolders,
                        onCheckedChange = onUpdateScanDotFolders,
                        testTag = "fs_switch_dot_folders"
                    )

                    FsActionRow(
                        icon = Icons.Filled.FilterAlt,
                        title = "Filter short videos",
                        subtitle = "Ignore videos shorter than the selected duration during scanning · $shortVideoLabel",
                        onClick = { showDurationPicker = true },
                        testTag = "fs_filter_short_videos_row"
                    )

                    FsActionRow(
                        icon = Icons.Filled.Storage,
                        title = "Filter small files",
                        subtitle = "Ignore files smaller than the selected size during scanning · $smallFileLabel",
                        onClick = { showSizePicker = true },
                        showDivider = false,
                        testTag = "fs_filter_small_files_row"
                    )
                }
            }

            // Section | Storage & Exclusions
            item(key = "sec_storage_exclusions") {
                FsFrostedSection(title = "| Storage & Exclusions") {
                    val blacklistDesc = if (settings.blacklistedFolders.isEmpty()) {
                        "No blacklisted folders"
                    } else {
                        "${settings.blacklistedFolders.size} folder(s) excluded from library"
                    }
                    FsActionRow(
                        icon = Icons.Filled.FolderOff,
                        title = "Blacklist",
                        subtitle = blacklistDesc,
                        onClick = { showBlacklistPicker = true },
                        testTag = "fs_blacklist_row"
                    )

                    val whitelistDesc = when {
                        !settings.whitelistEnabled -> "Whitelist disabled"
                        settings.whitelistedFolders.isEmpty() -> "Whitelist enabled · No folders chosen"
                        else -> "${settings.whitelistedFolders.size} folder(s) allowed"
                    }
                    FsActionRow(
                        icon = Icons.Filled.Folder,
                        title = "Whitelist",
                        subtitle = whitelistDesc,
                        onClick = { showWhitelistPicker = true },
                        showDivider = false,
                        testTag = "fs_whitelist_row"
                    )
                }
            }

            // Section | Library Maintenance
            item(key = "sec_maintenance") {
                FsFrostedSection(title = "| Library Maintenance") {
                    FsActionRow(
                        icon = Icons.Filled.DoneAll,
                        title = "Mark all videos as seen",
                        subtitle = "Clear folder count badges and NEW tags until new videos are added",
                        onClick = { onMarkAllAsSeen() },
                        testTag = "fs_mark_all_seen_row"
                    )

                    FsActionRow(
                        icon = Icons.Filled.Refresh,
                        title = "Force Rescan Library",
                        subtitle = "Re-index internal storage and permitted media folders",
                        onClick = {
                            onForceRescanLibrary()
                            onShowCapsuleToast("MediaStore library rescanned")
                        },
                        testTag = "fs_force_rescan_row"
                    )

                    FsActionRow(
                        icon = Icons.Filled.Delete,
                        title = "Clear Metadata Cache",
                        subtitle = "Delete cached video metadata, dimensions and thumbnail cache",
                        onClick = {
                            onClearMetadataCache()
                            onShowCapsuleToast("Metadata & thumbnail cache cleared")
                        },
                        showDivider = false,
                        testTag = "fs_clear_cache_row"
                    )
                }
            }
        }

        ScrollAwareFixedHeader(
            layoutState = scrollLayout,
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("folder_settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = palette.brandTitleColor
                    )
                }
                Text(
                    text = "Folders & Library",
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.brandTitleColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showAddMarkerDialog) {
        AddMarkerFileDialog(
            onDismiss = { showAddMarkerDialog = false },
            onAddMarker = { marker ->
                val normalized = if (marker.startsWith(".")) marker else ".$marker"
                onUpdateMarkerFiles(settings.markerFiles + normalized)
                showAddMarkerDialog = false
            }
        )
    }

    if (showDurationPicker) {
        val options = listOf(0, 5, 15, 30, 60, 300)
        FsSingleChoiceModal(
            title = "Filter short videos",
            options = options.map { it to formatMinDurationLabel(it) },
            selectedValue = settings.minVideoDurationSec,
            onSelect = {
                onUpdateMinVideoDurationSec(it)
                showDurationPicker = false
            },
            onDismiss = { showDurationPicker = false }
        )
    }

    if (showSizePicker) {
        val options = listOf(0, 1, 5, 10, 50)
        FsSingleChoiceModal(
            title = "Filter small files",
            options = options.map { it to formatMinSizeLabel(it) },
            selectedValue = settings.minFileSizeMb,
            onSelect = {
                onUpdateMinFileSizeMb(it)
                showSizePicker = false
            },
            onDismiss = { showSizePicker = false }
        )
    }

    if (showBlacklistPicker) {
        FolderMultiExclusionDialog(
            title = "Blacklist Folders",
            subtitle = "Selected folders and their subfolders will be hidden from scanning.",
            folders = (availableFolders + settings.blacklistedFolders).distinct().sorted(),
            selectedFolders = settings.blacklistedFolders,
            onSave = { updated ->
                onUpdateBlacklistedFolders(updated)
                showBlacklistPicker = false
            },
            onDismiss = { showBlacklistPicker = false }
        )
    }

    if (showWhitelistPicker) {
        WhitelistPickerDialog(
            folders = (availableFolders + settings.whitelistedFolders).distinct().sorted(),
            enabled = settings.whitelistEnabled,
            selectedFolders = settings.whitelistedFolders,
            onSave = { enabled, updated ->
                onUpdateWhitelistedFolders(updated, enabled)
                showWhitelistPicker = false
            },
            onDismiss = { showWhitelistPicker = false }
        )
    }
}

private fun formatMinDurationLabel(seconds: Int): String = when (seconds) {
    0 -> "No limit"
    60 -> "1 min"
    300 -> "5 min"
    else -> "${seconds}s"
}

private fun formatMinSizeLabel(mb: Int): String = if (mb <= 0) "No limit" else "$mb MB"

@Composable
private fun FsSelectablePillChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val chipShape = RoundedCornerShape(999.dp)
    val bg = when {
        selected && isDark -> FsCobaltActive.copy(alpha = 0.30f)
        selected -> FsActiveHighlightLight
        isDark -> Color.White.copy(alpha = 0.08f)
        else -> Color(0xFFF1F4FC)
    }
    val border = if (selected) FsCobaltActive else if (isDark) Color.White.copy(alpha = 0.14f) else FsDividerLight
    val textColor = when {
        selected && isDark -> Color(0xFFDBEAFE)
        selected -> FsCobaltActive
        else -> palette.secondaryText
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(chipShape)
            .background(bg)
            .border(1.dp, border, chipShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .testTag(testTag)
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun AddMarkerFileDialog(
    onDismiss: () -> Unit,
    onAddMarker: (String) -> Unit
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    var markerName by remember { mutableStateOf("") }

    FsModalCard(title = "Add Marker File", onDismiss = onDismiss) {
        Text(
            text = "Enter a marker filename such as .ignore or .nomedia.",
            color = palette.secondaryText,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = markerName,
            onValueChange = { markerName = it.trim() },
            placeholder = { Text(".ignore") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FsCobaltActive,
                unfocusedBorderColor = if (isDark) Color.White.copy(alpha = 0.2f) else Color(0xFFD6D9F8),
                focusedTextColor = palette.primaryText,
                unfocusedTextColor = palette.primaryText
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = palette.secondaryText)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (markerName.isNotBlank()) onAddMarker(markerName)
                },
                enabled = markerName.isNotBlank(),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FsCobaltActive,
                    contentColor = Color.White
                )
            ) {
                Text("Add")
            }
        }
    }
}

@Composable
private fun <T> FsSingleChoiceModal(
    title: String,
    options: List<Pair<T, String>>,
    selectedValue: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark

    FsModalCard(title = title, onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { (value, label) ->
                val isSelected = value == selectedValue
                val rowShape = RoundedCornerShape(14.dp)
                val rowBg = when {
                    isSelected && isDark -> FsCobaltActive.copy(alpha = 0.24f)
                    isSelected -> FsActiveHighlightLight
                    isDark -> Color.White.copy(alpha = 0.05f)
                    else -> Color.White.copy(alpha = 0.72f)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(rowShape)
                        .background(rowBg)
                        .border(
                            1.dp,
                            if (isSelected) FsCobaltActive else Color.Transparent,
                            rowShape
                        )
                        .clickable { onSelect(value) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelect(value) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = FsCobaltActive,
                            unselectedColor = palette.secondaryText
                        )
                    )
                    Text(
                        text = label,
                        color = palette.primaryText,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FsSlateHeaderLight,
                    contentColor = Color.White
                )
            ) {
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun FolderMultiExclusionDialog(
    title: String,
    subtitle: String,
    folders: List<String>,
    selectedFolders: Set<String>,
    onSave: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    var currentSelection by remember { mutableStateOf(selectedFolders) }

    FsModalCard(title = title, onDismiss = onDismiss) {
        Text(text = subtitle, color = palette.secondaryText, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 260.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (folders.isEmpty()) {
                Text(
                    text = "No folders discovered yet.",
                    color = palette.secondaryText,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                folders.forEach { folder ->
                    val checked = folder in currentSelection
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (checked) {
                                    if (isDark) FsCobaltActive.copy(alpha = 0.22f) else FsActiveHighlightLight
                                } else {
                                    Color.Transparent
                                }
                            )
                            .clickable {
                                currentSelection = if (checked) currentSelection - folder else currentSelection + folder
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = {
                                currentSelection = if (checked) currentSelection - folder else currentSelection + folder
                            },
                            colors = CheckboxDefaults.colors(checkedColor = FsCobaltActive)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = folder,
                            color = palette.primaryText,
                            fontSize = 13.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onDismiss) { Text("Cancel", color = palette.secondaryText) }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { onSave(currentSelection) },
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FsCobaltActive, contentColor = Color.White)
            ) {
                Text("Save")
            }
        }
    }
}

@Composable
private fun WhitelistPickerDialog(
    folders: List<String>,
    enabled: Boolean,
    selectedFolders: Set<String>,
    onSave: (Boolean, Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    var isEnabled by remember { mutableStateOf(enabled) }
    var currentSelection by remember { mutableStateOf(selectedFolders) }

    FsModalCard(title = "Whitelist Folders", onDismiss = onDismiss) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isDark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.72f))
                .clickable { isEnabled = !isEnabled }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Enable Whitelist Filter",
                color = palette.primaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Switch(
                checked = isEnabled,
                onCheckedChange = { isEnabled = it },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = FsCobaltActive)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 220.dp)
                .verticalScroll(rememberScrollState())
        ) {
            folders.forEach { folder ->
                val checked = folder in currentSelection
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (checked) {
                                if (isDark) FsCobaltActive.copy(alpha = 0.22f) else FsActiveHighlightLight
                            } else {
                                Color.Transparent
                            }
                        )
                        .clickable {
                            currentSelection = if (checked) currentSelection - folder else currentSelection + folder
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = {
                            currentSelection = if (checked) currentSelection - folder else currentSelection + folder
                        },
                        colors = CheckboxDefaults.colors(checkedColor = FsCobaltActive)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = folder,
                        color = palette.primaryText,
                        fontSize = 13.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onDismiss) { Text("Cancel", color = palette.secondaryText) }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { onSave(isEnabled, currentSelection) },
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FsCobaltActive, contentColor = Color.White)
            ) {
                Text("Apply")
            }
        }
    }
}

@Composable
private fun FsModalCard(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val cardShape = RoundedCornerShape(24.dp)
    val bgColor = if (isDark) Color(0xFF141A2E) else Color(0xFFF6F7FF)
    val bannerBg = if (isDark) Color(0xFF212B4A) else FsActiveHighlightLight

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .shadow(20.dp, cardShape)
                .clip(cardShape)
                .background(bgColor)
                .border(
                    1.dp,
                    if (isDark) Color.White.copy(alpha = 0.2f) else Color.White,
                    cardShape
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bannerBg)
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Text(
                    text = title,
                    color = palette.primaryText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(
                modifier = Modifier.padding(18.dp),
                content = content
            )
        }
    }
}

@Composable
private fun FsFrostedSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val headerColor = if (isDark) Color(0xFFB7C5E8) else FsSlateHeaderLight
    val cardShape = RoundedCornerShape(16.dp)
    val cardBg = if (isDark) Color(0xFF161B2E).copy(alpha = 0.80f) else Color.White.copy(alpha = 0.84f)
    val borderBrush = if (isDark) {
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color(0xFF60A5FA).copy(alpha = 0.14f)))
    } else {
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.94f), Color(0xFFD6D9F8).copy(alpha = 0.72f)))
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = headerColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp,
            modifier = Modifier.padding(start = 6.dp, top = 4.dp, bottom = 6.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 4.dp,
                    shape = cardShape,
                    ambientColor = Color(0xFF434A6E).copy(alpha = if (isDark) 0.28f else 0.06f),
                    spotColor = Color(0xFF2563EB).copy(alpha = if (isDark) 0.28f else 0.08f)
                )
                .clip(cardShape)
                .background(cardBg)
                .border(1.dp, borderBrush, cardShape)
                .padding(vertical = 4.dp),
            content = content
        )
    }
}

@Composable
private fun FsDivider() {
    val isDark = LocalExtendedPalette.current.isDark
    HorizontalDivider(
        thickness = 1.dp,
        color = if (isDark) Color.White.copy(alpha = 0.08f) else FsDividerLight,
        modifier = Modifier.padding(horizontal = 12.dp)
    )
}

@Composable
private fun FsIconBadge(icon: ImageVector) {
    val isDark = LocalExtendedPalette.current.isDark
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(if (isDark) FsCobaltActive.copy(alpha = 0.18f) else FsIconBgLight),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDark) Color(0xFF60A5FA) else FsCobaltActive,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun FsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
    showDivider: Boolean = true
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag(testTag)
        ) {
            FsIconBadge(icon = icon)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = palette.primaryText,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = palette.secondaryText,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    modifier = Modifier.scale(0.86f),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = FsCobaltActive,
                        checkedBorderColor = Color.Transparent,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
                        uncheckedBorderColor = Color.Transparent
                    )
                )
            }
        }
        if (showDivider) {
            FsDivider()
        }
    }
}

@Composable
private fun FsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String,
    showDivider: Boolean = true
) {
    val palette = LocalExtendedPalette.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag(testTag)
        ) {
            FsIconBadge(icon = icon)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = palette.primaryText,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = palette.secondaryText,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = palette.secondaryText,
                modifier = Modifier.size(18.dp)
            )
        }
        if (showDivider) {
            FsDivider()
        }
    }
}
