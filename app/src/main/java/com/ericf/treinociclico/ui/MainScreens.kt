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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ericf.treinociclico.data.SamplePrograms
import com.ericf.treinociclico.data.model.AdvancedTechnique
import com.ericf.treinociclico.data.model.ExerciseTemplate
import com.ericf.treinociclico.data.model.GeneralWarmup
import com.ericf.treinociclico.data.model.MuscleGroup
import com.ericf.treinociclico.data.model.ProgramBlock
import com.ericf.treinociclico.data.model.ScheduledWorkout
import com.ericf.treinociclico.data.model.SetKind
import com.ericf.treinociclico.data.model.TrainingProgram
import com.ericf.treinociclico.data.model.WarmupSetTarget
import com.ericf.treinociclico.data.model.WorkSetTarget
import com.ericf.treinociclico.data.model.WorkoutDayTemplate
import com.ericf.treinociclico.data.model.WorkoutSessionLog
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
internal fun TodayScreen(
    contentPadding: PaddingValues,
    scheduled: ScheduledWorkout,
    completedToday: WorkoutSessionLog?,
    hasDraft: Boolean,
    onStart: () -> Unit,
    onSkip: () -> Unit,
    onEditCompletedToday: (WorkoutSessionLog) -> Unit,
) {
    var showSkipConfirm by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (completedToday != null) {
            item {
                HeroCard(
                    title = "Treino concluído hoje",
                    subtitle = "${completedToday.dayName} - ${completedToday.blockName}",
                    body = "Os resultados de hoje ficam em destaque até a virada do dia. Amanhã o app volta a mostrar a próxima sessão.",
                )
            }
            item {
                MetricRow(
                    primary = "Séries de trabalho",
                    primaryValue = completedToday.exerciseLogs.sumOf { log -> log.loggedSets.count { it.setKind == SetKind.WORK } }.toString(),
                    secondary = "Exercícios",
                    secondaryValue = completedToday.exerciseLogs.size.toString(),
                )
            }
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Resumo da sessão", fontWeight = FontWeight.SemiBold)
                        completedToday.exerciseLogs.forEach { exercise ->
                            Text(exercise.exerciseName, fontWeight = FontWeight.Medium)
                            exercise.loggedSets.filter { it.setKind == SetKind.WORK }.forEach { set ->
                                Text(formatLoggedSetSummary(set), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
            item {
                OutlinedButton(onClick = { showEditDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Editar treino de hoje", modifier = Modifier.padding(start = 8.dp))
                }
            }
        } else {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.FitnessCenter,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(10.dp).size(22.dp),
                                )
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(if (hasDraft) "Treino em andamento" else "Treino de hoje", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text("${scheduled.day.name} • ${scheduled.block.name}")
                            }
                        }
                        Text("Sessão ${scheduled.dayIndex + 1} de ${scheduled.block.days.size} • repetição ${scheduled.blockIteration}/${scheduled.block.repeatCount}")
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Próximo exercício", style = MaterialTheme.typography.labelMedium)
                                Text(scheduled.day.exercises.firstOrNull()?.name ?: "Sem exercícios", fontWeight = FontWeight.SemiBold)
                            }
                        }
                        FilledTonalButton(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                            Icon(
                                imageVector = if (hasDraft) Icons.Outlined.PlayArrow else Icons.Outlined.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(if (hasDraft) "Continuar treino" else "Começar treino", modifier = Modifier.padding(start = 8.dp))
                        }
                        OutlinedButton(onClick = { showSkipConfirm = true }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Outlined.FastForward, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Pular treino", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard("Sessões concluídas", scheduled.completedSessionsOverall.toString(), Modifier.weight(1f))
                    MetricCard("Exercícios hoje", scheduled.day.exercises.size.toString(), Modifier.weight(1f))
                }
            }
            item { SessionOutlineCard(scheduled = scheduled) }
        }
    }

    if (showSkipConfirm) {
        ConfirmDialog(
            title = "Pular treino?",
            text = "Pular deve ser exceção. Quer mesmo marcar esta sessão como pulada e avançar para a próxima?",
            onConfirm = {
                onSkip()
                showSkipConfirm = false
            },
            onDismiss = { showSkipConfirm = false },
        )
    }

    if (showEditDialog && completedToday != null) {
        EditTodaySessionDialog(
            session = completedToday,
            onDismiss = { showEditDialog = false },
            onSave = {
                onEditCompletedToday(it)
                showEditDialog = false
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PlanScreen(
    contentPadding: PaddingValues,
    program: TrainingProgram,
    logs: List<WorkoutSessionLog>,
    scheduleStartIndex: Int,
    onProgramChange: (TrainingProgram) -> Unit,
    onScheduleStartChange: (Int) -> Unit,
) {
    val scheduled = remember(logs.size, program, scheduleStartIndex) {
        SamplePrograms.computeScheduledWorkout(program, logs, scheduleStartIndex)
    }
    val scheduleSlots = remember(program) { SamplePrograms.buildScheduleSlots(program) }
    var expandedDayKey by rememberSaveable { mutableStateOf<String?>(null) }
    var blockEditor by remember { mutableStateOf<ProgramBlock?>(null) }
    var dayEditor by remember { mutableStateOf<DayEditorState?>(null) }
    var exerciseEditor by remember { mutableStateOf<ExerciseEditorState?>(null) }
    var removeExerciseState by remember { mutableStateOf<ExerciseEditorState?>(null) }
    var removeBlockId by remember { mutableStateOf<String?>(null) }
    var removeDayState by remember { mutableStateOf<DayEditorState?>(null) }
    var showStartPointDialog by remember { mutableStateOf(false) }

    androidx.compose.foundation.lazy.LazyColumn(
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
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Ponto inicial do ciclo", fontWeight = FontWeight.SemiBold)
                    Text("Atual: ${scheduled.block.name} • repetição ${scheduled.blockIteration} • ${scheduled.day.name}")
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { showStartPointDialog = true }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Escolher início", modifier = Modifier.padding(start = 8.dp))
                        }
                        OutlinedButton(
                            onClick = { onProgramChange(addBlockToProgram(program)) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Novo bloco", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        }
        items(program.blocks.size) { blockIndex ->
            val block = program.blocks[blockIndex]
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(block.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Repeticoes do bloco: ${block.repeatCount}")
                            Text("Sessões por volta: ${block.days.size}")
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { blockEditor = block }) {
                                Icon(Icons.Outlined.Edit, contentDescription = "Editar bloco")
                            }
                            IconButton(
                                onClick = { onProgramChange(moveBlock(program, blockIndex, -1)) },
                                enabled = blockIndex > 0,
                            ) {
                                Icon(Icons.Outlined.ArrowUpward, contentDescription = "Subir bloco")
                            }
                            IconButton(
                                onClick = { onProgramChange(moveBlock(program, blockIndex, 1)) },
                                enabled = blockIndex < program.blocks.lastIndex,
                            ) {
                                Icon(Icons.Outlined.ArrowDownward, contentDescription = "Descer bloco")
                            }
                            IconButton(onClick = { removeBlockId = block.id }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Remover bloco")
                            }
                        }
                    }
                    block.days.forEachIndexed { index, day ->
                        val dayKey = "${block.id}:${day.id}"
                        val isNext = scheduled.block.id == block.id && scheduled.day.id == day.id
                        val expanded = expandedDayKey == dayKey
                        Card(
                            modifier = Modifier.clickable {
                                expandedDayKey = if (expanded) null else dayKey
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isNext) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("${index + 1}. ${day.name}", fontWeight = FontWeight.Medium)
                                Text(
                                    "${day.exercises.size} exercícios - toque para ${if (expanded) "fechar" else "editar"}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                if (expanded) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        OutlinedButton(
                                            onClick = { dayEditor = DayEditorState(block.id, day) },
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Text("Editar", modifier = Modifier.padding(start = 8.dp))
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                val newExercise = defaultExercise(day.exercises.size + 1)
                                                onProgramChange(addExerciseToDay(program, block.id, day.id, newExercise))
                                            },
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Text("Exercício", modifier = Modifier.padding(start = 8.dp))
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        OutlinedButton(
                                            onClick = { onProgramChange(addDayToBlock(program, block.id)) },
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Text("Sessão", modifier = Modifier.padding(start = 8.dp))
                                        }
                                        OutlinedButton(
                                            onClick = { removeDayState = DayEditorState(block.id, day) },
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Text("Remover", modifier = Modifier.padding(start = 8.dp))
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("Organizar sessão", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                                        IconButton(onClick = { onProgramChange(moveDay(program, block.id, index, -1)) }, enabled = index > 0) {
                                            Icon(Icons.Outlined.ArrowUpward, contentDescription = "Subir sessão")
                                        }
                                        IconButton(onClick = { onProgramChange(moveDay(program, block.id, index, 1)) }, enabled = index < block.days.lastIndex) {
                                            Icon(Icons.Outlined.ArrowDownward, contentDescription = "Descer sessão")
                                        }
                                    }
                                    day.exercises.forEach { exercise ->
                                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text(exercise.name, fontWeight = FontWeight.SemiBold)
                                                Text(
                                                    "${exercise.warmupSets.size} aquecimentos • ${exercise.workSets.size} séries de trabalho",
                                                    style = MaterialTheme.typography.bodySmall,
                                                )
                                                if (exercise.notes.isNotBlank()) {
                                                    Text(exercise.notes, style = MaterialTheme.typography.bodySmall)
                                                }
                                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    exercise.muscles.forEach { muscle ->
                                                        FilterChip(selected = true, onClick = {}, label = { Text(muscle.label) })
                                                    }
                                                }
                                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                    OutlinedButton(
                                                        onClick = { exerciseEditor = ExerciseEditorState(block.id, day.id, exercise) },
                                                        modifier = Modifier.weight(1f),
                                                    ) {
                                                        Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                                        Text("Editar", modifier = Modifier.padding(start = 8.dp))
                                                    }
                                                    OutlinedButton(
                                                        onClick = { removeExerciseState = ExerciseEditorState(block.id, day.id, exercise) },
                                                        modifier = Modifier.weight(1f),
                                                    ) {
                                                        Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                                        Text("Remover", modifier = Modifier.padding(start = 8.dp))
                                                    }
                                                }
                                                val exerciseIndex = day.exercises.indexOfFirst { it.id == exercise.id }
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Text("Mover", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                                                    IconButton(
                                                        onClick = { onProgramChange(moveExercise(program, block.id, day.id, exerciseIndex, -1)) },
                                                        enabled = exerciseIndex > 0,
                                                    ) {
                                                        Icon(Icons.Outlined.ArrowUpward, contentDescription = "Subir exercício")
                                                    }
                                                    IconButton(
                                                        onClick = { onProgramChange(moveExercise(program, block.id, day.id, exerciseIndex, 1)) },
                                                        enabled = exerciseIndex < day.exercises.lastIndex,
                                                    ) {
                                                        Icon(Icons.Outlined.ArrowDownward, contentDescription = "Descer exercício")
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
    }

    if (showStartPointDialog) {
        StartPointDialog(
            slots = scheduleSlots,
            selectedIndex = scheduleStartIndex,
            onDismiss = { showStartPointDialog = false },
            onSelect = {
                onScheduleStartChange(it)
                showStartPointDialog = false
            },
        )
    }

    blockEditor?.let { block ->
        BlockEditorDialog(
            block = block,
            onDismiss = { blockEditor = null },
            onSave = {
                onProgramChange(updateBlockInProgram(program, it))
                blockEditor = null
            },
        )
    }

    dayEditor?.let { state ->
        DayEditorDialog(
            day = state.day,
            onDismiss = { dayEditor = null },
            onSave = {
                onProgramChange(updateDayInProgram(program, state.blockId, it))
                dayEditor = null
            },
        )
    }

    exerciseEditor?.let { state ->
        ExerciseEditorDialog(
            state = state,
            onDismiss = { exerciseEditor = null },
            onSave = { updated ->
                onProgramChange(
                    updateExerciseInProgram(program, state.blockId, state.dayId, state.exercise.id) {
                        updated.copy(id = state.exercise.id)
                    },
                )
                exerciseEditor = null
            },
        )
    }

    removeExerciseState?.let { state ->
        ConfirmDialog(
            title = "Remover exercício?",
            text = "O exercício ${state.exercise.name} será removido deste treino.",
            onConfirm = {
                onProgramChange(removeExerciseFromDay(program, state.blockId, state.dayId, state.exercise.id))
                removeExerciseState = null
            },
            onDismiss = { removeExerciseState = null },
            confirmLabel = "Remover",
        )
    }

    removeBlockId?.let { blockId ->
        ConfirmDialog(
            title = "Remover bloco?",
            text = "Esse bloco será removido do plano.",
            onConfirm = {
                onProgramChange(removeBlock(program, blockId))
                removeBlockId = null
            },
            onDismiss = { removeBlockId = null },
            confirmLabel = "Remover",
        )
    }

    removeDayState?.let { state ->
        ConfirmDialog(
            title = "Remover sessão?",
            text = "A sessão ${state.day.name} será removida deste bloco.",
            onConfirm = {
                onProgramChange(removeDay(program, state.blockId, state.day.id))
                removeDayState = null
            },
            onDismiss = { removeDayState = null },
            confirmLabel = "Remover",
        )
    }
}

@Composable
internal fun HistoryScreen(
    contentPadding: PaddingValues,
    logs: List<WorkoutSessionLog>,
) {
    var visibleMonthText by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val visibleMonth = remember(visibleMonthText) { YearMonth.parse(visibleMonthText) }
    val days = visibleMonth.lengthOfMonth()
    val firstOffset = visibleMonth.atDay(1).dayOfWeek.value % 7
    val trainedDays = remember(logs) { logs.groupBy { it.date } }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            HeroCard(
                title = "Histórico",
                subtitle = "Calendário de sessões",
                body = "Navegue entre os meses e toque em um dia para abrir todos os detalhes do que foi feito naquela data.",
            )
        }
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(onClick = { visibleMonthText = visibleMonth.minusMonths(1).toString() }) {
                            Text("<")
                        }
                        Text(
                            "${visibleMonth.month.getDisplayName(TextStyle.FULL, Locale("pt", "BR")).replaceFirstChar { it.uppercase() }} ${visibleMonth.year}",
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.SemiBold,
                        )
                        OutlinedButton(onClick = { visibleMonthText = visibleMonth.plusMonths(1).toString() }) {
                            Text(">")
                        }
                    }
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
                                val date = if (dayNumber in 1..days) visibleMonth.atDay(dayNumber) else null
                                CalendarCell(date = date, logs = date?.let { trainedDays[it].orEmpty() }.orEmpty(), onClick = { selectedDate = it })
                            }
                        }
                    }
                }
            }
        }
        items(logs.sortedByDescending { it.date }.size) { index ->
            val log = logs.sortedByDescending { it.date }[index]
            Card(modifier = Modifier.clickable { selectedDate = log.date }) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${log.date} - ${log.dayName}", fontWeight = FontWeight.SemiBold)
                    Text("${log.blockName} - repetição ${log.blockIteration}")
                    Text(
                        if (log.wasSkipped) "Sessão pulada"
                        else "${log.exerciseLogs.sumOf { it.loggedSets.count { set -> set.setKind == SetKind.WORK } }} séries de trabalho registradas",
                    )
                }
            }
        }
    }

    selectedDate?.let { date ->
        DayDetailDialog(date = date, logs = trainedDays[date].orEmpty(), onDismiss = { selectedDate = null })
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
    var selectedExerciseMetric by rememberSaveable { mutableStateOf("Carga máxima") }
    var rpeScope by rememberSaveable { mutableStateOf("Geral") }

    val exerciseLogs = logs.flatMap { it.exerciseLogs }.filter { it.exerciseName == selectedExercise }
    val exerciseSets = exerciseLogs.flatMap { it.loggedSets }.filter { it.setKind == SetKind.WORK }
    val completeExerciseSets = exerciseSets.filter { it.loadKg != null && it.reps != null }
    val bestSet = completeExerciseSets.maxByOrNull { it.loadKg ?: 0.0 }
    val totalVolume = completeExerciseSets.sumOf { (it.loadKg ?: 0.0) * (it.reps ?: 0) }
    val oneRm = bestSet?.let { (it.loadKg ?: 0.0) * (1.0 + (it.reps ?: 0) / 30.0) } ?: 0.0
    val maxLoadPoints = logs.mapNotNull { log ->
        val sets = log.exerciseLogs
            .firstOrNull { it.exerciseName == selectedExercise }
            ?.loggedSets
            ?.filter { it.setKind == SetKind.WORK && it.loadKg != null }
            .orEmpty()
        val maxLoad = sets.maxOfOrNull { it.loadKg ?: 0.0 } ?: return@mapNotNull null
        ChartPoint(log.date, maxLoad)
    }
    val avgVolumePoints = logs.mapNotNull { log ->
        val sets = log.exerciseLogs
            .firstOrNull { it.exerciseName == selectedExercise }
            ?.loggedSets
            ?.filter { it.setKind == SetKind.WORK && it.loadKg != null && it.reps != null }
            .orEmpty()
        if (sets.isEmpty()) return@mapNotNull null
        ChartPoint(log.date, sets.sumOf { (it.loadKg ?: 0.0) * (it.reps ?: 0) } / sets.size)
    }

    val muscleLogs = logs.flatMap { it.exerciseLogs }.filter { selectedMuscle != null && it.muscles.contains(selectedMuscle) }
    val muscleSets = muscleLogs.flatMap { it.loggedSets }.filter { it.setKind == SetKind.WORK }
    val muscleVolume = muscleSets.sumOf { (it.loadKg ?: 0.0) * (it.reps ?: 0) }
    val rpePoints = logs.mapNotNull { log ->
        val sets = when (rpeScope) {
            "Exercício" -> log.exerciseLogs
                .firstOrNull { it.exerciseName == selectedExercise }
                ?.loggedSets
                ?.filter { it.setKind == SetKind.WORK && it.rpe != null }
                .orEmpty()

            else -> log.exerciseLogs.flatMap { it.loggedSets }.filter { it.setKind == SetKind.WORK && it.rpe != null }
        }
        if (sets.isEmpty()) null else ChartPoint(log.date, sets.mapNotNull { it.rpe }.average())
    }

    androidx.compose.foundation.lazy.LazyColumn(
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
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Carga máxima", "Volume médio").forEach { metric ->
                            FilterChip(
                                selected = selectedExerciseMetric == metric,
                                onClick = { selectedExerciseMetric = metric },
                                label = { Text(metric) },
                            )
                        }
                    }
                }
            }
        }
        item {
            LineChartCard(
                title = "Evolução de ${selectedExercise ?: "exercício"}",
                subtitle = if (selectedExerciseMetric == "Carga máxima") "Maior carga registrada em cada sessão" else "Volume médio por série em cada sessão",
                points = if (selectedExerciseMetric == "Carga máxima") maxLoadPoints else avgVolumePoints,
            )
        }
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Percepção de esforço", fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Geral", "Exercício").forEach { scope ->
                            FilterChip(selected = rpeScope == scope, onClick = { rpeScope = scope }, label = { Text(scope) })
                        }
                    }
                }
            }
        }
        item {
            LineChartCard(
                title = "RPE por dia",
                subtitle = if (rpeScope == "Geral") "Média das séries de trabalho em cada sessão" else "Média de RPE do exercício selecionado",
                points = rpePoints,
            )
        }
    }
}

