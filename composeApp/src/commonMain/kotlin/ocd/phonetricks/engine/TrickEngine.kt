package ocd.phonetricks.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ocd.phonetricks.data.SensorData
import ocd.phonetricks.sensor.SensorManager

class TrickEngine(
    private val sensorManager: SensorManager,
    private val scope: CoroutineScope
) {
    private val _currentSensorData = MutableStateFlow<SensorData?>(null)
    val currentSensorData: StateFlow<SensorData?> = _currentSensorData.asStateFlow()

    private val _sensorHistory = MutableStateFlow<List<SensorData>>(emptyList())
    val sensorHistory: StateFlow<List<SensorData>> = _sensorHistory.asStateFlow()

    private val maxHistorySize = 600 // 10 seconds at ~60Hz

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    init {
        scope.launch {
            sensorManager.sensorDataFlow.collect { data ->
                _currentSensorData.value = data

                if (_isRecording.value) {
                    val currentHistory = _sensorHistory.value.toMutableList()
                    currentHistory.add(data)

                    // Keep only the most recent data up to maxHistorySize
                    if (currentHistory.size > maxHistorySize) {
                        currentHistory.removeAt(0)
                    }

                    _sensorHistory.value = currentHistory
                }
            }
        }
    }

    fun startRecording() {
        _isRecording.value = true
        sensorManager.startListening()
    }

    fun stopRecording() {
        _isRecording.value = false
        sensorManager.stopListening()
    }

    fun clearHistory() {
        _sensorHistory.value = emptyList()
    }
}
