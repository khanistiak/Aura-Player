package com.istiak.auraplayer.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.istiak.auraplayer.ui.theme.LocalExtendedPalette
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/**
 * Reusable Material 3 Expressive morphing pull-to-refresh container:
 * - Displaces the scrollable list/grid content downward in lockstep with finger pull
 *   (rubber-band resistance up to `maxPullDistance` ~88.dp, trigger threshold `64.dp`).
 * - Gently holds the list at `refreshHoldDistance` (~56.dp) during active MediaStore re-scanning,
 *   then springs back (`dampingRatio = 0.78f, stiffness = 380f`) to `0.dp` upon completion.
 * - Renders a `46.dp` Liquid Crystal circular container below [headerTopPadding] housing a
 *   `24.dp` Aura Blue rotating polygon smoothly morphing between a 12-tooth scalloped starburst,
 *   soft organic blob, 8-sided soft octagon, and rounded diamond/squircle via 72-vertex polar
 *   radius interpolation.
 */
@Composable
fun ExpressivePullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    headerTopPadding: Dp,
    modifier: Modifier = Modifier,
    isAtTop: () -> Boolean = { true },
    enabled: Boolean = true,
    maxPullDistance: Dp = 88.dp,
    triggerThreshold: Dp = 64.dp,
    refreshHoldDistance: Dp = 56.dp,
    indicatorTestTag: String = "pull_to_refresh_indicator",
    content: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val palette = LocalExtendedPalette.current
    val isDark = palette.isDark

    val maxPullPx = with(density) { maxPullDistance.toPx() }
    val triggerThresholdPx = with(density) { triggerThreshold.toPx() }
    val refreshHoldPx = with(density) { refreshHoldDistance.toPx() }

    val displacementAnim = remember { Animatable(0f) }
    var rawPullPx by remember { mutableFloatStateOf(0f) }

    val latestIsRefreshing by rememberUpdatedState(isRefreshing)
    val latestOnRefresh by rememberUpdatedState(onRefresh)
    val latestIsAtTop by rememberUpdatedState(isAtTop)
    val latestEnabled by rememberUpdatedState(enabled)

    fun rubberBandOffset(rawPx: Float): Float {
        if (rawPx <= 0f || maxPullPx <= 0f) return 0f
        return (maxPullPx * (1f - exp(-rawPx / maxPullPx))).coerceIn(0f, maxPullPx)
    }

    val returnSpringSpec = remember {
        spring<Float>(
            dampingRatio = 0.78f,
            stiffness = 380f
        )
    }

    // Hold list gently at ~56.dp while refreshing, and spring back to 0.dp upon completion
    LaunchedEffect(isRefreshing, refreshHoldPx) {
        if (isRefreshing) {
            rawPullPx = 0f
            displacementAnim.animateTo(
                targetValue = refreshHoldPx,
                animationSpec = returnSpringSpec
            )
        } else {
            rawPullPx = 0f
            displacementAnim.animateTo(
                targetValue = 0f,
                animationSpec = returnSpringSpec
            )
        }
    }

    val nestedScrollConnection = remember(maxPullPx, triggerThresholdPx, refreshHoldPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!latestEnabled || latestIsRefreshing || source != NestedScrollSource.UserInput) {
                    return Offset.Zero
                }
                if (available.y < 0f && rawPullPx > 0f) {
                    val prevRaw = rawPullPx
                    val nextRaw = (prevRaw + available.y).coerceAtLeast(0f)
                    val consumedRawDelta = nextRaw - prevRaw
                    rawPullPx = nextRaw
                    val nextVisual = rubberBandOffset(nextRaw)
                    coroutineScope.launch {
                        displacementAnim.snapTo(nextVisual)
                    }
                    return Offset(0f, consumedRawDelta)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (!latestEnabled || latestIsRefreshing || source != NestedScrollSource.UserInput) {
                    return Offset.Zero
                }
                if (available.y > 0f && (rawPullPx > 0f || latestIsAtTop())) {
                    rawPullPx = (rawPullPx + available.y).coerceAtLeast(0f)
                    val nextVisual = rubberBandOffset(rawPullPx)
                    coroutineScope.launch {
                        displacementAnim.snapTo(nextVisual)
                    }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (latestIsRefreshing || rawPullPx <= 0f) {
                    return Velocity.Zero
                }
                val pulledVisual = displacementAnim.value
                rawPullPx = 0f
                return if (pulledVisual >= triggerThresholdPx) {
                    latestOnRefresh()
                    coroutineScope.launch {
                        displacementAnim.animateTo(
                            targetValue = refreshHoldPx,
                            animationSpec = returnSpringSpec
                        )
                    }
                    Velocity(0f, available.y)
                } else {
                    coroutineScope.launch {
                        displacementAnim.animateTo(
                            targetValue = 0f,
                            animationSpec = returnSpringSpec
                        )
                    }
                    if (pulledVisual > 2f) Velocity(0f, available.y) else Velocity.Zero
                }
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (!latestIsRefreshing && rawPullPx > 0f) {
                    val pulledVisual = displacementAnim.value
                    rawPullPx = 0f
                    coroutineScope.launch {
                        val target = if (pulledVisual >= triggerThresholdPx) {
                            latestOnRefresh()
                            refreshHoldPx
                        } else {
                            0f
                        }
                        displacementAnim.animateTo(target, returnSpringSpec)
                    }
                }
                return Velocity.Zero
            }
        }
    }

    val pullOffsetPx = displacementAnim.value.coerceAtLeast(0f)
    val pullProgress = if (triggerThresholdPx > 0f) {
        (pullOffsetPx / triggerThresholdPx).coerceIn(0f, 1f)
    } else {
        0f
    }
    val indicatorProgress = if (isRefreshing) 1f else pullProgress
    val showIndicator = isRefreshing || pullOffsetPx > 0.5f

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        // Physically displace the entire scrollable list/grid downward with pullOffsetPx
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = pullOffsetPx
                }
        ) {
            content()
        }

        if (showIndicator) {
            ExpressiveMorphingRefreshIndicator(
                isRefreshing = isRefreshing,
                pullProgress = pullProgress,
                revealProgress = indicatorProgress,
                isDark = isDark,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = headerTopPadding + 8.dp)
                    .testTag(indicatorTestTag)
            )
        }
    }
}

