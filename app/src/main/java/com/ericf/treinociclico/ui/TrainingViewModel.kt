package com.ericf.treinociclico.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ericf.treinociclico.data.PersistedAppState
import com.ericf.treinociclico.data.TrainingRepository
import com.ericf.treinociclico.data.local.TrainingDatabase
import com.ericf.treinociclico.data.model.TrainingProgram
import com.ericf.treinociclico.data.model.WorkoutDraft
import com.ericf.treinociclico.data.model.WorkoutSessionLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TrainingUiState(
    val isLoaded: Boolean = false,
    val program: TrainingProgram? = null,
    val scheduleStartIndex: Int = 0,
    val workoutDraft: WorkoutDraft? = null,
    val logs: List<WorkoutSessionLog> = emptyList(),
)

class TrainingViewModel(
    private val repository: TrainingRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TrainingUiState())
    val uiState: StateFlow<TrainingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureInitialized()
            repository.appState.collect { persisted ->
                applyPersistedState(persisted)
            }
        }
    }

    fun updateProgram(program: TrainingProgram) {
        viewModelScope.launch { repository.saveProgram(program) }
    }

    fun updateScheduleStartIndex(scheduleStartIndex: Int) {
        viewModelScope.launch { repository.saveScheduleStartIndex(scheduleStartIndex) }
    }

    fun updateWorkoutDraft(workoutDraft: WorkoutDraft?) {
        viewModelScope.launch { repository.saveWorkoutDraft(workoutDraft) }
    }

    fun addSessionLog(session: WorkoutSessionLog) {
        viewModelScope.launch {
            repository.saveSessionLog(session)
            repository.saveWorkoutDraft(null)
        }
    }

    private fun applyPersistedState(persisted: PersistedAppState) {
        _uiState.update {
            it.copy(
                isLoaded = true,
                program = persisted.program,
                scheduleStartIndex = persisted.scheduleStartIndex,
                workoutDraft = persisted.workoutDraft,
                logs = persisted.logs,
            )
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val database = TrainingDatabase.getInstance(context)
                val repository = TrainingRepository(database.trainingDao())
                return TrainingViewModel(repository) as T
            }
        }
    }
}
