package com.techgriffo254.harmonyhub.ui.presentation.service


import android.app.PendingIntent
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultAllocator
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionToken

class PlaybackService : MediaSessionService() {

    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var binder = LocalBinder()

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        // Setup LoadControl for ExoPlayer
        val loadControl: LoadControl = DefaultLoadControl.Builder()
            .setAllocator(DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE))
            .setBufferDurationsMs(
                DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
                DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            ).build()

        // Initialize ExoPlayer
        val dataSourceFactory = DefaultDataSource.Factory(this)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(), true)
            .setLoadControl(loadControl)
            .build()

        // Create PendingIntent to open the UI of the app
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        // Initialize Media3's MediaSession
        mediaSession = MediaSession.Builder(this, exoPlayer!!)
            .setSessionActivity(pendingIntent)
            .build()

        // Add a MediaItem to ExoPlayer as an example
        val mediaItem = MediaItem.Builder()
            .setUri("https://storage.googleapis.com/exoplayer-test-media-0/play.mp3")
            .setMimeType(MimeTypes.AUDIO_MPEG)
            .build()

        exoPlayer?.apply {
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    // Provide MediaSession.Token to client components like ViewModel
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    // Binder to expose the SessionToken to client components
    inner class LocalBinder : Binder() {
        fun getSessionToken(): SessionToken {
            return mediaSession!!.token
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        mediaSession?.release()
    }
}

//class PlaybackService : MediaSessionService() {
//
//    private var mediaSession: MediaSession? = null
//    private var binder = LocalBinder()
//    @OptIn(UnstableApi::class)
//    override fun onCreate() {
//        super.onCreate()
//
//        val loadControlBuilder = DefaultLoadControl.Builder()
//        loadControlBuilder.setBufferDurationsMs(
//            DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
//            DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
//            DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
//            DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
//        ).setBackBuffer(30_000, false)
//        val loadControl: LoadControl = loadControlBuilder.build()
//
//        val dataSourceFactory = DefaultDataSource.Factory(this)
//        val mediaSourceFactory = ProgressiveMediaSource.Factory(dataSourceFactory, DefaultExtractorsFactory())
//
//        val player = ExoPlayer.Builder(this)
//            .setMediaSourceFactory(mediaSourceFactory)
//            .setLoadControl(loadControl)
//            .setAudioAttributes(AudioAttributes.DEFAULT, true)
//            .setDeviceVolumeControlEnabled(true)
//            .setHandleAudioBecomingNoisy(true)
//            .setWakeMode(C.WAKE_MODE_NETWORK)
//            .build()
//
//        val intent = packageManager.getLaunchIntentForPackage(packageName)
//        val pendingIntent = PendingIntent.getActivity(this, 0, intent, FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT)
//
//        mediaSession = MediaSession.Builder(this, player)
//            .setSessionActivity(pendingIntent)
//            .build()
//
//    }
//    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
//        return mediaSession
//    }
//    override fun onDestroy() {
//        super.onDestroy()
//        // Release the player and the session when the service is destroyed
//        mediaSession?.player?.release()
//        mediaSession?.release()
//    }
//
//    override fun onBind(intent: Intent?): IBinder? {
//        return super.onBind(intent)
//        return binder
//    }
//
//    inner class LocalBinder: Binder() {
//        @SuppressLint("RestrictedApi")
//        @OptIn(UnstableApi::class)
//        fun getMediaSession() : MediaSessionCompat.Token?{
//            return mediaSession?.token
//        }
//    }
//
//}