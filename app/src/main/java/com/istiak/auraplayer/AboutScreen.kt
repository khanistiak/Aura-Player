package com.istiak.auraplayer

import android.app.ActivityManager
import android.content.Context
import android.media.MediaCodecList
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.istiak.auraplayer.ui.components.ScrollAwareFixedHeader
import com.istiak.auraplayer.ui.components.recordFrostedBackdrop
import com.istiak.auraplayer.ui.components.rememberScrollAwareGlassLayout
import com.istiak.auraplayer.ui.theme.LocalExtendedPalette
import kotlinx.coroutines.launch
import java.util.Locale

private val AboutCobalt = Color(0xFF2563EB)
private val AboutSlateHeaderLight = Color(0xFF434A6E)
private val AboutDividerLight = Color(0xFFE8ECF8)

private enum class AboutModalType {
    LIBRARIES,
    PRIVACY_POLICY
}

/**
 * About Page with Developer Credit & System/Device Info (`AboutScreen.kt`):
 * - Frozen `ScrollAwareFixedHeader`: `← About`
 * - Hero Brand Card (`24.dp` corners): AuraPlayer emblem, `Aura Player`, dynamic version badge,
 *   highlighted pill badge `By Khan Istiak Shahabuddin`, and rounded action buttons
 *   (`Libraries`, `Privacy Policy`, `Check for Updates Now`).
 * - Device Info & `| System` Card reading live Android / Media3 hardware properties.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onShowCapsuleToast: (String) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark

    val updateManager = remember(context) { AppUpdateManager(context.applicationContext) }
    val installedVersion = remember(context, updateManager) { updateManager.getInstalledVersionName(context) }
    var isCheckingForUpdates by remember { mutableStateOf(false) }
    var availableUpdate by remember { mutableStateOf<ReleaseUpdateInfo?>(null) }
    var activeModal by remember { mutableStateOf<AboutModalType?>(null) }

    val systemSpecs = remember(context, installedVersion) {
        collectLiveSystemSpecs(context, installedVersion)
    }

    val listState = rememberLazyListState()
    val navBottomDp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val scrollLayout = rememberScrollAwareGlassLayout(
        listState = listState,
        defaultHeaderHeight = 58.dp,
        extraTopGap = 2.dp,
        bottomPadding = navBottomDp + 16.dp,
        horizontalPadding = 12.dp
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("about_screen")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxSize()
                .recordFrostedBackdrop(
                    backdropState = scrollLayout.backdropState,
                    scrollableState = listState,
                    contentLayerOverride = scrollLayout.contentGraphicsLayer
                )
                .padding(scrollLayout.contentPadding)
        ) {
            // Hero Brand Card
            val heroShape = RoundedCornerShape(24.dp)
            val heroBrush = if (isDark) {
                Brush.linearGradient(
                    listOf(
                        Color(0xFF1A264A),
                        Color(0xFF281E49),
                        Color(0xFF161D33)
                    )
                )
            } else {
                Brush.linearGradient(
                    listOf(
                        Color(0xFFDCEEFF),
                        Color(0xFFECE2FF),
                        Color(0xFFF6F7FF)
                    )
                )
            }
            val heroBorder = if (isDark) {
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.24f), Color(0xFF60A5FA).copy(alpha = 0.22f)))
            } else {
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.96f), Color(0xFFD6D9F8).copy(alpha = 0.85f)))
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 8.dp,
                        shape = heroShape,
                        ambientColor = Color(0xFF2563EB).copy(alpha = if (isDark) 0.35f else 0.12f),
                        spotColor = Color(0xFF434A6E).copy(alpha = if (isDark) 0.35f else 0.14f)
                    )
                    .clip(heroShape)
                    .background(heroBrush)
                    .border(1.dp, heroBorder, heroShape)
                    .padding(horizontal = 20.dp, vertical = 22.dp)
            ) {
                // Frosted Liquid Glass Brand Emblem (76.dp container with soft ambient glow)
                val logoContainerShape = RoundedCornerShape(22.dp)
                val logoGlassBg = if (isDark) {
                    Color(0xFF10172C).copy(alpha = 0.78f)
                } else {
                    Color.White.copy(alpha = 0.80f)
                }
                val logoGlassBorder = if (isDark) {
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.24f),
                            Color(0xFF38BDF8).copy(alpha = 0.22f)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.96f),
                            Color(0xFFD6E4FF).copy(alpha = 0.85f)
                        )
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(76.dp)
                        .shadow(
                            elevation = 12.dp,
                            shape = logoContainerShape,
                            ambientColor = Color(0xFF2563EB).copy(alpha = if (isDark) 0.42f else 0.22f),
                            spotColor = Color(0xFF0EA5E9).copy(alpha = if (isDark) 0.38f else 0.18f)
                        )
                        .clip(logoContainerShape)
                        .background(logoGlassBg)
                        .border(1.dp, logoGlassBorder, logoContainerShape)
                        .padding(8.dp)
                        .testTag("about_brand_emblem")
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.aura_logo),
                        contentDescription = "Aura Player Logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Aura Player",
                    color = palette.primaryText,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "v${installedVersion} release",
                    color = palette.secondaryText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Two-line centered developer credit directly below app version
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.testTag("about_developer_badge")
                ) {
                    Text(
                        text = "Developer: Khan Istiak Shahabuddin",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Made with ❤️ in Bangladesh",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Libraries & Privacy Policy pill row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AboutActionPillButton(
                        icon = Icons.Filled.Code,
                        label = "Libraries",
                        onClick = { activeModal = AboutModalType.LIBRARIES },
                        testTag = "about_libraries_button"
                    )
                    AboutActionPillButton(
                        icon = Icons.Filled.PrivacyTip,
                        label = "Privacy Policy",
                        onClick = { activeModal = AboutModalType.PRIVACY_POLICY },
                        testTag = "about_privacy_button"
                    )
                }
            }

            // Device & App Info Card (`| System & App`) — stretches cleanly to fill remaining viewport height
            val cardShape = RoundedCornerShape(18.dp)
            val cardBg = if (isDark) Color(0xFF161B2E).copy(alpha = 0.80f) else Color.White.copy(alpha = 0.86f)
            val borderBrush = if (isDark) {
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color(0xFF60A5FA).copy(alpha = 0.14f)))
            } else {
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.94f), Color(0xFFD6D9F8).copy(alpha = 0.72f)))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Text(
                    text = "| System & App",
                    color = if (isDark) Color(0xFFB7C5E8) else AboutSlateHeaderLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp,
                    modifier = Modifier.padding(start = 6.dp, top = 2.dp, bottom = 6.dp)
                )
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .shadow(
                            elevation = 4.dp,
                            shape = cardShape,
                            ambientColor = Color(0xFF434A6E).copy(alpha = if (isDark) 0.28f else 0.06f),
                            spotColor = Color(0xFF2563EB).copy(alpha = if (isDark) 0.28f else 0.08f)
                        )
                        .clip(cardShape)
                        .background(cardBg)
                        .border(1.dp, borderBrush, cardShape)
                        .padding(vertical = 4.dp)
                ) {
                    systemSpecs.forEachIndexed { index, (label, value) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 14.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(0.44f)
                            )
                            Text(
                                text = value,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.End,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(0.56f)
                            )
                        }
                        if (index < systemSpecs.lastIndex) {
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.14f),
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )
                        }
                    }
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
                        .testTag("about_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = palette.brandTitleColor
                    )
                }
                Text(
                    text = "About",
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.brandTitleColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    activeModal?.let { modal ->
        AboutDetailDialog(
            modal = modal,
            onDismiss = { activeModal = null }
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
private fun AboutActionPillButton(
    icon: ImageVector,
    label: String,
    isPrimary: Boolean = false,
    isLoading: Boolean = false,
    onClick: () -> Unit,
    testTag: String
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val shape = RoundedCornerShape(999.dp)
    val bg = when {
        isPrimary -> AboutCobalt
        isDark -> Color.White.copy(alpha = 0.12f)
        else -> Color.White.copy(alpha = 0.88f)
    }
    val contentColor = when {
        isPrimary -> Color.White
        isDark -> Color(0xFFE2E8F0)
        else -> AboutSlateHeaderLight
    }
    val borderColor = when {
        isPrimary -> AboutCobalt
        isDark -> Color.White.copy(alpha = 0.22f)
        else -> Color(0xFFD6D9F8)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, borderColor, shape)
            .clickable(enabled = !isLoading, onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp)
            .testTag(testTag)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = contentColor,
                strokeWidth = 2.dp,
                modifier = Modifier
                    .size(15.dp)
                    .testTag("about_update_loading_spinner")
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(15.dp)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = contentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun AboutDetailDialog(
    modal: AboutModalType,
    onDismiss: () -> Unit
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val cardShape = RoundedCornerShape(24.dp)
    val bgColor = if (isDark) Color(0xFF141A2E) else Color(0xFFF6F7FF)
    val bannerBg = if (isDark) Color(0xFF212B4A) else Color(0xFFE3E6FA)

    val title = when (modal) {
        AboutModalType.LIBRARIES -> "Open Source Libraries"
        AboutModalType.PRIVACY_POLICY -> "Privacy Policy"
    }

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
                .border(1.dp, if (isDark) Color.White.copy(alpha = 0.2f) else Color.White, cardShape)
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
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                when (modal) {
                    AboutModalType.LIBRARIES -> {
                        listOf(
                            "AndroidX Media3 ExoPlayer 1.5.1" to "Apache-2.0 License",
                            "Jellyfin Media3 FFmpeg Decoder 1.5.0" to "GPLv3 / LGPL",
                            "Jetpack Compose & Material 3" to "Apache-2.0 License",
                            "Coil Image & Video Loader 2.7.0" to "Apache-2.0 License",
                            "AndroidX DataStore & Biometric" to "Apache-2.0 License",
                            "Kotlin Coroutines" to "Apache-2.0 License"
                        ).forEach { (lib, lic) ->
                            Column {
                                Text(lib, color = palette.primaryText, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                                Text(lic, color = palette.secondaryText, fontSize = 12.sp)
                            }
                        }
                    }
                    AboutModalType.PRIVACY_POLICY -> {
                        Text(
                            text = "Aura Player operates 100% locally on your device. No personal watch history, filenames, or device identifiers are transmitted to external servers.\n\nPrivate Vault items are stored strictly inside /storage/emulated/0/AuraPlayer/.private/ with .nomedia isolation and never leave your device.",
                            color = palette.secondaryText,
                            fontSize = 13.5.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AboutCobalt,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

private fun collectLiveSystemSpecs(
    context: Context,
    @Suppress("UNUSED_PARAMETER") installedVersion: String
): List<Pair<String, String>> {
    val cpuAbi = Build.SUPPORTED_ABIS?.firstOrNull() ?: "arm64-v8a"
    val hwVideoDecoderCount = countHardwareVideoDecoders()

    return listOf(
        "Package Name" to context.packageName.ifBlank { "com.istiak.auraplayer" },
        "License" to "Open Source (Ad-Free)",
        "Build Type" to "Release (Signed)",
        "Android version" to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        "Device brand" to Build.BRAND.replaceFirstChar { it.uppercase(Locale.US) },
        "Device manufacturer" to Build.MANUFACTURER,
        "Device model" to "${Build.MODEL} (${Build.DEVICE})",
        "Playback Engine" to "Media3 ExoPlayer 1.5.1",
        "Audio Decoder" to "Bundled FFmpeg + MediaCodec",
        "Hardware Video Decoders" to "$hwVideoDecoderCount HW Decoders",
        "CPU ABI" to cpuAbi
    )
}

internal fun countHardwareVideoDecoders(): Int {
    return runCatching {
        MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos.count { info ->
            if (info.isEncoder) return@count false
            val isVideo = info.supportedTypes.any { it.startsWith("video/", ignoreCase = true) }
            if (!isVideo) return@count false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                info.isHardwareAccelerated
            } else {
                !info.name.startsWith("OMX.google.", ignoreCase = true) &&
                    !info.name.startsWith("c2.android.", ignoreCase = true)
            }
        }
    }.getOrDefault(0)
}
