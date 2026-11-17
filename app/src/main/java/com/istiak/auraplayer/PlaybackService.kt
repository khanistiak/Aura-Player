package com.istiak.auraplayer

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Foreground Media3 `MediaSessionService` (`PlaybackService.kt`):
 * - Exposes a system `MediaSession` so Background Play (Headphones) supports:
 *   - Lock-screen media artwork & title metadata
 *   - Notification seek bar scrubbing
 *   - Bluetooth headset Play / Pause / Next / Previous hardware buttons
 * - Can either share the active in-app `ExoPlayer` instance or create a service-owned `ExoPlayer`
 *   when connected by system media controllers.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var ownedPlayer: ExoPlayer? = null

    override fun onCreate() {
        super.onCreate()
        val activePlayer = sharedPlayer ?: createFallbackPlayer().also { ownedPlayer = it }
        buildOrUpdateMediaSession(activePlayer)
    }

    private fun createFallbackPlayer(): ExoPlayer {
        val renderersFactory = DefaultRenderersFactory(this).apply {
            setEnableDecoderFallback(true)
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
        }
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        return ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
    }

    private fun buildOrUpdateMediaSession(player: Player) {
        mediaSession?.release()
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .setId("AuraPlayerMediaSession")
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        val activePlayer = sharedPlayer
        if (activePlayer != null && mediaSession?.player !== activePlayer) {
            buildOrUpdateMediaSession(activePlayer)
        }
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            release()
            mediaSession = null
        }
        ownedPlayer?.run {
            release()
            ownedPlayer = null
        }
        super.onDestroy()
    }

    companion object {
        @Volatile
        private var sharedPlayer: ExoPlayer? = null

        fun attachPlayer(player: ExoPlayer?) {
            sharedPlayer = player
        }
    }
}
