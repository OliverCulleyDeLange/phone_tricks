package ocd.phonetricks.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun TouchPad(
    onTouch: (Float, Float) -> Unit,
    onUnTouch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isTouching = remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .padding(12.dp)
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(12.dp)
            )
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .background(
                if (isTouching.value) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .pointerInput(Unit) {
                val boxSize = this.size

                awaitPointerEventScope {
                    while (true) {
                        val downEvent = awaitPointerEvent()
                        val downPointer = downEvent.changes.firstOrNull() ?: continue

                        if (!downPointer.pressed) continue

                        val position = downPointer.position
                        var normalizedX = (position.x / boxSize.width).coerceIn(0f, 1f)
                        var normalizedY = (1f - position.y / boxSize.height).coerceIn(0f, 1f)

                        onTouch(normalizedX, normalizedY)
                        isTouching.value = true
                        downPointer.consumeAllChanges()

                        try {
                            while (true) {
                                val moveEvent = awaitPointerEvent()
                                val movePointer = moveEvent.changes.firstOrNull() ?: break

                                if (!movePointer.pressed) {
                                    break
                                }

                                val movePosition = movePointer.position
                                normalizedX = (movePosition.x / boxSize.width).coerceIn(0f, 1f)
                                normalizedY = (1f - movePosition.y / boxSize.height).coerceIn(0f, 1f)

                                onTouch(normalizedX, normalizedY)
                                isTouching.value = true
                                movePointer.consumeAllChanges()
                            }
                        } finally {
                            onUnTouch()
                            isTouching.value = false
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ){}
}