package com.techgriffo254.harmonyhub.domain.repository

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.techgriffo254.harmonyhub.data.local.TrackEntity

class TrackConverters {

    @TypeConverter
    fun fromTrackList(value: List<TrackEntity>?): String? {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toTrackList(value: String?): List<TrackEntity>? {
        val listType = object : TypeToken<List<TrackEntity>>() {}.type
        return Gson().fromJson(value, listType)
    }
}