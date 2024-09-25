package com.techgriffo254.harmonyhub.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String,
    val name: String?,
    val artistName: String?,
    val albumName: String?,
    val image: String?,
    val audio: String?
)