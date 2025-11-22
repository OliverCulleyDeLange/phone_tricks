package ocd.phonetricks

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import ocd.phonetricks.sensor.SensorManager
import ocd.phonetricks.ui.SensorScreen
import ocd.phonetricks.ui.SensorViewModel
import ocd.phonetricks.ui.TrainingDataScreen
import ocd.phonetricks.ui.TrainingDataViewModel
import ocd.phonetricks.training.FileWriter
import ocd.phonetricks.engine.TrickEngine
import kotlinx.coroutines.CoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(sensorManager: SensorManager, fileWriter: FileWriter, coroutineScope: CoroutineScope) {
    MaterialTheme {
        var selectedTab by remember { mutableStateOf(0) }

        val sensorViewModel = remember { SensorViewModel(sensorManager) }
        val engine = remember { TrickEngine(sensorManager, coroutineScope) }
        val trainingViewModel = remember { TrainingDataViewModel(engine, fileWriter) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            when (selectedTab) {
                                0 -> "Phone Tricks - Sensors"
                                1 -> "Phone Tricks - Training"
                                else -> "Phone Tricks"
                            }
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Sensors, contentDescription = null) },
                        label = { Text("Sensors") },
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.DataUsage, contentDescription = null) },
                        label = { Text("Training") },
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 }
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
                when (selectedTab) {
                    0 -> SensorScreen(sensorViewModel)
                    1 -> TrainingDataScreen(trainingViewModel)
                }
            }
        }
    }
}