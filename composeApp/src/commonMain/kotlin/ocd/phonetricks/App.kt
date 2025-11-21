package ocd.phonetricks

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import ocd.phonetricks.sensor.SensorManager
import ocd.phonetricks.ui.SensorScreen
import ocd.phonetricks.ui.SensorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(sensorManager: SensorManager) {
    MaterialTheme {
        val viewModel = remember { SensorViewModel(sensorManager) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Phone Tricks") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        ) { paddingValues ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                color = MaterialTheme.colorScheme.background
            ) {
                SensorScreen(viewModel)
            }
        }
    }
}