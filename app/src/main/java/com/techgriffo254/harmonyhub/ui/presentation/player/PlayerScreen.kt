package com.techgriffo254.harmonyhub.ui.presentation.player

import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import coil.compose.rememberAsyncImagePainter
import com.techgriffo254.harmonyhub.domain.model.Track

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    trackId: String,
    onBackClick: () -> Unit,
    viewModel: PlayerViewModel
) {
    val playerState by viewModel.playerState.collectAsState()

    LaunchedEffect(trackId) {
        viewModel.getTrackById(trackId)
    }

    when {
        playerState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        playerState.error != null -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Error: ${playerState.error!!.message}", color = MaterialTheme.colorScheme.error)
            }
        }
        playerState.track.id.isNotEmpty() -> {
            PlayerContent(
                track = playerState.track,
                isPlaying = playerState.isPlaying,
                progress = playerState.progress,
                onProgressChange = { viewModel.seek(it) },
                onPlayPauseClick = { viewModel.playPause() },
                onNextClick = { viewModel.playNext() },
                onPreviousClick = { viewModel.playPrevious() },
                onBackClick = onBackClick,
                duration = playerState.duration,
                isReady = playerState.isReady,
                modifier = Modifier
            )
        }
        else -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Track not found", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun PlayerContent(
    track: Track,
    isPlaying: Boolean,
    isReady: Boolean,
    progress: Float,
    duration: Float,
    onProgressChange: (Float) -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0E0E15))
            .padding(16.dp)
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("Playing Now", style = MaterialTheme.typography.titleMedium, color = Color.White)
            IconButton(onClick = { /* TODO: Implement menu */ }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.White)
            }
        }

        // Album art
        Image(
            painter = rememberAsyncImagePainter(track.image),
            contentDescription = "Album Art",
            modifier = Modifier
                .size(300.dp)
                .clip(MaterialTheme.shapes.medium),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Track info
        Text(
            text = track.name,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = track.artistName,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))
        // Progress bar
        Slider(
            value = progress,
            onValueChange = onProgressChange,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
//            Text("${formatDuration(progress * track. .toFloat())}", color = Color.Gray)
//            Text("${formatDuration(track.duration.toFloat())}", color = Color.Gray)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Playback controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousClick) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(48.dp))
            }
            IconButton(onClick = onPlayPauseClick) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
            }
            IconButton(onClick = onNextClick) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(48.dp))
            }
        }
    }
}

fun formatDuration(durationInSeconds: Float): String {
    val minutes = (durationInSeconds / 60).toInt()
    val seconds = (durationInSeconds % 60).toInt()
    return String.format("%d:%02d", minutes, seconds)
}
