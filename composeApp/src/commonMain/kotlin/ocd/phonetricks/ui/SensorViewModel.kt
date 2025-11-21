package ocd.phonetricks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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

    // Playback index flow for replay UI (emits 0-based index into `sensorHistory`)
    private val _playbackIndex = MutableStateFlow(0)
    val playbackIndexFlow: StateFlow<Int> = _playbackIndex.asStateFlow()

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
        // reset playback index
        _playbackIndex.value = 0
    }

    fun stopReplay() {
        _isReplaying.value = false
        engine.startRecording() // Resume sensor collection
    }

    fun clearHistory() {
        engine.clearHistory()
    }

    init {
        // Start a coroutine to drive playback indices when replaying
        viewModelScope.launch {
            _isReplaying.collect { replaying ->
                if (replaying) {
                    // Snapshot history at start of replay
                    val history = sensorHistory.value
                    if (history.isEmpty()) return@collect

                    var idx = 0
                    while (_isReplaying.value && idx < history.size) {
                        _playbackIndex.value = idx
                        delay(16L) // ~60Hz
                        idx++
                    }

                    // Ensure index ends at last element
                    _playbackIndex.value = (history.size - 1).coerceAtLeast(0)
                }
            }
        }
    }
}
