package ocd.phonetricks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ocd.phonetricks.data.SensorData
import ocd.phonetricks.engine.TrickEngine
import ocd.phonetricks.sensor.SensorManager

class SensorViewModel(sensorManager: SensorManager) : ViewModel() {
    private val engine = TrickEngine(sensorManager, viewModelScope)

    // Fast updates for visualization (60Hz)
    val sensorData: StateFlow<SensorData?> = engine.currentSensorData

    // Throttled updates for text display (10Hz)
    private val _throttledSensorData = MutableStateFlow<SensorData?>(null)
    val throttledSensorData: StateFlow<SensorData?> = _throttledSensorData.asStateFlow()

    val isRecording: StateFlow<Boolean> = engine.isRecording

    init {
        // Throttle sensor data updates for text display
        viewModelScope.launch {
            engine.currentSensorData.collect { data ->
                _throttledSensorData.value = data
                delay(100) // Update text every 100ms (10Hz)
            }
        }
    }

    fun startRecording() {
        engine.startRecording()
    }

    fun stopRecording() {
        engine.stopRecording()
    }
}
