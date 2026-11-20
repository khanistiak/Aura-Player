package com.istiak.auraplayer

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.EnhancedEncryption
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockClock
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

/**
 * Private Video Vault Screen (`VaultScreen.kt`):
 * - Sets `WindowManager.LayoutParams.FLAG_SECURE` while active so screenshots and recent-apps
 *   thumbnails cannot capture PIN entry or private media.
 * - Protected by PBKDF2-HMAC-SHA256 salted PIN hashing wrapped in Android Keystore AES-256-GCM,
 *   5-attempt / 30-second brute-force lockout protection, and optional `BiometricPrompt`
 *   (`BIOMETRIC_STRONG or DEVICE_CREDENTIAL`).
 * - Features a Liquid Glass PIN Lock Screen with glowing Aura Blue lock emblem, animated PIN dots,
 *   tactile circular glass keypad (`0`–`9`, Biometric, Backspace), and smooth error shake animation.
 * - Unlocked Media Screen features a Liquid Glass header with item count + `"Protected with Keystore"`
 *   badge, frosted video cards with thumbnails & duration badges, and clear Unhide/Delete dialogs.
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
    isBiometricPromptShowing: Boolean = false,
    isBiometricUnlockEnabled: Boolean = true,
    onVaultPinSavedRecord: (String) -> Unit = {},
    onLeaveVault: () -> Unit = onLockVault,
    isSecureFolderSettingsHidden: Boolean = false,
    onOpenPrivateFolderSettings: () -> Unit = {},
    onUnhideSecureFolderSettings: () -> Unit = {},
    availableLibraryVideos: List<VideoItem> = emptyList(),
    onImportVideosToVault: (List<VideoItem>) -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnLockVault by rememberUpdatedState(onLockVault)
    val currentOnLeaveVault by rememberUpdatedState(onLeaveVault)
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    var isNavigatingAway by remember { mutableStateOf(false) }
    var isNavigatingToSettings by remember { mutableStateOf(false) }
    var wasUnlockedBeforeNavigatingAway by remember { mutableStateOf(isUnlocked) }
    if (!isNavigatingAway) {
        wasUnlockedBeforeNavigatingAway = isUnlocked
    }
    val showUnlockedVaultContent = isUnlocked || (isNavigatingAway && wasUnlockedBeforeNavigatingAway)

    val handleExitVault = {
        if (!isNavigatingAway) {
            isNavigatingAway = true
            isNavigatingToSettings = false
            onBack()
        }
    }
    val handleOpenSettings = {
        if (!isNavigatingAway) {
            isNavigatingAway = true
            isNavigatingToSettings = true
            onOpenSettings()
        }
    }
    val handleOpenPrivateFolderSettings = {
        if (!isNavigatingAway) {
            isNavigatingAway = true
            isNavigatingToSettings = true
            onOpenPrivateFolderSettings()
        }
    }
    BackHandler { handleExitVault() }

    var showVaultOverflowMenu by remember { mutableStateOf(false) }
    var showImportPickerSheet by remember { mutableStateOf(false) }
    var pendingUnhideVideo by remember { mutableStateOf<VideoItem?>(null) }
    var pendingDeleteVideo by remember { mutableStateOf<VideoItem?>(null) }
    val imageLoader = rememberVideoImageLoader()
    val vaultManager = remember(context) { PrivateVaultManager(context.applicationContext) }

    // Reset navigation guard on resume, lock on Activity background (ON_STOP),
    // and lock on dispose only when truly leaving the Vault (not navigating into Settings).
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    isNavigatingAway = false
                    isNavigatingToSettings = false
                }
                Lifecycle.Event.ON_STOP -> {
                    if (!isNavigatingToSettings) {
                        currentOnLockVault()
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (!isNavigatingToSettings) {
                currentOnLeaveVault()
            }
        }
    }

    // Enforce FLAG_SECURE while inside the Private Vault so screenshots & recent-apps snapshots
    // cannot capture PIN entry or private media.
    DisposableEffect(context) {
        val window = context.findHostActivity()?.window
        val hadFlagBefore = window != null &&
            (window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            if (!hadFlagBefore) {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    // Migrate any legacy plaintext PIN automatically on first Vault entry
    LaunchedEffect(Unit) {
        val migratedRecord = withContext(Dispatchers.IO) {
            BiometricAuth.migrateLegacyPinIfNeeded(context.applicationContext)
        }
        if (!migratedRecord.isNullOrBlank()) {
            onVaultPinSavedRecord(migratedRecord)
        }
    }

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
        defaultHeaderHeight = 84.dp,
        extraTopGap = 6.dp,
        bottomPadding = navBottomDp + 104.dp
    )

    val canUseBiometric = remember(context, isBiometricUnlockEnabled) {
        isBiometricUnlockEnabled && BiometricAuth.canUseSystemBiometricOrCredential(context)
    }

    PastelMeshBackground(
        modifier = modifier
            .fillMaxSize()
            .testTag("vault_screen")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!showUnlockedVaultContent) {
                if (!isNavigatingAway) {
                    AnimatedContent(
                        targetState = isBiometricPromptShowing,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(160))
                        },
                        label = "vault_biometric_vs_pin_content"
                    ) { biometricActive ->
                        if (biometricActive) {
                            VaultBiometricVerificationBackdrop(
                                topClearanceDp = scrollLayout.measuredHeaderHeightDp + 8.dp,
                                bottomClearanceDp = navBottomDp + 20.dp
                            )
                        } else if (!isUnlocked && !isBiometricPromptShowing && !isNavigatingAway) {
                            VaultPinLockScreen(
                                authErrorMessage = authErrorMessage,
                                canUseBiometric = canUseBiometric,
                                topClearanceDp = scrollLayout.measuredHeaderHeightDp + 8.dp,
                                bottomClearanceDp = navBottomDp + 20.dp,
                                onRequestBiometricUnlock = onRequestBiometricUnlock,
                                onPinAuthenticated = onFallbackPinAuthenticated,
                                onPinCreatedAndSaved = { encryptedRecord ->
                                    onVaultPinSavedRecord(encryptedRecord)
                                    onFallbackPinAuthenticated()
                                }
                            )
                        }
                    }
                }
            } else if (resolvedVaultVideos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = 24.dp,
                            end = 24.dp,
                            top = scrollLayout.measuredHeaderHeightDp,
                            bottom = navBottomDp + 80.dp
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val emptyCardShape = RoundedCornerShape(26.dp)
                    val emptyCardBg = if (isDark) {
                        Color(0xFF141824).copy(alpha = 0.74f)
                    } else {
                        Color.White.copy(alpha = 0.74f)
                    }
                    val emptyBorder = if (isDark) {
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.24f), Color(0xFF60A5FA).copy(alpha = 0.16f))
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.94f), Color(0xFFBFD7FF).copy(alpha = 0.55f))
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .widthIn(max = 360.dp)
                            .fillMaxWidth()
                            .clip(emptyCardShape)
                            .background(emptyCardBg)
                            .border(1.dp, emptyBorder, emptyCardShape)
                            .clickable { showImportPickerSheet = true }
                            .padding(horizontal = 24.dp, vertical = 28.dp)
                            .testTag("vault_empty_import_card")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(70.dp)
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
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Hidden Videos",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryText
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap the + button below or choose \"Move to Private Vault\" on any video to hide it in your Keystore-protected vault.",
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
                    verticalArrangement = Arrangement.spacedBy(12.dp),
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
                        VaultVideoGlassCard(
                            video = vaultVideo,
                            imageLoader = imageLoader,
                            onPlay = {
                                val directUri = vaultVideo.vaultFilePath
                                    ?.let { Uri.fromFile(File(it)) }
                                    ?: vaultVideo.uri
                                val playable = vaultVideo.copy(uri = directUri)
                                onPlayVaultVideo(playable, resolvedVaultVideos)
                            },
                            onRequestUnhide = { pendingUnhideVideo = vaultVideo },
                            onRequestDelete = { pendingDeleteVideo = vaultVideo }
                        )
                    }
                }
            }

            // Scroll-aware Liquid Glass Header showing title, item count & Keystore protection badge
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
                        .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = handleExitVault,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("vault_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = palette.brandTitleColor
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Private Vault",
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.brandTitleColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (showUnlockedVaultContent) {
                                    val countLabel = when (resolvedVaultVideos.size) {
                                        0 -> "0 items"
                                        1 -> "1 item"
                                        else -> "${resolvedVaultVideos.size} items"
                                    }
                                    Text(
                                        text = countLabel,
                                        color = palette.secondaryText,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.testTag("vault_header_item_count")
                                    )
                                    Text(
                                        text = "•",
                                        color = palette.secondaryText.copy(alpha = 0.6f),
                                        fontSize = 11.sp
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(
                                            if (isDark) Color(0xFF1D4ED8).copy(alpha = 0.24f)
                                            else Color(0xFF2563EB).copy(alpha = 0.11f)
                                        )
                                        .border(
                                            width = 0.7.dp,
                                            color = if (isDark) Color(0xFF60A5FA).copy(alpha = 0.35f)
                                            else Color(0xFF2563EB).copy(alpha = 0.22f),
                                            shape = CircleShape
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                        .testTag("vault_keystore_badge")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.VerifiedUser,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Protected with Keystore",
                                        color = if (isDark) Color(0xFFDBEAFE) else Color(0xFF1D4ED8),
                                        fontSize = 10.sp,
                                        lineHeight = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = handleOpenPrivateFolderSettings,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("vault_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Private Vault Settings",
                                tint = palette.brandTitleColor
                            )
                        }

                        Box {
                            IconButton(
                                onClick = { showVaultOverflowMenu = true },
                                modifier = Modifier
                                    .size(40.dp)
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
                                            text = "Private Vault Settings",
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
                                        handleOpenPrivateFolderSettings()
                                    },
                                    modifier = Modifier.testTag("vault_menu_private_folder_settings")
                                )
                                if (isSecureFolderSettingsHidden) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "Unhide Private Vault Settings",
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
                                        handleOpenSettings()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (showUnlockedVaultContent) {
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

        pendingUnhideVideo?.let { targetVideo ->
            VaultUnhideConfirmDialog(
                video = targetVideo,
                onDismiss = { pendingUnhideVideo = null },
                onConfirmUnhide = {
                    pendingUnhideVideo = null
                    onRestoreToGallery(targetVideo)
                }
            )
        }

        pendingDeleteVideo?.let { targetVideo ->
            VaultDeleteConfirmDialog(
                video = targetVideo,
                onDismiss = { pendingDeleteVideo = null },
                onConfirmDelete = {
                    pendingDeleteVideo = null
                    onDeletePermanently(targetVideo)
                }
            )
        }
    }
}

/**
 * Clean, uncluttered Liquid Glass backdrop shown while Android's system BiometricPrompt sheet
 * is active (`isBiometricPromptShowing == true`), keeping the PIN keypad hidden until needed.
 */
