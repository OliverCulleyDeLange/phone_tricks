package ocd.phonetricks.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ocd.phonetricks.audio.Waveform
import ocd.phonetricks.ui.components.PhoneVisualization3D
import ocd.phonetricks.ui.components.SensorGraph
import kotlin.math.roundToInt

@Composable
fun MainScreen(sensorViewModel: SensorViewModel, synthesizerViewModel: SynthesizerViewModel) {
    val rotationVectorData by sensorViewModel.rotationVectorData.collectAsState()
    val accelerometerHistory by sensorViewModel.accelerometerHistory.collectAsState()
    val gyroscopeHistory by sensorViewModel.gyroscopeHistory.collectAsState()

    val frequency by synthesizerViewModel.baseFrequency.collectAsState()
    val amplitude by synthesizerViewModel.amplitude.collectAsState()
    val waveform by synthesizerViewModel.waveform.collectAsState()
    val isTouchInBox by synthesizerViewModel.isTouchInBox.collectAsState()

    val scrollState = rememberScrollState()
    var isScrollEnabled by remember { mutableStateOf(true) }

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
                        .verticalScroll(scrollState, enabled = isScrollEnabled)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            rotationVectorData?.let { data ->
                                PhoneVisualization3D(
                                    rotationVector = data,
                                    onTareRequest = { sensorViewModel.tare() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(300.dp)
                                )
                            }
                        }
                    }


                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .aspectRatio(1.5f)
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isTouchInBox) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            )
                            .pointerInput(Unit) {
                                val boxSize = this.size

                                // Use direct pointer input handling instead of detectTapGestures
                                awaitPointerEventScope {
                                    while (true) {
                                        // Wait for touch down event
                                        val downEvent = awaitPointerEvent()
                                        val downPointer = downEvent.changes.firstOrNull() ?: continue

                                        if (!downPointer.pressed) continue

                                        // Disable scrolling when touch starts
                                        isScrollEnabled = false

                                        // Calculate normalized coordinates (0.0-1.0)
                                        val position = downPointer.position
                                        var normalizedX = (position.x / boxSize.width).coerceIn(0f, 1f)
                                        var normalizedY = (position.y / boxSize.height).coerceIn(0f, 1f)

                                        // Initial touch position
                                        synthesizerViewModel.onTouchInBox(true, normalizedX, normalizedY)
                                        downPointer.consumeAllChanges()

                                        // Track movement and wait for release
                                        try {
                                            // Continue tracking until released
                                            while (true) {
                                                val moveEvent = awaitPointerEvent()
                                                val movePointer = moveEvent.changes.firstOrNull() ?: break

                                                // If released, break the tracking loop
                                                if (!movePointer.pressed) {
                                                    break
                                                }

                                                // Recalculate normalized position
                                                val movePosition = movePointer.position
                                                normalizedX = (movePosition.x / boxSize.width).coerceIn(0f, 1f)
                                                normalizedY = (movePosition.y / boxSize.height).coerceIn(0f, 1f)

                                                // Update synth parameters based on position
                                                synthesizerViewModel.onTouchInBox(true, normalizedX, normalizedY)
                                                movePointer.consumeAllChanges()
                                            }
                                        } finally {
                                            // ALWAYS make sure sound stops when touch ends
                                            synthesizerViewModel.onTouchInBox(false)
                                            isScrollEnabled = true
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Frequency and volume indicators
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Max Volume",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Low",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                                Text(
                                    "High",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }

                            Text(
                                "Min Volume",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        // Main icon
                        if (isTouchInBox) {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = "Touch Area",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Synth parameters
                    SynthParametersCard(
                        frequency = frequency,
                        amplitude = amplitude,
                        waveform = waveform,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )

                    // Waveform selector button
                    Button(
                        onClick = { synthesizerViewModel.cycleWaveform() },
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Change Waveform",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(waveform.name.lowercase().capitalize())
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sensor graphs section
                    Text(
                        text = "Sensor Data",
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // How to use instructions
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "How to use:",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text("1. Touch pad: X-axis controls frequency (left=low, right=high)")
                            Text("2. Touch pad: Y-axis controls volume (top=loud, bottom=quiet)")
                            Text("3. Change waveform with the button below the pad")
                            Text("4. Move finger while touching to create effects")
                        }
                    }
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

@Composable
fun SynthParametersCard(
    frequency: Float,
    amplitude: Float,
    waveform: Waveform,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Synthesizer Parameters",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Frequency",
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    "${frequency.roundToInt()} Hz",
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Amplitude",
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    "${(amplitude * 100).roundToInt()}%",
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Waveform",
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    waveform.name.lowercase().capitalize(),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

// Helper function to capitalize first letter
private fun String.capitalize(): String {
    return this.replaceFirstChar { it.uppercase() }
}
