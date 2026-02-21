package ocd.phonetricks.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ocd.phonetricks.audio.Waveform
import ocd.phonetricks.data.Accelerometer
import ocd.phonetricks.data.Gyroscope
import ocd.phonetricks.data.RotationVector
import ocd.phonetricks.ui.components.SynthParametersCard
import ocd.phonetricks.ui.components.PhoneVisualization3D
import ocd.phonetricks.ui.components.SensorGraph
import ocd.phonetricks.ui.components.TouchPad
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun MainScreen(
    sensorViewModel: SensorViewModel,
    synthesizerViewModel: SynthesizerViewModel,
    onOpenSettings: () -> Unit,
) {
    val rotationVectorData by sensorViewModel.rotationVectorData.collectAsState()
    val accelerometerHistory by sensorViewModel.accelerometerHistory.collectAsState()
    val gyroscopeHistory by sensorViewModel.gyroscopeHistory.collectAsState()
    val quaternionHistory by sensorViewModel.quaternionHistory.collectAsState()

    val frequency by synthesizerViewModel.baseFrequency.collectAsState()
    val amplitude by synthesizerViewModel.amplitude.collectAsState()
    val waveform by synthesizerViewModel.waveform.collectAsState()

    MainScreenContent(
        frequency,
        amplitude,
        waveform,
        accelerometerHistory,
        gyroscopeHistory,
        quaternionHistory,
        rotationVectorData,
        onTouchPad = { x, y -> synthesizerViewModel.onTouchInBox(x, y) },
        onReleasePad = { synthesizerViewModel.onReleaseTouch() },
        onTare = { sensorViewModel.tare() },
        onOpenSettings = onOpenSettings,
    )
}

@Composable
private fun MainScreenContent(
    frequency: Float,
    amplitude: Float,
    waveform: Waveform,
    accelerometerHistory: List<Accelerometer>,
    gyroscopeHistory: List<Gyroscope>,
    quaternionHistory: List<RotationVector>,
    rotationVectorData: RotationVector?,
    onTouchPad: (Float, Float) -> Unit,
    onReleasePad: () -> Unit,
    onTare: () -> Unit,
    onOpenSettings: () -> Unit = {},
) {
    Scaffold { paddingValues ->

        Box(Modifier.fillMaxSize().padding(paddingValues)) {
            TouchPad(
                onTouch = onTouchPad,
                onUnTouch = onReleasePad,
                Modifier.fillMaxSize()
            )

            DebugInfo(
                frequency, amplitude, waveform, accelerometerHistory, gyroscopeHistory, quaternionHistory,
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            rotationVectorData?.let { data ->
                PhoneVisualization3D(
                    rotationVector = data,
                    onTareRequest = onTare,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .fillMaxWidth()
                        .height(300.dp)
                )
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
            ) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings")
            }
        }
    }
}

@Composable
private fun DebugInfo(
    frequency: Float,
    amplitude: Float,
    waveform: Waveform,
    accelerometerHistory: List<Accelerometer>,
    gyroscopeHistory: List<Gyroscope>,
    quaternionHistory: List<RotationVector>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp)
    ) {
        SynthParametersCard(
            frequency = frequency,
            amplitude = amplitude,
            waveform = waveform,
            modifier = Modifier.fillMaxWidth()
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

        SensorGraph(
            title = "Quaternion",
            sensorHistory = quaternionHistory,
            extractX = { it.x },
            extractY = { it.y },
            extractZ = { it.z },
            extractW = { it.scalar ?: 0f },
            yMin = -1f,
            yMax = 1f,
        )
    }
}

@Preview
@Composable
fun MainScreenContentPreview() = MaterialTheme {
    MainScreenContent(
        frequency = 440f,
        amplitude = 0.5f,
        waveform = Waveform.SINE,
        accelerometerHistory = emptyList(),
        gyroscopeHistory = emptyList(),
        quaternionHistory = emptyList(),
        rotationVectorData = null,
        onTouchPad = { _, _ -> },
        onReleasePad = {},
        onTare = {}
    )
}