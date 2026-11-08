package ocd.phonetricks.data

import ocd.phonetricks.audio.AudioEffect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SettingsRepositoryTest {

    private class InMemoryStore : SettingsStore {
        val map = mutableMapOf<String, String>()
        override fun read(key: String): String? = map[key]
        override fun write(key: String, value: String) { map[key] = value }
    }

    @Test
    fun loadFxStateFillsMissingEffectsWithDefault() {
        // Simulate a persisted FxState written before new AudioEffect entries
        // were added: only ECHO has a value. Loading must still produce an
        // entry for every current AudioEffect so UI lookups never return null.
        val store = InMemoryStore()
        store.write(
            "fx_state",
            """{"effectWetDry":{"ECHO":0.7},"filterPreset":"LOW_PASS","filterFrequency":1000.0,"filterWetDry":0.0}"""
        )

        val loaded = SettingsRepository(store).loadFxState()

        for (effect in AudioEffect.entries) {
            assertNotNull(loaded.effectWetDry[effect], "missing $effect")
        }
        assertEquals(0.7f, loaded.effectWetDry[AudioEffect.ECHO])
        assertEquals(0f, loaded.effectWetDry[AudioEffect.REVERB])
    }

    @Test
    fun loadFxStateReturnsDefaultsWhenNothingStored() {
        val loaded = SettingsRepository(InMemoryStore()).loadFxState()
        assertEquals(AudioEffect.entries.size, loaded.effectWetDry.size)
        assertEquals(0f, loaded.effectWetDry[AudioEffect.ECHO])
    }

    @Test
    fun loadFxStateRecoversFromCorruptJson() {
        val store = InMemoryStore()
        store.write("fx_state", "not json")
        val loaded = SettingsRepository(store).loadFxState()
        assertEquals(AudioEffect.entries.size, loaded.effectWetDry.size)
    }

    @Test
    fun saveLoadRoundTripPreservesAllEffectValues() {
        val store = InMemoryStore()
        val repo = SettingsRepository(store)
        val original = FxState(
            effectWetDry = AudioEffect.entries.associateWith { 0.25f },
            filterFrequency = 2000f,
            filterWetDry = 0.5f,
        )
        repo.saveFxState(original)
        val reloaded = repo.loadFxState()
        for (effect in AudioEffect.entries) {
            assertEquals(0.25f, reloaded.effectWetDry[effect])
        }
        assertEquals(2000f, reloaded.filterFrequency)
        assertEquals(0.5f, reloaded.filterWetDry)
    }
}
