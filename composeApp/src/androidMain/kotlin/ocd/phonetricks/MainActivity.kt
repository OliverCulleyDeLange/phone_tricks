package ocd.phonetricks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ocd.phonetricks.data.SettingsRepository
import ocd.phonetricks.data.createSettingsStore
import ocd.phonetricks.sensor.createSensorManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val sensorManager = createSensorManager(this)
        val settingsRepository = SettingsRepository(createSettingsStore(this))

        setContent {
            App(sensorManager, settingsRepository)
        }
    }
}