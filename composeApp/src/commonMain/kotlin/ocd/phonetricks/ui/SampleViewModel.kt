package ocd.phonetricks.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import ocd.phonetricks.audio.SamplePlayer

class SampleViewModel(private val samplePlayer: SamplePlayer) : ViewModel() {

    val isRecording: StateFlow<Boolean> = samplePlayer.isRecording
    val hasSample: StateFlow<Boolean> = samplePlayer.hasSample
    val isPlaying: StateFlow<Boolean> = samplePlayer.isPlaying
    val waveformData: StateFlow<FloatArray> = samplePlayer.waveformData

    fun getPlayPosition(): Float = samplePlayer.getPlayPosition()

    fun startRecording() = samplePlayer.startRecording()
    fun stopRecordingAndPlay() = samplePlayer.stopRecordingAndPlay()
    fun stopPlayback() = samplePlayer.stopPlayback()
    fun setLoopSpeed(speed: Float) = samplePlayer.setLoopSpeed(speed)
    fun setStartPoint(normalized: Float) = samplePlayer.setStartPoint(normalized)
    fun setEndPoint(normalized: Float) = samplePlayer.setEndPoint(normalized)
}
