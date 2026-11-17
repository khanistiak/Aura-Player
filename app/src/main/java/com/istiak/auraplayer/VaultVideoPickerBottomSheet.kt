package com.istiak.auraplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.istiak.auraplayer.ui.theme.LocalExtendedPalette

/**
 * Liquid Glass `ModalBottomSheet` allowing users to browse non-vault device library videos,
 * search by title/folder, toggle multi-select checkboxes or Select All, and move all chosen
 * videos into the `.private` vault via the existing zero-copy `.kv` relocation pipeline.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultVideoPickerBottomSheet(
    availableVideos: List<VideoItem>,
    imageLoader: ImageLoader,
    onDismiss: () -> Unit,
    onMoveSelectedToVault: (List<VideoItem>) -> Unit
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var searchQuery by remember { mutableStateOf("") }
    var selectedVideoIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    val filteredVideos = remember(availableVideos, searchQuery) {
        val trimmed = searchQuery.trim()
        if (trimmed.isEmpty()) {
            availableVideos
        } else {
            availableVideos.filter { video ->
                video.displayName.contains(trimmed, ignoreCase = true) ||
                    video.title.contains(trimmed, ignoreCase = true) ||
                    video.folderName.contains(trimmed, ignoreCase = true)
            }
        }
    }

    val allFilteredSelected = filteredVideos.isNotEmpty() &&
        filteredVideos.all { it.id in selectedVideoIds }
    val selectedVideosList = remember(availableVideos, selectedVideoIds) {
        availableVideos.filter { it.id in selectedVideoIds }
    }

    val sheetGlassBg = if (isDark) {
        Color(0xFF141B2E).copy(alpha = 0.94f)
    } else {
        Color(0xFFF6F8FF).copy(alpha = 0.96f)
    }
    val searchBg = if (isDark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color.White.copy(alpha = 0.85f)
    }
    val searchBorder = if (isDark) {
        Color.White.copy(alpha = 0.16f)
    } else {
        Color(0xFFD6DDF5)
    }
    val accentBlue = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = sheetGlassBg,
        contentColor = palette.primaryText,
        modifier = Modifier.testTag("vault_video_picker_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.84f)
                .navigationBarsPadding()
        ) {
            // Header + Select All Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Add Videos to Vault",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (selectedVideoIds.isNotEmpty()) {
                            "${selectedVideoIds.size} selected • ${filteredVideos.size} available"
                        } else {
                            "${availableVideos.size} library videos available"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryText
                    )
                }

                if (filteredVideos.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (allFilteredSelected) accentBlue.copy(alpha = 0.16f) else palette.periwinkleBadgeBg,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable {
                                val filteredIds = filteredVideos.map { it.id }.toSet()
                                selectedVideoIds = if (allFilteredSelected) {
                                    selectedVideoIds - filteredIds
                                } else {
                                    selectedVideoIds + filteredIds
                                }
                            }
                            .testTag("vault_picker_select_all")
                    ) {
                        Text(
                            text = if (allFilteredSelected) "Deselect All" else "Select All",
                            color = if (allFilteredSelected) accentBlue else palette.periwinkleBadgeText,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Frosted Search Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(46.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(searchBg)
                    .border(1.dp, searchBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search library videos",
                    tint = palette.secondaryText,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier.weight(1f)
                ) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search by video or folder name...",
                            color = palette.secondaryText.copy(alpha = 0.72f),
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = palette.primaryText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        cursorBrush = SolidColor(accentBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("vault_picker_search_input")
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("vault_picker_clear_search")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Clear search",
                            tint = palette.secondaryText,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Scrollable Video Selection List
            if (filteredVideos.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 28.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(palette.periwinkleBadgeBg)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VideoLibrary,
                                contentDescription = null,
                                tint = palette.periwinkleBadgeText,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (availableVideos.isEmpty()) {
                                "No Library Videos Available"
                            } else {
                                "No Matching Videos"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.primaryText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (availableVideos.isEmpty()) {
                                "All scanned videos from your library are already locked in Private Vault."
                            } else {
                                "Try a different keyword or clear the search filter."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryText,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("vault_picker_list")
                ) {
                    items(filteredVideos, key = { "${it.id}_${it.uri}" }) { video ->
                        val isSelected = video.id in selectedVideoIds
                        VaultPickerVideoRow(
                            video = video,
                            imageLoader = imageLoader,
                            isSelected = isSelected,
                            accentColor = accentBlue,
                            onToggle = {
                                selectedVideoIds = if (isSelected) {
                                    selectedVideoIds - video.id
                                } else {
                                    selectedVideoIds + video.id
                                }
                            }
                        )
                    }
                }
            }

            // Sticky Bottom Primary Action Footer
            HorizontalDivider(
                color = if (isDark) Color.White.copy(alpha = 0.10f) else Color(0xFFE2E8F6)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                sheetGlassBg.copy(alpha = 0.90f),
                                sheetGlassBg
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                val count = selectedVideosList.size
                val buttonLabel = when (count) {
                    0 -> "Select Videos to Move"
                    1 -> "Move 1 Video to Vault"
                    else -> "Move $count Videos to Vault"
                }

                Button(
                    onClick = {
                        if (selectedVideosList.isNotEmpty()) {
                            onMoveSelectedToVault(selectedVideosList)
                            onDismiss()
                        }
                    },
                    enabled = count > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentBlue,
                        contentColor = Color.White,
                        disabledContainerColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFDCE3F5),
                        disabledContentColor = palette.secondaryText
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("vault_picker_confirm_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = buttonLabel,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun VaultPickerVideoRow(
    video: VideoItem,
    imageLoader: ImageLoader,
    isSelected: Boolean,
    accentColor: Color,
    onToggle: () -> Unit
) {
    val context = LocalContext.current
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark

    val rowBg = when {
        isSelected && isDark -> Color(0xFF233258).copy(alpha = 0.85f)
        isSelected -> Color(0xFFE7EEFF)
        isDark -> Color.White.copy(alpha = 0.05f)
        else -> Color.White.copy(alpha = 0.72f)
    }
    val rowBorder = when {
        isSelected -> accentColor.copy(alpha = 0.65f)
        isDark -> Color.White.copy(alpha = 0.10f)
        else -> Color(0xFFE1E7F8)
    }

    val thumbnailKey = remember(
        video.id,
        video.uri,
        video.filePath,
        video.vaultFilePath,
        video.durationMs,
        video.dateModifiedSec,
        video.isVaultItem
    ) {
        VideoThumbnailKey(
            uri = video.uri,
            filePath = video.filePath ?: video.vaultFilePath,
            durationMs = video.durationMs,
            dateModifiedSec = video.dateModifiedSec,
            isVaultItem = video.isVaultItem
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(rowBg)
            .border(1.dp, rowBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 10.dp, vertical = 10.dp)
            .testTag("vault_picker_item_${video.id}")
    ) {
        Box(
            modifier = Modifier
                .width(106.dp)
                .height(60.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(palette.neutralChipBg)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(thumbnailKey)
                    .build(),
                imageLoader = imageLoader,
                contentDescription = video.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.Black.copy(alpha = 0.76f))
                    .padding(horizontal = 5.dp, vertical = 1.5.dp)
            ) {
                Text(
                    text = video.formattedDuration,
                    color = Color.White,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = video.displayName.ifBlank { video.title },
                color = palette.primaryText,
                fontSize = 13.5.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${video.folderName.ifBlank { "Library" }} • ${video.formattedDuration} • ${video.formattedSize}",
                color = palette.secondaryText,
                fontSize = 11.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = accentColor,
                uncheckedColor = palette.secondaryText.copy(alpha = 0.6f),
                checkmarkColor = Color.White
            ),
            modifier = Modifier.testTag("vault_picker_checkbox_${video.id}")
        )
    }
}
