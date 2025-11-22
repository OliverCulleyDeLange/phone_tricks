package ocd.phonetricks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ocd.phonetricks.data.TrickType
import ocd.phonetricks.engine.TrickEngine
import ocd.phonetricks.training.BufferStats
import ocd.phonetricks.training.FileWriter
import ocd.phonetricks.utils.currentTimeMillis

class TrainingDataViewModel(
    private val engine: TrickEngine,
    private val fileWriter: FileWriter
) : ViewModel() {

    private val _bufferStats = MutableStateFlow(BufferStats(0, 0, 0f))
    val bufferStats: StateFlow<BufferStats> = _bufferStats.asStateFlow()

    private val _saveDirectory = MutableStateFlow(fileWriter.getSaveDirectory())
    val saveDirectory: StateFlow<String> = _saveDirectory.asStateFlow()

    private val _lastSavedFile = MutableStateFlow("")
    val lastSavedFile: StateFlow<String> = _lastSavedFile.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveCount = MutableStateFlow(0)
    val saveCount: StateFlow<Int> = _saveCount.asStateFlow()

    init {
        // Update buffer stats periodically
        viewModelScope.launch {
            while (true) {
                _bufferStats.value = engine.getBufferStats()
                kotlinx.coroutines.delay(100) // Update 10 times per second
            }
        }
    }

    fun saveSample(trickType: TrickType) {
        viewModelScope.launch {
            _isSaving.value = true

            try {
                // Generate filename with timestamp and label
                val timestamp = currentTimeMillis()
                val filename = "sample_${trickType.name}_${timestamp}.json"

                // Export training data
                val jsonData = engine.exportTrainingData(trickType)

                // Save to file
                val success = fileWriter.saveFile(filename, jsonData)

                if (success) {
                    _lastSavedFile.value = filename
                    _saveCount.value += 1
                    println("✅ Saved training sample: $filename")
                } else {
                    println("❌ Failed to save training sample: $filename")
                }
            } catch (e: Exception) {
                println("❌ Error saving sample: ${e.message}")
                e.printStackTrace()
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun clearBuffer() {
        engine.clearHistory()
        _bufferStats.value = BufferStats(0, 0, 0f)
    }
}
