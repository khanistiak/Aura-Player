package com.istiak.auraplayer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.istiak.auraplayer.ui.theme.LocalExtendedPalette

@Composable
fun DirectoryFolderRowItem(
    node: DirectoryNode,
    settings: AppSettingsState,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val unwatchedCount = node.unwatchedVideoCount(
        watchedVideoIds = settings.watchedVideoIds,
        seenBaselineEpochSec = settings.seenBaselineEpochSec,
        showNewLabel = settings.showNewVideoLabel,
        newLabelDaysThreshold = settings.newVideoLabelDaysThreshold
    )

    val countSegments = remember(node, settings.enabledFields) {
        buildList {
            if (DisplayField.TOTAL_MEDIA in settings.enabledFields) {
                if (node.directVideoCount > 0) {
                    add("${node.directVideoCount} ${if (node.directVideoCount == 1) "video" else "videos"}")
                }
                if (node.immediateFolderCount > 0) {
                    add("${node.immediateFolderCount} ${if (node.immediateFolderCount == 1) "folder" else "folders"}")
                }
                if (isEmpty() && node.recursiveVideoCount > 0) {
                    add("${node.recursiveVideoCount} ${if (node.recursiveVideoCount == 1) "video" else "videos"}")
                }
            }
        }
    }
    val tailSegments = remember(node, settings.enabledFields) {
        buildList {
            if (DisplayField.FOLDER_SIZE in settings.enabledFields) {
                add(node.formattedRecursiveSize)
            }
            if (DisplayField.TOTAL_DURATION in settings.enabledFields && node.recursiveDurationMs > 0L) {
                add(node.formattedRecursiveDuration)
            }
        }
    }
    val metadataLine = buildFolderInlineMetadata(
        countSegments = countSegments,
        tailSegments = tailSegments,
        isDark = isDark
    )

    FrostedFolderCardShell(
        isSelected = isSelected,
        isDark = isDark,
        onClick = onClick,
        onLongPress = onLongPress,
        modifier = modifier
            .fillMaxWidth()
            .testTag("tree_folder_row_${node.name}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier.size(width = 60.dp, height = 50.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                PremiumFolderIcon(
                    width = 54.dp,
                    height = 46.dp
                )

                if (isSelected) {
                    SelectionCheckmarkBadge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-1).dp)
                    )
                } else if (unwatchedCount > 0) {
                    UnwatchedCountPillBadge(
                        count = unwatchedCount,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-1).dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = node.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryText,
                    maxLines = if (settings.showFullNames) Int.MAX_VALUE else 1,
                    overflow = if (settings.showFullNames) TextOverflow.Clip else TextOverflow.Ellipsis
                )

                if (metadataLine.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = metadataLine,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = palette.secondaryText.copy(alpha = 0.72f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun DirectoryFolderGridItem(
    node: DirectoryNode,
    settings: AppSettingsState,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {}
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val unwatchedCount = node.unwatchedVideoCount(
        watchedVideoIds = settings.watchedVideoIds,
        seenBaselineEpochSec = settings.seenBaselineEpochSec,
        showNewLabel = settings.showNewVideoLabel,
        newLabelDaysThreshold = settings.newVideoLabelDaysThreshold
    )
    val countSegments = remember(node) {
        buildList {
            if (node.directVideoCount > 0) {
                add("${node.directVideoCount} ${if (node.directVideoCount == 1) "video" else "videos"}")
            }
            if (node.immediateFolderCount > 0) {
                add("${node.immediateFolderCount} ${if (node.immediateFolderCount == 1) "folder" else "folders"}")
            }
            if (isEmpty() && node.recursiveVideoCount > 0) {
                add("${node.recursiveVideoCount} ${if (node.recursiveVideoCount == 1) "video" else "videos"}")
            }
        }
    }
    val tailSegments = remember(node) { listOf(node.formattedRecursiveSize) }
    val metadataLine = buildFolderInlineMetadata(
        countSegments = countSegments,
        tailSegments = tailSegments,
        isDark = isDark
    )

    FrostedFolderCardShell(
        isSelected = isSelected,
        isDark = isDark,
        onClick = onClick,
        onLongPress = onLongPress,
        innerPaddingHorizontal = 12.dp,
        innerPaddingVertical = 14.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tree_folder_grid_${node.name}")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier.size(width = 66.dp, height = 56.dp),
                contentAlignment = Alignment.Center
            ) {
                PremiumFolderIcon(
                    width = 60.dp,
                    height = 50.dp
                )
                if (isSelected) {
                    SelectionCheckmarkBadge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-1).dp)
                    )
                } else if (unwatchedCount > 0) {
                    UnwatchedCountPillBadge(
                        count = unwatchedCount,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-1).dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = node.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = palette.primaryText,
                textAlign = TextAlign.Center,
                maxLines = if (settings.showFullNames) Int.MAX_VALUE else 1,
                overflow = if (settings.showFullNames) TextOverflow.Clip else TextOverflow.Ellipsis
            )
            if (metadataLine.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = metadataLine,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun FlatFolderRowItem(
    folder: VideoFolder,
    settings: AppSettingsState,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val unwatchedCount = folder.videos.count {
        it.isConsideredNew(
            watchedVideoIds = settings.watchedVideoIds,
            seenBaselineEpochSec = settings.seenBaselineEpochSec,
            showNewLabel = settings.showNewVideoLabel,
            newLabelDaysThreshold = settings.newVideoLabelDaysThreshold
        )
    }

    val countSegments = remember(folder, settings.enabledFields) {
        buildList {
            if (DisplayField.TOTAL_MEDIA in settings.enabledFields) {
                add("${folder.videoCount} ${if (folder.videoCount == 1) "video" else "videos"}")
            }
        }
    }
    val tailSegments = remember(folder, settings.enabledFields) {
        buildList {
            if (DisplayField.FOLDER_SIZE in settings.enabledFields) {
                add(folder.formattedTotalSize)
            }
            if (DisplayField.TOTAL_DURATION in settings.enabledFields && folder.totalDurationMs > 0L) {
                add(folder.formattedTotalDuration)
            }
        }
    }
    val metadataLine = buildFolderInlineMetadata(
        countSegments = countSegments,
        tailSegments = tailSegments,
        isDark = isDark
    )

    FrostedFolderCardShell(
        isSelected = isSelected,
        isDark = isDark,
        onClick = onClick,
        onLongPress = onLongPress,
        modifier = modifier
            .fillMaxWidth()
            .testTag("flat_folder_row_${folder.name}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier.size(width = 60.dp, height = 50.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                PremiumFolderIcon(
                    width = 54.dp,
                    height = 46.dp
                )
                if (isSelected) {
                    SelectionCheckmarkBadge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-1).dp)
                    )
                } else if (unwatchedCount > 0) {
                    UnwatchedCountPillBadge(
                        count = unwatchedCount,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-1).dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryText,
                    maxLines = if (settings.showFullNames) Int.MAX_VALUE else 1,
                    overflow = if (settings.showFullNames) TextOverflow.Clip else TextOverflow.Ellipsis
                )
                if (metadataLine.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = metadataLine,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = palette.secondaryText.copy(alpha = 0.72f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun FlatFolderGridItem(
    folder: VideoFolder,
    unwatchedCount: Int,
    showFullNames: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {}
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val countSegments = remember(folder) {
        listOf("${folder.videoCount} ${if (folder.videoCount == 1) "video" else "videos"}")
    }
    val tailSegments = remember(folder) { listOf(folder.formattedTotalSize) }
    val metadataLine = buildFolderInlineMetadata(
        countSegments = countSegments,
        tailSegments = tailSegments,
        isDark = isDark
    )

    FrostedFolderCardShell(
        isSelected = isSelected,
        isDark = isDark,
        onClick = onClick,
        onLongPress = onLongPress,
        innerPaddingHorizontal = 12.dp,
        innerPaddingVertical = 14.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("flat_folder_grid_${folder.name}")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier.size(width = 66.dp, height = 56.dp),
                contentAlignment = Alignment.Center
            ) {
                PremiumFolderIcon(
                    width = 60.dp,
                    height = 50.dp
                )
                if (isSelected) {
                    SelectionCheckmarkBadge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-1).dp)
                    )
                } else if (unwatchedCount > 0) {
                    UnwatchedCountPillBadge(
                        count = unwatchedCount,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-1).dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = folder.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = palette.primaryText,
                textAlign = TextAlign.Center,
                maxLines = if (showFullNames) Int.MAX_VALUE else 1,
                overflow = if (showFullNames) TextOverflow.Clip else TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = metadataLine,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun PremiumFolderIcon(
    modifier: Modifier = Modifier,
    width: Dp = 54.dp,
    height: Dp = 46.dp
) {
    val primary = MaterialTheme.colorScheme.primary
    val topLightTint = remember(primary) {
        Color(
            red = (primary.red + 0.22f).coerceAtMost(1f),
            green = (primary.green + 0.22f).coerceAtMost(1f),
            blue = (primary.blue + 0.22f).coerceAtMost(1f),
            alpha = 1f
        )
    }
    val deepTint = remember(primary) {
        Color(
            red = (primary.red * 0.78f).coerceIn(0f, 1f),
            green = (primary.green * 0.78f).coerceIn(0f, 1f),
            blue = (primary.blue * 0.85f).coerceIn(0f, 1f),
            alpha = 1f
        )
    }

    Canvas(modifier = modifier.size(width = width, height = height)) {
        val w = size.width
        val h = size.height
        val cornerPx = 7.dp.toPx().coerceAtMost(h * 0.2f)

        drawRoundRect(
            color = deepTint.copy(alpha = 0.18f),
            topLeft = Offset(x = w * 0.04f, y = h * 0.28f),
            size = Size(width = w * 0.92f, height = h * 0.72f),
            cornerRadius = CornerRadius(cornerPx, cornerPx)
        )

        val backTabTop = h * 0.02f
        val backBodyTop = h * 0.18f
        val backBottom = h * 0.94f
        val tabEndX = w * 0.43f
        val shoulderX = w * 0.54f

        val backPath = Path().apply {
            moveTo(0f, backTabTop + cornerPx)
            quadraticTo(0f, backTabTop, cornerPx, backTabTop)
            lineTo(tabEndX - cornerPx * 0.5f, backTabTop)
            cubicTo(
                tabEndX + cornerPx * 0.35f, backTabTop,
                shoulderX - cornerPx * 0.35f, backBodyTop,
                shoulderX + cornerPx * 0.4f, backBodyTop
            )
            lineTo(w - cornerPx, backBodyTop)
            quadraticTo(w, backBodyTop, w, backBodyTop + cornerPx)
            lineTo(w, backBottom - cornerPx)
            quadraticTo(w, backBottom, w - cornerPx, backBottom)
            lineTo(cornerPx, backBottom)
            quadraticTo(0f, backBottom, 0f, backBottom - cornerPx)
            close()
        }
        drawPath(
            path = backPath,
            brush = Brush.linearGradient(
                colors = listOf(primary, deepTint),
                start = Offset.Zero,
                end = Offset(w, h * 0.9f)
            )
        )

        val peekHeight = 2.dp.toPx()
        val peekTop = h * 0.225f
        val peekInset = w * 0.07f
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.86f),
                    Color.White.copy(alpha = 0.50f)
                ),
                startY = peekTop,
                endY = peekTop + peekHeight * 2.2f
            ),
            topLeft = Offset(x = peekInset, y = peekTop),
            size = Size(width = w - peekInset * 2f, height = peekHeight * 2.4f),
            cornerRadius = CornerRadius(peekHeight, peekHeight)
        )

        val flapTop = h * 0.27f
        val flapHeight = h * 0.70f
        val flapCorner = 7.5.dp.toPx().coerceAtMost(flapHeight * 0.28f)

        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(topLightTint, primary, deepTint),
                startY = flapTop,
                endY = flapTop + flapHeight
            ),
            topLeft = Offset(x = 0f, y = flapTop),
            size = Size(width = w, height = flapHeight),
            cornerRadius = CornerRadius(flapCorner, flapCorner)
        )

        drawRoundRect(
            color = Color.White.copy(alpha = 0.20f),
            topLeft = Offset(x = 0.5.dp.toPx(), y = flapTop + 0.5.dp.toPx()),
            size = Size(width = w - 1.dp.toPx(), height = flapHeight - 1.dp.toPx()),
            cornerRadius = CornerRadius(flapCorner, flapCorner),
            style = Stroke(width = 0.75.dp.toPx())
        )

        val specStroke = 1.dp.toPx()
        val specY = flapTop + specStroke * 0.7f
        val specularPath = Path().apply {
            moveTo(flapCorner * 0.45f, specY + flapCorner * 0.28f)
            quadraticTo(flapCorner * 0.7f, specY, flapCorner * 1.15f, specY)
            lineTo(w - flapCorner * 1.15f, specY)
            quadraticTo(w - flapCorner * 0.7f, specY, w - flapCorner * 0.45f, specY + flapCorner * 0.28f)
        }
        drawPath(
            path = specularPath,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.22f),
                    Color.White.copy(alpha = 0.85f),
                    Color.White.copy(alpha = 0.22f)
                )
            ),
            style = Stroke(width = specStroke, cap = StrokeCap.Round)
        )

        val cx = w * 0.50f
        val cy = flapTop + flapHeight * 0.52f
        val triRadius = flapHeight * 0.21f
        val playPath = Path().apply {
            moveTo(cx - triRadius * 0.55f, cy - triRadius * 0.85f)
            lineTo(cx + triRadius * 0.95f, cy)
            lineTo(cx - triRadius * 0.55f, cy + triRadius * 0.85f)
            close()
        }
        drawPath(
            path = playPath,
            color = Color.White.copy(alpha = 0.18f),
            style = Stroke(
                width = 1.5.dp.toPx(),
                join = StrokeJoin.Round,
                cap = StrokeCap.Round
            )
        )
        drawPath(
            path = playPath,
            color = Color.White.copy(alpha = 0.18f)
        )
    }
}

