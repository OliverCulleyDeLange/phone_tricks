package ocd.phonetricks.audio

import kotlinx.cinterop.*
import platform.AVFAudio.*
import platform.Foundation.*
import kotlin.math.*

actual fun createAudioManager(): AudioManager = IOSAudioManager()

class IOSAudioManager : AudioManager {
    private val audioEngine = AVAudioEngine()
    private val playerNode = AVAudioPlayerNode()
    private val reverbNode = AVAudioUnitReverb()
    private val delayNode = AVAudioUnitDelay()
    private val distortionNode = AVAudioUnitDistortion()

    private val sampleRate = 44100.0
    private var isPlaying = false
    private var isSynthPlaying = false
    private var tailBuffersRemaining = 0

    private var currentFrequency = 440f
    private var currentAmplitude = 0.5f
    private var currentWaveformA = Waveform.SINE
    private var currentWaveformB = Waveform.SINE
    private var currentBlend = 0f

    private val bufferSize = 2048
    private var phase = 0.0

    private val echoDelay = (44100 * 0.5).toInt()
    private val echoHistory = FloatArray(echoDelay)
    private var echoPos = 0

    private var effectWetDry = FloatArray(AudioEffect.entries.size) { 0f }
    private var filterPreset = FilterPreset.LOW_PASS
    private var filterFrequency = 1000f
    private var filterWetDry = 0f

    init {
        setupAudioEngine()
    }

    private fun setupAudioEngine() {
        audioEngine.attachNode(playerNode)
        audioEngine.attachNode(reverbNode)
        audioEngine.attachNode(delayNode)
        audioEngine.attachNode(distortionNode)

        val format = AVAudioFormat(standardFormatWithSampleRate = sampleRate, channels = 1u) ?: return

        audioEngine.connect(playerNode, delayNode, format)
        audioEngine.connect(delayNode, reverbNode, format)
        audioEngine.connect(reverbNode, distortionNode, format)
        audioEngine.connect(distortionNode, audioEngine.mainMixerNode, format)

        reverbNode.wetDryMix = 0f
        delayNode.wetDryMix = 0f
        distortionNode.wetDryMix = 0f

        try {
            audioEngine.startAndReturnError(null)
        } catch (_: Throwable) {}
    }

    override fun playSynthSound(
        frequency: Float,
        amplitude: Float,
        waveformA: Waveform,
        waveformB: Waveform,
        blend: Float,
    ) {
        currentFrequency = frequency
        currentAmplitude = amplitude
        currentWaveformA = waveformA
        currentWaveformB = waveformB
        currentBlend = blend

        isSynthPlaying = true
        tailBuffersRemaining = 0
        if (!isPlaying) {
            isPlaying = true
            generateAndPlaySound()
        }
    }

    override fun stopSound() {
        isSynthPlaying = false
        // 2 seconds of tail at bufferSize frames per buffer
        tailBuffersRemaining = ((sampleRate * 2.0) / bufferSize).toInt()
    }

    override fun setEffect(effect: AudioEffect, wetDry: Float) {
        effectWetDry[effect.ordinal] = wetDry
        when (effect) {
            AudioEffect.REVERB -> reverbNode.wetDryMix = wetDry * 100f
            AudioEffect.DELAY -> delayNode.wetDryMix = wetDry * 100f
            AudioEffect.GUITAR_DISTORTION -> distortionNode.wetDryMix = wetDry * 100f
            else -> {}
        }
    }

    override fun setFilter(preset: FilterPreset, frequency: Float, wetDry: Float) {
        filterPreset = preset
        filterFrequency = frequency
        filterWetDry = wetDry
    }

    override fun release() {
        isPlaying = false
        isSynthPlaying = false
        playerNode.stop()
        audioEngine.stop()
    }

    private fun generateSample(waveform: Waveform, phase: Double): Double = when (waveform) {
        Waveform.SINE -> sin(phase)
        Waveform.SQUARE -> if (phase < PI) 1.0 else -1.0
        Waveform.TRIANGLE -> {
            val n = phase / (2.0 * PI)
            when {
                n < 0.25 -> n * 4.0
                n < 0.75 -> 2.0 - (n * 4.0)
                else -> (n * 4.0) - 4.0
            }
        }
        Waveform.SAWTOOTH -> {
            val n = phase / (2.0 * PI)
            2.0 * (n - floor(0.5 + n))
        }
    }

