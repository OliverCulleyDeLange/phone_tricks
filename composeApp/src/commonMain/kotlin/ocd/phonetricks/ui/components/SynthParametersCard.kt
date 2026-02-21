package ocd.phonetricks.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ocd.phonetricks.audio.Waveform
import kotlin.math.roundToInt

@Composable
fun SynthParametersCard(
    frequency: Float,
    amplitude: Float,
    waveformA: Waveform,
    waveformB: Waveform,
    blend: Float,
    modifier: Modifier = Modifier
) {
    val waveformLabel = if (waveformA == waveformB || blend <= 0f) {
        waveformA.name
    } else if (blend >= 1f) {
        waveformB.name
    } else {
        "${waveformA.name}→${waveformB.name} ${(blend * 100).roundToInt()}%"
    }
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            "${frequency.roundToInt()} Hz $waveformLabel @ ${(amplitude * 100).roundToInt()}%",
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}