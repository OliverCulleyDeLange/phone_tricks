package ocd.phonetricks

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import ocd.phonetricks.data.SettingsRepository
import ocd.phonetricks.data.createSettingsStore
import ocd.phonetricks.sensor.createSensorManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // ActivityCompat.requestPermissions never delivered a result here —
        // we never overrode onRequestPermissionsResult, so a denied prompt
        // left SamplePlayer to fail silently. Use the modern launcher API,
        // which is wired up before STARTED so it survives recreation, and
        // ignore the result for now (the recorder defensively bails on a
        // STATE_UNINITIALIZED AudioRecord).
        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { /* SamplePlayer surfaces failures via isRecording */ }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        val sensorManager = createSensorManager(this)
        val settingsRepository = SettingsRepository(createSettingsStore(this))

        setContent {
            App(sensorManager, settingsRepository)
        }
    }
}

