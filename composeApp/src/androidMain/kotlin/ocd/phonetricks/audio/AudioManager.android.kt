package ocd.phonetricks.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

actual fun createAudioManager(): AudioManager = AndroidAudioManager()

class AndroidAudioManager : AudioManager {
    private val sampleRate = 44100
    private val duration = 0.05 // 50ms
    private val frequency = 800f // Hz

    private var audioTrack: AudioTrack? = null

    init {
        setupAudioTrack()
    }

    private fun setupAudioTrack() {
        val numSamples = (duration * sampleRate).toInt()
        val samples = ShortArray(numSamples)

        // Generate a sine wave with envelope (fade in/out to avoid clicks)
        for (i in 0 until numSamples) {
            val angle = 2.0 * PI * i / (sampleRate / frequency)
            val envelope = when {
                i < numSamples * 0.1 -> i / (numSamples * 0.1) // Fade in (10%)
                i > numSamples * 0.7 -> (numSamples - i) / (numSamples * 0.3) // Fade out (30%)
                else -> 1.0
            }
            samples[i] = (sin(angle) * envelope * 32767.0 * 0.3).toInt().toShort() // 30% volume
        }

        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val format = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(format)
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack?.write(samples, 0, samples.size)
    }

    override fun playTapSound() {
        audioTrack?.let { track ->
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                track.stop()
            }
            track.reloadStaticData()
            track.play()
        }
    }

    override fun release() {
        audioTrack?.release()
        audioTrack = null
    }
}
