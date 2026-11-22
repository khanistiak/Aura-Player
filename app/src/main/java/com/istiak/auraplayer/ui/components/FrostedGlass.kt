package com.istiak.auraplayer.ui.components

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import com.istiak.auraplayer.HighlightStyle
import com.istiak.auraplayer.LiquidGlassConfig
import com.istiak.auraplayer.LocalLiquidGlassConfig
import com.istiak.auraplayer.ui.theme.LocalExtendedPalette

/**
 * AGSL fragment shader for true optical Liquid Glass edge refraction and RGB chromatic dispersion
 * on Android 13+ (API 33+) via `RenderEffect.createRuntimeShaderEffect`.
 */
private const val LIQUID_GLASS_REFRACTION_AGSL = """
    uniform shader content;
    uniform float2 resolution;
    uniform float cornerRadius;
    uniform float edgeWidthPx;
    uniform float refractionDispPx;
    uniform float chromaticShiftPx;
    uniform float isHeader;
    uniform float depthEffect;

    float sdRoundedBox(float2 p, float2 b, float r) {
        float2 q = abs(p) - b + float2(r);
        return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - r;
    }

    half4 main(float2 fragCoord) {
        float2 maxCoord = max(resolution - float2(1.0), float2(1.0));
        float band = max(edgeWidthPx, 1.0);
        float2 center = resolution * 0.5;
        float r = clamp(cornerRadius, 0.0, min(center.x, center.y));

        float2 n;
        float edgeT;
        float inwardDist = 0.0;
        if (isHeader > 0.5) {
            float opticalBottomY = resolution.y;
            float headerBand = max(band * 1.65, 2.0);
            float distBottom = max(0.0, opticalBottomY - fragCoord.y);
            float bottomT = clamp(1.0 - (distBottom / headerBand), 0.0, 1.0);

            float distLeft = fragCoord.x;
            float distRight = max(0.0, resolution.x - fragCoord.x);
            float distSide = min(distLeft, distRight);
            float sideT = clamp(1.0 - (distSide / band), 0.0, 1.0) * 0.65;

            inwardDist = min(distBottom, distSide);
            if (bottomT >= sideT) {
                edgeT = bottomT;
                n = float2(0.0, 1.0);
            } else {
                edgeT = sideT;
                n = (distLeft < distRight) ? float2(-1.0, 0.0) : float2(1.0, 0.0);
            }
        } else {
            float d = sdRoundedBox(fragCoord - center, center, r);
            inwardDist = max(0.0, -d);
            float eps = 1.0;
            float dx = sdRoundedBox(fragCoord + float2(eps, 0.0) - center, center, r)
                     - sdRoundedBox(fragCoord - float2(eps, 0.0) - center, center, r);
            float dy = sdRoundedBox(fragCoord + float2(0.0, eps) - center, center, r)
                     - sdRoundedBox(fragCoord - float2(0.0, eps) - center, center, r);
            float2 grad = float2(dx, dy);
            float len = length(grad);
            n = (len > 0.0001) ? (grad / len) : float2(0.0, 1.0);
            edgeT = clamp(1.0 - (inwardDist / band), 0.0, 1.0);
        }

        // Depth effect: gentle background parallax separation
        float2 parallaxSample = fragCoord;
        if (depthEffect > 0.001) {
            float depthScale = 1.0 - (0.025 * depthEffect);
            float parallaxY = min(resolution.y * 0.055, 4.0) * depthEffect;
            parallaxSample = center + (fragCoord - center) * depthScale - float2(0.0, parallaxY);
        }

        float lensCurve = edgeT * edgeT;
        float dispMul = (isHeader > 0.5) ? 1.35 : 1.0;
        float depthRefractionBoost = 1.0 + (0.12 * depthEffect);
        float2 samplePos = clamp(
            parallaxSample - n * (lensCurve * refractionDispPx * dispMul * depthRefractionBoost),
            float2(0.0),
            maxCoord
        );

        half4 outColor;
        if (chromaticShiftPx > 0.05 && lensCurve > 0.001) {
            float2 chromaOffset = n * (lensCurve * chromaticShiftPx * dispMul);
            half4 cR = content.eval(clamp(samplePos - chromaOffset, float2(0.0), maxCoord));
            half4 cG = content.eval(samplePos);
            half4 cB = content.eval(clamp(samplePos + chromaOffset, float2(0.0), maxCoord));
            outColor = half4(cR.r, cG.g, cB.b, cG.a);
        } else {
            outColor = content.eval(samplePos);
        }

        // Delicate top-rim specular catchlight only (no interior darkening)
        if (depthEffect > 0.001 && isHeader <= 0.5) {
            float normY = clamp(fragCoord.y / max(resolution.y, 1.0), 0.0, 1.0);
            float rimSpan = max(min(resolution.x, resolution.y) * 0.24, 4.0);
            float rimMask = clamp(1.0 - (inwardDist / rimSpan), 0.0, 1.0);
            float topSpecular = rimMask * rimMask * (1.0 - smoothstep(0.0, 0.34, normY));

            half specAdd = half(clamp(0.10 * topSpecular * depthEffect, 0.0, 0.14));
            outColor.rgb = min(outColor.rgb + half3(specAdd * outColor.a), half3(outColor.a));
        }

        return outColor;
    }
"""

/**
 * Shared real-time `GraphicsLayer` recording state for pure-Compose Frosted / Liquid Crystal surfaces.
 * Tracks `graphicsLayer`, `backgroundGraphicsLayer`, `backdropRootOffset`, `backgroundOriginInRoot`,
 * `lazyListState`, `activeContentLayer`, `scrollTick`, and `targetGlassProgress`.
 */
@Stable
class FrostedBackdropState(
    val graphicsLayer: GraphicsLayer,
    val backgroundGraphicsLayer: GraphicsLayer? = null
) {
    val contentGraphicsLayer: GraphicsLayer
        get() = graphicsLayer

    var backdropRootOffset: Offset by mutableStateOf(Offset.Zero)
    var backgroundOriginInRoot: Offset by mutableStateOf(Offset.Zero)
    var lazyListState: ScrollableState? by mutableStateOf(null)
    var activeContentLayer: GraphicsLayer? by mutableStateOf(null)
    var recordedContentLayer: GraphicsLayer? = null
    var hasRecordedContent: Boolean = false

    val redrawTickState: MutableIntState = mutableIntStateOf(0)
    val scrollProgressState: MutableFloatState = mutableFloatStateOf(0f)

    var scrollTick: Int
        get() = redrawTickState.intValue
        set(value) {
            redrawTickState.intValue = value
        }

    var targetGlassProgress: Float
        get() = scrollProgressState.floatValue
        set(value) {
            scrollProgressState.floatValue = value
        }

    var activeContentGraphicsLayer: GraphicsLayer
        get() = activeContentLayer ?: graphicsLayer
        set(value) {
            if (activeContentLayer !== value) {
                activeContentLayer = value
            }
        }

    var layerOriginInRoot: Offset
        get() = backdropRootOffset
        set(value) {
            if (backdropRootOffset != value) {
                backdropRootOffset = value
            }
        }

    var contentOffsetInRoot: Offset
        get() = backdropRootOffset
        set(value) {
            if (backdropRootOffset != value) {
                backdropRootOffset = value
            }
        }

    var backgroundOffsetInRoot: Offset
        get() = backgroundOriginInRoot
        set(value) {
            if (backgroundOriginInRoot != value) {
                backgroundOriginInRoot = value
            }
        }
}