@Composable
internal fun UnwatchedCountPillBadge(
    count: Int,
    modifier: Modifier = Modifier
) {
    if (count <= 0) return
    val badgeBrush = remember {
        Brush.linearGradient(
            colors = listOf(Color(0xFFFF5E62), Color(0xFFEF4444))
        )
    }
    val label = if (count > 99) "99+" else count.toString()
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 22.dp, minHeight = 22.dp)
            .shadow(3.dp, RoundedCornerShape(999.dp))
            .clip(RoundedCornerShape(999.dp))
            .background(badgeBrush)
            .border(1.5.dp, Color.White, RoundedCornerShape(999.dp))
            .padding(horizontal = if (count > 9) 5.5.dp else 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum")
        )
    }
}

@Composable
internal fun buildFolderInlineMetadata(
    countSegments: List<String>,
    tailSegments: List<String>,
    isDark: Boolean
): AnnotatedString {
    val palette = LocalExtendedPalette.current
    val baseColor = palette.secondaryText
    val countHighlightColor = MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.94f else 0.88f)
    val separatorColor = baseColor.copy(alpha = 0.55f)

    return remember(countSegments, tailSegments, isDark, countHighlightColor, baseColor) {
        buildAnnotatedString {
            var hasEmitted = false
            countSegments.forEach { segment ->
                if (hasEmitted) {
                    withStyle(SpanStyle(color = separatorColor, fontFeatureSettings = "tnum")) {
                        append("  •  ")
                    }
                }
                withStyle(
                    SpanStyle(
                        color = countHighlightColor,
                        fontWeight = FontWeight.SemiBold,
                        fontFeatureSettings = "tnum"
                    )
                ) {
                    append(segment)
                }
                hasEmitted = true
            }
            tailSegments.forEach { segment ->
                if (hasEmitted) {
                    withStyle(SpanStyle(color = separatorColor, fontFeatureSettings = "tnum")) {
                        append("  •  ")
                    }
                }
                withStyle(
                    SpanStyle(
                        color = baseColor,
                        fontWeight = FontWeight.Medium,
                        fontFeatureSettings = "tnum"
                    )
                ) {
                    append(segment)
                }
                hasEmitted = true
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FrostedFolderCardShell(
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    innerPaddingHorizontal: Dp = 14.dp,
    innerPaddingVertical: Dp = 12.dp,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val palette = LocalExtendedPalette.current
    val primary = MaterialTheme.colorScheme.primary
    val cardShape = remember { RoundedCornerShape(20.dp) }

    val cardBg = when {
        isSelected && isDark -> palette.periwinkleBadgeBg.copy(alpha = 0.78f)
        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.90f)
        isDark -> palette.sheetBackground.copy(alpha = 0.74f)
        else -> Color.White.copy(alpha = 0.76f)
    }
    val borderBrush = remember(isSelected, isDark, primary, palette.outlineBorder) {
        when {
            isSelected -> Brush.verticalGradient(
                listOf(primary.copy(alpha = 0.75f), primary.copy(alpha = 0.45f))
            )
            isDark -> Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.14f))
            )
            else -> Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.90f), palette.outlineBorder.copy(alpha = 0.65f))
            )
        }
    }
    val shadowAmbient = if (isDark) Color(0x33000000) else primary.copy(alpha = 0.08f)

    Box(
        modifier = modifier
            .shadow(
                elevation = 2.dp,
                shape = cardShape,
                ambientColor = shadowAmbient,
                spotColor = shadowAmbient
            )
            .clip(cardShape)
            .background(cardBg)
            .border(width = 1.dp, brush = borderBrush, shape = cardShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    runCatching {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    onLongPress()
                }
            )
            .padding(horizontal = innerPaddingHorizontal, vertical = innerPaddingVertical)
    ) {
        content()
    }
}
