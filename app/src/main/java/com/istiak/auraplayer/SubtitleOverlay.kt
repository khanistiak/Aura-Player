package com.istiak.auraplayer

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Cinema-grade Subtitle Overlay (`SubtitleOverlay.kt`):
 * 1. Crisp, high-contrast subtitle rendering:
 *    - Pure white text (`#FFFFFF` default) with dual-pass outline stroke (`borderEnabled`) + dark drop shadow
 *      (`Shadow(color = Color.Black.copy(alpha = 0.92f), offset = Offset(0f, 3f), blurRadius = 6f)`)
 *      and `Color.Transparent` default background so the movie scene stays 100% unobstructed.
 * 2. Automatic HUD-Aware Subtitle Position:
 *    - When `!controlsVisible` or `isDraggingSubtitle`: positioned at `(styleConfig.bottomMargin * 1.4f).dp`
 *      (where `0..150` maps smoothly to `0.dp..210.dp`, default `8` -> `~11.2.dp` + safe inset).
 *    - When `controlsVisible && !isDraggingSubtitle`: smoothly lifts via `animateDpAsState` to
 *      `maxOf(userBottomMarginDp, bottomHudHeightDp + 12.dp)` so player controls and the wavy seekbar
 *      never overlap active dialogue.
 * 3. Direct MX Player Vertical Subtitle Touch-and-Drag (`video_1.mp4`):
 *    - Attaches `detectVerticalDragGestures` specifically to the visible subtitle cue container bounds,
 *      leaving the rest of the screen free for brightness/volume/seek swipes and tap-to-toggle-HUD.
 *    - Wraps the subtitle text in a tight translucent highlight box (`Color.White.copy(alpha = 0.24f)`,
 *      `RoundedCornerShape(4.dp)`, `padding(horizontal = 8.dp, vertical = 2.dp)`) while touching/dragging.
 *    - Displays a centered `↕ <0-150>` OSD readout in the middle of the screen while dragging and lingers
 *      for `650ms` after finger release before fading out smoothly.
 */
