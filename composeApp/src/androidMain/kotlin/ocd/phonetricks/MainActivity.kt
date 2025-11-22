package ocd.phonetricks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import ocd.phonetricks.sensor.createSensorManager
import ocd.phonetricks.training.createFileWriter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val sensorManager = createSensorManager(this)
        val fileWriter = createFileWriter(this)

        setContent {
            App(sensorManager, fileWriter, lifecycleScope)
        }
    }
}