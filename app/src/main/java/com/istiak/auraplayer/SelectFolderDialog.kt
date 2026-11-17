package com.istiak.auraplayer

import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.istiak.auraplayer.ui.theme.DangerRed
import com.istiak.auraplayer.ui.theme.LocalExtendedPalette
import java.io.File
import java.util.Locale

/**
 * Centered rounded dialog (`24.dp` corners) for picking a destination directory during Move / Copy.
 */
@Composable
fun SelectFolderDialog(
    actionLabel: String, // "Move" or "Copy"
    allDirectories: List<String>,
    sourceRelativePaths: Set<String>,
    onCreateFolder: (String) -> Unit,
    onSelectFolder: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalExtendedPalette.current
    var currentPath by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateFolderDialog by remember { mutableStateOf(false) }

    val cleanSourcePaths = remember(sourceRelativePaths) {
        sourceRelativePaths.map { it.trim('/') }.toSet()
    }
    val isSameFolder = currentPath.isNotEmpty() &&
        cleanSourcePaths.size == 1 &&
        cleanSourcePaths.first() == currentPath

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

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = palette.sheetBackground,
            tonalElevation = 6.dp,
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("select_folder_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                // Header Row: Title + Home & Create New Folder circular buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Select Folder",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryText
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (currentPath.isEmpty()) {
                                "/storage/emulated/0"
                            } else {
                                "/storage/emulated/0/$currentPath"
                            },
                            fontSize = 12.sp,
                            color = palette.secondaryText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(palette.neutralChipBg)
                                .clickable {
                                    currentPath = ""
                                    searchQuery = ""
                                }
                                .testTag("folder_picker_home_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Home,
                                contentDescription = "Storage Root",
                                tint = palette.brandTitleColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(palette.neutralChipBg)
                                .clickable { showCreateFolderDialog = true }
                                .testTag("folder_picker_new_folder_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CreateNewFolder,
                                contentDescription = "Create New Folder",
                                tint = palette.brandTitleColor,
                                modifier = Modifier.size(20.dp)
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

                Spacer(modifier = Modifier.height(12.dp))

                // Search folders bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.neutralChipBg)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = palette.secondaryText,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search folders",
                                fontSize = 14.sp,
                                color = palette.secondaryText
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = palette.primaryText,
                                fontSize = 14.sp
                            ),
                            cursorBrush = SolidColor(palette.brandTitleColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("folder_picker_search_input")
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Clear",
                            tint = palette.secondaryText,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { searchQuery = "" }
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
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                currentPath = DirectoryTreeBuilder.getParentPath(currentPath)
                                searchQuery = ""
                            }
                            .padding(horizontal = 10.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Parent folder",
                            tint = palette.brandTitleColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "..",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.brandTitleColor
                        )
                    }
                    HorizontalDivider(color = palette.neutralChipBg)
                }

                // Scrollable subdirectory list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 280.dp)
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
                                    fontSize = 13.sp,
                                    color = palette.secondaryText
                                )
                            }
                        }
                    } else {
                        items(childDirectories, key = { it }) { folderName ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        currentPath = if (currentPath.isEmpty()) {
                                            folderName
                                        } else {
                                            "$currentPath/$folderName"
                                        }
                                        searchQuery = ""
                                    }
                                    .padding(horizontal = 10.dp, vertical = 11.dp)
                                    .testTag("folder_picker_item_$folderName")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Folder,
                                    contentDescription = null,
                                    tint = palette.folderIconTint,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = folderName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = palette.primaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action footer: Cancel & Select pill button
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Cancel",
                            color = palette.secondaryText,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSelectFolder(currentPath) },
                        enabled = currentPath.isNotEmpty() && !isSameFolder,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.brandTitleColor,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .height(42.dp)
                            .testTag("folder_picker_confirm_button")
                    ) {
                        Text(
                            text = if (currentPath.isEmpty()) "Select" else "$actionLabel Here",
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
    val palette = LocalExtendedPalette.current
    var folderName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = palette.sheetBackground,
        title = {
            Text(
                text = "Create New Folder",
                fontWeight = FontWeight.Bold,
                color = palette.primaryText
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (parentPath.isEmpty()) {
                        "Inside /storage/emulated/0"
                    } else {
                        "Inside /storage/emulated/0/$parentPath"
                    },
                    fontSize = 12.sp,
                    color = palette.secondaryText
                )
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    singleLine = true,
                    label = { Text("Folder name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_folder_name_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(folderName) },
                enabled = folderName.trim().isNotEmpty(),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = palette.brandTitleColor),
                modifier = Modifier.testTag("create_folder_confirm_button")
            ) {
                Text("Create", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = palette.secondaryText)
            }
        }
    )
}
