package ocd.phonetricks.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ocd.phonetricks.data.SensorData
import ocd.phonetricks.data.TrickEvent
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

    // Trick detection
    private val trickDetector = TrickDetector()
    private val tapDetector = TapDetector()

    private val _detectedTricks = MutableStateFlow<List<TrickEvent>>(emptyList())
    val detectedTricks: StateFlow<List<TrickEvent>> = _detectedTricks.asStateFlow()

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

                    // Detect tricks - pass the ring buffer to detectors
                    val newTricks = trickDetector.processSensorData(ringBuffer)

                    // Detect taps - pass the ring buffer to detectors
                    val newTaps = tapDetector.processSensorData(ringBuffer)

                    // Combine and add all detected events
                    val allEvents = newTricks + newTaps
                    if (allEvents.isNotEmpty()) {
                        _detectedTricks.value = _detectedTricks.value + allEvents
                    }
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
        _detectedTricks.value = emptyList()
        trickDetector.reset()
        tapDetector.reset()
    }
}