fun FrostedBackdropState(
    contentGraphicsLayer: GraphicsLayer,
    backgroundGraphicsLayer: GraphicsLayer? = null,
    @Suppress("UNUSED_PARAMETER") legacyNamedOverload: Unit = Unit
): FrostedBackdropState = FrostedBackdropState(
    graphicsLayer = contentGraphicsLayer,
    backgroundGraphicsLayer = backgroundGraphicsLayer
)

val LocalFrostedBackdropState = compositionLocalOf<FrostedBackdropState?> { null }

/**
 * Captures the root ambient gradient background into [FrostedBackdropState.backgroundGraphicsLayer]
 * via `bgLayer.record { drawContent() }` and tracks its root coordinates without per-frame state churn.
 */
fun Modifier.recordFrostedBackground(
    backdropState: FrostedBackdropState?
): Modifier {
    val bgLayer = backdropState?.backgroundGraphicsLayer ?: return this
    return this
        .onGloballyPositioned { coordinates ->
            if (coordinates.isAttached) {
                val pos = coordinates.positionInRoot()
                if (backdropState.backgroundOriginInRoot != pos) {
                    backdropState.backgroundOriginInRoot = pos
                }
            }
        }
        .drawWithContent {
            val w = size.width.toInt().coerceAtLeast(1)
            val h = size.height.toInt().coerceAtLeast(1)
            bgLayer.record(size = IntSize(w, h)) {
                this@drawWithContent.drawContent()
            }
            drawLayer(bgLayer)
        }
}

/**
 * High-FPS Backdrop Recording (`recordFrostedBackdrop`):
 * - Binds metadata (`backdropRootOffset`, `activeContentLayer`, `lazyListState`) inside `onGloballyPositioned`
 *   only when changed.
 * - Performs zero Compose `MutableState` writes inside `drawWithContent`, eliminating per-pixel
 *   recomposition/invalidation loops during 60/120fps `LazyColumn` flings.
 */
fun Modifier.recordFrostedBackdrop(
    backdropState: FrostedBackdropState?,
    lazyListState: ScrollableState? = null,
    contentLayer: GraphicsLayer? = null
): Modifier {
    if (backdropState == null && contentLayer == null) return this
    return this
        .onGloballyPositioned { coordinates ->
            if (coordinates.isAttached && backdropState != null) {
                val pos = coordinates.positionInRoot()
                if (backdropState.backdropRootOffset != pos) {
                    backdropState.backdropRootOffset = pos
                }
                val resolvedLayer = contentLayer ?: backdropState.graphicsLayer
                if (backdropState.activeContentLayer !== resolvedLayer) {
                    backdropState.activeContentLayer = resolvedLayer
                }
                if (backdropState.lazyListState !== lazyListState) {
                    backdropState.lazyListState = lazyListState
                }
            }
        }
        .drawWithContent {
            val targetLayer = contentLayer ?: backdropState?.graphicsLayer
            if (targetLayer != null) {
                val w = size.width.toInt().coerceAtLeast(1)
                val h = size.height.toInt().coerceAtLeast(1)
                targetLayer.record(size = IntSize(w, h)) {
                    this@drawWithContent.drawContent()
                }
                if (backdropState != null) {
                    backdropState.recordedContentLayer = targetLayer
                    backdropState.hasRecordedContent = true
                }
                drawLayer(targetLayer)
            } else {
                drawContent()
            }
        }
}

fun Modifier.recordFrostedBackdrop(
    backdropState: FrostedBackdropState?,
    scrollableState: ScrollableState? = null,
    contentLayerOverride: GraphicsLayer? = null,
    @Suppress("UNUSED_PARAMETER") legacyOverload: Unit = Unit
): Modifier = recordFrostedBackdrop(
    backdropState = backdropState,
    lazyListState = scrollableState,
    contentLayer = contentLayerOverride
)

/**
 * Renders a clean outer drop shadow via `clipPath(path, ClipOp.Difference)` and
 * `frameworkPaint.setShadowLayer(...)` without bleeding into translucent glass surfaces.
 */
fun Modifier.crystalOuterShadow(
    shape: Shape,
    color: Color = Color.Black.copy(alpha = 0.20f),
    blurSpread: Dp = 14.dp,
    offsetY: Dp = 4.dp
): Modifier = this.drawBehind {
    val blurPx = blurSpread.toPx()
    if (blurPx <= 0.5f || color.alpha <= 0.001f) return@drawBehind
    val offsetYPx = offsetY.toPx()
    val outline = shape.createOutline(size, layoutDirection, this)
    val path = Path().apply {
        when (outline) {
            is Outline.Rectangle -> addRect(outline.rect)
            is Outline.Rounded -> addRoundRect(outline.roundRect)
            is Outline.Generic -> addPath(outline.path)
        }
    }
    drawIntoCanvas { canvas ->
        canvas.save()
        canvas.clipPath(path, ClipOp.Difference)
        val shadowPaint = Paint().also { paint ->
            paint.asFrameworkPaint().apply {
                isAntiAlias = true
                this.color = android.graphics.Color.TRANSPARENT
                setShadowLayer(blurPx, 0f, offsetYPx, color.toArgb())
            }
        }
        canvas.drawPath(path, shadowPaint)
        canvas.restore()
    }
}

fun Modifier.crystalOuterShadow(
    shape: Shape,
    isDark: Boolean,
    elevation: Dp = 16.dp
): Modifier {
    val shadowColor = if (isDark) {
        Color.Black.copy(alpha = 0.45f)
    } else {
        Color(0xFF1E3A8A).copy(alpha = 0.20f)
    }
    return this.crystalOuterShadow(
        shape = shape,
        color = shadowColor,
        blurSpread = elevation,
        offsetY = 4.dp
    )
}

/**
 * Holds scroll-aware header measurement, dynamic content padding, and animated glass opacity
 * ramp (`0f` when idle at scroll top -> `1f` over `48.dp` scroll ramp).
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
 * Dynamically measures header height via `headerHeightPxState`, computes
 * `contentPadding = PaddingValues(top = measuredHeaderHeightDp + extraTopGap, bottom = navBarBottomPadding + bottomNavClearance)`,
 * and computes monotonic, hysteresis-free `targetGlassProgress` from cumulative scroll offset
 * (`0f..48f` -> `0f..1f`, smoothed via `tween(durationMillis = 120)`).
 * Scopes `tabContentGraphicsLayer` via `DisposableEffect` while keeping `hasRecordedContent = true`
 * permanently once the first frame records so the header never blinks mid-scroll.
 */
