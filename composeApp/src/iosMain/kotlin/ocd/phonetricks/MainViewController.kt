package ocd.phonetricks

import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import ocd.phonetricks.sensor.createSensorManager

fun MainViewController() = ComposeUIViewController {
    val sensorManager = createSensorManager()
    val fileWriter = createFileWriter()
    val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    App(sensorManager, fileWriter, coroutineScope)
}