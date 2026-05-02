package ocd.phonetricks.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun NoteSettingsScreen(viewModel: NoteSettingsViewModel, modifier: Modifier = Modifier) {
    val scale by viewModel.scale.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text("Note Settings", style = MaterialTheme.typography.titleMedium)

        HorizontalDivider()

        Text("Scale", style = MaterialTheme.typography.titleMedium)

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val dragAccum = remember { mutableFloatStateOf(0f) }
            Text(
                text = scale.displayName(),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { dragAccum.floatValue = 0f },
                    ) { _, dragAmount ->
                        dragAccum.floatValue += dragAmount.x
                        val steps = (dragAccum.floatValue / 80f).toInt()
                        if (steps != 0) {
                            viewModel.cycleScale(steps)
                            dragAccum.floatValue -= steps * 80f
                        }
                    }
                }
            )
            Text(
                text = "Drag to change",
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

