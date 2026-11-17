package com.istiak.auraplayer.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.istiak.auraplayer.ui.theme.LocalExtendedPalette

/**
 * Shared real-time `GraphicsLayer` recording state for pure-Compose Frosted / Liquid Glass surfaces.
 * Records both the root ambient background canvas (`backgroundGraphicsLayer`) and the scrolling
 * list/grid content (`contentGraphicsLayer`) so any `FrostedGlassSurface` (Top Header or Bottom
 * Navigation Capsule) can refract and blur the exact pixels underneath it.
 */
@Stable
class FrostedBackdropState(
    val contentGraphicsLayer: GraphicsLayer,
    val backgroundGraphicsLayer: GraphicsLayer? = null
) {
    var activeContentGraphicsLayer: GraphicsLayer by mutableStateOf(contentGraphicsLayer)
    var contentOffsetInRoot: Offset by mutableStateOf(Offset.Zero)
    var backgroundOffsetInRoot: Offset by mutableStateOf(Offset.Zero)
    val scrollProgressState: MutableFloatState = mutableFloatStateOf(0f)
    val redrawTickState: MutableIntState = mutableIntStateOf(0)
}

val LocalFrostedBackdropState = compositionLocalOf<FrostedBackdropState?> { null }

/**
 * Records the root ambient background canvas (`PastelMeshBackground`) into
 * [FrostedBackdropState.backgroundGraphicsLayer] and draws it normally on screen.
 */
fun Modifier.recordFrostedBackground(
    backdropState: FrostedBackdropState?
): Modifier {
    if (backdropState == null || backdropState.backgroundGraphicsLayer == null) return this
    return this
        .onGloballyPositioned { coordinates ->
            backdropState.backgroundOffsetInRoot = coordinates.positionInRoot()
        }
        .drawWithContent {
            val bgLayer = backdropState.backgroundGraphicsLayer
            if (bgLayer != null) {
                bgLayer.record {
                    this@drawWithContent.drawContent()
                }
                drawLayer(bgLayer)
            } else {
                drawContent()
            }
        }
}

/**
 * Records a scrolling `LazyColumn` or `LazyVerticalGrid` into [contentLayerOverride] (or
 * [FrostedBackdropState.contentGraphicsLayer]) so `FrostedGlassSurface` can sample and blur
 * the live scrolling thumbnails and rows beneath the Top Header and Bottom Capsule.
 */
fun Modifier.recordFrostedBackdrop(
    backdropState: FrostedBackdropState?,
    scrollableState: ScrollableState? = null,
    contentLayerOverride: GraphicsLayer? = null
): Modifier {
    if (backdropState == null && contentLayerOverride == null) return this
    return this
        .onGloballyPositioned { coordinates ->
            backdropState?.contentOffsetInRoot = coordinates.positionInRoot()
        }
        .drawWithContent {
            val targetLayer = contentLayerOverride ?: backdropState?.contentGraphicsLayer
            if (targetLayer != null) {
                if (backdropState != null) {
                    backdropState.activeContentGraphicsLayer = targetLayer
                }
                // Observe scroll state during draw phase so scrolling triggers backdrop re-recording
                val isScrolling = scrollableState?.isScrollInProgress == true
                targetLayer.record {
                    this@drawWithContent.drawContent()
                }
                drawLayer(targetLayer)
                if (isScrolling && backdropState != null) {
                    backdropState.redrawTickState.intValue =
                        (backdropState.redrawTickState.intValue + 1) and 0x7FFFFFFF
                }
            } else {
                drawContent()
            }
        }
}

/**
 * Applies a soft, luminous crystal outer drop shadow around floating glass capsules.
 */
fun Modifier.crystalOuterShadow(
    shape: Shape,
    isDark: Boolean = false,
    elevation: Dp = 16.dp
): Modifier {
    val ambient = if (isDark) {
        Color.Black.copy(alpha = 0.46f)
    } else {
        Color(0xFF334155).copy(alpha = 0.14f)
    }
    val spot = if (isDark) {
        Color.Black.copy(alpha = 0.54f)
    } else {
        Color(0xFF334155).copy(alpha = 0.18f)
    }
    return this.shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = ambient,
        spotColor = spot
    )
}

/**
 * Holds scroll-aware header measurement, dynamic content padding, and animated glass opacity
 * ramp (`0f` when idle at scroll top -> `1f` once scrolled past `48.dp`).
 */
