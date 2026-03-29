@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.ericf.treinociclico.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ericf.treinociclico.data.model.AdvancedTechnique
import com.ericf.treinociclico.data.model.ExerciseLog
import com.ericf.treinociclico.data.model.ExerciseTemplate
import com.ericf.treinociclico.data.model.LoggedSet
import com.ericf.treinociclico.data.model.ScheduledWorkout
import com.ericf.treinociclico.data.model.SetKind
import com.ericf.treinociclico.data.model.WarmupSetTarget
import com.ericf.treinociclico.data.model.WorkSetTarget
import com.ericf.treinociclico.data.model.WorkoutSessionLog
import java.time.LocalDate
import kotlinx.coroutines.delay

internal data class WorkoutDraft(
    val scheduledDayId: String,
    val currentStepIndex: Int,
    val generalWarmupDone: Boolean,
    val warmupDoneExerciseNames: Set<String>,
    val loggedSets: List<LoggedSet>,
)

private sealed interface WorkoutStep {
    data object GeneralWarmup : WorkoutStep
    data class ExerciseWarmup(val exerciseIndex: Int) : WorkoutStep
    data class WorkSet(val exerciseIndex: Int, val setIndex: Int) : WorkoutStep
}

@Composable
internal fun WorkoutFlowScreen(
    modifier: Modifier,
    contentPadding: PaddingValues,
    scheduledWorkout: ScheduledWorkout,
    initialDraft: WorkoutDraft?,
    onCancel: (WorkoutDraft) -> Unit,
    onComplete: (WorkoutSessionLog) -> Unit,
) {
    val steps = remember(scheduledWorkout) {
        buildList {
            if (scheduledWorkout.day.generalWarmup != null) add(WorkoutStep.GeneralWarmup)
            scheduledWorkout.day.exercises.forEachIndexed { exerciseIndex, exercise ->
                add(WorkoutStep.ExerciseWarmup(exerciseIndex))
                exercise.workSets.forEachIndexed { setIndex, _ ->
                    add(WorkoutStep.WorkSet(exerciseIndex, setIndex))
                }
            }
        }
    }
    val loggedSets = remember(scheduledWorkout, initialDraft?.loggedSets) {
        mutableStateListOf<LoggedSet>().apply {
            addAll(initialDraft?.loggedSets.orEmpty())
        }
    }
    val warmupDone = remember(scheduledWorkout, initialDraft?.warmupDoneExerciseNames) {
        mutableStateListOf<String>().apply {
            addAll(initialDraft?.warmupDoneExerciseNames.orEmpty())
        }
    }
    var generalWarmupDone by remember(scheduledWorkout, initialDraft?.generalWarmupDone) {
        mutableStateOf(initialDraft?.generalWarmupDone ?: false)
    }
    var currentStepIndex by rememberSaveable(scheduledWorkout.day.id) {
        mutableIntStateOf(initialDraft?.currentStepIndex?.coerceIn(0, (steps.size - 1).coerceAtLeast(0)) ?: 0)
    }
    var pendingSkipAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var restTimerTotalSeconds by remember { mutableIntStateOf(0) }
    var restTimerRemainingSeconds by remember { mutableIntStateOf(0) }
    var restTimerRunning by remember { mutableStateOf(false) }

    if (steps.isEmpty()) {
        LaunchedEffect(scheduledWorkout.day.id) {
            onComplete(buildSessionLog(scheduledWorkout, generalWarmupDone, warmupDone.toSet(), loggedSets))
        }
        return
    }

    val currentStep = steps[currentStepIndex]
    val currentExerciseIndex = when (currentStep) {
        WorkoutStep.GeneralWarmup -> null
        is WorkoutStep.ExerciseWarmup -> currentStep.exerciseIndex
        is WorkoutStep.WorkSet -> currentStep.exerciseIndex
    }
    val currentExercise = currentExerciseIndex?.let { scheduledWorkout.day.exercises[it] }
    val progressSubtitle = currentExerciseIndex?.let { "Exercicio ${it + 1}/${scheduledWorkout.day.exercises.size}" }
        ?: "Aquecimento geral"
    val progressBody = when (currentStep) {
        WorkoutStep.GeneralWarmup -> "Etapa ${currentStepIndex + 1} de ${steps.size}"
        is WorkoutStep.ExerciseWarmup -> "Series de aquecimento do exercicio"
        is WorkoutStep.WorkSet -> "Serie de trabalho ${currentStep.setIndex + 1}/${currentExercise?.workSets?.size ?: 0}"
    }

    fun buildDraft() = WorkoutDraft(
        scheduledDayId = scheduledWorkout.day.id,
        currentStepIndex = currentStepIndex,
        generalWarmupDone = generalWarmupDone,
        warmupDoneExerciseNames = warmupDone.toSet(),
        loggedSets = loggedSets.toList(),
    )

    fun finishWorkout() {
        restTimerRunning = false
        onComplete(buildSessionLog(scheduledWorkout, generalWarmupDone, warmupDone.toSet(), loggedSets))
    }

    fun advanceOrComplete() {
        if (currentStepIndex >= steps.lastIndex) {
            finishWorkout()
        } else {
            currentStepIndex += 1
        }
    }

    fun jumpToNextExerciseOrComplete(exerciseIndex: Int) {
        val nextIndex = steps.indexOfFirst { step ->
            when (step) {
                WorkoutStep.GeneralWarmup -> false
                is WorkoutStep.ExerciseWarmup -> step.exerciseIndex > exerciseIndex
                is WorkoutStep.WorkSet -> step.exerciseIndex > exerciseIndex
            }
        }
        if (nextIndex == -1) {
            finishWorkout()
        } else {
            currentStepIndex = nextIndex
        }
    }

    fun updateLoggedSet(set: LoggedSet) {
        val existingIndex = loggedSets.indexOfFirst {
            it.setKind == set.setKind &&
                it.exerciseName == set.exerciseName &&
                it.setNumber == set.setNumber
        }
        if (existingIndex >= 0) {
            loggedSets[existingIndex] = set
        } else {
            loggedSets.add(set)
        }
    }

    fun replaceWarmupSets(exerciseName: String, sets: List<LoggedSet>) {
        val existing = loggedSets.filterNot {
            it.setKind == SetKind.EXERCISE_WARMUP && it.exerciseName == exerciseName
        }
        loggedSets.clear()
        loggedSets.addAll(existing + sets)
    }

    fun startRestTimer(seconds: Int) {
        if (seconds <= 0) return
        restTimerTotalSeconds = seconds
        restTimerRemainingSeconds = seconds
        restTimerRunning = true
    }

    BackHandler(enabled = currentStepIndex > 0) {
        currentStepIndex -= 1
    }

    LaunchedEffect(restTimerRunning, restTimerRemainingSeconds) {
        if (restTimerRunning && restTimerRemainingSeconds > 0) {
            delay(1000)
            restTimerRemainingSeconds -= 1
            if (restTimerRemainingSeconds == 0) {
                restTimerRunning = false
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            HeroCard(
                title = currentExercise?.name ?: scheduledWorkout.day.name,
                subtitle = progressSubtitle,
                body = "$progressBody\n${scheduledWorkout.block.name} - repeticao ${scheduledWorkout.blockIteration}/${scheduledWorkout.block.repeatCount}",
            )
        }
        item {
            when (currentStep) {
                WorkoutStep.GeneralWarmup -> GeneralWarmupCard(
                    title = scheduledWorkout.day.generalWarmup?.title ?: "Aquecimento geral",
                    onSkip = { advanceOrComplete() },
                    onDone = {
                        generalWarmupDone = true
                        advanceOrComplete()
                    },
                )

                is WorkoutStep.ExerciseWarmup -> {
                    val exercise = scheduledWorkout.day.exercises[currentStep.exerciseIndex]
                    ExerciseWarmupCard(
                        exerciseIndex = currentStep.exerciseIndex,
                        totalExercises = scheduledWorkout.day.exercises.size,
                        exercise = exercise,
                        existingSets = loggedSets.filter {
                            it.setKind == SetKind.EXERCISE_WARMUP && it.exerciseName == exercise.name
                        },
                        onSkip = { advanceOrComplete() },
                        onDone = { sets ->
                            replaceWarmupSets(exercise.name, sets)
                            if (!warmupDone.contains(exercise.name)) {
                                warmupDone.add(exercise.name)
                            }
                            advanceOrComplete()
                        },
                    )
                }

                is WorkoutStep.WorkSet -> {
                    val exercise = scheduledWorkout.day.exercises[currentStep.exerciseIndex]
                    val existingSet = loggedSets.firstOrNull {
                        it.setKind == SetKind.WORK &&
                            it.exerciseName == exercise.name &&
                            it.setNumber == currentStep.setIndex + 1
                    }
                    WorkSetCard(
                        exerciseIndex = currentStep.exerciseIndex,
                        totalExercises = scheduledWorkout.day.exercises.size,
                        exercise = exercise,
                        setNumber = currentStep.setIndex + 1,
                        target = exercise.workSets[currentStep.setIndex],
                        existingSet = existingSet,
                        restTimerRunning = restTimerRunning,
                        restTimerTotalSeconds = restTimerTotalSeconds,
                        restTimerRemainingSeconds = restTimerRemainingSeconds,
                        onStartTimer = { seconds -> startRestTimer(seconds) },
                        onStopTimer = { restTimerRunning = false },
                        onSkipSet = {
                            pendingSkipAction = {
                                advanceOrComplete()
                            }
                        },
                        onSkipExercise = {
                            pendingSkipAction = {
                                jumpToNextExerciseOrComplete(currentStep.exerciseIndex)
                            }
                        },
                        onSave = { set ->
                            updateLoggedSet(set)
                            advanceOrComplete()
                        },
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { currentStepIndex -= 1 },
                    enabled = currentStepIndex > 0,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Voltar")
                }
                OutlinedButton(
                    onClick = { onCancel(buildDraft()) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Sair e continuar depois")
                }
            }
        }
        item {
            AssistChip(
                onClick = {},
                label = { Text("${loggedSets.count { it.setKind == SetKind.WORK }} series de trabalho salvas") },
            )
        }
    }

    if (pendingSkipAction != null) {
        ConfirmDialog(
            title = "Confirmar pulo",
            text = "Pular serie ou exercicio deve ser evitado. Quer continuar mesmo assim?",
            onConfirm = {
                pendingSkipAction?.invoke()
                pendingSkipAction = null
            },
            onDismiss = { pendingSkipAction = null },
        )
    }
}

private fun buildSessionLog(
    scheduledWorkout: ScheduledWorkout,
    generalWarmupDone: Boolean,
    warmupDone: Set<String>,
    loggedSets: List<LoggedSet>,
) = WorkoutSessionLog(
    id = "session-${LocalDate.now()}-${scheduledWorkout.day.id}",
    date = LocalDate.now(),
    blockName = scheduledWorkout.block.name,
    blockIteration = scheduledWorkout.blockIteration,
    dayName = scheduledWorkout.day.name,
    wasSkipped = false,
    generalWarmupCompleted = generalWarmupDone,
    exerciseLogs = scheduledWorkout.day.exercises.map { exercise ->
        ExerciseLog(
            exerciseName = exercise.name,
            muscles = exercise.muscles,
            warmupsCompleted = warmupDone.contains(exercise.name),
            loggedSets = loggedSets.filter { it.exerciseName == exercise.name },
        )
    },
)

@Composable
private fun GeneralWarmupCard(
    title: String,
    onSkip: () -> Unit,
    onDone: () -> Unit,
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Aquecimento geral", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(title)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onSkip, modifier = Modifier.weight(1f)) { Text("Pular") }
                Button(onClick = onDone, modifier = Modifier.weight(1f)) { Text("Concluir") }
            }
        }
    }
}