@Composable
fun rememberScrollAwareGlassLayout(
    listState: LazyListState = rememberLazyListState(),
    defaultHeaderHeight: Dp = 72.dp,
    extraTopGap: Dp = 4.dp,
    bottomNavClearance: Dp = 124.dp,
    gridState: LazyGridState? = null,
    useGridState: Boolean = false,
    scrollState: ScrollState? = null,
    useScrollState: Boolean = false,
    bottomPadding: Dp? = null,
    horizontalPadding: Dp = 0.dp
): ScrollAwareGlassLayoutState {
    val density = LocalDensity.current
    val ambientBackdrop = LocalFrostedBackdropState.current
    val tabContentGraphicsLayer = rememberGraphicsLayer()

    val statusBarTopDp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val fallbackHeaderDp = defaultHeaderHeight + statusBarTopDp
    val headerHeightPxState = remember { mutableIntStateOf(0) }

    val measuredHeaderHeightDp = if (headerHeightPxState.intValue > 0) {
        with(density) { headerHeightPxState.intValue.toDp() }
    } else {
        fallbackHeaderDp
    }

    val topContentPadding = measuredHeaderHeightDp + extraTopGap
    val effectiveBottomPadding = bottomPadding ?: (navBarBottomPadding + bottomNavClearance)
    val activeScrollableState: ScrollableState = when {
        useScrollState && scrollState != null -> scrollState
        useGridState && gridState != null -> gridState
        else -> listState
    }

    val targetGlassProgress by remember(
        listState,
        gridState,
        useGridState,
        scrollState,
        useScrollState
    ) {
        derivedStateOf {
            val scrolledPx = when {
                useScrollState && scrollState != null -> scrollState.value.toFloat()
                useGridState && gridState != null -> {
                    if (gridState.firstVisibleItemIndex > 0) 100f
                    else gridState.firstVisibleItemScrollOffset.toFloat()
                }
                else -> {
                    if (listState.firstVisibleItemIndex > 0) 100f
                    else listState.firstVisibleItemScrollOffset.toFloat()
                }
            }
            (scrolledPx / 48f).coerceIn(0f, 1f)
        }
    }

    val animatedGlassProgress by animateFloatAsState(
        targetValue = targetGlassProgress,
        animationSpec = tween(durationMillis = 120),
        label = "scroll_aware_glass_progress"
    )

    DisposableEffect(activeScrollableState, tabContentGraphicsLayer) {
        if (ambientBackdrop != null) {
            ambientBackdrop.activeContentLayer = tabContentGraphicsLayer
            ambientBackdrop.lazyListState = activeScrollableState
            ambientBackdrop.targetGlassProgress = targetGlassProgress
        }
        onDispose {
            if (ambientBackdrop != null && ambientBackdrop.activeContentLayer === tabContentGraphicsLayer) {
                ambientBackdrop.activeContentLayer = null
                ambientBackdrop.targetGlassProgress = 0f
            }
        }
    }

    SideEffect {
        if (ambientBackdrop != null) {
            if (ambientBackdrop.activeContentLayer !== tabContentGraphicsLayer) {
                ambientBackdrop.activeContentLayer = tabContentGraphicsLayer
            }
            if (ambientBackdrop.lazyListState !== activeScrollableState) {
                ambientBackdrop.lazyListState = activeScrollableState
            }
            ambientBackdrop.targetGlassProgress = animatedGlassProgress
        }
    }

    val resolvedPadding = remember(horizontalPadding, topContentPadding, effectiveBottomPadding) {
        PaddingValues(
            start = horizontalPadding,
            end = horizontalPadding,
            top = topContentPadding,
            bottom = effectiveBottomPadding
        )
    }

    return remember(
        ambientBackdrop,
        tabContentGraphicsLayer,
        measuredHeaderHeightDp,
        topContentPadding,
        resolvedPadding,
        animatedGlassProgress
    ) {
        ScrollAwareGlassLayoutState(
            backdropState = ambientBackdrop,
            contentGraphicsLayer = tabContentGraphicsLayer,
            headerHeightPxState = headerHeightPxState,
            measuredHeaderHeightDp = measuredHeaderHeightDp,
            topContentPadding = topContentPadding,
            contentPadding = resolvedPadding,
            glassProgress = animatedGlassProgress
        )
    }
}

/**
 * Anchors full-width at [Alignment.TopCenter] directly inside the root `Box` with
 * `RoundedCornerShape(0.dp)`, `border = null`, `isTopHeader = true`, and `statusBarsPadding()`
 * wrapping 100% of the header content (title row, action icons, and breadcrumb bar) inside a single
 * cut-to-cut [FrostedGlassSurface] spanning `0f` to `size.height`.
 * Driven live by [LocalLiquidGlassConfig]:
 * - Full-coverage `BlurEffect` and `palette.ambientTopLeft` tint run 100% uniformly from `y = 0f`
 *   to `y = size.height` with zero bottom fade, zero unblurred strip, and zero white edge line.
 * - Ends cleanly at `y = size.height` with a whisper-subtle `0.5.dp` `outlineVariant` hairline
 *   divider (`alpha = 0.22f * effectiveGlassProgress`).
 * - When `!config.enabled || !config.headerLiquidGlassEnabled`: stays completely transparent
 *   (`alpha = 0f`) at scroll rest (`effectiveGlassProgress == 0f`) so the root ambient gradient
 *   shows through uninterrupted, and smoothly fades in a soft tinted white/slate surface
 *   (`0.92f * effectiveGlassProgress`) with the `0.5.dp` hairline divider as the user scrolls.
 */
@Composable
fun BoxScope.ScrollAwareFixedHeader(
    layoutState: ScrollAwareGlassLayoutState,
    modifier: Modifier = Modifier,
    blurRadius: Dp = 28.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val glassConfig = LocalLiquidGlassConfig.current
    val palette = LocalExtendedPalette.current
    val isDarkHeader = palette.isDark
    val topAmbientBg = palette.ambientTopLeft
    val isHeaderLiveGlass = glassConfig.enabled && glassConfig.headerLiquidGlassEnabled
    val headerGlassProgress = layoutState.glassProgress

    val frostNorm = glassConfig.frostPercent.coerceIn(0f, 1f)
    val backdropOpacityNorm = glassConfig.backdropOpacity.coerceIn(0f, 1f)

    val headerBaseBg = if (isHeaderLiveGlass) {
        topAmbientBg.copy(
            alpha = ((0.18f + 0.44f * frostNorm) * backdropOpacityNorm).coerceIn(0f, 0.96f)
        )
    } else {
        if (isDarkHeader) {
            Color(0xFF0F172A).copy(alpha = 0.92f)
        } else {
            Color(0xFFF7F9FE).copy(alpha = 0.92f)
        }
    }

    FrostedGlassSurface(
        backdropState = layoutState.backdropState ?: LocalFrostedBackdropState.current,
        contentLayerOverride = layoutState.contentGraphicsLayer,
        glassProgressOverride = headerGlassProgress,
        shape = RoundedCornerShape(0.dp),
        border = null,
        blurRadius = blurRadius,
        isTopHeader = true,
        baseBackgroundColor = headerBaseBg,
        specularOverlayBrush = null,
        modifier = modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                val h = coordinates.size.height
                if (h > 0 && layoutState.headerHeightPxState.intValue != h) {
                    layoutState.headerHeightPxState.intValue = h
                }
            }
    ) {
        Box(
            contentAlignment = Alignment.TopStart,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
            content = content
        )
    }
}