@Composable
private fun VaultBiometricVerificationBackdrop(
    topClearanceDp: androidx.compose.ui.unit.Dp,
    bottomClearanceDp: androidx.compose.ui.unit.Dp
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val auraBlue = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = 24.dp,
                end = 24.dp,
                top = topClearanceDp,
                bottom = bottomClearanceDp + 72.dp
            )
            .testTag("vault_biometric_backdrop")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(116.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    auraBlue.copy(alpha = if (isDark) 0.34f else 0.24f),
                                    auraBlue.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            if (isDark) Color(0xFF141824).copy(alpha = 0.78f)
                            else Color.White.copy(alpha = 0.80f)
                        )
                        .border(
                            width = 1.2.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = if (isDark) 0.32f else 0.94f),
                                    auraBlue.copy(alpha = 0.42f)
                                )
                            ),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Fingerprint,
                        contentDescription = "Verify your identity",
                        tint = auraBlue,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Verify your identity",
                color = palette.primaryText,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Private Vault is protected with Android Keystore",
                color = palette.secondaryText,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Redesigned Liquid Glass Vault PIN Lock Screen:
 * - Centered frosted Liquid Glass card with glowing Aura Blue lock emblem.
 * - Sleek spring-animated 4-dot PIN indicator with smooth damped horizontal shake on wrong PIN.
 * - Tactile circular glass keypad (`0`–`9`, Biometric button, Backspace) with haptic feedback.
 * - Live 30-second brute-force lockout countdown after 5 failed attempts.
 * - Seamless first-time 4-digit PIN creation + confirmation flow when no PIN is configured yet.
 */