@Stable
class ScrollAwareGlassLayoutState(
    val backdropState: FrostedBackdropState?,
    val contentGraphicsLayer: GraphicsLayer,
    val headerHeightPxState: MutableIntState,
    val measuredHeaderHeightDp: Dp,
    val topContentPadding: Dp,
    val contentPadding: PaddingValues,
    val glassProgress: Float
)

/**
 * Creates a [ScrollAwareGlassLayoutState] driven by a [LazyListState] (or optional [LazyGridState]):
 * - Measures header height dynamically via `onSizeChanged { headerHeightPxState.intValue = it.height }`
 *   and computes `topContentPadding = measuredHeaderHeightDp + extraTopGap` (`4.dp`).
 * - Calculates `rawScrollPx` from `firstVisibleItemIndex` and `firstVisibleItemScrollOffset`
 *   over `rampDistancePx = 48.dp.toPx()`.
 * - When un-scrolled (`rawScrollPx <= 2f`), `targetGlassProgress = 0f` so the header sits completely
 *   transparent over the pastel ambient background without any white box or seam.
 * - As the user scrolls, ramps `targetGlassProgress` smoothly (`tween(180ms)`) up to `1f`.
 */
@Composable
fun rememberScrollAwareGlassLayout(
    listState: LazyListState = rememberLazyListState(),
    gridState: LazyGridState? = null,
    useGridState: Boolean = false,
    defaultHeaderHeight: Dp = 68.dp,
    extraTopGap: Dp = 4.dp,
    bottomPadding: Dp = 112.dp,
    horizontalPadding: Dp = 0.dp
): ScrollAwareGlassLayoutState {
    val density = LocalDensity.current
    val ambientBackdrop = LocalFrostedBackdropState.current
    val localContentLayer = rememberGraphicsLayer()
    val effectiveContentLayer = ambientBackdrop?.contentGraphicsLayer ?: localContentLayer

    val statusBarTopDp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val fallbackHeaderDp = defaultHeaderHeight + statusBarTopDp
    val headerHeightPxState = remember { mutableIntStateOf(0) }

    val measuredHeaderHeightDp = if (headerHeightPxState.intValue > 0) {
        with(density) { headerHeightPxState.intValue.toDp() }
    } else {
        fallbackHeaderDp
    }

    val topContentPadding = measuredHeaderHeightDp + extraTopGap
    val rampDistancePx = with(density) { 48.dp.toPx().coerceAtLeast(1f) }

    val targetGlassProgress by remember(listState, gridState, useGridState, rampDistancePx) {
        derivedStateOf {
            val firstIndex = if (useGridState && gridState != null) {
                gridState.firstVisibleItemIndex
            } else {
                listState.firstVisibleItemIndex
            }
            val firstOffset = if (useGridState && gridState != null) {
                gridState.firstVisibleItemScrollOffset
            } else {
                listState.firstVisibleItemScrollOffset
            }
            val rawScrollPx = if (firstIndex > 0) {
                rampDistancePx
            } else {
                firstOffset.toFloat()
            }
            if (rawScrollPx <= 2f) {
                0f
            } else {
                ((rawScrollPx - 2f) / (rampDistancePx - 2f)).coerceIn(0f, 1f)
            }
        }
    }

    val animatedGlassProgress by animateFloatAsState(
        targetValue = targetGlassProgress,
        animationSpec = tween(durationMillis = 180),
        label = "scroll_aware_glass_progress"
    )

    ambientBackdrop?.scrollProgressState?.floatValue = animatedGlassProgress

    val resolvedPadding = remember(horizontalPadding, topContentPadding, bottomPadding) {
        PaddingValues(
            start = horizontalPadding,
            end = horizontalPadding,
            top = topContentPadding,
            bottom = bottomPadding
        )
    }

    return remember(
        ambientBackdrop,
        effectiveContentLayer,
        measuredHeaderHeightDp,
        topContentPadding,
        resolvedPadding,
        animatedGlassProgress
    ) {
        ScrollAwareGlassLayoutState(
            backdropState = ambientBackdrop,
            contentGraphicsLayer = effectiveContentLayer,
            headerHeightPxState = headerHeightPxState,
            measuredHeaderHeightDp = measuredHeaderHeightDp,
            topContentPadding = topContentPadding,
            contentPadding = resolvedPadding,
            glassProgress = animatedGlassProgress
        )
    }
}

