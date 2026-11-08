package ocd.phonetricks

import androidx.compose.ui.window.ComposeUIViewController
import ocd.phonetricks.data.SettingsRepository
import ocd.phonetricks.data.createSettingsStore
import ocd.phonetricks.sensor.createSensorManager

fun MainViewController() = ComposeUIViewController {
    val sensorManager = createSensorManager(null)
    val settingsRepository = SettingsRepository(createSettingsStore(null))

    App(sensorManager, settingsRepository)
}