/**
 * Upgraded Real-Time Optical Liquid Glass Surface (`FrostedGlassSurface`):
 * Driven live by [LocalLiquidGlassConfig] across BOTH cut-to-cut top headers (`isTopHeader = true`,
 * full `0f` to `size.height` blur & tint coverage) and floating capsules (`LiquidGlassBottomNavPill`,
 * quick-resume Play button, etc.):
 * - `enabled` & `headerLiquidGlassEnabled`: toggles full optical glass or clean non-refractive fallback.
 * - `frostPercent` (`0%..100%`, default `20%`): couples BOTH the `BlurEffect` radius
 *   (`effectiveBlurDp = 28.dp * config.frostPercent`) AND the milky frost haze tint alpha
 *   (`(0.04f + 0.61f * frostPercent) * backdropOpacity`) so `0%` Frost is crystal-clear transparent
 *   liquid water and `100%` Frost is dense `28.dp` frosted glass.
 * - `refractionEdgeWidth`, `refractionStrength`, `chromaticAberration`, and `Highlights`:
 *   computed and rendered cleanly on both cut-to-cut top headers and closed floating capsules.
 */
@Composable
fun FrostedGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    blurRadius: Dp = 28.dp,
    border: BorderStroke? = null,
    featherBottomEdge: Boolean = false,
    glassProgressOverride: Float? = null,
    baseBackgroundColor: Color? = null,
    specularOverlayBrush: Brush? = null,
    contentLayerOverride: GraphicsLayer? = null,
    backdropState: FrostedBackdropState? = LocalFrostedBackdropState.current,
    configOverride: LiquidGlassConfig? = null,
    isTopHeader: Boolean = featherBottomEdge,
    content: @Composable BoxScope.() -> Unit
) {
    val glassConfig = configOverride ?: LocalLiquidGlassConfig.current
    val palette = LocalExtendedPalette.current
    val isDarkHeader = palette.isDark
    val topAmbientBg = palette.ambientTopLeft
    val surfaceContainerHigh = MaterialTheme.colorScheme.surfaceContainerHigh
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val density = LocalDensity.current
    var surfaceRootOffset by remember { mutableStateOf(Offset.Zero) }
    var surfaceSize by remember { mutableStateOf(IntSize.Zero) }

    val isFullWidthHeader = isTopHeader || featherBottomEdge || shape == RoundedCornerShape(0.dp)
    val isLiveGlassActive = if (isFullWidthHeader) {
        glassConfig.enabled && glassConfig.headerLiquidGlassEnabled
    } else {
        glassConfig.enabled
    }

    val effectiveGlassProgress = (glassProgressOverride
        ?: backdropState?.targetGlassProgress
        ?: 1f).coerceIn(0f, 1f)

    // Detect solid fallback surface (e.g., Play button when Liquid glass is OFF)
    val isSolidFallbackCapsule = !isLiveGlassActive &&
        !isFullWidthHeader &&
        baseBackgroundColor != null &&
        baseBackgroundColor.alpha >= 0.95f

    // Dynamic Frost (0%..100%) & Backdrop Opacity driven by LiquidGlassConfig
    val frostNorm = glassConfig.frostPercent.coerceIn(0f, 1f)
    val targetMaxBlurDp = if (blurRadius <= 0.dp) 0.dp else 28.dp
    val effectiveBlurDp = when {
        isLiveGlassActive && targetMaxBlurDp > 0.dp ->
            (28.dp * frostNorm) * effectiveGlassProgress
        !isLiveGlassActive && !isFullWidthHeader && !isSolidFallbackCapsule && targetMaxBlurDp > 0.dp ->
            14.dp * effectiveGlassProgress
        else -> 0.dp
    }
    val blurPx = with(density) { effectiveBlurDp.toPx() }
    val backdropAlphaScale = if (isLiveGlassActive) {
        glassConfig.backdropOpacity.coerceIn(0f, 1f)
    } else {
        1f
    }
    val layerBackdropAlpha = (effectiveGlassProgress * backdropAlphaScale).coerceIn(0f, 1f)

    // Color grading ColorMatrix (vibrancy, saturation, brightness, contrast) applied to recorded backdrop
    val isColorGradingActive = isLiveGlassActive && (
        glassConfig.vibrancyEnabled ||
            abs(glassConfig.saturation - 1f) > 0.005f ||
            abs(glassConfig.brightness) > 0.005f ||
            abs(glassConfig.contrast - 1f) > 0.005f
        )
    val backdropComposeMatrix = remember(
        isColorGradingActive,
        glassConfig.vibrancyEnabled,
        glassConfig.saturation,
        glassConfig.brightness,
        glassConfig.contrast
    ) {
        if (!isColorGradingActive) {
            null
        } else {
            val effectiveSat = (glassConfig.saturation.coerceIn(0f, 2f) *
                if (glassConfig.vibrancyEnabled) 1.20f else 1.0f).coerceIn(0f, 2.5f)
            val c = glassConfig.contrast.coerceIn(0.50f, 1.50f)
            val b = glassConfig.brightness.coerceIn(-0.50f, 0.50f) +
                if (glassConfig.vibrancyEnabled) 0.02f else 0f
            val translateVal = (-0.5f * c + 0.5f + b) * 255f
            val satMatrix = ColorMatrix().apply { setToSaturation(effectiveSat) }
            val bcMatrix = ColorMatrix(
                floatArrayOf(
                    c, 0f, 0f, 0f, translateVal,
                    0f, c, 0f, 0f, translateVal,
                    0f, 0f, c, 0f, translateVal,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            bcMatrix.timesAssign(satMatrix)
            bcMatrix
        }
    }

    val refractionEdgeWidthPx = with(density) {
        (10.dp * glassConfig.refractionEdgeWidth.coerceIn(0f, 2f)).toPx()
    }
    val refractionStrengthNorm = if (isLiveGlassActive) {
        glassConfig.refractionStrength.coerceIn(0f, 2f)
    } else {
        0f
    }
    val refractionDispPx = with(density) { (7.dp * refractionStrengthNorm).toPx() }
    val chromaticShiftPx = if (isLiveGlassActive && glassConfig.chromaticAberration) {
        with(density) { (1.5.dp * refractionStrengthNorm.coerceAtLeast(0.35f)).toPx() }
    } else {
        0f
    }

    val highlightStrengthNorm = glassConfig.highlightStrength.coerceIn(0f, 2f)
    val highlightWidthNorm = glassConfig.highlightWidth.coerceIn(0f, 2f)
    val highlightSoftnessNorm = glassConfig.highlightSoftness.coerceIn(0f, 2f)
    val dynamicStrokeDp = (0.5f + 1.0f * highlightWidthNorm).coerceIn(0.4f, 2.5f).dp

    // Milky Frost Haze Alpha:
    // At 0% Frost -> 0.02f * backdropOpacity (crystal-clear liquid water).
    // Smoothly increases to 0.60f * backdropOpacity at 100% Frost.
    val milkyHazeAlpha = ((0.02f + 0.58f * frostNorm) * backdropAlphaScale * effectiveGlassProgress)
        .coerceIn(0f, 0.88f)

    val activeBaseBackgroundColor = when {
        isFullWidthHeader && !isLiveGlassActive -> {
            // Smoothly fades from 0f transparent at rest (effectiveGlassProgress == 0f)
            // to soft translucent tinted white/slate (0.92f) on scroll without blur/refraction
            val defaultOffTint = if (isDarkHeader) {
                Color(0xFF0F172A).copy(alpha = 0.92f)
            } else {
                Color(0xFFF7F9FE).copy(alpha = 0.92f)
            }
            val base = baseBackgroundColor ?: defaultOffTint
            base.copy(alpha = (base.alpha * effectiveGlassProgress).coerceIn(0f, 0.96f))
        }
        isTopHeader && isLiveGlassActive -> {
            // Top ambient gradient matched header tint (never stark white)
            val base = baseBackgroundColor ?: topAmbientBg.copy(
                alpha = ((0.18f + 0.44f * frostNorm) * backdropAlphaScale).coerceIn(0f, 0.96f)
            )
            base.copy(alpha = (base.alpha * effectiveGlassProgress).coerceIn(0f, 0.96f))
        }
        isSolidFallbackCapsule -> {
            baseBackgroundColor!!.copy(alpha = 1.0f)
        }
        !isLiveGlassActive -> {
            // Standard frosted Material capsule fallback (surfaceContainerHigh at 0.88f alpha)
            surfaceContainerHigh.copy(alpha = (0.88f * effectiveGlassProgress).coerceIn(0f, 0.94f))
        }
        baseBackgroundColor != null -> {
            // Custom tinted glass surface (e.g., Quick-Resume Play Button primary accent tint)
            baseBackgroundColor.copy(
                alpha = (baseBackgroundColor.alpha * backdropAlphaScale * effectiveGlassProgress).coerceIn(0f, 0.95f)
            )
        }
        isDarkHeader -> {
            Color(0xFF0F172A).copy(alpha = milkyHazeAlpha)
        }
        else -> {
            // Crystal-clear cool ambient tint for closed capsules (never stark opaque white)
            // so blurred backdrop content remains 100% uniform across 0f..size.width.
            val capsuleTintAlpha = ((0.01f + 0.26f * frostNorm) * backdropAlphaScale * effectiveGlassProgress)
                .coerceIn(0f, 0.42f)
            topAmbientBg.copy(alpha = capsuleTintAlpha)
        }
    }

    val softnessSpread = (0.28f + 0.36f * highlightSoftnessNorm).coerceIn(0.18f, 0.95f)
    val sheenAlphaMul = when (glassConfig.highlightStyle) {
        HighlightStyle.MINIMAL -> 0.35f * highlightStrengthNorm
        HighlightStyle.TOP_SHEEN -> 1.15f * highlightStrengthNorm
        HighlightStyle.SPECULAR_RIM -> 0.85f * highlightStrengthNorm
        HighlightStyle.COMPONENT_DEFAULT -> 0.85f * highlightStrengthNorm
    }.coerceIn(0f, 1.8f)

    // Couple interior sheen overlay to frostNorm while keeping the center crystal-clear
    // so both halves of the bottom navigation pill refract the scrolling backdrop uniformly.
    val interiorSheenScale = (0.08f + 0.42f * frostNorm).coerceIn(0.06f, 0.65f)

    val useCustomHighlightOverlay = glassConfig.highlightStyle != HighlightStyle.COMPONENT_DEFAULT ||
        highlightStrengthNorm !in 0.98f..1.02f ||
        highlightSoftnessNorm !in 0.98f..1.02f
    val capsuleOverlayBrush = when {
        specularOverlayBrush != null && !useCustomHighlightOverlay -> specularOverlayBrush
        glassConfig.highlightStyle == HighlightStyle.TOP_SHEEN -> Brush.verticalGradient(
            0.0f to Color.White.copy(alpha = (0.28f * sheenAlphaMul * (0.25f + 0.75f * frostNorm)).coerceIn(0f, 0.45f)),
            (softnessSpread * 0.45f).coerceIn(0.10f, 0.36f) to Color.Transparent,
            1.0f to Color.Transparent
        )
        isDarkHeader -> Brush.verticalGradient(
            0.0f to Color.White.copy(alpha = (0.18f * sheenAlphaMul * interiorSheenScale).coerceIn(0f, 0.45f)),
            0.34f to Color.Transparent,
            0.76f to Color.Transparent,
            1.0f to palette.accentPrimary.copy(alpha = (0.10f * sheenAlphaMul * interiorSheenScale).coerceIn(0f, 0.30f))
        )
        else -> Brush.verticalGradient(
            0.0f to Color.White.copy(alpha = (0.14f * sheenAlphaMul * interiorSheenScale).coerceIn(0f, 0.24f)),
            0.22f to Color.Transparent,
            1.0f to Color.Transparent
        )
    }

    val ambientFallbackBrush = remember(
        palette.ambientTopLeft,
        palette.ambientTopRight,
        palette.ambientBase,
        isTopHeader
    ) {
        if (isTopHeader) {
            Brush.verticalGradient(listOf(palette.ambientTopLeft, palette.ambientTopRight))
        } else {
            Brush.verticalGradient(
                listOf(
                    palette.ambientTopLeft,
                    palette.ambientTopRight,
                    palette.ambientBase
                )
            )
        }
    }

    // Compute resolved border Brush & width dynamically from highlight settings
    val resolvedBorder: BorderStroke? = when {
        isFullWidthHeader -> null
        isSolidFallbackCapsule -> null
        !isLiveGlassActive -> BorderStroke(
            width = 1.dp,
            color = outlineVariant.copy(alpha = 0.35f)
        )
        border == null -> null
        glassConfig.highlightStyle == HighlightStyle.COMPONENT_DEFAULT &&
            highlightStrengthNorm in 0.98f..1.02f &&
            highlightWidthNorm in 0.98f..1.02f -> border
        else -> {
            val baseAlpha = (0.72f * highlightStrengthNorm).coerceIn(0f, 1f)
            val rimColors = when (glassConfig.highlightStyle) {
                HighlightStyle.MINIMAL -> listOf(
                    Color.White.copy(alpha = baseAlpha * 0.32f),
                    Color.White.copy(alpha = baseAlpha * 0.14f)
                )
                HighlightStyle.TOP_SHEEN -> listOf(
                    Color.White.copy(alpha = (baseAlpha * 1.1f).coerceIn(0f, 1f)),
                    Color.White.copy(alpha = baseAlpha * 0.25f),
                    Color.Transparent
                )
                HighlightStyle.SPECULAR_RIM -> listOf(
                    Color.White.copy(alpha = (baseAlpha * 1.15f).coerceIn(0f, 1f)),
                    palette.accentPrimary.copy(alpha = baseAlpha * 0.42f),
                    Color.White.copy(alpha = (baseAlpha * 0.92f).coerceIn(0f, 1f))
                )
                HighlightStyle.COMPONENT_DEFAULT -> listOf(
                    Color.White.copy(alpha = if (isDarkHeader) baseAlpha * 0.42f else baseAlpha * 0.95f),
                    palette.outlineBorder.copy(alpha = (baseAlpha * 0.45f).coerceIn(0f, 0.85f)),
                    palette.accentPrimary.copy(alpha = (baseAlpha * 0.36f).coerceIn(0f, 0.80f))
                )
            }
            val rimBrush = if (glassConfig.highlightStyle == HighlightStyle.TOP_SHEEN) {
                Brush.verticalGradient(rimColors)
            } else {
                Brush.linearGradient(rimColors)
            }
            BorderStroke(dynamicStrokeDp, rimBrush)
        }
    }

    // Android 13+ AGSL RuntimeShader instance (remembered and updated cleanly)
    val agslRuntimeShader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching { RuntimeShader(LIQUID_GLASS_REFRACTION_AGSL) }.getOrNull()
        } else {
            null
        }
    }

    val approxCornerRadiusPx = remember(shape, surfaceSize, density) {
        when (shape) {
            CircleShape -> minOf(surfaceSize.width, surfaceSize.height) * 0.5f
            RectangleShape -> 0f
            else -> {
                if (surfaceSize.width > 0 && surfaceSize.height > 0) {
                    val outline = shape.createOutline(
                        Size(surfaceSize.width.toFloat(), surfaceSize.height.toFloat()),
                        androidx.compose.ui.unit.LayoutDirection.Ltr,
                        density
                    )
                    when (outline) {
                        is Outline.Rounded -> outline.roundRect.topLeftCornerRadius.x
                        else -> with(density) { 24.dp.toPx() }
                    }
                } else {
                    with(density) { 24.dp.toPx() }
                }
            }
        }
    }

    val depthOuterModifier = if (
        isLiveGlassActive &&
        glassConfig.depthEffect &&
        !isFullWidthHeader &&
        effectiveGlassProgress > 0.05f
    ) {
        Modifier.crystalOuterShadow(
            shape = shape,
            color = if (isDarkHeader) {
                Color.Black.copy(alpha = 0.42f * effectiveGlassProgress)
            } else {
                Color(0xFF0F172A).copy(alpha = 0.18f * effectiveGlassProgress)
            },
            blurSpread = 20.dp,
            offsetY = 7.dp
        )
    } else {
        Modifier
    }

    Box(
        contentAlignment = Alignment.TopStart,
        modifier = modifier
            .then(if (isTopHeader) Modifier.fillMaxWidth() else Modifier)
            .then(depthOuterModifier)
            .onSizeChanged { surfaceSize = it }
            .onGloballyPositioned { coordinates ->
                if (coordinates.isAttached) {
                    val pos = coordinates.positionInRoot()
                    if (surfaceRootOffset != pos) {
                        surfaceRootOffset = pos
                    }
                }
            }
            .clip(shape)
            .then(if (resolvedBorder != null) Modifier.border(resolvedBorder, shape) else Modifier)
    ) {
        if (effectiveGlassProgress > 0.001f) {
            Box(
                contentAlignment = Alignment.TopStart,
                modifier = Modifier.matchParentSize()
            ) {
                if ((!isLiveGlassActive && isFullWidthHeader) || isSolidFallbackCapsule) {
                    // Scroll-aware soft tinted surface (cut-to-cut + subtle 0.5.dp outlineVariant hairline)
                    // or solid primary Play button when Liquid Glass is OFF
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .drawBehind {
                                drawRect(color = activeBaseBackgroundColor)
                                if (isFullWidthHeader && effectiveGlassProgress > 0.01f) {
                                    val strokePx = 0.5.dp.toPx()
                                    val hairlineColor = outlineVariant.copy(
                                        alpha = (0.22f * effectiveGlassProgress).coerceIn(0f, 1f)
                                    )
                                    drawLine(
                                        color = hairlineColor,
                                        start = Offset(0f, size.height - strokePx * 0.5f),
                                        end = Offset(size.width, size.height - strokePx * 0.5f),
                                        strokeWidth = strokePx
                                    )
                                }
                            }
                    )
                } else {
                    // Edge-to-edge GraphicsLayer backdrop with cached RenderEffect / BlurEffect(TileMode.Clamp)
                    val quantizedBlurPx = ((blurPx * 2f).toInt() / 2f).coerceAtLeast(0f)
                    val cachedHeaderOrFallbackEffect = remember(
                        quantizedBlurPx,
                        isLiveGlassActive,
                        backdropComposeMatrix
                    ) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val colorFilterEffect = if (isLiveGlassActive) {
                                backdropComposeMatrix?.let { cm ->
                                    android.graphics.RenderEffect.createColorFilterEffect(
                                        android.graphics.ColorMatrixColorFilter(
                                            android.graphics.ColorMatrix(cm.values)
                                        )
                                    )
                                }
                            } else {
                                null
                            }
                            val blurNativeEffect = if (quantizedBlurPx > 0.5f) {
                                android.graphics.RenderEffect.createBlurEffect(
                                    quantizedBlurPx,
                                    quantizedBlurPx,
                                    android.graphics.Shader.TileMode.CLAMP
                                )
                            } else {
                                null
                            }
                            val finalNative = when {
                                blurNativeEffect != null && colorFilterEffect != null ->
                                    android.graphics.RenderEffect.createChainEffect(
                                        colorFilterEffect,
                                        blurNativeEffect
                                    )
                                blurNativeEffect != null -> blurNativeEffect
                                colorFilterEffect != null -> colorFilterEffect
                                else -> null
                            }
                            finalNative?.asComposeRenderEffect()
                        } else {
                            null
                        }
                    }

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clipToBounds()
                            .graphicsLayer {
                                val layerW = size.width
                                val layerH = size.height
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    // Top headers always use full-width BlurEffect without AGSL RuntimeShader
                                    // so the blur spans 100% of screen width and height uniformly with zero seam.
                                    val canUseAgsl = !isFullWidthHeader &&
                                        isLiveGlassActive &&
                                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        agslRuntimeShader != null &&
                                        layerW > 1f &&
                                        layerH > 1f &&
                                        (refractionDispPx > 0.25f || chromaticShiftPx > 0.05f || glassConfig.depthEffect)

                                    if (canUseAgsl && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        val agslComposeEffect = runCatching {
                                            val shader = agslRuntimeShader!!
                                            val liveCornerR = if (shape == CircleShape) {
                                                minOf(layerW, layerH) * 0.5f
                                            } else {
                                                approxCornerRadiusPx.coerceAtMost(minOf(layerW, layerH) * 0.5f)
                                            }
                                            shader.setFloatUniform("resolution", layerW, layerH)
                                            shader.setFloatUniform("cornerRadius", liveCornerR)
                                            shader.setFloatUniform("edgeWidthPx", refractionEdgeWidthPx)
                                            shader.setFloatUniform("refractionDispPx", refractionDispPx)
                                            shader.setFloatUniform("chromaticShiftPx", chromaticShiftPx)
                                            shader.setFloatUniform("isHeader", 0f)
                                            shader.setFloatUniform(
                                                "depthEffect",
                                                if (glassConfig.depthEffect) effectiveGlassProgress else 0f
                                            )
                                            val shaderEffect = android.graphics.RenderEffect.createRuntimeShaderEffect(
                                                shader,
                                                "content"
                                            )
                                            val blurNativeEffect = if (quantizedBlurPx > 0.5f) {
                                                android.graphics.RenderEffect.createBlurEffect(
                                                    quantizedBlurPx,
                                                    quantizedBlurPx,
                                                    android.graphics.Shader.TileMode.CLAMP
                                                )
                                            } else {
                                                null
                                            }
                                            val chained = if (blurNativeEffect != null) {
                                                android.graphics.RenderEffect.createChainEffect(
                                                    shaderEffect,
                                                    blurNativeEffect
                                                )
                                            } else {
                                                shaderEffect
                                            }
                                            val colorFilterEffect = backdropComposeMatrix?.let { cm ->
                                                android.graphics.RenderEffect.createColorFilterEffect(
                                                    android.graphics.ColorMatrixColorFilter(
                                                        android.graphics.ColorMatrix(cm.values)
                                                    )
                                                )
                                            }
                                            val finalEffect = if (colorFilterEffect != null) {
                                                android.graphics.RenderEffect.createChainEffect(
                                                    colorFilterEffect,
                                                    chained
                                                )
                                            } else {
                                                chained
                                            }
                                            finalEffect.asComposeRenderEffect()
                                        }.getOrNull()
                                        renderEffect = agslComposeEffect ?: cachedHeaderOrFallbackEffect
                                    } else {
                                        renderEffect = cachedHeaderOrFallbackEffect
                                    }
                                } else {
                                    renderEffect = null
                                }
                                alpha = layerBackdropAlpha
                            }
                            .drawBehind {
                                when (val scrollState = backdropState?.lazyListState) {
                                    is LazyListState -> {
                                        scrollState.firstVisibleItemIndex
                                        scrollState.firstVisibleItemScrollOffset
                                    }
                                    is LazyGridState -> {
                                        scrollState.firstVisibleItemIndex
                                        scrollState.firstVisibleItemScrollOffset
                                    }
                                    is ScrollState -> {
                                        scrollState.value
                                    }
                                }

                                // 1. Fill 0f, 0f to size.width, size.height completely with ambient theme background
                                drawRect(
                                    brush = ambientFallbackBrush,
                                    topLeft = Offset.Zero,
                                    size = size
                                )

                                // 2 & 3. Clip BOTH background and content layers symmetrically to [0f..size.height]
                                // so BlurEffect(TileMode.CLAMP) extends evenly to the exact cut-to-cut edge without
                                // unoccluded background below size.height washing out the bottom of the header.
                                val bgLayer = backdropState?.backgroundGraphicsLayer
                                val candidateLayer = contentLayerOverride ?: backdropState?.activeContentLayer
                                val isLayerValid = if (contentLayerOverride != null) {
                                    backdropState == null || backdropState.hasRecordedContent
                                } else {
                                    backdropState != null &&
                                        backdropState.hasRecordedContent &&
                                        candidateLayer != null
                                }

                                drawIntoCanvas { canvas ->
                                    canvas.save()
                                    canvas.clipRect(
                                        left = 0f,
                                        top = 0f,
                                        right = size.width,
                                        bottom = size.height
                                    )
                                    val depthActive = isLiveGlassActive &&
                                        glassConfig.depthEffect &&
                                        !isFullWidthHeader &&
                                        effectiveGlassProgress > 0.01f
                                    val depthFactor = if (depthActive) effectiveGlassProgress else 0f
                                    val pivotCenter = Offset(size.width * 0.5f, size.height * 0.5f)

                                    if (bgLayer != null) {
                                        val bgOrigin = backdropState.backgroundOriginInRoot
                                        val bgParallaxY = if (depthActive) 1.5.dp.toPx() * depthFactor else 0f
                                        val bgDx = bgOrigin.x - surfaceRootOffset.x
                                        val bgDy = bgOrigin.y - surfaceRootOffset.y + bgParallaxY
                                        if (depthActive) {
                                            val bgScale = 1f + 0.015f * depthFactor
                                            scale(
                                                scaleX = bgScale,
                                                scaleY = bgScale,
                                                pivot = pivotCenter
                                            ) {
                                                translate(left = bgDx, top = bgDy) {
                                                    drawLayer(bgLayer)
                                                }
                                            }
                                        } else {
                                            translate(left = bgDx, top = bgDy) {
                                                drawLayer(bgLayer)
                                            }
                                        }
                                    }
                                    if (candidateLayer != null && isLayerValid) {
                                        val contentOrigin = backdropState?.backdropRootOffset ?: Offset.Zero
                                        val contentParallaxY = if (depthActive) 3.5.dp.toPx() * depthFactor else 0f
                                        val dx = contentOrigin.x - surfaceRootOffset.x
                                        val dy = contentOrigin.y - surfaceRootOffset.y + contentParallaxY
                                        if (depthActive) {
                                            val contentScale = 1f + 0.04f * depthFactor
                                            scale(
                                                scaleX = contentScale,
                                                scaleY = contentScale,
                                                pivot = pivotCenter
                                            ) {
                                                translate(left = dx, top = dy) {
                                                    drawLayer(candidateLayer)
                                                }
                                            }
                                        } else {
                                            translate(left = dx, top = dy) {
                                                drawLayer(candidateLayer)
                                            }
                                        }
                                    }
                                    canvas.restore()
                                }

                                // 4. Fill 0f, 0f to size.width, size.height completely with activeBaseBackgroundColor
                                drawRect(
                                    color = activeBaseBackgroundColor,
                                    topLeft = Offset.Zero,
                                    size = size
                                )
                            }
                    )

                    // Uniform edge-to-edge Liquid Glass sheen overlay (closed capsules or explicit custom brush only;
                    // ScrollAwareFixedHeader has zero white overlay gradient)
                    if (isLiveGlassActive && (!isFullWidthHeader || specularOverlayBrush != null)) {
                        val overlayBrushToUse = specularOverlayBrush ?: capsuleOverlayBrush
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .graphicsLayer {
                                    alpha = effectiveGlassProgress *
                                        (0.35f + 0.65f * highlightStrengthNorm.coerceIn(0f, 1.5f))
                                }
                                .background(overlayBrushToUse)
                        )
                    }
                }

                // Header bottom edge: whisper-subtle 0.5.dp outlineVariant hairline flush at y = size.height.
                // Closed capsules retain full perimeter Liquid Glass optical rim reflection.
                if (isFullWidthHeader) {
                    if (isLiveGlassActive && effectiveGlassProgress > 0.01f) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .drawBehind {
                                    val strokePx = 0.5.dp.toPx()
                                    val lineY = (size.height - strokePx * 0.5f).coerceAtLeast(strokePx * 0.5f)
                                    drawLine(
                                        color = outlineVariant.copy(
                                            alpha = (0.22f * effectiveGlassProgress).coerceIn(0f, 1f)
                                        ),
                                        start = Offset(0f, lineY),
                                        end = Offset(size.width, lineY),
                                        strokeWidth = strokePx
                                    )
                                }
                        )
                    }
                } else if (
                    isLiveGlassActive &&
                    effectiveGlassProgress > 0.01f &&
                    (
                        glassConfig.chromaticAberration ||
                            glassConfig.depthEffect ||
                            refractionStrengthNorm > 0.01f ||
                            highlightStrengthNorm > 0.01f
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .drawBehind {
                                drawLiquidGlassEdgeReflection(
                                    glassConfig = glassConfig,
                                    isDarkHeader = isDarkHeader,
                                    effectiveGlassProgress = effectiveGlassProgress,
                                    refractionEdgeWidthPx = refractionEdgeWidthPx,
                                    refractionStrengthNorm = refractionStrengthNorm,
                                    highlightStrengthNorm = highlightStrengthNorm,
                                    approxCornerRadiusPx = approxCornerRadiusPx
                                )
                            }
                    )
                }
            }
        }

        // Sharp Foreground Content
        content()
    }
}

