package com.istiak.auraplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.istiak.auraplayer.ui.theme.LocalExtendedPalette
import java.util.Locale

/**
 * Material 3 Update Bottom Sheet (`UpdateBottomSheet.kt`):
 * - `28.dp` top corners, translucent obsidian-indigo glass `Color(0xFF141824)` in Dark Mode /
 *   frosted pastel white `Color(0xFFF6F8FF)` in Light Mode, with a `1.dp` top specular border.
 * - Header Row:
 *   - Left column: Bold uppercase title `AURA PLAYER V${release.version} UPDATE` (`17.sp`, `FontWeight.ExtraBold`)
 *     with release month/year subtitle below (`12.sp`, muted slate).
 *   - Right badge: Rounded pill badge (`RoundedCornerShape(8.dp)`) displaying `CRITICAL` in crimson `#EF4444`
 *     (if release body contains `[critical]` or `critical`) or `NEW` in royal blue `#2563EB`.
 * - Changelog Summary & Card:
 *   - Concise intro paragraph followed by a rounded inner container (`RoundedCornerShape(16.dp)`)
 *     listing bulleted release note highlights from `release.body` and APK size indicator.
 * - Action Buttons:
 *   - Full-width primary button (`height = 50.dp`, `RoundedCornerShape(14.dp)`):
 *     `Download v${release.version} Update (APK)` opening `release.apkDownloadUrl` via Custom Tabs / browser.
 *   - Full-width secondary `Close` button (`height = 46.dp`, `RoundedCornerShape(14.dp)`) underneath.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateBottomSheet(
    release: ReleaseUpdateInfo,
    updateManager: AppUpdateManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val sheetContainerColor = if (isDark) Color(0xFF141824) else Color(0xFFF6F8FF)
    val topSpecularBorder = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.22f),
                Color.White.copy(alpha = 0.04f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.96f),
                Color(0xFFD6E0FA).copy(alpha = 0.55f)
            )
        )
    }

    val badgeColor = if (release.isCritical) Color(0xFFEF4444) else Color(0xFF2563EB)
    val badgeLabel = if (release.isCritical) "CRITICAL" else "NEW"

    val primaryButtonColor = when {
        isDark && release.isCritical -> Color(0xFFEF4444)
        isDark -> Color(0xFF3B82F6)
        else -> Color(0xFF2563EB)
    }

    val closeButtonBg = if (isDark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color(0xFFE5EAF8)
    }
    val closeButtonText = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155)
    val mutedSlate = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    val innerCardBg = if (isDark) {
        Color(0xFF1B2133)
    } else {
        Color.White.copy(alpha = 0.88f)
    }
    val innerCardBorder = if (isDark) {
        Color.White.copy(alpha = 0.10f)
    } else {
        Color(0xFFDCE4FA)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = sheetShape,
        containerColor = sheetContainerColor,
        contentColor = palette.primaryText,
        dragHandle = null,
        modifier = Modifier.testTag("update_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(sheetShape)
                .border(1.dp, topSpecularBorder, sheetShape)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 22.dp, vertical = 18.dp)
        ) {
            // Subtle top drag pill handle
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 16.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(
                        if (isDark) Color.White.copy(alpha = 0.22f)
                        else Color(0xFF94A3B8).copy(alpha = 0.40f)
                    )
            )

            // Header Row: Title + Month/Year Subtitle on Left, CRITICAL/NEW Badge on Right
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AURA PLAYER V${release.version.uppercase(Locale.US)} UPDATE",
                        color = palette.primaryText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.3.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("update_sheet_title")
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = release.publishedMonthYear,
                        color = mutedSlate,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.testTag("update_sheet_date")
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeColor)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("update_sheet_status_badge"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeLabel,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Concise Intro Paragraph
            Text(
                text = if (release.isCritical) {
                    "A critical update for Aura Player is ready with important playback stability, security, and decoder improvements."
                } else {
                    "A newer version of Aura Player is available on GitHub Releases with enhanced playback performance and UI refinements."
                },
                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                fontSize = 13.5.sp,
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Rounded Inner Container listing bulleted release note highlights + APK size indicator
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(innerCardBg)
                    .border(1.dp, innerCardBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
                    .testTag("update_sheet_changelog_card")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "RELEASE HIGHLIGHTS",
                        color = mutedSlate,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isDark) Color.White.copy(alpha = 0.08f)
                                else Color(0xFFE8EEFC)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                            .testTag("update_sheet_apk_size")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FileDownload,
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
                    thickness = 1.dp,
                    color = if (isDark) Color.White.copy(alpha = 0.07f) else Color(0xFFE2E8F0)
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    release.changelogLines.forEach { line ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = if (release.isCritical) Color(0xFFEF4444) else Color(0xFF3B82F6),
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = line,
                                color = palette.primaryText,
                                fontSize = 13.sp,
                                lineHeight = 18.5.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Primary Action Button: Download v${release.version} Update (APK)
            Button(
                onClick = {
                    updateManager.openApkDownloadUrl(context, release.apkDownloadUrl)
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryButtonColor,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("update_sheet_download_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Download v${release.version} Update (APK)",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Secondary Full-Width Close Button
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = closeButtonBg,
                    contentColor = closeButtonText
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("update_sheet_close_button")
            ) {
                Text(
                    text = "Close",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
