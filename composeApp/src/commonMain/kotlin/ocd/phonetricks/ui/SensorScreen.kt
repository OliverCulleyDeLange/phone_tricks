package ocd.phonetricks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ocd.phonetricks.ui.components.PhoneVisualization3D
import ocd.phonetricks.ui.components.SensorGraph
import ocd.phonetricks.ui.components.TrickTimeline

@Composable
fun SensorScreen(viewModel: SensorViewModel) {
    val rotationVectorData by viewModel.rotationVectorData.collectAsState()
    val accelerometerHistory by viewModel.accelerometerHistory.collectAsState()
    val gyroscopeHistory by viewModel.gyroscopeHistory.collectAsState()
    val magnetometerHistory by viewModel.magnetometerHistory.collectAsState()
    val rotationVectorHistory by viewModel.rotationVectorHistory.collectAsState()
    val linearAccelerationHistory by viewModel.linearAccelerationHistory.collectAsState()
    val gravityHistory by viewModel.gravityHistory.collectAsState()
    val detectedTricks by viewModel.detectedTricks.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 4.dp
        ) {
            if (rotationVectorData != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    rotationVectorData?.let { data ->
                        TrickTimeline(
                            tricks = detectedTricks,
                            currentTime = data.timestampMs
                        )
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
                                    text = "Live Phone Motion",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            rotationVectorData?.let { data ->
                                PhoneVisualization3D(
                                    rotationVector = data,
                                    onTareRequest = { viewModel.tare() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(400.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Sensors",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    SensorGraph(
                        title = "Accelerometer (m/s²)",
                        sensorHistory = accelerometerHistory,
                        extractX = { it.x },
                        extractY = { it.y },
                        extractZ = { it.z }
                    )

                    SensorGraph(
                        title = "Gyroscope (rad/s)",
                        sensorHistory = gyroscopeHistory,
                        extractX = { it.x },
                        extractY = { it.y },
                        extractZ = { it.z }
                    )

                    if (magnetometerHistory.isNotEmpty()) {
                        SensorGraph(
                            title = "Magnetometer (µT)",
                            sensorHistory = magnetometerHistory,
                            extractX = { it.x },
                            extractY = { it.y },
                            extractZ = { it.z }
                        )
                    }

                    if (linearAccelerationHistory.isNotEmpty()) {
                        SensorGraph(
                            title = "Linear Acceleration (m/s²)",
                            sensorHistory = linearAccelerationHistory,
                            extractX = { it.x },
                            extractY = { it.y },
                            extractZ = { it.z }
                        )
                    }

                    if (gravityHistory.isNotEmpty()) {
                        SensorGraph(
                            title = "Gravity (m/s²)",
                            sensorHistory = gravityHistory,
                            extractX = { it.x },
                            extractY = { it.y },
                            extractZ = { it.z }
                        )
                    }

                    if (rotationVectorHistory.isNotEmpty()) {
                        SensorGraph(
                            title = "Rotation Vector (Quaternion)",
                            sensorHistory = rotationVectorHistory,
                            extractX = { it.x },
                            extractY = { it.y },
                            extractZ = { it.z }
                        )
                    }

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
}
