package com.techgriffo254.harmonyhub.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.techgriffo254.harmonyhub.domain.repository.TrackConverters

@Database(entities = [TrackEntity::class], version = 1)
@TypeConverters(TrackConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "harmony_hub_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}