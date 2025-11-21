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
import androidx.compose.ui.unit.dp
import ocd.phonetricks.data.SensorData
import kotlin.math.max
import kotlin.math.min

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
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val padding = 20f

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
                        return padding + (index.toFloat() / max(1, sensorHistory.size - 1)) * (width - 2 * padding)
                    }

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
                            start = Offset(padding, zeroY),
                            end = Offset(width - padding, zeroY),
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