@Composable
private fun ExerciseWarmupCard(
    exerciseIndex: Int,
    totalExercises: Int,
    exercise: ExerciseTemplate,
    existingSets: List<LoggedSet>,
    onSkip: () -> Unit,
    onDone: (List<LoggedSet>) -> Unit,
) {
    var repsValues by rememberSaveable(exercise.id) {
        mutableStateOf(
            exercise.warmupSets.mapIndexed { index, _ ->
                existingSets.firstOrNull { it.setNumber == index + 1 }?.reps?.toString().orEmpty()
            },
        )
    }
    var loadValues by rememberSaveable(exercise.id) {
        mutableStateOf(
            exercise.warmupSets.mapIndexed { index, _ ->
                existingSets.firstOrNull { it.setNumber == index + 1 }?.loadKg?.formatLoad().orEmpty()
            },
        )
    }
    val rowsValid = exercise.warmupSets.indices.all { index ->
        val reps = repsValues[index]
        val load = loadValues[index]
        val repsValid = reps.isEmpty() || reps.toIntOrNull() != null
        val loadValid = load.isEmpty() || parseDecimal(load) != null
        repsValid && loadValid && (reps.isNotBlank() == load.isNotBlank())
    }

    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Exercicio ${exerciseIndex + 1}/$totalExercises", style = MaterialTheme.typography.bodySmall)
            Text("Series de aquecimento", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(exercise.name)
            exercise.warmupSets.forEachIndexed { index, target ->
                WarmupSetRow(
                    index = index,
                    target = target,
                    reps = repsValues[index],
                    onRepsChange = {
                        repsValues = repsValues.toMutableList().also { values ->
                            values[index] = filterIntegerInput(it)
                        }
                    },
                    load = loadValues[index],
                    onLoadChange = {
                        loadValues = loadValues.toMutableList().also { values ->
                            values[index] = filterDecimalInput(it)
                        }
                    },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onSkip, modifier = Modifier.weight(1f)) { Text("Pular") }
                Button(
                    onClick = {
                        val sets = exercise.warmupSets.mapIndexedNotNull { index, _ ->
                            val reps = repsValues[index].toIntOrNull()
                            val load = parseDecimal(loadValues[index])
                            if (reps != null && load != null) {
                                LoggedSet(
                                    setKind = SetKind.EXERCISE_WARMUP,
                                    exerciseName = exercise.name,
                                    setNumber = index + 1,
                                    reps = reps,
                                    loadKg = load,
                                    restSeconds = null,
                                    rpe = null,
                                    advancedTechnique = null,
                                )
                            } else {
                                null
                            }
                        }
                        onDone(sets)
                    },
                    enabled = rowsValid,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Salvar aquecimento")
                }
            }
        }
    }
}

