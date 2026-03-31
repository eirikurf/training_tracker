package com.ericf.treinociclico.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ericf.treinociclico.data.model.ScheduledWorkout

@Composable
internal fun HeroCard(title: String, subtitle: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.titleMedium)
            HorizontalDivider()
            Text(body)
        }
    }
}

@Composable
internal fun MetricRow(primary: String, primaryValue: String, secondary: String, secondaryValue: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MetricCard(primary, primaryValue, Modifier.weight(1f))
        MetricCard(secondary, secondaryValue, Modifier.weight(1f))
    }
}

@Composable
internal fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(value, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun SessionOutlineCard(scheduled: ScheduledWorkout) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Roteiro da sessão", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("A divisão se repete por sessões concluídas, não por dias fixos da semana.")
            scheduled.day.generalWarmup?.let {
                AssistChip(onClick = {}, label = { Text(it.title) })
            }
            scheduled.day.exercises.forEachIndexed { index, exercise ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${index + 1}. ${exercise.name}", fontWeight = FontWeight.Medium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            exercise.muscles.forEach { muscle ->
                                AssistChip(onClick = {}, label = { Text(muscle.label) })
                            }
                        }
                        Text("${exercise.workSets.size} séries de trabalho planejadas", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
internal fun ConfirmDialog(
    title: String,
    text: String = "",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String = "Confirmar",
    dismissLabel: String = "Cancelar",
    showDismiss: Boolean = true,
    content: (@Composable () -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (text.isNotBlank()) {
                    Text(text)
                }
                content?.invoke()
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel)
            }
        },
        dismissButton = if (showDismiss) {
            {
                TextButton(onClick = onDismiss) {
                    Text(dismissLabel)
                }
            }
        } else {
            null
        },
    )
}
