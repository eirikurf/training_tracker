package com.ericf.treinociclico.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ericf.treinociclico.data.SamplePrograms
import com.ericf.treinociclico.data.model.ExerciseTemplate
import com.ericf.treinociclico.data.model.MuscleGroup
import com.ericf.treinociclico.data.model.ScheduledWorkout
import com.ericf.treinociclico.data.model.SetKind
import com.ericf.treinociclico.data.model.TrainingProgram
import com.ericf.treinociclico.data.model.WorkoutSessionLog
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

@Composable
internal fun TodayScreen(
    contentPadding: PaddingValues,
    scheduled: ScheduledWorkout,
    completedToday: WorkoutSessionLog?,
    hasDraft: Boolean,
    onStart: () -> Unit,
    onSkip: () -> Unit,
) {
    var showSkipConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (completedToday != null) {
            item {
                HeroCard(
                    title = "Treino concluido hoje",
                    subtitle = "${completedToday.dayName} - ${completedToday.blockName}",
                    body = "Resultados do treino de hoje ficam em destaque ate a virada do dia. Amanhã o app volta a mostrar a proxima sessao.",
                )
            }
            item {
                MetricRow(
                    primary = "Series de trabalho",
                    primaryValue = completedToday.exerciseLogs.sumOf { log -> log.loggedSets.count { it.setKind == SetKind.WORK } }.toString(),
                    secondary = "Exercicios",
                    secondaryValue = completedToday.exerciseLogs.size.toString(),
                )
            }
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Resumo da sessao", fontWeight = FontWeight.SemiBold)
                        completedToday.exerciseLogs.forEach { exercise ->
                            Text("${exercise.exerciseName} - ${exercise.loggedSets.count { it.setKind == SetKind.WORK }} series")
                        }
                    }
                }
            }
        } else {
            item {
                HeroCard(
                    title = if (hasDraft) "Treino em andamento" else "Treino de hoje",
                    subtitle = "${scheduled.day.name} - ${scheduled.block.name}",
                    body = "Sessao ${scheduled.dayIndex + 1} de ${scheduled.block.days.size}. Repeticao ${scheduled.blockIteration}/${scheduled.block.repeatCount} do bloco.",
                )
            }
            item {
                MetricRow(
                    primary = "Proximo exercicio",
                    primaryValue = scheduled.day.exercises.firstOrNull()?.name ?: "Sem exercicios",
                    secondary = "Sessoes concluidas",
                    secondaryValue = scheduled.completedSessionsOverall.toString(),
                )
            }
            item { SessionOutlineCard(scheduled = scheduled) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onStart, modifier = Modifier.weight(1f)) {
                        Text(if (hasDraft) "Continuar treino" else "Comecar")
                    }
                    OutlinedButton(onClick = { showSkipConfirm = true }, modifier = Modifier.weight(1f)) {
                        Text("Pular treino")
                    }
                }
            }
        }
    }

    if (showSkipConfirm) {
        ConfirmDialog(
            title = "Pular treino?",
            text = "Pular deve ser excecao. Quer mesmo marcar esta sessao como pulada e avancar para a proxima?",
            onConfirm = {
                onSkip()
                showSkipConfirm = false
            },
            onDismiss = { showSkipConfirm = false },
        )
    }
}

