package ocd.phonetricks.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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

        if (sensorData != null) {
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

            Spacer(modifier = Modifier.height(8.dp))

            // 3D Axis Visualization - uses real-time data
            sensorData?.let { data ->
                AxisVisualization(
                    accelX = data.accelerometer.x,
                    accelY = data.accelerometer.y,
                    accelZ = data.accelerometer.z
                )
            }
        } else {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Waiting for sensor data...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
