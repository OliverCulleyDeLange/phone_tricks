package ocd.phonetricks.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ocd.phonetricks.audio.EqBand
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val MIN_FREQ = 20f
private const val MAX_FREQ = 20000f
private const val MAX_GAIN_DB = 24f
private const val FFT_SIZE = 2048
private const val SAMPLE_RATE = 44100f

private fun freqToX(freq: Float, width: Float): Float {
    val logMin = log10(MIN_FREQ.toDouble())
    val logMax = log10(MAX_FREQ.toDouble())
    return ((log10(freq.toDouble()) - logMin) / (logMax - logMin) * width).toFloat()
        .coerceIn(0f, width)
}

private fun xToFreq(x: Float, width: Float): Float {
    val logMin = log10(MIN_FREQ.toDouble())
    val logMax = log10(MAX_FREQ.toDouble())
    return 10.0.pow(x / width * (logMax - logMin) + logMin).toFloat()
}

private fun gainToY(gainDb: Float, height: Float): Float =
    (0.5f - gainDb / (MAX_GAIN_DB * 2)) * height

private fun yToGain(y: Float, height: Float): Float =
    (0.5f - y / height) * (MAX_GAIN_DB * 2)

@Composable
fun EqScreen(viewModel: EqViewModel, modifier: Modifier = Modifier) {
    val bands by viewModel.bands.collectAsState()
    val spectrum by viewModel.spectrum.collectAsState()
    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("EQ / Spectrum", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { viewModel.addBand() }) {
                Icon(Icons.Filled.Add, contentDescription = "Add EQ band")
            }
        }

        EqCanvas(
            spectrum = spectrum,
            bands = bands,
            textMeasurer = textMeasurer,
            onBandMoved = { id, freq, gain -> viewModel.updateBand(id, freq, gain) },
            onBandRemoved = { id -> viewModel.removeBand(id) },
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp),
        )

        Text(
            "Drag bands to adjust • Double-tap to remove",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun SpectrumAnalyserCanvas(
    spectrum: FloatArray,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier) {
        drawSpectrumGrid(surfaceVariant, onSurface, textMeasurer)
        drawSpectrumBars(spectrum, primaryColor)
    }
}

@Composable
private fun EqCanvas(
    spectrum: FloatArray,
    bands: List<EqBand>,
    textMeasurer: TextMeasurer,
    onBandMoved: (id: Int, freq: Float, gain: Float) -> Unit,
    onBandRemoved: (id: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val secondary = MaterialTheme.colorScheme.secondary

    var dragTargetId by remember { mutableStateOf<Int?>(null) }
    var lastTapId by remember { mutableStateOf<Int?>(null) }
    var lastTapTime by remember { mutableStateOf(0L) }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(bands) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitPointerEvent(PointerEventPass.Main)
                            val press = down.changes.firstOrNull() ?: continue
                            if (!press.pressed) continue

                            val downPos = press.position
                            val downTime = System.currentTimeMillis()

                            val hitId = bands.minByOrNull { band ->
                                val bx = freqToX(band.frequency, size.width.toFloat())
                                val by = gainToY(band.gainDb, size.height.toFloat())
                                abs(downPos.x - bx) + abs(downPos.y - by)
                            }?.takeIf { band ->
                                val bx = freqToX(band.frequency, size.width.toFloat())
                                val by = gainToY(band.gainDb, size.height.toFloat())
                                abs(downPos.x - bx) < 64f && abs(downPos.y - by) < 64f
                            }?.id

                            if (hitId == null) continue

                            press.consume()
                            dragTargetId = hitId

                            var wasDrag = false
                            var dragging = true
                            while (dragging) {
                                val event = awaitPointerEvent(PointerEventPass.Main)
                                val change = event.changes.firstOrNull() ?: break
                                if (!change.pressed) {
                                    dragging = false
                                    dragTargetId = null

                                    val elapsed = System.currentTimeMillis() - downTime
                                    val moved = abs(change.position.x - downPos.x) + abs(change.position.y - downPos.y)
                                    val wasTap = !wasDrag && elapsed < 250L && moved < 20f

                                    if (wasTap) {
                                        val now = System.currentTimeMillis()
                                        if (hitId == lastTapId && now - lastTapTime < 500L) {
                                            onBandRemoved(hitId)
                                            lastTapId = null
                                        } else {
                                            lastTapId = hitId
                                            lastTapTime = now
                                        }
                                    }
                                } else {
                                    val moved = abs(change.position.x - downPos.x) + abs(change.position.y - downPos.y)
                                    if (moved > 10f) wasDrag = true
                                    change.consume()
                                    val freq = xToFreq(change.position.x, size.width.toFloat())
                                    val gain = yToGain(change.position.y, size.height.toFloat())
                                    onBandMoved(hitId, freq, gain)
                                }
                            }
                        }
                    }
                }
        ) {
            drawEqGrid(surfaceVariant, onSurface, textMeasurer)
            drawSpectrumBars(spectrum, primaryColor.copy(alpha = 0.35f))
            drawEqCurve(bands, secondary)
            bands.forEach { band ->
                drawBandHandle(band, if (band.id == dragTargetId) secondary else onSurface, textMeasurer)
            }
        }
    }
}

