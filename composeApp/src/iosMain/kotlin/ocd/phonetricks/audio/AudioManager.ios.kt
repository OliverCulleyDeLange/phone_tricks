package ocd.phonetricks.audio

import kotlinx.cinterop.*
import platform.AVFAudio.*
import platform.Foundation.*
import platform.Accelerate.*
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

    private var eqBands: List<EqBand> = emptyList()
    private val eqBiquads = mutableListOf<IirBiquad>()

    private val spectrumSize = 512
    private val fftSize = 1024
    private val fftAccum = FloatArray(fftSize)
    private var fftAccumPos = 0
    private var fftBufferCount = 0
    private var spectrumData = FloatArray(spectrumSize)

    init {
        setupAudioEngine()
    }

    @OptIn(ExperimentalForeignApi::class)
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

    override fun getSpectrumData(): FloatArray = spectrumData.copyOf()

    override fun setEqBands(bands: List<EqBand>) {
        eqBands = bands
        eqBiquads.clear()
        bands.forEach { band ->
            eqBiquads.add(IirBiquad().apply { setPeaking(band.frequency, sampleRate.toFloat(), band.gainDb, band.q) })
        }
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
        return kotlin.math.round(sample * levels.toDouble()).toFloat() / levels
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
            var s = rawSamples[i]
            for (bq in eqBiquads) s = bq.process(s)
            rawSamples[i] = s

            fftAccum[fftAccumPos++] = s
            if (fftAccumPos >= fftSize) {
                fftAccumPos = 0
                fftBufferCount++
                if (fftBufferCount % 4 == 0) {
                    spectrumData = computeSpectrum(fftAccum, fftSize, spectrumSize)
                }
            }
        }

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

private class IirBiquad {
    private var b0 = 1f; private var b1 = 0f; private var b2 = 0f
    private var a1 = 0f; private var a2 = 0f
    private var x1 = 0f; private var x2 = 0f
    private var y1 = 0f; private var y2 = 0f

    fun setPeaking(freq: Float, sr: Float, gainDb: Float, q: Float) {
        val A = 10f.pow(gainDb / 40f)
        val w0 = 2f * PI.toFloat() * freq / sr
        val alpha = sin(w0) / (2f * q)
        val a0 = 1f + alpha / A
        b0 = (1f + alpha * A) / a0
        b1 = (-2f * cos(w0)) / a0
        b2 = (1f - alpha * A) / a0
        a1 = (-2f * cos(w0)) / a0
        a2 = (1f - alpha / A) / a0
    }

    fun process(input: Float): Float {
        val out = b0 * input + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2 = x1; x1 = input; y2 = y1; y1 = out
        return out
    }
}

private fun Float.pow(exp: Float): Float = this.toDouble().pow(exp.toDouble()).toFloat()

private fun computeSpectrum(input: FloatArray, fftSize: Int, bands: Int): FloatArray {
    val re = FloatArray(fftSize)
    val im = FloatArray(fftSize)
    for (i in 0 until fftSize) {
        val hann = 0.5f * (1f - cos(2f * PI.toFloat() * i / (fftSize - 1)))
        re[i] = input[i] * hann
    }
    ktFft(re, im, fftSize)
    val out = FloatArray(bands)
    for (k in 0 until bands) {
        val mag = sqrt(re[k] * re[k] + im[k] * im[k]) / fftSize
        val db = if (mag > 0f) (20f * log10(mag) + 80f) / 80f else 0f
        out[k] = db.coerceIn(0f, 1f)
    }
    return out
}

private fun ktFft(re: FloatArray, im: FloatArray, n: Int) {
    var j = 0
    for (i in 1 until n) {
        var bit = n shr 1
        while (j and bit != 0) { j = j xor bit; bit = bit shr 1 }
        j = j xor bit
        if (i < j) { re[i] = re[j].also { re[j] = re[i] }; im[i] = im[j].also { im[j] = im[i] } }
    }
    var len = 2
    while (len <= n) {
        val halfLen = len / 2
        val ang = -2f * PI.toFloat() / len
        val wRe = cos(ang); val wIm = sin(ang)
        var i = 0
        while (i < n) {
            var curRe = 1f; var curIm = 0f
            for (k in 0 until halfLen) {
                val uRe = re[i + k]; val uIm = im[i + k]
                val vRe = re[i + k + halfLen] * curRe - im[i + k + halfLen] * curIm
                val vIm = re[i + k + halfLen] * curIm + im[i + k + halfLen] * curRe
                re[i + k] = uRe + vRe; im[i + k] = uIm + vIm
                re[i + k + halfLen] = uRe - vRe; im[i + k + halfLen] = uIm - vIm
                val newCurRe = curRe * wRe - curIm * wIm
                curIm = curRe * wIm + curIm * wRe
                curRe = newCurRe
            }
            i += len
        }
        len = len shl 1
    }
}
