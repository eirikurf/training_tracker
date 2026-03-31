package com.ericf.treinociclico.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ericf.treinociclico.data.model.AdvancedTechnique
import com.ericf.treinociclico.data.model.ExerciseLog
import com.ericf.treinociclico.data.model.ExerciseTemplate
import com.ericf.treinociclico.data.model.LoggedSet
import com.ericf.treinociclico.data.model.ScheduledWorkout
import com.ericf.treinociclico.data.model.SetKind
import com.ericf.treinociclico.data.model.WorkoutDraft
import com.ericf.treinociclico.data.model.WorkoutSessionLog
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private sealed interface WorkoutStep {
    data object GeneralWarmup : WorkoutStep
    data class Exercise(val exerciseIndex: Int) : WorkoutStep
}

@Composable
internal fun WorkoutFlowScreen(
    modifier: Modifier,
    contentPadding: PaddingValues,
    scheduledWorkout: ScheduledWorkout,
    initialDraft: WorkoutDraft?,
    onCancel: (WorkoutDraft) -> Unit,
    onDraftChange: (WorkoutDraft) -> Unit,
    onComplete: (WorkoutSessionLog) -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val steps = remember(scheduledWorkout) {
        buildList {
            if (scheduledWorkout.day.generalWarmup != null) add(WorkoutStep.GeneralWarmup)
            scheduledWorkout.day.exercises.forEachIndexed { index, _ -> add(WorkoutStep.Exercise(index)) }
        }
    }

    var currentStepIndex by rememberSaveable(scheduledWorkout.day.id) {
        mutableIntStateOf(initialDraft?.currentStepIndex?.coerceIn(0, (steps.size - 1).coerceAtLeast(0)) ?: 0)
    }
    var generalWarmupDone by remember(scheduledWorkout.day.id) { mutableStateOf(initialDraft?.generalWarmupDone ?: false) }
    var warmupDoneNames by remember(scheduledWorkout.day.id) { mutableStateOf(initialDraft?.warmupDoneExerciseNames.orEmpty()) }
    var loggedSets by remember(scheduledWorkout.day.id) { mutableStateOf(initialDraft?.loggedSets.orEmpty()) }
    var showEndWorkoutConfirm by remember { mutableStateOf(false) }
    var restTimerRemainingSeconds by remember { mutableIntStateOf(0) }

    val currentStep = steps[currentStepIndex]

    fun buildDraft() = WorkoutDraft(
        scheduledDayId = scheduledWorkout.day.id,
        currentStepIndex = currentStepIndex,
        generalWarmupDone = generalWarmupDone,
        warmupDoneExerciseNames = warmupDoneNames,
        loggedSets = loggedSets,
    )

    fun upsertLoggedSet(set: LoggedSet) {
        val updated = loggedSets.toMutableList()
        val index = updated.indexOfFirst {
            it.setKind == set.setKind && it.exerciseName == set.exerciseName && it.setNumber == set.setNumber
        }
        if (index >= 0) updated[index] = set else updated.add(set)
        loggedSets = updated
    }

    fun replaceExerciseWorkSets(exerciseName: String, sets: List<LoggedSet>) {
        loggedSets = loggedSets.filterNot {
            it.setKind == SetKind.WORK && it.exerciseName == exerciseName
        } + sets
    }

    fun finishWorkout() {
        onComplete(
            WorkoutSessionLog(
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
                        warmupsCompleted = warmupDoneNames.contains(exercise.name),
                        loggedSets = loggedSets.filter { it.exerciseName == exercise.name },
                    )
                },
            ),
        )
    }

    suspend fun advanceStep() {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        if (currentStepIndex >= steps.lastIndex) {
            finishWorkout()
        } else {
            currentStepIndex += 1
            listState.scrollToItem(0)
        }
    }

    BackHandler(enabled = currentStepIndex > 0) { currentStepIndex -= 1 }

    LaunchedEffect(currentStepIndex, generalWarmupDone, warmupDoneNames, loggedSets) {
        onDraftChange(buildDraft())
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) onDraftChange(buildDraft())
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(restTimerRemainingSeconds) {
        if (restTimerRemainingSeconds > 0) {
            delay(1000)
            restTimerRemainingSeconds -= 1
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when (currentStep) {
            WorkoutStep.GeneralWarmup -> {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Aquecimento geral", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text(scheduledWorkout.day.generalWarmup?.title.orEmpty())
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { scope.launch { advanceStep() } }, modifier = Modifier.weight(1f)) {
                            Text("Pular")
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    generalWarmupDone = true
                                    advanceStep()
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Concluir")
                        }
                    }
                }
            }

            is WorkoutStep.Exercise -> {
                val exercise = scheduledWorkout.day.exercises[currentStep.exerciseIndex]
                item {
                    HeroCard(
                        title = exercise.name,
                        subtitle = "Exercício ${currentStep.exerciseIndex + 1}/${scheduledWorkout.day.exercises.size}",
                        body = "${scheduledWorkout.block.name} • repetição ${scheduledWorkout.blockIteration}/${scheduledWorkout.block.repeatCount}",
                    )
                }
                item {
                    ExerciseLoggerCard(
                        exercise = exercise,
                        initialSets = loggedSets.filter {
                            it.setKind == SetKind.WORK && it.exerciseName == exercise.name
                        },
                        warmupDone = warmupDoneNames.contains(exercise.name),
                        timerSeconds = restTimerRemainingSeconds,
                        onWarmupDoneChange = { done ->
                            warmupDoneNames = if (done) warmupDoneNames + exercise.name else warmupDoneNames - exercise.name
                        },
                        onSetComplete = { set, nextLoad, timer ->
                            upsertLoggedSet(set)
                            restTimerRemainingSeconds = timer
                            nextLoad?.let { (nextIndex, loadValue) ->
                                val nextSet = loggedSets.firstOrNull {
                                    it.exerciseName == exercise.name && it.setKind == SetKind.WORK && it.setNumber == nextIndex + 1
                                }
                                if (nextSet == null || nextSet.loadKg == null) {
                                    upsertLoggedSet(
                                        LoggedSet(
                                            setKind = SetKind.WORK,
                                            exerciseName = exercise.name,
                                            setNumber = nextIndex + 1,
                                            reps = nextSet?.reps,
                                            loadKg = loadValue,
                                            restSeconds = nextSet?.restSeconds ?: exercise.workSets[nextIndex].restSeconds,
                                            rpe = nextSet?.rpe,
                                            advancedTechnique = nextSet?.advancedTechnique,
                                        ),
                                    )
                                }
                            }
                        },
                        onSaveExercise = { sets ->
                            scope.launch {
                                replaceExerciseWorkSets(exercise.name, sets)
                                advanceStep()
                            }
                        },
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { currentStepIndex = (currentStepIndex - 1).coerceAtLeast(0) },
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
                    OutlinedButton(onClick = { showEndWorkoutConfirm = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Encerrar treino")
                    }
                }
            }
        }
    }

    if (showEndWorkoutConfirm) {
        ConfirmDialog(
            title = "Encerrar treino agora?",
            text = "O treino será salvo com o que já foi preenchido.",
            onConfirm = {
                showEndWorkoutConfirm = false
                finishWorkout()
            },
            onDismiss = { showEndWorkoutConfirm = false },
            confirmLabel = "Encerrar",
        )
    }
}