private data class DayEditorState(
    val blockId: String,
    val day: WorkoutDayTemplate,
)

private data class ExerciseEditorState(
    val blockId: String,
    val dayId: String,
    val exercise: ExerciseTemplate,
)

@Composable
private fun StartPointDialog(
    slots: List<ScheduledWorkout>,
    selectedIndex: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    ConfirmDialog(
        title = "Escolher ponto inicial",
        text = "",
        onConfirm = onDismiss,
        onDismiss = onDismiss,
        confirmLabel = "Fechar",
        showDismiss = false,
        content = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                slots.forEachIndexed { index, slot ->
                    Card(
                        modifier = Modifier.clickable { onSelect(index) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (index == selectedIndex) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${slot.block.name} • repeticao ${slot.blockIteration}", fontWeight = FontWeight.SemiBold)
                            Text("Sessao ${slot.dayIndex + 1}: ${slot.day.name}")
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun BlockEditorDialog(
    block: ProgramBlock,
    onDismiss: () -> Unit,
    onSave: (ProgramBlock) -> Unit,
) {
    var name by remember(block.id) { mutableStateOf(block.name) }
    var repeatCount by remember(block.id) { mutableStateOf(block.repeatCount.toString()) }
    ConfirmDialog(
        title = "Editar bloco",
        text = "",
        onConfirm = {
            onSave(
                block.copy(
                    name = name.trim().ifBlank { block.name },
                    repeatCount = repeatCount.toIntOrNull()?.coerceAtLeast(1) ?: block.repeatCount,
                ),
            )
        },
        onDismiss = onDismiss,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome do bloco") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = repeatCount,
                    onValueChange = { repeatCount = it.filter(Char::isDigit) },
                    label = { Text("Repeticoes") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    )
}

@Composable
private fun DayEditorDialog(
    day: WorkoutDayTemplate,
    onDismiss: () -> Unit,
    onSave: (WorkoutDayTemplate) -> Unit,
) {
    var name by remember(day.id) { mutableStateOf(day.name) }
    var warmupTitle by remember(day.id) { mutableStateOf(day.generalWarmup?.title.orEmpty()) }
    var warmupMinutes by remember(day.id) { mutableStateOf(day.generalWarmup?.durationMinutes?.toString().orEmpty()) }
    ConfirmDialog(
        title = "Editar dia",
        text = "",
        onConfirm = {
            val updatedWarmup = if (warmupTitle.isBlank()) {
                null
            } else {
                GeneralWarmup(
                    title = warmupTitle.trim(),
                    durationMinutes = warmupMinutes.toIntOrNull() ?: day.generalWarmup?.durationMinutes ?: 5,
                )
            }
            onSave(day.copy(name = name.trim().ifBlank { day.name }, generalWarmup = updatedWarmup))
        },
        onDismiss = onDismiss,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome do dia") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = warmupTitle, onValueChange = { warmupTitle = it }, label = { Text("Aquecimento geral") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = warmupMinutes,
                    onValueChange = { warmupMinutes = it.filter(Char::isDigit) },
                    label = { Text("Duracao do aquecimento (min)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseEditorDialog(
    state: ExerciseEditorState,
    onDismiss: () -> Unit,
    onSave: (ExerciseTemplate) -> Unit,
) {
    var name by remember(state.exercise.id) { mutableStateOf(state.exercise.name) }
    var notes by remember(state.exercise.id) { mutableStateOf(state.exercise.notes) }
    var selectedMuscles by remember(state.exercise.id) { mutableStateOf(state.exercise.muscles) }
    var warmupSets by remember(state.exercise.id) { mutableStateOf(state.exercise.warmupSets) }
    var workSets by remember(state.exercise.id) { mutableStateOf(state.exercise.workSets) }

    ConfirmDialog(
        title = "Editar exercicio",
        text = "",
        onConfirm = {
            onSave(
                state.exercise.copy(
                    name = name.trim().ifBlank { state.exercise.name },
                    notes = notes.trim(),
                    muscles = if (selectedMuscles.isEmpty()) state.exercise.muscles else selectedMuscles,
                    warmupSets = warmupSets,
                    workSets = workSets,
                ),
            )
        },
        onDismiss = onDismiss,
        content = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notas") }, modifier = Modifier.fillMaxWidth())
                Text("Musculos alvo", fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MuscleGroup.entries.forEach { muscle ->
                        FilterChip(
                            selected = selectedMuscles.contains(muscle),
                            onClick = {
                                selectedMuscles = if (selectedMuscles.contains(muscle)) selectedMuscles - muscle else selectedMuscles + muscle
                            },
                            label = { Text(muscle.label) },
                        )
                    }
                }
                Text("Series de aquecimento", fontWeight = FontWeight.SemiBold)
                warmupSets.forEachIndexed { index, target ->
                    WarmupTargetEditor(
                        index = index,
                        total = warmupSets.size,
                        target = target,
                        onChange = { updated ->
                            warmupSets = warmupSets.toMutableList().also { list -> list[index] = updated }
                        },
                        onMove = { delta ->
                            warmupSets = warmupSets.moveItem(index, delta)
                        },
                        onRemove = {
                            warmupSets = warmupSets.toMutableList().also { list -> list.removeAt(index) }
                        },
                    )
                }
                OutlinedButton(
                    onClick = { warmupSets = warmupSets + WarmupSetTarget(targetReps = 8, suggestedLoadKg = null, label = "") },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Adicionar aquecimento")
                }
                Text("Series de trabalho", fontWeight = FontWeight.SemiBold)
                workSets.forEachIndexed { index, target ->
                    WorkSetTargetEditor(
                        index = index,
                        total = workSets.size,
                        target = target,
                        onChange = { updated ->
                            workSets = workSets.toMutableList().also { list -> list[index] = updated }
                        },
                        onMove = { delta ->
                            workSets = workSets.moveItem(index, delta)
                        },
                        onRemove = {
                            workSets = workSets.toMutableList().also { list -> list.removeAt(index) }
                        },
                    )
                }
                OutlinedButton(
                    onClick = {
                        workSets = workSets + WorkSetTarget(
                            targetReps = 8,
                            targetRepLabel = "8-10",
                            targetRpe = 8.0,
                            targetRpeLabel = "~8",
                            restSeconds = 120,
                            restLabel = "2 min",
                            techniqueHint = null,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Adicionar serie de trabalho")
                }
            }
        },
    )
}

@Composable
private fun WarmupTargetEditor(
    index: Int,
    total: Int,
    target: WarmupSetTarget,
    onChange: (WarmupSetTarget) -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    var reps by remember(target) { mutableStateOf(target.targetReps.toString()) }
    var label by remember(target) { mutableStateOf(target.label.orEmpty()) }
    Card {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Aquecimento ${index + 1}", fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = reps,
                onValueChange = {
                    reps = it.filter(Char::isDigit)
                    onChange(target.copy(targetReps = reps.toIntOrNull() ?: target.targetReps, label = label))
                },
                label = { Text("Repeticoes") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = label,
                onValueChange = {
                    label = it
                    onChange(target.copy(targetReps = reps.toIntOrNull() ?: target.targetReps, label = it))
                },
                label = { Text("Label") },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { onMove(-1) }, enabled = index > 0, modifier = Modifier.weight(1f)) { Text("Subir") }
                OutlinedButton(onClick = { onMove(1) }, enabled = index < total - 1, modifier = Modifier.weight(1f)) { Text("Descer") }
            }
            OutlinedButton(onClick = onRemove, modifier = Modifier.fillMaxWidth()) {
                Text("Remover aquecimento")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkSetTargetEditor(
    index: Int,
    total: Int,
    target: WorkSetTarget,
    onChange: (WorkSetTarget) -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    var repLabel by remember(target) { mutableStateOf(target.targetRepLabel) }
    var reps by remember(target) { mutableStateOf(target.targetReps.toString()) }
    var rpeLabel by remember(target) { mutableStateOf(target.targetRpeLabel.orEmpty()) }
    var rpeValue by remember(target) { mutableStateOf(target.targetRpe?.toString().orEmpty()) }
    var restLabel by remember(target) { mutableStateOf(target.restLabel.orEmpty()) }
    var restSeconds by remember(target) { mutableStateOf(target.restSeconds?.toString().orEmpty()) }
    var technique by remember(target) { mutableStateOf(target.techniqueHint) }

    fun buildTarget() = target.copy(
        targetReps = reps.toIntOrNull() ?: target.targetReps,
        targetRepLabel = repLabel,
        targetRpe = rpeValue.replace(',', '.').toDoubleOrNull(),
        targetRpeLabel = rpeLabel.ifBlank { null },
        restSeconds = restSeconds.toIntOrNull(),
        restLabel = restLabel.ifBlank { null },
        techniqueHint = technique,
    )

    Card {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Serie de trabalho ${index + 1}", fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = repLabel,
                onValueChange = {
                    repLabel = it
                    onChange(buildTarget())
                },
                label = { Text("Rep range") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = reps,
                onValueChange = {
                    reps = it.filter(Char::isDigit)
                    onChange(buildTarget())
                },
                label = { Text("Reps alvo numerico") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = rpeLabel,
                onValueChange = {
                    rpeLabel = it
                    onChange(buildTarget())
                },
                label = { Text("RPE label") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = rpeValue,
                onValueChange = {
                    rpeValue = filterDecimalText(it)
                    onChange(buildTarget())
                },
                label = { Text("RPE numerico") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = restLabel,
                onValueChange = {
                    restLabel = it
                    onChange(buildTarget())
                },
                label = { Text("Descanso label") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = restSeconds,
                onValueChange = {
                    restSeconds = it.filter(Char::isDigit)
                    onChange(buildTarget())
                },
                label = { Text("Descanso em segundos") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = technique == null, onClick = {
                    technique = null
                    onChange(buildTarget())
                }, label = { Text("Sem tecnica") })
                AdvancedTechnique.entries.forEach { option ->
                    FilterChip(selected = technique == option, onClick = {
                        technique = option
                        onChange(buildTarget())
                    }, label = { Text(option.label) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { onMove(-1) }, enabled = index > 0, modifier = Modifier.weight(1f)) { Text("Subir") }
                OutlinedButton(onClick = { onMove(1) }, enabled = index < total - 1, modifier = Modifier.weight(1f)) { Text("Descer") }
            }
            OutlinedButton(onClick = onRemove, modifier = Modifier.fillMaxWidth()) {
                Text("Remover serie")
            }
        }
    }
}

@Composable
private fun DayDetailDialog(
    date: LocalDate,
    logs: List<WorkoutSessionLog>,
    onDismiss: () -> Unit,
) {
    ConfirmDialog(
        title = "Sessoes de $date",
        text = "",
        onConfirm = onDismiss,
        onDismiss = onDismiss,
        confirmLabel = "Fechar",
        showDismiss = false,
        content = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (logs.isEmpty()) {
                    Text("Nenhuma sessao encontrada.")
                } else {
                    logs.forEach { log ->
                        Card {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("${log.dayName} • ${log.blockName}", fontWeight = FontWeight.SemiBold)
                                Text("Repeticao ${log.blockIteration}")
                                Text(if (log.wasSkipped) "Sessao pulada" else "Treino registrado")
                                if (!log.wasSkipped) {
                                    log.exerciseLogs.forEach { exercise ->
                                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(exercise.exerciseName, fontWeight = FontWeight.Medium)
                                                exercise.loggedSets.forEach { set ->
                                                    Text(
                                                        "${setKindLabel(set.setKind)} ${set.setNumber}: ${set.reps} reps x ${set.loadKg} kg" +
                                                            (set.restSeconds?.let { " • descanso ${it}s" } ?: "") +
                                                            (set.rpe?.let { " • RPE $it" } ?: "") +
                                                            (set.advancedTechnique?.let { " • ${it.label}" } ?: ""),
                                                        style = MaterialTheme.typography.bodySmall,
                                                    )
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
        },
    )
}

@Composable
private fun EditTodaySessionDialog(
    session: WorkoutSessionLog,
    onDismiss: () -> Unit,
    onSave: (WorkoutSessionLog) -> Unit,
) {
    var editableSession by remember(session.id) { mutableStateOf(session) }
    ConfirmDialog(
        title = "Editar treino de hoje",
        text = "",
        onConfirm = { onSave(editableSession) },
        onDismiss = onDismiss,
        confirmLabel = "Salvar",
        content = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                editableSession.exerciseLogs.forEachIndexed { exerciseIndex, exercise ->
                    Card {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(exercise.exerciseName, fontWeight = FontWeight.SemiBold)
                            exercise.loggedSets.filter { it.setKind == SetKind.WORK }.forEachIndexed { setIndex, set ->
                                var repsText by remember(session.id, exercise.exerciseName, set.setNumber) {
                                    mutableStateOf(set.reps?.toString().orEmpty())
                                }
                                var loadText by remember(session.id, exercise.exerciseName, set.setNumber, "load") {
                                    mutableStateOf(set.loadKg?.toString().orEmpty())
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Set ${set.setNumber}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        OutlinedTextField(
                                            value = repsText,
                                            onValueChange = {
                                                repsText = it.filter(Char::isDigit)
                                                editableSession = editableSession.updateSet(
                                                    exerciseIndex = exerciseIndex,
                                                    setIndex = setIndex,
                                                    transform = { current -> current.copy(reps = repsText.toIntOrNull()) },
                                                )
                                            },
                                            label = { Text("Reps") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                        )
                                        OutlinedTextField(
                                            value = loadText,
                                            onValueChange = {
                                                loadText = filterDecimalText(it)
                                                editableSession = editableSession.updateSet(
                                                    exerciseIndex = exerciseIndex,
                                                    setIndex = setIndex,
                                                    transform = { current -> current.copy(loadKg = loadText.replace(',', '.').toDoubleOrNull()) },
                                                )
                                            },
                                            label = { Text("Carga") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                    Text(formatLoggedSetSummary(set.copy(reps = repsText.toIntOrNull(), loadKg = loadText.replace(',', '.').toDoubleOrNull())), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        },
    )
}

private fun setKindLabel(kind: SetKind): String = when (kind) {
    SetKind.GENERAL_WARMUP -> "Aquecimento geral"
    SetKind.EXERCISE_WARMUP -> "Aquecimento"
    SetKind.WORK -> "Trabalho"
}

private fun formatLoggedSetSummary(set: com.ericf.treinociclico.data.model.LoggedSet): String = when {
    set.reps == null && set.loadKg == null -> "Set ${set.setNumber}  ⚠ incompleto"
    set.reps == null -> "Set ${set.setNumber}  ⚠ sem reps"
    set.loadKg == null -> "Set ${set.setNumber}  ⚠ sem carga"
    else -> "Set ${set.setNumber}  ${set.reps} reps - ${set.loadKg}kg"
}

private fun WorkoutSessionLog.updateSet(
    exerciseIndex: Int,
    setIndex: Int,
    transform: (com.ericf.treinociclico.data.model.LoggedSet) -> com.ericf.treinociclico.data.model.LoggedSet,
): WorkoutSessionLog = copy(
    exerciseLogs = exerciseLogs.mapIndexed { currentExerciseIndex, exercise ->
        if (currentExerciseIndex != exerciseIndex) exercise
        else exercise.copy(
            loggedSets = exercise.loggedSets.filter { it.setKind != SetKind.WORK } +
                exercise.loggedSets.filter { it.setKind == SetKind.WORK }.mapIndexed { currentSetIndex, loggedSet ->
                    if (currentSetIndex == setIndex) transform(loggedSet) else loggedSet
                },
        )
    },
)

private fun defaultExercise(index: Int): ExerciseTemplate = ExerciseTemplate(
    id = "custom-exercise-$index",
    name = "Novo exercicio $index",
    muscles = setOf(MuscleGroup.CHEST),
    warmupSets = listOf(WarmupSetTarget(targetReps = 8, suggestedLoadKg = null, label = "50% x 8")),
    workSets = listOf(
        WorkSetTarget(
            targetReps = 8,
            targetRepLabel = "8-10",
            targetRpe = 8.0,
            targetRpeLabel = "~8",
            restSeconds = 120,
            restLabel = "2 min",
            techniqueHint = null,
        ),
    ),
    notes = "",
)

private fun updateBlockInProgram(program: TrainingProgram, updatedBlock: ProgramBlock): TrainingProgram = program.copy(
    blocks = program.blocks.map { block -> if (block.id == updatedBlock.id) updatedBlock else block },
)

private fun addBlockToProgram(program: TrainingProgram): TrainingProgram {
    val nextIndex = program.blocks.size + 1
    val block = ProgramBlock(
        id = "custom-block-$nextIndex",
        name = "Novo bloco $nextIndex",
        repeatCount = 1,
        days = listOf(defaultDay(1)),
    )
    return program.copy(blocks = program.blocks + block)
}

private fun moveBlock(program: TrainingProgram, index: Int, delta: Int): TrainingProgram {
    val targetIndex = (index + delta).coerceIn(0, program.blocks.lastIndex)
    if (index == targetIndex) return program
    val blocks = program.blocks.toMutableList()
    val block = blocks.removeAt(index)
    blocks.add(targetIndex, block)
    return program.copy(blocks = blocks)
}

private fun removeBlock(program: TrainingProgram, blockId: String): TrainingProgram {
    val filtered = program.blocks.filterNot { it.id == blockId }
    return program.copy(blocks = if (filtered.isEmpty()) listOf(ProgramBlock("custom-block-1", "Novo bloco 1", 1, listOf(defaultDay(1)))) else filtered)
}

private fun updateDayInProgram(program: TrainingProgram, blockId: String, updatedDay: WorkoutDayTemplate): TrainingProgram = program.copy(
    blocks = program.blocks.map { block ->
        if (block.id != blockId) block
        else block.copy(days = block.days.map { day -> if (day.id == updatedDay.id) updatedDay else day })
    },
)

private fun addDayToBlock(program: TrainingProgram, blockId: String): TrainingProgram = program.copy(
    blocks = program.blocks.map { block ->
        if (block.id != blockId) block
        else block.copy(days = block.days + defaultDay(block.days.size + 1))
    },
)

private fun moveDay(program: TrainingProgram, blockId: String, index: Int, delta: Int): TrainingProgram = program.copy(
    blocks = program.blocks.map { block ->
        if (block.id != blockId) block
        else {
            val targetIndex = (index + delta).coerceIn(0, block.days.lastIndex)
            if (index == targetIndex) block
            else {
                val days = block.days.toMutableList()
                val day = days.removeAt(index)
                days.add(targetIndex, day)
                block.copy(days = days)
            }
        }
    },
)

private fun removeDay(program: TrainingProgram, blockId: String, dayId: String): TrainingProgram = program.copy(
    blocks = program.blocks.map { block ->
        if (block.id != blockId) block
        else {
            val filtered = block.days.filterNot { it.id == dayId }
            block.copy(days = if (filtered.isEmpty()) listOf(defaultDay(1)) else filtered)
        }
    },
)

private fun addExerciseToDay(
    program: TrainingProgram,
    blockId: String,
    dayId: String,
    exercise: ExerciseTemplate,
): TrainingProgram = program.copy(
    blocks = program.blocks.map { block ->
        if (block.id != blockId) block
        else block.copy(
            days = block.days.map { day ->
                if (day.id != dayId) day else day.copy(exercises = day.exercises + exercise.copy(id = "${exercise.id}-${day.exercises.size + 1}"))
            },
        )
    },
)

private fun removeExerciseFromDay(
    program: TrainingProgram,
    blockId: String,
    dayId: String,
    exerciseId: String,
): TrainingProgram = program.copy(
    blocks = program.blocks.map { block ->
        if (block.id != blockId) block
        else block.copy(days = block.days.map { day -> if (day.id != dayId) day else day.copy(exercises = day.exercises.filterNot { it.id == exerciseId }) })
    },
)

private fun moveExercise(
    program: TrainingProgram,
    blockId: String,
    dayId: String,
    index: Int,
    delta: Int,
): TrainingProgram = program.copy(
    blocks = program.blocks.map { block ->
        if (block.id != blockId) block
        else block.copy(
            days = block.days.map { day ->
                if (day.id != dayId) day
                else {
                    val targetIndex = (index + delta).coerceIn(0, day.exercises.lastIndex)
                    if (index == targetIndex) day
                    else {
                        val exercises = day.exercises.toMutableList()
                        val exercise = exercises.removeAt(index)
                        exercises.add(targetIndex, exercise)
                        day.copy(exercises = exercises)
                    }
                }
            },
        )
    },
)

private fun defaultDay(index: Int): WorkoutDayTemplate = WorkoutDayTemplate(
    id = "custom-day-$index",
    name = "Sessão $index",
    generalWarmup = GeneralWarmup("5 min de cardio leve", 5),
    exercises = listOf(defaultExercise(1)),
)

private fun filterDecimalText(value: String): String {
    val normalized = value.replace(',', '.')
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

private fun <T> List<T>.moveItem(index: Int, delta: Int): List<T> {
    val targetIndex = (index + delta).coerceIn(0, lastIndex)
    if (index == targetIndex) return this
    val mutable = toMutableList()
    val item = mutable.removeAt(index)
    mutable.add(targetIndex, item)
    return mutable
}
