package ocd.phonetricks.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ocd.phonetricks.audio.MusicalScale
import ocd.phonetricks.data.NoteSettings
import ocd.phonetricks.data.SettingsRepository

class NoteSettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    private val _scale = MutableStateFlow(repository.loadNoteSettings().scale)
    val scale: StateFlow<MusicalScale> = _scale.asStateFlow()

    fun cycleScale(delta: Int) {
        val scales = MusicalScale.entries
        val newIndex = (scales.indexOf(_scale.value) + delta).mod(scales.size)
        _scale.value = scales[newIndex]
        repository.saveNoteSettings(NoteSettings(scale = _scale.value))
    }
}

