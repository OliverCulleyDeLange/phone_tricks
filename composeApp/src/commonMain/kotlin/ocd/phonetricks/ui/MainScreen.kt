package ocd.phonetricks.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
    eqViewModel: EqViewModel,
    sampleViewModel: SampleViewModel,
    onOpenSettings: () -> Unit,
    onOpenFx: () -> Unit,
    onOpenNoteSettings: () -> Unit,
    onOpenEq: () -> Unit,
    onOpenSampler: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotationVectorData by sensorViewModel.rotationVectorData.collectAsState()
    val accelerometerHistory by sensorViewModel.accelerometerHistory.collectAsState()
    val gyroscopeHistory by sensorViewModel.gyroscopeHistory.collectAsState()
    val quaternionHistory by sensorViewModel.quaternionHistory.collectAsState()

    val frequency by synthesizerViewModel.baseFrequency.collectAsState()
    val amplitude by synthesizerViewModel.amplitude.collectAsState()
    val waveformA by synthesizerViewModel.waveformA.collectAsState()
    val waveformB by synthesizerViewModel.waveformB.collectAsState()
    val waveformBlend by synthesizerViewModel.waveformBlend.collectAsState()
    val spectrum by eqViewModel.spectrum.collectAsState()

    MainScreenContent(
        frequency,
        amplitude,
        waveformA,
        waveformB,
        waveformBlend,
        spectrum,
        accelerometerHistory,
        gyroscopeHistory,
        quaternionHistory,
        rotationVectorData,
        onTouchPad = { x, y -> synthesizerViewModel.onTouchInBox(x, y) },
        onReleasePad = { synthesizerViewModel.onReleaseTouch() },
        onTare = { sensorViewModel.tare() },
        onOpenSettings = onOpenSettings,
        onOpenFx = onOpenFx,
        onOpenNoteSettings = onOpenNoteSettings,
        onOpenEq = onOpenEq,
        onOpenSampler = onOpenSampler,
    )
}

@Composable
private fun MainScreenContent(
    frequency: Float,
    amplitude: Float,
    waveformA: Waveform,
    waveformB: Waveform,
    waveformBlend: Float,
    spectrum: FloatArray,
    accelerometerHistory: List<Accelerometer>,
    gyroscopeHistory: List<Gyroscope>,
    quaternionHistory: List<RotationVector>,
    rotationVectorData: RotationVector?,
    onTouchPad: (Float, Float) -> Unit,
    onReleasePad: () -> Unit,
    onTare: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenFx: () -> Unit = {},
    onOpenNoteSettings: () -> Unit = {},
    onOpenEq: () -> Unit = {},
    onOpenSampler: () -> Unit = {},
) {
    Scaffold { paddingValues ->
        Box(Modifier.fillMaxSize().padding(paddingValues)) {
            DebugInfo(
                frequency, amplitude, waveformA, waveformB, waveformBlend, spectrum, accelerometerHistory, gyroscopeHistory, quaternionHistory,
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            rotationVectorData?.let { data ->
                PhoneVisualization3D(
                    rotationVector = data,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .fillMaxWidth()
                        .height(300.dp)
                )
            }

            TouchPad(
                onTouch = onTouchPad,
                onUnTouch = onReleasePad,
                Modifier.fillMaxSize()
            )

            Column(
                modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                IconButton(onClick = onOpenSampler) {
                    Icon(Icons.Filled.Mic, contentDescription = "Sampler")
                }
                IconButton(onClick = onTare) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Tare")
                }
                IconButton(onClick = onOpenNoteSettings) {
                    Icon(Icons.Filled.MusicNote, contentDescription = "Note Settings")
                }
                IconButton(onClick = onOpenEq) {
                    Icon(Icons.Filled.Equalizer, contentDescription = "EQ")
                }
                IconButton(onClick = onOpenFx) {
                    Text("FX", style = MaterialTheme.typography.labelMedium)
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings")
                }
            }
        }
    }
}

@Composable
private fun DebugInfo(
    frequency: Float,
    amplitude: Float,
    waveformA: Waveform,
    waveformB: Waveform,
    waveformBlend: Float,
    spectrum: FloatArray,
    accelerometerHistory: List<Accelerometer>,
    gyroscopeHistory: List<Gyroscope>,
    quaternionHistory: List<RotationVector>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp)
    ) {
        SpectrumAnalyserCanvas(
            spectrum = spectrum,
            modifier = Modifier.fillMaxWidth().height(80.dp)
        )

        SynthParametersCard(
            frequency = frequency,
            amplitude = amplitude,
            waveformA = waveformA,
            waveformB = waveformB,
            blend = waveformBlend,
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
        waveformA = Waveform.SINE,
        waveformB = Waveform.SQUARE,
        waveformBlend = 0f,
        spectrum = FloatArray(512),
        accelerometerHistory = emptyList(),
        gyroscopeHistory = emptyList(),
        quaternionHistory = emptyList(),
        rotationVectorData = null,
        onTouchPad = { _, _ -> },
        onReleasePad = {},
        onTare = {},
        onOpenNoteSettings = {},
        onOpenEq = {},
    )
}