/**
 * Optical Liquid Glass perimeter reflection rendered on closed floating capsules
 * (`LiquidGlassBottomNavPill`, floating buttons, etc.): ambient depth occlusion,
 * dual-tone caustic rim, and optional chromatic dispersion.
 */
private fun DrawScope.drawLiquidGlassEdgeReflection(
    glassConfig: LiquidGlassConfig,
    isDarkHeader: Boolean,
    effectiveGlassProgress: Float,
    refractionEdgeWidthPx: Float,
    refractionStrengthNorm: Float,
    highlightStrengthNorm: Float,
    approxCornerRadiusPx: Float
) {
    val w = size.width
    val h = size.height
    if (w <= 1f || h <= 1f) return
    val cornerR = approxCornerRadiusPx.coerceAtMost(minOf(w, h) * 0.5f)
    val edgeBand = refractionEdgeWidthPx.coerceIn(1.dp.toPx(), minOf(w, h) * 0.45f)

    if (glassConfig.depthEffect) {
        // 1. Airy, subtle upper glass dome specular sheen (zero dark interior vignette)
        val topGlowAlpha = (if (isDarkHeader) 0.10f else 0.18f) * effectiveGlassProgress
        drawRoundRect(
            brush = Brush.verticalGradient(
                0.0f to Color.White.copy(alpha = topGlowAlpha),
                0.16f to Color.White.copy(alpha = topGlowAlpha * 0.35f),
                0.34f to Color.Transparent,
                1.0f to Color.Transparent
            ),
            cornerRadius = CornerRadius(cornerR, cornerR)
        )

        // 2. Delicate luminous top-rim specular highlight hugging the upper perimeter
        val topSpecStroke = 1.2.dp.toPx()
        val topSpecInset = topSpecStroke * 0.5f
        val topSpecAlpha = (if (isDarkHeader) 0.52f else 0.78f) * effectiveGlassProgress
        drawRoundRect(
            brush = Brush.verticalGradient(
                0.0f to Color.White.copy(alpha = topSpecAlpha),
                0.14f to Color.White.copy(alpha = topSpecAlpha * 0.40f),
                0.34f to Color.Transparent,
                1.0f to Color.Transparent
            ),
            topLeft = Offset(topSpecInset, topSpecInset),
            size = Size(
                (w - topSpecInset * 2f).coerceAtLeast(1f),
                (h - topSpecInset * 2f).coerceAtLeast(1f)
            ),
            cornerRadius = CornerRadius(
                (cornerR - topSpecInset).coerceAtLeast(0f),
                (cornerR - topSpecInset).coerceAtLeast(0f)
            ),
            style = Stroke(width = topSpecStroke)
        )
    }

    if (refractionStrengthNorm > 0.02f && glassConfig.refractionEdgeWidth > 0.02f) {
        val causticAlpha = (0.20f * refractionStrengthNorm * effectiveGlassProgress)
            .coerceIn(0f, 0.42f)
        val rimStroke = (edgeBand * 0.32f).coerceIn(1.dp.toPx(), 6.dp.toPx())
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = causticAlpha),
                    Color.Transparent,
                    Color.White.copy(alpha = causticAlpha * 0.8f)
                ),
                start = Offset.Zero,
                end = Offset(w, h)
            ),
            cornerRadius = CornerRadius(cornerR, cornerR),
            style = Stroke(width = rimStroke)
        )
    }

    if (glassConfig.chromaticAberration && (refractionStrengthNorm > 0.05f || highlightStrengthNorm > 0.05f)) {
        val dispAlpha = (0.32f * (0.4f + 0.6f * refractionStrengthNorm) * effectiveGlassProgress)
            .coerceIn(0f, 0.62f)
        val dispWidth = (1.5.dp.toPx() * (0.6f + 0.4f * glassConfig.refractionEdgeWidth))
            .coerceIn(0.8.dp.toPx(), 4.dp.toPx())
        drawRoundRect(
            brush = Brush.linearGradient(
                0.0f to Color(0xFFFF6B9E).copy(alpha = dispAlpha),
                0.22f to Color(0xFFF59E0B).copy(alpha = dispAlpha * 0.55f),
                0.50f to Color.Transparent,
                0.78f to Color(0xFF22D3EE).copy(alpha = dispAlpha * 0.65f),
                1.0f to Color(0xFF38BDF8).copy(alpha = dispAlpha),
                start = Offset.Zero,
                end = Offset(w, h)
            ),
            topLeft = Offset(dispWidth * 0.5f, dispWidth * 0.5f),
            size = Size(
                (w - dispWidth).coerceAtLeast(1f),
                (h - dispWidth).coerceAtLeast(1f)
            ),
            cornerRadius = CornerRadius(
                (cornerR - dispWidth * 0.5f).coerceAtLeast(0f),
                (cornerR - dispWidth * 0.5f).coerceAtLeast(0f)
            ),
            style = Stroke(width = dispWidth)
        )
    }
}
