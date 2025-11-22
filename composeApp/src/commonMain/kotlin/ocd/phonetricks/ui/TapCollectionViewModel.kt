package ocd.phonetricks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import ocd.phonetricks.engine.TrickEngine
import ocd.phonetricks.training.FileWriter
import ocd.phonetricks.utils.currentTimeMillis

class TapCollectionViewModel(
    private val engine: TrickEngine,
    private val fileWriter: FileWriter
) : ViewModel() {

    private val recordingDurationMs = 10000L

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _tapTimestamps = MutableStateFlow<List<Long>>(emptyList())
    val tapTimestamps: StateFlow<List<Long>> = _tapTimestamps.asStateFlow()

    private val _recordingStartTime = MutableStateFlow(0L)
    val recordingStartTime: StateFlow<Long> = _recordingStartTime.asStateFlow()

    private val _timeRemainingMs = MutableStateFlow(0L)
    val timeRemainingMs: StateFlow<Long> = _timeRemainingMs.asStateFlow()

    private val _savedSessionCount = MutableStateFlow(0)
    val savedSessionCount: StateFlow<Int> = _savedSessionCount.asStateFlow()

    private val _lastSavedFile = MutableStateFlow("")
    val lastSavedFile: StateFlow<String> = _lastSavedFile.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _sessionTag = MutableStateFlow("")
    val sessionTag: StateFlow<String> = _sessionTag.asStateFlow()

    fun updateSessionTag(tag: String) {
        _sessionTag.value = tag
    }

    fun startRecording() {
        if (_isRecording.value) return

        engine.clearHistory()
        _tapTimestamps.value = emptyList()
        _recordingStartTime.value = currentTimeMillis()
        _isRecording.value = true

        viewModelScope.launch {
            val startTime = currentTimeMillis()
            while (_isRecording.value && (currentTimeMillis() - startTime) < recordingDurationMs) {
                _timeRemainingMs.value = recordingDurationMs - (currentTimeMillis() - startTime)
                delay(100)
            }
            if (_isRecording.value) {
                stopRecording()
                if (_tapTimestamps.value.isNotEmpty()) {
                    saveSession()
                }
            }
        }
    }

    private fun stopRecording() {
        _isRecording.value = false
        _timeRemainingMs.value = 0
    }

    fun recordTap() {
        if (!_isRecording.value) return

        val timestamp = currentTimeMillis()
        _tapTimestamps.value = _tapTimestamps.value + timestamp
    }

    private fun saveSession() {
        if (_tapTimestamps.value.isEmpty()) {
            println("No taps recorded, not saving")
            return
        }

        viewModelScope.launch {
            _isSaving.value = true

            try {
                val recordingStart = _recordingStartTime.value
                val recordingEnd = currentTimeMillis()
                val tapCount = _tapTimestamps.value.size

                val timestamp = currentTimeMillis()
                val filename = "tap_collection_${_sessionTag.value}_${tapCount}taps_${timestamp}.json"

                val jsonData = engine.exportTrainingDataWithTimestamps(
                    label = _sessionTag.value,
                    tapTimestamps = _tapTimestamps.value,
                    recordingStartMs = recordingStart,
                    recordingEndMs = recordingEnd
                )

                val success = fileWriter.saveFile(filename, jsonData)

                if (success) {
                    _lastSavedFile.value = filename
                    _savedSessionCount.value += 1
                    println("✅ Saved tap collection session: $filename")

                    _tapTimestamps.value = emptyList()
                } else {
                    println("❌ Failed to save tap collection session: $filename")
                }
            } catch (e: Exception) {
                println("❌ Error saving session: ${e.message}")
                e.printStackTrace()
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun clearSession() {
        _tapTimestamps.value = emptyList()
        _isRecording.value = false
        _timeRemainingMs.value = 0
        engine.clearHistory()
    }
}