@Composable
internal fun PlanScreen(
    contentPadding: PaddingValues,
    program: TrainingProgram,
    logs: List<WorkoutSessionLog>,
    onProgramChange: (TrainingProgram) -> Unit,
) {
    val scheduled = remember(logs.size, program) { SamplePrograms.computeScheduledWorkout(program, logs) }
    var expandedDayId by rememberSaveable { mutableStateOf<String?>(null) }
    var editor by remember { mutableStateOf<ExerciseEditorState?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            HeroCard(
                title = program.name,
                subtitle = "Planejamento circular de blocos",
                body = program.description,
            )
        }
        items(program.blocks) { block ->
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(block.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Repeticoes do bloco: ${block.repeatCount}")
                    Text("Sessoes por volta: ${block.days.size}")
                    block.days.forEachIndexed { index, day ->
                        val isNext = scheduled.block.id == block.id && scheduled.day.id == day.id
                        val expanded = expandedDayId == day.id
                        Card(
                            modifier = Modifier.clickable {
                                expandedDayId = if (expanded) null else day.id
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isNext) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("${index + 1}. ${day.name}", fontWeight = FontWeight.Medium)
                                Text(
                                    "${day.exercises.size} exercicios - toque para ${if (expanded) "fechar" else "editar"}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                if (expanded) {
                                    day.exercises.forEach { exercise ->
                                        Card(
                                            modifier = Modifier.clickable {
                                                editor = ExerciseEditorState(block.id, day.id, exercise)
                                            },
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(exercise.name, fontWeight = FontWeight.SemiBold)
                                                Text(exercise.workSets.joinToString { it.targetRepLabel }, style = MaterialTheme.typography.bodySmall)
                                                if (exercise.notes.isNotBlank()) {
                                                    Text(exercise.notes, style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (editor != null) {
        ExerciseEditorDialog(
            state = editor!!,
            onDismiss = { editor = null },
            onSave = { updated ->
                onProgramChange(
                    updateExerciseInProgram(program, editor!!.blockId, editor!!.dayId, editor!!.exercise.id) {
                        it.copy(name = updated.name, notes = updated.notes)
                    },
                )
                editor = null
            },
        )
    }
}

@Composable
internal fun HistoryScreen(
    contentPadding: PaddingValues,
    logs: List<WorkoutSessionLog>,
) {
    val month = YearMonth.now()
    val days = month.lengthOfMonth()
    val firstOffset = month.atDay(1).dayOfWeek.value % 7
    val trainedDays = logs.groupBy { it.date }
    var selectedLog by remember { mutableStateOf<WorkoutSessionLog?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            HeroCard(
                title = "Historico",
                subtitle = "Calendario de sessoes",
                body = "Toque num dia marcado ou numa sessao listada para ver os detalhes do que foi registrado.",
            )
        }
        item {
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(month.month.name.lowercase().replaceFirstChar { it.uppercase() } + " ${month.year}")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("D", "S", "T", "Q", "Q", "S", "S").forEach {
                            Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        }
                    }
                    val cells = firstOffset + days
                    val weeks = (cells + 6) / 7
                    repeat(weeks) { week ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            repeat(7) { day ->
                                val cellIndex = week * 7 + day
                                val dayNumber = cellIndex - firstOffset + 1
                                val date = if (dayNumber in 1..days) month.atDay(dayNumber) else null
                                CalendarCell(
                                    date = date,
                                    logs = date?.let { trainedDays[it].orEmpty() }.orEmpty(),
                                    onClick = { pickedDate ->
                                        selectedLog = trainedDays[pickedDate]?.firstOrNull()
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
        items(logs.sortedByDescending { it.date }) { log ->
            Card(modifier = Modifier.clickable { selectedLog = log }) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("${log.date} - ${log.dayName}", fontWeight = FontWeight.SemiBold)
                    Text("${log.blockName} - repeticao ${log.blockIteration}")
                    Text(
                        if (log.wasSkipped) "Sessao pulada"
                        else "${log.exerciseLogs.sumOf { it.loggedSets.count { set -> set.setKind == SetKind.WORK } }} series de trabalho registradas",
                    )
                }
            }
        }
    }

    if (selectedLog != null) {
        SessionDetailDialog(log = selectedLog!!, onDismiss = { selectedLog = null })
    }
}

@Composable
private fun CalendarCell(
    date: LocalDate?,
    logs: List<WorkoutSessionLog>,
    onClick: (LocalDate) -> Unit,
) {
    val topLog = logs.firstOrNull()
    Box(
        modifier = Modifier
            .padding(4.dp)
            .size(40.dp)
            .background(
                when {
                    date == null -> Color.Transparent
                    logs.isEmpty() -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    topLog?.wasSkipped == true -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.primaryContainer
                },
                RoundedCornerShape(12.dp),
            )
            .clickable(enabled = date != null && logs.isNotEmpty()) { onClick(date!!) },
        contentAlignment = Alignment.Center,
    ) {
        Text(date?.dayOfMonth?.toString() ?: "", fontWeight = if (logs.isNotEmpty()) FontWeight.Bold else FontWeight.Normal)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StatsScreen(
    contentPadding: PaddingValues,
    logs: List<WorkoutSessionLog>,
) {
    val exerciseNames = logs.flatMap { it.exerciseLogs }.map { it.exerciseName }.distinct().sorted()
    var selectedExercise by rememberSaveable { mutableStateOf(exerciseNames.firstOrNull()) }
    var selectedMuscle by rememberSaveable { mutableStateOf(MuscleGroup.entries.firstOrNull()) }

    val exerciseLogs = logs.flatMap { it.exerciseLogs }.filter { it.exerciseName == selectedExercise }
    val exerciseSets = exerciseLogs.flatMap { it.loggedSets }.filter { it.setKind == SetKind.WORK }
    val bestSet = exerciseSets.maxByOrNull { it.loadKg }
    val totalVolume = exerciseSets.sumOf { it.loadKg * it.reps }
    val oneRm = bestSet?.let { it.loadKg * (1.0 + it.reps / 30.0) } ?: 0.0

    val muscleLogs = logs.flatMap { it.exerciseLogs }.filter { selectedMuscle != null && it.muscles.contains(selectedMuscle) }
    val muscleSets = muscleLogs.flatMap { it.loggedSets }.filter { it.setKind == SetKind.WORK }
    val muscleVolume = muscleSets.sumOf { it.loadKg * it.reps }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            HeroCard(
                title = "Estatisticas",
                subtitle = "Progressao por exercicio e por musculo",
                body = "A leitura por musculo usa volume e frequencia para evitar conclusoes artificiais quando os exercicios mudam entre blocos.",
            )
        }
        item {
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Exercicio", fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        exerciseNames.forEach { name ->
                            FilterChip(selected = selectedExercise == name, onClick = { selectedExercise = name }, label = { Text(name) })
                        }
                    }
                    MetricRow(
                        primary = "Melhor carga",
                        primaryValue = if (bestSet != null) "${bestSet.loadKg} kg" else "Sem dados",
                        secondary = "1RM estimado",
                        secondaryValue = if (oneRm > 0) "${(oneRm * 10).roundToInt() / 10.0} kg" else "Sem dados",
                    )
                    MetricRow(
                        primary = "Volume total",
                        primaryValue = "${totalVolume.roundToInt()} kg",
                        secondary = "Sessoes",
                        secondaryValue = exerciseLogs.size.toString(),
                    )
                }
            }
        }
        item {
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Musculo alvo", fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MuscleGroup.entries.forEach { muscle ->
                            FilterChip(selected = selectedMuscle == muscle, onClick = { selectedMuscle = muscle }, label = { Text(muscle.label) })
                        }
                    }
                    MetricRow(
                        primary = "Series de trabalho",
                        primaryValue = muscleSets.size.toString(),
                        secondary = "Volume total",
                        secondaryValue = "${muscleVolume.roundToInt()} kg",
                    )
                    Text("Sugestao de leitura: acompanhe consistencia de exposicao, sets de trabalho e volume agregado por musculo.")
                }
            }
        }
    }
}

private data class ExerciseEditorState(
    val blockId: String,
    val dayId: String,
    val exercise: ExerciseTemplate,
)

@Composable
private fun ExerciseEditorDialog(
    state: ExerciseEditorState,
    onDismiss: () -> Unit,
    onSave: (ExerciseTemplate) -> Unit,
) {
    var name by remember(state.exercise.id) { mutableStateOf(state.exercise.name) }
    var notes by remember(state.exercise.id) { mutableStateOf(state.exercise.notes) }
    ConfirmDialog(
        title = "Editar exercicio",
        text = "",
        onConfirm = { onSave(state.exercise.copy(name = name, notes = notes)) },
        onDismiss = onDismiss,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notas") }, modifier = Modifier.fillMaxWidth())
            }
        },
    )
}

@Composable
private fun SessionDetailDialog(
    log: WorkoutSessionLog,
    onDismiss: () -> Unit,
) {
    ConfirmDialog(
        title = "${log.dayName} - ${log.date}",
        text = "",
        onConfirm = onDismiss,
        onDismiss = onDismiss,
        confirmLabel = "Fechar",
        showDismiss = false,
        content = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(log.exerciseLogs) { exercise ->
                    Card {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(exercise.exerciseName, fontWeight = FontWeight.SemiBold)
                            exercise.loggedSets.forEach { set ->
                                Text(
                                    "${set.setKind.name} ${set.setNumber}: ${set.reps} reps x ${set.loadKg} kg" +
                                        (set.rpe?.let { " - RPE $it" } ?: ""),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        },
    )
}
