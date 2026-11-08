package ocd.phonetricks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ocd.phonetricks.audio.AudioManager
import ocd.phonetricks.audio.EqBand
import ocd.phonetricks.data.SettingsRepository

class EqViewModel(
    private val audioManager: AudioManager,
    private val repository: SettingsRepository,
) : ViewModel() {

    private val defaultBands = listOf(
        EqBand(id = 1, frequency = 200f, gainDb = 0f),
        EqBand(id = 2, frequency = 1000f, gainDb = 0f),
        EqBand(id = 3, frequency = 8000f, gainDb = 0f),
    )

    private val _bands = MutableStateFlow(repository.loadEqBands().ifEmpty { defaultBands })
    val bands: StateFlow<List<EqBand>> = _bands.asStateFlow()

    private val _spectrum = MutableStateFlow(FloatArray(512))
    val spectrum: StateFlow<FloatArray> = _spectrum.asStateFlow()

    private var nextId = (_bands.value.maxOfOrNull { it.id } ?: 0) + 1

    init {
        applyBands()
        // The spectrum is consumed by both the EQ sheet and the always-visible
        // debug overlay on MainScreen, so polling runs for the lifetime of
        // the ViewModel. viewModelScope cancels the loop on onCleared().
        viewModelScope.launch {
            while (isActive) {
                _spectrum.value = audioManager.getSpectrumData()
                delay(50)
            }
        }
    }

    fun addBand(frequency: Float = 1000f, gainDb: Float = 0f) {
        val band = EqBand(id = nextId++, frequency = frequency, gainDb = gainDb)
        _bands.value = _bands.value + band
        applyBands()
        save()
    }

    fun removeBand(id: Int) {
        _bands.value = _bands.value.filter { it.id != id }
        applyBands()
        save()
    }

    fun updateBand(id: Int, frequency: Float, gainDb: Float) {
        _bands.value = _bands.value.map {
            if (it.id == id) it.copy(frequency = frequency.coerceIn(20f, 20000f), gainDb = gainDb.coerceIn(-24f, 24f))
            else it
        }
        applyBands()
        save()
    }

    private fun applyBands() {
        audioManager.setEqBands(_bands.value)
    }

    private fun save() {
        repository.saveEqBands(_bands.value)
    }
}


