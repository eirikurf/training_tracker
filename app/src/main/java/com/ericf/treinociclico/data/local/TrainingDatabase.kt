package com.ericf.treinociclico.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [AppStateEntity::class, WorkoutSessionEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class TrainingDatabase : RoomDatabase() {
    abstract fun trainingDao(): TrainingDao

    companion object {
        @Volatile
        private var instance: TrainingDatabase? = null

        fun getInstance(context: Context): TrainingDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TrainingDatabase::class.java,
                    "training-tracker.db",
                ).build().also { instance = it }
            }
    }
}
