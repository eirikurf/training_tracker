package com.ericf.treinociclico.data.model

import java.time.LocalDate

enum class MuscleGroup(val label: String) {
    CHEST("Peitoral"),
    BACK("Dorsal"),
    DELTS("Deltoides"),
    BICEPS("Biceps"),
    TRICEPS("Triceps"),
    QUADS("Quadriceps"),
    GLUTES("Gluteos"),
    HAMSTRINGS("Posterior"),
    CALVES("Panturrilhas"),
    ABS("Core"),
}

enum class AdvancedTechnique(val label: String) {
    PARTIALS("Repeticoes parciais"),
    DROP_SET("Dropset"),
    REST_PAUSE("Rest and pause"),
}

enum class SetKind {
    GENERAL_WARMUP,
    EXERCISE_WARMUP,
    WORK,
}

data class GeneralWarmup(
    val title: String,
    val durationMinutes: Int,
)

data class WarmupSetTarget(
    val targetReps: Int,
    val suggestedLoadKg: Double?,
    val label: String? = null,
)

data class WorkSetTarget(
    val targetReps: Int,
    val targetRepLabel: String = targetReps.toString(),
    val targetRpe: Double?,
    val targetRpeLabel: String? = targetRpe?.toString(),
    val restSeconds: Int?,
    val restLabel: String? = null,
    val techniqueHint: AdvancedTechnique? = null,
)

data class ExerciseTemplate(
    val id: String,
    val name: String,
    val muscles: Set<MuscleGroup>,
    val warmupSets: List<WarmupSetTarget>,
    val workSets: List<WorkSetTarget>,
    val notes: String = "",
)

data class WorkoutDayTemplate(
    val id: String,
    val name: String,
    val generalWarmup: GeneralWarmup?,
    val exercises: List<ExerciseTemplate>,
)

data class ProgramBlock(
    val id: String,
    val name: String,
    val repeatCount: Int,
    val days: List<WorkoutDayTemplate>,
)

data class TrainingProgram(
    val id: String,
    val name: String,
    val description: String,
    val blocks: List<ProgramBlock>,
)

data class LoggedSet(
    val setKind: SetKind,
    val exerciseName: String?,
    val setNumber: Int,
    val reps: Int?,
    val loadKg: Double?,
    val restSeconds: Int?,
    val rpe: Double?,
    val advancedTechnique: AdvancedTechnique?,
)

data class ExerciseLog(
    val exerciseName: String,
    val muscles: Set<MuscleGroup>,
    val warmupsCompleted: Boolean,
    val loggedSets: List<LoggedSet>,
)

data class WorkoutSessionLog(
    val id: String,
    val date: LocalDate,
    val blockName: String,
    val blockIteration: Int,
    val dayName: String,
    val wasSkipped: Boolean,
    val generalWarmupCompleted: Boolean,
    val exerciseLogs: List<ExerciseLog>,
)

data class WorkoutDraft(
    val scheduledDayId: String,
    val currentStepIndex: Int,
    val generalWarmupDone: Boolean,
    val warmupDoneExerciseNames: Set<String>,
    val loggedSets: List<LoggedSet>,
)

data class ScheduledWorkout(
    val block: ProgramBlock,
    val blockIteration: Int,
    val dayIndex: Int,
    val day: WorkoutDayTemplate,
    val completedSessionsOverall: Int,
)
