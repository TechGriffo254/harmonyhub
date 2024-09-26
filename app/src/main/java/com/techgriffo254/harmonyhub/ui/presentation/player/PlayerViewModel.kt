package com.techgriffo254.harmonyhub.ui.presentation.player


import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.session.MediaController
import com.techgriffo254.harmonyhub.domain.model.Track
import com.techgriffo254.harmonyhub.domain.repository.TrackRepository
import com.techgriffo254.harmonyhub.ui.presentation.service.PlaybackService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.asDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException

class PlayerViewModel(
    private val application: Context,
    private val repository: TrackRepository
) : AndroidViewModel(application as Application)
{

    private var mediaController: MediaController? = null
    private var serviceConnection: ServiceConnection? = null
    private var exoPlayer: ExoPlayer? = null
    private var trackList = listOf<Track>()

    private val _playerState = MutableStateFlow(TrackUiState())
    val playerState = _playerState.asStateFlow()

    init {
        connectToService()
    }

    private fun connectToService() {
        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as PlaybackService.LocalBinder
                val sessionToken = binder.getSessionToken()
                viewModelScope.launch {
                    mediaController = MediaController.Builder(application, sessionToken)
                        .buildAsync()
                        .asDeferred()
                        .await() // Await for asynchronous initialization
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                mediaController = null
            }
        }

        Intent(application, PlaybackService::class.java).also { intent ->
            application.bindService(intent, serviceConnection!!, Context.BIND_AUTO_CREATE)
        }
    }

        /**
     * Fetches a track by its ID and prepares the ExoPlayer for playback.
     *
     * @param id The ID of the track to fetch.
     */
    fun getTrackById(id: String) {
        viewModelScope.launch {
            repository.getTrackById(id).collect { track ->
                if (track != null) {
                    _playerState.value = _playerState.value.copy(
                        isLoading = false,
                        track = track,
                        error = null
                    )
                    prepareExoPlayer(track.audio)
                } else {
                    _playerState.value = _playerState.value.copy(
                        isLoading = false,
                        error = Error("Track not found")
                    )
                }
            }
        }
    }

    /**
     * Prepares the ExoPlayer for playback.
     *
     * @param audioUrl The URL of the audio to play.
     */
    @OptIn(UnstableApi::class)
    private fun prepareExoPlayer(audioUrl: String) {
        viewModelScope.launch {
//            val directUrl = withContext(Dispatchers.IO) { getRedirectedUrl(audioUrl) }
            val dataSourceFactory = DefaultDataSource.Factory(application)

            withContext(Dispatchers.Main) {
                exoPlayer?.release()
                val extractorsFactory = DefaultExtractorsFactory()
                val mediaSourceFactory = ProgressiveMediaSource.Factory(dataSourceFactory)
                exoPlayer = ExoPlayer.Builder(application)
                    .setMediaSourceFactory(mediaSourceFactory)
                    .build()
                exoPlayer?.let {
                    val mediaItem = MediaItem.Builder()
                        .setUri(audioUrl)
                        .setMimeType(MimeTypes.AUDIO_MPEG)
                        .build()
                    it.setMediaItem(mediaItem)
                    it.prepare()
                    it.playWhenReady = true
                    it.addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            when (playbackState) {
                                Player.STATE_READY -> {
                                    _playerState.value = _playerState.value.copy(
                                        isReady = true,
                                        duration = it.duration.toFloat()
                                    )
                                }

                                Player.STATE_ENDED -> {
                                    _playerState.value = _playerState.value.copy(isPlaying = false)
                                }

                                Player.STATE_BUFFERING -> {
                                    _playerState.value = _playerState.value.copy(isLoading = true)
                                }

                                Player.STATE_IDLE -> {
                                    _playerState.value = _playerState.value.copy(isLoading = false)
                                }
                            }
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            val errorMessage = when (error.errorCode) {
                                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "Network connection failed"
                                PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> "File not found"
                                else -> "Unknown error: ${error.message}"
                            }
                            _playerState.value = _playerState.value.copy(
                                error = Error("ExoPlayer error: $errorMessage"),
                                isReady = false
                            )
                        }
                    })
                }
            }
        }
    }

    /**
     * Seeks to a position in the currently playing track.
     *
     * @param progress The position to seek to, as a fraction of the track's duration.
     */
    fun seek(progress: Float) {
        exoPlayer?.let { player ->
            if (player.duration > 0) {
                val seekPosition = (progress * player.duration).toInt()
                player.seekTo(seekPosition.toLong())
                _playerState.value = _playerState.value.copy(progress = progress)
            }
        }
    }

    /**
     * Plays the next track in the track list.
     */
    fun playNext() {
        val currentTrack = _playerState.value.track
        val currentTrackIndex = trackList.indexOfFirst { it.id == currentTrack.id }
        if (currentTrackIndex < trackList.size - 1) {
            val nextTrack = trackList[currentTrackIndex + 1]
            _playerState.value = _playerState.value.copy(
                track = nextTrack,
                progress = 0f,
                isPlaying = false,
                isReady = false
            )
            prepareExoPlayer(nextTrack.audio)
        }
    }

    /**
     * Plays the previous track in the track list.
     */
    fun playPrevious() {
        val currentTrack = _playerState.value.track
        val currentTrackIndex = trackList.indexOfFirst { it.id == currentTrack.id }
        if (currentTrackIndex > 0) {
            val previousTrack = trackList[currentTrackIndex - 1]
            _playerState.value = _playerState.value.copy(
                track = previousTrack,
                progress = 0f,
                isPlaying = false,
                isReady = false
            )
            prepareExoPlayer(previousTrack.audio)
        }
    }

    /**
     * Releases the ExoPlayer when the ViewModel is cleared.
     */
    override fun onCleared() {
        super.onCleared()
        exoPlayer?.release()
        exoPlayer = null
        application.unbindService(serviceConnection!!)
    }

    /**
     * Toggles playback between play and pause.
     */
    fun playPause() {
        val playbackState = mediaController?.playbackState
        if (playbackState == Player.STATE_READY && mediaController?.isPlaying == true) {
            mediaController?.pause()
        } else {
            mediaController?.play()
        }
    }

    /**
     * Gets the redirected URL for a given URL.
     *
     * @param url The URL to get the redirected URL for.
     * @return The redirected URL.
     */
    private fun getRedirectedUrl(url: String): String {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            return response.request.url.toString()
        }
    }

}

data class TrackUiState(
    val isLoading: Boolean = false,
    val error: Error? = null,
    val track: Track = Track("", "", "", "", "", ""),
    val isPlaying: Boolean = false,
    val isReady: Boolean = false,
    val progress: Float = 0f,
    val duration: Float = 0f
)


