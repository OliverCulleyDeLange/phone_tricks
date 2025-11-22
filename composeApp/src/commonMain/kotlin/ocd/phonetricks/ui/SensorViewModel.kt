package ocd.phonetricks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ocd.phonetricks.data.SensorData
import ocd.phonetricks.data.TrickEvent
import ocd.phonetricks.data.RotationVector
import ocd.phonetricks.engine.TrickEngine
import ocd.phonetricks.sensor.SensorManager
import kotlin.math.sqrt

class SensorViewModel(sensorManager: SensorManager) : ViewModel() {
    private val engine = TrickEngine(sensorManager, viewModelScope)

    // Tare quaternion - stores the offset rotation to align the model with the device
    private val _tareQuaternion = MutableStateFlow<RotationVector?>(null)

    // Fast updates for visualization (60Hz) - with tare applied
    val sensorData: StateFlow<SensorData?> = engine.currentSensorData.map { data ->
        data?.let { applyTare(it, _tareQuaternion.value) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, engine.currentSensorData.value)

    // Sensor history for graphs - with tare applied
    val sensorHistory: StateFlow<List<SensorData>> = combine(
        engine.sensorHistory,
        _tareQuaternion
    ) { history, tare ->
        if (tare == null) {
            history
        } else {
            history.map { data -> applyTare(data, tare) }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, engine.sensorHistory.value)

    // Detected tricks
    val detectedTricks: StateFlow<List<TrickEvent>> = engine.detectedTricks

    val isRecording: StateFlow<Boolean> = engine.isRecording

    // Replay mode
    private val _isReplaying = MutableStateFlow(false)
    val isReplaying: StateFlow<Boolean> = _isReplaying.asStateFlow()

    // Playback index flow for replay UI (emits 0-based index into `sensorHistory`)
    private val _playbackIndex = MutableStateFlow(0)
    val playbackIndexFlow: StateFlow<Int> = _playbackIndex.asStateFlow()

    // Store the replay snapshot (last 10 seconds)
    private val _replaySnapshot = MutableStateFlow<List<SensorData>>(emptyList())
    val replaySnapshot: StateFlow<List<SensorData>> = _replaySnapshot.asStateFlow()

    fun startRecording() {
        engine.startRecording()
        _isReplaying.value = false
    }

    fun stopRecording() {
        engine.stopRecording()
    }

    fun startReplay() {
        engine.stopRecording() // Pause sensor collection during replay

        // Get the last 10 seconds of data
        val history = sensorHistory.value
        if (history.isEmpty()) return

        val currentTime = history.last().timestampMs
        val tenSecondsAgo = currentTime - 10_000_000_000L // 10 seconds in nanoseconds
        val last10Seconds = history.filter { it.timestampMs >= tenSecondsAgo }

        _replaySnapshot.value = last10Seconds
        _playbackIndex.value = 0
        _isReplaying.value = true
    }

    fun stopReplay() {
        _isReplaying.value = false
        _replaySnapshot.value = emptyList()
        engine.startRecording() // Resume sensor collection
    }

    fun clearHistory() {
        engine.clearHistory()
    }

    fun tare() {
        val currentData = engine.currentSensorData.value
        if (currentData != null) {
            _tareQuaternion.value = currentData.rotationVector
        }
    }

    /**
     * Apply tare quaternion to sensor data's rotation vector
     */
    private fun applyTare(data: SensorData, tareQuat: RotationVector?): SensorData {
        if (tareQuat == null) return data

        val rotVec = data.rotationVector
        val x = rotVec.x
        val y = rotVec.y
        val z = rotVec.z
        val w = rotVec.scalar ?: computeScalar(x.toDouble(), y.toDouble(), z.toDouble())

        val tareX = tareQuat.x
        val tareY = tareQuat.y
        val tareZ = tareQuat.z
        val tareW = tareQuat.scalar ?: computeScalar(tareX.toDouble(), tareY.toDouble(), tareZ.toDouble())

        // Compute inverse of tare quaternion (conjugate for unit quaternions)
        val tareInvX = -tareX
        val tareInvY = -tareY
        val tareInvZ = -tareZ
        val tareInvW = tareW

        // Multiply: result = tareInverse * sensorQuat
        val resultW = tareInvW * w - tareInvX * x - tareInvY * y - tareInvZ * z
        val resultX = tareInvW * x + tareInvX * w + tareInvY * z - tareInvZ * y
        val resultY = tareInvW * y - tareInvX * z + tareInvY * w + tareInvZ * x
        val resultZ = tareInvW * z + tareInvX * y - tareInvY * x + tareInvZ * w

        val taredRotationVector = RotationVector(
            x = resultX,
            y = resultY,
            z = resultZ,
            scalar = resultW
        )

        return data.copy(rotationVector = taredRotationVector)
    }

    /**
     * Compute the scalar (w) component if not provided
     */
    private fun computeScalar(x: Double, y: Double, z: Double): Float {
        val sumSquares = x * x + y * y + z * z
        return if (sumSquares < 1.0) {
            sqrt(1.0 - sumSquares).toFloat()
        } else {
            0f
        }
    }

    init {
        // Start a coroutine to drive playback indices when replaying
        viewModelScope.launch {
            _isReplaying.collect { replaying ->
                if (replaying) {
                    // Snapshot history at start of replay
                    val history = _replaySnapshot.value
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
