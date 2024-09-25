package com.techgriffo254.harmonyhub.ui.presentation.player

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.extractor.DefaultExtractorsFactory
import com.techgriffo254.harmonyhub.domain.model.Track
import com.techgriffo254.harmonyhub.domain.repository.TrackRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException

@UnstableApi
class PlayerViewModel(
    private val application: Context,
    private val repository: TrackRepository,
    private val exoPlayerFactory: () -> ExoPlayer = {
        val trackSelector = DefaultTrackSelector(application)
        ExoPlayer.Builder(application)
            .setTrackSelector(trackSelector)
            .build()
    }
) : AndroidViewModel(application as Application) {

    private val _playerState = MutableStateFlow(TrackUiState())
    val playerState = _playerState.asStateFlow()

    private var exoPlayer: ExoPlayer? = null
    private var trackList = listOf<Track>()


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

    private fun prepareExoPlayer(audioUrl: String) {
    viewModelScope.launch {
        val directUrl = withContext(Dispatchers.IO) { getRedirectedUrl(audioUrl) }
        val dataSourceFactory = DefaultDataSource.Factory(application)

        withContext(Dispatchers.Main) {
            exoPlayer?.release()
            val extractorsFactory = DefaultExtractorsFactory()
                //.setFallbackMimeType("audio/mpeg")
            val mediaSourceFactory = ProgressiveMediaSource.Factory(dataSourceFactory, extractorsFactory)
            exoPlayer = ExoPlayer.Builder(application)
                .setMediaSourceFactory(mediaSourceFactory)
                .build()
            exoPlayer?.let {
                val mediaItem = MediaItem.Builder()
                    .setUri(directUrl)
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
                                    duration = it.duration.toFloat() // Access duration from player
                                )
                            }

                            Player.STATE_ENDED -> {
                                _playerState.value = _playerState.value.copy(isPlaying = false)
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

    fun seek(progress: Float) {
        exoPlayer?.let { player ->
            if (player.duration > 0) {
                val seekPosition = (progress * player.duration).toInt()
                player.seekTo(seekPosition.toLong())
                _playerState.value = _playerState.value.copy(progress = progress)
            }
        }
    }

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

    override fun onCleared() {
        super.onCleared()
        exoPlayer?.release()
        exoPlayer = null
    }

    fun playPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                _playerState.value = _playerState.value.copy(isPlaying = false)
            } else {
                player.play()
                _playerState.value = _playerState.value.copy(isPlaying = true)
            }
        }
    }

    private suspend fun getRedirectedUrl(url: String): String {
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