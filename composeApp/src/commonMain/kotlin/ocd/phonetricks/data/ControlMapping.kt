package ocd.phonetricks.data

import ocd.phonetricks.audio.AudioEffect
import ocd.phonetricks.audio.FilterPreset
import ocd.phonetricks.audio.Waveform
import ocd.phonetricks.audio.Waveform.SINE
import ocd.phonetricks.audio.Waveform.SQUARE

enum class ControlSurface {
    TOUCH_X,
    TOUCH_Y,
    /** LEFT / RIGHT */
    ACCEL_X,
    /** FORWARD / BACKWARDS */
    ACCEL_Y,
    /** UP / DOWN*/
    ACCEL_Z,
    GYRO_X,
    GYRO_Y,
    GYRO_Z,
    QUATERNION_X,
    QUATERNION_Y,
    QUATERNION_Z,
    QUATERNION_W,
}

fun ControlSurface.defaultInputRange(): Pair<Float, Float> = when (this) {
    ControlSurface.TOUCH_X, ControlSurface.TOUCH_Y -> 0f to 1f
    ControlSurface.ACCEL_X, ControlSurface.ACCEL_Y, ControlSurface.ACCEL_Z -> -10f to 10f
    ControlSurface.GYRO_X, ControlSurface.GYRO_Y, ControlSurface.GYRO_Z -> -5f to 5f
    ControlSurface.QUATERNION_X, ControlSurface.QUATERNION_Y,
    ControlSurface.QUATERNION_Z, ControlSurface.QUATERNION_W -> -1f to 1f
}

sealed class ControlParameter {
    abstract val inputMin: Float
    abstract val inputMax: Float

    data class Volume(
        val min: Float = 0.1f,
        val max: Float = 1.0f,
        override val inputMin: Float = 0f,
        override val inputMax: Float = 1f,
    ) : ControlParameter()

    data class Pitch(
        val min: Float = 110f,
        val max: Float = 1760f,
        override val inputMin: Float = 0f,
        override val inputMax: Float = 1f,
    ) : ControlParameter()

    data class Waveform(
        val startWaveform: ocd.phonetricks.audio.Waveform = SINE,
        val endWaveform: ocd.phonetricks.audio.Waveform = SQUARE,
        override val inputMin: Float = -1f,
        override val inputMax: Float = 1f,
    ) : ControlParameter()

    data class EffectWetDry(
        val effect: AudioEffect = AudioEffect.ECHO,
        override val inputMin: Float = 0f,
        override val inputMax: Float = 1f,
    ) : ControlParameter()

    data class FilterFrequency(
        val preset: FilterPreset = FilterPreset.LOW_PASS,
        val min: Float = 20f,
        val max: Float = 20000f,
        override val inputMin: Float = 0f,
        override val inputMax: Float = 1f,
    ) : ControlParameter()

    data class FilterWetDry(
        val preset: FilterPreset = FilterPreset.LOW_PASS,
        override val inputMin: Float = 0f,
        override val inputMax: Float = 1f,
    ) : ControlParameter()
}

data class ControlMapping(
    val surface: ControlSurface,
    val parameter: ControlParameter,
)

val defaultMappings: List<ControlMapping> = listOf(
    ControlMapping(ControlSurface.TOUCH_X, ControlParameter.Pitch(
        110f, 1000f,
        ControlSurface.TOUCH_X.defaultInputRange().first,
        ControlSurface.TOUCH_X.defaultInputRange().second
    )),
    ControlMapping(ControlSurface.TOUCH_Y, ControlParameter.Waveform(
        Waveform.SINE, Waveform.SQUARE,
        ControlSurface.TOUCH_Y.defaultInputRange().first,
        ControlSurface.TOUCH_Y.defaultInputRange().second)
    ),
    ControlMapping(ControlSurface.ACCEL_Z, ControlParameter.Pitch(
        500f, 1000f,
        ControlSurface.ACCEL_Z.defaultInputRange().first,
        ControlSurface.ACCEL_Z.defaultInputRange().second)
    ),
)
