package ocd.phonetricks.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.time.TimeSource

private const val START_ANGLE = 150f
private const val SWEEP_MAX = 240f
private const val DRAG_SENSITIVITY = 0.004f

@Composable
fun Dial(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    defaultValue: Float = 0.5f,
    valueLabel: String = "",
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val fillColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurface
    val strokeWidth = size.value * 0.12f
    val currentValue by rememberUpdatedState(value)
    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(
            modifier = Modifier
                .size(size)
                .pointerInput(defaultValue) {
                    val clock = TimeSource.Monotonic
                    var lastTapMark = clock.markNow()
                    var firstTap = false
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitPointerEvent(PointerEventPass.Main)
                            val press = down.changes.firstOrNull() ?: continue
                            if (!press.pressed) continue

                            val now = clock.markNow()
                            val isDoubleTap = firstTap && (now - lastTapMark).inWholeMilliseconds < 350L
                            lastTapMark = now
                            firstTap = true

                            var dragged = false
                            press.consume()

                            while (true) {
                                val move = awaitPointerEvent(PointerEventPass.Main)
                                val change = move.changes.firstOrNull() ?: break
                                if (!change.pressed) {
                                    if (!dragged && isDoubleTap) onValueChange(defaultValue)
                                    break
                                }
                                val delta = (change.position.x - change.previousPosition.x -
                                        (change.position.y - change.previousPosition.y)) * DRAG_SENSITIVITY
                                if (kotlin.math.abs(delta) > 0.001f) dragged = true
                                change.consume()
                                onValueChange((currentValue + delta).coerceIn(0f, 1f))
                            }
                        }
                    }
                }
        ) {
            val inset = strokeWidth / 2f
            val arcSize = Size(this.size.width - strokeWidth, this.size.height - strokeWidth)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = trackColor,
                startAngle = START_ANGLE,
                sweepAngle = SWEEP_MAX,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            drawArc(
                color = fillColor,
                startAngle = START_ANGLE,
                sweepAngle = SWEEP_MAX * value,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )

            if (valueLabel.isNotEmpty()) {
                val style = TextStyle(color = labelColor, fontSize = (size.value * 0.18f).sp, textAlign = TextAlign.Center)
                val measured = textMeasurer.measure(valueLabel, style)
                drawText(
                    measured,
                    topLeft = Offset(
                        this.size.width / 2f - measured.size.width / 2f,
                        this.size.height / 2f - measured.size.height / 2f,
                    )
                )
            }
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
        )
    }
}



