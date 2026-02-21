package ocd.phonetricks.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ocd.phonetricks.audio.AudioEffect
import ocd.phonetricks.audio.AudioManager
import ocd.phonetricks.audio.FilterPreset

class FxViewModel(private val audioManager: AudioManager) : ViewModel() {

    private val _effectWetDry = MutableStateFlow(
        AudioEffect.entries.associateWith { 0f }
    )
    val effectWetDry: StateFlow<Map<AudioEffect, Float>> = _effectWetDry.asStateFlow()

    private val _filterPreset = MutableStateFlow(FilterPreset.LOW_PASS)
    val filterPreset: StateFlow<FilterPreset> = _filterPreset.asStateFlow()

    private val _filterFrequency = MutableStateFlow(1000f)
    val filterFrequency: StateFlow<Float> = _filterFrequency.asStateFlow()

    private val _filterWetDry = MutableStateFlow(0f)
    val filterWetDry: StateFlow<Float> = _filterWetDry.asStateFlow()

    fun setEffectWetDry(effect: AudioEffect, wetDry: Float) {
        val clamped = wetDry.coerceIn(0f, 1f)
        _effectWetDry.value = _effectWetDry.value.toMutableMap().also { it[effect] = clamped }
        audioManager.setEffect(effect, clamped)
    }

    fun cycleFilterPreset(delta: Int) {
        val presets = FilterPreset.entries
        val current = _filterPreset.value
        val newIndex = (presets.indexOf(current) + delta).mod(presets.size)
        _filterPreset.value = presets[newIndex]
        audioManager.setFilter(_filterPreset.value, _filterFrequency.value, _filterWetDry.value)
    }

    fun setFilterFrequency(frequency: Float) {
        val clamped = frequency.coerceIn(20f, 20000f)
        _filterFrequency.value = clamped
        audioManager.setFilter(_filterPreset.value, clamped, _filterWetDry.value)
    }

    fun setFilterWetDry(wetDry: Float) {
        val clamped = wetDry.coerceIn(0f, 1f)
        _filterWetDry.value = clamped
        audioManager.setFilter(_filterPreset.value, _filterFrequency.value, clamped)
    }
}

