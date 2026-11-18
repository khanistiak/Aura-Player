package com.istiak.auraplayer

import android.os.Environment
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.FileCopy
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.istiak.auraplayer.ui.theme.AppThemePalette
import com.istiak.auraplayer.ui.theme.DangerRed
import com.istiak.auraplayer.ui.theme.LocalExtendedPalette
import java.io.File
import java.util.Locale

private data class FolderBreadcrumbSegment(
    val label: String,
    val relativePath: String
)

@Composable
private fun auraPickerSurfaceColor(): Color {
    val palette = LocalExtendedPalette.current
    return if (palette.themePalette == AppThemePalette.AURORA) {
        if (palette.isDark) Color(0xFF111827) else Color(0xFFF8FAFF)
    } else {
        MaterialTheme.colorScheme.surface
    }
}

/**
 * Redesigned Move / Copy Destination Picker (`24.dp` corners) matching Aura Player's Aura Blue & Material 3 design.
 */
@Composable
fun SelectFolderDialog(
    actionLabel: String, // "Move" or "Copy"
    allDirectories: List<String>,
    sourceRelativePaths: Set<String>,
    onCreateFolder: (String) -> Unit,
    onSelectFolder: (String) -> Unit,
    onDismiss: () -> Unit,
    itemCount: Int = 1
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = auraPickerSurfaceColor()
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    val isMoveMode = actionLabel.equals("Move", ignoreCase = true)

    var currentPath by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    val breadcrumbListState = rememberLazyListState()

    val cleanSourcePaths = remember(sourceRelativePaths) {
        sourceRelativePaths.map { it.trim('/') }.toSet()
    }
    val isSameFolder = currentPath.isNotEmpty() &&
        cleanSourcePaths.size == 1 &&
        cleanSourcePaths.first() == currentPath

    val breadcrumbs = remember(currentPath) {
        buildList {
            add(FolderBreadcrumbSegment(label = "Internal Storage", relativePath = ""))
            val segments = currentPath.split('/').filter { it.isNotBlank() }
            val pathAcc = StringBuilder()
            for (seg in segments) {
                if (pathAcc.isNotEmpty()) pathAcc.append('/')
                pathAcc.append(seg)
                add(FolderBreadcrumbSegment(label = seg, relativePath = pathAcc.toString()))
            }
        }
    }

    LaunchedEffect(breadcrumbs.size) {
        if (breadcrumbs.isNotEmpty()) {
            breadcrumbListState.animateScrollToItem(breadcrumbs.lastIndex)
        }
    }

    val childDirectories = remember(allDirectories, currentPath, searchQuery) {
        val prefix = if (currentPath.isEmpty()) "" else "$currentPath/"
        val children = allDirectories
            .mapNotNull { path ->
                val clean = path.trim('/')
                if (currentPath.isEmpty()) {
                    clean.substringBefore('/').takeIf { it.isNotEmpty() }
                } else if (clean.startsWith(prefix)) {
                    val remainder = clean.removePrefix(prefix)
                    remainder.substringBefore('/').takeIf { it.isNotEmpty() }
                } else {
                    null
                }
            }
            .distinct()
            .sortedBy { it.lowercase(Locale.US) }

        if (searchQuery.isBlank()) {
            children
        } else {
            val q = searchQuery.trim().lowercase(Locale.US)
            children.filter { it.lowercase(Locale.US).contains(q) }
        }
    }

    val safeItemCount = itemCount.coerceAtLeast(1)
    val itemCountLabel = if (safeItemCount == 1) "1 item" else "$safeItemCount items"
    val headerTitle = "$actionLabel $itemCountLabel to..."

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = surfaceColor,
            border = BorderStroke(1.dp, borderColor),
            tonalElevation = 6.dp,
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .widthIn(max = 460.dp)
                .testTag("select_folder_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                // Mode Header: Aura Blue badge + explicit "Move/Copy X item(s) to..." title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(primaryColor.copy(alpha = 0.12f))
                    ) {
                        Icon(
                            imageVector = if (isMoveMode) {
                                Icons.AutoMirrored.Rounded.DriveFileMove
                            } else {
                                Icons.Rounded.FileCopy
                            },
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = headerTitle,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (currentPath.isEmpty()) {
                                "Choose a destination folder"
                            } else {
                                "/storage/emulated/0/$currentPath"
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive Breadcrumb Path Chip Bar (Internal Storage > DCIM > ...)
                LazyRow(
                    state = breadcrumbListState,
                    verticalAlignment = Alignment.CenterVertically,
                    contentPadding = PaddingValues(vertical = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("folder_picker_breadcrumbs")
                ) {
                    itemsIndexed(breadcrumbs) { index, segment ->
                        val isLast = index == breadcrumbs.lastIndex
                        if (index > 0) {
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                modifier = Modifier
                                    .padding(horizontal = 2.dp)
                                    .size(16.dp)
                            )
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .heightIn(min = 32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isLast) primaryColor.copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                )
                                .clickable {
                                    currentPath = segment.relativePath
                                    searchQuery = ""
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("folder_breadcrumb_${segment.label}")
                        ) {
                            Text(
                                text = segment.label,
                                fontSize = 12.5.sp,
                                fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isLast) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }

                if (isSameFolder) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Cannot select the same folder",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = DangerRed,
                        modifier = Modifier.testTag("same_folder_warning")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Compact Search Bar + Aura Blue Tinted Home & New Folder Icon Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            .padding(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            contentAlignment = Alignment.CenterStart,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search folders",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp
                                ),
                                cursorBrush = SolidColor(primaryColor),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("folder_picker_search_input")
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(primaryColor.copy(alpha = 0.12f))
                            .clickable {
                                currentPath = ""
                                searchQuery = ""
                            }
                            .testTag("folder_picker_home_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Home,
                            contentDescription = "Internal Storage Root",
                            tint = primaryColor,
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(primaryColor.copy(alpha = 0.12f))
                            .clickable { showCreateFolderDialog = true }
                            .testTag("folder_picker_new_folder_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CreateNewFolder,
                            contentDescription = "Create New Folder",
                            tint = primaryColor,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Parent step-up row when inside a subdirectory
                if (currentPath.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                currentPath = DirectoryTreeBuilder.getParentPath(currentPath)
                                searchQuery = ""
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("folder_picker_parent_row")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(primaryColor.copy(alpha = 0.10f))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Parent folder",
                                tint = primaryColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "..",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = primaryColor
                        )
                    }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // Scrollable Subdirectory List
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(min = 100.dp, max = 240.dp)
                        .testTag("folder_picker_list")
                ) {
                    if (childDirectories.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (searchQuery.isNotBlank()) "No matching folders"
                                    else "No subfolders here",
                                    fontSize = 13.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(childDirectories, key = { it }) { folderName ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        currentPath = if (currentPath.isEmpty()) {
                                            folderName
                                        } else {
                                            "$currentPath/$folderName"
                                        }
                                        searchQuery = ""
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .testTag("folder_picker_item_$folderName")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(primaryColor.copy(alpha = 0.12f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Folder,
                                        contentDescription = null,
                                        tint = primaryColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = folderName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action Bar: Cancel & Move Here / Copy Here
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("folder_picker_cancel_button")
                    ) {
                        Text(
                            text = "Cancel",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSelectFolder(currentPath) },
                        enabled = currentPath.isNotEmpty() && !isSameFolder,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .height(42.dp)
                            .testTag("folder_picker_confirm_button")
                    ) {
                        Text(
                            text = "$actionLabel Here",
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                    }
                }
            }
        }
    }

    if (showCreateFolderDialog) {
        CreateSubfolderDialog(
            parentPath = currentPath,
            onDismiss = { showCreateFolderDialog = false },
            onCreate = { folderName ->
                val cleanName = folderName.trim().replace("/", "_")
                if (cleanName.isNotEmpty()) {
                    val newRelPath = if (currentPath.isEmpty()) {
                        cleanName
                    } else {
                        "$currentPath/$cleanName"
                    }
                    runCatching {
                        val targetDir = File(Environment.getExternalStorageDirectory(), newRelPath)
                        if (!targetDir.exists()) targetDir.mkdirs()
                    }
                    onCreateFolder(newRelPath)
                    currentPath = newRelPath
                }
                showCreateFolderDialog = false
            }
        )
    }
}

@Composable
private fun CreateSubfolderDialog(
    parentPath: String,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = auraPickerSurfaceColor()
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    var folderName by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val invalidChars = remember { setOf('/', '\\', ':', '*', '?', '"', '<', '>', '|') }
    val trimmed = folderName.trim()
    val hasInvalid = folderName.any { it in invalidChars }
    val canCreate = trimmed.isNotEmpty() && !hasInvalid

    Dialog(onDismissRequest = onDismiss) {
        LaunchedEffect(Unit) {
            runCatching { focusRequester.requestFocus() }
        }
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = surfaceColor,
            border = BorderStroke(1.dp, borderColor),
            tonalElevation = 6.dp,
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("create_folder_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(primaryColor.copy(alpha = 0.12f))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CreateNewFolder,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "New folder",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (parentPath.isEmpty()) {
                                "Inside Internal Storage"
                            } else {
                                "Inside $parentPath"
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    singleLine = true,
                    isError = hasInvalid,
                    label = { Text("Folder name") },
                    trailingIcon = {
                        if (folderName.isNotEmpty()) {
                            IconButton(onClick = { folderName = "" }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    supportingText = if (hasInvalid) {
                        {
                            Text(
                                text = "Cannot contain / \\ : * ? \" < > |",
                                fontSize = 12.sp,
                                color = DangerRed
                            )
                        }
                    } else null,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedLabelColor = primaryColor,
                        cursorColor = primaryColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .testTag("create_folder_name_input")
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Cancel",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { if (canCreate) onCreate(trimmed) },
                        enabled = canCreate,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .height(42.dp)
                            .testTag("create_folder_confirm_button")
                    ) {
                        Text(
                            text = "Create",
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