/**
 * Material 3 Expressive Morphing Shape Indicator:
 * - Outer Container: `46.dp` circle filled with subtle translucent Aura Blue tint and `1.dp`
 *   Liquid Crystal border (`Color.White.copy(alpha = 0.55f)`).
 * - Inner Polygon: `24.dp` Aura Blue rotating (`0° -> 360°`) and continuously shape-morphing
 *   polygon cycling through 4 shapes via 72-vertex polar radius interpolation.
 */
@Composable
fun ExpressiveMorphingRefreshIndicator(
    isRefreshing: Boolean,
    pullProgress: Float,
    revealProgress: Float,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val clampedReveal = revealProgress.coerceIn(0f, 1f)
    val clampedPull = pullProgress.coerceIn(0f, 1f)

    val primaryAccent = MaterialTheme.colorScheme.primary
    val containerBg = primaryAccent.copy(alpha = if (isDark) 0.18f else 0.14f)
    val shapeColor = primaryAccent
    val borderColor = Color.White.copy(alpha = 0.55f)

    var fromShapeIndex by remember { mutableIntStateOf(0) }
    var toShapeIndex by remember { mutableIntStateOf(1) }
    val morphAnim = remember { Animatable(0f) }
    var refreshBaseAngleDeg by remember { mutableFloatStateOf(0f) }

    val pullAngleDeg = clampedPull * 300f

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            refreshBaseAngleDeg = pullAngleDeg
            fromShapeIndex = 0
            toShapeIndex = 1
            while (true) {
                morphAnim.snapTo(0f)
                morphAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
                )
                fromShapeIndex = toShapeIndex
                toShapeIndex = (toShapeIndex + 1) % 4
            }
        } else {
            morphAnim.snapTo(0f)
        }
    }

    val spinRotationDeg = if (isRefreshing) {
        val infiniteTransition = rememberInfiniteTransition(label = "expressive_refresh_spin")
        val angle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1600, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "expressive_spin_angle"
        )
        (refreshBaseAngleDeg + angle) % 360f
    } else {
        pullAngleDeg
    }

    val activeFromIndex = if (isRefreshing) fromShapeIndex else 0
    val activeToIndex = if (isRefreshing) toShapeIndex else 1
    val activeMorphT = if (isRefreshing) morphAnim.value else clampedPull
    val pathBuffer = remember { Path() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer {
                scaleX = clampedReveal
                scaleY = clampedReveal
                alpha = clampedReveal
            }
            .size(46.dp)
            .clip(CircleShape)
            .background(containerBg, CircleShape)
            .border(1.dp, borderColor, CircleShape)
    ) {
        Canvas(
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    rotationZ = spinRotationDeg
                }
        ) {
            val radiusPx = size.minDimension * 0.48f
            val cx = size.width * 0.5f
            val cy = size.height * 0.5f
            buildInterpolatedPolarPath(
                targetPath = pathBuffer,
                center = Offset(cx, cy),
                baseRadiusPx = radiusPx,
                fromShapeIndex = activeFromIndex,
                toShapeIndex = activeToIndex,
                t = activeMorphT
            )
            drawPath(
                path = pathBuffer,
                color = shapeColor
            )
        }
    }
}

