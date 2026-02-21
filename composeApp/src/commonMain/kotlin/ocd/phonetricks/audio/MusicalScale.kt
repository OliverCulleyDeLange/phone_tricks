package ocd.phonetricks.audio

import kotlinx.serialization.Serializable
import kotlin.math.ln
import kotlin.math.pow

@Serializable
enum class MusicalScale {
    CHROMATIC,
    MAJOR,
    NATURAL_MINOR,
    HARMONIC_MINOR,
    PENTATONIC_MAJOR,
    PENTATONIC_MINOR,
    BLUES,
    DORIAN,
    PHRYGIAN,
    LYDIAN,
    MIXOLYDIAN,
    LOCRIAN,
    WHOLE_TONE,
    DIMINISHED;

    fun displayName(): String = when (this) {
        CHROMATIC -> "Chromatic"
        MAJOR -> "Major"
        NATURAL_MINOR -> "Natural Minor"
        HARMONIC_MINOR -> "Harmonic Minor"
        PENTATONIC_MAJOR -> "Pentatonic Major"
        PENTATONIC_MINOR -> "Pentatonic Minor"
        BLUES -> "Blues"
        DORIAN -> "Dorian"
        PHRYGIAN -> "Phrygian"
        LYDIAN -> "Lydian"
        MIXOLYDIAN -> "Mixolydian"
        LOCRIAN -> "Locrian"
        WHOLE_TONE -> "Whole Tone"
        DIMINISHED -> "Diminished"
    }

    private val semitoneIntervals: List<Int> get() = when (this) {
        CHROMATIC -> listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)
        MAJOR -> listOf(0, 2, 4, 5, 7, 9, 11)
        NATURAL_MINOR -> listOf(0, 2, 3, 5, 7, 8, 10)
        HARMONIC_MINOR -> listOf(0, 2, 3, 5, 7, 8, 11)
        PENTATONIC_MAJOR -> listOf(0, 2, 4, 7, 9)
        PENTATONIC_MINOR -> listOf(0, 3, 5, 7, 10)
        BLUES -> listOf(0, 3, 5, 6, 7, 10)
        DORIAN -> listOf(0, 2, 3, 5, 7, 9, 10)
        PHRYGIAN -> listOf(0, 1, 3, 5, 7, 8, 10)
        LYDIAN -> listOf(0, 2, 4, 6, 7, 9, 11)
        MIXOLYDIAN -> listOf(0, 2, 4, 5, 7, 9, 10)
        LOCRIAN -> listOf(0, 1, 3, 5, 6, 8, 10)
        WHOLE_TONE -> listOf(0, 2, 4, 6, 8, 10)
        DIMINISHED -> listOf(0, 2, 3, 5, 6, 8, 9, 11)
    }

    fun snapFrequency(frequency: Float, rootNoteHz: Float = 16.35f): Float {
        if (this == CHROMATIC) return frequency
        val intervals = semitoneIntervals
        val semitoneFromRoot = 12.0 * ln(frequency.toDouble() / rootNoteHz) / ln(2.0)
        val octave = kotlin.math.floor(semitoneFromRoot / 12).toInt()

        val candidates = listOf(octave - 1, octave, octave + 1).flatMap { oct ->
            intervals.map { interval -> oct * 12 + interval }
        }
        val snappedSemitone = candidates.minByOrNull { kotlin.math.abs(it - semitoneFromRoot) }
            ?: return frequency
        return (rootNoteHz * 2.0.pow(snappedSemitone / 12.0)).toFloat()
    }
}




