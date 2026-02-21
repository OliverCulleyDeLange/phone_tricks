package ocd.phonetricks

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import ocd.phonetricks.data.SettingsRepository
import ocd.phonetricks.data.createSettingsStore
import ocd.phonetricks.sensor.createSensorManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }

        val sensorManager = createSensorManager(this)
        val settingsRepository = SettingsRepository(createSettingsStore(this))

        setContent {
            App(sensorManager, settingsRepository)
        }
    }
}

