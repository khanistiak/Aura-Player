package com.istiak.auraplayer

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.istiak.auraplayer.ui.components.FrostedGlassSurface
import com.istiak.auraplayer.ui.components.ScrollAwareFixedHeader
import com.istiak.auraplayer.ui.components.crystalOuterShadow
import com.istiak.auraplayer.ui.components.recordFrostedBackdrop
import com.istiak.auraplayer.ui.components.rememberScrollAwareGlassLayout
import com.istiak.auraplayer.ui.theme.DangerRed
import com.istiak.auraplayer.ui.theme.LocalExtendedPalette
import java.io.File

/**
 * Private Video Vault Screen (`VaultScreen.kt`):
 * - Protected by `androidx.biometric.BiometricPrompt` (Fingerprint, Face, or custom Vault PIN fallback).
 * - Parses `/storage/emulated/0/AuraPlayer/.private/vault.kv` (`<private_path>><original_path>`) so each
 *   locked item displays its original filename (`File(originalAbsolutePath).name`), original parent folder
 *   badge, isolated thumbnail (`context.filesDir/vault_thumbs/`), duration, and file size.
 * - Passes `Uri.fromFile(File(entry.privateFilePath))` directly to `ExoPlayer` for instant 4K hardware playback.
 */
@Composable
fun VaultScreen(
    isUnlocked: Boolean,
    showFallbackPinSheet: Boolean,
    vaultVideos: List<VideoItem>,
    authErrorMessage: String?,
    onRequestBiometricUnlock: () -> Unit,
    onFallbackPinAuthenticated: () -> Unit,
    onDismissFallbackPin: () -> Unit,
    onLockVault: () -> Unit,
    onOpenSettings: () -> Unit,
    onPlayVaultVideo: (VideoItem, List<VideoItem>) -> Unit,
    onRestoreToGallery: (VideoItem) -> Unit,
    onDeletePermanently: (VideoItem) -> Unit,
    isSecureFolderSettingsHidden: Boolean = false,
    onOpenPrivateFolderSettings: () -> Unit = {},
    onUnhideSecureFolderSettings: () -> Unit = {},
    availableLibraryVideos: List<VideoItem> = emptyList(),
    onImportVideosToVault: (List<VideoItem>) -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    var showVaultOverflowMenu by remember { mutableStateOf(false) }
    var showImportPickerSheet by remember { mutableStateOf(false) }
    val imageLoader = rememberVideoImageLoader()
    val vaultManager = remember(context) { PrivateVaultManager(context.applicationContext) }

    // Parse vault.kv directly so originalAbsolutePath & privateFilePath mappings are always authoritative
    val kvLookupByPrivatePath = remember(vaultVideos, isUnlocked) {
        vaultManager.readVaultKvEntries().associateBy { it.privateFilePath }
    }

    val resolvedVaultVideos = remember(vaultVideos, kvLookupByPrivatePath) {
        vaultVideos.map { item ->
            val privatePath = item.vaultFilePath ?: item.uri.path ?: ""
            val kvEntry = kvLookupByPrivatePath[privatePath]
            val origAbsPath = kvEntry?.originalAbsolutePath
                ?: item.relativePath.takeIf { it.startsWith("/") }
                ?: "/storage/emulated/0/${item.folderName}/${item.displayName}"
            val origFile = File(origAbsPath)
            val pFile = if (privatePath.isNotBlank()) File(privatePath) else origFile
            val hexId = kvEntry?.hexId ?: pFile.nameWithoutExtension
            val isolatedThumb = File(context.filesDir, "vault_thumbs/vault_thumb_$hexId.jpg")
            val thumbPath = item.vaultThumbnailPath
                ?: isolatedThumb.takeIf { it.exists() && it.length() > 0L }?.absolutePath

            item.copy(
                uri = Uri.fromFile(pFile),
                displayName = origFile.name.ifBlank { item.displayName },
                title = origFile.nameWithoutExtension.ifBlank { item.title },
                folderName = origFile.parentFile?.name?.takeIf { it.isNotBlank() } ?: item.folderName,
                relativePath = origAbsPath,
                sizeBytes = if (pFile.exists() && pFile.length() > 0L) pFile.length() else item.sizeBytes,
                isVaultItem = true,
                vaultFilePath = pFile.absolutePath,
                vaultThumbnailPath = thumbPath
            )
        }
    }

    val listState = rememberLazyListState()
    val navBottomDp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val scrollLayout = rememberScrollAwareGlassLayout(
        listState = listState,
        defaultHeaderHeight = 64.dp,
        extraTopGap = 4.dp,
        bottomPadding = navBottomDp + 104.dp
    )

    PastelMeshBackground(
        modifier = modifier
            .fillMaxSize()
            .testTag("vault_screen")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!isUnlocked) {
                // Biometric / Device Credential Authentication Gate
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = 24.dp,
                            end = 24.dp,
                            top = scrollLayout.measuredHeaderHeightDp + 16.dp,
                            bottom = navBottomDp + 80.dp
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = palette.sheetBackground,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(28.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(palette.periwinkleBadgeBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Fingerprint,
                                    contentDescription = "Biometric Unlock",
                                    tint = palette.periwinkleBadgeText,
                                    modifier = Modifier.size(40.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Text(
                                text = "Private Vault Locked",
                                style = MaterialTheme.typography.titleLarge,
                                color = palette.primaryText,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Locked videos are relocated instantly into /storage/emulated/0/AuraPlayer/.private/ with .nomedia isolation and vault.kv path mapping—hidden from Gallery apps and playable natively in 4K.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.secondaryText,
                                textAlign = TextAlign.Center
                            )

                            if (!authErrorMessage.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = authErrorMessage,
                                    color = DangerRed,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }

                            if (showFallbackPinSheet) {
                                Spacer(modifier = Modifier.height(16.dp))
                                VaultFallbackPinSection(
                                    hasExistingPin = BiometricAuth.hasCustomVaultPin(context),
                                    onVerifyOrCreatePin = { enteredPin ->
                                        if (!BiometricAuth.hasCustomVaultPin(context)) {
                                            BiometricAuth.setCustomVaultPin(context, enteredPin)
                                            onFallbackPinAuthenticated()
                                            true
                                        } else if (BiometricAuth.verifyCustomVaultPin(context, enteredPin)) {
                                            onFallbackPinAuthenticated()
                                            true
                                        } else {
                                            false
                                        }
                                    },
                                    onCancel = onDismissFallbackPin
                                )
                            } else {
                                Spacer(modifier = Modifier.height(22.dp))

                                Button(
                                    onClick = onRequestBiometricUnlock,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = palette.brandTitleColor,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("unlock_vault_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.LockOpen,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Unlock with Biometrics / PIN",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (resolvedVaultVideos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = 28.dp,
                            end = 28.dp,
                            top = scrollLayout.measuredHeaderHeightDp,
                            bottom = navBottomDp + 80.dp
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { showImportPickerSheet = true }
                            .padding(horizontal = 20.dp, vertical = 22.dp)
                            .testTag("vault_empty_import_card")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(palette.periwinkleBadgeBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shield,
                                contentDescription = "Tap to import videos into Private Vault",
                                tint = palette.periwinkleBadgeText,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No Hidden Videos",
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.primaryText
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap the + button below or long-press any video in your folders and choose \"Move to Private Vault\" to lock it instantly.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.secondaryText,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = scrollLayout.contentPadding,
                    modifier = Modifier
                        .fillMaxSize()
                        .recordFrostedBackdrop(
                            backdropState = scrollLayout.backdropState,
                            scrollableState = listState,
                            contentLayerOverride = scrollLayout.contentGraphicsLayer
                        )
                        .testTag("vault_videos_list")
                ) {
                    items(resolvedVaultVideos, key = { it.id }) { vaultVideo ->
                        VaultVideoRowItem(
                            video = vaultVideo,
                            imageLoader = imageLoader,
                            onPlay = {
                                val directUri = vaultVideo.vaultFilePath
                                    ?.let { Uri.fromFile(File(it)) }
                                    ?: vaultVideo.uri
                                val playable = vaultVideo.copy(uri = directUri)
                                onPlayVaultVideo(playable, resolvedVaultVideos)
                            },
                            onRestore = { onRestoreToGallery(vaultVideo) },
                            onDelete = { onDeletePermanently(vaultVideo) }
                        )
                    }
                }
            }

            // Scroll-aware, bottom-feathered Liquid Glass Top App Bar
            ScrollAwareFixedHeader(
                layoutState = scrollLayout,
                blurRadius = 18.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 6.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("vault_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = palette.brandTitleColor
                            )
                        }
                        Text(
                            text = "Private Vault",
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.brandTitleColor
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isUnlocked) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = palette.periwinkleBadgeBg,
                                modifier = Modifier
                                    .clickable { onLockVault() }
                                    .testTag("lock_vault_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Lock,
                                        contentDescription = "Lock Vault",
                                        tint = palette.periwinkleBadgeText,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Lock",
                                        color = palette.periwinkleBadgeText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("vault_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Settings",
                                tint = palette.brandTitleColor
                            )
                        }

                        Box {
                            IconButton(
                                onClick = { showVaultOverflowMenu = true },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("vault_more_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = "More Vault Options",
                                    tint = palette.brandTitleColor
                                )
                            }
                            DropdownMenu(
                                expanded = showVaultOverflowMenu,
                                onDismissRequest = { showVaultOverflowMenu = false },
                                containerColor = palette.sheetBackground
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Private Folder Settings",
                                            color = palette.primaryText,
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.Security,
                                            contentDescription = null,
                                            tint = palette.brandTitleColor
                                        )
                                    },
                                    onClick = {
                                        showVaultOverflowMenu = false
                                        onOpenPrivateFolderSettings()
                                    },
                                    modifier = Modifier.testTag("vault_menu_private_folder_settings")
                                )
                                if (isSecureFolderSettingsHidden) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "Unhide Secure Folder Settings",
                                                color = palette.primaryText,
                                                fontWeight = FontWeight.Medium
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Filled.Visibility,
                                                contentDescription = null,
                                                tint = palette.brandTitleColor
                                            )
                                        },
                                        onClick = {
                                            showVaultOverflowMenu = false
                                            onUnhideSecureFolderSettings()
                                        },
                                        modifier = Modifier.testTag("vault_menu_unhide_secure_settings")
                                    )
                                }
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
                                            tint = palette.secondaryText
                                        )
                                    },
                                    onClick = {
                                        showVaultOverflowMenu = false
                                        onOpenSettings()
                                    }
                                )
                            }
                        }
                    }
                }
            }

        if (isUnlocked) {
            val fabInteractionSource = remember { MutableInteractionSource() }
            val isPressed by fabInteractionSource.collectIsPressedAsState()
            val pressScale by animateFloatAsState(
                targetValue = if (isPressed) 0.92f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "vault_add_fab_press_scale"
            )
            val glassBaseBg = if (isDark) {
                Color(0xFF1A2236).copy(alpha = 0.64f)
            } else {
                Color.White.copy(alpha = 0.60f)
            }
            val crystalBorder = if (isDark) {
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.24f),
                        Color(0xFF93C5FD).copy(alpha = 0.14f),
                        Color.White.copy(alpha = 0.10f)
                    )
                )
            } else {
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.96f),
                        Color(0xFFE0EBFF).copy(alpha = 0.72f),
                        Color.White.copy(alpha = 0.88f)
                    )
                )
            }
            val specularSheen = if (isDark) {
                Brush.verticalGradient(
                    0.0f to Color.White.copy(alpha = 0.16f),
                    0.5f to Color.Transparent,
                    1.0f to Color.White.copy(alpha = 0.06f)
                )
            } else {
                Brush.verticalGradient(
                    0.0f to Color.White.copy(alpha = 0.64f),
                    0.48f to Color.White.copy(alpha = 0.16f),
                    1.0f to Color.White.copy(alpha = 0.34f)
                )
            }
            val plusTint = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)

            FrostedGlassSurface(
                shape = CircleShape,
                blurRadius = 28.dp,
                border = BorderStroke(1.dp, crystalBorder),
                glassProgressOverride = 1f,
                baseBackgroundColor = glassBaseBg,
                specularOverlayBrush = specularSheen,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 20.dp, bottom = 20.dp)
                    .graphicsLayer {
                        scaleX = pressScale
                        scaleY = pressScale
                    }
                    .size(58.dp)
                    .crystalOuterShadow(
                        shape = CircleShape,
                        isDark = isDark,
                        elevation = 18.dp
                    )
                    .clickable(
                        interactionSource = fabInteractionSource,
                        indication = null,
                        onClick = { showImportPickerSheet = true }
                    )
                    .testTag("vault_add_videos_fab")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add Videos to Private Vault",
                        tint = plusTint,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }

    if (isUnlocked && showImportPickerSheet) {
        val importableVideos = remember(availableLibraryVideos, resolvedVaultVideos) {
            val lockedPaths = resolvedVaultVideos.mapNotNull { it.relativePath.takeIf(String::isNotBlank) }.toSet()
            availableLibraryVideos.filterNot { candidate ->
                candidate.isVaultItem || (candidate.filePath != null && candidate.filePath in lockedPaths)
            }
        }
        VaultVideoPickerBottomSheet(
            availableVideos = importableVideos,
            imageLoader = imageLoader,
            onDismiss = { showImportPickerSheet = false },
            onMoveSelectedToVault = { selected ->
                onImportVideosToVault(selected)
            }
        )
    }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VaultVideoRowItem(
    video: VideoItem,
    imageLoader: coil.ImageLoader,
    onPlay: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val palette = LocalExtendedPalette.current

    val originalFileName = remember(video.relativePath, video.displayName) {
        val fromOrigPath = File(video.relativePath).name
        if (fromOrigPath.isNotBlank() && fromOrigPath.contains('.')) {
            fromOrigPath
        } else {
            video.displayName
        }
    }

    val originalFolderBadge = remember(video.relativePath, video.folderName) {
        File(video.relativePath).parentFile?.name?.takeIf { it.isNotBlank() }
            ?: video.folderName.ifBlank { "Movies" }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("vault_video_item_${video.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .width(130.dp)
                    .height(74.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.neutralChipBg)
            ) {
                val isolatedThumbModel = remember(video.id, video.vaultThumbnailPath, video.vaultFilePath) {
                    val thumbFile = video.vaultThumbnailPath?.let { File(it) }
                    if (thumbFile != null && thumbFile.exists() && thumbFile.length() > 0L) {
                        thumbFile
                    } else {
                        val hexId = video.vaultFilePath?.let { File(it).nameWithoutExtension }
                        val fallbackThumb = hexId?.let { File(context.filesDir, "vault_thumbs/vault_thumb_$it.jpg") }
                        if (fallbackThumb != null && fallbackThumb.exists() && fallbackThumb.length() > 0L) {
                            fallbackThumb
                        } else {
                            video.vaultFilePath?.let { File(it) } ?: video.uri
                        }
                    }
                }
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(isolatedThumbModel)
                        .diskCachePolicy(CachePolicy.DISABLED)
                        .videoFrameMillis(1000L)
                        .build(),
                    imageLoader = imageLoader,
                    contentDescription = originalFileName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(palette.periwinkleBadgeBg)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "VAULT",
                        color = palette.periwinkleBadgeText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.76f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = video.formattedDuration,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = originalFileName,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = palette.primaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Original parent folder badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(palette.periwinkleBadgeBg)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Folder,
                            contentDescription = null,
                            tint = palette.periwinkleBadgeText,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = originalFolderBadge,
                            color = palette.periwinkleBadgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    MetadataChip(text = video.formattedDuration)
                    MetadataChip(text = video.formattedSize)
                    MetadataChip(text = ".private", isAccent = true)
                }
                if (video.relativePath.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = video.relativePath,
                        fontSize = 11.sp,
                        color = palette.secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onRestore,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.periwinkleBadgeBg,
                    contentColor = palette.periwinkleBadgeText
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .testTag("restore_vault_video_${video.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.Restore,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Restore to $originalFolderBadge",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DangerRed.copy(alpha = 0.12f))
                    .testTag("delete_vault_video_${video.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Delete Permanently",
                    tint = DangerRed,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = palette.neutralChipBg)
    }
}

@Composable
private fun VaultFallbackPinSection(
    hasExistingPin: Boolean,
    onVerifyOrCreatePin: (String) -> Boolean,
    onCancel: () -> Unit
) {
    val palette = LocalExtendedPalette.current
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = if (hasExistingPin) {
                "Enter your Vault PIN"
            } else {
                "Create a 4-digit Vault PIN"
            },
            style = MaterialTheme.typography.titleMedium,
            color = palette.brandTitleColor
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = pinInput,
            onValueChange = { value ->
                if (value.length <= 6 && value.all { it.isDigit() }) {
                    pinInput = value
                    pinError = null
                }
            },
            label = { Text("Vault PIN") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("vault_pin_input")
        )
        if (pinError != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = pinError!!, color = DangerRed, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.neutralChipBg,
                    contentColor = palette.primaryText
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    if (pinInput.length < 4) {
                        pinError = "Enter at least 4 digits"
                    } else if (!onVerifyOrCreatePin(pinInput)) {
                        pinError = "Incorrect PIN"
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.brandTitleColor,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("submit_vault_pin_button")
            ) {
                Text(
                    text = if (hasExistingPin) "Unlock" else "Save & Unlock",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