@Composable
fun SubtitleOverlay(
    cueLines: List<String>,
    areSubtitlesEnabled: Boolean,
    isInPipMode: Boolean,
    isScreenLocked: Boolean,
    controlsVisible: Boolean,
    bottomHudHeightDp: Dp,
    styleConfig: SubtitleStyleConfig,
    isSubtitleDrawerOpen: Boolean = false,
    onBottomMarginDragUpdate: (Int) -> Unit,
    onBottomMarginDragCommit: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!areSubtitlesEnabled || isInPipMode) return

    val activeLines = remember(cueLines, isSubtitleDrawerOpen) {
        val cleaned = cueLines.map { it.trim() }.filter { it.isNotEmpty() }
        if (cleaned.isNotEmpty()) {
            cleaned
        } else if (isSubtitleDrawerOpen) {
            listOf("Sample subtitle preview text")
        } else {
            emptyList()
        }
    }

    val density = LocalDensity.current
    var isDraggingSubtitle by remember { mutableStateOf(false) }
    var showCenterOsd by remember { mutableStateOf(false) }
    var dragCompletionTick by remember { mutableIntStateOf(0) }
    var dragAccumulatedMargin by remember { mutableFloatStateOf(styleConfig.bottomMargin.toFloat()) }

    val latestStyleConfig by rememberUpdatedState(styleConfig)
    val latestOnDragUpdate by rememberUpdatedState(onBottomMarginDragUpdate)
    val latestOnDragCommit by rememberUpdatedState(onBottomMarginDragCommit)

    LaunchedEffect(styleConfig.bottomMargin, isDraggingSubtitle) {
        if (!isDraggingSubtitle) {
            dragAccumulatedMargin = styleConfig.bottomMargin.toFloat()
        }
    }

    // Linger the center ↕ <0-150> OSD readout for 650ms after finger release
    LaunchedEffect(isDraggingSubtitle, dragCompletionTick) {
        if (isDraggingSubtitle) {
            showCenterOsd = true
        } else if (showCenterOsd) {
            delay(650L)
            showCenterOsd = false
        }
    }

    val osdAlpha by animateFloatAsState(
        targetValue = if (isDraggingSubtitle || showCenterOsd) 1f else 0f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "subtitle_margin_osd_alpha"
    )

    val userBottomMarginDp = (styleConfig.bottomMargin.coerceIn(0, 150) * 1.4f).dp
    val hudLiftedBottomDp = maxOf(userBottomMarginDp, bottomHudHeightDp + 12.dp)
    val targetBottomPaddingDp = if (controlsVisible && !isDraggingSubtitle && !isSubtitleDrawerOpen) {
        hudLiftedBottomDp
    } else {
        userBottomMarginDp
    }

    val animatedBottomPaddingDp by animateDpAsState(
        targetValue = targetBottomPaddingDp,
        animationSpec = tween(
            durationMillis = if (isDraggingSubtitle) 0 else 220,
            easing = FastOutSlowInEasing
        ),
        label = "subtitle_hud_aware_bottom_padding"
    )

    val boxAlignment = when (styleConfig.alignment) {
        SubtitleAlignment.CENTER -> Alignment.BottomCenter
        SubtitleAlignment.LEFT -> Alignment.BottomStart
        SubtitleAlignment.RIGHT -> Alignment.BottomEnd
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(3f)
    ) {
        if (activeLines.isNotEmpty()) {
            val joinedText = remember(activeLines) { activeLines.joinToString("\n") }
            val fontSize = styleConfig.effectiveTextSizeSp.sp
            val fontWeight = if (styleConfig.isBold) FontWeight.Bold else FontWeight.Medium
            val fontFamily = styleConfig.fontFamily.toComposeFontFamily()
            val textAlign = styleConfig.alignment.toTextAlign()
            val textColor = Color(styleConfig.textColorArgb)
            val borderColor = Color(styleConfig.borderColorArgb)
            val borderAlpha = (styleConfig.borderStrengthPercent.coerceIn(0, 100) / 100f)
            val strokeWidthPx = with(density) {
                (2.2.dp.toPx() * (0.4f + 0.8f * borderAlpha)).coerceIn(1.2f, 8f)
            }
            val textShadow = if (styleConfig.dropShadowEnabled) {
                Shadow(
                    color = Color.Black.copy(alpha = 0.92f),
                    offset = Offset(0f, 3f),
                    blurRadius = 6f
                )
            } else {
                null
            }

            val outerWindowBgColor = if (styleConfig.backgroundEnabled) {
                Color(styleConfig.backgroundColorArgb)
            } else {
                Color.Transparent
            }
            val innerTextBgColor = if (styleConfig.textBackgroundEnabled) {
                Color(styleConfig.textBackgroundColorArgb)
            } else {
                Color.Transparent
            }

            Box(
                contentAlignment = boxAlignment,
                modifier = Modifier
                    .align(boxAlignment)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(
                        start = 20.dp,
                        end = 20.dp,
                        bottom = animatedBottomPaddingDp
                    )
            ) {
                // Interactive Subtitle Cue Container — ONLY this box captures vertical drag gestures!
                Box(
                    modifier = Modifier
                        .then(
                            if (styleConfig.fitIntoVideoSize) {
                                Modifier.widthIn(max = 760.dp)
                            } else {
                                Modifier
                            }
                        )
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when {
                                isDraggingSubtitle -> Color.White.copy(alpha = 0.24f)
                                outerWindowBgColor.alpha > 0.01f -> outerWindowBgColor
                                else -> Color.Transparent
                            }
                        )
                        .pointerInput(isScreenLocked, areSubtitlesEnabled) {
                            if (isScreenLocked || !areSubtitlesEnabled) return@pointerInput
                            val pxPerMarginUnit = 1.4.dp.toPx().coerceAtLeast(1f)
                            detectVerticalDragGestures(
                                onDragStart = {
                                    isDraggingSubtitle = true
                                    showCenterOsd = true
                                    dragAccumulatedMargin = latestStyleConfig.bottomMargin.toFloat()
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    // Dragging upward (dragAmount < 0) increases bottom margin (0 -> 150)
                                    val deltaUnits = -dragAmount / pxPerMarginUnit
                                    dragAccumulatedMargin = (dragAccumulatedMargin + deltaUnits)
                                        .coerceIn(0f, 150f)
                                    val newMarginInt = dragAccumulatedMargin.roundToInt().coerceIn(0, 150)
                                    if (newMarginInt != latestStyleConfig.bottomMargin) {
                                        latestOnDragUpdate(newMarginInt)
                                    }
                                },
                                onDragEnd = {
                                    isDraggingSubtitle = false
                                    dragCompletionTick++
                                    val finalMargin = dragAccumulatedMargin.roundToInt().coerceIn(0, 150)
                                    latestOnDragCommit(finalMargin)
                                },
                                onDragCancel = {
                                    isDraggingSubtitle = false
                                    dragCompletionTick++
                                    val finalMargin = dragAccumulatedMargin.roundToInt().coerceIn(0, 150)
                                    latestOnDragCommit(finalMargin)
                                }
                            )
                        }
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .testTag("subtitle_ interactive_cue_box")
                ) {
                    Box(
                        modifier = Modifier
                            .then(
                                if (!isDraggingSubtitle && innerTextBgColor.alpha > 0.01f) {
                                    Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(innerTextBgColor)
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        // Pass 1: Crisp outline stroke around glyphs (when Border is enabled)
                        if (styleConfig.borderEnabled && borderAlpha > 0.02f) {
                            Text(
                                text = joinedText,
                                color = borderColor.copy(alpha = borderAlpha),
                                fontSize = fontSize,
                                fontWeight = fontWeight,
                                fontFamily = fontFamily,
                                textAlign = textAlign,
                                lineHeight = 1.25.em,
                                style = TextStyle(
                                    drawStyle = Stroke(
                                        width = strokeWidthPx,
                                        join = StrokeJoin.Round
                                    )
                                )
                            )
                        }

                        // Pass 2: Foreground fill + optional cinema drop shadow
                        Text(
                            text = joinedText,
                            color = textColor,
                            fontSize = fontSize,
                            fontWeight = fontWeight,
                            fontFamily = fontFamily,
                            textAlign = textAlign,
                            lineHeight = 1.25.em,
                            style = TextStyle(shadow = textShadow),
                            modifier = Modifier.testTag("subtitle_cue_text")
                        )
                    }
                }
            }
        }

        // MX Player–Style Center-Screen `↕ <0–150>` OSD Readout while dragging subtitle vertically
        if (osdAlpha > 0.01f) {
            val osdDropShadow = remember {
                Shadow(
                    color = Color.Black.copy(alpha = 0.85f),
                    offset = Offset(0f, 2f),
                    blurRadius = 6f
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer { alpha = osdAlpha }
                    .testTag("subtitle_vertical_drag_osd")
            ) {
                Icon(
                    imageVector = Icons.Rounded.SwapVert,
                    contentDescription = "Subtitle Vertical Position",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${styleConfig.bottomMargin.coerceIn(0, 150)}",
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(shadow = osdDropShadow)
                )
            }
        }
    }
}
