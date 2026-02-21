package ocd.phonetricks

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import ocd.phonetricks.sensor.SensorManager
import ocd.phonetricks.ui.MainScreen
import ocd.phonetricks.ui.SensorViewModel
import ocd.phonetricks.ui.SynthesizerViewModel
import ocd.phonetricks.audio.createAudioManager
import kotlinx.coroutines.CoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(sensorManager: SensorManager, coroutineScope: CoroutineScope) {
    MaterialTheme {
        val sensorViewModel = remember { SensorViewModel(sensorManager) }
        val audioManager = remember { createAudioManager() }
        val synthesizerViewModel = remember { SynthesizerViewModel(sensorManager, audioManager) }

        Scaffold { paddingValues ->
            Box(Modifier.padding(paddingValues)) {
                MainScreen(sensorViewModel, synthesizerViewModel)
            }
        }
    }
}