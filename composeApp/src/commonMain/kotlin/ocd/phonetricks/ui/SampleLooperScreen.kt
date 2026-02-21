package ocd.phonetricks.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import ocd.phonetricks.ui.components.Dial
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun SampleLooperScreen(viewModel: SampleViewModel, modifier: Modifier = Modifier) {
    val waveform by viewModel.waveformData.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()

    var smoothPosition by remember { mutableFloatStateOf(0f) }

    // Poll getPlayPosition() every display frame — no StateFlow batching, no extrapolation needed
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                withFrameMillis {
                    smoothPosition = viewModel.getPlayPosition()
                }
            }
        } else {
            smoothPosition = 0f
        }
    }

    var startPoint by remember { mutableFloatStateOf(0f) }
    var endPoint by remember { mutableFloatStateOf(1f) }
    var loopSpeed by remember { mutableFloatStateOf(0.231f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Sample Looper", style = MaterialTheme.typography.titleMedium)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val hasSample by viewModel.hasSample.collectAsState()
                IconButton(
                    onClick = { if (isPlaying) viewModel.stopPlayback() else viewModel.stopRecordingAndPlay() },
                    enabled = hasSample && !isRecording,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Stop" else "Play",
                    )
                }

                IconButton(
                    onClick = {
                        if (isRecording) viewModel.stopRecordingAndPlay()
                        else viewModel.startRecording()
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isRecording) MaterialTheme.colorScheme.error
                                         else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                        contentColor = if (isRecording) MaterialTheme.colorScheme.onError
                                       else MaterialTheme.colorScheme.error,
                    ),
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                        contentDescription = if (isRecording) "Stop recording" else "Record",
                    )
                }
            }
        }

        WaveformEditor(
            waveform = waveform,
            startPoint = startPoint,
            endPoint = endPoint,
            playPosition = smoothPosition,
            onStartChange = { startPoint = it; viewModel.setStartPoint(it) },
            onEndChange = { endPoint = it; viewModel.setEndPoint(it) },
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(horizontal = 24.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val speedRaw = 0.1f + loopSpeed * 3.9f
            val speedLabel = "${(speedRaw * 10).roundToInt() / 10f}x"
            Dial(
                label = "Speed",
                value = loopSpeed,
                valueLabel = speedLabel,
                defaultValue = 0.231f,
                onValueChange = { loopSpeed = it; viewModel.setLoopSpeed(0.1f + it * 3.9f) },
            )

            val startPct = "${(startPoint * 100).roundToInt()}%"
            Dial(
                label = "Start",
                value = startPoint,
                valueLabel = startPct,
                defaultValue = 0f,
                onValueChange = { v ->
                    startPoint = v.coerceIn(0f, endPoint - 0.01f)
                    viewModel.setStartPoint(startPoint)
                },
            )

            val endPct = "${(endPoint * 100).roundToInt()}%"
            Dial(
                label = "End",
                value = endPoint,
                valueLabel = endPct,
                defaultValue = 1f,
                onValueChange = { v ->
                    endPoint = v.coerceIn(startPoint + 0.01f, 1f)
                    viewModel.setEndPoint(endPoint)
                },
            )
        }
    }
}

@Composable
private fun WaveformEditor(
    waveform: FloatArray,
    startPoint: Float,
    endPoint: Float,
    playPosition: Float,
    onStartChange: (Float) -> Unit,
    onEndChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val waveColor = MaterialTheme.colorScheme.primary
    val regionColor = MaterialTheme.colorScheme.secondary
    val handleColor = MaterialTheme.colorScheme.secondary
    val dimColor = MaterialTheme.colorScheme.surfaceVariant
    val markerColor = MaterialTheme.colorScheme.tertiary

    var draggingStart by remember { mutableFloatStateOf(-1f) }
    var draggingEnd by remember { mutableFloatStateOf(-1f) }

    Canvas(
        modifier = modifier.pointerInput(startPoint, endPoint) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    val change = event.changes.firstOrNull() ?: continue
                    val x = change.position.x
                    val norm = (x / size.width).coerceIn(0f, 1f)
                    val startX = startPoint * size.width
                    val endX = endPoint * size.width

                    if (change.pressed) {
                        if (draggingStart < 0f && draggingEnd < 0f) {
                            draggingStart = if (abs(x - startX) < 56f) x else -1f
                            draggingEnd = if (draggingStart < 0f && abs(x - endX) < 56f) x else -1f
                        }
                        if (draggingStart >= 0f) {
                            change.consume()
                            onStartChange(norm.coerceIn(0f, endPoint - 0.01f))
                        } else if (draggingEnd >= 0f) {
                            change.consume()
                            onEndChange(norm.coerceIn(startPoint + 0.01f, 1f))
                        }
                    } else {
                        draggingStart = -1f
                        draggingEnd = -1f
                    }
                }
            }
        }
    ) {
        val w = size.width
        val h = size.height
        val midY = h / 2f
        val sx = startPoint * w
        val ex = endPoint * w

        drawRect(dimColor.copy(alpha = 0.3f), topLeft = Offset(0f, 0f), size = Size(sx, h))
        drawRect(dimColor.copy(alpha = 0.3f), topLeft = Offset(ex, 0f), size = Size(w - ex, h))
        drawRect(regionColor.copy(alpha = 0.08f), topLeft = Offset(sx, 0f), size = Size(ex - sx, h))

        if (waveform.isNotEmpty()) {
            val path = Path()
            val step = w / waveform.size
            waveform.forEachIndexed { i, v ->
                val x = i * step
                val amp = v * midY * 0.9f
                if (i == 0) path.moveTo(x, midY - amp) else path.lineTo(x, midY - amp)
            }
            waveform.indices.reversed().forEach { i ->
                val x = i * step
                path.lineTo(x, midY + waveform[i] * midY * 0.9f)
            }
            path.close()
            drawPath(path, waveColor.copy(alpha = 0.5f))
        } else {
            drawLine(waveColor.copy(alpha = 0.3f), Offset(0f, midY), Offset(w, midY), strokeWidth = 1f)
        }

        val markerX = sx + playPosition * (ex - sx)
        drawLine(markerColor, Offset(markerX, 0f), Offset(markerX, h), strokeWidth = 2f)
        drawCircle(markerColor, radius = 6f, center = Offset(markerX, midY))

        drawLine(handleColor, Offset(sx, 0f), Offset(sx, h), strokeWidth = 2.5f)
        drawCircle(handleColor, radius = 14f, center = Offset(sx, h * 0.15f))
        drawCircle(Color.Black.copy(alpha = 0.4f), radius = 5f, center = Offset(sx, h * 0.15f))

        drawLine(handleColor, Offset(ex, 0f), Offset(ex, h), strokeWidth = 2.5f)
        drawCircle(handleColor, radius = 14f, center = Offset(ex, h * 0.85f))
        drawCircle(Color.Black.copy(alpha = 0.4f), radius = 5f, center = Offset(ex, h * 0.85f))
    }
}


