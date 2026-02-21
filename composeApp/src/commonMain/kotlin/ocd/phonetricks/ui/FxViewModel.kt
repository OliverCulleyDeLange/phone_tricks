package ocd.phonetricks.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ocd.phonetricks.audio.AudioEffect
import ocd.phonetricks.audio.AudioManager
import ocd.phonetricks.audio.FilterPreset
import ocd.phonetricks.data.FxState
import ocd.phonetricks.data.SettingsRepository

class FxViewModel(private val audioManager: AudioManager, private val repository: SettingsRepository) : ViewModel() {

    private val initialState = repository.loadFxState()

    private val _effectWetDry = MutableStateFlow(initialState.effectWetDry)
    val effectWetDry: StateFlow<Map<AudioEffect, Float>> = _effectWetDry.asStateFlow()

    private val _filterPreset = MutableStateFlow(initialState.filterPreset)
    val filterPreset: StateFlow<FilterPreset> = _filterPreset.asStateFlow()

    private val _filterFrequency = MutableStateFlow(initialState.filterFrequency)
    val filterFrequency: StateFlow<Float> = _filterFrequency.asStateFlow()

    private val _filterWetDry = MutableStateFlow(initialState.filterWetDry)
    val filterWetDry: StateFlow<Float> = _filterWetDry.asStateFlow()

    init {
        initialState.effectWetDry.forEach { (effect, wetDry) ->
            audioManager.setEffect(effect, wetDry)
        }
        audioManager.setFilter(initialState.filterPreset, initialState.filterFrequency, initialState.filterWetDry)
    }

    fun setEffectWetDry(effect: AudioEffect, wetDry: Float) {
        val clamped = wetDry.coerceIn(0f, 1f)
        _effectWetDry.value = _effectWetDry.value.toMutableMap().also { it[effect] = clamped }
        audioManager.setEffect(effect, clamped)
        saveState()
    }

    fun cycleFilterPreset(delta: Int) {
        val presets = FilterPreset.entries
        val current = _filterPreset.value
        val newIndex = (presets.indexOf(current) + delta).mod(presets.size)
        _filterPreset.value = presets[newIndex]
        audioManager.setFilter(_filterPreset.value, _filterFrequency.value, _filterWetDry.value)
        saveState()
    }

    fun setFilterFrequency(frequency: Float) {
        val clamped = frequency.coerceIn(20f, 20000f)
        _filterFrequency.value = clamped
        audioManager.setFilter(_filterPreset.value, clamped, _filterWetDry.value)
        saveState()
    }

    fun setFilterWetDry(wetDry: Float) {
        val clamped = wetDry.coerceIn(0f, 1f)
        _filterWetDry.value = clamped
        audioManager.setFilter(_filterPreset.value, _filterFrequency.value, clamped)
        saveState()
    }

    private fun saveState() {
        repository.saveFxState(
            FxState(
                effectWetDry = _effectWetDry.value,
                filterPreset = _filterPreset.value,
                filterFrequency = _filterFrequency.value,
                filterWetDry = _filterWetDry.value,
            )
        )
    }
}
