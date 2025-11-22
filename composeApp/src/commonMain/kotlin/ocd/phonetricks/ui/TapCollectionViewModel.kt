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
import ocd.phonetricks.training.Labels
import ocd.phonetricks.utils.currentTimeMillis
import ocd.phonetricks.utils.formatTimestampForFilename

enum class CollectionMode {
    POSITIVE,
    NEGATIVE
}

class TapCollectionViewModel(
    private val engine: TrickEngine,
    private val fileWriter: FileWriter
) : ViewModel() {

    private val recordingDurationMs = 20000L

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

    private val _selectedSurfaceTags = MutableStateFlow<Set<String>>(emptySet())
    val selectedSurfaceTags: StateFlow<Set<String>> = _selectedSurfaceTags.asStateFlow()

    private val _selectedTapTags = MutableStateFlow<Set<String>>(emptySet())
    val selectedTapTags: StateFlow<Set<String>> = _selectedTapTags.asStateFlow()

    private val _collectionMode = MutableStateFlow(CollectionMode.POSITIVE)
    val collectionMode: StateFlow<CollectionMode> = _collectionMode.asStateFlow()

    val surfaceTags = listOf("hard", "soft", "held")
    val tapTags = listOf("soft", "medium", "hard", "quick-double", "quick-triple")
    val negativeSampleTags = listOf("waving", "walking", "pocket", "table-vibration", "typing", "scrolling")

    fun toggleSurfaceTag(tag: String) {
        val current = _selectedSurfaceTags.value
        _selectedSurfaceTags.value = if (current.contains(tag)) {
            current - tag
        } else {
            current + tag
        }
    }

    fun toggleTapTag(tag: String) {
        val current = _selectedTapTags.value
        _selectedTapTags.value = if (current.contains(tag)) {
            current - tag
        } else {
            current + tag
        }
    }

    fun setCollectionMode(mode: CollectionMode) {
        if (!_isRecording.value) {
            _collectionMode.value = mode
        }
    }

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
                if (_collectionMode.value == CollectionMode.NEGATIVE || _tapTimestamps.value.isNotEmpty()) {
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
        if (_collectionMode.value == CollectionMode.NEGATIVE) return

        val timestamp = currentTimeMillis()
        _tapTimestamps.value = _tapTimestamps.value + timestamp
    }

    private fun saveSession() {
        viewModelScope.launch {
            _isSaving.value = true

            try {
                val recordingStart = _recordingStartTime.value
                val recordingEnd = currentTimeMillis()
                val tapCount = _tapTimestamps.value.size

                val labels = Labels(
                    sampleType = if (_collectionMode.value == CollectionMode.NEGATIVE) "negative" else "positive",
                    sessionTag = _sessionTag.value,
                    surface = _selectedSurfaceTags.value.toList(),
                    taps = _selectedTapTags.value.toList()
                )

                val timestamp = currentTimeMillis()
                val formattedTimestamp = formatTimestampForFilename(timestamp)
                val sessionTagPart = if (_sessionTag.value.isNotBlank()) "${_sessionTag.value}_" else ""
                val modePrefix = if (_collectionMode.value == CollectionMode.NEGATIVE) "negative_" else ""
                val filename = if (_collectionMode.value == CollectionMode.NEGATIVE) {
                    "${formattedTimestamp}_tap_collection_${modePrefix}${sessionTagPart}.json"
                } else {
                    "${formattedTimestamp}_tap_collection_${sessionTagPart}${tapCount}taps.json"
                }

                val jsonData = engine.exportTrainingDataWithTimestamps(
                    labels = labels,
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