/**
 * Feathered Liquid Glass Fixed Top Header container:
 * - Measures its own height into [ScrollAwareGlassLayoutState.headerHeightPxState].
 * - Uses `blurRadius = 10.dp`, `border = null`, and `featherBottomEdge = true`.
 * - Dissolves its bottom edge via `CompositingStrategy.Offscreen` + `BlendMode.DstIn`
 *   (`0.0f to Color.Black`, `0.72f to Color.Black`, `1.0f to Color.Transparent`).
 */
@Composable
fun ScrollAwareFixedHeader(
    layoutState: ScrollAwareGlassLayoutState,
    modifier: Modifier = Modifier,
    blurRadius: Dp = 12.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = LocalExtendedPalette.current.isDark
    val bgColor = MaterialTheme.colorScheme.background
    val headerScrimBrush = remember(bgColor, isDark) {
        Brush.verticalGradient(
            0.0f to bgColor.copy(alpha = 0.96f),
            0.75f to bgColor.copy(alpha = 0.92f),
            1.0f to bgColor.copy(alpha = 0.88f)
        )
    }
    val bottomDividerColor = if (isDark) {
        Color.White.copy(alpha = 0.12f * (0.35f + 0.65f * layoutState.glassProgress))
    } else {
        Color(0xFFCBD5E1).copy(alpha = 0.55f * (0.35f + 0.65f * layoutState.glassProgress))
    }

    FrostedGlassSurface(
        backdropState = layoutState.backdropState,
        contentLayerOverride = layoutState.contentGraphicsLayer,
        glassProgressOverride = layoutState.glassProgress.coerceAtLeast(0.85f),
        shape = RectangleShape,
        blurRadius = blurRadius,
        border = null,
        featherBottomEdge = false,
        baseBackgroundColor = Color.Transparent,
        specularOverlayBrush = headerScrimBrush,
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { size ->
                if (size.height > 0 && layoutState.headerHeightPxState.intValue != size.height) {
                    layoutState.headerHeightPxState.intValue = size.height
                }
            },
        content = {
            content()
            Box(
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.BottomCenter)
                    .fillMaxWidth()
                    .drawBehind {
                        drawRect(color = bottomDividerColor)
                    }
                    .then(Modifier.onSizeChanged { })
            )
        }
    )
}

/**
 * Pure-Compose `GraphicsLayer` Frosted / Liquid Glass Surface:
 * - Draws both `backgroundGraphicsLayer` (`bgDx`, `bgDy`) and `activeContentLayer` (`dx`, `dy`)
 *   aligned via `-surfaceOffsetInRoot` inside `drawBehind`, then applies
 *   `BlurEffect(blurPx, blurPx, TileMode.Clamp)` via `Modifier.graphicsLayer`.
 * - When `featherBottomEdge = true`:
 *   - Light Mode: `headerBaseBg = MaterialTheme.colorScheme.surface.copy(alpha = 0.16f)` and
 *     `headerOverlayBrush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.32f), Color.White.copy(alpha = 0.22f), Color.Transparent))`.
 *   - Dark Mode: `headerBaseBg = MaterialTheme.colorScheme.background.copy(alpha = 0.22f)` and
 *     `headerOverlayBrush = Brush.verticalGradient(listOf(Color(0xFF0F172A).copy(alpha = 0.36f), Color(0xFF1E293B).copy(alpha = 0.28f), Color.Transparent))`.
 *   - Applies `CompositingStrategy.Offscreen` + `BlendMode.DstIn`
 *     (`0.0f to Color.Black`, `0.72f to Color.Black`, `1.0f to Color.Transparent`) so the bottom edge
 *     dissolves smoothly into the scrolling list without any hard seam.
 */
