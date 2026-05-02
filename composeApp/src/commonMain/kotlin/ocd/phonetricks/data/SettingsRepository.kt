package ocd.phonetricks.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ocd.phonetricks.audio.AudioEffect
import ocd.phonetricks.audio.EqBand
import ocd.phonetricks.audio.FilterPreset
import ocd.phonetricks.audio.MusicalScale

@Serializable
data class NoteSettings(
    val scale: MusicalScale = MusicalScale.CHROMATIC,
)

@Serializable
data class FxState(
    val effectWetDry: Map<AudioEffect, Float> = AudioEffect.entries.associateWith { 0f },
    val filterPreset: FilterPreset = FilterPreset.LOW_PASS,
    val filterFrequency: Float = 1000f,
    val filterWetDry: Float = 0f,
)

private val json = Json { ignoreUnknownKeys = true }

class SettingsRepository(private val store: SettingsStore) {


    fun loadMappings(): List<ControlMapping> {
        val raw = store.read("control_mappings") ?: return defaultMappings
        return try {
            json.decodeFromString(raw)
        } catch (_: Exception) {
            defaultMappings
        }
    }

    fun saveMappings(mappings: List<ControlMapping>) {
        store.write("control_mappings", json.encodeToString(mappings))
    }

    fun loadFxState(): FxState {
        val raw = store.read("fx_state") ?: return FxState()
        val decoded = try {
            json.decodeFromString<FxState>(raw)
        } catch (_: Exception) {
            return FxState()
        }
        return decoded.copy(effectWetDry = mergeWithDefaults(decoded.effectWetDry))
    }

    private fun mergeWithDefaults(persisted: Map<AudioEffect, Float>): Map<AudioEffect, Float> {
        // Persisted state may have been written before new effects were added
        // to the enum. Re-merge so every entry is present and lookups never
        // return null.
        val out = LinkedHashMap<AudioEffect, Float>(AudioEffect.entries.size)
        for (effect in AudioEffect.entries) {
            out[effect] = persisted[effect] ?: 0f
        }
        return out
    }

    fun saveFxState(state: FxState) {
        store.write("fx_state", json.encodeToString(state))
    }

    fun loadNoteSettings(): NoteSettings {
        val raw = store.read("note_settings") ?: return NoteSettings()
        return try {
            json.decodeFromString(raw)
        } catch (_: Exception) {
            NoteSettings()
        }
    }

    fun saveNoteSettings(settings: NoteSettings) {
        store.write("note_settings", json.encodeToString(settings))
    }

    fun loadEqBands(): List<EqBand> {
        val raw = store.read("eq_bands") ?: return emptyList()
        return try {
            json.decodeFromString(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveEqBands(bands: List<EqBand>) {
        store.write("eq_bands", json.encodeToString(bands))
    }
}


