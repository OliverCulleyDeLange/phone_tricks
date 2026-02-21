package ocd.phonetricks

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import ocd.phonetricks.audio.createAudioManager
import ocd.phonetricks.sensor.SensorManager
import ocd.phonetricks.ui.MainScreen
import ocd.phonetricks.ui.SensorViewModel
import ocd.phonetricks.ui.SettingsScreen
import ocd.phonetricks.ui.SettingsViewModel
import ocd.phonetricks.ui.SynthesizerViewModel

@Composable
fun App(sensorManager: SensorManager, coroutineScope: CoroutineScope) {
    MaterialTheme {
        val navController = rememberNavController()
        val sensorViewModel = remember { SensorViewModel(sensorManager) }
        val audioManager = remember { createAudioManager() }
        val settingsViewModel = remember { SettingsViewModel() }
        val synthesizerViewModel = remember { SynthesizerViewModel(sensorManager, audioManager, settingsViewModel) }

        NavHost(
            navController = navController,
            startDestination = "main",
            modifier = Modifier.fillMaxSize()
        ) {
            composable("main") {
                MainScreen(
                    sensorViewModel = sensorViewModel,
                    synthesizerViewModel = synthesizerViewModel,
                    onOpenSettings = { navController.navigate("settings") },
                )
            }
            composable("settings") {
                SettingsScreen(
                    settingsViewModel = settingsViewModel,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}