@Composable
private fun VaultPinLockScreen(
    authErrorMessage: String?,
    canUseBiometric: Boolean,
    topClearanceDp: androidx.compose.ui.unit.Dp,
    bottomClearanceDp: androidx.compose.ui.unit.Dp,
    onRequestBiometricUnlock: () -> Unit,
    onPinAuthenticated: () -> Unit,
    onPinCreatedAndSaved: (String) -> Unit
) {
    val context = LocalContext.current
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var hasExistingPin by remember { mutableStateOf(BiometricAuth.hasCustomVaultPin(context)) }
    var enteredPin by remember { mutableStateOf("") }
    var firstSetupPin by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }
    var statusError by remember { mutableStateOf<String?>(null) }
    var isErrorDotFlash by remember { mutableStateOf(false) }
    var lockoutState by remember { mutableStateOf(BiometricAuth.getLockoutState(context)) }
    var shakeTrigger by remember { mutableIntStateOf(0) }

    val shakeOffsetPx = remember { Animatable(0f) }

    // Live 30-second brute-force lockout countdown
    LaunchedEffect(lockoutState.lockoutUntilEpochMs) {
        if (lockoutState.lockoutUntilEpochMs > System.currentTimeMillis()) {
            while (true) {
                val updated = BiometricAuth.getLockoutState(context)
                lockoutState = updated
                if (!updated.isLockedOut) {
                    statusError = null
                    break
                }
                delay(250L)
            }
        }
    }

    // Smooth damped horizontal shake animation on wrong PIN
    LaunchedEffect(shakeTrigger) {
        if (shakeTrigger == 0) return@LaunchedEffect
        isErrorDotFlash = true
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        val keyframes = listOf(-24f, 22f, -18f, 14f, -8f, 5f, 0f)
        for (target in keyframes) {
            shakeOffsetPx.animateTo(
                targetValue = target,
                animationSpec = tween(durationMillis = 48)
            )
        }
        delay(120L)
        isErrorDotFlash = false
        enteredPin = ""
    }

    fun handleDigitTap(digit: String) {
        if (isVerifying || lockoutState.isLockedOut || enteredPin.length >= 4) return
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        statusError = null
        val updatedPin = enteredPin + digit
        enteredPin = updatedPin

        if (updatedPin.length == 4) {
            if (!hasExistingPin) {
                // First-time 2-step PIN creation + confirmation
                val initial = firstSetupPin
                if (initial == null) {
                    firstSetupPin = updatedPin
                    enteredPin = ""
                } else if (initial == updatedPin) {
                    isVerifying = true
                    scope.launch {
                        val encryptedRecord = withContext(Dispatchers.Default) {
                            BiometricAuth.setCustomVaultPin(context.applicationContext, updatedPin)
                        }
                        hasExistingPin = true
                        firstSetupPin = null
                        enteredPin = ""
                        isVerifying = false
                        onPinCreatedAndSaved(encryptedRecord)
                    }
                } else {
                    statusError = "PINs didn't match. Please try again."
                    firstSetupPin = null
                    shakeTrigger++
                }
            } else {
                // Verify hashed PIN off the UI thread (PBKDF2WithHmacSHA256 + constant-time check)
                isVerifying = true
                scope.launch {
                    val result = withContext(Dispatchers.Default) {
                        BiometricAuth.verifyVaultPinWithLockout(context.applicationContext, updatedPin)
                    }
                    isVerifying = false
                    when (result) {
                        is VaultPinVerificationResult.Success -> {
                            enteredPin = ""
                            statusError = null
                            lockoutState = VaultLockoutState()
                            onPinAuthenticated()
                        }
                        is VaultPinVerificationResult.InvalidPin -> {
                            statusError = "Incorrect PIN • ${result.remainingAttempts} ${if (result.remainingAttempts == 1) "attempt" else "attempts"} left"
                            lockoutState = BiometricAuth.getLockoutState(context.applicationContext)
                            shakeTrigger++
                        }
                        is VaultPinVerificationResult.LockedOut -> {
                            lockoutState = VaultLockoutState(
                                failedAttempts = BiometricAuth.MAX_FAILED_ATTEMPTS,
                                lockoutUntilEpochMs = result.lockoutUntilEpochMs,
                                remainingLockoutSeconds = result.remainingSeconds
                            )
                            statusError = null
                            shakeTrigger++
                        }
                    }
                }
            }
        }
    }

    fun handleBackspace() {
        if (isVerifying || lockoutState.isLockedOut) return
        if (enteredPin.isNotEmpty()) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            enteredPin = enteredPin.dropLast(1)
            statusError = null
        } else if (!hasExistingPin && firstSetupPin != null) {
            firstSetupPin = null
            statusError = null
        }
    }

    val auraBlue = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
    val cardShape = RoundedCornerShape(30.dp)
    val cardFill = if (isDark) {
        Color(0xFF141824).copy(alpha = 0.78f)
    } else {
        Color.White.copy(alpha = 0.76f)
    }
    val cardInnerGloss = if (isDark) {
        Brush.verticalGradient(
            0.0f to Color.White.copy(alpha = 0.10f),
            0.42f to Color.Transparent,
            1.0f to Color(0xFF60A5FA).copy(alpha = 0.06f)
        )
    } else {
        Brush.verticalGradient(
            0.0f to Color.White.copy(alpha = 0.72f),
            0.46f to Color.White.copy(alpha = 0.14f),
            1.0f to Color(0xFFDBEAFE).copy(alpha = 0.22f)
        )
    }
    val cardBorder = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.28f),
                Color.White.copy(alpha = 0.09f),
                Color(0xFF64B5F6).copy(alpha = 0.18f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.94f),
                Color(0xFFDCE7FF).copy(alpha = 0.65f),
                Color(0xFF93C5FD).copy(alpha = 0.36f)
            )
        )
    }

    val headline = when {
        lockoutState.isLockedOut -> "Vault Temporarily Locked"
        !hasExistingPin && firstSetupPin == null -> "Create Vault PIN"
        !hasExistingPin -> "Confirm Vault PIN"
        else -> "Enter Vault PIN"
    }
    val subtitle = when {
        lockoutState.isLockedOut -> "Too many failed PIN attempts"
        !hasExistingPin && firstSetupPin == null -> "Choose a 4-digit PIN"
        !hasExistingPin -> "Re-enter your 4-digit PIN to confirm"
        canUseBiometric -> "Use your 4-digit PIN or tap fingerprint to unlock"
        else -> "Enter your 4-digit PIN to access private videos"
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = topClearanceDp,
                bottom = bottomClearanceDp
            )
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 384.dp)
                .fillMaxWidth()
                .clip(cardShape)
                .background(cardFill)
                .background(cardInnerGloss)
                .border(1.1.dp, cardBorder, cardShape)
                .padding(horizontal = 24.dp, vertical = 26.dp)
                .testTag("vault_pin_lock_card")
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Glowing Aura Blue Lock Emblem
                val emblemTint = if (lockoutState.isLockedOut || isErrorDotFlash) DangerRed else auraBlue
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(86.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        emblemTint.copy(alpha = if (isDark) 0.36f else 0.24f),
                                        emblemTint.copy(alpha = 0.08f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(
                                if (isDark) Color(0xFF1A2540).copy(alpha = 0.88f)
                                else Color.White.copy(alpha = 0.88f)
                            )
                            .border(
                                width = 1.2.dp,
                                color = emblemTint.copy(alpha = if (isDark) 0.55f else 0.38f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = when {
                                lockoutState.isLockedOut -> Icons.Rounded.LockClock
                                !hasExistingPin -> Icons.Rounded.EnhancedEncryption
                                else -> Icons.Rounded.Lock
                            },
                            contentDescription = "Private Vault Lock",
                            tint = emblemTint,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = headline,
                    color = palette.primaryText,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = subtitle,
                    color = palette.secondaryText,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(22.dp))

                // Sleek Animated PIN Dots with Horizontal Shake Animation on Wrong PIN
                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .offset { IntOffset(shakeOffsetPx.value.roundToInt(), 0) }
                        .testTag("vault_pin_dots_row")
                ) {
                    repeat(4) { index ->
                        val isFilled = index < enteredPin.length
                        val dotScale by animateFloatAsState(
                            targetValue = if (isFilled) 1.18f else 1f,
                            animationSpec = spring(
                                dampingRatio = 0.52f,
                                stiffness = 520f
                            ),
                            label = "vault_pin_dot_scale_$index"
                        )
                        val dotFill by animateColorAsState(
                            targetValue = when {
                                isErrorDotFlash -> DangerRed
                                isFilled -> auraBlue
                                else -> if (isDark) Color.White.copy(alpha = 0.10f) else Color(0xFFE2E8F8)
                            },
                            animationSpec = tween(140),
                            label = "vault_pin_dot_fill_$index"
                        )
                        val dotBorder by animateColorAsState(
                            targetValue = when {
                                isErrorDotFlash -> DangerRed
                                isFilled -> auraBlue
                                else -> if (isDark) Color.White.copy(alpha = 0.28f) else Color(0xFF94A3B8).copy(alpha = 0.55f)
                            },
                            animationSpec = tween(140),
                            label = "vault_pin_dot_border_$index"
                        )

                        Box(
                            modifier = Modifier
                                .size(15.dp)
                                .graphicsLayer {
                                    scaleX = dotScale
                                    scaleY = dotScale
                                }
                                .clip(CircleShape)
                                .background(dotFill)
                                .border(1.4.dp, dotBorder, CircleShape)
                                .testTag("vault_pin_dot_$index")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Fixed-height status / lockout countdown slot so keypad never jumps vertically
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                ) {
                    when {
                        lockoutState.isLockedOut -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(DangerRed.copy(alpha = if (isDark) 0.22f else 0.12f))
                                    .border(0.8.dp, DangerRed.copy(alpha = 0.35f), CircleShape)
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                                    .testTag("vault_lockout_banner")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LockClock,
                                    contentDescription = null,
                                    tint = DangerRed,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Try again in ${lockoutState.remainingLockoutSeconds}s",
                                    color = DangerRed,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum")
                                )
                            }
                        }
                        !statusError.isNullOrBlank() -> {
                            Text(
                                text = statusError!!,
                                color = DangerRed,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.testTag("vault_pin_error_text")
                            )
                        }
                        !authErrorMessage.isNullOrBlank() -> {
                            Text(
                                text = authErrorMessage,
                                color = DangerRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                        else -> Unit
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tactile Circular Liquid Glass Keypad (0-9, Biometric, Backspace)
                val keypadRows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9")
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    keypadRows.forEach { rowDigits ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowDigits.forEach { digit ->
                                LiquidGlassKeypadButton(
                                    enabled = !lockoutState.isLockedOut && !isVerifying,
                                    onClick = { handleDigitTap(digit) },
                                    testTag = "vault_keypad_$digit"
                                ) {
                                    Text(
                                        text = digit,
                                        color = palette.primaryText,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontFeatureSettings = "tnum"
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Row: [Biometric] [0] [Backspace]
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (canUseBiometric && hasExistingPin) {
                            LiquidGlassKeypadButton(
                                enabled = true,
                                accentTint = auraBlue,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onRequestBiometricUnlock()
                                },
                                testTag = "vault_keypad_biometric"
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Fingerprint,
                                    contentDescription = "Unlock with Biometrics",
                                    tint = auraBlue,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.size(66.dp))
                        }

                        LiquidGlassKeypadButton(
                            enabled = !lockoutState.isLockedOut && !isVerifying,
                            onClick = { handleDigitTap("0") },
                            testTag = "vault_keypad_0"
                        ) {
                            Text(
                                text = "0",
                                color = palette.primaryText,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontFeatureSettings = "tnum"
                                )
                            )
                        }

                        LiquidGlassKeypadButton(
                            enabled = !lockoutState.isLockedOut && !isVerifying &&
                                (enteredPin.isNotEmpty() || (!hasExistingPin && firstSetupPin != null)),
                            onClick = { handleBackspace() },
                            testTag = "vault_keypad_backspace"
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Backspace,
                                contentDescription = "Backspace",
                                tint = palette.primaryText.copy(
                                    alpha = if (enteredPin.isNotEmpty()) 0.92f else 0.42f
                                ),
                                modifier = Modifier.size(23.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiquidGlassKeypadButton(
    enabled: Boolean,
    accentTint: Color? = null,
    onClick: () -> Unit,
    testTag: String,
    content: @Composable () -> Unit
) {
    val isDark = LocalExtendedPalette.current.isDark
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = 520f
        ),
        label = "keypad_press_scale_$testTag"
    )

    val fillColor = when {
        !enabled -> if (isDark) Color.White.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.36f)
        isPressed -> (accentTint ?: Color(0xFF3B82F6)).copy(alpha = if (isDark) 0.28f else 0.16f)
        accentTint != null -> accentTint.copy(alpha = if (isDark) 0.16f else 0.10f)
        isDark -> Color.White.copy(alpha = 0.09f)
        else -> Color.White.copy(alpha = 0.72f)
    }
    val rimBrush = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = if (enabled) 0.24f else 0.08f),
                Color.White.copy(alpha = if (enabled) 0.06f else 0.03f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = if (enabled) 0.95f else 0.55f),
                Color(0xFFCBD5E1).copy(alpha = if (enabled) 0.42f else 0.20f)
            )
        )
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(66.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.5f
            }
            .clip(CircleShape)
            .background(fillColor)
            .border(1.dp, rimBrush, CircleShape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag(testTag)
    ) {
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VaultVideoGlassCard(
    video: VideoItem,
    imageLoader: coil.ImageLoader,
    onPlay: () -> Unit,
    onRequestUnhide: () -> Unit,
    onRequestDelete: () -> Unit
) {
    val context = LocalContext.current
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark

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

    val cardShape = RoundedCornerShape(20.dp)
    val cardBg = if (isDark) {
        Color(0xFF141824).copy(alpha = 0.74f)
    } else {
        Color.White.copy(alpha = 0.76f)
    }
    val cardGloss = if (isDark) {
        Brush.verticalGradient(
            0.0f to Color.White.copy(alpha = 0.08f),
            0.5f to Color.Transparent,
            1.0f to Color(0xFF60A5FA).copy(alpha = 0.04f)
        )
    } else {
        Brush.verticalGradient(
            0.0f to Color.White.copy(alpha = 0.65f),
            0.5f to Color.White.copy(alpha = 0.10f),
            1.0f to Color(0xFFE0ECFF).copy(alpha = 0.16f)
        )
    }
    val cardBorder = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.22f),
                Color(0xFF64B5F6).copy(alpha = 0.14f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.94f),
                Color(0xFFC7DAFF).copy(alpha = 0.55f)
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .clip(cardShape)
            .background(cardBg)
            .background(cardGloss)
            .border(1.dp, cardBorder, cardShape)
            .clickable(onClick = onPlay)
            .padding(12.dp)
            .testTag("vault_video_item_${video.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .width(128.dp)
                    .height(78.dp)
                    .clip(RoundedCornerShape(14.dp))
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

                // Subtle bottom gradient scrim for duration badge contrast
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(30.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.62f))
                            )
                        )
                )

                // Subtle centered frosted play indicator
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.42f))
                        .border(0.8.dp, Color.White.copy(alpha = 0.50f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(palette.periwinkleBadgeBg.copy(alpha = 0.92f))
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
                        .background(Color.Black.copy(alpha = 0.78f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = video.formattedDuration,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum")
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = originalFileName,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.primaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
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
                    MetadataChip(text = video.formattedSize)
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
                onClick = onRequestUnhide,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.periwinkleBadgeBg,
                    contentColor = palette.periwinkleBadgeText
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
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
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = "Unhide to $originalFolderBadge",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onRequestDelete,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DangerRed.copy(alpha = if (isDark) 0.18f else 0.12f))
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
    }
}

@Composable
private fun VaultUnhideConfirmDialog(
    video: VideoItem,
    onDismiss: () -> Unit,
    onConfirmUnhide: () -> Unit
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val folderName = remember(video.relativePath, video.folderName) {
        File(video.relativePath).parentFile?.name?.takeIf { it.isNotBlank() }
            ?: video.folderName.ifBlank { "Movies" }
    }
    val auraBlue = if (isDark) Color(0xFF3B82F6) else Color(0xFF2563EB)

    VaultLiquidGlassDialogContainer(
        onDismiss = onDismiss,
        testTag = "vault_unhide_confirm_dialog"
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(auraBlue.copy(alpha = if (isDark) 0.22f else 0.12f))
            ) {
                Icon(
                    imageVector = Icons.Filled.Restore,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFF93C5FD) else auraBlue,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Unhide Video?",
                color = palette.primaryText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Restore \"${video.displayName}\" back to $folderName? It will move out of your Private Vault and reappear in your public library and gallery.",
            color = palette.secondaryText,
            fontSize = 13.5.sp,
            lineHeight = 19.sp
        )
        Spacer(modifier = Modifier.height(18.dp))
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("vault_cancel_unhide_button")
            ) {
                Text("Cancel", color = palette.secondaryText, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onConfirmUnhide,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = auraBlue,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                modifier = Modifier.testTag("vault_confirm_unhide_button")
            ) {
                Text("Unhide", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun VaultDeleteConfirmDialog(
    video: VideoItem,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark

    VaultLiquidGlassDialogContainer(
        onDismiss = onDismiss,
        testTag = "vault_delete_confirm_dialog"
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(DangerRed.copy(alpha = if (isDark) 0.22f else 0.14f))
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = null,
                    tint = DangerRed,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Delete Permanently?",
                color = DangerRed,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Permanently erase \"${video.displayName}\" from your Private Vault? This action removes the video file and cannot be undone.",
            color = palette.secondaryText,
            fontSize = 13.5.sp,
            lineHeight = 19.sp
        )
        Spacer(modifier = Modifier.height(18.dp))
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("vault_cancel_delete_button")
            ) {
                Text("Cancel", color = palette.secondaryText, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onConfirmDelete,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DangerRed,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                modifier = Modifier.testTag("vault_confirm_delete_button")
            ) {
                Text("Delete", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun VaultLiquidGlassDialogContainer(
    onDismiss: () -> Unit,
    testTag: String,
    content: @Composable () -> Unit
) {
    val isDark = LocalExtendedPalette.current.isDark
    val cardShape = RoundedCornerShape(26.dp)
    val bgColor = if (isDark) Color(0xFF141824).copy(alpha = 0.94f) else Color(0xFFF6F8FF).copy(alpha = 0.96f)
    val borderBrush = if (isDark) {
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.26f), Color(0xFF60A5FA).copy(alpha = 0.18f)))
    } else {
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.95f), Color(0xFFD6E0FA).copy(alpha = 0.80f)))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 22.dp)
                .widthIn(max = 380.dp)
                .fillMaxWidth()
                .clip(cardShape)
                .background(bgColor)
                .border(1.dp, borderBrush, cardShape)
                .padding(22.dp)
                .testTag(testTag)
        ) {
            content()
        }
    }
}

private tailrec fun Context.findHostActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findHostActivity()
        else -> null
    }
}
