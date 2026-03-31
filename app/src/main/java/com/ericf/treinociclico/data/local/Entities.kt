package com.ericf.treinociclico.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_state")
data class AppStateEntity(
    @PrimaryKey val id: Int = 0,
    val programJson: String,
    val scheduleStartIndex: Int,
    val workoutDraftJson: String?,
)

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey val id: String,
    val date: String,
    val sessionJson: String,
)
