package com.techgriffo254.harmonyhub.data.local

import android.util.Log
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks")
    fun getAllTracks(): Flow<List<TrackEntity>>

    @Query("Select * FROM tracks WHERE id = :id")
    fun getTrackById(id: String): Flow<TrackEntity?>

    @Query("SELECT * FROM tracks WHERE name LIKE '%' || :query || '%' OR artistName LIKE '%' || :query || '%'")
    fun searchTracks(query: String): Flow<List<TrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<TrackEntity>)

    @Query("DELETE FROM tracks")
    suspend fun deleteAllTracks()
}