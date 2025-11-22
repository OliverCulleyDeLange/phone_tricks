package ocd.phonetricks.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import ocd.phonetricks.data.SensorData
import kotlin.math.atan2
import kotlin.math.sqrt

@Composable
fun PhoneReplayView(
    sensorHistory: List<SensorData>,
    /**
     * Flow that emits the current index into `sensorHistory` (0-based).
     * The ViewModel should emit at the desired playback rate (e.g. 60Hz).
     */
    playbackIndexFlow: Flow<Int>,
    onReplayComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Collect playback index from ViewModel-provided flow
    val currentIndex by playbackIndexFlow.collectAsState(initial = 0)
    var completionNotified by remember { mutableStateOf(false) }

    val currentData = if (sensorHistory.isNotEmpty() && currentIndex in sensorHistory.indices) {
        sensorHistory[currentIndex]
    } else {
        sensorHistory.lastOrNull()
    }

    // Notify completion once when the flow reaches the end of the history
    val isFinished = sensorHistory.isEmpty() || currentIndex >= (sensorHistory.size - 1)
    LaunchedEffect(isFinished, completionNotified) {
        if (isFinished && !completionNotified) {
            completionNotified = true
            onReplayComplete()
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Replay",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                // Progress indicator
                Text(
                    text = "${(currentIndex * 100f / sensorHistory.size.coerceAtLeast(1)).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = currentIndex.toFloat() / sensorHistory.size.coerceAtLeast(1),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Phone animation
            currentData?.let { data ->
                PhoneAnimation(
                    rotationVector = data.rotationVector,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Control button
            Button(
                onClick = {
                    // Playback is controlled by the ViewModel flow. When finished, user can request a replay
                    if (isFinished) onReplayComplete()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    when {
                        isFinished -> "Replay Again"
                        else -> "Playing"
                    }
                )
            }
        }
    }
}

@Composable
fun PhoneAnimation(
    rotationVector: ocd.phonetricks.data.RotationVector,
    modifier: Modifier = Modifier,
    onTareRequest: (() -> Unit)? = null
) {
    // Use SceneView 3D visualization on Android
    PhoneVisualization3D(
        rotationVector = rotationVector,
        modifier = modifier,
        onTareRequest = onTareRequest
    )
}

/**
 * Convert a quaternion (x, y, z, w) to a 4x4 rotation matrix
 */
private fun quaternionToMatrix(
    qx: Double, qy: Double, qz: Double, qw: Double,
    matrix: androidx.compose.ui.graphics.Matrix
) {
    // Normalize the quaternion
    val length = kotlin.math.sqrt(qx * qx + qy * qy + qz * qz + qw * qw)
    val x = qx / length
    val y = qy / length
    val z = qz / length
    val w = qw / length

    // Convert quaternion to rotation matrix
    // Based on: https://www.euclideanspace.com/maths/geometry/rotations/conversions/quaternionToMatrix/index.htm
    val xx = x * x
    val xy = x * y
    val xz = x * z
    val xw = x * w
    val yy = y * y
    val yz = y * z
    val yw = y * w
    val zz = z * z
    val zw = z * w

    matrix.reset()
    matrix[0, 0] = (1.0 - 2.0 * (yy + zz)).toFloat()
    matrix[0, 1] = (2.0 * (xy - zw)).toFloat()
    matrix[0, 2] = (2.0 * (xz + yw)).toFloat()

    matrix[1, 0] = (2.0 * (xy + zw)).toFloat()
    matrix[1, 1] = (1.0 - 2.0 * (xx + zz)).toFloat()
    matrix[1, 2] = (2.0 * (yz - xw)).toFloat()

    matrix[2, 0] = (2.0 * (xz - yw)).toFloat()
    matrix[2, 1] = (2.0 * (yz + xw)).toFloat()
    matrix[2, 2] = (1.0 - 2.0 * (xx + yy)).toFloat()

    matrix[3, 3] = 1.0f
}

/**
 * Compute the scalar (w) component if not provided
 * Since x² + y² + z² + w² = 1
 */
private fun computeScalar(x: Double, y: Double, z: Double): Float {
    val sumSquares = x * x + y * y + z * z
    return if (sumSquares < 1.0) {
        kotlin.math.sqrt(1.0 - sumSquares).toFloat()
    } else {
        0f
    }
}
