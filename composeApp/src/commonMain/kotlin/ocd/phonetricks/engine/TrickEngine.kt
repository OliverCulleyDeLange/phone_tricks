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

    private val maxHistorySize = 600 // 10 seconds at ~60Hz
    private val ringBuffer = RingBuffer<SensorData>(maxHistorySize)

    private val _sensorHistory = MutableStateFlow<List<SensorData>>(emptyList())
    val sensorHistory: StateFlow<List<SensorData>> = _sensorHistory.asStateFlow()

    private val _isRecording = MutableStateFlow(true) // Auto-start recording
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    init {
        // Start listening immediately
        sensorManager.startListening()

        scope.launch {
            sensorManager.sensorDataFlow.collect { data ->
                _currentSensorData.value = data

                if (_isRecording.value) {
                    // Add to ring buffer (automatically handles overflow)
                    ringBuffer.add(data)

                    // Update the state flow with the current buffer contents
                    _sensorHistory.value = ringBuffer.toList()
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
        ringBuffer.clear()
        _sensorHistory.value = emptyList()
    }
}
