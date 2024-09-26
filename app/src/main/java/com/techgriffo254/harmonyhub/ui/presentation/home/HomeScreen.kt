package com.techgriffo254.harmonyhub.ui.presentation.home

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.techgriffo254.harmonyhub.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.jar.Manifest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onSearchQueryChange: (String) -> Unit,
    onRemoteTrackClick: (String) -> Unit, // Separate click handler for remote tracks
    onLocalTrackClick: (String) -> Unit // Separate click handler
    ) {
    var selectedScreen by remember { mutableStateOf(Screens.Remote) } // Keep track of selected screen

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedScreen == Screens.Remote,
                    icon = { Icon(Icons.Default.MusicNote, contentDescription = "Remote Tracks") },
                    label = { Text("Remote") },
                    onClick = { selectedScreen = Screens.Remote }
                )
                NavigationBarItem(
                    selected = selectedScreen == Screens.Local,
                    icon = { Icon(Icons.Default.Storage, contentDescription = "Local Tracks") },
                    label = { Text("Local") },
                    onClick = { selectedScreen = Screens.Local }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply padding from the Scaffold
                .padding(16.dp)
        ) {
            Text(
                text = "HarmonyHub",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Display content based on selected screen
            when (selectedScreen) {
                Screens.Remote -> {
                    // Remote Search Bar
                    SearchBar(
                        query = uiState.searchQuery,
                        onQueryChange = onSearchQueryChange,
                        onSearch = { onSearchQueryChange(it) },
                        active = false,
                        onActiveChange = { false },
                        trailingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        modifier = Modifier.fillMaxWidth()
                    ){}

                    if (uiState.isLoading) {
                        Box(
                            Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (uiState.error != null) {
                        Text(
                            text = "Error Occurred",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    } else {
                        Log.d("Track", "Tracks: ${uiState.remoteTracks}")
                        LazyColumn {
                            items(uiState.remoteTracks) { track ->
                                TrackItem(track, onRemoteTrackClick)
                            }
                        }
                    }
                }
                Screens.Local -> {
                    // Local Tracks
                    if (uiState.localTracks .isEmpty()) {
                        Text("No local tracks found", modifier = Modifier.padding(16.dp))
                    } else {
                        LazyColumn {
                            items(uiState.localTracks) { track ->
                                TrackItem(track, onLocalTrackClick)
                            }
                        }
                    }
                }
            }
        }
    }
}



// Define your screens
enum class Screens {
    Remote,
    Local
}


@Composable
fun TrackItem(
    track: Track,
    onClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick(track.id) }
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            TrackImageLoader(track = track)
            Text(text = track.name, style = MaterialTheme.typography.headlineSmall)
            Text(text = "Artist: ${track.artistName}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Album: ${track.albumName}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun TrackImageLoader(modifier: Modifier = Modifier, track: Track) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(track.image) {
        isLoading = true
        error = null
        try {
            val loadedBitmap = withContext(Dispatchers.IO) {
                val connection = URL(track.image).openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val inputStream = connection.inputStream
                BitmapFactory.decodeStream(inputStream)
            }
            bitmap = loadedBitmap
        } catch (e: Exception) {
            error = e.message
        } finally {
            isLoading = false
        }
    }
}