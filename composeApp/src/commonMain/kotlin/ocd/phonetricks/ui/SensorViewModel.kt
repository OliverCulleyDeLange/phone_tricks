package ocd.phonetricks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    // Replay mode
    private val _isReplaying = MutableStateFlow(false)
    val isReplaying: StateFlow<Boolean> = _isReplaying.asStateFlow()

    fun startRecording() {
        engine.startRecording()
        _isReplaying.value = false
    }

    fun stopRecording() {
        engine.stopRecording()
    }

    fun startReplay() {
        engine.stopRecording() // Pause sensor collection during replay
        _isReplaying.value = true
    }

    fun stopReplay() {
        _isReplaying.value = false
        engine.startRecording() // Resume sensor collection
    }

    fun clearHistory() {
        engine.clearHistory()
    }
}
