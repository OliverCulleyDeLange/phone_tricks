package ocd.phonetricks

import androidx.compose.ui.window.ComposeUIViewController
import ocd.phonetricks.sensor.createSensorManager

fun MainViewController() = ComposeUIViewController {
    val sensorManager = createSensorManager()
    App(sensorManager)
}