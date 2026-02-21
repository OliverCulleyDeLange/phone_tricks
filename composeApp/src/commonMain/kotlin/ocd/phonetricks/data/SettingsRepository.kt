package ocd.phonetricks.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ocd.phonetricks.audio.AudioEffect
import ocd.phonetricks.audio.FilterPreset

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
        return try {
            json.decodeFromString(raw)
        } catch (_: Exception) {
            FxState()
        }
    }

    fun saveFxState(state: FxState) {
        store.write("fx_state", json.encodeToString(state))
    }
}


