package ocd.phonetricks.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ocd.phonetricks.data.ControlMapping
import ocd.phonetricks.data.ControlParameter
import ocd.phonetricks.data.ControlSurface
import ocd.phonetricks.data.SettingsRepository
import ocd.phonetricks.data.defaultInputRange

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    private val _mappings = MutableStateFlow<List<ControlMapping>>(repository.loadMappings())
    val mappings: StateFlow<List<ControlMapping>> = _mappings.asStateFlow()

    fun addMapping(mapping: ControlMapping) {
        _mappings.value = _mappings.value + mapping
        repository.saveMappings(_mappings.value)
    }

    fun removeMapping(index: Int) {
        _mappings.value = _mappings.value.toMutableList().also { it.removeAt(index) }
        repository.saveMappings(_mappings.value)
    }

    fun updateMapping(index: Int, mapping: ControlMapping) {
        _mappings.value = _mappings.value.toMutableList().also { it[index] = mapping }
        repository.saveMappings(_mappings.value)
    }

    fun updateSurface(index: Int, surface: ControlSurface) {
        val (inMin, inMax) = surface.defaultInputRange()
        val current = _mappings.value[index]
        val updatedParameter = when (val p = current.parameter) {
            is ControlParameter.Pitch -> p.copy(inputMin = inMin, inputMax = inMax)
            is ControlParameter.Volume -> p.copy(inputMin = inMin, inputMax = inMax)
            is ControlParameter.Waveform -> p.copy(inputMin = inMin, inputMax = inMax)
            is ControlParameter.EffectWetDry -> p.copy(inputMin = inMin, inputMax = inMax)
            is ControlParameter.FilterFrequency -> p.copy(inputMin = inMin, inputMax = inMax)
            is ControlParameter.FilterWetDry -> p.copy(inputMin = inMin, inputMax = inMax)
        }
        updateMapping(index, current.copy(surface = surface, parameter = updatedParameter))
    }

    fun updateParameter(index: Int, parameter: ControlParameter) {
        val (inMin, inMax) = _mappings.value[index].surface.defaultInputRange()
        val adjusted = when (parameter) {
            is ControlParameter.Pitch -> parameter.copy(inputMin = inMin, inputMax = inMax)
            is ControlParameter.Volume -> parameter.copy(inputMin = inMin, inputMax = inMax)
            is ControlParameter.Waveform -> parameter.copy(inputMin = inMin, inputMax = inMax)
            is ControlParameter.EffectWetDry -> parameter.copy(inputMin = inMin, inputMax = inMax)
            is ControlParameter.FilterFrequency -> parameter.copy(inputMin = inMin, inputMax = inMax)
            is ControlParameter.FilterWetDry -> parameter.copy(inputMin = inMin, inputMax = inMax)
        }
        updateMapping(index, _mappings.value[index].copy(parameter = adjusted))
    }
}