private fun DrawScope.drawEqGrid(lineColor: Color, labelColor: Color, textMeasurer: TextMeasurer) {
    val labelStyle = TextStyle(color = labelColor.copy(alpha = 0.6f), fontSize = 9.sp)
    val freqLabels = listOf(50f to "50", 100f to "100", 200f to "200", 500f to "500",
        1000f to "1k", 2000f to "2k", 5000f to "5k", 10000f to "10k", 20000f to "20k")

    freqLabels.forEach { (freq, label) ->
        val x = freqToX(freq, size.width)
        drawLine(lineColor.copy(alpha = 0.25f), Offset(x, 0f), Offset(x, size.height - 14.dp.toPx()), strokeWidth = 1f)
        val measured = textMeasurer.measure(label, labelStyle)
        drawText(measured, topLeft = Offset(x - measured.size.width / 2f, size.height - measured.size.height))
    }

    val dbLabels = listOf(-18f to "-18", -12f to "-12", -6f to "-6", 0f to "0",
        6f to "+6", 12f to "+12", 18f to "+18")
    dbLabels.forEach { (db, label) ->
        val y = gainToY(db, size.height)
        drawLine(
            if (db == 0f) lineColor.copy(alpha = 0.6f) else lineColor.copy(alpha = 0.2f),
            Offset(0f, y), Offset(size.width, y),
            strokeWidth = if (db == 0f) 1.5f else 1f,
        )
        val measured = textMeasurer.measure(label, labelStyle)
        drawText(measured, topLeft = Offset(2.dp.toPx(), y - measured.size.height / 2f))
    }
}

