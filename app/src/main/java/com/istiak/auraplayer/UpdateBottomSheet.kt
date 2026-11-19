package com.istiak.auraplayer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.istiak.auraplayer.ui.theme.LocalExtendedPalette

/**
 * Liquid Glass Update Popup Dialog (`LiquidGlassUpdateDialog`):
 * - Centered frosted Liquid Glass dialog (`RoundedCornerShape(28.dp)`) with luminous vertical
 *   specular edge reflection and subtle inner gloss sheen (zero dark drop shadow halo).
 * - Displays the Aura Player app icon, `"Update Available"` title, version pill badge (e.g. `v2.6.5`),
 *   scrollable formatted release notes from GitHub Releases `body`, and APK size chip.
 * - Two action buttons:
 *   - `"Later"`: persists `dismissed_update_version` in DataStore and closes the popup.
 *   - `"Download Update"`: launches the existing GitHub APK download/install flow via [AppUpdateManager].
 */
@Composable
fun LiquidGlassUpdateDialog(
    release: ReleaseUpdateInfo,
    updateManager: AppUpdateManager,
    onLater: () -> Unit,
    onDownloadStarted: () -> Unit = {}
) {
    val context = LocalContext.current
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark

    val dialogShape = RoundedCornerShape(28.dp)
    val glassFill = if (isDark) {
        Color(0xFF141929).copy(alpha = 0.94f)
    } else {
        Color(0xFFF6F9FF).copy(alpha = 0.96f)
    }
    val innerGloss = if (isDark) {
        Brush.verticalGradient(
            0.0f to Color.White.copy(alpha = 0.10f),
            0.42f to Color.Transparent,
            1.0f to Color(0xFF60A5FA).copy(alpha = 0.06f)
        )
    } else {
        Brush.verticalGradient(
            0.0f to Color.White.copy(alpha = 0.72f),
            0.45f to Color.White.copy(alpha = 0.14f),
            1.0f to Color(0xFFDBEAFE).copy(alpha = 0.24f)
        )
    }
    val specularRim = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.28f),
                Color.White.copy(alpha = 0.10f),
                Color(0xFF64B5F6).copy(alpha = 0.20f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.96f),
                Color(0xFFD6E3FF).copy(alpha = 0.70f),
                Color(0xFF93C5FD).copy(alpha = 0.45f)
            )
        )
    }

    val auraBlue = if (isDark) Color(0xFF3B82F6) else Color(0xFF2563EB)
    val versionPillBg = if (release.isCritical) {
        Color(0xFFEF4444).copy(alpha = if (isDark) 0.22f else 0.12f)
    } else {
        auraBlue.copy(alpha = if (isDark) 0.22f else 0.12f)
    }
    val versionPillText = if (release.isCritical) {
        if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626)
    } else {
        if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8)
    }

    val notesCardBg = if (isDark) {
        Color(0xFF1C2338).copy(alpha = 0.84f)
    } else {
        Color.White.copy(alpha = 0.82f)
    }
    val notesCardBorder = if (isDark) {
        Color.White.copy(alpha = 0.10f)
    } else {
        Color(0xFFD8E2FB)
    }
    val laterButtonBg = if (isDark) {
        Color.White.copy(alpha = 0.09f)
    } else {
        Color(0xFFE5ECFA)
    }
    val laterButtonText = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155)
    val mutedSlate = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Dialog(
        onDismissRequest = onLater,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .clip(dialogShape)
                .background(glassFill)
                .background(innerGloss)
                .border(1.1.dp, specularRim, dialogShape)
                .testTag("update_bottom_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 22.dp)
            ) {
                // Header: App Icon + "Update Available" Title + Version Pill Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(
                                if (isDark) Color(0xFF1E2942) else Color.White.copy(alpha = 0.92f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isDark) Color.White.copy(alpha = 0.16f) else Color(0xFFD8E4FF),
                                shape = RoundedCornerShape(15.dp)
                            )
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.aura_logo),
                            contentDescription = "Aura Player Icon",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Update Available",
                            color = palette.primaryText,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("update_sheet_title")
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(versionPillBg)
                                    .border(
                                        width = 0.8.dp,
                                        color = versionPillText.copy(alpha = 0.30f),
                                        shape = CircleShape
                                    )
                                    .padding(horizontal = 9.dp, vertical = 3.dp)
                                    .testTag("update_sheet_status_badge")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.NewReleases,
                                    contentDescription = null,
                                    tint = versionPillText,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = release.displayTag,
                                    color = versionPillText,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = release.publishedMonthYear,
                                color = mutedSlate,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("update_sheet_date")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (release.isCritical) {
                        "A critical Aura Player release is ready with important playback, decoder, and security fixes."
                    } else {
                        "A new Aura Player release is ready on GitHub with improvements and bug fixes."
                    },
                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                    fontSize = 13.sp,
                    lineHeight = 18.5.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Release Notes Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(notesCardBg)
                        .border(1.dp, notesCardBorder, RoundedCornerShape(18.dp))
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState())
                        .testTag("update_sheet_changelog_card")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "WHAT'S NEW",
                            color = mutedSlate,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (isDark) Color.White.copy(alpha = 0.08f)
                                    else Color(0xFFE8EEFC)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                .testTag("update_sheet_apk_size")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FileDownload,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "APK • ${release.formattedSize}",
                                color = if (isDark) Color(0xFFDBEAFE) else Color(0xFF1E40AF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(
                        thickness = 0.8.dp,
                        color = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        release.changelogLines.forEach { line ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = if (release.isCritical) Color(0xFFEF4444) else auraBlue,
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(9.dp))
                                Text(
                                    text = line,
                                    color = palette.primaryText,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Row: "Later" + "Download Update"
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onLater,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = laterButtonBg,
                            contentColor = laterButtonText
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        modifier = Modifier
                            .weight(0.85f)
                            .height(48.dp)
                            .testTag("update_dialog_later_button")
                    ) {
                        Text(
                            text = "Later",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }

                    Button(
                        onClick = {
                            updateManager.openApkDownloadUrl(context, release.apkDownloadUrl)
                            onDownloadStarted()
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (release.isCritical) Color(0xFFEF4444) else auraBlue,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        modifier = Modifier
                            .weight(1.35f)
                            .height(48.dp)
                            .testTag("update_dialog_download_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            text = "Download Update",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compatibility wrapper preserving existing call sites (`SettingsScreen.kt`, `AboutScreen.kt`)
 * while rendering the unified [LiquidGlassUpdateDialog].
 */
@Composable
fun UpdateBottomSheet(
    release: ReleaseUpdateInfo,
    updateManager: AppUpdateManager,
    onDismiss: () -> Unit
) {
    LiquidGlassUpdateDialog(
        release = release,
        updateManager = updateManager,
        onLater = onDismiss
    )
}
