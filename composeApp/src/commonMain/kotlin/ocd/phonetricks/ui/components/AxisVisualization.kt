package ocd.phonetricks.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun AxisVisualization(
    accelX: Float,
    accelY: Float,
    accelZ: Float
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "3D Axes (Top View)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val centerX = size.width / 2
                val centerY = size.height / 2
                val scale = 20f

                // Draw center point
                drawCircle(
                    color = Color.Gray,
                    radius = 5f,
                    center = Offset(centerX, centerY)
                )

                // Draw X axis (Red)
                drawLine(
                    color = Color.Red,
                    start = Offset(centerX, centerY),
                    end = Offset(centerX + accelX * scale, centerY),
                    strokeWidth = 8f,
                    cap = StrokeCap.Round
                )

                // Draw Y axis (Green)
                drawLine(
                    color = Color.Green,
                    start = Offset(centerX, centerY),
                    end = Offset(centerX, centerY + accelY * scale),
                    strokeWidth = 8f,
                    cap = StrokeCap.Round
                )

                // Draw Z axis magnitude indicator (Blue circle)
                val zMagnitude = abs(accelZ * scale)
                drawCircle(
                    color = Color.Blue.copy(alpha = 0.3f),
                    radius = zMagnitude,
                    center = Offset(centerX, centerY)
                )
            }
        }
    }
}