    private fun applyBitcrusher(sample: Float, amount: Float): Float {
        val bits = (1.0f + (1.0f - amount) * 14.0f).toInt()
        val levels = 2f.pow(bits.toFloat())
        return kotlin.math.roundToInt(sample * levels).toFloat() / levels
    }

    private fun applySimpleFilter(samples: FloatArray, size: Int) {
        if (filterWetDry <= 0f) return
        val rc = 1.0f / (2.0f * PI.toFloat() * filterFrequency)
        val dt = 1.0f / sampleRate.toFloat()
        val alpha = when (filterPreset) {
            FilterPreset.LOW_PASS, FilterPreset.LOW_SHELF -> dt / (rc + dt)
            FilterPreset.HIGH_PASS, FilterPreset.HIGH_SHELF -> rc / (rc + dt)
            else -> dt / (rc + dt)
        }
        var prev = samples[0]
        for (i in 0 until size) {
            val filtered = alpha * samples[i] + (1f - alpha) * prev
            prev = filtered
            samples[i] = samples[i] * (1f - filterWetDry) + filtered * filterWetDry
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun generateAndPlaySound() {
        val isTail = !isSynthPlaying && tailBuffersRemaining > 0
        if (!isSynthPlaying && tailBuffersRemaining <= 0) {
            isPlaying = false
            playerNode.stop()
            return
        }

        if (isTail) tailBuffersRemaining--

        val format = AVAudioFormat(standardFormatWithSampleRate = sampleRate, channels = 1u) ?: return
        val buffer = AVAudioPCMBuffer(pCMFormat = format, frameCapacity = bufferSize.toUInt()) ?: return
        buffer.frameLength = bufferSize.toUInt()

        val channelData = buffer.floatChannelData ?: return
        val samples = channelData[0] ?: return

        val phaseIncrement = 2.0 * PI * currentFrequency.toDouble() / sampleRate
        val blend = currentBlend.toDouble()
        val echoWet = effectWetDry[AudioEffect.ECHO.ordinal]
        val bitcrusherWet = effectWetDry[AudioEffect.BITCRUSHER.ordinal]
        val whooshWet = effectWetDry[AudioEffect.WHOOSH.ordinal]
        var whooshPhase = 0.0

        val rawSamples = FloatArray(bufferSize)
        for (i in 0 until bufferSize) {
            if (isSynthPlaying) {
                val sA = generateSample(currentWaveformA, phase)
                val sB = generateSample(currentWaveformB, phase)
                rawSamples[i] = ((sA * (1.0 - blend) + sB * blend) * currentAmplitude.toDouble()).toFloat()
                phase += phaseIncrement
                if (phase > 2.0 * PI) phase -= 2.0 * PI
            }
            // else rawSamples[i] stays 0f — feeds silence into effect chain for tail decay
        }

        for (i in 0 until bufferSize) {
            var s = rawSamples[i]

            if (echoWet > 0f) {
                val delayed = echoHistory[echoPos]
                val mixed = s + delayed * 0.5f
                echoHistory[echoPos] = mixed
                echoPos = (echoPos + 1) % echoDelay
                s = s * (1f - echoWet) + mixed * echoWet
            }

            if (bitcrusherWet > 0f && isSynthPlaying) {
                val crushed = applyBitcrusher(s, bitcrusherWet)
                s = s * (1f - bitcrusherWet) + crushed * bitcrusherWet
            }

            if (whooshWet > 0f && isSynthPlaying) {
                val mod = (0.5 + 0.5 * sin(whooshPhase)).toFloat()
                s = s * (1f - whooshWet) + s * mod * whooshWet
                whooshPhase += 2.0 * PI * 2.0 / sampleRate
            }

            rawSamples[i] = s
        }

        applySimpleFilter(rawSamples, bufferSize)

        for (i in 0 until bufferSize) {
            samples[i] = rawSamples[i]
        }

        playerNode.scheduleBuffer(buffer, completionCallbackType = AVAudioPlayerNodeCompletionDataConsumed) {
            if (this.isSynthPlaying || this.tailBuffersRemaining > 0) this.generateAndPlaySound()
            else {
                this.isPlaying = false
            }
        }

        if (!playerNode.playing) playerNode.play()
    }
}

private fun Float.pow(exp: Float): Float = kotlin.math.pow(this.toDouble(), exp.toDouble()).toFloat()
private fun kotlin.math.roundToInt(x: Float): Int = kotlin.math.round(x.toDouble()).toInt()
