package ocd.phonetricks.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ocd.phonetricks.ui.components.AxisVisualization
import ocd.phonetricks.ui.components.SensorDataCard

@Composable
fun SensorScreen(viewModel: SensorViewModel) {
    val sensorData by viewModel.sensorData.collectAsState()
    val throttledData by viewModel.throttledSensorData.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Sensor Data Visualization",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

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
            )
        ) {
            Text(if (isRecording) "Stop Recording" else "Start Recording")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (sensorData != null) {
            // Use throttled data for text displays
            throttledData?.let { data ->
                // Accelerometer Data
                SensorDataCard(
                    title = "Accelerometer (m/s²)",
                    xValue = data.accelerometer.x,
                    yValue = data.accelerometer.y,
                    zValue = data.accelerometer.z
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Gyroscope Data
                SensorDataCard(
                    title = "Gyroscope (rad/s)",
                    xValue = data.gyroscope.x,
                    yValue = data.gyroscope.y,
                    zValue = data.gyroscope.z
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Use full-speed data for visualization (60Hz)
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
                text = if (isRecording) "Waiting for sensor data..." else "Press Start to begin recording",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
