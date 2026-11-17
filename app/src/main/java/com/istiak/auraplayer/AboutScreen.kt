package com.istiak.auraplayer

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
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
import androidx.compose.material.icons.filled.PlayCircleFilled
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
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
 * - Hero Brand Card (`24.dp` corners): AuraPlayer emblem, `Aura Player`, `v2.5.0 release`,
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
    var isCheckingForUpdates by remember { mutableStateOf(false) }
    var availableUpdate by remember { mutableStateOf<ReleaseUpdateInfo?>(null) }
    var activeModal by remember { mutableStateOf<AboutModalType?>(null) }

    val systemSpecs = remember(context) { collectLiveSystemSpecs(context) }
    val appIconBitmap = remember(context) { loadLauncherImageBitmap(context) }

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
            .testTag("about_screen")
    ) {
        LazyColumn(
            state = listState,
            contentPadding = scrollLayout.contentPadding,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .recordFrostedBackdrop(
                    backdropState = scrollLayout.backdropState,
                    scrollableState = listState,
                    contentLayerOverride = scrollLayout.contentGraphicsLayer
                )
        ) {
            // Hero Brand Card
            item(key = "hero_brand_card") {
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
                    // Launcher emblem
                    Box(
                        modifier = Modifier
                            .size(74.dp)
                            .shadow(10.dp, RoundedCornerShape(20.dp))
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isDark) Color(0xFF101629) else Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        if (appIconBitmap != null) {
                            Image(
                                bitmap = appIconBitmap,
                                contentDescription = "Aura Player Emblem",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.PlayCircleFilled,
                                contentDescription = "Aura Player Emblem",
                                tint = AboutCobalt,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Aura Player",
                        color = palette.primaryText,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "v2.5.0 release",
                        color = palette.secondaryText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Highlighted Developer Credit Pill Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (isDark) Color(0xFF283454) else Color(0xFFE3E6FA))
                            .border(
                                1.dp,
                                AboutCobalt.copy(alpha = if (isDark) 0.50f else 0.30f),
                                RoundedCornerShape(999.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("about_developer_badge")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF93C5FD) else AboutCobalt,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "By Khan Istiak Shahabuddin",
                            color = if (isDark) Color(0xFFDBEAFE) else AboutCobalt,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
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
                        AboutActionPillButton(
                            icon = Icons.Filled.SystemUpdate,
                            label = if (isCheckingForUpdates) "Checking..." else "Check for Updates Now",
                            isPrimary = true,
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
                                                onShowCapsuleToast("You're on the latest version")
                                            }
                                            is UpdateCheckResult.Error -> {
                                                onShowCapsuleToast("You're on the latest version")
                                            }
                                        }
                                    }
                                }
                            },
                            testTag = "about_check_updates_button"
                        )
                    }
                }
            }

            // Device Info & | System Card
            item(key = "system_info_card") {
                val cardShape = RoundedCornerShape(18.dp)
                val cardBg = if (isDark) Color(0xFF161B2E).copy(alpha = 0.80f) else Color.White.copy(alpha = 0.86f)
                val borderBrush = if (isDark) {
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color(0xFF60A5FA).copy(alpha = 0.14f)))
                } else {
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.94f), Color(0xFFD6D9F8).copy(alpha = 0.72f)))
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "| System",
                        color = if (isDark) Color(0xFFB7C5E8) else AboutSlateHeaderLight,
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
                            .padding(vertical = 6.dp)
                    ) {
                        systemSpecs.forEachIndexed { index, (label, value) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = palette.secondaryText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(0.46f)
                                )
                                Text(
                                    text = value,
                                    color = palette.primaryText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.End,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(0.54f)
                                )
                            }
                            if (index < systemSpecs.lastIndex) {
                                HorizontalDivider(
                                    thickness = 1.dp,
                                    color = if (isDark) Color.White.copy(alpha = 0.08f) else AboutDividerLight,
                                    modifier = Modifier.padding(horizontal = 14.dp)
                                )
                            }
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
            onDismiss = { availableUpdate = null }
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

private fun loadLauncherImageBitmap(context: Context): androidx.compose.ui.graphics.ImageBitmap? {
    return runCatching {
        val drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher) ?: return@runCatching null
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return@runCatching drawable.bitmap.asImageBitmap()
        }
        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 144
        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 144
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bmp.asImageBitmap()
    }.getOrNull()
}

private fun collectLiveSystemSpecs(context: Context): List<Pair<String, String>> {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    val memInfo = ActivityManager.MemoryInfo()
    val ramFormatted = if (am != null) {
        am.getMemoryInfo(memInfo)
        val totalGb = memInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
        if (totalGb > 0.0) String.format(Locale.US, "%.2f GB", totalGb) else "Unknown"
    } else {
        "Unknown"
    }
    val glEsVersion = runCatching {
        am?.deviceConfigurationInfo?.glEsVersion?.takeIf { it.isNotBlank() }
    }.getOrNull() ?: "3.2"

    val cpuAbi = Build.SUPPORTED_ABIS?.firstOrNull() ?: "arm64-v8a"
    val cpuCores = Runtime.getRuntime().availableProcessors()
    val kernel = System.getProperty("os.version") ?: "Linux"

    val hwVideoDecoderCount = countHardwareVideoDecoders()

    return listOf(
        "App version" to "v2.5.0 release",
        "Android version" to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        "Device brand" to Build.BRAND.replaceFirstChar { it.uppercase(Locale.US) },
        "Device manufacturer" to Build.MANUFACTURER,
        "Device model" to "${Build.MODEL} (${Build.DEVICE})",
        "Playback Engine" to "AndroidX Media3 ExoPlayer 1.5.1",
        "Audio Decoder" to "Bundled FFmpeg + MediaCodec",
        "Hardware Video Codecs" to "$hwVideoDecoderCount HW Decoders",
        "CPU ABI" to cpuAbi,
        "CPU Cores" to "$cpuCores",
        "RAM" to ramFormatted,
        "OpenGL ES" to glEsVersion,
        "Board" to Build.BOARD,
        "Kernel" to kernel
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