private fun DrawScope.drawSpectrumGrid(lineColor: Color, labelColor: Color, textMeasurer: TextMeasurer) {
    val labelStyle = TextStyle(color = labelColor.copy(alpha = 0.5f), fontSize = 9.sp)
    val freqLabels = listOf(100f to "100", 500f to "500", 1000f to "1k", 5000f to "5k", 10000f to "10k")
    freqLabels.forEach { (freq, label) ->
        val x = freqToX(freq, size.width)
        drawLine(lineColor.copy(alpha = 0.2f), Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
        val measured = textMeasurer.measure(label, labelStyle)
        drawText(measured, topLeft = Offset(x - measured.size.width / 2f, 2.dp.toPx()))
    }
}

private fun DrawScope.drawSpectrumBars(spectrum: FloatArray, color: Color) {
    if (spectrum.isEmpty()) return
    val path = Path()
    val fillPath = Path()
    fillPath.moveTo(0f, size.height)

    // Each bin k corresponds to frequency k * SAMPLE_RATE / FFT_SIZE
    // Map that frequency to log-scale x position
    for (k in spectrum.indices) {
        val binFreq = k * SAMPLE_RATE / FFT_SIZE
        if (binFreq < MIN_FREQ || binFreq > MAX_FREQ) continue
        val x = freqToX(binFreq, size.width)
        val y = size.height * (1f - spectrum[k])
        if (path.isEmpty) {
            path.moveTo(x, y)
            fillPath.lineTo(x, y)
        } else {
            path.lineTo(x, y)
            fillPath.lineTo(x, y)
        }
    }
    fillPath.lineTo(size.width, size.height)
    fillPath.close()
    drawPath(fillPath, color.copy(alpha = 0.15f))
    drawPath(path, color, style = Stroke(width = 1.5f))
}

private fun DrawScope.drawEqCurve(bands: List<EqBand>, color: Color) {
    if (bands.isEmpty()) return
    val path = Path()
    val steps = size.width.toInt().coerceAtMost(512)
    for (i in 0..steps) {
        val x = i.toFloat() / steps * size.width
        val freq = xToFreq(x, size.width)
        var totalDb = 0f
        bands.forEach { band ->
            // Peaking EQ magnitude response: standard biquad formula
            val w = 2f * PI.toFloat() * freq / SAMPLE_RATE
            val w0 = 2f * PI.toFloat() * band.frequency / SAMPLE_RATE
            val A = 10f.toDouble().pow(band.gainDb / 40.0).toFloat()
            val alpha = sin(w0) / (2f * band.q)
            val cosW = cos(w.toDouble()).toFloat()
            val cosW0 = cos(w0.toDouble()).toFloat()
            // Evaluate H(e^jw) numerically
            val b0 = 1f + alpha * A
            val b1 = -2f * cosW0
            val b2 = 1f - alpha * A
            val a0 = 1f + alpha / A
            val a1n = -2f * cosW0
            val a2n = 1f - alpha / A
            val ejwRe = cos(w.toDouble()).toFloat()
            val ejwIm = -sin(w.toDouble()).toFloat()
            val ej2wRe = ejwRe * ejwRe - ejwIm * ejwIm
            val ej2wIm = 2f * ejwRe * ejwIm
            val numRe = b0 + b1 * ejwRe + b2 * ej2wRe
            val numIm = b1 * ejwIm + b2 * ej2wIm
            val denRe = a0 + a1n * ejwRe + a2n * ej2wRe
            val denIm = a1n * ejwIm + a2n * ej2wIm
            val numMag = sqrt((numRe * numRe + numIm * numIm).toDouble()).toFloat()
            val denMag = sqrt((denRe * denRe + denIm * denIm).toDouble()).toFloat()
            val mag = if (denMag > 0f) numMag / denMag else 1f
            totalDb += 20f * log10(mag.coerceAtLeast(1e-6f).toDouble()).toFloat()
        }
        val y = gainToY(totalDb.coerceIn(-MAX_GAIN_DB, MAX_GAIN_DB), size.height)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path, color, style = Stroke(width = 2.5f))
}

private fun DrawScope.drawBandHandle(band: EqBand, color: Color, textMeasurer: TextMeasurer) {
    val x = freqToX(band.frequency, size.width)
    val y = gainToY(band.gainDb, size.height)
    drawCircle(color = color.copy(alpha = 0.25f), radius = 26f, center = Offset(x, y))
    drawCircle(color = color, radius = 8f, center = Offset(x, y))
    drawLine(color.copy(alpha = 0.4f), Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
    val freqLabel = if (band.frequency >= 1000f) "${"%.1f".format(band.frequency / 1000f)}k" else "${"%.0f".format(band.frequency)}"
    val gainLabel = "${"%.1f".format(band.gainDb)}dB"
    val labelStyle = TextStyle(color = color, fontSize = 9.sp)
    val fl = textMeasurer.measure(freqLabel, labelStyle)
    val gl = textMeasurer.measure(gainLabel, labelStyle)
    val labelY = (y - 36f).coerceAtLeast(2f)
    drawText(fl, topLeft = Offset(x - fl.size.width / 2f, labelY))
    drawText(gl, topLeft = Offset(x - gl.size.width / 2f, labelY + fl.size.height))
}


