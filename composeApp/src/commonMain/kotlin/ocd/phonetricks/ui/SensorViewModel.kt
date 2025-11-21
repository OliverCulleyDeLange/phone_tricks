package ocd.phonetricks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import ocd.phonetricks.data.SensorData
import ocd.phonetricks.data.TrickEvent
import ocd.phonetricks.engine.TrickEngine
import ocd.phonetricks.sensor.SensorManager

class SensorViewModel(sensorManager: SensorManager) : ViewModel() {
    private val engine = TrickEngine(sensorManager, viewModelScope)

    // Fast updates for visualization (60Hz)
    val sensorData: StateFlow<SensorData?> = engine.currentSensorData

    // Sensor history for graphs
    val sensorHistory: StateFlow<List<SensorData>> = engine.sensorHistory

    // Detected tricks
    val detectedTricks: StateFlow<List<TrickEvent>> = engine.detectedTricks

    val isRecording: StateFlow<Boolean> = engine.isRecording

    fun startRecording() {
        engine.startRecording()
    }

    fun stopRecording() {
        engine.stopRecording()
    }

    fun clearHistory() {
        engine.clearHistory()
    }
}
