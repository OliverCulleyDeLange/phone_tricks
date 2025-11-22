package ocd.phonetricks.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
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
import ocd.phonetricks.data.SensorData
import kotlin.math.max
import kotlin.math.min
import kotlin.math.abs

@Composable
fun SensorGraph(
    title: String,
    sensorHistory: List<SensorData>,
    extractX: (SensorData) -> Float,
    extractY: (SensorData) -> Float,
    extractZ: (SensorData) -> Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                // Legend
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LegendItem("X", Color.Red)
                    LegendItem("Y", Color.Green)
                    LegendItem("Z", Color.Blue)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (sensorHistory.isEmpty()) {
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
            } else {
                val textMeasurer = rememberTextMeasurer()

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                ) {
                    val width = size.width
                    val height = size.height

                    // Y-axis label area width
                    val yAxisLabelSpace = 40f
                    val padding = 20f

                    // Graph area starts after yAxisLabelSpace
                    val graphLeft = yAxisLabelSpace + padding
                    val graphRight = width - padding
                    val graphWidth = graphRight - graphLeft

                    // Extract all values
                    val xValues = sensorHistory.map { extractX(it) }
                    val yValues = sensorHistory.map { extractY(it) }
                    val zValues = sensorHistory.map { extractZ(it) }

                    // Find min and max across all axes for adaptive scaling
                    val allValues = xValues + yValues + zValues
                    val minValue = allValues.minOrNull() ?: -10f
                    val maxValue = allValues.maxOrNull() ?: 10f
                    val valueRange = maxValue - minValue
                    val adjustedMin = minValue - (valueRange * 0.1f)
                    val adjustedMax = maxValue + (valueRange * 0.1f)
                    val adjustedRange = adjustedMax - adjustedMin

                    // Helper function to map value to Y coordinate
                    fun mapToY(value: Float): Float {
                        return height - padding - ((value - adjustedMin) / adjustedRange) * (height - 2 * padding)
                    }

                    // Helper function to map index to X coordinate
                    fun mapToX(index: Int): Float {
                        return graphLeft + (index.toFloat() / max(1, sensorHistory.size - 1)) * (graphRight - graphLeft)
                    }

                    // Draw Y-axis scale (tick marks and labels)
                    val numYTicks = 5
                    val yTickValues = (0 until numYTicks).map { tickIdx ->
                        adjustedMin + (tickIdx * (adjustedMax - adjustedMin) / (numYTicks - 1))
                    }

                    yTickValues.forEach { value ->
                        val y = mapToY(value)
                        // Draw tick
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.3f),
                            start = Offset(yAxisLabelSpace - 6f, y),
                            end = Offset(yAxisLabelSpace, y),
                            strokeWidth = 1.8f
                        )
                        // Draw label
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

                    // Draw Y axis line itself
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.3f),
                        start = Offset(yAxisLabelSpace, padding),
                        end = Offset(yAxisLabelSpace, height - padding),
                        strokeWidth = 1.4f
                    )

                    // Draw X axis line (Red)
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

                    // Draw Y axis line (Green)
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

                    // Draw Z axis line (Blue)
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

                    // Draw center reference line (zero line if in range)
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
