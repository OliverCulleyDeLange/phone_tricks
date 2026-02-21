package ocd.phonetricks.audio

import kotlinx.coroutines.flow.StateFlow

interface SamplePlayer {
    val isRecording: StateFlow<Boolean>
    val hasSample: StateFlow<Boolean>
    val isPlaying: StateFlow<Boolean>
    val waveformData: StateFlow<FloatArray>

    fun getPlayPosition(): Float

    fun startRecording()
    fun stopRecordingAndPlay()
    fun stopPlayback()
    fun setLoopSpeed(speed: Float)
    fun setStartPoint(normalized: Float)
    fun setEndPoint(normalized: Float)
    fun release()
}

expect fun createSamplePlayer(): SamplePlayer




