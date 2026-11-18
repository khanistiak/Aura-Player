package com.istiak.auraplayer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.istiak.auraplayer.ui.components.FrostedGlassSurface
import com.istiak.auraplayer.ui.components.crystalOuterShadow
import com.istiak.auraplayer.ui.theme.AppThemePalette
import com.istiak.auraplayer.ui.theme.DangerRed
import com.istiak.auraplayer.ui.theme.LocalExtendedPalette
import com.istiak.auraplayer.ui.theme.MxBlueAccent
import java.util.Locale

/**
 * Top Contextual Action Header that replaces `LiquidGlassTopHeader` when `selectedIds.isNotEmpty()`.
 * - Left: Close (`✕`) button + dropdown counter pill (`1 / 5 Selected ▾`) with
 *   quick options (`Select All`, `Invert Selection`, `Deselect All`).
 * - Right: Play Selected (`▶`), Media Info (`ⓘ`, enabled when 1 selected), Share (`Share`).
 */
@Composable
fun ContextualSelectionTopBar(
    selectedCount: Int,
    totalCount: Int,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onPlaySelected: () -> Unit,
    onShowMediaInfo: () -> Unit,
    onShareSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val specularEdge = if (isDark) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.65f)
    var showSelectionMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("contextual_selection_top_bar")
    ) {
        LiquidGlassBackdrop(
            isDark = isDark,
            isTopHeader = true,
            modifier = Modifier.matchParentSize()
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp)
                    .padding(horizontal = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onClearSelection,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("selection_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Exit selection mode",
                            tint = palette.brandTitleColor
                        )
                    }

                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(palette.periwinkleBadgeBg)
                                .clickable { showSelectionMenu = true }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                                .testTag("selection_counter_dropdown")
                        ) {
                            Text(
                                text = "$selectedCount / ${totalCount.coerceAtLeast(selectedCount)} Selected",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.periwinkleBadgeText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = "Selection options",
                                tint = palette.periwinkleBadgeText,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showSelectionMenu,
                            onDismissRequest = { showSelectionMenu = false },
                            containerColor = palette.sheetBackground
                        ) {
                            DropdownMenuItem(
                                text = { Text("Select All", color = palette.primaryText) },
                                onClick = {
                                    showSelectionMenu = false
                                    onSelectAll()
                                },
                                modifier = Modifier.testTag("selection_menu_select_all")
                            )
                            DropdownMenuItem(
                                text = { Text("Invert Selection", color = palette.primaryText) },
                                onClick = {
                                    showSelectionMenu = false
                                    onInvertSelection()
                                },
                                modifier = Modifier.testTag("selection_menu_invert")
                            )
                            DropdownMenuItem(
                                text = { Text("Deselect All", color = palette.primaryText) },
                                onClick = {
                                    showSelectionMenu = false
                                    onClearSelection()
                                },
                                modifier = Modifier.testTag("selection_menu_deselect_all")
                            )
                        }
                    }
                }

                // Right Quick Actions: Play Selected, Media Info (1 selected), Share
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPlaySelected,
                        enabled = selectedCount > 0,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("selection_play_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play Selected",
                            tint = palette.brandTitleColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    val infoEnabled = selectedCount == 1
                    IconButton(
                        onClick = onShowMediaInfo,
                        enabled = infoEnabled,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("selection_media_info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = "Media Info",
                            tint = if (infoEnabled) palette.brandTitleColor
                            else palette.secondaryText.copy(alpha = 0.35f),
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    IconButton(
                        onClick = onShareSelected,
                        enabled = selectedCount > 0,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("selection_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share Selected",
                            tint = palette.brandTitleColor,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(specularEdge)
            )
        }
    }
}

/**
 * Floating Bottom Action Dock (replaces normal bottom navigation pill during Multi-Select Mode).
 * Rounded `#ECEBF2` pill container with 6 icon buttons (excluding Compress) + Move to Private Vault:
 * Move, Copy, Rename, Add to Playlist, Move to Private Vault, Delete.
 */
@Composable
fun FloatingSelectionActionDock(
    selectedCount: Int,
    onMoveClick: () -> Unit,
    onCopyClick: () -> Unit,
    onRenameClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onMoveToVaultClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val dockShape = RoundedCornerShape(24.dp)
    val dockFill = if (isDark) palette.bottomNavCapsuleBg else Color(0xFFECEBF2)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.85f)
    val canRename = selectedCount == 1

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = dockShape,
            color = dockFill,
            shadowElevation = 14.dp,
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier
                .shadow(
                    elevation = 14.dp,
                    shape = dockShape,
                    ambientColor = Color.Black.copy(alpha = 0.16f),
                    spotColor = Color.Black.copy(alpha = 0.18f)
                )
                .testTag("floating_selection_action_dock")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                DockActionButton(
                    icon = Icons.AutoMirrored.Filled.DriveFileMove,
                    contentDescription = "Move",
                    tint = palette.brandTitleColor,
                    enabled = selectedCount > 0,
                    onClick = onMoveClick,
                    testTag = "dock_move_button"
                )
                DockActionButton(
                    icon = Icons.Filled.ContentCopy,
                    contentDescription = "Copy",
                    tint = palette.brandTitleColor,
                    enabled = selectedCount > 0,
                    onClick = onCopyClick,
                    testTag = "dock_copy_button"
                )
                DockActionButton(
                    icon = Icons.Filled.Edit,
                    contentDescription = "Rename",
                    tint = if (canRename) palette.brandTitleColor else palette.secondaryText.copy(alpha = 0.35f),
                    enabled = canRename,
                    onClick = onRenameClick,
                    testTag = "dock_rename_button"
                )
                DockActionButton(
                    icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                    contentDescription = "Add to Playlist",
                    tint = palette.brandTitleColor,
                    enabled = selectedCount > 0,
                    onClick = onAddToPlaylistClick,
                    testTag = "dock_playlist_button"
                )
                DockActionButton(
                    icon = Icons.Filled.Lock,
                    contentDescription = "Move to Private Vault",
                    tint = MxBlueAccent,
                    enabled = selectedCount > 0,
                    onClick = onMoveToVaultClick,
                    testTag = "dock_vault_button"
                )
                DockActionButton(
                    icon = Icons.Filled.DeleteOutline,
                    contentDescription = "Delete",
                    tint = DangerRed,
                    enabled = selectedCount > 0,
                    onClick = onDeleteClick,
                    testTag = "dock_delete_button"
                )
            }
        }
    }
}