@Composable
fun FrostedGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    blurRadius: Dp = 10.dp,
    border: BorderStroke? = null,
    featherBottomEdge: Boolean = false,
    glassProgressOverride: Float? = null,
    baseBackgroundColor: Color? = null,
    specularOverlayBrush: Brush? = null,
    contentLayerOverride: GraphicsLayer? = null,
    backdropState: FrostedBackdropState? = LocalFrostedBackdropState.current,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark
    val density = LocalDensity.current
    var surfaceOffsetInRoot by remember { mutableStateOf(Offset.Zero) }

    val glassProgress = (glassProgressOverride
        ?: backdropState?.scrollProgressState?.floatValue
        ?: 1f).coerceIn(0f, 1f)

    val effectiveBlurPx = with(density) { (blurRadius * glassProgress).toPx() }
    val surfaceColor = MaterialTheme.colorScheme.surface
    val backgroundColor = MaterialTheme.colorScheme.background

    val headerBaseBg = if (isDark) {
        backgroundColor.copy(alpha = 0.22f * glassProgress)
    } else {
        surfaceColor.copy(alpha = 0.16f * glassProgress)
    }

    val headerOverlayBrush = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF0F172A).copy(alpha = 0.36f * glassProgress),
                Color(0xFF1E293B).copy(alpha = 0.28f * glassProgress),
                Color.Transparent
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.32f * glassProgress),
                Color.White.copy(alpha = 0.22f * glassProgress),
                Color.Transparent
            )
        )
    }

    val capsuleBaseBg = (baseBackgroundColor ?: if (isDark) {
        Color(0xFF121726).copy(alpha = 0.45f)
    } else {
        Color.White.copy(alpha = 0.34f)
    }).let { c -> c.copy(alpha = c.alpha * glassProgress) }

    val capsuleOverlayBrush = specularOverlayBrush ?: if (isDark) {
        Brush.verticalGradient(
            0.0f to Color.White.copy(alpha = 0.24f * glassProgress),
            0.48f to Color.White.copy(alpha = 0.06f * glassProgress),
            1.0f to Color(0xFF60A5FA).copy(alpha = 0.16f * glassProgress)
        )
    } else {
        Brush.verticalGradient(
            0.0f to Color.White.copy(alpha = 0.65f * glassProgress),
            0.48f to Color.White.copy(alpha = 0.14f * glassProgress),
            1.0f to Color.White.copy(alpha = 0.38f * glassProgress)
        )
    }

    val featherMaskBrush = remember {
        Brush.verticalGradient(
            0.0f to Color.Black,
            0.72f to Color.Black,
            1.0f to Color.Transparent
        )
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                surfaceOffsetInRoot = coordinates.positionInRoot()
            }
            .clip(shape)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
    ) {
        if (glassProgress > 0.001f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .then(
                        if (featherBottomEdge) {
                            Modifier
                                .graphicsLayer {
                                    compositingStrategy = CompositingStrategy.Offscreen
                                }
                                .drawWithContent {
                                    drawContent()
                                    drawRect(
                                        brush = featherMaskBrush,
                                        blendMode = BlendMode.DstIn
                                    )
                                }
                        } else {
                            Modifier
                        }
                    )
            ) {
                // Blurred GraphicsLayer refraction backdrop
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            if (effectiveBlurPx > 0.5f) {
                                renderEffect = BlurEffect(
                                    radiusX = effectiveBlurPx,
                                    radiusY = effectiveBlurPx,
                                    edgeTreatment = TileMode.Clamp
                                )
                            }
                            alpha = glassProgress
                        }
                        .drawBehind {
                            // Read redrawTickState so live scrolling invalidates this draw scope
                            val _tick = backdropState?.redrawTickState?.intValue ?: 0
                            val bgLayer = backdropState?.backgroundGraphicsLayer
                            if (bgLayer != null) {
                                val bgOffset = backdropState.backgroundOffsetInRoot
                                val bgDx = bgOffset.x - surfaceOffsetInRoot.x
                                val bgDy = bgOffset.y - surfaceOffsetInRoot.y
                                translate(left = bgDx, top = bgDy) {
                                    drawLayer(bgLayer)
                                }
                            }

                            val activeContentLayer = contentLayerOverride
                                ?: backdropState?.activeContentGraphicsLayer
                                ?: backdropState?.contentGraphicsLayer
                            if (activeContentLayer != null) {
                                val contentOffset = backdropState?.contentOffsetInRoot ?: Offset.Zero
                                val dx = contentOffset.x - surfaceOffsetInRoot.x
                                val dy = contentOffset.y - surfaceOffsetInRoot.y
                                translate(left = dx, top = dy) {
                                    drawLayer(activeContentLayer)
                                }
                            }
                        }
                )

                // Translucent base & vertical gradient specular sheen overlay
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(if (featherBottomEdge) headerBaseBg else capsuleBaseBg)
                        .background(if (featherBottomEdge) headerOverlayBrush else capsuleOverlayBrush)
                )
            }
        }

        // Sharp Foreground Content
        content()
    }
}
