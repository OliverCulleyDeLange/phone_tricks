package ocd.phonetricks

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import ocd.phonetricks.sensor.SensorManager
import ocd.phonetricks.ui.SensorScreen
import ocd.phonetricks.ui.SensorViewModel
import ocd.phonetricks.ui.TrainingDataScreen
import ocd.phonetricks.ui.TrainingDataViewModel
import ocd.phonetricks.ui.TapCollectionScreen
import ocd.phonetricks.ui.TapCollectionViewModel
import ocd.phonetricks.training.FileWriter
import ocd.phonetricks.engine.TrickEngine
import kotlinx.coroutines.CoroutineScope

enum class AppScreen(val title: String) {
    SENSORS("Phone Tricks - Sensors"),
    TRAINING("Phone Tricks - Training"),
    TAP_COLLECTION("Front Tap Collection")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(sensorManager: SensorManager, fileWriter: FileWriter, coroutineScope: CoroutineScope) {
    MaterialTheme {
        var currentScreen by remember { mutableStateOf(AppScreen.SENSORS) }

        val sensorViewModel = remember { SensorViewModel(sensorManager) }
        val engine = remember { TrickEngine(sensorManager, coroutineScope) }
        val trainingViewModel = remember { TrainingDataViewModel(engine, fileWriter) }
        val tapCollectionViewModel = remember { TapCollectionViewModel(engine, fileWriter) }

        Scaffold(
            topBar = {
                if (currentScreen != AppScreen.TAP_COLLECTION) {
                    TopAppBar(
                        title = { Text(currentScreen.title) },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Sensors, contentDescription = null) },
                        label = { Text("Sensors") },
                        selected = currentScreen == AppScreen.SENSORS,
                        onClick = { currentScreen = AppScreen.SENSORS }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.DataUsage, contentDescription = null) },
                        label = { Text("Training") },
                        selected = currentScreen == AppScreen.TRAINING,
                        onClick = { currentScreen = AppScreen.TRAINING }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.TouchApp, contentDescription = null) },
                        label = { Text("Tap Data") },
                        selected = currentScreen == AppScreen.TAP_COLLECTION,
                        onClick = { currentScreen = AppScreen.TAP_COLLECTION }
                    )
                }
            }
        ) { paddingValues ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                color = MaterialTheme.colorScheme.background
            ) {
                when (currentScreen) {
                    AppScreen.SENSORS -> SensorScreen(sensorViewModel)
                    AppScreen.TRAINING -> TrainingDataScreen(trainingViewModel)
                    AppScreen.TAP_COLLECTION -> TapCollectionScreen(tapCollectionViewModel)
                }
            }
        }
    }
}