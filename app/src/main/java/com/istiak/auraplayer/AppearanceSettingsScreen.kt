package com.istiak.auraplayer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.sp
import com.istiak.auraplayer.ui.components.ScrollAwareFixedHeader
import com.istiak.auraplayer.ui.components.recordFrostedBackdrop
import com.istiak.auraplayer.ui.components.rememberScrollAwareGlassLayout
import com.istiak.auraplayer.ui.theme.LocalExtendedPalette

private val AsCobaltActive = Color(0xFF2563EB)
private val AsIconBgLight = Color(0xFFEEF2FF)
private val AsPeriwinkleTint = Color(0xFFD6D9F8)
private val AsSlateHeaderLight = Color(0xFF434A6E)
private val AsDividerLight = Color(0xFFE8ECF8)

/**
 * Appearance Sub-Screen (`AppearanceSettingsScreen.kt`):
 * - Frozen `ScrollAwareFixedHeader`: `← Appearance`
 * - Theme Mode: `[System | Light | Dark]` (single-row inline segmented pills)
 * - App Disguise: `[Default (Aura) | Calculator | Notes]` (switches launcher icon & name via PackageManager aliases)
 */
@Composable
fun AppearanceSettingsScreen(
    settings: AppSettingsState,
    onUpdateThemeMode: (ThemeMode) -> Unit,
    onUpdateAppDisguiseMode: (AppDisguiseMode) -> Unit,
    onShowCapsuleToast: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark

    val listState = rememberLazyListState()
    val navBottomDp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val scrollLayout = rememberScrollAwareGlassLayout(
        listState = listState,
        defaultHeaderHeight = 64.dp,
        extraTopGap = 4.dp,
        bottomPadding = navBottomDp + 24.dp,
        horizontalPadding = 12.dp
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("appearance_settings_screen")
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
            item(key = "sec_appearance_theme_disguise") {
                AsFrostedSection(title = "| Theme & Identity") {
                    // Theme Mode: [System | Light | Dark] single-row inline segmented pills
                    AsInlineSegmentedRow(
                        icon = Icons.Filled.Palette,
                        title = "Theme Mode",
                        options = ThemeMode.entries.map { it.label },
                        selectedIndex = ThemeMode.entries.indexOf(settings.themeMode).coerceAtLeast(0),
                        onSelectIndex = { idx ->
                            onUpdateThemeMode(ThemeMode.entries[idx])
                        },
                        testTagPrefix = "theme_mode_option"
                    )

                    HorizontalDivider(
                        thickness = 1.dp,
                        color = if (isDark) Color.White.copy(alpha = 0.08f) else AsDividerLight,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    // App Disguise: [Default (Aura) | Calculator | Notes]
                    AsStackedSegmentedRow(
                        icon = Icons.Filled.Calculate,
                        title = "App Disguise",
                        subtitle = "Switch launcher icon and app name on your home screen",
                        options = AppDisguiseMode.entries.map { it.label },
                        selectedIndex = AppDisguiseMode.entries.indexOf(settings.appDisguiseMode).coerceAtLeast(0),
                        onSelectIndex = { idx ->
                            val chosen = AppDisguiseMode.entries[idx]
                            PrivateVaultManager.applyAppDisguise(context, chosen)
                            onUpdateAppDisguiseMode(chosen)
                            onShowCapsuleToast("App Disguise set to ${chosen.label}")
                        },
                        testTagPrefix = "app_disguise_option"
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
                        .testTag("appearance_settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = palette.brandTitleColor
                    )
                }
                Text(
                    text = "Appearance",
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.brandTitleColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AsFrostedSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val headerColor = if (isDark) Color(0xFFB7C5E8) else AsSlateHeaderLight
    val cardShape = RoundedCornerShape(18.dp)
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
private fun AsIconBadge(icon: ImageVector) {
    val isDark = LocalExtendedPalette.current.isDark
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(if (isDark) AsCobaltActive.copy(alpha = 0.18f) else AsIconBgLight),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDark) Color(0xFF60A5FA) else AsCobaltActive,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun AsInlineSegmentedRow(
    icon: ImageVector,
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    testTagPrefix: String
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val inactiveBg = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F4FC)
    val inactiveBorder = if (isDark) Color.White.copy(alpha = 0.12f) else AsDividerLight

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        AsIconBadge(icon = icon)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            color = palette.primaryText,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEachIndexed { idx, label ->
                val isSelected = idx == selectedIndex
                val chipShape = RoundedCornerShape(999.dp)
                Box(
                    modifier = Modifier
                        .clip(chipShape)
                        .background(if (isSelected) AsCobaltActive else inactiveBg)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) AsPeriwinkleTint.copy(alpha = 0.7f) else inactiveBorder,
                            shape = chipShape
                        )
                        .clickable { onSelectIndex(idx) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("${testTagPrefix}_$idx"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else palette.primaryText.copy(alpha = 0.86f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun AsStackedSegmentedRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    options: List<String>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    testTagPrefix: String
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val inactiveBg = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F4FC)
    val inactiveBorder = if (isDark) Color.White.copy(alpha = 0.12f) else AsDividerLight

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsIconBadge(icon = icon)
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
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 44.dp)
        ) {
            options.forEachIndexed { idx, label ->
                val isSelected = idx == selectedIndex
                val chipShape = RoundedCornerShape(999.dp)
                Box(
                    modifier = Modifier
                        .clip(chipShape)
                        .background(if (isSelected) AsCobaltActive else inactiveBg)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) AsPeriwinkleTint.copy(alpha = 0.7f) else inactiveBorder,
                            shape = chipShape
                        )
                        .clickable { onSelectIndex(idx) }
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                        .testTag("${testTagPrefix}_$idx"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else palette.primaryText.copy(alpha = 0.86f),
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        softWrap = false,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
