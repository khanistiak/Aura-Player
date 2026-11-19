package com.istiak.auraplayer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.istiak.auraplayer.ui.components.ScrollAwareFixedHeader
import com.istiak.auraplayer.ui.components.recordFrostedBackdrop
import com.istiak.auraplayer.ui.components.rememberScrollAwareGlassLayout
import com.istiak.auraplayer.ui.theme.LocalExtendedPalette
import kotlinx.coroutines.launch

private data class SettingsCategoryEntry(
    val key: String,
    val title: String,
    val icon: ImageVector,
    val testTag: String,
    val isLoading: Boolean = false,
    val onClick: () -> Unit
)

/**
 * Category-Only Root Settings Hub (`SettingsScreen.kt`):
 * - Frozen `ScrollAwareFixedHeader`: `← Settings`
 * - Single elevated frosted card (`20.dp` radius, hairline dividers, leading tinted squircle icon badge,
 *   single-line title, right chevron `>`) with top-level categories including `Liquid glass`, `About`,
 *   and `Check for Updates` directly below `About`.
 */
@Composable
fun SettingsScreen(
    settings: AppSettingsState,
    onOpenAppearanceSettings: () -> Unit,
    onOpenLiquidGlassSettings: () -> Unit = {},
    onOpenFolderSettings: () -> Unit,
    onOpenPlayerSettings: () -> Unit,
    onOpenGestureSettings: () -> Unit,
    onOpenPrivateFolderSettings: () -> Unit,
    onOpenCodecDiagnostics: () -> Unit,
    onOpenAbout: () -> Unit,
    onBack: () -> Unit,
    onShowCapsuleToast: ((String) -> Unit)? = null
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark

    val updateManager = remember(context) { AppUpdateManager(context.applicationContext) }
    var isCheckingForUpdates by remember { mutableStateOf(false) }
    var availableUpdate by remember { mutableStateOf<ReleaseUpdateInfo?>(null) }
    var localToastEvent by remember { mutableStateOf<VaultToastEvent?>(null) }

    fun triggerCapsuleToast(message: String) {
        if (onShowCapsuleToast != null) {
            onShowCapsuleToast(message)
        } else {
            localToastEvent = VaultToastEvent(message = message, type = VaultToastType.INFO)
        }
    }

    val categories = buildList {
        add(
            SettingsCategoryEntry(
                key = "appearance",
                title = "Appearance",
                icon = Icons.Filled.Palette,
                testTag = "settings_category_appearance",
                onClick = onOpenAppearanceSettings
            )
        )
        add(
            SettingsCategoryEntry(
                key = "liquid_glass",
                title = "Liquid glass",
                icon = Icons.Filled.AutoAwesome,
                testTag = "settings_category_liquid_glass",
                onClick = onOpenLiquidGlassSettings
            )
        )
        add(
            SettingsCategoryEntry(
                key = "folders",
                title = "Folders & Library",
                icon = Icons.Filled.FolderSpecial,
                testTag = "settings_category_folders",
                onClick = onOpenFolderSettings
            )
        )
        add(
            SettingsCategoryEntry(
                key = "player",
                title = "Player & Decoder",
                icon = Icons.Filled.PlayCircleOutline,
                testTag = "settings_category_player",
                onClick = onOpenPlayerSettings
            )
        )
        add(
            SettingsCategoryEntry(
                key = "gestures",
                title = "Gestures & Controls",
                icon = Icons.Filled.Swipe,
                testTag = "settings_category_gestures",
                onClick = onOpenGestureSettings
            )
        )
        if (!settings.hideSecureFolderSettings) {
            add(
                SettingsCategoryEntry(
                    key = "private_folder",
                    title = "Private Folder",
                    icon = Icons.Filled.Lock,
                    testTag = "settings_category_private_folder",
                    onClick = onOpenPrivateFolderSettings
                )
            )
        }
        add(
            SettingsCategoryEntry(
                key = "codecs",
                title = "Hardware & Software Codecs",
                icon = Icons.Filled.DeveloperBoard,
                testTag = "settings_category_codecs",
                onClick = onOpenCodecDiagnostics
            )
        )
        add(
            SettingsCategoryEntry(
                key = "about",
                title = "About",
                icon = Icons.Filled.Info,
                testTag = "settings_category_about",
                onClick = onOpenAbout
            )
        )
        add(
            SettingsCategoryEntry(
                key = "check_for_updates",
                title = "Check for Updates",
                icon = Icons.Rounded.SystemUpdate,
                testTag = "settings_category_check_updates",
                isLoading = isCheckingForUpdates,
                onClick = {
                    if (!isCheckingForUpdates) {
                        isCheckingForUpdates = true
                        coroutineScope.launch {
                            val result = updateManager.checkForUpdates()
                            isCheckingForUpdates = false
                            when (result) {
                                is UpdateCheckResult.UpdateAvailable -> {
                                    availableUpdate = result.info
                                }
                                is UpdateCheckResult.UpToDate -> {
                                    triggerCapsuleToast("You're on the latest version")
                                }
                                is UpdateCheckResult.Error -> {
                                    triggerCapsuleToast("You're on the latest version")
                                }
                            }
                        }
                    }
                }
            )
        )
    }

    val listState = rememberLazyListState()
    val navBottomDp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val scrollLayout = rememberScrollAwareGlassLayout(
        listState = listState,
        defaultHeaderHeight = 64.dp,
        extraTopGap = 6.dp,
        bottomPadding = navBottomDp + 24.dp,
        horizontalPadding = 12.dp
    )

    val primary = MaterialTheme.colorScheme.primary
    val cardShape = RoundedCornerShape(20.dp)
    val cardBg = if (isDark) {
        palette.sheetBackground.copy(alpha = 0.84f)
    } else {
        Color.White.copy(alpha = 0.86f)
    }
    val borderBrush = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.22f),
                primary.copy(alpha = 0.16f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.94f),
                palette.outlineBorder.copy(alpha = 0.72f)
            )
        )
    }
    val dividerColor = if (isDark) Color.White.copy(alpha = 0.08f) else palette.outlineBorder.copy(alpha = 0.70f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen")
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
            item(key = "settings_categories_card") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 6.dp,
                            shape = cardShape,
                            ambientColor = primary.copy(alpha = if (isDark) 0.28f else 0.07f),
                            spotColor = primary.copy(alpha = if (isDark) 0.28f else 0.09f)
                        )
                        .clip(cardShape)
                        .background(cardBg)
                        .border(1.dp, borderBrush, cardShape)
                        .padding(vertical = 4.dp)
                ) {
                    categories.forEachIndexed { index, entry ->
                        SettingsCategoryRow(
                            icon = entry.icon,
                            title = entry.title,
                            isLoading = entry.isLoading,
                            onClick = entry.onClick,
                            testTag = entry.testTag
                        )
                        if (index < categories.lastIndex) {
                            HorizontalDivider(
                                thickness = 1.dp,
                                color = dividerColor,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )
                        }
                    }
                }
            }
        }

        // Frozen Liquid Glass Top Header
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
                        .testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = palette.brandTitleColor
                    )
                }
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.brandTitleColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        VaultNotificationPill(
            event = localToastEvent,
            onDismiss = { localToastEvent = null },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    availableUpdate?.let { releaseInfo ->
        UpdateBottomSheet(
            release = releaseInfo,
            updateManager = updateManager,
            onDismiss = {
                updateManager.cancelUpdateNotification()
                availableUpdate = null
            }
        )
    }
}

@Composable
private fun SettingsCategoryRow(
    icon: ImageVector,
    title: String,
    isLoading: Boolean = false,
    onClick: () -> Unit,
    testTag: String
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val primary = MaterialTheme.colorScheme.primary
    val iconContainerBg = primary.copy(alpha = if (isDark) 0.16f else 0.12f)
    val iconTint = primary

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clickable(enabled = !isLoading, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconContainerBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = palette.primaryText,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        if (isLoading) {
            CircularProgressIndicator(
                color = primary,
                strokeWidth = 2.dp,
                modifier = Modifier
                    .size(18.dp)
                    .testTag("settings_update_loading_spinner")
            )
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = palette.secondaryText,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
