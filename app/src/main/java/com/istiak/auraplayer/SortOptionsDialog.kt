package com.istiak.auraplayer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.istiak.auraplayer.ui.theme.LocalExtendedPalette
import com.istiak.auraplayer.ui.theme.MxBlueAccent
import kotlinx.coroutines.delay

/**
 * MX Player-Style Centered Sorting & View Options Dialog (`SortOptionsDialog.kt`):
 * - Elevated white dialog card (`20.dp` rounded corners)
 * - Top Split Row: `View Mode` (All folders | Files | Folders) on Left | `Layout` (List | Grid) on Right
 * - `Sort` Section: 2x5 icon+label grid + full-width `↑ A to Z` | `↓ Z to A` direction toggle
 * - Collapsible `Fields` Section: 3-column checkbox grid (15 items)
 * - Collapsible `Advanced` Section: 3 toggle switches
 * - Footer Buttons: Right-aligned `Cancel` and bold `#2563EB` `Done` text buttons
 */
@Composable
fun SortOptionsDialog(
    settings: AppSettingsState,
    onApplyOptions: (
        sortField: SortField,
        sortAscending: Boolean,
        viewMode: LibraryViewMode,
        layoutMode: LibraryLayoutMode,
        enabledFields: Set<DisplayField>,
        displayLengthOverThumbnail: Boolean,
        showHiddenFolders: Boolean,
        recognizeNoMedia: Boolean
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val dialogSurfaceColor = if (isDark) palette.sheetBackground else Color.White
    val dividerColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFE5E7EB)
    val mutedColor = if (isDark) palette.secondaryText else Color(0xFF5F6368)

    var draftViewMode by remember(settings.viewMode) { mutableStateOf(settings.viewMode) }
    var draftLayoutMode by remember(settings.layoutMode) { mutableStateOf(settings.layoutMode) }
    var draftSortField by remember(settings.sortField) { mutableStateOf(settings.sortField) }
    var draftSortAscending by remember(settings.sortAscending) { mutableStateOf(settings.sortAscending) }
    var draftEnabledFields by remember(settings.enabledFields) { mutableStateOf(settings.enabledFields) }
    var draftDisplayLengthOverThumb by remember(settings.displayLengthOverThumbnail) {
        mutableStateOf(settings.displayLengthOverThumbnail)
    }
    var draftShowHidden by remember(settings.showHiddenFolders) { mutableStateOf(settings.showHiddenFolders) }
    var draftRecognizeNoMedia by remember(settings.recognizeNoMedia) { mutableStateOf(settings.recognizeNoMedia) }

    var fieldsExpanded by remember { mutableStateOf(true) }
    var isAdvancedExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    LaunchedEffect(isAdvancedExpanded) {
        if (isAdvancedExpanded) {
            withFrameNanos { }
            scrollState.animateScrollTo(scrollState.maxValue)
            delay(200L)
            withFrameNanos { }
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = dialogSurfaceColor,
            contentColor = palette.primaryText,
            tonalElevation = 6.dp,
            shadowElevation = 18.dp,
            modifier = Modifier
                .fillMaxWidth(0.93f)
                .widthIn(max = 430.dp)
                .heightIn(max = 680.dp)
                .testTag("sort_options_dialog")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Scrollable Body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 18.dp, vertical = 18.dp)
                ) {
                    // 1. TOP SPLIT ROW: View Mode (Left) | Layout (Right)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1.48f)) {
                            Text(
                                text = "View Mode",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = mutedColor,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                CompactModeIconOption(
                                    label = "All folders",
                                    icon = Icons.Filled.FolderOpen,
                                    selected = draftViewMode == LibraryViewMode.FOLDER,
                                    mutedColor = mutedColor,
                                    onClick = { draftViewMode = LibraryViewMode.FOLDER },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("view_mode_folder")
                                )
                                CompactModeIconOption(
                                    label = "Files",
                                    icon = Icons.AutoMirrored.Filled.InsertDriveFile,
                                    selected = draftViewMode == LibraryViewMode.LIBRARY,
                                    mutedColor = mutedColor,
                                    onClick = { draftViewMode = LibraryViewMode.LIBRARY },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("view_mode_library")
                                )
                                CompactModeIconOption(
                                    label = "Folders",
                                    icon = Icons.Filled.AccountTree,
                                    selected = draftViewMode == LibraryViewMode.TREE,
                                    mutedColor = mutedColor,
                                    onClick = { draftViewMode = LibraryViewMode.TREE },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("view_mode_tree")
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(dividerColor)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Layout",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = mutedColor,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                CompactModeIconOption(
                                    label = "List",
                                    icon = Icons.AutoMirrored.Filled.ViewList,
                                    selected = draftLayoutMode == LibraryLayoutMode.LIST,
                                    mutedColor = mutedColor,
                                    onClick = { draftLayoutMode = LibraryLayoutMode.LIST },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("layout_mode_list")
                                )
                                CompactModeIconOption(
                                    label = "Grid",
                                    icon = Icons.Filled.GridView,
                                    selected = draftLayoutMode == LibraryLayoutMode.GRID,
                                    mutedColor = mutedColor,
                                    onClick = { draftLayoutMode = LibraryLayoutMode.GRID },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("layout_mode_grid")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = dividerColor)
                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. SORT SECTION: 2x5 Icon Grid + Direction Pill
                    Text(
                        text = "Sort",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.primaryText
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val sortRow1 = listOf(
                        Triple(SortField.TITLE, "Title", Icons.Filled.Title),
                        Triple(SortField.DATE, "Date", Icons.Filled.CalendarToday),
                        Triple(SortField.PLAYED_TIME, "Played time", Icons.Filled.History),
                        Triple(SortField.STATUS, "Status", Icons.Filled.CheckCircleOutline),
                        Triple(SortField.LENGTH, "Length", Icons.Filled.Timer)
                    )
                    val sortRow2 = listOf(
                        Triple(SortField.SIZE, "Size", Icons.Filled.SdStorage),
                        Triple(SortField.RESOLUTION, "Resolution", Icons.Filled.AspectRatio),
                        Triple(SortField.PATH, "Path", Icons.Filled.Route),
                        Triple(SortField.FRAME_RATE, "Frame rate", Icons.Filled.Speed),
                        Triple(SortField.TYPE, "Type", Icons.Filled.Category)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        sortRow1.forEach { (field, label, icon) ->
                            CompactModeIconOption(
                                label = label,
                                icon = icon,
                                selected = draftSortField == field,
                                mutedColor = mutedColor,
                                onClick = { draftSortField = field },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sort_card_${field.name.lowercase()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        sortRow2.forEach { (field, label, icon) ->
                            CompactModeIconOption(
                                label = label,
                                icon = icon,
                                selected = draftSortField == field,
                                mutedColor = mutedColor,
                                onClick = { draftSortField = field },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sort_card_${field.name.lowercase()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Full-Width Split Direction Toggle: ↑ A to Z | ↓ Z to A
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DirectionToggleButton(
                            text = "A to Z",
                            icon = Icons.Filled.ArrowUpward,
                            selected = draftSortAscending,
                            dividerColor = dividerColor,
                            mutedColor = mutedColor,
                            onClick = { draftSortAscending = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sort_order_asc")
                        )
                        DirectionToggleButton(
                            text = "Z to A",
                            icon = Icons.Filled.ArrowDownward,
                            selected = !draftSortAscending,
                            dividerColor = dividerColor,
                            mutedColor = mutedColor,
                            onClick = { draftSortAscending = false },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sort_order_desc")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = dividerColor)

                    // 3. COLLAPSIBLE FIELDS SECTION (3-Column Checkbox Grid)
                    CollapsibleSectionHeader(
                        title = "Fields",
                        expanded = fieldsExpanded,
                        onToggle = { fieldsExpanded = !fieldsExpanded },
                        modifier = Modifier.testTag("fields_section_header")
                    )

                    AnimatedVisibility(visible = fieldsExpanded) {
                        val orderedFields = listOf(
                            DisplayField.THUMBNAILS to "Thumbnail",
                            DisplayField.DURATION to "Length",
                            DisplayField.EXTENSION to "File extension",
                            DisplayField.PLAYED_TIME to "Played time",
                            DisplayField.RESOLUTION to "Resolution",
                            DisplayField.FRAMERATE to "Frame rate",
                            DisplayField.PATH to "Path",
                            DisplayField.SIZE to "Size",
                            DisplayField.DATE to "Date",
                            DisplayField.TOTAL_MEDIA to "Total Media",
                            DisplayField.FOLDER_SIZE to "Folder Size",
                            DisplayField.TOTAL_DURATION to "Total Duration",
                            DisplayField.CODEC_SUPPORT to "Codec support",
                            DisplayField.SUBTITLE to "Subtitle",
                            DisplayField.PROGRESS_BAR to "Progress Bar"
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            orderedFields.chunked(3).forEach { rowFields ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    rowFields.forEach { (field, label) ->
                                        val isChecked = field in draftEnabledFields
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    draftEnabledFields = if (isChecked) {
                                                        draftEnabledFields - field
                                                    } else {
                                                        draftEnabledFields + field
                                                    }
                                                }
                                                .padding(vertical = 3.dp)
                                                .testTag("field_checkbox_${field.name.lowercase()}")
                                        ) {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = { checked ->
                                                    draftEnabledFields = if (checked) {
                                                        draftEnabledFields + field
                                                    } else {
                                                        draftEnabledFields - field
                                                    }
                                                },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = MxBlueAccent,
                                                    uncheckedColor = mutedColor,
                                                    checkmarkColor = Color.White
                                                ),
                                                modifier = Modifier.size(30.dp)
                                            )
                                            Text(
                                                text = label,
                                                fontSize = 11.sp,
                                                color = palette.primaryText,
                                                maxLines = 2,
                                                lineHeight = 13.sp,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(start = 2.dp, end = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = dividerColor)

                    // 4. COLLAPSIBLE ADVANCED SECTION (Toggle Switches)
                    CollapsibleSectionHeader(
                        title = "Advanced",
                        expanded = isAdvancedExpanded,
                        onToggle = { isAdvancedExpanded = !isAdvancedExpanded },
                        modifier = Modifier.testTag("advanced_section_header")
                    )

                    AnimatedVisibility(visible = isAdvancedExpanded) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp)
                        ) {
                            AdvancedDialogSwitchRow(
                                label = "Display length over thumbnail",
                                checked = draftDisplayLengthOverThumb,
                                onCheckedChange = { draftDisplayLengthOverThumb = it },
                                testTag = "adv_switch_length_over_thumb"
                            )
                            AdvancedDialogSwitchRow(
                                label = "Show hidden files and folders",
                                checked = draftShowHidden,
                                onCheckedChange = { draftShowHidden = it },
                                testTag = "adv_switch_hidden_files"
                            )
                            AdvancedDialogSwitchRow(
                                label = "Recognize .nomedia",
                                checked = draftRecognizeNoMedia,
                                onCheckedChange = { draftRecognizeNoMedia = it },
                                testTag = "adv_switch_recognize_nomedia"
                            )
                        }
                    }
                }

                HorizontalDivider(color = dividerColor)

                // 5. FOOTER BUTTONS: Right-Aligned Cancel & Bold #2563EB Done
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("sort_dialog_cancel_button")
                    ) {
                        Text(
                            text = "Cancel",
                            color = mutedColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    TextButton(
                        onClick = {
                            onApplyOptions(
                                draftSortField,
                                draftSortAscending,
                                draftViewMode,
                                draftLayoutMode,
                                draftEnabledFields,
                                draftDisplayLengthOverThumb,
                                draftShowHidden,
                                draftRecognizeNoMedia
                            )
                            onDismiss()
                        },
                        modifier = Modifier.testTag("sort_dialog_done_button")
                    ) {
                        Text(
                            text = "Done",
                            color = MxBlueAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactModeIconOption(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    mutedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTint = MxBlueAccent
    val bg = if (selected) MxBlueAccent.copy(alpha = 0.08f) else Color.Transparent
    val tint = if (selected) activeTint else mutedColor

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 3.dp, vertical = 7.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(21.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = tint,
            fontSize = 10.5.sp,
            lineHeight = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DirectionToggleButton(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    dividerColor: Color,
    mutedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) MxBlueAccent else dividerColor
    val contentColor = if (selected) MxBlueAccent else mutedColor
    val bgColor = if (selected) MxBlueAccent.copy(alpha = 0.08f) else Color.Transparent

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = contentColor,
            fontSize = 12.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun CollapsibleSectionHeader(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalExtendedPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 11.dp)
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.primaryText
        )
        Icon(
            imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = if (expanded) "Collapse $title" else "Expand $title",
            tint = palette.secondaryText,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun AdvancedDialogSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    val palette = LocalExtendedPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 38.dp, max = 42.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp)
            .testTag(testTag)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 14.sp,
            color = palette.primaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        )
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MxBlueAccent
                ),
                modifier = Modifier.scale(0.82f)
            )
        }
    }
}
