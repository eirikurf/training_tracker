package com.ericf.treinociclico.data

import com.ericf.treinociclico.data.local.AppStateEntity
import com.ericf.treinociclico.data.local.TrainingDao
import com.ericf.treinociclico.data.local.WorkoutSessionEntity
import com.ericf.treinociclico.data.model.TrainingProgram
import com.ericf.treinociclico.data.model.WorkoutDraft
import com.ericf.treinociclico.data.model.WorkoutSessionLog
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class PersistedAppState(
    val program: TrainingProgram,
    val scheduleStartIndex: Int,
    val workoutDraft: WorkoutDraft?,
    val logs: List<WorkoutSessionLog>,
)

class TrainingRepository(
    private val dao: TrainingDao,
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter)
        .create(),
) {
    val appState: Flow<PersistedAppState> = combine(
        dao.observeAppState(),
        dao.observeSessions(),
    ) { appState, sessionEntities ->
        val baseProgram = appState?.programJson?.let { gson.fromJson(it, TrainingProgram::class.java) } ?: SamplePrograms.starterProgram
        val workoutDraft = appState?.workoutDraftJson?.let { gson.fromJson(it, WorkoutDraft::class.java) }
        PersistedAppState(
            program = baseProgram,
            scheduleStartIndex = appState?.scheduleStartIndex ?: 0,
            workoutDraft = workoutDraft,
            logs = sessionEntities.map { entity ->
                runCatching { gson.fromJson(entity.sessionJson, WorkoutSessionLog::class.java) }
                    .getOrElse {
                        WorkoutSessionLog(
                            id = entity.id,
                            date = entity.date.toLocalDateOrToday(),
                            blockName = "Sessão importada",
                            blockIteration = 1,
                            dayName = "Treino",
                            wasSkipped = false,
                            generalWarmupCompleted = false,
                            exerciseLogs = emptyList(),
                        )
                    }
            },
        )
    }

    suspend fun ensureInitialized() {
        if (dao.observeAppState().first() == null) {
            dao.upsertAppState(
                AppStateEntity(
                    programJson = gson.toJson(SamplePrograms.starterProgram),
                    scheduleStartIndex = 0,
                    workoutDraftJson = null,
                ),
            )
        }
    }

    suspend fun saveProgram(program: TrainingProgram) {
        val current = currentAppState()
        dao.upsertAppState(current.copy(programJson = gson.toJson(program)))
    }

    suspend fun saveScheduleStartIndex(scheduleStartIndex: Int) {
        val current = currentAppState()
        dao.upsertAppState(current.copy(scheduleStartIndex = scheduleStartIndex))
    }

    suspend fun saveWorkoutDraft(workoutDraft: WorkoutDraft?) {
        val current = currentAppState()
        dao.upsertAppState(current.copy(workoutDraftJson = workoutDraft?.let(gson::toJson)))
    }

    suspend fun saveSessionLog(session: WorkoutSessionLog) {
        dao.upsertSession(
            WorkoutSessionEntity(
                id = session.id,
                date = session.date.toString(),
                sessionJson = gson.toJson(session),
            ),
        )
    }

    private suspend fun currentAppState(): AppStateEntity = dao.observeAppState().map { entity ->
        entity
    }.first() ?: AppStateEntity(
        programJson = gson.toJson(SamplePrograms.starterProgram),
        scheduleStartIndex = 0,
        workoutDraftJson = null,
    )
}

private object LocalDateAdapter : JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
    override fun serialize(src: LocalDate?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement =
        JsonPrimitive(src?.toString())

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): LocalDate {
        val value = json?.asString ?: throw JsonParseException("LocalDate vazio")
        return value.toLocalDateOrToday()
    }
}

private fun String.toLocalDateOrToday(): LocalDate = runCatching { LocalDate.parse(this) }.getOrElse { LocalDate.now() }
