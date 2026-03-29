package com.ericf.treinociclico.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ericf.treinociclico.data.SamplePrograms
import com.ericf.treinociclico.data.model.ExerciseTemplate
import com.ericf.treinociclico.data.model.ScheduledWorkout
import com.ericf.treinociclico.data.model.TrainingProgram
import com.ericf.treinociclico.data.model.WorkoutSessionLog
import java.time.LocalDate

private enum class RootTab(val label: String) {
    TODAY("Hoje"),
    PLAN("Plano"),
    HISTORY("Historico"),
    STATS("Estatisticas"),
}

@Composable
fun TrainingTrackerApp() {
    val snackbarHostState = remember { SnackbarHostState() }
    var program by remember { mutableStateOf(SamplePrograms.starterProgram) }
    val logs = remember { mutableStateListOf<WorkoutSessionLog>().apply { addAll(SamplePrograms.demoLogs()) } }
    var tab by rememberSaveable { mutableStateOf(RootTab.TODAY) }
    var activeWorkout by remember { mutableStateOf<ScheduledWorkout?>(null) }
    var workoutDraft by remember { mutableStateOf<WorkoutDraft?>(null) }
    val scheduled = remember(program, logs.size) { SamplePrograms.computeScheduledWorkout(program, logs) }
    val completedToday = remember(logs.size) {
        logs.lastOrNull { !it.wasSkipped && it.date == LocalDate.now() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            BottomAppBar(containerColor = MaterialTheme.colorScheme.surface) {
                RootTab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        if (tab == item) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        androidx.compose.foundation.shape.CircleShape,
                                    ),
                            )
                        },
                        label = { androidx.compose.material3.Text(item.label) },
                    )
                }
            }
        },
    ) { padding ->
        if (activeWorkout != null) {
            WorkoutFlowScreen(
                modifier = Modifier,
                contentPadding = padding,
                scheduledWorkout = activeWorkout!!,
                initialDraft = if (workoutDraft?.scheduledDayId == activeWorkout!!.day.id) workoutDraft else null,
                onCancel = { draft ->
                    workoutDraft = draft
                    activeWorkout = null
                },
                onComplete = { session ->
                    logs.add(session)
                    workoutDraft = null
                    activeWorkout = null
                    tab = RootTab.TODAY
                },
            )
        } else {
            when (tab) {
                RootTab.TODAY -> TodayScreen(
                    contentPadding = padding,
                    scheduled = scheduled,
                    completedToday = completedToday,
                    hasDraft = workoutDraft?.scheduledDayId == scheduled.day.id,
                    onStart = { activeWorkout = scheduled },
                    onSkip = {
                        logs.add(
                            WorkoutSessionLog(
                                id = "skip-${scheduled.day.id}-${logs.size}",
                                date = LocalDate.now(),
                                blockName = scheduled.block.name,
                                blockIteration = scheduled.blockIteration,
                                dayName = scheduled.day.name,
                                wasSkipped = true,
                                generalWarmupCompleted = false,
                                exerciseLogs = emptyList(),
                            ),
                        )
                    },
                )
                RootTab.PLAN -> PlanScreen(
                    contentPadding = padding,
                    program = program,
                    logs = logs,
                    onProgramChange = { program = it },
                )
                RootTab.HISTORY -> HistoryScreen(contentPadding = padding, logs = logs)
                RootTab.STATS -> StatsScreen(contentPadding = padding, logs = logs)
            }
        }
    }
}

internal fun updateExerciseInProgram(
    program: TrainingProgram,
    blockId: String,
    dayId: String,
    exerciseId: String,
    transform: (ExerciseTemplate) -> ExerciseTemplate,
): TrainingProgram = program.copy(
    blocks = program.blocks.map { block ->
        if (block.id != blockId) {
            block
        } else {
            block.copy(
                days = block.days.map { day ->
                    if (day.id != dayId) {
                        day
                    } else {
                        day.copy(
                            exercises = day.exercises.map { exercise ->
                                if (exercise.id == exerciseId) transform(exercise) else exercise
                            },
                        )
                    }
                },
            )
        }
    },
)