@Composable
private fun DockActionButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(48.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun auraFileDialogSurfaceColor(): Color {
    val palette = LocalExtendedPalette.current
    return if (palette.themePalette == AppThemePalette.AURORA) {
        if (palette.isDark) Color(0xFF111827) else Color(0xFFF8FAFF)
    } else {
        MaterialTheme.colorScheme.surface
    }
}

/**
 * Confirmation dialog shown before permanently deleting one or more videos from device storage.
 */
@Composable
fun DeleteConfirmationDialog(
    videos: List<VideoItem>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (videos.isEmpty()) return
    val count = videos.size
    val destructiveRed = Color(0xFFDC2626)
    val surfaceColor = auraFileDialogSurfaceColor()
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    val bodyText = if (count == 1) {
        val fileName = videos.first().displayName
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)) {
                append(fileName)
            }
            append(" will be permanently removed from your device.")
        }
    } else {
        buildAnnotatedString {
            append("$count selected videos will be permanently removed from your device.")
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = surfaceColor,
            border = BorderStroke(1.dp, borderColor),
            tonalElevation = 6.dp,
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("delete_confirmation_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 20.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(destructiveRed.copy(alpha = 0.12f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        tint = destructiveRed,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (count == 1) "Delete video?" else "Delete $count videos?",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = bodyText,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("delete_confirm_cancel_button")
                    ) {
                        Text(
                            text = "Cancel",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = destructiveRed,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .height(42.dp)
                            .testTag("delete_confirm_button")
                    ) {
                        Text(
                            text = "Delete",
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Confirmation dialog shown before encrypting/moving videos into the Private Vault (.private + .kv).
 */
@Composable
fun MoveToVaultConfirmationDialog(
    videos: List<VideoItem>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (videos.isEmpty()) return
    val count = videos.size
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = auraFileDialogSurfaceColor()
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    val singleTitle = videos.first().let { vid ->
        vid.title.ifBlank { vid.displayName }
    }
    val bodyMessage = if (count == 1) {
        "Hide \"$singleTitle\" in your PIN/biometric-protected Private Vault? It will be removed from public folders and gallery."
    } else {
        "Hide $count selected videos in your Private Vault? They will be removed from public folders and gallery."
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = surfaceColor,
            border = BorderStroke(1.dp, borderColor),
            tonalElevation = 6.dp,
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("move_to_vault_confirmation_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 20.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(primaryColor.copy(alpha = 0.12f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(25.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Move to Private Vault?",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = bodyMessage,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("vault_confirm_cancel_button")
                    ) {
                        Text(
                            text = "Cancel",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .height(42.dp)
                            .testTag("vault_confirm_button")
                    ) {
                        Text(
                            text = "Move to Vault",
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Redesigned Rename Video dialog (`24.dp` corners) with Aura Blue icon badge, extension pill badge,
 * auto-focused `OutlinedTextField` with cursor at end, clear `×` button, and inline invalid-character validation.
 */
@Composable
fun RenameFileDialog(
    video: VideoItem,
    onDismiss: () -> Unit,
    onConfirmRename: (String) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = auraFileDialogSurfaceColor()
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    val extension = remember(video.displayName, video.extensionLabel) {
        val rawExt = video.displayName.substringAfterLast('.', "").lowercase(Locale.US)
        rawExt.ifBlank { video.extensionLabel.lowercase(Locale.US) }
    }
    val initialName = remember(video.id, video.displayName) {
        video.displayName.substringBeforeLast('.')
    }
    var nameValue by remember(video.id, video.displayName) {
        mutableStateOf(
            TextFieldValue(
                text = initialName,
                selection = TextRange(initialName.length)
            )
        )
    }
    val focusRequester = remember { FocusRequester() }
    val invalidChars = remember { setOf('/', '\\', ':', '*', '?', '"', '<', '>', '|') }
    val trimmedName = nameValue.text.trim()
    val hasInvalidChars = nameValue.text.any { it in invalidChars }
    val isRenameEnabled = trimmedName.isNotEmpty() && !hasInvalidChars

    Dialog(onDismissRequest = onDismiss) {
        LaunchedEffect(video.id) {
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
                .testTag("rename_file_dialog")
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
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Rename video",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(primaryColor.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                    .testTag("rename_extension_badge")
                            ) {
                                Text(
                                    text = ".$extension",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = primaryColor
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Extension .$extension is preserved automatically",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = nameValue,
                    onValueChange = { nameValue = it },
                    singleLine = true,
                    isError = hasInvalidChars,
                    label = { Text("File name") },
                    trailingIcon = {
                        if (nameValue.text.isNotEmpty()) {
                            IconButton(
                                onClick = { nameValue = TextFieldValue("") },
                                modifier = Modifier.testTag("rename_clear_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear name",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    supportingText = if (hasInvalidChars) {
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
                        .testTag("rename_file_input")
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("rename_cancel_button")
                    ) {
                        Text(
                            text = "Cancel",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (isRenameEnabled) {
                                onConfirmRename(trimmedName)
                            }
                        },
                        enabled = isRenameEnabled,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .height(42.dp)
                            .testTag("rename_file_confirm_button")
                    ) {
                        Text(
                            text = "Rename",
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Solid opaque Material 3 Add to Playlist Dialog (`24.dp` corners):
 * - Header with Aura Blue `PlaylistAdd` badge, subtitle `"Adding X video(s)"`, and close button
 * - Single-plus `"Create New Playlist"` outlined action button
 * - Compact `"Search playlists"` bar with clear button
 * - `"Existing Playlists"` checkable list with theme-colored checkboxes & leading icon badges
 */
@Composable
fun AddToPlaylistSheet(
    videoCount: Int,
    playlists: List<PlaylistItem>,
    onCreatePlaylist: (String) -> Unit,
    onAddVideosToPlaylists: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = auraFileDialogSurfaceColor()
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    var searchQuery by remember { mutableStateOf("") }
    var selectedPlaylistIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val filteredPlaylists = remember(playlists, searchQuery) {
        if (searchQuery.isBlank()) playlists
        else {
            val q = searchQuery.trim().lowercase(Locale.US)
            playlists.filter { it.name.lowercase(Locale.US).contains(q) }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = surfaceColor,
            tonalElevation = 0.dp,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .widthIn(max = 460.dp)
                .wrapContentHeight()
                .heightIn(max = 460.dp)
                .testTag("add_to_playlist_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(primaryColor.copy(alpha = 0.12f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Add to Playlist",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Adding $videoCount ${if (videoCount == 1) "video" else "videos"}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("close_add_to_playlist_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = primaryColor.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showCreateDialog = true }
                        .testTag("create_new_playlist_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Create New Playlist",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = primaryColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
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
                                text = "Search playlists",
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
                                .testTag("search_playlists_input")
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("clear_playlist_search_button")
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

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Existing Playlists",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryColor
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .testTag("existing_playlists_list")
                ) {
                    items(filteredPlaylists, key = { it.id }) { playlist ->
                        val isChecked = playlist.id in selectedPlaylistIds
                        val isFav = playlist.id == PlaylistRepository.FAVORITES_ID
                        val rowShape = RoundedCornerShape(14.dp)
                        val rowBackground = if (isChecked) {
                            primaryColor.copy(alpha = 0.10f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .clip(rowShape)
                                .background(rowBackground)
                                .then(
                                    if (isChecked) {
                                        Modifier.border(
                                            width = 1.dp,
                                            color = primaryColor.copy(alpha = 0.40f),
                                            shape = rowShape
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable {
                                    selectedPlaylistIds = if (isChecked) {
                                        selectedPlaylistIds - playlist.id
                                    } else {
                                        selectedPlaylistIds + playlist.id
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("playlist_item_${playlist.id}")
                        ) {
                            val badgeBg = if (isFav) {
                                Color(0xFFEC4899).copy(alpha = 0.12f)
                            } else {
                                primaryColor.copy(alpha = 0.12f)
                            }
                            val badgeIcon = if (isFav) {
                                Icons.Rounded.Favorite
                            } else {
                                Icons.AutoMirrored.Rounded.QueueMusic
                            }
                            val badgeTint = if (isFav) Color(0xFFEC4899) else primaryColor

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(badgeBg)
                            ) {
                                Icon(
                                    imageVector = badgeIcon,
                                    contentDescription = null,
                                    tint = badgeTint,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = playlist.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${playlist.count} ${if (playlist.count == 1) "video" else "videos"}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = primaryColor,
                                    checkmarkColor = Color.White,
                                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier.testTag("playlist_checkbox_${playlist.id}")
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("cancel_add_to_playlist_button")
                    ) {
                        Text(
                            text = "Cancel",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onAddVideosToPlaylists(selectedPlaylistIds) },
                        enabled = selectedPlaylistIds.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .height(42.dp)
                            .testTag("confirm_add_to_playlist_button")
                    ) {
                        Text(
                            text = "Add to Selected",
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreatePlaylistPromptDialog(
            onConfirm = { name ->
                onCreatePlaylist(name)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }
}

/**
 * Frosted Playlists Browser Dialog triggered from the Home screen `Playlists` shortcut chip:
 * - Lists `Favorites` & all custom playlists saved in `playlists.json`.
 * - Allows creating new playlists, deleting custom playlists, inspecting playlist videos, and
 *   launching sequential playlist playback.
 */
@Composable
fun PlaylistsBrowserDialog(
    playlists: List<PlaylistItem>,
    allVideos: List<VideoItem>,
    onCreatePlaylist: (String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onRemoveVideoFromPlaylist: (playlistId: String, videoId: Long) -> Unit,
    onPlayPlaylistVideos: (startVideo: VideoItem, queue: List<VideoItem>) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    var selectedPlaylistId by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val selectedPlaylist = remember(playlists, selectedPlaylistId) {
        playlists.find { it.id == selectedPlaylistId }
    }
    val videoMap = remember(allVideos) {
        allVideos.associateBy { it.id }
    }
    val selectedVideos = remember(selectedPlaylist, videoMap) {
        selectedPlaylist?.videoIds?.mapNotNull { videoMap[it] }.orEmpty()
    }

    val dialogShape = RoundedCornerShape(26.dp)
    val glassBg = if (isDark) Color(0xFF141B2D).copy(alpha = 0.88f) else Color.White.copy(alpha = 0.88f)
    val rimBrush = if (isDark) {
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.26f), Color.White.copy(alpha = 0.08f)))
    } else {
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.95f), Color(0xFFD6D9F8).copy(alpha = 0.65f)))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
        ) {
            FrostedGlassSurface(
                shape = dialogShape,
                blurRadius = 24.dp,
                border = BorderStroke(1.2.dp, rimBrush),
                glassProgressOverride = 1f,
                baseBackgroundColor = glassBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .crystalOuterShadow(shape = dialogShape, isDark = isDark, elevation = 18.dp)
                    .testTag("playlists_browser_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (selectedPlaylist != null) {
                            IconButton(
                                onClick = { selectedPlaylistId = null },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("playlists_browser_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Playlists",
                                    tint = palette.brandTitleColor
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedPlaylist?.name ?: "Playlists",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.primaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (selectedPlaylist != null) {
                                    "${selectedVideos.size} ${if (selectedVideos.size == 1) "video" else "videos"}"
                                } else {
                                    "${playlists.size} saved ${if (playlists.size == 1) "playlist" else "playlists"}"
                                },
                                fontSize = 12.5.sp,
                                color = palette.secondaryText
                            )
                        }
                        if (selectedPlaylist == null) {
                            IconButton(
                                onClick = { showCreateDialog = true },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("playlists_browser_add_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Create New Playlist",
                                    tint = palette.brandTitleColor
                                )
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("playlists_browser_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close",
                                tint = palette.secondaryText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (selectedPlaylist == null) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 140.dp, max = 340.dp)
                                .testTag("playlists_browser_list")
                        ) {
                            items(playlists, key = { it.id }) { playlist ->
                                val resolvedCount = playlist.videoIds.count { it in videoMap }
                                val isFav = playlist.id == PlaylistRepository.FAVORITES_ID
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(palette.neutralChipBg.copy(alpha = 0.72f))
                                        .clickable { selectedPlaylistId = playlist.id }
                                        .padding(horizontal = 14.dp, vertical = 12.dp)
                                        .testTag("browser_playlist_row_${playlist.id}")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isFav) Color(0xFFEC4899).copy(alpha = 0.14f)
                                                else palette.periwinkleBadgeBg
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isFav) Icons.Filled.Favorite else Icons.Filled.PlaylistPlay,
                                            contentDescription = null,
                                            tint = if (isFav) Color(0xFFEC4899) else palette.brandTitleColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = playlist.name,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = palette.primaryText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "$resolvedCount ${if (resolvedCount == 1) "video" else "videos"}",
                                            fontSize = 12.sp,
                                            color = palette.secondaryText
                                        )
                                    }
                                    if (!isFav) {
                                        IconButton(
                                            onClick = { onDeletePlaylist(playlist.id) },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("delete_playlist_${playlist.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.DeleteOutline,
                                                contentDescription = "Delete playlist",
                                                tint = DangerRed,
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else if (selectedVideos.isEmpty()) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .testTag("playlist_empty_state")
                        ) {
                            Text(
                                text = "No videos saved in ${selectedPlaylist.name} yet.\nUse Add to Playlist from any video or selection.",
                                fontSize = 13.5.sp,
                                color = palette.secondaryText
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                onDismiss()
                                onPlayPlaylistVideos(selectedVideos.first(), selectedVideos)
                            },
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.brandTitleColor,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("play_playlist_all_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Play All (${selectedVideos.size})",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 300.dp)
                                .testTag("playlist_videos_list")
                        ) {
                            items(selectedVideos, key = { it.id }) { video ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(palette.neutralChipBg.copy(alpha = 0.65f))
                                        .clickable {
                                            onDismiss()
                                            onPlayPlaylistVideos(video, selectedVideos)
                                        }
                                        .padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
                                        .testTag("playlist_video_row_${video.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        tint = palette.brandTitleColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = video.displayName,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = palette.primaryText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${video.formattedDuration} • ${video.formattedResolution} • ${video.formattedSize}",
                                            fontSize = 11.5.sp,
                                            color = palette.secondaryText
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            onRemoveVideoFromPlaylist(selectedPlaylist.id, video.id)
                                        },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("remove_from_playlist_${video.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Remove from playlist",
                                            tint = palette.secondaryText,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreatePlaylistPromptDialog(
            onConfirm = { name ->
                onCreatePlaylist(name)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }
}

@Composable
private fun CreatePlaylistPromptDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalExtendedPalette.current
    var playlistName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = palette.sheetBackground,
        title = {
            Text(
                text = "New Playlist",
                fontWeight = FontWeight.Bold,
                color = palette.primaryText
            )
        },
        text = {
            OutlinedTextField(
                value = playlistName,
                onValueChange = { playlistName = it },
                singleLine = true,
                label = { Text("Playlist name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("new_playlist_name_input")
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleaned = playlistName.trim()
                    if (cleaned.isNotEmpty()) {
                        onConfirm(cleaned)
                    }
                },
                enabled = playlistName.trim().isNotEmpty(),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = palette.brandTitleColor),
                modifier = Modifier.testTag("new_playlist_confirm_button")
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
