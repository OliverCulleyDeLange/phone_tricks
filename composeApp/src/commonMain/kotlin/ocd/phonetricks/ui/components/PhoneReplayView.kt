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
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ocd.phonetricks.data.SensorData
import kotlin.math.atan2
import kotlin.math.sqrt

@Composable
fun PhoneReplayView(
    sensorHistory: List<SensorData>,
    onReplayComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentIndex by remember { mutableStateOf(0) }
    var isPlaying by remember { mutableStateOf(true) }

    // Play through the data at 2x speed
    LaunchedEffect(isPlaying) {
        if (isPlaying && sensorHistory.isNotEmpty()) {
            while (currentIndex < sensorHistory.size) {
                delay(8L) // ~120Hz playback (60Hz * 2 for double speed)
                currentIndex++
            }
            onReplayComplete()
        }
    }

    val currentData = if (currentIndex < sensorHistory.size) {
        sensorHistory[currentIndex]
    } else {
        sensorHistory.lastOrNull()
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
                    text = "Replay (2x Speed)",
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
                    accelerometer = data.accelerometer,
                    gyroscope = data.gyroscope,
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
                    if (currentIndex >= sensorHistory.size) {
                        // Restart
                        currentIndex = 0
                        isPlaying = true
                    } else {
                        // Toggle pause/play
                        isPlaying = !isPlaying
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    when {
                        currentIndex >= sensorHistory.size -> "Replay Again"
                        isPlaying -> "Pause"
                        else -> "Resume"
                    }
                )
            }
        }
    }
}

