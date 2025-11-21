package ocd.phonetricks.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ocd.phonetricks.ui.components.AxisVisualization
import ocd.phonetricks.ui.components.SensorGraph

@Composable
fun SensorScreen(viewModel: SensorViewModel) {
    val sensorData by viewModel.sensorData.collectAsState()
    val sensorHistory by viewModel.sensorHistory.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Fixed header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sensor Data Visualization",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Recording status indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isRecording) {
                        Canvas(modifier = Modifier.size(8.dp)) {
                            drawCircle(
                                color = androidx.compose.ui.graphics.Color.Red
                            )
                        }
                    }
                    Button(
                        onClick = {
                            if (isRecording) {
                                viewModel.stopRecording()
                            } else {
                                viewModel.startRecording()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecording)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(if (isRecording) "Pause" else "Resume")
                    }
                }
            }
        }

        // Scrollable content
        if (sensorData != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Core Sensors Section
                Text(
                    text = "Core Sensors",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                // Accelerometer Graph
                SensorGraph(
                    title = "Accelerometer (m/s²)",
                    sensorHistory = sensorHistory,
                    extractX = { it.accelerometer.x },
                    extractY = { it.accelerometer.y },
                    extractZ = { it.accelerometer.z }
                )

                // Gyroscope Graph
                SensorGraph(
                    title = "Gyroscope (rad/s)",
                    sensorHistory = sensorHistory,
                    extractX = { it.gyroscope.x },
                    extractY = { it.gyroscope.y },
                    extractZ = { it.gyroscope.z }
                )

                // Additional Sensors Section (if any are available)
                val hasAdditionalSensors = sensorHistory.any {
                    it.magnetometer != null ||
                        it.rotationVector != null ||
                        it.linearAcceleration != null ||
                        it.gravity != null
                }

                if (hasAdditionalSensors) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Additional Sensors",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Magnetometer Graph (if available)
                    if (sensorHistory.any { it.magnetometer != null }) {
                        SensorGraph(
                            title = "Magnetometer (µT)",
                            sensorHistory = sensorHistory.filter { it.magnetometer != null },
                            extractX = { it.magnetometer!!.x },
                            extractY = { it.magnetometer!!.y },
                            extractZ = { it.magnetometer!!.z }
                        )
                    }

                    // Linear Acceleration Graph (if available)
                    if (sensorHistory.any { it.linearAcceleration != null }) {
                        SensorGraph(
                            title = "Linear Acceleration (m/s²)",
                            sensorHistory = sensorHistory.filter { it.linearAcceleration != null },
                            extractX = { it.linearAcceleration!!.x },
                            extractY = { it.linearAcceleration!!.y },
                            extractZ = { it.linearAcceleration!!.z }
                        )
                    }

                    // Gravity Graph (if available)
                    if (sensorHistory.any { it.gravity != null }) {
                        SensorGraph(
                            title = "Gravity (m/s²)",
                            sensorHistory = sensorHistory.filter { it.gravity != null },
                            extractX = { it.gravity!!.x },
                            extractY = { it.gravity!!.y },
                            extractZ = { it.gravity!!.z }
                        )
                    }

                    // Rotation Vector Graph (if available)
                    if (sensorHistory.any { it.rotationVector != null }) {
                        SensorGraph(
                            title = "Rotation Vector (Quaternion)",
                            sensorHistory = sensorHistory.filter { it.rotationVector != null },
                            extractX = { it.rotationVector!!.x },
                            extractY = { it.rotationVector!!.y },
                            extractZ = { it.rotationVector!!.z }
                        )
                    }
                }

                // 3D Visualization Section
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Real-Time Visualization",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                // 3D Axis Visualization - uses real-time data
                sensorData?.let { data ->
                    AxisVisualization(
                        accelX = data.accelerometer.x,
                        accelY = data.accelerometer.y,
                        accelZ = data.accelerometer.z
                    )
                }

                // Bottom padding for scroll
                Spacer(modifier = Modifier.height(16.dp))
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Waiting for sensor data...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
