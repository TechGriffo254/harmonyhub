package com.techgriffo254.harmonyhub.domain.repository

import android.util.Log
import com.techgriffo254.harmonyhub.data.JamendoApiService
import com.techgriffo254.harmonyhub.data.local.TrackDao
import com.techgriffo254.harmonyhub.data.local.TrackEntity
import com.techgriffo254.harmonyhub.domain.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class TrackRepository(
    private val trackDao: TrackDao,
    private val jamendoApiService: JamendoApiService
) {
    fun getAllTracks(): Flow<List<Track>> {
        return trackDao.getAllTracks().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    fun searchTracks(query: String): Flow<List<Track>> {
        return trackDao.searchTracks(query).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    fun getTrackById(id: String): Flow<Track?> {
        return trackDao.getTrackById(id).map { it?.toDomainModel() }
    }

    suspend fun refreshTracks(accessToken: String, clientId: String) {
        try {
            val response =
                jamendoApiService.getTracks(accessToken = accessToken, clientId = clientId)
            Log.d("TrackRepository", "API Response: ${response.results}")

            // Convert the API response into a list of TrackEntity
            val tracks = response.results.map { it.toEntity() }

            // Clear the existing tracks from the database
            trackDao.deleteAllTracks()

            // Insert the list of tracks into the database (it will be converted into a JSON string)
            trackDao.insertTracks(tracks)

            // Fetch and log the inserted tracks
            val insertedTracks = trackDao.getAllTracks().first()
            Log.d("TrackRepository", "Inserted Tracks: $insertedTracks")
        } catch (e: Exception) {
            // Handle network errors
            e.printStackTrace()
        }
    }

    private fun TrackEntity.toDomainModel(): Track {
        return Track(
            id,
            name ?: "Unknown",
            artistName ?: "Unknown Artist",
            albumName ?: "Unknown Album",
            image ?: "",
            audio ?: ""
        )
    }

    private fun Track.toEntity(): TrackEntity {
        return TrackEntity(id, name, artistName, albumName, image, audio)
    }
}