@Composable
fun PhoneAnimation(
    accelerometer: ocd.phonetricks.data.Accelerometer,
    gyroscope: ocd.phonetricks.data.Gyroscope,
    rotationVector: ocd.phonetricks.data.RotationVector?,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2

        // If we have rotation vector (quaternion), use it for proper orientation
        // Otherwise fall back to accelerometer-based estimation
        if (rotationVector != null) {
            // Use quaternion to rotate reference vectors
            // This avoids gimbal lock issues with Euler angles
            val x = rotationVector.x.toDouble()
            val y = rotationVector.y.toDouble()
            val z = rotationVector.z.toDouble()
            val w = (rotationVector.scalar ?: computeScalar(x, y, z)).toDouble()

            // Rotate the phone's "up" vector (0, 1, 0) by the quaternion
            // This tells us which way the top of the phone is pointing
            val upVector = rotateVectorByQuaternion(0.0, 1.0, 0.0, x, y, z, w)

            // Rotate the phone's "forward" vector (0, 0, 1) - pointing out of screen
            val forwardVector = rotateVectorByQuaternion(0.0, 0.0, 1.0, x, y, z, w)

            // Rotate the phone's "right" vector (1, 0, 0)
            val rightVector = rotateVectorByQuaternion(1.0, 0.0, 0.0, x, y, z, w)

            // Project to 2D screen space
            // The forward vector's Z component tells us how much the phone is facing us
            val facingAmount = forwardVector.third.toFloat().coerceIn(-1f, 1f)

            // Calculate screen rotation from the up and right vectors
            // atan2 of the projected up vector gives us the rotation in screen space
            // Add 180 degrees to flip the orientation so cyan appears at top
            val screenRotation = kotlin.math.atan2(upVector.first, upVector.second).toFloat()
            val screenRotationDeg = (screenRotation * 180f / kotlin.math.PI.toFloat()) + 180f

            // Calculate perspective scaling
            // When phone faces away (forward.z < 0), it's flipped away from us
            // When facing us (forward.z > 0), normal view
            val perspectiveScale = kotlin.math.abs(facingAmount)

            // Width scaling based on how much phone is tilted left/right (right vector's Z)
            val rightZ = rightVector.third.toFloat()
            val widthScale = kotlin.math.sqrt(1.0 - rightZ * rightZ).toFloat().coerceAtLeast(0.1f)

            // Height scaling based on how much phone is tilted forward/back (up vector's Z)
            val upZ = upVector.third.toFloat()
            val heightScale = kotlin.math.sqrt(1.0 - upZ * upZ).toFloat().coerceAtLeast(0.1f)

            // Phone dimensions - BIGGER!
            val phoneWidth = 200f
            val phoneHeight = 400f
            val cornerRadius = 30f

            val scaledWidth = phoneWidth * widthScale
            val scaledHeight = phoneHeight * heightScale

            // Calculate 3D perspective offsets based on tilt
            // When tilted, we see the edges at different positions
            val depthOffsetX = rightZ * 30f  // Right vector's Z component affects X offset
            val depthOffsetY = -upZ * 30f    // Up vector's Z component affects Y offset (negated to fix pitch direction)

            // Determine if we're looking at front or back
            val isFrontFacing = facingAmount > 0f

            // Apply rotation based on how the phone's up vector projects to screen
            // No negative sign - rotate as-is
            rotate(degrees = screenRotationDeg, pivot = Offset(centerX, centerY)) {
                // Draw shadow/depth edge first (behind the phone)
                if (kotlin.math.abs(depthOffsetX) > 2f || kotlin.math.abs(depthOffsetY) > 2f) {
                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.4f),
                        topLeft = Offset(
                            centerX - scaledWidth / 2 + depthOffsetX,
                            centerY - scaledHeight / 2 + depthOffsetY
                        ),
                        size = Size(scaledWidth, scaledHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius * widthScale.coerceAtLeast(0.3f))
                    )
                }

                // Phone body - different color for front vs back
                val bodyColor = if (isFrontFacing) {
                    Color(0xFF2C3E50) // Dark blue-gray for front
                } else {
                    Color(0xFF8B4513) // Brown for back
                }

                drawRoundRect(
                    color = bodyColor,
                    topLeft = Offset(
                        centerX - scaledWidth / 2,
                        centerY - scaledHeight / 2
                    ),
                    size = Size(scaledWidth, scaledHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius * widthScale.coerceAtLeast(0.3f))
                )

                // Draw edge highlights to show 3D depth
                if (kotlin.math.abs(depthOffsetX) > 2f) {
                    // Draw left or right edge
                    val edgeX = if (depthOffsetX > 0) {
                        centerX - scaledWidth / 2  // Left edge visible
                    } else {
                        centerX + scaledWidth / 2  // Right edge visible
                    }

                    drawLine(
                        color = Color.White.copy(alpha = 0.2f),
                        start = Offset(edgeX, centerY - scaledHeight / 2),
                        end = Offset(edgeX + depthOffsetX, centerY - scaledHeight / 2 + depthOffsetY),
                        strokeWidth = 3f
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.2f),
                        start = Offset(edgeX, centerY + scaledHeight / 2),
                        end = Offset(edgeX + depthOffsetX, centerY + scaledHeight / 2 + depthOffsetY),
                        strokeWidth = 3f
                    )
                }

                if (kotlin.math.abs(depthOffsetY) > 2f) {
                    // Draw top or bottom edge
                    val edgeY = if (depthOffsetY > 0) {
                        centerY - scaledHeight / 2  // Top edge visible
                    } else {
                        centerY + scaledHeight / 2  // Bottom edge visible
                    }

                    drawLine(
                        color = Color.White.copy(alpha = 0.2f),
                        start = Offset(centerX - scaledWidth / 2, edgeY),
                        end = Offset(centerX - scaledWidth / 2 + depthOffsetX, edgeY + depthOffsetY),
                        strokeWidth = 3f
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.2f),
                        start = Offset(centerX + scaledWidth / 2, edgeY),
                        end = Offset(centerX + scaledWidth / 2 + depthOffsetX, edgeY + depthOffsetY),
                        strokeWidth = 3f
                    )
                }

                if (isFrontFacing) {
                    // Front: Show screen
                    val screenInset = 12f * widthScale.coerceAtLeast(0.2f)
                    val topInset = 30f * heightScale.coerceAtLeast(0.2f)
                    val bottomInset = 30f * heightScale.coerceAtLeast(0.2f)

                    drawRoundRect(
                        color = Color(0xFF1a1a1a), // Very dark screen
                        topLeft = Offset(
                            centerX - scaledWidth / 2 + screenInset,
                            centerY - scaledHeight / 2 + topInset
                        ),
                        size = Size(
                            (scaledWidth - screenInset * 2).coerceAtLeast(1f),
                            (scaledHeight - topInset - bottomInset).coerceAtLeast(1f)
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius((cornerRadius - 6f) * widthScale.coerceAtLeast(0.3f))
                    )

                    // Camera notch at TOP (cyan) - visible when phone is mostly facing you
                    if (perspectiveScale > 0.5f) {
                        drawCircle(
                            color = Color.Cyan,
                            radius = 8f * widthScale.coerceAtLeast(0.3f),
                            center = Offset(
                                centerX,
                                centerY - scaledHeight / 2 + 18f * heightScale
                            )
                        )
                    }

                    // Bottom indicator (magenta)
                    if (perspectiveScale > 0.5f) {
                        drawCircle(
                            color = Color.Magenta,
                            radius = 8f * widthScale.coerceAtLeast(0.3f),
                            center = Offset(
                                centerX,
                                centerY + scaledHeight / 2 - 18f * heightScale
                            )
                        )
                    }
                } else {
                    // Back: Show camera module
                    if (perspectiveScale > 0.5f) {
                        // Camera bump (top left on back)
                        drawRoundRect(
                            color = Color(0xFF555555),
                            topLeft = Offset(
                                centerX - scaledWidth / 2 + 15f * widthScale,
                                centerY - scaledHeight / 2 + 15f * heightScale
                            ),
                            size = Size(
                                60f * widthScale,
                                60f * heightScale
                            ),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f * widthScale.coerceAtLeast(0.3f))
                        )

                        // Camera lens
                        drawCircle(
                            color = Color(0xFF222222),
                            radius = 12f * widthScale,
                            center = Offset(
                                centerX - scaledWidth / 2 + 35f * widthScale,
                                centerY - scaledHeight / 2 + 35f * heightScale
                            )
                        )
                    }
                }
            }

            // Debug text
            drawIntoCanvas { canvas ->
                val debugText =
                    "Facing: ${(facingAmount * 100).toInt()}% Width: ${(widthScale * 100).toInt()}% Height: ${(heightScale * 100).toInt()}%"
                val textLayoutResult = textMeasurer.measure(
                    text = debugText,
                    style = TextStyle(
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                )
                canvas.save()
                canvas.translate(10f, size.height - 25f)
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset.Zero
                )
                canvas.restore()
            }

        } else {
            // Fallback: estimate from accelerometer
            val ax = -accelerometer.x
            val ay = accelerometer.y
            val az = accelerometer.z

            val rollRad = kotlin.math.atan2(ax.toDouble(), kotlin.math.sqrt(ay * ay + az * az.toDouble())).toFloat()
            val pitchRad = kotlin.math.atan2(ay.toDouble(), kotlin.math.sqrt(ax * ax + az * az.toDouble())).toFloat()

            val rollDeg = rollRad * 180f / kotlin.math.PI.toFloat()
            val pitchDeg = pitchRad * 180f / kotlin.math.PI.toFloat()

            val rollCos = kotlin.math.abs(kotlin.math.cos(rollRad.toDouble())).toFloat()
            val pitchCos = kotlin.math.abs(kotlin.math.cos(pitchRad.toDouble())).toFloat()

            val phoneWidth = 200f
            val phoneHeight = 400f
            val cornerRadius = 30f

            val scaledWidth = phoneWidth * rollCos.coerceAtLeast(0.1f)
            val scaledHeight = phoneHeight * pitchCos.coerceAtLeast(0.1f)

            // Phone body
            drawRoundRect(
                color = Color(0xFF2C3E50),
                topLeft = Offset(
                    centerX - scaledWidth / 2,
                    centerY - scaledHeight / 2
                ),
                size = Size(scaledWidth, scaledHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius * rollCos.coerceAtLeast(0.3f))
            )

            // Screen
            val screenInset = 12f * rollCos.coerceAtLeast(0.2f)
            val topInset = 30f * pitchCos.coerceAtLeast(0.2f)
            val bottomInset = 30f * pitchCos.coerceAtLeast(0.2f)

            drawRoundRect(
                color = Color(0xFF34495E),
                topLeft = Offset(
                    centerX - scaledWidth / 2 + screenInset,
                    centerY - scaledHeight / 2 + topInset
                ),
                size = Size(
                    (scaledWidth - screenInset * 2).coerceAtLeast(1f),
                    (scaledHeight - topInset - bottomInset).coerceAtLeast(1f)
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius((cornerRadius - 6f) * rollCos.coerceAtLeast(0.3f))
            )

            // Camera notch
            if (pitchCos > 0.5f && rollCos > 0.5f) {
                drawCircle(
                    color = Color(0xFF1A252F),
                    radius = 8f * rollCos.coerceAtLeast(0.3f),
                    center = Offset(
                        centerX,
                        centerY - scaledHeight / 2 + 18f * pitchCos
                    )
                )
            }

            // Debug text
            drawIntoCanvas { canvas ->
                val debugText = "Roll: ${rollDeg.toInt()}° Pitch: ${pitchDeg.toInt()}° (fallback)"
                val textLayoutResult = textMeasurer.measure(
                    text = debugText,
                    style = TextStyle(
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                )
                canvas.save()
                canvas.translate(10f, size.height - 25f)
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset.Zero
                )
                canvas.restore()
            }
        }

        // Draw reference axes (fixed position)
        val axisLength = 60f
        val axisStartX = 80f
        val axisStartY = 80f

        // X axis (Red) - left/right
        drawLine(
            color = Color.Red,
            start = Offset(axisStartX, axisStartY),
            end = Offset(axisStartX + axisLength, axisStartY),
            strokeWidth = 3f
        )

        // Y axis (Green) - up/down
        drawLine(
            color = Color.Green,
            start = Offset(axisStartX, axisStartY),
            end = Offset(axisStartX, axisStartY + axisLength),
            strokeWidth = 3f
        )

        // Z axis (Blue circle) - into/out of screen
        drawCircle(
            color = Color.Blue.copy(alpha = 0.5f),
            radius = 8f,
            center = Offset(axisStartX, axisStartY)
        )
        drawCircle(
            color = Color.Blue,
            radius = 4f,
            center = Offset(axisStartX, axisStartY)
        )

        // Orientation info text (for debugging)
        drawLine(
            color = Color.Gray.copy(alpha = 0.3f),
            start = Offset(0f, size.height - 40f),
            end = Offset(size.width, size.height - 40f),
            strokeWidth = 1f
        )
    }
}

/**
 * Rotate a 3D vector by a quaternion
 * Returns Triple(x, y, z) of the rotated vector
 */
private fun rotateVectorByQuaternion(
    vx: Double, vy: Double, vz: Double,
    qx: Double, qy: Double, qz: Double, qw: Double
): Triple<Double, Double, Double> {
    // Quaternion rotation formula: v' = q * v * q^-1
    // Optimized version without computing full quaternion multiplication

    val ix = qw * vx + qy * vz - qz * vy
    val iy = qw * vy + qz * vx - qx * vz
    val iz = qw * vz + qx * vy - qy * vx
    val iw = -qx * vx - qy * vy - qz * vz

    val rx = ix * qw + iw * -qx + iy * -qz - iz * -qy
    val ry = iy * qw + iw * -qy + iz * -qx - ix * -qz
    val rz = iz * qw + iw * -qz + ix * -qy - iy * -qx

    return Triple(rx, ry, rz)
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
