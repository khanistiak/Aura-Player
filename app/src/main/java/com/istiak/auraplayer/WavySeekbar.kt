package com.istiak.auraplayer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.istiak.auraplayer.ui.theme.PlayerSeekPeriwinkle
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * High-FPS SeekBar Row with Thumb-Anchored Floating Preview Tooltip (`WavySeekbar.kt`):
 * - Uses lambda state reads (`positionProvider: () -> Long`) so 60/120fps playback interpolation
 *   only redraws the seekbar `Canvas` and 1-second-derived timestamp text without recomposing
 *   `VideoPlayerScreen` or the surrounding Liquid Glass HUD.
 * - Relative, non-jumping drag scrubbing (`detectHorizontalDragGestures`): anchors
 *   `dragStartPositionMs = positionProvider()` on finger down and advances/rewinds proportionally
 *   (`dragStartPositionMs + (cumulativeDragPx / usableWidth) * durationMs`) so touching the bar
 *   never snaps/jumps before dragging.
 * - Deferred `onSeekCommit`: updates lightweight preview state while dragging and commits
 *   `exoPlayer.seekTo()` once cleanly on finger lift (`onDragEnd` / `onDragCancel`).
 */
@Composable
fun SeekBarWithFloatingTooltip(
    positionProvider: () -> Long,
    durationMs: Long,
    isPlaying: Boolean,
    isScrubbing: Boolean,
    chapters: List<VideoChapter>,
    abLoopStartMs: Long?,
    abLoopEndMs: Long?,
    onSeekStartOrUpdate: (Long) -> Unit,
    onSeekCommit: (Long) -> Unit,
    seekBarTestTag: String,
    showTimestamps: Boolean,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var containerWidthPx by remember { mutableIntStateOf(0) }
    var seekbarStartX by remember { mutableFloatStateOf(0f) }
    var seekbarWidthPx by remember { mutableFloatStateOf(0f) }
    var tooltipWidthPx by remember { mutableIntStateOf(with(density) { 120.dp.roundToPx() }) }
    var tooltipHeightPx by remember { mutableIntStateOf(with(density) { 30.dp.roundToPx() }) }

    Box(
        modifier = modifier.onSizeChanged { containerWidthPx = it.width }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (showTimestamps) {
                SeekTimestampLabel(positionProvider = positionProvider)
                Spacer(modifier = Modifier.width(12.dp))
            }

            WavyVideoSeekBar(
                positionProvider = positionProvider,
                durationMs = durationMs,
                isPlaying = isPlaying,
                chapters = chapters,
                abLoopStartMs = abLoopStartMs,
                abLoopEndMs = abLoopEndMs,
                onSeekStartOrUpdate = onSeekStartOrUpdate,
                onSeekCommit = onSeekCommit,
                modifier = Modifier
                    .weight(1f)
                    .onGloballyPositioned { coords ->
                        seekbarStartX = coords.positionInParent().x
                        seekbarWidthPx = coords.size.width.toFloat()
                    }
                    .testTag(seekBarTestTag)
            )

            if (showTimestamps) {
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = formatDurationMillis(durationMs),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (isScrubbing && seekbarWidthPx > 0f) {
            val displayedPosMs = positionProvider()
            val safeDuration = durationMs.coerceAtLeast(1000L)
            val fraction = (displayedPosMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
            val strokePx = with(density) { 4.dp.toPx() }
            val trackStart = strokePx / 2f
            val trackEnd = (seekbarWidthPx - strokePx / 2f).coerceAtLeast(trackStart + 1f)
            val usableWidth = (trackEnd - trackStart).coerceAtLeast(1f)
            val thumbCenterX = seekbarStartX + (trackStart + usableWidth * fraction).coerceIn(trackStart, trackEnd)

            val desiredX = (thumbCenterX - tooltipWidthPx / 2f).roundToInt()
            val maxX = (containerWidthPx - tooltipWidthPx).coerceAtLeast(0)
            val clampedX = desiredX.coerceIn(0, maxX)
            val gapPx = with(density) { 10.dp.roundToPx() }
            val tooltipY = -(tooltipHeightPx + gapPx)

            val activeChapter = remember(chapters, displayedPosMs / 500L) {
                findChapterForPosition(chapters, displayedPosMs)
            }
            val tooltipText = buildString {
                if (!activeChapter?.title.isNullOrBlank()) {
                    append(activeChapter?.title)
                    append(" • ")
                }
                append(formatDurationMillis(displayedPosMs))
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .zIndex(10f)
                    .onSizeChanged {
                        tooltipWidthPx = it.width
                        tooltipHeightPx = it.height
                    }
                    .offset { IntOffset(x = clampedX, y = tooltipY) }
                    .widthIn(max = 260.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFF0F172A).copy(alpha = 0.84f))
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.45f),
                                PlayerSeekPeriwinkle.copy(alpha = 0.65f)
                            )
                        ),
                        shape = RoundedCornerShape(999.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("seekbar_preview_tooltip")
            ) {
                Text(
                    text = tooltipText,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SeekTimestampLabel(
    positionProvider: () -> Long
) {
    val displayedSecond by remember(positionProvider) {
        derivedStateOf { (positionProvider().coerceAtLeast(0L)) / 1000L }
    }
    Text(
        text = formatDurationMillis(displayedSecond * 1000L),
        color = Color.White,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold
    )
}

/**
 * Material 3 / MX Player–Smooth Wavy Seekbar (`WavyVideoSeekBar`) drawn on Compose `Canvas`:
 * - Relative horizontal drag (`detectHorizontalDragGestures`): anchors `dragStartPositionMs = latestPositionProvider()`
 *   on initial drag start and scrubs proportionally (`dragStartPositionMs + (cumulativeDragPx / usableWidth) * durationMs`)
 *   with zero initial snap/jump.
 * - Point-to-point single tap (`detectTapGestures`) for direct tap seeking without interfering with drag gestures.
 * - Reads `positionProvider()` inside `Canvas` draw scope so 60/120fps frame interpolation never triggers
 *   parent Composable recompositions.
 */
@Composable
fun WavyVideoSeekBar(
    positionProvider: () -> Long,
    durationMs: Long,
    isPlaying: Boolean,
    chapters: List<VideoChapter>,
    abLoopStartMs: Long? = null,
    abLoopEndMs: Long? = null,
    onSeekStartOrUpdate: (Long) -> Unit,
    onSeekCommit: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val safeDuration = durationMs.coerceAtLeast(1000L)
    val latestDurationMs by rememberUpdatedState(safeDuration)
    val latestPositionProvider by rememberUpdatedState(positionProvider)
    val latestSeekUpdate by rememberUpdatedState(onSeekStartOrUpdate)
    val latestSeekCommit by rememberUpdatedState(onSeekCommit)
    val textMeasurer = rememberTextMeasurer()

    val animatedAmplitudeFactor by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(durationMillis = 320),
        label = "wavy_seekbar_amplitude"
    )

    var wavePhaseRad by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        var lastFrameNs = 0L
        while (true) {
            withFrameNanos { frameNs ->
                if (lastFrameNs != 0L) {
                    val deltaSec = ((frameNs - lastFrameNs) / 1_000_000_000f).coerceIn(0f, 0.05f)
                    wavePhaseRad = (wavePhaseRad + deltaSec * 4.2f) % (2f * PI.toFloat())
                }
                lastFrameNs = frameNs
            }
        }
    }

    val chapterColors = remember {
        listOf(
            Color(0xFFF59E0B), // amber
            Color(0xFF3B82F6), // blue
            Color(0xFFEF4444), // crimson
            Color(0xFF8B5CF6), // violet
            Color(0xFF14B8A6)  // teal
        )
    }

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .height(48.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val strokePx = 4.dp.toPx()
                    val trackStart = strokePx / 2f
                    val trackEnd = (size.width - strokePx / 2f).coerceAtLeast(trackStart + 1f)
                    val usableWidth = (trackEnd - trackStart).coerceAtLeast(1f)
                    val tapFraction = ((offset.x - trackStart) / usableWidth).coerceIn(0f, 1f)
                    val targetMs = (tapFraction * latestDurationMs).toLong().coerceIn(0L, latestDurationMs)
                    latestSeekCommit(targetMs)
                }
            }
            .pointerInput(Unit) {
                var dragStartPositionMs = 0L
                var cumulativeDragPx = 0f
                var activeDragTargetMs = 0L
                detectHorizontalDragGestures(
                    onDragStart = {
                        // Anchor to current playback position without jumping to the touch X coordinate
                        dragStartPositionMs = latestPositionProvider().coerceIn(0L, latestDurationMs)
                        cumulativeDragPx = 0f
                        activeDragTargetMs = dragStartPositionMs
                        latestSeekUpdate(activeDragTargetMs)
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        cumulativeDragPx += dragAmount
                        val strokePx = 4.dp.toPx()
                        val trackStart = strokePx / 2f
                        val trackEnd = (size.width - strokePx / 2f).coerceAtLeast(trackStart + 1f)
                        val usableWidth = (trackEnd - trackStart).coerceAtLeast(1f)
                        val deltaMs = ((cumulativeDragPx / usableWidth) * latestDurationMs.toDouble()).toLong()
                        activeDragTargetMs = (dragStartPositionMs + deltaMs).coerceIn(0L, latestDurationMs)
                        latestSeekUpdate(activeDragTargetMs)
                    },
                    onDragEnd = {
                        latestSeekCommit(activeDragTargetMs)
                    },
                    onDragCancel = {
                        latestSeekCommit(activeDragTargetMs)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val currentMs = positionProvider().coerceIn(0L, safeDuration)
            val fraction = (currentMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
            val strokePx = 4.dp.toPx()
            val amplitudePx = 3.5.dp.toPx() * animatedAmplitudeFactor
            val wavelengthPx = 24.dp.toPx().coerceAtLeast(12f)
            val thumbWidthPx = 4.dp.toPx()
            val thumbHeightPx = 20.dp.toPx()
            val thumbGapPx = 5.dp.toPx()

            val centerY = size.height / 2f
            val trackStart = strokePx / 2f
            val trackEnd = (size.width - strokePx / 2f).coerceAtLeast(trackStart + 1f)
            val usableWidth = (trackEnd - trackStart).coerceAtLeast(1f)
            val thumbCenterX = (trackStart + usableWidth * fraction).coerceIn(trackStart, trackEnd)

            // 1. Subtle translucent chapter / bookmark highlight blocks (only when 2+ real chapters exist)
            if (chapters.size >= 2) {
                val sortedChapters = chapters.sortedBy { it.startTimeMs }
                sortedChapters.forEachIndexed { idx, chapter ->
                    val startFrac = (chapter.startTimeMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
                    val endMs = sortedChapters.getOrNull(idx + 1)?.startTimeMs ?: safeDuration
                    val endFrac = (endMs.toFloat() / safeDuration.toFloat()).coerceIn(startFrac, 1f)
                    val segStartX = trackStart + usableWidth * startFrac
                    val segEndX = trackStart + usableWidth * endFrac
                    val segW = (segEndX - segStartX - 2.dp.toPx()).coerceAtLeast(2f)
                    val blockColor = chapterColors[idx % chapterColors.size].copy(alpha = 0.24f)
                    drawRoundRect(
                        color = blockColor,
                        topLeft = Offset(segStartX, centerY - 5.dp.toPx()),
                        size = Size(segW, 10.dp.toPx()),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
            }

            // 2. Unplayed straight track (right of thumb with clean gap)
            val unplayedStartX = (thumbCenterX + thumbGapPx).coerceAtMost(trackEnd)
            if (trackEnd - unplayedStartX > 2f) {
                drawLine(
                    color = Color.White.copy(alpha = 0.28f),
                    start = Offset(unplayedStartX, centerY),
                    end = Offset(trackEnd, centerY),
                    strokeWidth = strokePx,
                    cap = StrokeCap.Round
                )
            }

            // 3. Played sinusoidal wavy track (left of thumb with clean gap)
            val playedEndX = (thumbCenterX - thumbGapPx).coerceAtLeast(trackStart)
            if (playedEndX - trackStart > 1.5f) {
                val wavePath = Path()
                val stepPx = 2.5.dp.toPx().coerceAtLeast(2f)
                var x = trackStart
                var firstPoint = true
                while (x <= playedEndX) {
                    val distFromThumb = (playedEndX - x).coerceAtLeast(0f)
                    val edgeTaper = (distFromThumb / (wavelengthPx * 0.45f)).coerceIn(0f, 1f)
                    val startTaper = ((x - trackStart) / (wavelengthPx * 0.35f)).coerceIn(0f, 1f)
                    val angle = ((x - trackStart) / wavelengthPx) * (2f * PI.toFloat()) - wavePhaseRad
                    val y = centerY + sin(angle) * amplitudePx * edgeTaper * startTaper
                    if (firstPoint) {
                        wavePath.moveTo(x, y)
                        firstPoint = false
                    } else {
                        wavePath.lineTo(x, y)
                    }
                    x += stepPx
                }
                wavePath.lineTo(playedEndX, centerY)

                drawPath(
                    path = wavePath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF58A6FF), Color(0xFF6366F1)),
                        startX = trackStart,
                        endX = playedEndX.coerceAtLeast(trackStart + 1f)
                    ),
                    style = Stroke(
                        width = strokePx,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // 3b. Highlighted A-B Loop segment & glowing Cyan (A) / Coral (B) pin markers
            val cyanPinColor = Color(0xFF22D3EE)
            val coralPinColor = Color(0xFFFF6B6B)
            val aX = abLoopStartMs?.let {
                trackStart + usableWidth * (it.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
            }
            val bX = abLoopEndMs?.let {
                trackStart + usableWidth * (it.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
            }

            if (aX != null && bX != null && bX > aX) {
                val segWidth = (bX - aX).coerceAtLeast(2f)
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(cyanPinColor.copy(alpha = 0.28f), coralPinColor.copy(alpha = 0.28f)),
                        startX = aX,
                        endX = bX
                    ),
                    topLeft = Offset(aX, centerY - 6.dp.toPx()),
                    size = Size(segWidth, 12.dp.toPx()),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(cyanPinColor, coralPinColor),
                        startX = aX,
                        endX = bX
                    ),
                    start = Offset(aX, centerY),
                    end = Offset(bX, centerY),
                    strokeWidth = 3.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            fun drawLoopPin(pinX: Float, pinColor: Color, badgeText: String) {
                val badgeRadius = 6.5.dp.toPx()
                val badgeCenterY = (centerY - 12.dp.toPx()).coerceAtLeast(badgeRadius + 1f)
                drawCircle(
                    color = pinColor.copy(alpha = 0.35f),
                    radius = badgeRadius + 3.dp.toPx(),
                    center = Offset(pinX, badgeCenterY)
                )
                drawLine(
                    color = pinColor,
                    start = Offset(pinX, badgeCenterY + badgeRadius - 1f),
                    end = Offset(pinX, centerY + 6.dp.toPx()),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = pinColor,
                    radius = badgeRadius,
                    center = Offset(pinX, badgeCenterY)
                )
                val layout = textMeasurer.measure(
                    text = badgeText,
                    style = TextStyle(
                        color = Color.Black,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(
                        x = pinX - layout.size.width / 2f,
                        y = badgeCenterY - layout.size.height / 2f
                    )
                )
            }

            if (aX != null) drawLoopPin(aX, cyanPinColor, "A")
            if (bX != null) drawLoopPin(bX, coralPinColor, "B")

            // 4. Chapter tick dividers
            if (chapters.size >= 2) {
                chapters.forEach { chapter ->
                    if (chapter.startTimeMs in 1L until safeDuration) {
                        val tickFrac = (chapter.startTimeMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
                        val tickX = trackStart + usableWidth * tickFrac
                        if (abs(tickX - thumbCenterX) > thumbGapPx + 2.dp.toPx()) {
                            drawLine(
                                color = Color.White.copy(alpha = 0.80f),
                                start = Offset(tickX, centerY - 4.5.dp.toPx()),
                                end = Offset(tickX, centerY + 4.5.dp.toPx()),
                                strokeWidth = 1.6.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }

            // 5. Vertical rounded pill thumb handle (#58A6FF outer glow + crisp white core)
            val thumbLeft = (thumbCenterX - thumbWidthPx / 2f).coerceIn(0f, size.width - thumbWidthPx)
            val thumbTop = centerY - thumbHeightPx / 2f
            drawRoundRect(
                color = Color(0xFF58A6FF).copy(alpha = 0.45f),
                topLeft = Offset(thumbLeft - 1.5.dp.toPx(), thumbTop - 1.5.dp.toPx()),
                size = Size(thumbWidthPx + 3.dp.toPx(), thumbHeightPx + 3.dp.toPx()),
                cornerRadius = CornerRadius(999f, 999f)
            )
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(thumbLeft, thumbTop),
                size = Size(thumbWidthPx, thumbHeightPx),
                cornerRadius = CornerRadius(999f, 999f)
            )
        }
    }
}
