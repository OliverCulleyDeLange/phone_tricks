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
    Column(
        modifier = modifier.padding(16.dp)
    ) {
        TitleAndLegend(title)

        Spacer(modifier = Modifier.height(8.dp))

        if (sensorHistory.isEmpty()) {
            EmptyState()
        } else {
            HistoryChart(sensorHistory, extractX, extractY, extractZ)
        }
    }

}

@Composable
private fun <T> HistoryChart(
    sensorHistory: List<T>,
    extractX: (T) -> Float,
    extractY: (T) -> Float,
    extractZ: (T) -> Float
) {
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
        val graphWidth = graphRight - graphLeft

        val xValues = sensorHistory.map { extractX(it) }
        val yValues = sensorHistory.map { extractY(it) }
        val zValues = sensorHistory.map { extractZ(it) }

        val allValues = xValues + yValues + zValues
        val minValue = allValues.minOrNull() ?: -10f
        val maxValue = allValues.maxOrNull() ?: 10f
        val valueRange = maxValue - minValue
        val adjustedMin = minValue - (valueRange * 0.1f)
        val adjustedMax = maxValue + (valueRange * 0.1f)
        val adjustedRange = adjustedMax - adjustedMin

        fun mapToY(value: Float): Float {
            return height - padding - ((value - adjustedMin) / adjustedRange) * (height - 2 * padding)
        }

        fun mapToX(index: Int): Float {
            return graphLeft + (index.toFloat() / max(1, sensorHistory.size - 1)) * (graphRight - graphLeft)
        }

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
                    style = TextStyle(
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                )

                val textWidth = textLayoutResult.size.width
                val textHeight = textLayoutResult.size.height

                canvas.save()
                canvas.translate(
                    yAxisLabelSpace - 9f - textWidth,
                    y - textHeight / 2
                )
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset.Zero
                )
                canvas.restore()
            }
        }

        drawLine(
            color = Color.Gray.copy(alpha = 0.3f),
            start = Offset(yAxisLabelSpace, padding),
            end = Offset(yAxisLabelSpace, height - padding),
            strokeWidth = 1.4f
        )

        if (xValues.size > 1) {
            val pathX = Path().apply {
                moveTo(mapToX(0), mapToY(xValues[0]))
                for (i in 1 until xValues.size) {
                    lineTo(mapToX(i), mapToY(xValues[i]))
                }
            }
            drawPath(
                path = pathX,
                color = Color.Red,
                style = Stroke(width = 3f, cap = StrokeCap.Round)
            )
        }

        if (yValues.size > 1) {
            val pathY = Path().apply {
                moveTo(mapToX(0), mapToY(yValues[0]))
                for (i in 1 until yValues.size) {
                    lineTo(mapToX(i), mapToY(yValues[i]))
                }
            }
            drawPath(
                path = pathY,
                color = Color.Green,
                style = Stroke(width = 3f, cap = StrokeCap.Round)
            )
        }

        if (zValues.size > 1) {
            val pathZ = Path().apply {
                moveTo(mapToX(0), mapToY(zValues[0]))
                for (i in 1 until zValues.size) {
                    lineTo(mapToX(i), mapToY(zValues[i]))
                }
            }
            drawPath(
                path = pathZ,
                color = Color.Blue,
                style = Stroke(width = 3f, cap = StrokeCap.Round)
            )
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
private fun TitleAndLegend(title: String) {
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

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LegendItem("X", Color.Red)
            LegendItem("Y", Color.Green)
            LegendItem("Z", Color.Blue)
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
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
