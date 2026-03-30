package com.ericf.treinociclico.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import kotlin.math.pow
import kotlin.math.roundToInt

data class ChartPoint(
    val date: LocalDate,
    val value: Double,
)

@Composable
internal fun LineChartCard(
    title: String,
    subtitle: String,
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        val outlineColor = MaterialTheme.colorScheme.outline
        val primaryColor = MaterialTheme.colorScheme.primary
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
            if (points.size < 2) {
                Text("Dados insuficientes para desenhar o gráfico.")
            } else {
                val minValue = points.minOf { it.value }
                val maxValue = points.maxOf { it.value }
                val range = (maxValue - minValue).takeIf { it > 0 } ?: 1.0
                Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                    val leftPadding = 24.dp.toPx()
                    val topPadding = 16.dp.toPx()
                    val bottomPadding = 24.dp.toPx()
                    val chartWidth = size.width - leftPadding
                    val chartHeight = size.height - topPadding - bottomPadding
                    val stepX = chartWidth / (points.size - 1).coerceAtLeast(1)
                    val path = Path()
                    points.forEachIndexed { index, point ->
                        val x = leftPadding + (stepX * index)
                        val normalizedY = ((point.value - minValue) / range).toFloat()
                        val y = topPadding + chartHeight - (normalizedY * chartHeight.toFloat())
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawLine(
                        color = outlineColor,
                        start = Offset(leftPadding, topPadding),
                        end = Offset(leftPadding, size.height - bottomPadding),
                        strokeWidth = 2.dp.toPx(),
                    )
                    drawLine(
                        color = outlineColor,
                        start = Offset(leftPadding, size.height - bottomPadding),
                        end = Offset(size.width, size.height - bottomPadding),
                        strokeWidth = 2.dp.toPx(),
                    )
                    drawPath(
                        path = path,
                        color = primaryColor,
                        style = Stroke(width = 3.dp.toPx()),
                    )
                    points.forEachIndexed { index, point ->
                        val x = leftPadding + (stepX * index)
                        val normalizedY = ((point.value - minValue) / range).toFloat()
                        val y = topPadding + chartHeight - (normalizedY * chartHeight.toFloat())
                        drawCircle(
                            color = primaryColor,
                            radius = 4.dp.toPx(),
                            center = Offset(x, y),
                        )
                    }
                }
                Text("Mín: ${minValue.round(1)} • Máx: ${maxValue.round(1)}")
                Text("Início: ${points.first().date} • Fim: ${points.last().date}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun Double.round(decimals: Int): Double {
    val factor = 10.0.pow(decimals.toDouble())
    return (this * factor).roundToInt() / factor
}
