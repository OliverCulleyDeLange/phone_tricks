package ocd.phonetricks.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.max

data class GraphSeries(val label: String, val color: Color, val values: List<Float>)

@Preview
@Composable
private fun SensorGraphPreview() = MaterialTheme {
    SensorGraph(
        title = "Sensor Data",
        sensorHistory = listOf(
            Triple(3f, 1f, 1f),
            Triple(2f, 2f, 1f),
            Triple(3f, 2f, 1f),
        ),
        extractX = { it.first },
        extractY = { it.second },
        extractZ = { it.third }
    )
}

@Composable
fun <T> SensorGraph(
    title: String,
    sensorHistory: List<T>,
    extractX: (T) -> Float,
    extractY: (T) -> Float,
    extractZ: (T) -> Float,
    modifier: Modifier = Modifier
) {
    SensorGraphMulti(
        title = title,
        series = listOf(
            GraphSeries("X", Color.Red, sensorHistory.map { extractX(it) }),
            GraphSeries("Y", Color.Green, sensorHistory.map { extractY(it) }),
            GraphSeries("Z", Color.Blue, sensorHistory.map { extractZ(it) }),
        ),
        modifier = modifier,
    )
}

@Composable
fun <T> SensorGraph(
    title: String,
    sensorHistory: List<T>,
    extractX: (T) -> Float,
    extractY: (T) -> Float,
    extractZ: (T) -> Float,
    extractW: (T) -> Float,
    modifier: Modifier = Modifier,
    yMin: Float? = null,
    yMax: Float? = null,
) {
    SensorGraphMulti(
        title = title,
        series = listOf(
            GraphSeries("X", Color.Red, sensorHistory.map { extractX(it) }),
            GraphSeries("Y", Color.Green, sensorHistory.map { extractY(it) }),
            GraphSeries("Z", Color.Blue, sensorHistory.map { extractZ(it) }),
            GraphSeries("W", Color.Magenta, sensorHistory.map { extractW(it) }),
        ),
        modifier = modifier,
        yMin = yMin,
        yMax = yMax,
    )
}

@Composable
fun SensorGraphMulti(
    title: String,
    series: List<GraphSeries>,
    modifier: Modifier = Modifier,
    yMin: Float? = null,
    yMax: Float? = null,
) {
    Column(modifier = modifier.padding(16.dp)) {
        TitleAndLegend(title, series)
        Spacer(modifier = Modifier.height(8.dp))
        if (series.all { it.values.isEmpty() }) {
            EmptyState()
        } else {
            HistoryChart(series, yMin, yMax)
        }
    }
}

@Composable
private fun HistoryChart(series: List<GraphSeries>, fixedYMin: Float? = null, fixedYMax: Float? = null) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        val width = size.width
        val height = size.height

        val yAxisLabelSpace = 40f
        val padding = 20f

        val graphLeft = yAxisLabelSpace + padding
        val graphRight = width - padding

        val allValues = series.flatMap { it.values }
        val minValue = allValues.minOrNull() ?: -10f
        val maxValue = allValues.maxOrNull() ?: 10f
        val valueRange = maxValue - minValue
        val adjustedMin = fixedYMin ?: (minValue - valueRange * 0.1f)
        val adjustedMax = fixedYMax ?: (maxValue + valueRange * 0.1f)
        val adjustedRange = adjustedMax - adjustedMin

        val historySize = series.maxOfOrNull { it.values.size } ?: 0

        fun mapToY(value: Float): Float =
            height - padding - ((value - adjustedMin) / adjustedRange) * (height - 2 * padding)

        fun mapToX(index: Int): Float =
            graphLeft + (index.toFloat() / max(1, historySize - 1)) * (graphRight - graphLeft)

        val numYTicks = 5
        val yTickValues = (0 until numYTicks).map { tickIdx ->
            adjustedMin + (tickIdx * (adjustedMax - adjustedMin) / (numYTicks - 1))
        }

        yTickValues.forEach { value ->
            val y = mapToY(value)
            drawLine(
                color = Color.Gray.copy(alpha = 0.3f),
                start = Offset(yAxisLabelSpace - 6f, y),
                end = Offset(yAxisLabelSpace, y),
                strokeWidth = 1.8f
            )
            val rounded = (value * 100).toInt() / 100.0
            val label = rounded.toString()
            drawIntoCanvas { canvas ->
                val textLayoutResult = textMeasurer.measure(
                    text = label,
                    style = TextStyle(fontSize = 10.sp, color = Color.Gray)
                )
                canvas.save()
                canvas.translate(
                    yAxisLabelSpace - 9f - textLayoutResult.size.width,
                    y - textLayoutResult.size.height / 2
                )
                drawText(textLayoutResult = textLayoutResult, topLeft = Offset.Zero)
                canvas.restore()
            }
        }

        drawLine(
            color = Color.Gray.copy(alpha = 0.3f),
            start = Offset(yAxisLabelSpace, padding),
            end = Offset(yAxisLabelSpace, height - padding),
            strokeWidth = 1.4f
        )

        series.forEach { s ->
            if (s.values.size > 1) {
                val path = Path().apply {
                    moveTo(mapToX(0), mapToY(s.values[0]))
                    for (i in 1 until s.values.size) {
                        lineTo(mapToX(i), mapToY(s.values[i]))
                    }
                }
                drawPath(path = path, color = s.color, style = Stroke(width = 3f, cap = StrokeCap.Round))
            }
        }

        if (adjustedMin <= 0f && adjustedMax >= 0f) {
            val zeroY = mapToY(0f)
            drawLine(
                color = Color.Gray.copy(alpha = 0.3f),
                start = Offset(graphLeft, zeroY),
                end = Offset(graphRight, zeroY),
                strokeWidth = 1f
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Waiting for data...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TitleAndLegend(title: String, series: List<GraphSeries>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            series.forEach { LegendItem(it.label, it.color) }
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(16.dp)) {
            drawLine(
                color = color,
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
        }
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}
