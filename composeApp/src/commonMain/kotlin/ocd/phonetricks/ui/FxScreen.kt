package ocd.phonetricks.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ocd.phonetricks.audio.AudioEffect
import ocd.phonetricks.ui.components.Dial

@Composable
fun FxScreen(fxViewModel: FxViewModel, modifier: Modifier = Modifier) {
    val effectWetDry by fxViewModel.effectWetDry.collectAsState()
    val filterPreset by fxViewModel.filterPreset.collectAsState()
    val filterFrequency by fxViewModel.filterFrequency.collectAsState()
    val filterWetDry by fxViewModel.filterWetDry.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text("Effects", style = MaterialTheme.typography.titleMedium)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            AudioEffect.entries.take(3).forEach { effect ->
                Dial(
                    label = effect.displayName(),
                    value = effectWetDry[effect] ?: 0f,
                    onValueChange = { fxViewModel.setEffectWetDry(effect, it) },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            AudioEffect.entries.drop(3).forEach { effect ->
                Dial(
                    label = effect.displayName(),
                    value = effectWetDry[effect] ?: 0f,
                    onValueChange = { fxViewModel.setEffectWetDry(effect, it) },
                )
            }
        }

        HorizontalDivider()

        Text("Filter", style = MaterialTheme.typography.titleMedium)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val dragAccum = remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
                Text(
                    text = filterPreset.displayName(),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { dragAccum.floatValue = 0f },
                        ) { _, dragAmount ->
                            dragAccum.floatValue += dragAmount.x - dragAmount.y
                            val steps = (dragAccum.floatValue / 80f).toInt()
                            if (steps != 0) {
                                fxViewModel.cycleFilterPreset(steps)
                                dragAccum.floatValue -= steps * 80f
                            }
                        }
                    }
                )
                Text(
                    text = "Preset",
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                )
            }

            val freqNorm = ((filterFrequency - 20f) / (20000f - 20f)).coerceIn(0f, 1f)
            Dial(
                label = "Frequency",
                value = freqNorm,
                onValueChange = { norm ->
                    fxViewModel.setFilterFrequency(20f + norm * (20000f - 20f))
                },
            )

            Dial(
                label = "Mix",
                value = filterWetDry,
                onValueChange = { fxViewModel.setFilterWetDry(it) },
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}