@Composable
private fun ExerciseLoggerCard(
    exercise: ExerciseTemplate,
    initialSets: List<LoggedSet>,
    warmupDone: Boolean,
    timerSeconds: Int,
    onWarmupDoneChange: (Boolean) -> Unit,
    onSetComplete: (LoggedSet, Pair<Int, Double?>?, Int) -> Unit,
    onSaveExercise: (List<LoggedSet>) -> Unit,
) {
    var repsValues by rememberSaveable(exercise.id) {
        mutableStateOf(List(exercise.workSets.size) { index -> initialSets.firstOrNull { it.setNumber == index + 1 }?.reps?.toString().orEmpty() })
    }
    var loadValues by rememberSaveable(exercise.id) {
        mutableStateOf(List(exercise.workSets.size) { index -> initialSets.firstOrNull { it.setNumber == index + 1 }?.loadKg?.formatLoad().orEmpty() })
    }
    var rpeValues by rememberSaveable(exercise.id) {
        mutableStateOf(List(exercise.workSets.size) { index -> initialSets.firstOrNull { it.setNumber == index + 1 }?.rpe?.formatLoad().orEmpty() })
    }
    var techniques by rememberSaveable(exercise.id) {
        mutableStateOf(List(exercise.workSets.size) { index -> initialSets.firstOrNull { it.setNumber == index + 1 }?.advancedTechnique })
    }
    var techniqueExpanded by rememberSaveable(exercise.id) {
        mutableStateOf(List(exercise.workSets.size) { false })
    }
    var completedSets by rememberSaveable(exercise.id) {
        mutableStateOf(initialSets.map { it.setNumber }.toSet())
    }
    var warmupExpanded by rememberSaveable(exercise.id) { mutableStateOf(false) }
    val activeIndex = exercise.workSets.indices.firstOrNull { !completedSets.contains(it + 1) } ?: exercise.workSets.lastIndex

    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Aquecimento sugerido", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        FilterChip(
                            selected = warmupDone,
                            onClick = { onWarmupDoneChange(!warmupDone) },
                            label = { Text(if (warmupDone) "Feito" else "Opcional") },
                        )
                    }
                    if (warmupExpanded) {
                        exercise.warmupSets.forEach { target ->
                            Text("- ${target.label ?: "${target.targetReps} reps"}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    OutlinedButton(onClick = { warmupExpanded = !warmupExpanded }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (warmupExpanded) "Ocultar aquecimento" else "Mostrar aquecimento")
                    }
                }
            }

            Text("Séries de trabalho", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            exercise.workSets.forEachIndexed { index, target ->
                val completed = completedSets.contains(index + 1)
                val active = index == activeIndex
                val locked = index > activeIndex && !completed

                if (timerSeconds > 0 && active && index > 0) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Descanso", fontWeight = FontWeight.SemiBold)
                                Text(formatDuration(timerSeconds), style = MaterialTheme.typography.headlineSmall)
                            }
                            Text("Próximo: Set ${index + 1}", fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            completed -> MaterialTheme.colorScheme.surfaceVariant
                            active -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        },
                    ),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = when {
                                        completed -> "Set ${index + 1} concluído"
                                        active -> "Set ${index + 1} ativo"
                                        else -> "Set ${index + 1}"
                                    },
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text("Meta", style = MaterialTheme.typography.bodySmall)
                            }
                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(target.targetRepLabel, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                if (!target.targetRpeLabel.isNullOrBlank()) {
                                    Text("RPE ${target.targetRpeLabel}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = repsValues[index],
                                onValueChange = {
                                    repsValues = repsValues.toMutableList().also { values -> values[index] = filterIntegerInput(it) }
                                },
                                enabled = !locked,
                                label = { Text("Reps") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedTextField(
                                value = loadValues[index],
                                onValueChange = {
                                    loadValues = loadValues.toMutableList().also { values -> values[index] = filterDecimalInput(it) }
                                },
                                enabled = !locked,
                                label = { Text("Carga") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                            )
                        }

                        if (!locked) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = rpeValues[index],
                                    onValueChange = {
                                        rpeValues = rpeValues.toMutableList().also { values -> values[index] = filterDecimalInput(it) }
                                    },
                                    label = { Text("RPE") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                )
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Descanso", style = MaterialTheme.typography.bodySmall)
                                    Text(formatDuration(target.restSeconds ?: 0), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    techniqueExpanded = techniqueExpanded.toMutableList().also { values ->
                                        values[index] = !values[index]
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(if (techniqueExpanded[index]) "Ocultar técnica avançada" else "Adicionar técnica avançada")
                            }

                            if (techniqueExpanded[index]) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AdvancedTechnique.entries.forEach { option ->
                                        FilterChip(
                                            selected = techniques[index] == option,
                                            onClick = {
                                                techniques = techniques.toMutableList().also { values ->
                                                    values[index] = if (values[index] == option) null else option
                                                }
                                            },
                                            label = { Text(option.label) },
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    completedSets = completedSets + (index + 1)
                                    val loadValue = parseDecimal(loadValues[index])
                                    val set = LoggedSet(
                                        setKind = SetKind.WORK,
                                        exerciseName = exercise.name,
                                        setNumber = index + 1,
                                        reps = repsValues[index].toIntOrNull(),
                                        loadKg = loadValue,
                                        restSeconds = target.restSeconds,
                                        rpe = parseDecimal(rpeValues[index]),
                                        advancedTechnique = techniques[index],
                                    )
                                    val nextPair = if (index < exercise.workSets.lastIndex) index + 1 to loadValue else null
                                    if (nextPair != null && loadValues[index + 1].isBlank() && loadValue != null) {
                                        loadValues = loadValues.toMutableList().also { values -> values[index + 1] = loadValue.formatLoad() }
                                    }
                                    onSetComplete(set, nextPair, target.restSeconds ?: 0)
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(if (completed) "Atualizar série" else "Concluir série")
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val sets = exercise.workSets.mapIndexedNotNull { index, target ->
                        val reps = repsValues[index].toIntOrNull()
                        val load = parseDecimal(loadValues[index])
                        val rpe = parseDecimal(rpeValues[index])
                        val hasAnyValue = reps != null || load != null || rpe != null || techniques[index] != null || completedSets.contains(index + 1)
                        if (!hasAnyValue) null
                        else LoggedSet(
                            setKind = SetKind.WORK,
                            exerciseName = exercise.name,
                            setNumber = index + 1,
                            reps = reps,
                            loadKg = load,
                            restSeconds = target.restSeconds,
                            rpe = rpe,
                            advancedTechnique = techniques[index],
                        )
                    }
                    onSaveExercise(sets)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Concluir exercício → próximo")
            }
        }
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

private fun Double?.formatLoad(): String = this?.let { value ->
    val integer = value.toInt().toDouble()
    if (value == integer) integer.toInt().toString() else value.toString()
}.orEmpty()

private fun formatDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