private const val MORPH_VERTEX_COUNT = 72

/**
 * Evaluates normalized polar radius `r(theta)` across 72 vertices for the 4 M3 Expressive shapes:
 * 0 -> 12-tooth scalloped gear / starburst
 * 1 -> Soft organic rounded blob
 * 2 -> 8-sided soft octagon
 * 3 -> Rounded diamond / squircle
 */
private fun evaluateShapePolarRadius(shapeIndex: Int, theta: Float): Float {
    return when (shapeIndex % 4) {
        0 -> 0.83f + 0.17f * cos(12f * theta)
        1 -> 0.85f + 0.11f * cos(3f * theta) + 0.04f * cos(6f * theta)
        2 -> 0.945f + 0.045f * cos(8f * theta) - 0.010f * cos(16f * theta)
        else -> 0.87f + 0.13f * cos(4f * theta)
    }
}

private fun buildInterpolatedPolarPath(
    targetPath: Path,
    center: Offset,
    baseRadiusPx: Float,
    fromShapeIndex: Int,
    toShapeIndex: Int,
    t: Float
) {
    val clampedT = t.coerceIn(0f, 1f)
    val twoPi = (2.0 * PI).toFloat()
    val xs = FloatArray(MORPH_VERTEX_COUNT)
    val ys = FloatArray(MORPH_VERTEX_COUNT)

    for (i in 0 until MORPH_VERTEX_COUNT) {
        val theta = twoPi * (i.toFloat() / MORPH_VERTEX_COUNT.toFloat())
        val rFrom = evaluateShapePolarRadius(fromShapeIndex, theta)
        val rTo = evaluateShapePolarRadius(toShapeIndex, theta)
        val r = (rFrom + (rTo - rFrom) * clampedT) * baseRadiusPx
        xs[i] = center.x + r * cos(theta)
        ys[i] = center.y + r * sin(theta)
    }

    targetPath.reset()
    val firstMidX = (xs[MORPH_VERTEX_COUNT - 1] + xs[0]) * 0.5f
    val firstMidY = (ys[MORPH_VERTEX_COUNT - 1] + ys[0]) * 0.5f
    targetPath.moveTo(firstMidX, firstMidY)

    for (i in 0 until MORPH_VERTEX_COUNT) {
        val next = (i + 1) % MORPH_VERTEX_COUNT
        val midX = (xs[i] + xs[next]) * 0.5f
        val midY = (ys[i] + ys[next]) * 0.5f
        targetPath.quadraticTo(xs[i], ys[i], midX, midY)
    }
    targetPath.close()
}
