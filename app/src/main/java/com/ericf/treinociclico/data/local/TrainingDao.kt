package com.ericf.treinociclico.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingDao {
    @Query("SELECT * FROM app_state WHERE id = 0")
    fun observeAppState(): Flow<AppStateEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAppState(entity: AppStateEntity)

    @Query("SELECT * FROM workout_sessions ORDER BY date ASC, id ASC")
    fun observeSessions(): Flow<List<WorkoutSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(entity: WorkoutSessionEntity)
}
