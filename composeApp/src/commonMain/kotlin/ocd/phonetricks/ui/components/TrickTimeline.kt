package ocd.phonetricks.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ocd.phonetricks.data.TrickEvent
import ocd.phonetricks.data.TrickType
import kotlinx.coroutines.delay

@Composable
fun TrickTimeline(
    tricks: List<TrickEvent>,
    currentTime: Long,
    timeWindowMs: Long = 10000L,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

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
                    text = "Detected Tricks",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                // Legend
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LegendItem("Spin", Color(0xFFFF6B6B))
                    LegendItem("Flip", Color(0xFF4ECDC4))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Trick count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TrickCount("Spins", tricks.count { it.type == TrickType.SPIN }, Color(0xFFFF6B6B))
                TrickCount("Flips", tricks.count { it.type == TrickType.FLIP }, Color(0xFF4ECDC4))
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (tricks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tricks detected yet. Try spinning or flipping your phone!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Timeline visualization - scrolls like sensor graphs
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val padding = 20f

                    // Draw timeline base
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.3f),
                        start = Offset(padding, height / 2),
                        end = Offset(width - padding, height / 2),
                        strokeWidth = 2f
                    )

                    // Use current time as "now" (right edge) - this makes it scroll!
                    val startTime = currentTime - timeWindowMs

                    // Filter tricks in time window
                    val visibleTricks = tricks.filter { it.timestamp >= startTime }

                    // Draw tricks as dots (newest on right, scrolling left as time passes)
                    visibleTricks.forEach { trick ->
                        // Map timestamp to X position (right = most recent)
                        val timeSinceStart = trick.timestamp - startTime
                        val progress = timeSinceStart.toFloat() / timeWindowMs.toFloat()
                        val x = padding + (progress * (width - 2 * padding))

                        // Choose color based on trick type
                        val color = when (trick.type) {
                            TrickType.SPIN -> Color(0xFFFF6B6B)
                            TrickType.FLIP -> Color(0xFF4ECDC4)
                        }

                        // Dot size based on confidence
                        val baseRadius = 16f
                        val radius = baseRadius + (trick.confidence * 8f)

                        // Draw glow effect
                        drawCircle(
                            color = color.copy(alpha = 0.3f),
                            radius = radius * 1.3f,
                            center = Offset(x, height / 2)
                        )

                        // Draw main dot
                        drawCircle(
                            color = color,
                            radius = radius,
                            center = Offset(x, height / 2)
                        )

                        // Draw confidence percentage in the dot
                        val confidencePercent = (trick.confidence * 100).toInt()
                        val text = "$confidencePercent%"

                        drawIntoCanvas { canvas ->
                            val textLayoutResult = textMeasurer.measure(
                                text = text,
                                style = TextStyle(
                                    color = Color.White,
                                    fontSize = 10.sp
                                )
                            )

                            val textWidth = textLayoutResult.size.width
                            val textHeight = textLayoutResult.size.height

                            canvas.save()
                            canvas.translate(
                                x - textWidth / 2,
                                height / 2 - textHeight / 2
                            )
                            drawText(
                                textLayoutResult = textLayoutResult,
                                topLeft = Offset.Zero
                            )
                            canvas.restore()
                        }
                    }

                    // Draw time markers with labels
                    val markerCount = 5
                    for (i in 0..markerCount) {
                        val x = padding + (i.toFloat() / markerCount) * (width - 2 * padding)

                        // Vertical line
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.2f),
                            start = Offset(x, height / 2 - 15f),
                            end = Offset(x, height / 2 + 15f),
                            strokeWidth = 1f
                        )

                        // Time label (seconds ago)
                        val secondsAgo = ((markerCount - i).toFloat() / markerCount * timeWindowMs / 1000).toInt()
                        val label = if (secondsAgo == 0) "now" else "${secondsAgo}s"

                        drawIntoCanvas { canvas ->
                            val textLayoutResult = textMeasurer.measure(
                                text = label,
                                style = TextStyle(
                                    color = Color.Gray.copy(alpha = 0.6f),
                                    fontSize = 9.sp
                                )
                            )

                            val textWidth = textLayoutResult.size.width

                            canvas.save()
                            canvas.translate(
                                x - textWidth / 2,
                                height / 2 + 25f
                            )
                            drawText(
                                textLayoutResult = textLayoutResult,
                                topLeft = Offset.Zero
                            )
                            canvas.restore()
                        }
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
        Canvas(modifier = Modifier.size(12.dp)) {
            drawCircle(color = color)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TrickCount(label: String, count: Int, color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            drawCircle(
                color = color.copy(alpha = 0.2f)
            )
            drawCircle(
                color = color,
                radius = size.minDimension / 4
            )
        }
        Column {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
