package com.istiak.auraplayer

import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Typeface
import android.graphics.drawable.Icon as AndroidIcon
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.MediaStore
import android.provider.Settings
import android.util.Rational
import android.util.TypedValue
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.TextureView
import android.view.WindowManager
import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.extractor.metadata.id3.ChapterFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import com.istiak.auraplayer.ui.theme.LocalExtendedPalette
import com.istiak.auraplayer.ui.theme.PlayerHudGlassBg
import com.istiak.auraplayer.ui.theme.PlayerHudGlassBorder
import com.istiak.auraplayer.ui.theme.PlayerSeekPeriwinkle
import com.istiak.auraplayer.ui.theme.PlayerVerticalSliderBg
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(UnstableApi::class)
enum class PlayerScaleMode(val label: String, val resizeMode: Int) {
    FIT("FIT TO SCREEN", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    STRETCH("STRETCH", AspectRatioFrameLayout.RESIZE_MODE_FILL),
    CROP("CROP", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    ORIGINAL_100("100%", AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH)
}

data class VideoChapter(
    val title: String,
    val startTimeMs: Long
)

data class ChapterMarker(
    val title: String,
    val startMs: Long,
    val endMs: Long
)

sealed interface InteractivePlaybackPopup {
    val markerId: String
    val actionLabel: String
    val subtitleText: String?

    data class SkipIntro(
        override val markerId: String,
        val targetSeekMs: Long,
        override val subtitleText: String? = null
    ) : InteractivePlaybackPopup {
        override val actionLabel: String = "Skip Intro ›|"
    }

    data class SkipOutro(
        override val markerId: String,
        val targetSeekMs: Long,
        val hasPostCredits: Boolean,
        override val subtitleText: String? = null
    ) : InteractivePlaybackPopup {
        override val actionLabel: String = "Skip Outro ›|"
    }

    data class NextVideo(
        override val markerId: String,
        val nextVideoTitle: String
    ) : InteractivePlaybackPopup {
        override val actionLabel: String = "Next Video ›|"
        override val subtitleText: String = nextVideoTitle
    }
}

private enum class ActiveGestureType {
    NONE,
    BRIGHTNESS,
    VOLUME,
    SCRUB,
    PINCH
}

private sealed class CenterBannerState {
    data class Scrub(
        val targetTimeMs: Long,
        val deltaMs: Long,
        val chapterTitle: String? = null
    ) : CenterBannerState()
    data class Zoom(val percent: Int) : CenterBannerState()
    data class Toast(val message: String) : CenterBannerState()
}

const val ACTION_PIP_CONTROL = "com.istiak.auraplayer.ACTION_PIP_CONTROL"
const val EXTRA_PIP_CONTROL_TYPE = "pip_control_type"
const val PIP_CONTROL_REWIND = 1
const val PIP_CONTROL_PLAY_PAUSE = 2
const val PIP_CONTROL_FORWARD = 3

fun computePipAspectRatio(width: Int, height: Int, rotationDegrees: Int = 0): Rational {
    val normalizedRot = ((rotationDegrees % 360) + 360) % 360
    val isRotated90or270 = (normalizedRot == 90 || normalizedRot == 270)
    val effectiveW = if (isRotated90or270) height else width
    val effectiveH = if (isRotated90or270) width else height
    if (effectiveW <= 0 || effectiveH <= 0) {
        return Rational(16, 9)
    }
    // Android enforces PiP Rational ratios strictly between 1/2.39 and 2.39/1
    val clampedRatio = (effectiveW.toFloat() / effectiveH.toFloat()).coerceIn(0.42f, 2.38f)
    return Rational((clampedRatio * 1000f).roundToInt(), 1000)
}

@RequiresApi(Build.VERSION_CODES.O)
fun buildPlayerPictureInPictureParams(
    context: Context,
    width: Int,
    height: Int,
    rotationDegrees: Int,
    isPlaying: Boolean,
    autoEnterEnabled: Boolean
): PictureInPictureParams {
    fun actionPendingIntent(controlType: Int, requestCode: Int): PendingIntent {
        val intent = Intent(ACTION_PIP_CONTROL).apply {
            setPackage(context.packageName)
            putExtra(EXTRA_PIP_CONTROL_TYPE, controlType)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    val rewindAction = RemoteAction(
        AndroidIcon.createWithResource(context, R.drawable.ic_pip_rewind_10),
        "Rewind 10s",
        "Rewind 10 seconds",
        actionPendingIntent(PIP_CONTROL_REWIND, 101)
    )
    val playPauseAction = RemoteAction(
        AndroidIcon.createWithResource(
            context,
            if (isPlaying) R.drawable.ic_pip_pause else R.drawable.ic_pip_play
        ),
        if (isPlaying) "Pause" else "Play",
        if (isPlaying) "Pause playback" else "Resume playback",
        actionPendingIntent(PIP_CONTROL_PLAY_PAUSE, 102)
    )
    val forwardAction = RemoteAction(
        AndroidIcon.createWithResource(context, R.drawable.ic_pip_forward_10),
        "Forward 10s",
        "Forward 10 seconds",
        actionPendingIntent(PIP_CONTROL_FORWARD, 103)
    )

    val builder = PictureInPictureParams.Builder()
        .setAspectRatio(computePipAspectRatio(width, height, rotationDegrees))
        .setActions(listOf(rewindAction, playPauseAction, forwardAction))

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        builder.setAutoEnterEnabled(autoEnterEnabled && isPlaying)
        builder.setSeamlessResizeEnabled(true)
    }
    return builder.build()
}

/**
 * MX Player-Inspired Fullscreen Playback Interface (`VideoPlayerScreen.kt`):
 * - 2.0x Press-and-Hold Speed Boost (`2.0x ▶▶` floating top-center pill while holding finger down).
 * - 200% Software Volume Boost via `android.media.audiofx.LoudnessEnhancer(exoPlayer.audioSessionId)`
 *   from `0 mB` (100%) up to `+1500 mB` (200%) with amber/blue boost tint on the left-edge volume slider.
 * - Frame-by-Frame Stepping (`<|` Previous Frame and `|>` Next Frame) inside the expanded quick-action ribbon.
 * - Smart Per-Video Memory (`PlaybackMemoryRepository`) persisting resume timestamp, audio track, subtitle track,
 *   subtitle sync offset, and aspect ratio mode, plus floating "Next Episode" (final 30s) & "+85s Skip Intro" pills.
 * - Embedded Chapter Markers on the `#58A6FF` scrub bar with chapter title preview while scrubbing.
 * - Online Subtitle Search & Download (`OpenSubtitlesHelper`) inside the right-side Subtitle Drawer.
 * - Seamless Android 12+ Auto-PiP with RemoteActions (`Rewind 10s`, `Play/Pause`, `Forward 10s`).
 * - Predictive Back Gesture Animation (Android 14+) and Media3 `MediaSessionService` (`PlaybackService`) integration.
 */
@OptIn(UnstableApi::class, ExperimentalFoundationApi::class)
@Composable
fun VideoPlayerScreen(
    initialVideo: VideoItem,
    playlist: List<VideoItem>,
    settings: AppSettingsState,
    initialSavedPositionMs: Long = 0L,
    forceResumeOnStart: Boolean = false,
    onSavePlaybackProgress: (VideoItem, Long, Long) -> Unit,
    onSaveBrightnessPreference: (videoUriKey: String, brightness: Float) -> Unit = { _, _ -> },
    onOpenSettingsFromPlayer: () -> Unit = {},
    onShareVideo: (VideoItem) -> Unit = {},
    onRenameVideoFromPlayer: (VideoItem, String) -> Unit = { _, _ -> },
    onDeleteVideoFromPlayer: (VideoItem) -> Unit = {},
    onPipMetadataChanged: (width: Int, height: Int, rotationDegrees: Int, isPlaying: Boolean) -> Unit = { _, _, _, _ -> },
    onClosePlayer: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    val activity = context as? Activity
    val componentActivity = activity as? ComponentActivity
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    val playbackMemoryRepository = remember(context) {
        PlaybackMemoryRepository(context.applicationContext)
    }

    val effectivePlaylist = remember(playlist, initialVideo) {
        if (playlist.isEmpty()) listOf(initialVideo) else playlist
    }
    var currentIndex by remember(initialVideo, effectivePlaylist) {
        val idx = effectivePlaylist.indexOfFirst { it.id == initialVideo.id && it.uri == initialVideo.uri }
        mutableIntStateOf(if (idx >= 0) idx else 0)
    }
    val currentVideo = effectivePlaylist.getOrElse(currentIndex) { initialVideo }
    var hasConsumedForceResume by remember(initialVideo.id, initialVideo.uri) { mutableStateOf(false) }

    var activeDecoderMode by remember(settings.decoderPriority) { mutableStateOf(settings.decoderPriority) }
    var useSwAudioDecoder by remember(activeDecoderMode) {
        mutableStateOf(activeDecoderMode != DecoderPriority.HW)
    }
    var preservedReloadPositionMs by remember { mutableLongStateOf(-1L) }

    // Configure DefaultRenderersFactory + FFmpeg audio extension + AudioAttributes + AudioFocus
    val exoPlayer = remember(activeDecoderMode) {
        val extensionMode = when {
            activeDecoderMode == DecoderPriority.HW && !useSwAudioDecoder ->
                DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
            activeDecoderMode == DecoderPriority.SW || useSwAudioDecoder ->
                DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
            else -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
        }
        val renderersFactory = DefaultRenderersFactory(context).apply {
            setEnableDecoderFallback(true)
            setExtensionRendererMode(extensionMode)
        }
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        val seekIncrementMs = (settings.doubleTapSeekSeconds * 1000L).coerceAtLeast(5000L)
        ExoPlayer.Builder(context, renderersFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setSeekParameters(SeekParameters.CLOSEST_SYNC)
            .setSeekBackIncrementMs(seekIncrementMs)
            .setSeekForwardIncrementMs(seekIncrementMs)
            .build()
            .apply {
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                playbackParameters = PlaybackParameters(settings.defaultPlaybackSpeed)
                playWhenReady = true
            }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var pendingSeekPositionMs by remember(currentVideo.id) { mutableStateOf<Long?>(null) }
    var pendingSeekTimestampMs by remember(currentVideo.id) { mutableLongStateOf(0L) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(currentVideo.durationMs.coerceAtLeast(1000L)) }
    var activePlayerView by remember { mutableStateOf<PlayerView?>(null) }
    var playbackSpeed by remember { mutableFloatStateOf(settings.defaultPlaybackSpeed) }
    var scaleMode by remember { mutableStateOf(PlayerScaleMode.FIT) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }

    var isControlsVisible by remember { mutableStateOf(true) }
    var isScreenLocked by remember { mutableStateOf(false) }
    var isLandscape by remember(currentVideo.id) {
        val initialOri = resolveTargetActivityOrientation(
            mode = settings.videoOrientationMode,
            width = currentVideo.width,
            height = currentVideo.height,
            rotationDegrees = currentVideo.rotationDegrees
        )
        mutableStateOf(
            initialOri == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE ||
                initialOri == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE ||
                initialOri == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        )
    }
    var manualOrientationOverridden by remember { mutableStateOf(false) }
    var activeVideoWidth by remember(currentVideo.id) { mutableIntStateOf(currentVideo.width) }
    var activeVideoHeight by remember(currentVideo.id) { mutableIntStateOf(currentVideo.height) }
    var activeVideoRotation by remember(currentVideo.id) { mutableIntStateOf(currentVideo.rotationDegrees) }
    var isInPipMode by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                componentActivity?.isInPictureInPictureMode == true
        )
    }
    var detectedResolutionText by remember(currentVideo) {
        mutableStateOf(currentVideo.formattedResolution)
    }

    // Ribbon & drawer states
    var activeDrawer by remember { mutableStateOf(PlayerDrawerType.NONE) }
    var lastNonNoneDrawer by remember { mutableStateOf(PlayerDrawerType.AUDIO) }
    var showSpeedBar by remember { mutableStateOf(false) }
    var showDecoderDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showOnlineSubtitleDialog by remember { mutableStateOf(false) }
    var showLeaveConfirmDialog by remember { mutableStateOf(false) }
    var isExtraRibbonExpanded by remember { mutableStateOf(false) }

    // Playback tools
    var backgroundAudioActive by remember { mutableStateOf(settings.backgroundAudioOrPipOnHome) }
    var nightModeActive by remember { mutableStateOf(false) }
    var shuffleActive by remember { mutableStateOf(false) }
    var loopSingleActive by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var longPress2xEnabled by remember { mutableStateOf(true) }
    var isTemporary2xActive by remember { mutableStateOf(false) }
    var sleepTimerSelection by remember { mutableStateOf<SleepTimerSelection>(SleepTimerSelection.Off) }
    var sleepTimerRemainingSeconds by remember { mutableStateOf<Int?>(null) }
    var isSleepFadeOutRunning by remember { mutableStateOf(false) }
    var abRepeatStartMs by remember(currentVideo.id) { mutableStateOf<Long?>(null) }
    var abRepeatEndMs by remember(currentVideo.id) { mutableStateOf<Long?>(null) }
    var skipIntroDismissed by remember(currentVideo.id) { mutableStateOf(false) }
    var pendingResumePromptMs by remember(currentVideo.id) { mutableStateOf<Long?>(null) }
    var resumePromptCountdownSec by remember(currentVideo.id) { mutableIntStateOf(4) }

    // Track lists & per-video memory state
    var audioTracks by remember { mutableStateOf<List<AudioTrackUiModel>>(emptyList()) }
    var subtitleTracks by remember { mutableStateOf<List<SubtitleTrackUiModel>>(emptyList()) }
    var externalSubtitleUris by remember(currentVideo.uri) {
        mutableStateOf<List<Triple<Uri, String, String>>>(emptyList())
    }
    var rememberedMemory by remember(currentVideo.id) { mutableStateOf<VideoPlaybackMemory?>(null) }
    var hasAppliedRememberedTracks by remember(currentVideo.id) { mutableStateOf(false) }
    var selectedAudioGroupIdx by remember(currentVideo.id) { mutableIntStateOf(-1) }
    var selectedAudioTrackIdx by remember(currentVideo.id) { mutableIntStateOf(-1) }
    var selectedSubtitleGroupIdx by remember(currentVideo.id) { mutableIntStateOf(-1) }
    var selectedSubtitleTrackIdx by remember(currentVideo.id) { mutableIntStateOf(-1) }
    var latestExternalSubPath by remember(currentVideo.id) { mutableStateOf<String?>(null) }

    var isAudioDisabled by remember { mutableStateOf(false) }
    var areSubtitlesEnabled by remember { mutableStateOf(true) }
    var audioSyncOffsetMs by remember { mutableIntStateOf(0) }
    var stereoMode by remember { mutableStateOf(StereoMode.STEREO) }
    var subtitleSyncOffsetMs by remember { mutableLongStateOf(0L) }
    var subtitleSpeed by remember { mutableFloatStateOf(1.0f) }

    val subtitlePreferences = remember(context) {
        SubtitlePreferences(context.applicationContext)
    }
    val persistedSubtitleStyle by subtitlePreferences.styleConfigFlow.collectAsState(
        initial = SubtitleStyleConfig.Default
    )
    var subtitleStyleConfig by remember { mutableStateOf(SubtitleStyleConfig.Default) }
    LaunchedEffect(persistedSubtitleStyle) {
        subtitleStyleConfig = persistedSubtitleStyle
    }
    var activeCueLines by remember(currentVideo.id, currentVideo.uri) {
        mutableStateOf<List<String>>(emptyList())
    }
    var bottomHudHeightPx by remember { mutableIntStateOf(0) }
    val bottomHudHeightDp = with(LocalDensity.current) {
        if (bottomHudHeightPx > 0) {
            // Subtract invisible top touch-box padding above the centered seekbar/timestamps row
            val seekbarVisualTopDp = bottomHudHeightPx.toDp() - 24.dp
            if (isPortrait) seekbarVisualTopDp.coerceIn(136.dp, 160.dp)
            else seekbarVisualTopDp.coerceIn(84.dp, 96.dp)
        } else {
            if (isPortrait) 144.dp else 94.dp
        }
    }

    // Chapters parsed dynamically from MediaMetadata / ExoPlayer ID3 / Container (MKV EBML / MP4 chpl)
    var id3Chapters by remember(currentVideo.id, currentVideo.uri) {
        mutableStateOf<List<VideoChapter>>(emptyList())
    }
    var containerChapters by remember(currentVideo.id, currentVideo.uri) {
        mutableStateOf<List<VideoChapter>>(emptyList())
    }
    val combinedRawChapters = remember(id3Chapters, containerChapters) {
        (id3Chapters + containerChapters)
            .distinctBy { it.startTimeMs }
            .sortedBy { it.startTimeMs }
    }
    var chapters by remember(currentVideo.id, currentVideo.uri, durationMs, combinedRawChapters) {
        mutableStateOf(buildVideoChapters(exoPlayer.mediaMetadata, durationMs, combinedRawChapters))
    }
    val chapterMarkers = remember(chapters, durationMs) {
        resolveChapterMarkers(chapters, durationMs)
    }
    val dismissedMarkerIds = remember(currentVideo.id, currentVideo.uri) {
        mutableStateListOf<String>()
    }
    var activeInteractivePopup by remember(currentVideo.id, currentVideo.uri) {
        mutableStateOf<InteractivePlaybackPopup?>(null)
    }
    var popupCountdownFraction by remember(currentVideo.id, currentVideo.uri) {
        mutableFloatStateOf(1f)
    }

    // Equalizer state
    var eqPreset by remember { mutableStateOf(EqPreset.NORMAL) }
    val eqBandsDb = remember { mutableStateListOf(0f, 0f, 0f, 0f, 0f) }
    var bassBoostStrength by remember { mutableFloatStateOf(0f) }

    var lastInteractionTick by remember { mutableIntStateOf(0) }
    var toastTriggerTick by remember { mutableIntStateOf(0) }
    var centerBanner by remember { mutableStateOf<CenterBannerState?>(null) }
    var lastDisplayedBanner by remember { mutableStateOf<CenterBannerState?>(null) }
    var leftEdgeVolumeVisible by remember { mutableStateOf(false) }
    var rightEdgeBrightnessVisible by remember { mutableStateOf(false) }

    fun showCenterToast(message: String) {
        val toast = CenterBannerState.Toast(message)
        centerBanner = toast
        lastDisplayedBanner = toast
        toastTriggerTick++
    }

    // Brightness (0.01..1.0) & Volume (0.0..2.0 for 200% Software Volume Boost)
    var brightnessFraction by remember {
        mutableFloatStateOf(resolveInitialDisplayBrightness(context))
    }
    var volumeFraction by remember {
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        mutableFloatStateOf((curVol.toFloat() / maxVol.toFloat()).coerceIn(0f, 2.0f))
    }

    fun persistCurrentVideoMemory(positionOverrideMs: Long? = null) {
        val effectiveDuration = exoPlayer.duration.takeIf { it > 0L }
            ?: durationMs.takeIf { it > 0L }
            ?: currentVideo.durationMs.coerceAtLeast(0L)
        val rawPos = positionOverrideMs
            ?: pendingResumePromptMs
            ?: exoPlayer.currentPosition
        val pos = rawPos.coerceAtLeast(0L)
        onSavePlaybackProgress(currentVideo, pos, effectiveDuration)
        coroutineScope.launch {
            withContext(NonCancellable) {
                playbackMemoryRepository.saveMemory(
                    VideoPlaybackMemory(
                        videoId = currentVideo.id,
                        videoUri = currentVideo.uri.toString(),
                        resumePositionMs = pos,
                        selectedAudioGroupIndex = selectedAudioGroupIdx,
                        selectedAudioTrackIndex = selectedAudioTrackIdx,
                        selectedSubtitleGroupIndex = selectedSubtitleGroupIdx,
                        selectedSubtitleTrackIndex = selectedSubtitleTrackIdx,
                        areSubtitlesEnabled = areSubtitlesEnabled,
                        subtitleSyncOffsetMs = subtitleSyncOffsetMs,
                        scaleMode = scaleMode,
                        externalSubtitlePath = latestExternalSubPath
                    )
                )
            }
        }
    }

    val latestPersistCurrentVideoMemory by rememberUpdatedState<(Long?) -> Unit> { posOverride ->
        persistCurrentVideoMemory(posOverride)
    }
    val latestPendingResumePromptMs by rememberUpdatedState(pendingResumePromptMs)
    val latestCurrentVideo by rememberUpdatedState(currentVideo)
    val latestDurationMs by rememberUpdatedState(durationMs)
    val latestSettings by rememberUpdatedState(settings)
    val latestChapterMarkers by rememberUpdatedState(chapterMarkers)
    val latestCombinedRawChapters by rememberUpdatedState(combinedRawChapters)
    val latestOnSaveBrightnessPreference by rememberUpdatedState(onSaveBrightnessPreference)

    fun performSeek(targetMs: Long, useExactSeek: Boolean = true) {
        val maxDuration = exoPlayer.duration.takeIf { it > 0L } ?: durationMs.coerceAtLeast(1000L)
        val clamped = targetMs.coerceIn(0L, maxDuration)
        pendingSeekPositionMs = clamped
        pendingSeekTimestampMs = System.currentTimeMillis()
        currentPositionMs = clamped
        exoPlayer.setSeekParameters(
            if (useExactSeek) SeekParameters.EXACT else SeekParameters.CLOSEST_SYNC
        )
        exoPlayer.seekTo(clamped)
        persistCurrentVideoMemory(positionOverrideMs = clamped)
    }

    fun attachSubtitleUriToPlayer(subUri: Uri, subMime: String, subLabel: String, localPath: String? = null) {
        externalSubtitleUris = externalSubtitleUris + Triple(subUri, subMime, subLabel)
        if (localPath != null) {
            latestExternalSubPath = localPath
        }
        val resumeAt = exoPlayer.currentPosition.coerceAtLeast(0L)
        val subs = externalSubtitleUris.mapIndexed { idx, (u, m, l) ->
            MediaItem.SubtitleConfiguration.Builder(u)
                .setMimeType(m)
                .setLanguage("en")
                .setLabel("External ${idx + 1}: $l")
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()
        }
        val directPlaybackUri = if (currentVideo.isVaultItem && !currentVideo.vaultFilePath.isNullOrBlank()) {
            Uri.fromFile(File(currentVideo.vaultFilePath))
        } else {
            currentVideo.uri
        }
        val mediaItem = MediaItem.Builder()
            .setUri(directPlaybackUri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(currentVideo.title)
                    .setDisplayTitle(currentVideo.displayName)
                    .build()
            )
            .setSubtitleConfigurations(subs)
            .build()
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.seekTo(resumeAt)
        areSubtitlesEnabled = true
        persistCurrentVideoMemory(resumeAt)
        showCenterToast("Loaded Subtitle: $subLabel")
    }

    val openSubtitleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val mimeType = when {
                uri.toString().endsWith(".ass", ignoreCase = true) ||
                    uri.toString().endsWith(".ssa", ignoreCase = true) -> MimeTypes.TEXT_SSA
                uri.toString().endsWith(".vtt", ignoreCase = true) -> MimeTypes.TEXT_VTT
                else -> MimeTypes.APPLICATION_SUBRIP
            }
            val label = uri.lastPathSegment?.substringAfterLast('/') ?: "External Subtitle"
            attachSubtitleUriToPlayer(uri, mimeType, label)
        }
    }

    val openAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val label = uri.lastPathSegment?.substringAfterLast('/') ?: "External Audio"
            val extTrack = AudioTrackUiModel(
                id = "ext_audio_${audioTracks.size + 1}",
                groupIndex = -1,
                trackIndex = -1,
                label = label,
                detail = "External Audio Stream / 48 kHz",
                isSelected = true
            )
            isAudioDisabled = false
            audioTracks = audioTracks.map { it.copy(isSelected = false) } + extTrack
            showCenterToast("Loaded Audio: $label")
        }
    }

    // AudioFX Equalizer + BassBoost + LoudnessEnhancer (200% Software Volume Boost) attached to ExoPlayer's audioSessionId
    var activeAudioSessionId by remember(exoPlayer) {
        mutableIntStateOf(exoPlayer.audioSessionId)
    }
    val audioFxBundle = remember(exoPlayer, activeAudioSessionId) {
        var equalizer: Equalizer? = null
        var bassBoost: BassBoost? = null
        var loudnessEnhancer: LoudnessEnhancer? = null
        try {
            if (activeAudioSessionId != C.AUDIO_SESSION_ID_UNSET && activeAudioSessionId != 0) {
                equalizer = Equalizer(0, activeAudioSessionId).apply { enabled = true }
                bassBoost = BassBoost(0, activeAudioSessionId).apply { enabled = true }
                loudnessEnhancer = LoudnessEnhancer(activeAudioSessionId).apply {
                    enabled = true
                    setTargetGain(0)
                }
            }
        } catch (_: Exception) {
            equalizer = null
            bassBoost = null
            loudnessEnhancer = null
        }
        Triple(equalizer, bassBoost, loudnessEnhancer)
    }

    DisposableEffect(audioFxBundle) {
        onDispose {
            runCatching { audioFxBundle.first?.release() }
            runCatching { audioFxBundle.second?.release() }
            runCatching { audioFxBundle.third?.release() }
        }
    }

    fun applyVolumeAndSoftwareBoost(newFraction: Float) {
        val clamped = newFraction.coerceIn(0f, 2.0f)
        volumeFraction = clamped
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val hwFraction = clamped.coerceIn(0f, 1.0f)
        val newHwVol = (hwFraction * maxVol).roundToInt().coerceIn(0, maxVol)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newHwVol, 0)
        isMuted = newHwVol == 0 && clamped <= 0.01f

        // 100% -> 200% progressively applies 0 mB up to +1500 mB via LoudnessEnhancer
        val boostFraction = (clamped - 1.0f).coerceIn(0f, 1.0f)
        val targetGainMb = (boostFraction * 1500f).roundToInt()
        runCatching {
            audioFxBundle.third?.let { enhancer ->
                enhancer.enabled = true
                enhancer.setTargetGain(targetGainMb)
            }
        }
    }

    fun applyEqBandsToHardware(levelsDb: List<Float>) {
        val eq = audioFxBundle.first ?: return
        runCatching {
            val numBands = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange
            val minMb = range[0].toFloat()
            val maxMb = range[1].toFloat()
            for (i in 0 until minOf(numBands, levelsDb.size)) {
                val targetMb = (levelsDb[i] * 100f).coerceIn(minMb, maxMb).toInt().toShort()
                eq.setBandLevel(i.toShort(), targetMb)
            }
        }
    }

    LaunchedEffect(audioFxBundle) {
        applyEqBandsToHardware(eqBandsDb.toList())
        runCatching {
            audioFxBundle.second?.setStrength((bassBoostStrength * 1000f).toInt().toShort())
        }
        val boostFraction = (volumeFraction - 1.0f).coerceIn(0f, 1.0f)
        runCatching {
            audioFxBundle.third?.setTargetGain((boostFraction * 1500f).roundToInt())
        }
    }

    // Bind ExoPlayer to PlaybackService (Media3 MediaSessionService) for lock-screen & Bluetooth controls
    DisposableEffect(exoPlayer, backgroundAudioActive) {
        PlaybackService.attachPlayer(exoPlayer)
        if (backgroundAudioActive) {
            runCatching {
                context.startService(Intent(context, PlaybackService::class.java))
            }
        }
        onDispose {
            if (!backgroundAudioActive) {
                PlaybackService.attachPlayer(null)
            }
        }
    }

    // Extract embedded container chapters (Matroska EBML Chapters / MP4 chpl) off the main thread
    LaunchedEffect(currentVideo.uri, currentVideo.vaultFilePath, currentVideo.filePath) {
        val localPath = currentVideo.vaultFilePath?.takeIf { it.isNotBlank() }
            ?: currentVideo.filePath?.takeIf { it.isNotBlank() }
        val directPlaybackUri = if (currentVideo.isVaultItem && !currentVideo.vaultFilePath.isNullOrBlank()) {
            Uri.fromFile(File(currentVideo.vaultFilePath))
        } else {
            currentVideo.uri
        }
        val parsedContainerChapters = extractContainerChapters(
            context = context,
            uri = directPlaybackUri,
            directFilePath = localPath
        )
        if (parsedContainerChapters.isNotEmpty()) {
            containerChapters = parsedContainerChapters
        }
    }

    // Apply Remember display brightness preference on video open / switch / mode change
    LaunchedEffect(currentVideo.uri, settings.brightnessMemoryMode) {
        val targetSavedBrightness = when (settings.brightnessMemoryMode) {
            BrightnessMemoryMode.OFF -> null
            BrightnessMemoryMode.ALL_VIDEOS -> settings.savedGlobalBrightness.takeIf { it in 0.01f..1.0f }
            BrightnessMemoryMode.PER_VIDEO ->
                settings.savedPerVideoBrightnessMap[currentVideo.uri.toString()]?.takeIf { it in 0.01f..1.0f }
        }
        if (targetSavedBrightness != null) {
            brightnessFraction = targetSavedBrightness
            activity?.window?.let { win ->
                val lp = win.attributes
                lp.screenBrightness = targetSavedBrightness
                win.attributes = lp
            }
        } else {
            brightnessFraction = resolveInitialDisplayBrightness(context)
            activity?.window?.let { win ->
                val lp = win.attributes
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                win.attributes = lp
            }
        }
    }

    // Load media item and restore smart per-video memory when currentVideo or activeDecoderMode changes
    LaunchedEffect(currentVideo.uri, activeDecoderMode) {
        hasAppliedRememberedTracks = false
        val savedMem = playbackMemoryRepository.getMemory(currentVideo.id, currentVideo.uri.toString())
        rememberedMemory = savedMem

        if (savedMem != null) {
            scaleMode = savedMem.scaleMode
            subtitleSyncOffsetMs = savedMem.subtitleSyncOffsetMs
            areSubtitlesEnabled = savedMem.areSubtitlesEnabled
            selectedAudioGroupIdx = savedMem.selectedAudioGroupIndex
            selectedAudioTrackIdx = savedMem.selectedAudioTrackIndex
            selectedSubtitleGroupIdx = savedMem.selectedSubtitleGroupIndex
            selectedSubtitleTrackIdx = savedMem.selectedSubtitleTrackIndex

            val savedExtPath = savedMem.externalSubtitlePath
            if (!savedExtPath.isNullOrBlank()) {
                val extFile = File(savedExtPath)
                if (extFile.exists() && externalSubtitleUris.none { it.first.path == extFile.absolutePath }) {
                    latestExternalSubPath = extFile.absolutePath
                    externalSubtitleUris = externalSubtitleUris + Triple(
                        Uri.fromFile(extFile),
                        MimeTypes.APPLICATION_SUBRIP,
                        extFile.name
                    )
                }
            }
        }

        val subs = externalSubtitleUris.mapIndexed { idx, (subUri, subMime, subLabel) ->
            MediaItem.SubtitleConfiguration.Builder(subUri)
                .setMimeType(subMime)
                .setLanguage("en")
                .setLabel("External ${idx + 1}: $subLabel")
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()
        }
        val directPlaybackUri = if (currentVideo.isVaultItem && !currentVideo.vaultFilePath.isNullOrBlank()) {
            Uri.fromFile(File(currentVideo.vaultFilePath))
        } else {
            currentVideo.uri
        }
        val mediaItem = MediaItem.Builder()
            .setUri(directPlaybackUri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(currentVideo.title)
                    .setDisplayTitle(currentVideo.displayName)
                    .build()
            )
            .setSubtitleConfigurations(subs)
            .build()
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.playbackParameters = PlaybackParameters(playbackSpeed)
        exoPlayer.repeatMode = if (loopSingleActive) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        exoPlayer.volume = if (isMuted) 0f else 1f
        exoPlayer.prepare()

        val fallbackInitialPos = if (currentVideo.id == initialVideo.id && currentVideo.uri == initialVideo.uri) {
            initialSavedPositionMs
        } else {
            0L
        }
        val rememberedPos = savedMem?.resumePositionMs
            ?: fallbackInitialPos.takeIf { it > 0L }
            ?: settings.playbackPositionsMs[currentVideo.id]
            ?: 0L
        val effectiveDuration = currentVideo.durationMs.coerceAtLeast(1000L)
        val maxPlayablePos = (effectiveDuration - 1500L).coerceAtLeast(0L)
        val isAlreadyCompleted = effectiveDuration > 0L && (
            rememberedPos >= maxPlayablePos ||
                (rememberedPos.toFloat() / effectiveDuration.toFloat()) >= 0.95f
            )
        val resumablePos = if (isAlreadyCompleted) 0L else rememberedPos.coerceAtLeast(0L)
        val hasPartialProgress = resumablePos > 5000L
        val shouldForceResume = forceResumeOnStart && !hasConsumedForceResume &&
            currentVideo.id == initialVideo.id && currentVideo.uri == initialVideo.uri
        if (shouldForceResume) {
            hasConsumedForceResume = true
        }
        durationMs = effectiveDuration
        pendingSeekPositionMs = null

        when {
            preservedReloadPositionMs >= 0L -> {
                val reloadPos = preservedReloadPositionMs.also { preservedReloadPositionMs = -1L }
                pendingResumePromptMs = null
                performSeek(reloadPos)
                exoPlayer.playWhenReady = true
            }
            shouldForceResume && resumablePos > 0L -> {
                pendingResumePromptMs = null
                exoPlayer.seekTo(resumablePos)
                currentPositionMs = resumablePos
                exoPlayer.playWhenReady = true
                persistCurrentVideoMemory(resumablePos)
            }
            settings.resumePlaybackMode == ResumePlaybackMode.ASK && hasPartialProgress && !shouldForceResume -> {
                pendingResumePromptMs = resumablePos
                resumePromptCountdownSec = 4
                currentPositionMs = 0L
                exoPlayer.seekTo(0L)
                exoPlayer.playWhenReady = false
                onSavePlaybackProgress(currentVideo, resumablePos, effectiveDuration)
            }
            settings.resumePlaybackMode == ResumePlaybackMode.RESUME && resumablePos > 0L -> {
                pendingResumePromptMs = null
                exoPlayer.seekTo(resumablePos)
                currentPositionMs = resumablePos
                exoPlayer.playWhenReady = true
                persistCurrentVideoMemory(resumablePos)
            }
            else -> {
                pendingResumePromptMs = null
                currentPositionMs = 0L
                exoPlayer.seekTo(0L)
                exoPlayer.playWhenReady = true
                persistCurrentVideoMemory(0L)
            }
        }
    }

    suspend fun executeSleepTimerFadeAndExit() {
        if (isSleepFadeOutRunning) return
        isSleepFadeOutRunning = true
        val startVolume = exoPlayer.volume.coerceIn(0f, 1f)
        val fadeSteps = 20
        for (step in 1..fadeSteps) {
            val fraction = 1f - (step.toFloat() / fadeSteps.toFloat())
            exoPlayer.volume = (startVolume * fraction).coerceIn(0f, 1f)
            delay(100L)
        }
        exoPlayer.pause()
        exoPlayer.volume = if (isMuted) 0f else 1f
        sleepTimerSelection = SleepTimerSelection.Off
        sleepTimerRemainingSeconds = null
        persistCurrentVideoMemory()
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        onClosePlayer()
    }

    // Apply configured 8-mode VideoOrientationMode on load and video dimension updates
    fun applyConfiguredOrientation(videoW: Int, videoH: Int, rotationDegrees: Int = 0) {
        val act = activity ?: return
        val targetOrientation = resolveTargetActivityOrientation(
            mode = settings.videoOrientationMode,
            width = videoW,
            height = videoH,
            rotationDegrees = rotationDegrees
        )
        val normalizedRot = ((rotationDegrees % 360) + 360) % 360
        val isRotated90or270 = (normalizedRot == 90 || normalizedRot == 270)
        val effectiveWidth = if (isRotated90or270) videoH else videoW
        val effectiveHeight = if (isRotated90or270) videoW else videoH

        isLandscape = when (targetOrientation) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE -> true
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT,
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT -> false
            else -> effectiveWidth > effectiveHeight
        }
        act.requestedOrientation = targetOrientation
    }

    var lastAppliedOrientationKey by remember {
        mutableStateOf<Pair<Long, VideoOrientationMode>?>(null)
    }
    val currentOrientationKey = currentVideo.id to settings.videoOrientationMode
    SideEffect {
        if (lastAppliedOrientationKey != currentOrientationKey) {
            lastAppliedOrientationKey = currentOrientationKey
            manualOrientationOverridden = false
            applyConfiguredOrientation(
                videoW = currentVideo.width,
                videoH = currentVideo.height,
                rotationDegrees = currentVideo.rotationDegrees
            )
        }
    }

    fun requestPlayerExit() {
        if (settings.confirmBeforeLeavingPlayback) {
            showLeaveConfirmDialog = true
        } else {
            persistCurrentVideoMemory()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            onClosePlayer()
        }
    }

    // Android 14+ Predictive Back Gesture Animation: smoothly scales (1.0f -> 0.92f) + rounded corners
    var predictiveBackProgress by remember { mutableFloatStateOf(0f) }
    PredictiveBackHandler(enabled = true) { progress: Flow<BackEventCompat> ->
        try {
            progress.collect { backEvent ->
                predictiveBackProgress = backEvent.progress.coerceIn(0f, 1f)
            }
            predictiveBackProgress = 0f
            if (activeDrawer != PlayerDrawerType.NONE) {
                activeDrawer = PlayerDrawerType.NONE
            } else if (showSpeedBar) {
                showSpeedBar = false
            } else {
                requestPlayerExit()
            }
        } catch (_: CancellationException) {
            predictiveBackProgress = 0f
        }
    }

    // Keep-screen-on, adaptive status bar brightness, & orientation restoration when leaving player
    val localView = LocalView.current
    val latestIsDarkTheme by rememberUpdatedState(LocalExtendedPalette.current.isDark)
    val isAdaptiveLightStatusBar = rememberVideoLuminanceState(
        playerView = activePlayerView,
        isControlsVisible = isControlsVisible,
        isPlaying = isPlaying,
        isInPipMode = isInPipMode
    )

    DisposableEffect(Unit) {
        activity?.window?.let { win ->
            win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            WindowCompat.getInsetsController(win, localView).apply {
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        onDispose {
            activity?.window?.let { win ->
                win.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                val lp = win.attributes
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                win.attributes = lp
                WindowCompat.getInsetsController(win, localView).apply {
                    show(WindowInsetsCompat.Type.systemBars())
                    isAppearanceLightStatusBars = !latestIsDarkTheme
                    isAppearanceLightNavigationBars = !latestIsDarkTheme
                }
            }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Sync system status & navigation bars cleanly with player HUD tap / auto-hide visibility
    LaunchedEffect(isControlsVisible, isInPipMode) {
        if (!isInPipMode) {
            activity?.window?.let { win ->
                val insetsController = WindowCompat.getInsetsController(win, localView)
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                if (isControlsVisible) {
                    insetsController.show(WindowInsetsCompat.Type.systemBars())
                } else {
                    insetsController.hide(WindowInsetsCompat.Type.systemBars())
                }
            }
        }
    }

    // Dynamically match status bar icons/text to top scrim (white) or sampled video frame luminance
    LaunchedEffect(isControlsVisible, isAdaptiveLightStatusBar, isInPipMode) {
        if (!isInPipMode) {
            activity?.window?.let { win ->
                val insetsController = WindowCompat.getInsetsController(win, localView)
                insetsController.isAppearanceLightStatusBars = if (isControlsVisible) {
                    false
                } else {
                    isAdaptiveLightStatusBar
                }
            }
        }
    }

    // Track Activity PiP transitions to hide all overlays in PiP and restore HUD on exit
    DisposableEffect(componentActivity) {
        if (componentActivity == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            onDispose {}
        } else {
            val consumer = Consumer<PictureInPictureModeChangedInfo> { info ->
                val enteredPip = info.isInPictureInPictureMode
                val wasInPip = isInPipMode
                isInPipMode = enteredPip
                if (enteredPip) {
                    isTemporary2xActive = false
                    activeDrawer = PlayerDrawerType.NONE
                    showSpeedBar = false
                    showDecoderDialog = false
                    showSleepTimerDialog = false
                    showInfoDialog = false
                    showRenameDialog = false
                    showOnlineSubtitleDialog = false
                    showLeaveConfirmDialog = false
                    leftEdgeVolumeVisible = false
                    rightEdgeBrightnessVisible = false
                    centerBanner = null
                } else if (wasInPip) {
                    isControlsVisible = true
                    lastInteractionTick++
                }
            }
            componentActivity.addOnPictureInPictureModeChangedListener(consumer)
            onDispose {
                componentActivity.removeOnPictureInPictureModeChangedListener(consumer)
            }
        }
    }

    // Sync Android 12+ Auto-PiP params, aspect ratio, and RemoteAction Play/Pause state live
    LaunchedEffect(
        isPlaying,
        isScreenLocked,
        settings.backgroundAudioOrPipOnHome,
        activeVideoWidth,
        activeVideoHeight,
        activeVideoRotation
    ) {
        val activeForAutoPip = isPlaying && !isScreenLocked
        val allowAutoPip = activeForAutoPip && settings.backgroundAudioOrPipOnHome
        onPipMetadataChanged(activeVideoWidth, activeVideoHeight, activeVideoRotation, activeForAutoPip)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
            runCatching {
                val params = buildPlayerPictureInPictureParams(
                    context = context,
                    width = activeVideoWidth,
                    height = activeVideoHeight,
                    rotationDegrees = activeVideoRotation,
                    isPlaying = isPlaying,
                    autoEnterEnabled = allowAutoPip
                )
                activity.setPictureInPictureParams(params)
            }
        }
    }

    // Explicitly disable Android 12+ Auto-PiP when leaving VideoPlayerScreen back to Home/Browser
    DisposableEffect(activity) {
        onDispose {
            onPipMetadataChanged(0, 0, 0, false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && activity != null) {
                runCatching {
                    activity.setPictureInPictureParams(
                        PictureInPictureParams.Builder()
                            .setAutoEnterEnabled(false)
                            .build()
                    )
                }
            }
        }
    }

    // Handle PiP RemoteAction broadcasts (Rewind 10s, Play/Pause, Forward 10s)
    DisposableEffect(context, exoPlayer) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action != ACTION_PIP_CONTROL) return
                when (intent.getIntExtra(EXTRA_PIP_CONTROL_TYPE, -1)) {
                    PIP_CONTROL_REWIND -> {
                        val target = (exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)
                        performSeek(target)
                    }
                    PIP_CONTROL_PLAY_PAUSE -> {
                        if (exoPlayer.isPlaying) {
                            exoPlayer.pause()
                        } else {
                            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                                exoPlayer.seekTo(0L)
                            }
                            exoPlayer.play()
                        }
                    }
                    PIP_CONTROL_FORWARD -> {
                        val total = exoPlayer.duration.takeIf { it > 0L } ?: durationMs
                        val target = (exoPlayer.currentPosition + 10_000L).coerceAtMost(total)
                        performSeek(target)
                    }
                }
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(ACTION_PIP_CONTROL),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    // Pause playback on background unless Background Audio or PiP mode is active
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, exoPlayer, backgroundAudioActive, currentVideo) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    persistCurrentVideoMemory()
                }
                Lifecycle.Event.ON_STOP -> {
                    val inPip = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                        activity?.isInPictureInPictureMode == true
                    if (!inPip && !backgroundAudioActive) {
                        exoPlayer.pause()
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (!playing &&
                    exoPlayer.playbackState != Player.STATE_IDLE &&
                    exoPlayer.playbackState != Player.STATE_ENDED &&
                    latestPendingResumePromptMs == null
                ) {
                    latestPersistCurrentVideoMemory(null)
                }
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                activeAudioSessionId = audioSessionId
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                chapters = buildVideoChapters(mediaMetadata, durationMs, latestCombinedRawChapters)
            }

            override fun onMetadata(metadata: Metadata) {
                val extracted = mutableListOf<VideoChapter>()
                for (i in 0 until metadata.length()) {
                    val entry = metadata.get(i)
                    if (entry is ChapterFrame && entry.startTimeMs >= 0) {
                        var title = ""
                        for (s in 0 until entry.subFrameCount) {
                            val sub = entry.getSubFrame(s)
                            if (sub is TextInformationFrame && sub.id == "TIT2") {
                                title = sub.values.firstOrNull().orEmpty().trim()
                                if (title.isNotEmpty()) break
                            }
                        }
                        if (title.isEmpty()) {
                            title = entry.chapterId.trim().ifEmpty { "Chapter ${extracted.size + 1}" }
                        }
                        extracted.add(
                            VideoChapter(
                                title = title,
                                startTimeMs = entry.startTimeMs.toLong()
                            )
                        )
                    }
                }
                if (extracted.isNotEmpty()) {
                    val mergedId3 = (id3Chapters + extracted)
                        .distinctBy { it.startTimeMs }
                        .sortedBy { it.startTimeMs }
                    id3Chapters = mergedId3
                    val combined = (mergedId3 + containerChapters)
                        .distinctBy { it.startTimeMs }
                        .sortedBy { it.startTimeMs }
                    chapters = buildVideoChapters(exoPlayer.mediaMetadata, durationMs, combined)
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                val rot = ((videoSize.unappliedRotationDegrees % 360) + 360) % 360
                val isRotated90or270 = (rot == 90 || rot == 270)
                val effectiveW = if (isRotated90or270) videoSize.height else videoSize.width
                val effectiveH = if (isRotated90or270) videoSize.width else videoSize.height
                if (effectiveW > 0 && effectiveH > 0) {
                    activeVideoWidth = videoSize.width
                    activeVideoHeight = videoSize.height
                    activeVideoRotation = rot
                    detectedResolutionText = "${effectiveW}×${effectiveH}"
                    if (!manualOrientationOverridden) {
                        applyConfiguredOrientation(
                            videoW = videoSize.width,
                            videoH = videoSize.height,
                            rotationDegrees = rot
                        )
                    }
                }
            }

            override fun onCues(cueGroup: CueGroup) {
                activeCueLines = cueGroup.cues
                    .mapNotNull { cue -> cue.text?.toString()?.trim() }
                    .filter { it.isNotEmpty() }
            }

            override fun onTracksChanged(tracks: Tracks) {
                val (parsedAudio, parsedSubs) = parseExoTracks(tracks)
                audioTracks = parsedAudio
                subtitleTracks = parsedSubs

                if (!hasAppliedRememberedTracks) {
                    hasAppliedRememberedTracks = true
                    val mem = rememberedMemory
                    if (mem != null) {
                        val audioGroup = tracks.groups.getOrNull(mem.selectedAudioGroupIndex)
                        if (audioGroup != null && mem.selectedAudioTrackIndex in 0 until audioGroup.length) {
                            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                .buildUpon()
                                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                                .setOverrideForType(
                                    TrackSelectionOverride(audioGroup.mediaTrackGroup, mem.selectedAudioTrackIndex)
                                )
                                .build()
                        }
                        if (!mem.areSubtitlesEnabled) {
                            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                .buildUpon()
                                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                                .build()
                        } else {
                            val subGroup = tracks.groups.getOrNull(mem.selectedSubtitleGroupIndex)
                            if (subGroup != null && mem.selectedSubtitleTrackIndex in 0 until subGroup.length) {
                                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                    .buildUpon()
                                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                    .setOverrideForType(
                                        TrackSelectionOverride(subGroup.mediaTrackGroup, mem.selectedSubtitleTrackIndex)
                                    )
                                    .build()
                            }
                        }
                    }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (exoPlayer.duration > 0) {
                    durationMs = exoPlayer.duration
                    chapters = buildVideoChapters(exoPlayer.mediaMetadata, durationMs, latestCombinedRawChapters)
                }
                if (playbackState == Player.STATE_ENDED) {
                    val completedPos = exoPlayer.duration.takeIf { it > 0L }
                        ?: latestDurationMs.takeIf { it > 0L }
                        ?: latestCurrentVideo.durationMs.coerceAtLeast(1L)
                    latestPersistCurrentVideoMemory(completedPos)
                    if (sleepTimerSelection == SleepTimerSelection.EndOfVideo) {
                        coroutineScope.launch {
                            executeSleepTimerFadeAndExit()
                        }
                    } else if (shuffleActive && effectivePlaylist.size > 1) {
                        val nextIdx = (effectivePlaylist.indices - currentIndex).random()
                        currentIndex = nextIdx
                    } else if (latestSettings.autoPlayNextEpisode && currentIndex < effectivePlaylist.lastIndex) {
                        val nextVid = effectivePlaylist.getOrNull(currentIndex + 1)
                        val nextMarkerKey = "next_video_${latestCurrentVideo.id}"
                        if (nextVid != null && nextMarkerKey !in dismissedMarkerIds) {
                            activeInteractivePopup = InteractivePlaybackPopup.NextVideo(
                                markerId = nextMarkerKey,
                                nextVideoTitle = nextVid.title
                            )
                        }
                        isControlsVisible = true
                    } else {
                        isControlsVisible = true
                    }
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            latestPersistCurrentVideoMemory(null)
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // 6-second interactive countdown timer for Skip Intro, Skip Outro, and Next Video popups:
    // If not tapped within 6 seconds, smoothly hides the popup and records the marker in dismissedMarkerIds.
    LaunchedEffect(activeInteractivePopup?.markerId) {
        val currentPopup = activeInteractivePopup ?: return@LaunchedEffect
        val totalWindowMs = 6000L
        val stepMs = 50L
        var elapsedMs = 0L
        popupCountdownFraction = 1f
        while (elapsedMs < totalWindowMs) {
            delay(stepMs)
            if (activeInteractivePopup?.markerId != currentPopup.markerId) return@LaunchedEffect
            elapsedMs += stepMs
            popupCountdownFraction = (1f - (elapsedMs.toFloat() / totalWindowMs.toFloat())).coerceIn(0f, 1f)
        }
        if (activeInteractivePopup?.markerId == currentPopup.markerId) {
            if (currentPopup.markerId !in dismissedMarkerIds) {
                dismissedMarkerIds.add(currentPopup.markerId)
            }
            activeInteractivePopup = null
        }
    }

    // 60/120fps Smooth Playback Progress Interpolation while seekbar is visible and playing
    LaunchedEffect(
        exoPlayer,
        isPlaying,
        playbackSpeed,
        isTemporary2xActive,
        isControlsVisible,
        settings.showSeekbarWhenControlsHidden,
        isScrubbing
    ) {
        val seekbarVisible = isControlsVisible || settings.showSeekbarWhenControlsHidden
        if (!isPlaying || !seekbarVisible || isScrubbing) return@LaunchedEffect

        var anchorExoPosMs = exoPlayer.currentPosition.coerceAtLeast(0L)
        var anchorRealtimeMs = SystemClock.elapsedRealtime()
        while (true) {
            withFrameNanos {
                val nowRealtimeMs = SystemClock.elapsedRealtime()
                val elapsedMs = (nowRealtimeMs - anchorRealtimeMs).coerceAtLeast(0L)
                if (elapsedMs >= 500L) {
                    anchorExoPosMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                    anchorRealtimeMs = nowRealtimeMs
                    if (pendingSeekPositionMs == null && !isScrubbing) {
                        currentPositionMs = anchorExoPosMs
                    }
                } else if (pendingSeekPositionMs == null && !isScrubbing) {
                    val activeSpeed = if (isTemporary2xActive) {
                        settings.holdForSpeedMultiplier.coerceIn(0.5f, 4.0f)
                    } else {
                        playbackSpeed.coerceIn(0.25f, 4.0f)
                    }
                    val totalDur = exoPlayer.duration.takeIf { it > 0L } ?: durationMs.coerceAtLeast(1000L)
                    val interpolated = (anchorExoPosMs + (elapsedMs * activeSpeed).toLong())
                        .coerceIn(0L, totalDur)
                    currentPositionMs = interpolated
                }
            }
        }
    }

    // Poll playback position smoothly + A-B repeat + Interactive Skip Intro / Skip Outro / Next Video popup detection
    LaunchedEffect(exoPlayer, isPlaying, currentVideo, abRepeatStartMs, abRepeatEndMs) {
        while (true) {
            val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
            val pendingTarget = pendingSeekPositionMs
            if (pendingTarget != null) {
                val elapsedSinceSeek = System.currentTimeMillis() - pendingSeekTimestampMs
                if (abs(pos - pendingTarget) <= 500L || elapsedSinceSeek >= 2000L) {
                    pendingSeekPositionMs = null
                    if (!isScrubbing) {
                        currentPositionMs = pos
                    }
                } else if (!isScrubbing) {
                    currentPositionMs = pendingTarget
                }
            } else if (!isScrubbing) {
                currentPositionMs = pos
            }
            if (exoPlayer.duration > 0 && exoPlayer.duration != durationMs) {
                durationMs = exoPlayer.duration
                chapters = buildVideoChapters(exoPlayer.mediaMetadata, durationMs, latestCombinedRawChapters)
            }
            val startA = abRepeatStartMs
            val endB = abRepeatEndMs
            if (startA != null && endB != null && endB > startA && pos >= endB) {
                performSeek(startA)
            }

            // Interactive popup detection for Auto-Skip Intro, Auto-Skip Outro, & Autoplay Next Video
            val totalDur = exoPlayer.duration.takeIf { it > 0L } ?: durationMs
            if (isPlaying &&
                pendingTarget == null &&
                !isScrubbing &&
                latestPendingResumePromptMs == null &&
                totalDur > 0L
            ) {
                val activeMarkers = latestChapterMarkers
                val introMatch = if (latestSettings.autoSkipIntro) {
                    activeMarkers.firstOrNull { marker ->
                        val chapterKey = "intro:${marker.startMs}:${marker.title}"
                        chapterKey !in dismissedMarkerIds &&
                            isIntroChapterTitle(marker.title) &&
                            (marker.endMs - marker.startMs) >= 1000L &&
                            pos in marker.startMs..(marker.endMs - 500L)
                    }
                } else {
                    null
                }

                val fallbackOpeningIntroActive = latestSettings.autoSkipIntro &&
                    introMatch == null &&
                    activeMarkers.none { isIntroChapterTitle(it.title) } &&
                    totalDur >= 180_000L &&
                    pos in 5_000L..90_000L &&
                    "intro:opening_window" !in dismissedMarkerIds

                val outroMatch = if (latestSettings.autoSkipOutro) {
                    activeMarkers.firstOrNull { marker ->
                        val chapterKey = "outro:${marker.startMs}:${marker.title}"
                        chapterKey !in dismissedMarkerIds &&
                            isOutroChapterTitle(marker.title) &&
                            (marker.endMs - marker.startMs) >= 1000L &&
                            pos in marker.startMs..(marker.endMs - 500L)
                    }
                } else {
                    null
                }

                val remainingMs = (totalDur - pos).coerceAtLeast(0L)
                val hasNextInPlaylist = effectivePlaylist.size > 1 && currentIndex < effectivePlaylist.lastIndex
                val nextVideoMarkerKey = "next_video_${currentVideo.id}"
                val shouldShowNextVideoPopup = latestSettings.autoPlayNextEpisode &&
                    hasNextInPlaylist &&
                    remainingMs in 1L..10_000L &&
                    nextVideoMarkerKey !in dismissedMarkerIds

                when {
                    introMatch != null -> {
                        val key = "intro:${introMatch.startMs}:${introMatch.title}"
                        if (activeInteractivePopup?.markerId != key) {
                            activeInteractivePopup = InteractivePlaybackPopup.SkipIntro(
                                markerId = key,
                                targetSeekMs = introMatch.endMs.coerceIn(0L, totalDur),
                                subtitleText = introMatch.title
                            )
                        }
                    }
                    fallbackOpeningIntroActive -> {
                        val key = "intro:opening_window"
                        if (activeInteractivePopup?.markerId != key) {
                            activeInteractivePopup = InteractivePlaybackPopup.SkipIntro(
                                markerId = key,
                                targetSeekMs = (pos + 85_000L).coerceAtMost(totalDur)
                            )
                        }
                    }
                    outroMatch != null -> {
                        val key = "outro:${outroMatch.startMs}:${outroMatch.title}"
                        if (activeInteractivePopup?.markerId != key) {
                            activeInteractivePopup = InteractivePlaybackPopup.SkipOutro(
                                markerId = key,
                                targetSeekMs = outroMatch.endMs.coerceIn(0L, totalDur),
                                hasPostCredits = outroMatch.endMs < totalDur - 1500L,
                                subtitleText = outroMatch.title
                            )
                        }
                    }
                    shouldShowNextVideoPopup -> {
                        val nextVid = effectivePlaylist.getOrNull(currentIndex + 1)
                        if (nextVid != null && activeInteractivePopup?.markerId != nextVideoMarkerKey) {
                            activeInteractivePopup = InteractivePlaybackPopup.NextVideo(
                                markerId = nextVideoMarkerKey,
                                nextVideoTitle = nextVid.title
                            )
                        }
                    }
                }
            }

            delay(180L)
        }
    }

    // Periodic 2-second playback progress persistence during active playback
    LaunchedEffect(exoPlayer, isPlaying, currentVideo) {
        if (!isPlaying) return@LaunchedEffect
        while (true) {
            delay(2000L)
            if (exoPlayer.isPlaying && pendingResumePromptMs == null && !isScrubbing) {
                persistCurrentVideoMemory()
            }
        }
    }

    // Sleep timer live countdown + gentle 2s fade-out exit
    LaunchedEffect(sleepTimerSelection) {
        when (val selection = sleepTimerSelection) {
            is SleepTimerSelection.Off -> {
                sleepTimerRemainingSeconds = null
            }
            is SleepTimerSelection.Minutes -> {
                var remainingSec = (selection.minutes * 60).coerceAtLeast(1)
                sleepTimerRemainingSeconds = remainingSec
                while (remainingSec > 0) {
                    delay(1000L)
                    remainingSec--
                    sleepTimerRemainingSeconds = remainingSec
                }
                executeSleepTimerFadeAndExit()
            }
            is SleepTimerSelection.EndOfVideo -> {
                sleepTimerRemainingSeconds = null
            }
        }
    }

    // Resume "Ask" mode 4-second auto-select countdown
    LaunchedEffect(pendingResumePromptMs) {
        val resumeMs = pendingResumePromptMs ?: return@LaunchedEffect
        for (sec in 4 downTo 1) {
            resumePromptCountdownSec = sec
            delay(1000L)
            if (pendingResumePromptMs != resumeMs) return@LaunchedEffect
        }
        pendingResumePromptMs = null
        performSeek(resumeMs)
        exoPlayer.playWhenReady = true
    }

    // Ensure live tracks are refreshed whenever Audio or Subtitle drawer opens
    LaunchedEffect(activeDrawer, exoPlayer) {
        if (activeDrawer != PlayerDrawerType.NONE) {
            lastNonNoneDrawer = activeDrawer
        }
        if (activeDrawer == PlayerDrawerType.AUDIO || activeDrawer == PlayerDrawerType.SUBTITLE) {
            val (parsedAudio, parsedSubs) = parseExoTracks(exoPlayer.currentTracks)
            audioTracks = parsedAudio
            subtitleTracks = parsedSubs
        }
    }

    // Automatically dismiss speed bar & extra top toolbar whenever HUD controls hide, screen locks, or a side drawer opens
    LaunchedEffect(isControlsVisible, isScreenLocked, activeDrawer) {
        if (!isControlsVisible || isScreenLocked || activeDrawer != PlayerDrawerType.NONE) {
            showSpeedBar = false
        }
        if (!isControlsVisible) {
            isExtraRibbonExpanded = false
        }
    }

    // Auto-hide HUD controls after 3.5s of inactivity
    LaunchedEffect(
        isControlsVisible,
        isPlaying,
        isScrubbing,
        lastInteractionTick,
        activeDrawer,
        showSpeedBar,
        showDecoderDialog,
        showSleepTimerDialog,
        showInfoDialog,
        showRenameDialog,
        showOnlineSubtitleDialog,
        pendingResumePromptMs
    ) {
        if (
            isControlsVisible &&
            isPlaying &&
            !isScrubbing &&
            activeDrawer == PlayerDrawerType.NONE &&
            !showDecoderDialog &&
            !showSleepTimerDialog &&
            !showInfoDialog &&
            !showRenameDialog &&
            !showOnlineSubtitleDialog &&
            pendingResumePromptMs == null
        ) {
            delay(3500L)
            showSpeedBar = false
            isControlsVisible = false
        }
    }

    // Auto-dismiss transient center banners 1.2s after the last button tap
    LaunchedEffect(centerBanner, toastTriggerTick) {
        val current = centerBanner
        if (current != null) {
            lastDisplayedBanner = current
        }
        if (current is CenterBannerState.Toast || current is CenterBannerState.Zoom) {
            delay(1200L)
            centerBanner = null
        }
    }

    val predictiveScale = 1f - (0.08f * predictiveBackProgress) // 1.0f -> 0.92f
    val predictiveAlpha = 1f - (0.18f * predictiveBackProgress)
    val predictiveCorner = (24f * predictiveBackProgress).dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .graphicsLayer {
                scaleX = predictiveScale
                scaleY = predictiveScale
                alpha = predictiveAlpha
                shape = RoundedCornerShape(predictiveCorner)
                clip = predictiveBackProgress > 0.01f
            }
            .testTag("video_player_screen")
    ) {
        // Hardware/FFmpeg Accelerated PlayerView with Subtitle Customization & Pinch-to-Zoom scaling
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = scaleMode.resizeMode
                    player = exoPlayer
                    keepScreenOn = true
                    activePlayerView = this
                }
            },
            update = { playerView ->
                activePlayerView = playerView
                playerView.player = exoPlayer
                playerView.resizeMode = if (isInPipMode) {
                    AspectRatioFrameLayout.RESIZE_MODE_FIT
                } else {
                    scaleMode.resizeMode
                }
                playerView.videoSurfaceView?.apply {
                    scaleX = if (isInPipMode) 1f else zoomScale
                    scaleY = if (isInPipMode) 1f else zoomScale
                }
                playerView.subtitleView?.apply {
                    val showNativeSubView = areSubtitlesEnabled &&
                        (isInPipMode || subtitleStyleConfig.useEmbeddedAssStyles)
                    visibility = if (showNativeSubView) android.view.View.VISIBLE else android.view.View.GONE
                    setApplyEmbeddedStyles(subtitleStyleConfig.useEmbeddedAssStyles)
                    setApplyEmbeddedFontSizes(subtitleStyleConfig.useEmbeddedAssStyles)
                    setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, subtitleStyleConfig.effectiveTextSizeSp)
                    setBottomPaddingFraction(subtitleStyleConfig.bottomMarginFraction)
                    val edgeType = when {
                        subtitleStyleConfig.borderEnabled -> CaptionStyleCompat.EDGE_TYPE_OUTLINE
                        subtitleStyleConfig.dropShadowEnabled -> CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW
                        else -> CaptionStyleCompat.EDGE_TYPE_NONE
                    }
                    setStyle(
                        CaptionStyleCompat(
                            subtitleStyleConfig.textColorArgb,
                            if (subtitleStyleConfig.backgroundEnabled) {
                                subtitleStyleConfig.backgroundColorArgb
                            } else {
                                android.graphics.Color.TRANSPARENT
                            },
                            android.graphics.Color.TRANSPARENT,
                            edgeType,
                            subtitleStyleConfig.borderColorArgb,
                            subtitleStyleConfig.fontFamily.toAndroidTypeface(subtitleStyleConfig.isBold)
                        )
                    )
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("exoplayer_surface_view")
        )

        // Optional Night-Mode Blue-Light Warmth Filter over video surface only
        if (nightModeActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x28FF9800))
                    .testTag("night_tint_overlay")
            )
        }

        // Dedicated Surface Gesture Layer positioned BELOW the HUD & Drawers
        if (!isInPipMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(
                        isScreenLocked,
                        durationMs,
                    settings.brightnessSwipeEnabled,
                    settings.volumeSwipeEnabled,
                    settings.horizontalSeekSwipeEnabled,
                    settings.pinchToZoomEnabled,
                    settings.holdForSpeedMultiplier,
                    settings.horizontalSwipeSensitivity,
                    settings.doubleTapSeekAreaWidthPercent,
                    settings.doubleTapLeftAction,
                    settings.doubleTapCenterAction,
                    settings.doubleTapRightAction,
                    settings.useSingleTapForCenterGesture,
                    settings.doubleTapSeekSeconds,
                    settings.hapticFeedbackEnabled,
                    longPress2xEnabled,
                    playbackSpeed,
                    chapters
                ) {
                    var lastTapTimeMs = 0L
                    var lastTapX = 0f

                    fun executeConfiguredTapAction(action: String, isRightZone: Boolean) {
                        when (action) {
                            "Seek" -> {
                                val seekDeltaMs = settings.doubleTapSeekSeconds * 1000L
                                val nextPos = if (isRightZone) {
                                    (exoPlayer.currentPosition + seekDeltaMs).coerceAtMost(durationMs)
                                } else {
                                    (exoPlayer.currentPosition - seekDeltaMs).coerceAtLeast(0L)
                                }
                                performSeek(nextPos)
                                showCenterToast(
                                    if (isRightZone) "+${settings.doubleTapSeekSeconds}s" else "-${settings.doubleTapSeekSeconds}s"
                                )
                            }
                            "Play/Pause" -> {
                                if (exoPlayer.isPlaying) {
                                    exoPlayer.pause()
                                    showCenterToast("Paused")
                                } else {
                                    if (exoPlayer.playbackState == Player.STATE_ENDED) exoPlayer.seekTo(0L)
                                    exoPlayer.play()
                                    showCenterToast("Playing")
                                }
                            }
                            "Previous" -> {
                                if (currentIndex > 0) {
                                    persistCurrentVideoMemory()
                                    currentIndex--
                                } else {
                                    performSeek(0L)
                                }
                                showCenterToast("Previous")
                            }
                            "Next" -> {
                                if (currentIndex < effectivePlaylist.lastIndex) {
                                    persistCurrentVideoMemory()
                                    currentIndex++
                                    showCenterToast("Next")
                                } else {
                                    val target = (exoPlayer.currentPosition + settings.doubleTapSeekSeconds * 1000L)
                                        .coerceAtMost(durationMs)
                                    performSeek(target)
                                    showCenterToast("+${settings.doubleTapSeekSeconds}s")
                                }
                            }
                        }
                        if (action != "None" && settings.hapticFeedbackEnabled) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    }

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = true)
                        val startX = down.position.x
                        val widthPx = size.width.toFloat().coerceAtLeast(1f)
                        val heightPx = size.height.toFloat().coerceAtLeast(1f)
                        val downTimestamp = System.currentTimeMillis()

                        var gestureType = ActiveGestureType.NONE
                        var totalDx = 0f
                        var totalDy = 0f
                        val scrubStartPosMs = exoPlayer.currentPosition
                        var targetScrubMs = scrubStartPosMs
                        var movedSignificantly = false
                        var triggeredLongPress2x = false
                        var anyConsumedByChild = down.isConsumed

                        // Multi-x Press-and-Hold Speed Boost (configured 1.5x–3.0x)
                        val longPressJob = if (!isScreenLocked && longPress2xEnabled) {
                            coroutineScope.launch {
                                delay(380L)
                                if (!movedSignificantly && !anyConsumedByChild) {
                                    triggeredLongPress2x = true
                                    isTemporary2xActive = true
                                    if (settings.hapticFeedbackEnabled) {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    exoPlayer.setPlaybackSpeed(settings.holdForSpeedMultiplier)
                                }
                            }
                        } else null

                        do {
                            val event = awaitPointerEvent()
                            val canceled = event.changes.any { it.isConsumed }
                            if (canceled) {
                                anyConsumedByChild = true
                                longPressJob?.cancel()
                            } else {
                                if (isScreenLocked) {
                                    val pan = event.calculatePan()
                                    if (abs(pan.x) > 18f || abs(pan.y) > 18f) {
                                        movedSignificantly = true
                                    }
                                } else if (event.changes.size >= 2 && settings.pinchToZoomEnabled) {
                                    longPressJob?.cancel()
                                    gestureType = ActiveGestureType.PINCH
                                    movedSignificantly = true
                                    val zoomChange = event.calculateZoom()
                                    zoomScale = (zoomScale * zoomChange).coerceIn(1.0f, 4.0f)
                                    val zoomBanner = CenterBannerState.Zoom((zoomScale * 100f).roundToInt())
                                    centerBanner = zoomBanner
                                    lastDisplayedBanner = zoomBanner
                                    toastTriggerTick++
                                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                                } else {
                                    val pan = event.calculatePan()
                                    totalDx += pan.x
                                    totalDy += pan.y

                                    if (gestureType == ActiveGestureType.NONE && !triggeredLongPress2x) {
                                        val touchSlop = 22f
                                        if (abs(totalDx) > touchSlop || abs(totalDy) > touchSlop) {
                                            movedSignificantly = true
                                            longPressJob?.cancel()
                                            gestureType = when {
                                                abs(totalDx) > abs(totalDy) * 1.2f && settings.horizontalSeekSwipeEnabled -> {
                                                    isScrubbing = true
                                                    scrubPositionMs = scrubStartPosMs
                                                    exoPlayer.setSeekParameters(SeekParameters.CLOSEST_SYNC)
                                                    ActiveGestureType.SCRUB
                                                }
                                                startX < widthPx * 0.5f && settings.brightnessSwipeEnabled ->
                                                    ActiveGestureType.BRIGHTNESS
                                                startX >= widthPx * 0.5f && settings.volumeSwipeEnabled ->
                                                    ActiveGestureType.VOLUME
                                                else -> ActiveGestureType.NONE
                                            }
                                        }
                                    }

                                    when (gestureType) {
                                        ActiveGestureType.BRIGHTNESS -> {
                                            val delta = -pan.y / (heightPx * 0.72f)
                                            brightnessFraction = (brightnessFraction + delta).coerceIn(0.01f, 1.0f)
                                            activity?.window?.let { win ->
                                                val lp = win.attributes
                                                lp.screenBrightness = brightnessFraction
                                                win.attributes = lp
                                            }
                                            rightEdgeBrightnessVisible = true
                                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                                        }
                                        ActiveGestureType.VOLUME -> {
                                            // Extends up to 2.0f (200% Software Volume Boost via LoudnessEnhancer)
                                            val delta = (-pan.y / (heightPx * 0.72f)) * 2.0f
                                            applyVolumeAndSoftwareBoost(volumeFraction + delta)
                                            leftEdgeVolumeVisible = true
                                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                                        }
                                        ActiveGestureType.SCRUB -> {
                                            // MX Player–style fine-resolution horizontal swipe scrubbing (~90s full width)
                                            val sensitivityScale = (settings.horizontalSwipeSensitivity / 50f).coerceIn(0.2f, 2.0f)
                                            val baseScrubRangeMs = minOf(durationMs, 90_000L).coerceAtLeast(10_000L)
                                            val normalizedDx = (totalDx / widthPx.coerceAtLeast(1f))
                                            val deltaMs = (normalizedDx * baseScrubRangeMs * sensitivityScale).toLong()
                                            targetScrubMs = (scrubStartPosMs + deltaMs).coerceIn(0L, durationMs)
                                            scrubPositionMs = targetScrubMs
                                            val activeChapter = findChapterForPosition(chapters, targetScrubMs)
                                            val scrubBanner = CenterBannerState.Scrub(
                                                targetTimeMs = targetScrubMs,
                                                deltaMs = targetScrubMs - scrubStartPosMs,
                                                chapterTitle = activeChapter?.title
                                            )
                                            centerBanner = scrubBanner
                                            lastDisplayedBanner = scrubBanner
                                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                                        }
                                        else -> Unit
                                    }
                                }
                            }
                        } while (event.changes.any { it.pressed })

                        longPressJob?.cancel()

                        if (triggeredLongPress2x) {
                            isTemporary2xActive = false
                            exoPlayer.setPlaybackSpeed(playbackSpeed)
                        }

                        if (gestureType == ActiveGestureType.SCRUB) {
                            performSeek(targetScrubMs, useExactSeek = true)
                            isScrubbing = false
                            centerBanner = null
                        }
                        if (gestureType == ActiveGestureType.BRIGHTNESS) {
                            rightEdgeBrightnessVisible = false
                            if (latestSettings.brightnessMemoryMode != BrightnessMemoryMode.OFF) {
                                latestOnSaveBrightnessPreference(
                                    latestCurrentVideo.uri.toString(),
                                    brightnessFraction
                                )
                            }
                        }
                        if (gestureType == ActiveGestureType.VOLUME) {
                            leftEdgeVolumeVisible = false
                        }

                        if (!movedSignificantly && !triggeredLongPress2x && !anyConsumedByChild) {
                            val now = System.currentTimeMillis()
                            val zoneFraction = (settings.doubleTapSeekAreaWidthPercent / 100f).coerceIn(0.15f, 0.45f)
                            val isLeftZone = startX < widthPx * zoneFraction
                            val isRightZone = startX > widthPx * (1f - zoneFraction)
                            val isCenterZone = !isLeftZone && !isRightZone

                            if (!isScreenLocked && now - lastTapTimeMs < 260L && abs(startX - lastTapX) < widthPx * 0.35f) {
                                when {
                                    isLeftZone -> executeConfiguredTapAction(settings.doubleTapLeftAction, isRightZone = false)
                                    isRightZone -> executeConfiguredTapAction(settings.doubleTapRightAction, isRightZone = true)
                                    else -> executeConfiguredTapAction(settings.doubleTapCenterAction, isRightZone = true)
                                }
                                lastTapTimeMs = 0L
                            } else {
                                lastTapTimeMs = downTimestamp
                                lastTapX = startX
                                if (activeDrawer != PlayerDrawerType.NONE) {
                                    activeDrawer = PlayerDrawerType.NONE
                                } else if (showSpeedBar) {
                                    showSpeedBar = false
                                } else if (!isScreenLocked && settings.useSingleTapForCenterGesture && isCenterZone) {
                                    executeConfiguredTapAction(settings.doubleTapCenterAction, isRightZone = true)
                                } else {
                                    val nextVisible = !isControlsVisible
                                    isControlsVisible = nextVisible
                                    if (!nextVisible) {
                                        showSpeedBar = false
                                    }
                                }
                                lastInteractionTick++
                            }
                        }
                    }
                }
        )
        }

        // Cinema-Grade HUD-Aware Subtitle Overlay with Direct Vertical Drag & Center `↕ <0–150>` OSD Readout
        SubtitleOverlay(
            cueLines = activeCueLines,
            areSubtitlesEnabled = areSubtitlesEnabled && !subtitleStyleConfig.useEmbeddedAssStyles,
            isInPipMode = isInPipMode,
            isScreenLocked = isScreenLocked,
            controlsVisible = isControlsVisible && !isScreenLocked && activeDrawer == PlayerDrawerType.NONE,
            bottomHudHeightDp = bottomHudHeightDp,
            styleConfig = subtitleStyleConfig,
            isSubtitleDrawerOpen = activeDrawer == PlayerDrawerType.SUBTITLE,
            onBottomMarginDragUpdate = { newMargin ->
                subtitleStyleConfig = subtitleStyleConfig.copy(bottomMargin = newMargin.coerceIn(0, 150))
            },
            onBottomMarginDragCommit = { finalMargin ->
                val clampedMargin = finalMargin.coerceIn(0, 150)
                subtitleStyleConfig = subtitleStyleConfig.copy(bottomMargin = clampedMargin)
                coroutineScope.launch {
                    subtitlePreferences.saveBottomMargin(clampedMargin)
                }
            }
        )

        // Multi-x Press-and-Hold Floating Pill (`2.0x ▶▶`) near top center of screen
        AnimatedVisibility(
            visible = !isInPipMode && isTemporary2xActive,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(top = 22.dp)
        ) {
            val formattedHoldSpeed = remember(settings.holdForSpeedMultiplier) {
                val raw = String.format(Locale.US, "%.2f", settings.holdForSpeedMultiplier)
                raw.trimEnd('0').trimEnd('.') + if (raw.contains('.')) "" else ""
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.78f))
                    .border(1.dp, PlayerSeekPeriwinkle.copy(alpha = 0.7f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 18.dp, vertical = 8.dp)
                    .testTag("floating_2x_speed_pill"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${formattedHoldSpeed}x ▶▶",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Horizontal safe drawing + display cutout insets so volume/brightness pills never overlap
        // the punch-hole camera cutout regardless of landscape rotation direction.
        val horizontalSafeInsets = WindowInsets.safeDrawing
            .union(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal)
            .asPaddingValues()

        // OPPOSITE-SIDE VERTICAL VOLUME SLIDER (Pinned to LEFT edge during Right-Side Swipe, supports up to 200% Boost)
        AnimatedVisibility(
            visible = !isInPipMode && leftEdgeVolumeVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(horizontalSafeInsets)
                .padding(start = 16.dp)
        ) {
            val volPercent = (volumeFraction * 100f).roundToInt().coerceIn(0, 200)
            val isBoostActive = volPercent > 100
            VerticalEdgeSliderPill(
                icon = if (volumeFraction <= 0.01f) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                fraction = (volumeFraction / 2.0f).coerceIn(0f, 1f),
                labelText = "$volPercent%",
                isBoostActive = isBoostActive,
                modifier = Modifier.testTag("left_edge_volume_slider")
            )
        }

        // OPPOSITE-SIDE VERTICAL BRIGHTNESS SLIDER (Pinned to RIGHT edge during Left-Side Swipe)
        AnimatedVisibility(
            visible = !isInPipMode && rightEdgeBrightnessVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(horizontalSafeInsets)
                .padding(end = 16.dp)
        ) {
            VerticalEdgeSliderPill(
                icon = Icons.Filled.WbSunny,
                fraction = brightnessFraction,
                labelText = "${(brightnessFraction * 100f).roundToInt()}%",
                isBoostActive = false,
                modifier = Modifier.testTag("right_edge_brightness_slider")
            )
        }

        // Exact Center Action Feedback / Seek / Zoom Pill (`Alignment.Center`)
        AnimatedVisibility(
            visible = !isInPipMode && centerBanner != null,
            enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.92f, animationSpec = tween(150)),
            exit = fadeOut(tween(200)),
            modifier = Modifier
                .align(Alignment.Center)
                .zIndex(15f)
        ) {
            val bannerToRender = centerBanner ?: lastDisplayedBanner
            if (bannerToRender != null) {
                CenterSeekOrStatusPill(banner = bannerToRender)
            }
        }

        // Interactive 6-Second Tap-to-Execute Liquid Glass Playback Popup
        // ("Skip Intro ›|", "Skip Outro ›|", "Next Video ›|") with circular countdown progress ring
        val popupVisible = activeInteractivePopup != null &&
            !isScreenLocked &&
            !isInPipMode &&
            !isScrubbing &&
            activeDrawer == PlayerDrawerType.NONE
        AnimatedVisibility(
            visible = popupVisible,
            enter = fadeIn(tween(220)) + slideInVertically { it / 2 },
            exit = fadeOut(tween(200)) + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(end = 20.dp, bottom = if (isPortrait) 172.dp else 116.dp)
                .zIndex(12f)
        ) {
            val popup = activeInteractivePopup
            if (popup != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .shadow(10.dp, RoundedCornerShape(50))
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF1E293B).copy(alpha = 0.86f),
                                    Color(0xFF2563EB).copy(alpha = 0.78f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.75f),
                                    PlayerSeekPeriwinkle.copy(alpha = 0.55f)
                                )
                            ),
                            shape = RoundedCornerShape(50)
                        )
                        .clickable {
                            if (popup.markerId !in dismissedMarkerIds) {
                                dismissedMarkerIds.add(popup.markerId)
                            }
                            activeInteractivePopup = null
                            when (popup) {
                                is InteractivePlaybackPopup.SkipIntro -> {
                                    performSeek(popup.targetSeekMs)
                                    showCenterToast("Skipped Intro")
                                }
                                is InteractivePlaybackPopup.SkipOutro -> {
                                    if (popup.hasPostCredits) {
                                        performSeek(popup.targetSeekMs)
                                        showCenterToast("Skipped Outro")
                                    } else if (effectivePlaylist.size > 1 && currentIndex < effectivePlaylist.lastIndex) {
                                        persistCurrentVideoMemory(durationMs)
                                        showCenterToast("Skipped Outro")
                                        currentIndex = (currentIndex + 1).coerceAtMost(effectivePlaylist.lastIndex)
                                    } else {
                                        performSeek(durationMs)
                                        exoPlayer.pause()
                                        showCenterToast("Skipped Outro")
                                    }
                                }
                                is InteractivePlaybackPopup.NextVideo -> {
                                    persistCurrentVideoMemory()
                                    currentIndex = (currentIndex + 1).coerceAtMost(effectivePlaylist.lastIndex)
                                }
                            }
                            lastInteractionTick++
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .testTag(
                            when (popup) {
                                is InteractivePlaybackPopup.SkipIntro -> "interactive_skip_intro_popup"
                                is InteractivePlaybackPopup.SkipOutro -> "interactive_skip_outro_popup"
                                is InteractivePlaybackPopup.NextVideo -> "next_episode_pill"
                            }
                        )
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(22.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { popupCountdownFraction.coerceIn(0f, 1f) },
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.24f),
                            strokeWidth = 2.2.dp,
                            modifier = Modifier.fillMaxSize()
                        )
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = popup.actionLabel,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        if (!popup.subtitleText.isNullOrBlank()) {
                            Text(
                                text = popup.subtitleText!!,
                                color = Color.White.copy(alpha = 0.82f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 160.dp)
                            )
                        }
                    }
                }
            }
        }

        // Screen Locked Floating Unlock Icon in TOP-LEFT corner
        AnimatedVisibility(
            visible = !isInPipMode && isScreenLocked && isControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(start = 20.dp, top = 20.dp)
        ) {
            FrostedCircleHudButton(
                icon = Icons.Filled.Lock,
                contentDescription = "Unlock Screen",
                size = 48.dp,
                onClick = {
                    isScreenLocked = false
                    lastInteractionTick++
                },
                modifier = Modifier.testTag("unlock_screen_controls_button")
            )
        }

        // MAIN MX PLAYER HUD (Top Header + Second-Row Quick Ribbon + Bottom Seek/Transport)
        AnimatedVisibility(
            visible = !isInPipMode && isControlsVisible && !isScreenLocked && activeDrawer == PlayerDrawerType.NONE,
            enter = fadeIn(tween(durationMillis = 220, easing = FastOutSlowInEasing)),
            exit = fadeOut(tween(durationMillis = 220, easing = FastOutSlowInEasing)),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(2f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val topHudAlpha by animateFloatAsState(
                    targetValue = if (isScrubbing) 0f else 1f,
                    animationSpec = tween(durationMillis = 150),
                    label = "top_hud_scrub_fade"
                )

                // Top Gradient Scrim + Header Bar + Second-Row Circular Quick-Action Ribbon
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .graphicsLayer { alpha = topHudAlpha }
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.78f),
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Transparent
                                )
                            )
                        )
                        .clickable(
                            enabled = !isScrubbing,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { lastInteractionTick++ }
                        )
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    // 1. Top Header Bar: Back Arrow + Marquee Title + Audio Track + Subtitles + Decoder Pill + More (⋮)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = { requestPlayerExit() },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("player_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }

                        Text(
                            text = currentVideo.displayName,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                                .basicMarquee()
                        )

                        IconButton(
                            onClick = {
                                val (parsedAudio, parsedSubs) = parseExoTracks(exoPlayer.currentTracks)
                                audioTracks = parsedAudio
                                subtitleTracks = parsedSubs
                                showSpeedBar = false
                                isControlsVisible = false
                                lastNonNoneDrawer = PlayerDrawerType.AUDIO
                                activeDrawer = PlayerDrawerType.AUDIO
                                lastInteractionTick++
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("player_audio_track_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Audiotrack,
                                contentDescription = "Audio Track",
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = {
                                val (parsedAudio, parsedSubs) = parseExoTracks(exoPlayer.currentTracks)
                                audioTracks = parsedAudio
                                subtitleTracks = parsedSubs
                                showSpeedBar = false
                                isControlsVisible = false
                                lastNonNoneDrawer = PlayerDrawerType.SUBTITLE
                                activeDrawer = PlayerDrawerType.SUBTITLE
                                lastInteractionTick++
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("player_subtitles_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Subtitles,
                                contentDescription = "Subtitles",
                                tint = if (areSubtitlesEnabled) Color.White else Color.White.copy(alpha = 0.5f)
                            )
                        }

                        // Outlined Decoder Mode Pill (HW / HW+ / SW)
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.2.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.45f))
                                .clickable {
                                    showSpeedBar = false
                                    showDecoderDialog = true
                                    lastInteractionTick++
                                }
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                                .testTag("player_decoder_pill"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = activeDecoderMode.badgeText,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = {
                                showSpeedBar = false
                                isControlsVisible = false
                                lastNonNoneDrawer = PlayerDrawerType.MORE_OPTIONS
                                activeDrawer = PlayerDrawerType.MORE_OPTIONS
                                lastInteractionTick++
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("player_more_options_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "More Options",
                                tint = Color.White
                            )
                        }
                    }

                    val sleepTimerBadgeLabel: String? = when (sleepTimerSelection) {
                        is SleepTimerSelection.Off -> null
                        is SleepTimerSelection.Minutes -> {
                            val secs = sleepTimerRemainingSeconds ?: 0
                            String.format(Locale.US, "%02d:%02d", secs / 60, secs % 60)
                        }
                        is SleepTimerSelection.EndOfVideo -> "End of video"
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 2. Single-Line Quick-Action Ribbon:
                    // Newly revealed tools expand on the LEFT (`expandFrom = Alignment.End`),
                    // smoothly pushing the core visible tools (`Equalizer, 1X, Screenshot, Background Play, Rotation`)
                    // and the `>` / `<` toggle button across to the RIGHT in one fluid horizontal motion.
                    val chevronRotation by animateFloatAsState(
                        targetValue = if (isExtraRibbonExpanded) 180f else 0f,
                        animationSpec = spring(dampingRatio = 0.82f, stiffness = 360f),
                        label = "ribbon_expand_chevron_rotation"
                    )
                    val exactFrameDurationMs = remember(currentVideo.frameRate) {
                        (1000f / currentVideo.frameRate.coerceAtLeast(1f)).toLong().coerceAtLeast(16L)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .testTag("player_quick_ribbon")
                    ) {
                        // LEFT: Newly Revealed Tools (Night Mode, Shuffle, Loop, Mute, Sleep Timer, A-B Repeat, Frame Step)
                        AnimatedVisibility(
                            visible = isExtraRibbonExpanded,
                            enter = expandHorizontally(
                                expandFrom = Alignment.End,
                                animationSpec = spring(dampingRatio = 0.82f, stiffness = 360f)
                            ) + fadeIn(animationSpec = tween(200)),
                            exit = shrinkHorizontally(
                                shrinkTowards = Alignment.End,
                                animationSpec = tween(200)
                            ) + fadeOut(animationSpec = tween(150))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Night Tint (warm amber blue-light filter overlay)
                                FrostedCircleHudButton(
                                    icon = Icons.Filled.DarkMode,
                                    contentDescription = "Night Tint",
                                    size = 40.dp,
                                    iconSize = 19.dp,
                                    isActiveHighlight = nightModeActive,
                                    onClick = {
                                        nightModeActive = !nightModeActive
                                        showCenterToast(if (nightModeActive) "Night Tint: ON" else "Night Tint: OFF")
                                        lastInteractionTick++
                                    },
                                    modifier = Modifier.testTag("ribbon_night_tint_button")
                                )

                                // Shuffle
                                FrostedCircleHudButton(
                                    icon = Icons.Filled.Shuffle,
                                    contentDescription = "Shuffle",
                                    size = 40.dp,
                                    iconSize = 19.dp,
                                    isActiveHighlight = shuffleActive,
                                    onClick = {
                                        shuffleActive = !shuffleActive
                                        showCenterToast(if (shuffleActive) "Shuffle: ON" else "Shuffle: OFF")
                                        lastInteractionTick++
                                    },
                                    modifier = Modifier.testTag("ribbon_shuffle_button")
                                )

                                // Loop Current Video
                                FrostedCircleHudButton(
                                    icon = if (loopSingleActive) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                                    contentDescription = "Loop",
                                    size = 40.dp,
                                    iconSize = 19.dp,
                                    isActiveHighlight = loopSingleActive,
                                    onClick = {
                                        loopSingleActive = !loopSingleActive
                                        exoPlayer.repeatMode = if (loopSingleActive) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                                        showCenterToast(if (loopSingleActive) "Loop Current Video: ON" else "Loop: OFF")
                                        lastInteractionTick++
                                    },
                                    modifier = Modifier.testTag("ribbon_loop_button")
                                )

                                // Mute / Unmute
                                FrostedCircleHudButton(
                                    icon = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Mute",
                                    size = 40.dp,
                                    iconSize = 19.dp,
                                    isActiveHighlight = isMuted,
                                    onClick = {
                                        isMuted = !isMuted
                                        exoPlayer.volume = if (isMuted) 0f else 1f
                                        showCenterToast(if (isMuted) "Muted" else "Audio Unmuted")
                                        lastInteractionTick++
                                    },
                                    modifier = Modifier.testTag("ribbon_mute_button")
                                )

                                // Sleep Timer
                                FrostedCircleHudButton(
                                    icon = Icons.Filled.Timer,
                                    contentDescription = "Sleep Timer",
                                    size = 40.dp,
                                    iconSize = 19.dp,
                                    isActiveHighlight = sleepTimerSelection != SleepTimerSelection.Off,
                                    onClick = {
                                        showSleepTimerDialog = true
                                        lastInteractionTick++
                                    },
                                    modifier = Modifier.testTag("ribbon_sleep_timer_button")
                                )

                                // A–B Loop Repeater circular button
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                abRepeatEndMs != null -> Color(0xFF22D3EE).copy(alpha = 0.35f)
                                                abRepeatStartMs != null -> PlayerSeekPeriwinkle.copy(alpha = 0.45f)
                                                else -> PlayerHudGlassBg
                                            }
                                        )
                                        .border(
                                            1.dp,
                                            when {
                                                abRepeatEndMs != null -> Color(0xFF22D3EE)
                                                abRepeatStartMs != null -> PlayerSeekPeriwinkle
                                                else -> PlayerHudGlassBorder
                                            },
                                            CircleShape
                                        )
                                        .clickable {
                                            val livePos = exoPlayer.currentPosition.coerceAtLeast(0L)
                                            when {
                                                abRepeatStartMs == null -> {
                                                    abRepeatStartMs = livePos
                                                    showCenterToast("Point A set")
                                                }
                                                abRepeatEndMs == null -> {
                                                    val startA = abRepeatStartMs!!
                                                    if (abs(livePos - startA) < 300L) {
                                                        showCenterToast("Move playhead past A")
                                                    } else if (livePos < startA) {
                                                        abRepeatStartMs = livePos
                                                        abRepeatEndMs = startA
                                                        showCenterToast("A–B loop active")
                                                    } else {
                                                        abRepeatEndMs = livePos
                                                        showCenterToast("A–B loop active")
                                                    }
                                                }
                                                else -> {
                                                    abRepeatStartMs = null
                                                    abRepeatEndMs = null
                                                    showCenterToast("A–B loop cleared")
                                                }
                                            }
                                            lastInteractionTick++
                                        }
                                        .testTag("ribbon_ab_loop_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Repeat,
                                        contentDescription = "A-B Loop",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .offset(y = (-4).dp)
                                    )
                                    Text(
                                        text = when {
                                            abRepeatEndMs != null -> "A-B"
                                            abRepeatStartMs != null -> "A→"
                                            else -> "A-B"
                                        },
                                        color = if (abRepeatStartMs != null) Color(0xFF22D3EE) else Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.offset(y = 8.dp)
                                    )
                                }

                                // Previous Frame (<|)
                                Box(
                                    modifier = Modifier
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(PlayerHudGlassBg)
                                        .border(1.dp, PlayerHudGlassBorder, RoundedCornerShape(999.dp))
                                        .clickable {
                                            exoPlayer.pause()
                                            val prevFramePos = (exoPlayer.currentPosition - exactFrameDurationMs).coerceAtLeast(0L)
                                            performSeek(prevFramePos)
                                            showCenterToast("Frame -${exactFrameDurationMs}ms")
                                            lastInteractionTick++
                                        }
                                        .padding(horizontal = 12.dp)
                                        .testTag("ribbon_prev_frame_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "<|",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Next Frame (|>)
                                Box(
                                    modifier = Modifier
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(PlayerHudGlassBg)
                                        .border(1.dp, PlayerHudGlassBorder, RoundedCornerShape(999.dp))
                                        .clickable {
                                            exoPlayer.pause()
                                            val nextFramePos = (exoPlayer.currentPosition + exactFrameDurationMs).coerceAtMost(durationMs)
                                            performSeek(nextFramePos)
                                            showCenterToast("Frame +${exactFrameDurationMs}ms")
                                            lastInteractionTick++
                                        }
                                        .padding(horizontal = 12.dp)
                                        .testTag("ribbon_next_frame_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "|>",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // EXISTING CORE TOOLS (glide smoothly to the right when expanded):
                        // 1. Equalizer
                        FrostedCircleHudButton(
                            icon = Icons.Filled.Equalizer,
                            contentDescription = "Equalizer",
                            size = 40.dp,
                            iconSize = 20.dp,
                            isActiveHighlight = eqPreset != EqPreset.NORMAL || bassBoostStrength > 0f,
                            onClick = {
                                showSpeedBar = false
                                isControlsVisible = false
                                lastNonNoneDrawer = PlayerDrawerType.EQUALIZER
                                activeDrawer = PlayerDrawerType.EQUALIZER
                                lastInteractionTick++
                            },
                            modifier = Modifier.testTag("ribbon_equalizer_button")
                        )

                        // 2. Playback Speed (1X)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (showSpeedBar || playbackSpeed != 1.0f) PlayerSeekPeriwinkle.copy(alpha = 0.45f)
                                    else PlayerHudGlassBg
                                )
                                .border(1.dp, PlayerHudGlassBorder, CircleShape)
                                .clickable {
                                    showSpeedBar = !showSpeedBar
                                    lastInteractionTick++
                                }
                                .testTag("playback_speed_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            val speedLabel = if (playbackSpeed % 1f == 0f) {
                                "${playbackSpeed.toInt()}X"
                            } else {
                                "${String.format(Locale.US, "%.2f", playbackSpeed).trimEnd('0').trimEnd('.')}X"
                            }
                            Text(
                                text = speedLabel,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // 3. Screenshot
                        FrostedCircleHudButton(
                            icon = Icons.Filled.CameraAlt,
                            contentDescription = "Screenshot",
                            size = 40.dp,
                            iconSize = 19.dp,
                            onClick = {
                                lastInteractionTick++
                                coroutineScope.launch {
                                    val saved = captureFrameToPictures(context, currentVideo, currentPositionMs)
                                    showCenterToast(
                                        if (saved) "Screenshot saved to Pictures" else "Unable to capture frame"
                                    )
                                }
                            },
                            modifier = Modifier.testTag("ribbon_screenshot_button")
                        )

                        // 4. Background Play (Headphones)
                        FrostedCircleHudButton(
                            icon = Icons.Filled.Headset,
                            contentDescription = "Background Play",
                            size = 40.dp,
                            iconSize = 19.dp,
                            isActiveHighlight = backgroundAudioActive,
                            onClick = {
                                backgroundAudioActive = !backgroundAudioActive
                                if (backgroundAudioActive) {
                                    runCatching {
                                        context.startService(Intent(context, PlaybackService::class.java))
                                    }
                                }
                                showCenterToast(
                                    if (backgroundAudioActive) "Background Play: ON" else "Background Play: OFF"
                                )
                                lastInteractionTick++
                            },
                            modifier = Modifier.testTag("background_audio_button")
                        )

                        // 5. Screen Rotation
                        FrostedCircleHudButton(
                            icon = Icons.Filled.ScreenRotation,
                            contentDescription = "Screen Rotation",
                            size = 40.dp,
                            iconSize = 19.dp,
                            onClick = {
                                manualOrientationOverridden = true
                                isLandscape = !isLandscape
                                activity?.requestedOrientation = if (isLandscape) {
                                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                } else {
                                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                }
                                lastInteractionTick++
                            },
                            modifier = Modifier.testTag("screen_rotation_button")
                        )

                        // RIGHT END: Smoothly Rotating `>` / `<` Toggle Button
                        FrostedCircleHudButton(
                            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = if (isExtraRibbonExpanded) "Collapse Quick Tools" else "Expand Quick Tools",
                            size = 40.dp,
                            iconSize = 20.dp,
                            iconModifier = Modifier.graphicsLayer { rotationZ = chevronRotation },
                            onClick = {
                                isExtraRibbonExpanded = !isExtraRibbonExpanded
                                lastInteractionTick++
                            },
                            modifier = Modifier.testTag("ribbon_expand_button")
                        )
                    }

                    // Live Sleep Timer Countdown Badge inside HUD
                    if (sleepTimerBadgeLabel != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier
                                .padding(start = 10.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(PlayerHudGlassBg)
                                .border(1.dp, PlayerSeekPeriwinkle.copy(alpha = 0.65f), RoundedCornerShape(999.dp))
                                .clickable {
                                    showSleepTimerDialog = true
                                    lastInteractionTick++
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .testTag("player_sleep_timer_badge")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Timer,
                                contentDescription = "Sleep Timer Active",
                                tint = PlayerSeekPeriwinkle,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = sleepTimerBadgeLabel,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // 3. BOTTOM TRANSPORT & SEEK AREA (Pinned to bottom with subtle dark gradient scrim)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .zIndex(2f)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.45f),
                                    Color.Black.copy(alpha = 0.82f)
                                )
                            )
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { lastInteractionTick++ }
                        )
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing
                                .union(WindowInsets.displayCutout)
                                .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                        )
                        .onSizeChanged { bottomHudHeightPx = it.height }
                        .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp)
                ) {
                    // Seek Row: Left Timestamp + Sky-Blue (#58A6FF) Slider with Thumb-Anchored Tooltip + Right Total Duration
                    SeekBarWithFloatingTooltip(
                        positionProvider = {
                            if (isScrubbing) scrubPositionMs else (pendingSeekPositionMs ?: currentPositionMs)
                        },
                        durationMs = durationMs,
                        isPlaying = isPlaying && !isScrubbing,
                        isScrubbing = isScrubbing,
                        chapters = chapters,
                        abLoopStartMs = abRepeatStartMs,
                        abLoopEndMs = abRepeatEndMs,
                        onSeekStartOrUpdate = { targetMs ->
                            if (!isScrubbing) {
                                exoPlayer.setSeekParameters(SeekParameters.CLOSEST_SYNC)
                            }
                            isScrubbing = true
                            scrubPositionMs = targetMs
                            centerBanner = null
                            lastInteractionTick++
                        },
                        onSeekCommit = { targetMs ->
                            scrubPositionMs = targetMs
                            performSeek(targetMs, useExactSeek = true)
                            centerBanner = null
                            lastInteractionTick++
                            isScrubbing = false
                        },
                        seekBarTestTag = "player_seek_slider",
                        showTimestamps = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(if (isPortrait) 8.dp else 4.dp))

                    val onLockClick: () -> Unit = {
                        isScreenLocked = true
                        isControlsVisible = true
                        lastInteractionTick++
                    }
                    val onPreviousClick: () -> Unit = {
                        if (currentIndex > 0) {
                            persistCurrentVideoMemory()
                            currentIndex--
                        } else {
                            performSeek(0L)
                        }
                        lastInteractionTick++
                    }
                    val onRewind10sClick: () -> Unit = {
                        val target = (exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)
                        performSeek(target)
                        lastInteractionTick++
                    }
                    val onPlayPauseClick: () -> Unit = {
                        if (exoPlayer.isPlaying) {
                            exoPlayer.pause()
                        } else {
                            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                                exoPlayer.seekTo(0L)
                            }
                            exoPlayer.play()
                        }
                        lastInteractionTick++
                    }
                    val onForward10sClick: () -> Unit = {
                        val target = (exoPlayer.currentPosition + 10_000L).coerceAtMost(durationMs)
                        performSeek(target)
                        lastInteractionTick++
                    }
                    val onNextClick: () -> Unit = {
                        if (currentIndex < effectivePlaylist.lastIndex) {
                            persistCurrentVideoMemory()
                            currentIndex++
                        } else {
                            val target = (exoPlayer.currentPosition + settings.doubleTapSeekSeconds * 1000L)
                                .coerceAtMost(durationMs)
                            performSeek(target)
                        }
                        lastInteractionTick++
                    }
                    val onPipClick: () -> Unit = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
                            try {
                                val pipParams = buildPlayerPictureInPictureParams(
                                    context = context,
                                    width = activeVideoWidth,
                                    height = activeVideoHeight,
                                    rotationDegrees = activeVideoRotation,
                                    isPlaying = isPlaying,
                                    autoEnterEnabled = isPlaying && !isScreenLocked && settings.backgroundAudioOrPipOnHome
                                )
                                activity.enterPictureInPictureMode(pipParams)
                            } catch (_: Exception) {
                                showCenterToast("PiP Mode Requested")
                            }
                        } else {
                            showCenterToast("PiP requires Android 8.0+")
                        }
                        lastInteractionTick++
                    }
                    val onScaleModeClick: () -> Unit = {
                        val modes = PlayerScaleMode.entries
                        val nextMode = modes[(modes.indexOf(scaleMode) + 1) % modes.size]
                        scaleMode = nextMode
                        zoomScale = 1.0f
                        persistCurrentVideoMemory()
                        showCenterToast(nextMode.label)
                        lastInteractionTick++
                    }
                    val onRotateClick: () -> Unit = {
                        manualOrientationOverridden = true
                        isLandscape = !isLandscape
                        activity?.requestedOrientation = if (isLandscape) {
                            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        } else {
                            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        }
                        lastInteractionTick++
                    }

                    if (isPortrait) {
                        // Portrait: Two clean, well-spaced rows to prevent PiP & transport button crowding
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Row 1 — Primary Transport Row (Centered): Previous • Rewind -10s • Play/Pause • Forward +10s • Next
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("portrait_primary_transport_row")
                            ) {
                                IconButton(
                                    onClick = onPreviousClick,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .testTag("player_previous_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SkipPrevious,
                                        contentDescription = "Previous Video",
                                        tint = Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }

                                IconButton(
                                    onClick = onRewind10sClick,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .testTag("player_rewind_10s_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Replay10,
                                        contentDescription = "Rewind 10 seconds",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(PlayerHudGlassBg)
                                        .border(1.dp, PlayerHudGlassBorder, CircleShape)
                                        .clickable(onClick = onPlayPauseClick)
                                        .testTag("play_pause_button")
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }

                                IconButton(
                                    onClick = onForward10sClick,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .testTag("player_forward_10s_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Forward10,
                                        contentDescription = "Forward 10 seconds",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                IconButton(
                                    onClick = onNextClick,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .testTag("player_next_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SkipNext,
                                        contentDescription = "Next Video",
                                        tint = Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }

                            // Row 2 — Secondary Utility Row: Left Lock | Right PiP + Aspect Ratio + Rotate Orientation
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .testTag("portrait_secondary_utility_row")
                            ) {
                                IconButton(
                                    onClick = onLockClick,
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .size(48.dp)
                                        .testTag("lock_screen_controls_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.LockOpen,
                                        contentDescription = "Lock Screen Controls",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.align(Alignment.CenterEnd)
                                ) {
                                    IconButton(
                                        onClick = onPipClick,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("pip_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.PictureInPictureAlt,
                                            contentDescription = "Picture-in-Picture",
                                            tint = Color.White,
                                            modifier = Modifier.size(23.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = onScaleModeClick,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("aspect_ratio_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.AspectRatio,
                                            contentDescription = "Aspect Ratio / Resize Mode",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = onRotateClick,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("bottom_screen_rotation_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ScreenRotation,
                                            contentDescription = "Rotate Orientation",
                                            tint = Color.White,
                                            modifier = Modifier.size(23.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Landscape: Unified wide single-row transport layout with >= 14.dp spacing
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("landscape_transport_row")
                        ) {
                            // Far Left: Screen Lock (Padlock)
                            IconButton(
                                onClick = onLockClick,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("lock_screen_controls_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LockOpen,
                                    contentDescription = "Lock Screen Controls",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Center: Previous, Rewind -10s, Play/Pause, Forward +10s, Next
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = onPreviousClick,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("player_previous_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SkipPrevious,
                                        contentDescription = "Previous Video",
                                        tint = Color.White,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }

                                IconButton(
                                    onClick = onRewind10sClick,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("player_rewind_10s_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Replay10,
                                        contentDescription = "Rewind 10 seconds",
                                        tint = Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(PlayerHudGlassBg)
                                        .border(1.dp, PlayerHudGlassBorder, CircleShape)
                                        .clickable(onClick = onPlayPauseClick)
                                        .testTag("play_pause_button")
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }

                                IconButton(
                                    onClick = onForward10sClick,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("player_forward_10s_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Forward10,
                                        contentDescription = "Forward 10 seconds",
                                        tint = Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }

                                IconButton(
                                    onClick = onNextClick,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("player_next_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SkipNext,
                                        contentDescription = "Next Video",
                                        tint = Color.White,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }

                            // Far Right: Picture-in-Picture (PiP) + Aspect Ratio / Resize Mode
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = onPipClick,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("pip_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PictureInPictureAlt,
                                        contentDescription = "Picture-in-Picture",
                                        tint = Color.White,
                                        modifier = Modifier.size(23.dp)
                                    )
                                }

                                IconButton(
                                    onClick = onScaleModeClick,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("aspect_ratio_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AspectRatio,
                                        contentDescription = "Aspect Ratio / Resize Mode",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Elevated Floating Playback Speed Bar (`zIndex(20f)`) above gesture & HUD layers
        AnimatedVisibility(
            visible = !isInPipMode &&
                showSpeedBar &&
                isControlsVisible &&
                !isScreenLocked &&
                activeDrawer == PlayerDrawerType.NONE,
            enter = fadeIn(tween(150)) + slideInVertically { it / 3 },
            exit = fadeOut(tween(150)) + slideOutVertically { it / 3 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(20f)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(bottom = if (isPortrait) 184.dp else 128.dp)
        ) {
            FloatingPlaybackSpeedBar(
                speed = playbackSpeed,
                onSpeedChange = { newSpeed ->
                    val clamped = newSpeed.coerceIn(0.25f, 4.0f)
                    playbackSpeed = clamped
                    exoPlayer.setPlaybackSpeed(clamped)
                    exoPlayer.playbackParameters = PlaybackParameters(clamped)
                    lastInteractionTick++
                },
                onClose = {
                    showSpeedBar = false
                    lastInteractionTick++
                }
            )
        }

        // Optional compact Wavy Seekbar when HUD controls are hidden
        AnimatedVisibility(
            visible = !isInPipMode &&
                settings.showSeekbarWhenControlsHidden &&
                !isControlsVisible &&
                !isScreenLocked &&
                activeDrawer == PlayerDrawerType.NONE,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .zIndex(1.5f)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            SeekBarWithFloatingTooltip(
                positionProvider = {
                    if (isScrubbing) scrubPositionMs else (pendingSeekPositionMs ?: currentPositionMs)
                },
                durationMs = durationMs,
                isPlaying = isPlaying && !isScrubbing,
                isScrubbing = isScrubbing,
                chapters = chapters,
                abLoopStartMs = abRepeatStartMs,
                abLoopEndMs = abRepeatEndMs,
                onSeekStartOrUpdate = { targetMs ->
                    if (!isScrubbing) {
                        exoPlayer.setSeekParameters(SeekParameters.CLOSEST_SYNC)
                    }
                    isScrubbing = true
                    scrubPositionMs = targetMs
                    centerBanner = null
                },
                onSeekCommit = { targetMs ->
                    scrubPositionMs = targetMs
                    performSeek(targetMs, useExactSeek = true)
                    isScrubbing = false
                },
                seekBarTestTag = "hidden_controls_wavy_seekbar",
                showTimestamps = false,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Sleek Translucent Resume Prompt Pill for 3-State "Ask" Mode (auto-selects after 4s)
        AnimatedVisibility(
            visible = !isInPipMode && pendingResumePromptMs != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.Center)
                .zIndex(4f)
                .padding(horizontal = 24.dp)
        ) {
            val targetMs = pendingResumePromptMs ?: 0L
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF101524).copy(alpha = 0.90f))
                    .border(1.dp, Color.White.copy(alpha = 0.24f), RoundedCornerShape(28.dp))
                    .padding(horizontal = 22.dp, vertical = 18.dp)
                    .testTag("resume_ask_dialog")
            ) {
                Text(
                    text = "Resume from ${formatDurationMillis(targetMs)}?",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Auto-resuming in ${resumePromptCountdownSec}s",
                    color = Color.White.copy(alpha = 0.68f),
                    fontSize = 12.sp
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(999.dp))
                            .clickable {
                                pendingResumePromptMs = null
                                performSeek(0L)
                                exoPlayer.playWhenReady = true
                            }
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                            .testTag("resume_ask_start_over")
                    ) {
                        Text(
                            text = "Start Over",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(PlayerSeekPeriwinkle)
                            .clickable {
                                pendingResumePromptMs = null
                                performSeek(targetMs)
                                exoPlayer.playWhenReady = true
                            }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                            .testTag("resume_ask_resume")
                    ) {
                        Text(
                            text = "Resume",
                            color = Color.Black,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // MX Player-Style Translucent Right-Side Overlay Drawers (`RightSidePlayerDrawer`)
        RightSidePlayerDrawer(
            visible = !isInPipMode && activeDrawer != PlayerDrawerType.NONE,
            isLandscape = isLandscape,
            onDismiss = {
                activeDrawer = PlayerDrawerType.NONE
                lastInteractionTick++
            }
        ) {
            val drawerToRender = if (activeDrawer != PlayerDrawerType.NONE) activeDrawer else lastNonNoneDrawer
            when (drawerToRender) {
                PlayerDrawerType.AUDIO -> {
                    AudioTrackDrawerContent(
                        tracks = audioTracks,
                        isAudioDisabled = isAudioDisabled,
                        useSwAudioDecoder = useSwAudioDecoder,
                        audioSyncOffsetMs = audioSyncOffsetMs,
                        stereoMode = stereoMode,
                        onSelectTrack = { track ->
                            isAudioDisabled = false
                            exoPlayer.volume = if (isMuted) 0f else 1f
                            selectedAudioGroupIdx = track.groupIndex
                            selectedAudioTrackIdx = track.trackIndex
                            val group = exoPlayer.currentTracks.groups.getOrNull(track.groupIndex)
                            if (group != null) {
                                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                    .buildUpon()
                                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                                    .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, track.trackIndex))
                                    .build()
                            }
                            audioTracks = audioTracks.map { it.copy(isSelected = it.id == track.id) }
                            persistCurrentVideoMemory()
                            showCenterToast("Audio: ${track.label}")
                            activeDrawer = PlayerDrawerType.NONE
                            lastInteractionTick++
                        },
                        onSelectDefaultAudio = {
                            isAudioDisabled = false
                            selectedAudioGroupIdx = -1
                            selectedAudioTrackIdx = -1
                            exoPlayer.volume = if (isMuted) 0f else 1f
                            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                .buildUpon()
                                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                                .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                                .build()
                            persistCurrentVideoMemory()
                            showCenterToast("Audio: Default Stream")
                            activeDrawer = PlayerDrawerType.NONE
                            lastInteractionTick++
                        },
                        onDisableAudio = {
                            isAudioDisabled = true
                            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                .buildUpon()
                                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                                .build()
                            showCenterToast("Audio Disabled")
                            activeDrawer = PlayerDrawerType.NONE
                            lastInteractionTick++
                        },
                        onToggleSwAudioDecoder = { enabled ->
                            useSwAudioDecoder = enabled
                            preservedReloadPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                            activeDecoderMode = if (enabled) DecoderPriority.HW_PLUS else DecoderPriority.HW
                            showCenterToast(
                                if (enabled) "FFmpeg SW Audio Decoder: ON" else "HW Audio Decoder: ON"
                            )
                            lastInteractionTick++
                        },
                        onOpenExternalAudio = {
                            openAudioLauncher.launch(arrayOf("audio/*", "*/*"))
                            lastInteractionTick++
                        },
                        onCycleStereoMode = {
                            stereoMode = stereoMode.next()
                            showCenterToast("Stereo Mode: ${stereoMode.label}")
                            lastInteractionTick++
                        },
                        onAdjustAudioSyncMs = { newOffset ->
                            audioSyncOffsetMs = newOffset.coerceIn(-5000, 5000)
                            lastInteractionTick++
                        },
                        onClose = {
                            activeDrawer = PlayerDrawerType.NONE
                            lastInteractionTick++
                        }
                    )
                }

                PlayerDrawerType.SUBTITLE -> {
                    SubtitleDrawerContent(
                        tracks = subtitleTracks,
                        areSubtitlesEnabled = areSubtitlesEnabled,
                        subtitleSyncOffsetMs = subtitleSyncOffsetMs,
                        subtitleSpeed = subtitleSpeed,
                        styleConfig = subtitleStyleConfig,
                        onSelectTrack = { track ->
                            areSubtitlesEnabled = true
                            selectedSubtitleGroupIdx = track.groupIndex
                            selectedSubtitleTrackIdx = track.trackIndex
                            val group = exoPlayer.currentTracks.groups.getOrNull(track.groupIndex)
                            if (group != null) {
                                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                    .buildUpon()
                                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                    .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, track.trackIndex))
                                    .build()
                            }
                            subtitleTracks = subtitleTracks.map { it.copy(isSelected = it.id == track.id) }
                            persistCurrentVideoMemory()
                            showCenterToast("Subtitle: ${track.label}")
                            activeDrawer = PlayerDrawerType.NONE
                            lastInteractionTick++
                        },
                        onDisableSubtitles = {
                            areSubtitlesEnabled = false
                            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                .buildUpon()
                                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                                .build()
                            persistCurrentVideoMemory()
                            showCenterToast("Subtitles Disabled")
                            activeDrawer = PlayerDrawerType.NONE
                            lastInteractionTick++
                        },
                        onOpenExternalSubtitle = {
                            openSubtitleLauncher.launch(arrayOf("*/*"))
                            lastInteractionTick++
                        },
                        onSearchOnlineSubtitles = {
                            activeDrawer = PlayerDrawerType.NONE
                            showOnlineSubtitleDialog = true
                            lastInteractionTick++
                        },
                        onAdjustSubtitleSyncMs = { newOffset ->
                            subtitleSyncOffsetMs = newOffset.coerceIn(-30_000L, 30_000L)
                            persistCurrentVideoMemory()
                            lastInteractionTick++
                        },
                        onAdjustSubtitleSpeed = { newSpeed ->
                            subtitleSpeed = newSpeed
                            lastInteractionTick++
                        },
                        onUpdateStyleConfig = { updatedStyle ->
                            subtitleStyleConfig = updatedStyle
                            coroutineScope.launch {
                                subtitlePreferences.saveStyleConfig(updatedStyle)
                            }
                            lastInteractionTick++
                        },
                        onResetStyleConfig = {
                            val defaultConfig = SubtitleStyleConfig.Default
                            subtitleStyleConfig = defaultConfig
                            coroutineScope.launch {
                                subtitlePreferences.resetToDefault()
                            }
                            lastInteractionTick++
                        },
                        onClose = {
                            activeDrawer = PlayerDrawerType.NONE
                            lastInteractionTick++
                        }
                    )
                }

                PlayerDrawerType.EQUALIZER -> {
                    EqualizerDrawerContent(
                        selectedPreset = eqPreset,
                        bandLevelsDb = eqBandsDb,
                        bassBoostStrength = bassBoostStrength,
                        onSelectPreset = { preset ->
                            eqPreset = preset
                            preset.bandsDb.forEachIndexed { idx, v -> eqBandsDb[idx] = v }
                            applyEqBandsToHardware(preset.bandsDb)
                        },
                        onBandLevelChange = { idx, db ->
                            eqPreset = EqPreset.CUSTOM
                            eqBandsDb[idx] = db
                            applyEqBandsToHardware(eqBandsDb.toList())
                        },
                        onBassBoostChange = { strength ->
                            bassBoostStrength = strength
                            runCatching {
                                audioFxBundle.second?.setStrength((strength * 1000f).toInt().toShort())
                            }
                        },
                        onClose = {
                            activeDrawer = PlayerDrawerType.NONE
                            lastInteractionTick++
                        }
                    )
                }

                PlayerDrawerType.MORE_OPTIONS -> {
                    val drawerTimerLabel: String? = when (sleepTimerSelection) {
                        is SleepTimerSelection.Off -> null
                        is SleepTimerSelection.Minutes -> {
                            val secs = sleepTimerRemainingSeconds ?: 0
                            String.format(Locale.US, "%02d:%02d", secs / 60, secs % 60)
                        }
                        is SleepTimerSelection.EndOfVideo -> "End of video"
                    }
                    MoreOptionsDrawerContent(
                        longPress2xEnabled = longPress2xEnabled,
                        sleepTimerStatusLabel = drawerTimerLabel,
                        onCycleAspectRatio = {
                            val modes = PlayerScaleMode.entries
                            val nextMode = modes[(modes.indexOf(scaleMode) + 1) % modes.size]
                            scaleMode = nextMode
                            zoomScale = 1.0f
                            persistCurrentVideoMemory()
                            showCenterToast(nextMode.label)
                            activeDrawer = PlayerDrawerType.NONE
                            lastInteractionTick++
                        },
                        onToggleLongPress2x = { longPress2xEnabled = it },
                        onOpenSleepTimer = {
                            activeDrawer = PlayerDrawerType.NONE
                            showSleepTimerDialog = true
                            lastInteractionTick++
                        },
                        onShowInformation = {
                            activeDrawer = PlayerDrawerType.NONE
                            showInfoDialog = true
                            lastInteractionTick++
                        },
                        onShareVideo = {
                            activeDrawer = PlayerDrawerType.NONE
                            onShareVideo(currentVideo)
                        },
                        onRenameVideo = {
                            activeDrawer = PlayerDrawerType.NONE
                            showRenameDialog = true
                            lastInteractionTick++
                        },
                        onDeleteVideo = {
                            activeDrawer = PlayerDrawerType.NONE
                            onDeleteVideoFromPlayer(currentVideo)
                            onClosePlayer()
                        },
                        onOpenSettings = {
                            activeDrawer = PlayerDrawerType.NONE
                            persistCurrentVideoMemory()
                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            onClosePlayer()
                            onOpenSettingsFromPlayer()
                        },
                        onClose = {
                            activeDrawer = PlayerDrawerType.NONE
                            lastInteractionTick++
                        }
                    )
                }

                PlayerDrawerType.NONE -> Unit
            }
        }
    }

    if (showOnlineSubtitleDialog) {
        OnlineSubtitleSearchDialog(
            videoTitle = currentVideo.displayName,
            onDismiss = { showOnlineSubtitleDialog = false },
            onSelectAndDownload = { chosenResult ->
                coroutineScope.launch {
                    val (downloadedUri, downloadedFile) = OpenSubtitlesHelper.downloadSubtitleToCache(
                        context = context,
                        result = chosenResult,
                        videoTitle = currentVideo.displayName
                    )
                    showOnlineSubtitleDialog = false
                    attachSubtitleUriToPlayer(
                        subUri = downloadedUri,
                        subMime = MimeTypes.APPLICATION_SUBRIP,
                        subLabel = "${chosenResult.languageLabel} (${downloadedFile.name})",
                        localPath = downloadedFile.absolutePath
                    )
                }
            }
        )
    }

    if (showDecoderDialog) {
        DecoderSelectorDialog(
            activeDecoder = activeDecoderMode,
            onSelectDecoder = { chosen ->
                if (chosen != activeDecoderMode) {
                    preservedReloadPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                    activeDecoderMode = chosen
                    showCenterToast("Switched to ${chosen.label}")
                }
            },
            onDismiss = { showDecoderDialog = false }
        )
    }

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            activeSelection = sleepTimerSelection,
            onSelectOption = { choice ->
                sleepTimerSelection = choice
                showCenterToast(
                    when (choice) {
                        is SleepTimerSelection.Off -> "Sleep Timer turned off"
                        is SleepTimerSelection.Minutes -> "Sleep Timer: ${choice.minutes} min"
                        is SleepTimerSelection.EndOfVideo -> "Sleep Timer: End of current video"
                    }
                )
            },
            onDismiss = { showSleepTimerDialog = false }
        )
    }

    if (showInfoDialog) {
        val activeAudioSummary = audioTracks.firstOrNull { it.isSelected }?.let { "${it.label} (${it.detail})" }
            ?: currentVideo.audioTrackInfo
        PlayerVideoInfoDialog(
            video = currentVideo,
            decoderMode = activeDecoderMode,
            detectedResolution = detectedResolutionText,
            audioSummary = activeAudioSummary,
            onDismiss = { showInfoDialog = false }
        )
    }

    if (showRenameDialog) {
        RenameVideoDialog(
            video = currentVideo,
            onDismiss = { showRenameDialog = false },
            onConfirmRename = { newBase ->
                showRenameDialog = false
                onRenameVideoFromPlayer(currentVideo, newBase)
                showCenterToast("Renamed to $newBase")
            }
        )
    }

    if (showLeaveConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirmDialog = false },
            containerColor = Color(0xFF161821),
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(alpha = 0.85f),
            title = {
                Text(
                    text = "Leave playback?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Exit video player and save your current playback position?",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLeaveConfirmDialog = false
                        persistCurrentVideoMemory()
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        onClosePlayer()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB),
                        contentColor = Color.White
                    )
                ) {
                    Text("Leave")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirmDialog = false }) {
                    Text("Stay", color = PlayerSeekPeriwinkle)
                }
            }
        )
    }
}

@Composable
private fun OnlineSubtitleSearchDialog(
    videoTitle: String,
    onDismiss: () -> Unit,
    onSelectAndDownload: (OnlineSubtitleResult) -> Unit
) {
    var queryText by remember(videoTitle) {
        mutableStateOf(OpenSubtitlesHelper.cleanQueryFromFilename(videoTitle))
    }
    var selectedLangCode by remember { mutableStateOf("en") }
    var isSearching by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<OnlineSubtitleResult>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(selectedLangCode) {
        isSearching = true
        results = OpenSubtitlesHelper.searchSubtitles(queryText, selectedLangCode)
        isSearching = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161821),
        titleContentColor = Color.White,
        textContentColor = Color.White.copy(alpha = 0.88f),
        title = {
            Text(
                text = "Online Subtitle Search (.srt)",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = queryText,
                        onValueChange = { queryText = it },
                        singleLine = true,
                        label = { Text("Movie / Episode Title", color = Color.White.copy(alpha = 0.7f)) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("online_subtitle_query_input")
                    )
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                isSearching = true
                                results = OpenSubtitlesHelper.searchSubtitles(queryText, selectedLangCode)
                                isSearching = false
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PlayerSeekPeriwinkle.copy(alpha = 0.25f))
                            .testTag("online_subtitle_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search Subtitles",
                            tint = PlayerSeekPeriwinkle
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OpenSubtitlesHelper.supportedLanguages.forEach { (code, label) ->
                        val isSelected = selectedLangCode == code
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) PlayerSeekPeriwinkle
                                    else Color.White.copy(alpha = 0.1f)
                                )
                                .clickable { selectedLangCode = code }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .testTag("subtitle_lang_$code")
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (isSearching) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = PlayerSeekPeriwinkle,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                } else {
                    results.forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                .clickable { onSelectAndDownload(item) }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("online_subtitle_result_${item.id}")
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${item.languageLabel} • ${item.releaseGroup} • ★ ${item.rating}",
                                    color = PlayerSeekPeriwinkle,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Filled.CloudDownload,
                                contentDescription = "Download .srt",
                                tint = PlayerSeekPeriwinkle,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = PlayerSeekPeriwinkle)
            }
        }
    )
}

/**
 * Opposite-Side Vertical Pill Slider (`44.dp` wide, `180.dp` tall, `#CC141418`):
 * - Brightness: Left-side vertical swipe -> pinned to RIGHT edge with Sun icon + %
 * - Volume: Right-side vertical swipe -> pinned to LEFT edge with Speaker icon + % (100%-200% in amber/blue boost tint)
 */
@Composable
private fun VerticalEdgeSliderPill(
    icon: ImageVector,
    fraction: Float,
    labelText: String,
    isBoostActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val clamped = fraction.coerceIn(0f, 1f)
    val fillBrush = if (isBoostActive) {
        Brush.verticalGradient(listOf(Color(0xFFF59E0B), Color(0xFF38BDF8)))
    } else {
        Brush.verticalGradient(listOf(PlayerSeekPeriwinkle, PlayerSeekPeriwinkle))
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .width(48.dp)
            .height(186.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(PlayerVerticalSliderBg)
            .border(
                width = 1.dp,
                color = if (isBoostActive) Color(0xFFF59E0B).copy(alpha = 0.7f) else PlayerHudGlassBorder,
                shape = RoundedCornerShape(24.dp)
            )
            .padding(vertical = 12.dp, horizontal = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isBoostActive) Color(0xFFF59E0B) else Color.White,
            modifier = Modifier.size(18.dp)
        )

        Box(
            modifier = Modifier
                .width(6.dp)
                .weight(1f)
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.25f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(clamped)
                    .clip(RoundedCornerShape(999.dp))
                    .background(fillBrush)
            )
        }

        Text(
            text = labelText,
            color = if (isBoostActive) Color(0xFFFBBF24) else Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CenterSeekOrStatusPill(banner: CenterBannerState) {
    val pillShape = RoundedCornerShape(16.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .shadow(
                elevation = 16.dp,
                shape = pillShape,
                ambientColor = Color.Black.copy(alpha = 0.50f),
                spotColor = Color.Black.copy(alpha = 0.65f)
            )
            .clip(pillShape)
            .background(Color(0xFF0F172A).copy(alpha = 0.72f))
            .border(1.dp, Color.White.copy(alpha = 0.25f), pillShape)
            .padding(horizontal = 20.dp, vertical = 11.dp)
            .testTag("center_feedback_toast_pill")
    ) {
        when (banner) {
            is CenterBannerState.Scrub -> {
                val sign = if (banner.deltaMs >= 0) "+" else "-"
                val targetFormatted = formatDurationMillis(banner.targetTimeMs)
                val deltaFormatted = formatDurationMillis(abs(banner.deltaMs))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (!banner.chapterTitle.isNullOrBlank()) {
                        Text(
                            text = banner.chapterTitle,
                            color = PlayerSeekPeriwinkle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = targetFormatted,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "[$sign$deltaFormatted]",
                            color = PlayerSeekPeriwinkle,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            is CenterBannerState.Zoom -> {
                Text(
                    text = "${banner.percent}%",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            is CenterBannerState.Toast -> {
                Text(
                    text = banner.message,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun FrostedCircleHudButton(
    icon: ImageVector,
    contentDescription: String,
    size: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 20.dp,
    iconModifier: Modifier = Modifier,
    isActiveHighlight: Boolean = false
) {
    val glassConfig = LocalLiquidGlassConfig.current
    val useGlassStyle = glassConfig.enabled && glassConfig.hdrModeEnabled
    val bgAlpha = if (useGlassStyle) {
        (0.45f * glassConfig.backdropOpacity.coerceIn(0.35f, 1f)).coerceIn(0.20f, 0.72f)
    } else {
        0.55f
    }
    val bg = if (isActiveHighlight) {
        PlayerSeekPeriwinkle.copy(alpha = 0.55f)
    } else if (useGlassStyle) {
        PlayerHudGlassBg.copy(alpha = bgAlpha)
    } else {
        Color.Black.copy(alpha = 0.58f)
    }
    val borderWidth = if (useGlassStyle) glassConfig.effectiveHighlightStrokeDp() else 1.dp
    val borderColor = if (useGlassStyle) {
        PlayerHudGlassBorder.copy(alpha = (0.28f * glassConfig.highlightStrength).coerceIn(0.08f, 0.75f))
    } else {
        Color.White.copy(alpha = 0.16f)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bg)
            .border(borderWidth, borderColor, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier
                .size(iconSize)
                .then(iconModifier)
        )
    }
}

internal fun buildVideoChapters(
    mediaMetadata: MediaMetadata,
    durationMs: Long,
    metadataChapters: List<VideoChapter> = emptyList()
): List<VideoChapter> {
    val safeDur = durationMs.coerceAtLeast(1000L)
    val parsed = mutableListOf<VideoChapter>()

    val extras = mediaMetadata.extras
    if (extras != null) {
        val titles = extras.getStringArray("chapter_titles")
        val starts = extras.getLongArray("chapter_starts_ms")
        if (titles != null && starts != null && titles.size == starts.size) {
            for (i in titles.indices) {
                val title = titles[i].trim()
                val start = starts[i]
                if (title.isNotEmpty() && start in 0L until safeDur) {
                    parsed.add(VideoChapter(title, start))
                }
            }
        }
    }

    metadataChapters.forEach { chapter ->
        if (chapter.title.isNotBlank() && chapter.startTimeMs in 0L until safeDur) {
            parsed.add(chapter)
        }
    }

    return parsed
        .distinctBy { it.startTimeMs }
        .sortedBy { it.startTimeMs }
}

internal fun resolveChapterMarkers(
    chapters: List<VideoChapter>,
    totalDurationMs: Long
): List<ChapterMarker> {
    if (chapters.isEmpty()) return emptyList()
    val safeTotal = totalDurationMs.coerceAtLeast(0L)
    val sorted = chapters
        .filter { it.title.isNotBlank() && it.startTimeMs >= 0L }
        .distinctBy { it.startTimeMs }
        .sortedBy { it.startTimeMs }
    if (sorted.isEmpty()) return emptyList()

    return sorted.mapIndexed { index, chapter ->
        val rawEnd = if (index < sorted.lastIndex) {
            sorted[index + 1].startTimeMs
        } else {
            safeTotal
        }
        ChapterMarker(
            title = chapter.title.trim(),
            startMs = chapter.startTimeMs,
            endMs = rawEnd.coerceAtLeast(chapter.startTimeMs)
        )
    }
}

private val INTRO_EXACT_TITLES = setOf(
    "intro",
    "opening",
    "op",
    "opening credits",
    "main title",
    "title sequence",
    "theme song"
)
private val INTRO_WORD_REGEX = Regex("\\b(op|opening|intro)(\\s*\\d+)?\\b")
private val INTRO_EXPLICIT_TAG_REGEX = Regex("\\bintro(\\s*\\d+)?\\b")

internal fun isIntroChapterTitle(title: String): Boolean {
    val normalized = title.trim()
        .lowercase(Locale.ROOT)
        .replace(Regex("[_\\-]+"), " ")
        .replace(Regex("\\s+"), " ")
    if (normalized.isEmpty()) return false

    val hasColdOpenOrRecap = normalized.contains("cold open") || normalized.contains("recap")
    if (hasColdOpenOrRecap && !INTRO_EXPLICIT_TAG_REGEX.containsMatchIn(normalized)) {
        return false
    }

    if (normalized in INTRO_EXACT_TITLES) return true
    if (normalized.contains("opening credits") ||
        normalized.contains("main title") ||
        normalized.contains("title sequence") ||
        normalized.contains("theme song")
    ) {
        return true
    }
    return INTRO_WORD_REGEX.containsMatchIn(normalized)
}

private val OUTRO_EXACT_TITLES = setOf(
    "outro",
    "ending",
    "ed",
    "end credits",
    "closing",
    "closing credits",
    "credits"
)
private val OUTRO_WORD_REGEX = Regex("\\b(ed|ending|outro|credits)(\\s*\\d+)?\\b")

internal fun isOutroChapterTitle(title: String): Boolean {
    val normalized = title.trim()
        .lowercase(Locale.ROOT)
        .replace(Regex("[_\\-]+"), " ")
        .replace(Regex("\\s+"), " ")
    if (normalized.isEmpty()) return false
    if (isIntroChapterTitle(title)) return false
    if (normalized.contains("post credit") || normalized.contains("after credit")) return false

    if (normalized in OUTRO_EXACT_TITLES) return true
    if (normalized.contains("end credits") || normalized.contains("closing credits")) return true
    return OUTRO_WORD_REGEX.containsMatchIn(normalized)
}

private fun resolveInitialDisplayBrightness(context: Context): Float {
    val sysVal = runCatching {
        Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 140)
    }.getOrDefault(140)
    return (sysVal / 255f).coerceIn(0.05f, 1.0f)
}

internal suspend fun extractContainerChapters(
    context: Context,
    uri: Uri,
    directFilePath: String? = null
): List<VideoChapter> = withContext(Dispatchers.IO) {
    runCatching {
        val directFile = directFilePath?.let { File(it) }?.takeIf { it.isFile && it.canRead() }
            ?: uri.takeIf { it.scheme == "file" }?.path?.let { File(it) }?.takeIf { it.isFile && it.canRead() }
        if (directFile != null) {
            FileInputStream(directFile).use { fis ->
                return@runCatching parseChaptersFromFileChannel(fis.channel)
            }
        }
        val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return@runCatching emptyList()
        pfd.use { descriptor ->
            ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { fis ->
                return@runCatching parseChaptersFromFileChannel(fis.channel)
            }
        }
    }.getOrDefault(emptyList())
}

internal fun parseChaptersFromFileChannel(channel: FileChannel): List<VideoChapter> {
    val fileSize = runCatching { channel.size() }.getOrDefault(0L)
    if (fileSize < 16L) return emptyList()
    channel.position(0L)
    val magic = ByteArray(8)
    val read = readFullyFromChannel(channel, magic, 0, 8)
    if (read < 8) return emptyList()
    channel.position(0L)

    return if (magic[0] == 0x1A.toByte() &&
        magic[1] == 0x45.toByte() &&
        magic[2] == 0xDF.toByte() &&
        magic[3] == 0xA3.toByte()
    ) {
        parseMatroskaChaptersFromChannel(channel, fileSize)
    } else {
        parseMp4ChaptersFromChannel(channel, fileSize)
    }
}

private fun parseMatroskaChaptersFromChannel(channel: FileChannel, fileSize: Long): List<VideoChapter> {
    // Read EBML header element
    val headerId = readEbmlId(channel, fileSize) ?: return emptyList()
    if (headerId != 0x1A45DFA3L) return emptyList()
    val headerSize = readEbmlSize(channel, fileSize) ?: return emptyList()
    if (headerSize < 0L || channel.position() + headerSize >= fileSize) return emptyList()
    channel.position(channel.position() + headerSize)

    // Expect Segment (0x18538067)
    val segId = readEbmlId(channel, fileSize) ?: return emptyList()
    if (segId != 0x18538067L) return emptyList()
    val segSize = readEbmlSize(channel, fileSize)
    val segDataStart = channel.position()
    val segEnd = if (segSize != null && segSize > 0L) minOf(fileSize, segDataStart + segSize) else fileSize

    var chaptersOffset: Long? = null
    var iterations = 0
    while (channel.position() < segEnd && iterations++ < 64) {
        val elemStart = channel.position()
        val elemId = readEbmlId(channel, segEnd) ?: break
        val elemSize = readEbmlSize(channel, segEnd) ?: break
        val dataStart = channel.position()
        if (elemSize < 0L) break

        when (elemId) {
            0x1043A770L -> { // Chapters
                val endPos = minOf(segEnd, dataStart + elemSize)
                return parseEbmlChaptersPayload(channel, dataStart, endPos)
            }
            0x114D9B74L -> { // SeekHead
                val endPos = minOf(segEnd, dataStart + elemSize)
                val foundSeek = findChaptersSeekOffsetInSeekHead(channel, dataStart, endPos, segDataStart)
                if (foundSeek != null) {
                    chaptersOffset = foundSeek
                }
                channel.position(endPos)
            }
            0x1F43B675L -> { // Cluster: jump to SeekHead target if known, otherwise stop scanning
                val jumpPos = chaptersOffset
                if (jumpPos != null && jumpPos > elemStart && jumpPos < segEnd) {
                    channel.position(jumpPos)
                    chaptersOffset = null
                } else {
                    break
                }
            }
            else -> {
                val nextPos = dataStart + elemSize
                if (nextPos <= elemStart || nextPos > segEnd) break
                channel.position(nextPos)
            }
        }
    }

    val targetOffset = chaptersOffset
    if (targetOffset != null && targetOffset in segDataStart until segEnd) {
        channel.position(targetOffset)
        val elemId = readEbmlId(channel, segEnd)
        val elemSize = readEbmlSize(channel, segEnd)
        if (elemId == 0x1043A770L && elemSize != null && elemSize > 0L) {
            val dataStart = channel.position()
            return parseEbmlChaptersPayload(channel, dataStart, minOf(segEnd, dataStart + elemSize))
        }
    }
    return emptyList()
}

private fun findChaptersSeekOffsetInSeekHead(
    channel: FileChannel,
    startPos: Long,
    endPos: Long,
    segmentDataStart: Long
): Long? {
    channel.position(startPos)
    while (channel.position() < endPos) {
        val id = readEbmlId(channel, endPos) ?: break
        val size = readEbmlSize(channel, endPos) ?: break
        val childStart = channel.position()
        val childEnd = minOf(endPos, childStart + size.coerceAtLeast(0L))
        if (id == 0x4DBBL) { // Seek
            var seekIdValue = -1L
            var seekPosValue = -1L
            while (channel.position() < childEnd) {
                val sid = readEbmlId(channel, childEnd) ?: break
                val ssize = readEbmlSize(channel, childEnd) ?: break
                val sStart = channel.position()
                val sEnd = minOf(childEnd, sStart + ssize.coerceAtLeast(0L))
                when (sid) {
                    0x53ABL -> seekIdValue = readEbmlUInt(channel, ssize.toInt().coerceIn(0, 8))
                    0x53ACL -> seekPosValue = readEbmlUInt(channel, ssize.toInt().coerceIn(0, 8))
                }
                channel.position(sEnd)
            }
            if (seekIdValue == 0x1043A770L && seekPosValue >= 0L) {
                return segmentDataStart + seekPosValue
            }
        }
        channel.position(childEnd)
    }
    return null
}

private fun parseEbmlChaptersPayload(
    channel: FileChannel,
    startPos: Long,
    endPos: Long
): List<VideoChapter> {
    val result = mutableListOf<VideoChapter>()
    channel.position(startPos)
    while (channel.position() < endPos) {
        val id = readEbmlId(channel, endPos) ?: break
        val size = readEbmlSize(channel, endPos) ?: break
        val childStart = channel.position()
        val childEnd = minOf(endPos, childStart + size.coerceAtLeast(0L))
        if (id == 0x45B9L) { // EditionEntry
            parseEbmlEditionEntry(channel, childStart, childEnd, result)
            if (result.isNotEmpty()) break
        }
        channel.position(childEnd)
    }
    return result.distinctBy { it.startTimeMs }.sortedBy { it.startTimeMs }
}

private fun parseEbmlEditionEntry(
    channel: FileChannel,
    startPos: Long,
    endPos: Long,
    out: MutableList<VideoChapter>
) {
    channel.position(startPos)
    while (channel.position() < endPos) {
        val id = readEbmlId(channel, endPos) ?: break
        val size = readEbmlSize(channel, endPos) ?: break
        val childStart = channel.position()
        val childEnd = minOf(endPos, childStart + size.coerceAtLeast(0L))
        if (id == 0xB6L) { // ChapterAtom
            parseEbmlChapterAtom(channel, childStart, childEnd, out)
        }
        channel.position(childEnd)
    }
}

private fun parseEbmlChapterAtom(
    channel: FileChannel,
    startPos: Long,
    endPos: Long,
    out: MutableList<VideoChapter>
) {
    channel.position(startPos)
    var timeStartNs = -1L
    var chapterTitle = ""
    while (channel.position() < endPos) {
        val id = readEbmlId(channel, endPos) ?: break
        val size = readEbmlSize(channel, endPos) ?: break
        val childStart = channel.position()
        val childEnd = minOf(endPos, childStart + size.coerceAtLeast(0L))
        when (id) {
            0x91L -> { // ChapterTimeStart (nanoseconds)
                timeStartNs = readEbmlUInt(channel, size.toInt().coerceIn(0, 8))
            }
            0x80L -> { // ChapterDisplay
                if (chapterTitle.isEmpty()) {
                    chapterTitle = parseEbmlChapterDisplayTitle(channel, childStart, childEnd)
                }
            }
            0xB6L -> { // Nested ChapterAtom
                parseEbmlChapterAtom(channel, childStart, childEnd, out)
            }
        }
        channel.position(childEnd)
    }
    if (timeStartNs >= 0L) {
        val title = chapterTitle.ifBlank { "Chapter ${out.size + 1}" }
        out.add(VideoChapter(title = title, startTimeMs = timeStartNs / 1_000_000L))
    }
}

private fun parseEbmlChapterDisplayTitle(channel: FileChannel, startPos: Long, endPos: Long): String {
    channel.position(startPos)
    while (channel.position() < endPos) {
        val id = readEbmlId(channel, endPos) ?: break
        val size = readEbmlSize(channel, endPos) ?: break
        val childStart = channel.position()
        val childEnd = minOf(endPos, childStart + size.coerceAtLeast(0L))
        if (id == 0x85L && size in 1L..4096L) { // ChapString (UTF-8)
            val bytes = ByteArray(size.toInt())
            val read = readFullyFromChannel(channel, bytes, 0, bytes.size)
            if (read > 0) {
                return String(bytes, 0, read, Charsets.UTF_8).trim()
            }
        }
        channel.position(childEnd)
    }
    return ""
}

private fun readEbmlId(channel: FileChannel, limit: Long): Long? {
    if (channel.position() >= limit) return null
    val first = readSingleByte(channel) ?: return null
    var mask = 0x80
    var length = 1
    while (length <= 4 && (first and mask) == 0) {
        mask = mask shr 1
        length++
    }
    if (length > 4 || channel.position() + (length - 1) > limit) return null
    var value = first.toLong()
    for (i in 1 until length) {
        val b = readSingleByte(channel) ?: return null
        value = (value shl 8) or b.toLong()
    }
    return value
}

private fun readEbmlSize(channel: FileChannel, limit: Long): Long? {
    if (channel.position() >= limit) return null
    val first = readSingleByte(channel) ?: return null
    var mask = 0x80
    var length = 1
    while (length <= 8 && (first and mask) == 0) {
        mask = mask shr 1
        length++
    }
    if (length > 8 || channel.position() + (length - 1) > limit) return null
    var value = (first and (mask - 1)).toLong()
    for (i in 1 until length) {
        val b = readSingleByte(channel) ?: return null
        value = (value shl 8) or b.toLong()
    }
    return value
}

private fun readEbmlUInt(channel: FileChannel, byteLen: Int): Long {
    var result = 0L
    for (i in 0 until byteLen) {
        val b = readSingleByte(channel) ?: return -1L
        result = (result shl 8) or b.toLong()
    }
    return result
}

private fun parseMp4ChaptersFromChannel(channel: FileChannel, fileSize: Long): List<VideoChapter> {
    val moovRange = findMp4BoxRange(channel, 0L, fileSize, 0x6D6F6F76) ?: return emptyList() // moov
    val udtaRange = findMp4BoxRange(channel, moovRange.first, moovRange.second, 0x75647461) ?: return emptyList() // udta
    val chplRange = findMp4BoxRange(channel, udtaRange.first, udtaRange.second, 0x6368706C) ?: return emptyList() // chpl

    val payloadLen = (chplRange.second - chplRange.first).toInt().coerceAtLeast(0)
    if (payloadLen < 6 || payloadLen > 1024 * 1024) return emptyList()
    channel.position(chplRange.first)
    val bytes = ByteArray(payloadLen)
    val read = readFullyFromChannel(channel, bytes, 0, payloadLen)
    if (read < 6) return emptyList()

    val buf = ByteBuffer.wrap(bytes, 0, read).order(ByteOrder.BIG_ENDIAN)
    val version = buf.get().toInt() and 0xFF
    buf.position(4) // skip flags (3 bytes)
    if (version == 1) {
        if (buf.remaining() < 5) return emptyList()
        buf.int // skip 4-byte reserved
    }
    if (buf.remaining() < 1) return emptyList()
    val count = buf.get().toInt() and 0xFF
    val out = mutableListOf<VideoChapter>()
    for (i in 0 until count) {
        if (buf.remaining() < 9) break
        val timestamp100Ns = buf.long
        val titleLen = buf.get().toInt() and 0xFF
        if (buf.remaining() < titleLen) break
        val titleBytes = ByteArray(titleLen)
        buf.get(titleBytes)
        val startMs = (timestamp100Ns / 10_000L).coerceAtLeast(0L)
        val title = String(titleBytes, Charsets.UTF_8).trim().ifEmpty { "Chapter ${i + 1}" }
        out.add(VideoChapter(title = title, startTimeMs = startMs))
    }
    return out.distinctBy { it.startTimeMs }.sortedBy { it.startTimeMs }
}

private fun findMp4BoxRange(
    channel: FileChannel,
    startPos: Long,
    endPos: Long,
    targetFourCc: Int
): Pair<Long, Long>? {
    channel.position(startPos)
    val headerBuf = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN)
    var iterations = 0
    while (channel.position() + 8L <= endPos && iterations++ < 256) {
        val boxStart = channel.position()
        headerBuf.clear()
        headerBuf.limit(8)
        if (readBufferFully(channel, headerBuf) < 8) break
        headerBuf.flip()
        val size32 = headerBuf.int.toLong() and 0xFFFFFFFFL
        val type = headerBuf.int
        var headerLen = 8L
        val boxSize = when (size32) {
            0L -> endPos - boxStart
            1L -> {
                if (channel.position() + 8L > endPos) break
                headerBuf.clear()
                headerBuf.limit(8)
                if (readBufferFully(channel, headerBuf) < 8) break
                headerBuf.flip()
                headerLen = 16L
                headerBuf.long
            }
            else -> size32
        }
        if (boxSize < headerLen) break
        val dataStart = boxStart + headerLen
        val boxEnd = minOf(endPos, boxStart + boxSize)
        if (type == targetFourCc) {
            return dataStart to boxEnd
        }
        if (boxEnd <= boxStart) break
        channel.position(boxEnd)
    }
    return null
}

private fun readSingleByte(channel: FileChannel): Int? {
    val buf = ByteBuffer.allocate(1)
    val n = channel.read(buf)
    if (n != 1) return null
    return buf.get(0).toInt() and 0xFF
}

private fun readFullyFromChannel(channel: FileChannel, dest: ByteArray, offset: Int, len: Int): Int {
    val buf = ByteBuffer.wrap(dest, offset, len)
    return readBufferFully(channel, buf)
}

private fun readBufferFully(channel: FileChannel, buf: ByteBuffer): Int {
    var total = 0
    while (buf.hasRemaining()) {
        val n = channel.read(buf)
        if (n <= 0) break
        total += n
    }
    return total
}

internal fun findChapterForPosition(chapters: List<VideoChapter>, positionMs: Long): VideoChapter? {
    if (chapters.isEmpty()) return null
    return chapters.lastOrNull { it.startTimeMs <= positionMs } ?: chapters.firstOrNull()
}

private suspend fun captureFrameToPictures(
    context: Context,
    video: VideoItem,
    positionMs: Long
): Boolean = withContext(Dispatchers.IO) {
    val retriever = MediaMetadataRetriever()
    return@withContext try {
        retriever.setDataSource(context, video.uri)
        val bitmap = retriever.getFrameAtTime(
            positionMs * 1000L,
            MediaMetadataRetriever.OPTION_CLOSEST_SYNC
        ) ?: return@withContext false

        val filename = "Aura_${System.currentTimeMillis()}.png"
        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/AuraPlayer")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val outUri = resolver.insert(collection, values) ?: return@withContext false
        resolver.openOutputStream(outUri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val publish = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
            resolver.update(outUri, publish, null, null)
        }
        true
    } catch (_: Exception) {
        false
    } finally {
        runCatching { retriever.release() }
    }
}

@OptIn(UnstableApi::class)
private fun parseExoTracks(tracks: Tracks): Pair<List<AudioTrackUiModel>, List<SubtitleTrackUiModel>> {
    val parsedAudio = mutableListOf<AudioTrackUiModel>()
    val parsedSubs = mutableListOf<SubtitleTrackUiModel>()

    tracks.groups.forEachIndexed { groupIdx, group ->
        if (group.type == C.TRACK_TYPE_AUDIO) {
            for (trackIdx in 0 until group.length) {
                val fmt = group.getTrackFormat(trackIdx)
                val codecName = when (fmt.sampleMimeType) {
                    MimeTypes.AUDIO_E_AC3, MimeTypes.AUDIO_E_AC3_JOC -> "Dolby Digital Plus"
                    MimeTypes.AUDIO_AC3 -> "Dolby Digital (AC3)"
                    MimeTypes.AUDIO_DTS, MimeTypes.AUDIO_DTS_HD -> "DTS Audio"
                    MimeTypes.AUDIO_TRUEHD -> "Dolby TrueHD"
                    MimeTypes.AUDIO_AAC -> "AAC"
                    MimeTypes.AUDIO_FLAC -> "FLAC"
                    MimeTypes.AUDIO_OPUS -> "Opus"
                    else -> fmt.sampleMimeType?.substringAfter('/')?.uppercase(Locale.US) ?: "Audio"
                }
                val channels = when (fmt.channelCount) {
                    6 -> "5.1"
                    8 -> "7.1"
                    2 -> "Stereo"
                    1 -> "Mono"
                    else -> if (fmt.channelCount > 0) "${fmt.channelCount}ch" else ""
                }
                val sampleRate = if (fmt.sampleRate > 0) "${fmt.sampleRate / 1000} kHz" else ""
                val bitrate = if (fmt.bitrate > 0) "${fmt.bitrate / 1000} Kbps" else ""
                val lang = fmt.language?.let { Locale(it).displayLanguage }?.takeIf { it.isNotBlank() }
                    ?: "Track ${parsedAudio.size + 1}"
                val detailSpecs = listOf(codecName, channels, sampleRate, bitrate)
                    .filter { it.isNotEmpty() }
                    .joinToString(" / ")
                val detailWithLang = if (detailSpecs.isNotEmpty()) "$detailSpecs - $lang" else lang
                parsedAudio.add(
                    AudioTrackUiModel(
                        id = "a_${groupIdx}_$trackIdx",
                        groupIndex = groupIdx,
                        trackIndex = trackIdx,
                        label = fmt.label?.takeIf { it.isNotBlank() } ?: "Track ${parsedAudio.size + 1} - $lang",
                        detail = detailWithLang,
                        isSelected = group.isTrackSelected(trackIdx)
                    )
                )
            }
        } else if (group.type == C.TRACK_TYPE_TEXT) {
            for (trackIdx in 0 until group.length) {
                val fmt = group.getTrackFormat(trackIdx)
                val lang = fmt.language?.let { Locale(it).displayLanguage }?.takeIf { it.isNotBlank() }
                val label = fmt.label?.takeIf { it.isNotBlank() } ?: lang ?: "Subtitle ${parsedSubs.size + 1}"
                parsedSubs.add(
                    SubtitleTrackUiModel(
                        id = "s_${groupIdx}_$trackIdx",
                        groupIndex = groupIdx,
                        trackIndex = trackIdx,
                        label = label,
                        isExternal = fmt.id?.startsWith("ext") == true || label.startsWith("External"),
                        isSelected = group.isTrackSelected(trackIdx)
                    )
                )
            }
        }
    }
    return parsedAudio to parsedSubs
}

@Composable
private fun rememberVideoLuminanceState(
    playerView: PlayerView?,
    isControlsVisible: Boolean,
    isPlaying: Boolean,
    isInPipMode: Boolean
): Boolean {
    var isLightTopRegion by remember { mutableStateOf(false) }

    LaunchedEffect(playerView, isControlsVisible, isPlaying, isInPipMode) {
        if (isControlsVisible || isInPipMode || playerView == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            isLightTopRegion = false
            return@LaunchedEffect
        }

        while (true) {
            val luminance = sampleTopRegionLuminance(playerView)
            if (luminance != null) {
                isLightTopRegion = luminance >= 0.5f
            }
            delay(800L)
        }
    }

    return isLightTopRegion
}

@OptIn(UnstableApi::class)
private suspend fun sampleTopRegionLuminance(playerView: PlayerView): Float? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
    val surfaceView = playerView.videoSurfaceView ?: return null
    if (!surfaceView.isAttachedToWindow || surfaceView.width <= 0 || surfaceView.height <= 0) {
        return null
    }

    val sampleWidth = 32
    val sampleHeight = 18

    val bitmap: Bitmap = when (surfaceView) {
        is SurfaceView -> {
            val surface = surfaceView.holder?.surface
            if (surface == null || !surface.isValid) return null
            val destBitmap = Bitmap.createBitmap(sampleWidth, sampleHeight, Bitmap.Config.ARGB_8888)
            val copyStatus = withTimeoutOrNull(400L) {
                suspendCancellableCoroutine<Int> { cont ->
                    runCatching {
                        PixelCopy.request(
                            surfaceView,
                            destBitmap,
                            { result ->
                                if (cont.isActive) {
                                    cont.resume(result)
                                }
                            },
                            Handler(Looper.getMainLooper())
                        )
                    }.onFailure {
                        if (cont.isActive) {
                            cont.resume(PixelCopy.ERROR_UNKNOWN)
                        }
                    }
                }
            }
            if (copyStatus != PixelCopy.SUCCESS) {
                destBitmap.recycle()
                return null
            }
            destBitmap
        }
        is TextureView -> {
            if (!surfaceView.isAvailable) return null
            surfaceView.getBitmap(sampleWidth, sampleHeight) ?: return null
        }
        else -> return null
    }

    return try {
        withContext(Dispatchers.Default) {
            val width = bitmap.width
            val topRows = (bitmap.height / 4).coerceAtLeast(3).coerceAtMost(bitmap.height)
            if (width <= 0 || topRows <= 0) return@withContext null

            val pixels = IntArray(width * topRows)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, topRows)

            var totalLuminance = 0f
            for (px in pixels) {
                val r = ((px shr 16) and 0xFF) / 255f
                val g = ((px shr 8) and 0xFF) / 255f
                val b = (px and 0xFF) / 255f
                totalLuminance += (0.299f * r + 0.587f * g + 0.114f * b)
            }
            (totalLuminance / pixels.size.toFloat()).coerceIn(0f, 1f)
        }
    } finally {
        if (!bitmap.isRecycled) {
            bitmap.recycle()
        }
    }
}