@Composable
private fun WarmupSetRow(
    index: Int,
    target: WarmupSetTarget,
    reps: String,
    onRepsChange: (String) -> Unit,
    load: String,
    onLoadChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Serie de aquecimento ${index + 1}${target.label?.let { " - $it" }.orEmpty()}",
            style = MaterialTheme.typography.bodySmall,
        )
        SeriesFieldRow(
            label = "Registro",
            reps = reps,
            onRepsChange = onRepsChange,
            load = load,
            onLoadChange = onLoadChange,
        )
    }
}

@Composable
private fun WorkSetCard(
    exerciseIndex: Int,
    totalExercises: Int,
    exercise: ExerciseTemplate,
    setNumber: Int,
    target: WorkSetTarget,
    existingSet: LoggedSet?,
    restTimerRunning: Boolean,
    restTimerTotalSeconds: Int,
    restTimerRemainingSeconds: Int,
    onStartTimer: (Int) -> Unit,
    onStopTimer: () -> Unit,
    onSkipSet: () -> Unit,
    onSkipExercise: () -> Unit,
    onSave: (LoggedSet) -> Unit,
) {
    var reps by rememberSaveable("${exercise.id}-$setNumber-reps") {
        mutableStateOf(existingSet?.reps?.toString() ?: target.targetReps.toString())
    }
    var load by rememberSaveable("${exercise.id}-$setNumber-load") {
        mutableStateOf(existingSet?.loadKg?.formatLoad().orEmpty())
    }
    var rest by rememberSaveable("${exercise.id}-$setNumber-rest") {
        mutableStateOf(existingSet?.restSeconds?.toString() ?: target.restSeconds?.toString().orEmpty())
    }
    var rpe by rememberSaveable("${exercise.id}-$setNumber-rpe") {
        mutableStateOf(existingSet?.rpe?.formatLoad() ?: target.targetRpe?.formatLoad().orEmpty())
    }
    var technique by rememberSaveable("${exercise.id}-$setNumber-technique") {
        mutableStateOf(existingSet?.advancedTechnique ?: target.techniqueHint)
    }
    val restTarget = target.restLabel ?: target.restSeconds?.let { "${it}s" } ?: "-"
    val timerSeconds = rest.toIntOrNull() ?: target.restSeconds ?: 0
    val saveEnabled = reps.toIntOrNull() != null && parseDecimal(load) != null

    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Exercicio ${exerciseIndex + 1}/$totalExercises", style = MaterialTheme.typography.bodySmall)
            Text(exercise.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Serie de trabalho $setNumber/${exercise.workSets.size}")
            Text("Meta: ${target.targetRepLabel} reps - descanso $restTarget - RPE ${target.targetRpeLabel ?: "-"}")
            if (exercise.notes.isNotBlank()) {
                Text(exercise.notes, style = MaterialTheme.typography.bodySmall)
            }
            SeriesFieldRow(
                label = "Registro",
                reps = reps,
                onRepsChange = { reps = filterIntegerInput(it) },
                load = load,
                onLoadChange = { load = filterDecimalInput(it) },
            )
            OutlinedTextField(
                value = rest,
                onValueChange = { rest = filterIntegerInput(it) },
                label = { Text("Descanso (opcional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = rpe,
                onValueChange = { rpe = filterDecimalInput(it) },
                label = { Text("RPE (opcional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AdvancedTechnique.entries.forEach { option ->
                    FilterChip(
                        selected = technique == option,
                        onClick = { technique = if (technique == option) null else option },
                        label = { Text(option.label) },
                    )
                }
            }
            Card {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Timer de descanso", fontWeight = FontWeight.Medium)
                    Text(
                        if (restTimerTotalSeconds > 0) {
                            if (restTimerRunning || restTimerRemainingSeconds > 0) {
                                formatDuration(restTimerRemainingSeconds)
                            } else {
                                "Concluido"
                            }
                        } else {
                            "Use o tempo estipulado para esta serie"
                        },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { onStartTimer(timerSeconds) },
                            enabled = timerSeconds > 0,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Iniciar ${if (timerSeconds > 0) formatDuration(timerSeconds) else ""}".trim())
                        }
                        OutlinedButton(
                            onClick = onStopTimer,
                            enabled = restTimerRunning || restTimerRemainingSeconds > 0,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Parar")
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onSkipSet, modifier = Modifier.weight(1f)) { Text("Pular serie") }
                OutlinedButton(onClick = onSkipExercise, modifier = Modifier.weight(1f)) { Text("Pular exercicio") }
            }
            Button(
                onClick = {
                    onSave(
                        LoggedSet(
                            setKind = SetKind.WORK,
                            exerciseName = exercise.name,
                            setNumber = setNumber,
                            reps = reps.toIntOrNull() ?: 0,
                            loadKg = parseDecimal(load) ?: 0.0,
                            restSeconds = rest.toIntOrNull(),
                            rpe = parseDecimal(rpe),
                            advancedTechnique = technique,
                        ),
                    )
                },
                enabled = saveEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Salvar e proxima")
            }
        }
    }
}

@Composable
private fun SeriesFieldRow(
    label: String,
    reps: String,
    onRepsChange: (String) -> Unit,
    load: String,
    onLoadChange: (String) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = reps,
            onValueChange = onRepsChange,
            modifier = Modifier.weight(1f),
            label = { Text("$label reps") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        OutlinedTextField(
            value = load,
            onValueChange = onLoadChange,
            modifier = Modifier.weight(1f),
            label = { Text("$label carga") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
    }
}

private fun filterIntegerInput(input: String): String = input.filter { it.isDigit() }

private fun filterDecimalInput(input: String): String {
    val normalized = input.replace(',', '.')
    val builder = StringBuilder()
    var dotSeen = false
    normalized.forEachIndexed { index, char ->
        when {
            char.isDigit() -> builder.append(char)
            char == '.' && !dotSeen && index != 0 -> {
                builder.append(char)
                dotSeen = true
            }
        }
    }
    return builder.toString()
}

private fun parseDecimal(value: String): Double? = value.replace(',', '.').toDoubleOrNull()

private fun Double.formatLoad(): String {
    val integer = toInt().toDouble()
    return if (this == integer) integer.toInt().toString() else toString()
}

private fun formatDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
