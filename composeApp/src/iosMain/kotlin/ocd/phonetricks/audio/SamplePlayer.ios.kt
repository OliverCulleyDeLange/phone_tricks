package ocd.phonetricks.audio

import kotlin.concurrent.Volatile
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.set
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioPlayerNode
import platform.AVFAudio.AVAudioPlayerNodeCompletionDataConsumed
import kotlin.math.abs
import kotlin.math.roundToInt

actual fun createSamplePlayer(): SamplePlayer = IOSSamplePlayer()

@OptIn(ExperimentalForeignApi::class)
class IOSSamplePlayer : SamplePlayer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val sampleRate = 44100.0
    private val bufferSize = 4096

    private val engine = AVAudioEngine()
    private val playerNode = AVAudioPlayerNode()
    private val format = AVAudioFormat(standardFormatWithSampleRate = sampleRate, channels = 1u)

    private var rawSample: FloatArray = FloatArray(0)

    private val _isRecording = MutableStateFlow(false)
    override val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _hasSample = MutableStateFlow(false)
    override val hasSample: StateFlow<Boolean> = _hasSample.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _waveformData = MutableStateFlow(FloatArray(0))
    override val waveformData: StateFlow<FloatArray> = _waveformData.asStateFlow()

    // Position is maintained as a 0..1 float updated per-buffer and readable any time
    @Volatile private var currentPosition: Float = 0f
    @Volatile private var currentLoopLen: Int = 1
    @Volatile private var loopStartSampleTime: Long = 0L

    override fun getPlayPosition(): Float {
        if (!_isPlaying.value) return 0f
        val renderTime = playerNode.lastRenderTime ?: return currentPosition
        val playerTime = playerNode.playerTimeForNodeTime(renderTime) ?: return currentPosition
        val playedFrames = playerTime.sampleTime
        val len = currentLoopLen.coerceAtLeast(1)
        val posInLoop = ((playedFrames - loopStartSampleTime).coerceAtLeast(0L) % len).toFloat() / len
        return posInLoop.coerceIn(0f, 1f)
    }

    private var loopSpeed = 1f
    private var startPoint = 0f
    private var endPoint = 1f

    private val recordedChunks = mutableListOf<FloatArray>()

    init {
        configureSharedAudioSession()
        engine.attachNode(playerNode)
        engine.connect(playerNode, engine.mainMixerNode, format)
        try { engine.startAndReturnError(null) } catch (_: Throwable) {}
    }

    override fun startRecording() {
        stopPlayback()
        recordedChunks.clear()
        _isRecording.value = true
        val inputNode = engine.inputNode
        inputNode.installTapOnBus(
            bus = 0u,
            bufferSize = bufferSize.toUInt(),
            format = inputNode.inputFormatForBus(0u),
        ) { buffer, _ ->
            if (!_isRecording.value) return@installTapOnBus
            val channelData = buffer?.floatChannelData ?: return@installTapOnBus
            val frames = buffer.frameLength.toInt()
            val row = channelData[0] ?: return@installTapOnBus
            val chunk = FloatArray(frames) { row[it] }
            recordedChunks.add(chunk)
        }
    }

    override fun stopRecordingAndPlay() {
        _isRecording.value = false
        engine.inputNode.removeTapOnBus(0u)
        rawSample = FloatArray(recordedChunks.sumOf { it.size }).also { dst ->
            var pos = 0
            recordedChunks.forEach { it.copyInto(dst, pos); pos += it.size }
        }
        _waveformData.value = buildWaveform(rawSample, 512)
        _hasSample.value = rawSample.isNotEmpty()
        if (rawSample.isNotEmpty()) scheduleLoop()
    }

    private var scheduledFrames: Long = 0L

    private fun scheduleLoop() {
        _isPlaying.value = true
        scheduledFrames = 0L
        scheduleNextBuffer(isLoopStart = true)
    }

    private fun scheduleNextBuffer(isLoopStart: Boolean = false) {
        val sample = rawSample
        if (sample.isEmpty()) return
        val start = (startPoint * sample.size).roundToInt().coerceIn(0, sample.size - 1)
        val end = (endPoint * sample.size).roundToInt().coerceIn(start + 1, sample.size)
        val len = end - start
        val playbackLen = (len / loopSpeed).roundToInt().coerceAtLeast(1)
        currentLoopLen = playbackLen

        val buffer = AVAudioPCMBuffer(pCMFormat = format!!, frameCapacity = playbackLen.toUInt())
        buffer.frameLength = playbackLen.toUInt()
        val channelData = buffer.floatChannelData ?: return
        val dst = channelData[0] ?: return
        for (i in 0 until playbackLen) {
            val srcIdx = start + (i.toFloat() / playbackLen * len).roundToInt().coerceIn(0, len - 1)
            dst[i] = sample[srcIdx]
        }

        scheduledFrames += playbackLen

        playerNode.scheduleBuffer(buffer, completionCallbackType = AVAudioPlayerNodeCompletionDataConsumed) {
            if (_isPlaying.value) {
                // Each loop wrap: reset the start reference from the actual player timeline
                val renderTime = playerNode.lastRenderTime
                val playerTime = renderTime?.let { playerNode.playerTimeForNodeTime(it) }
                if (playerTime != null) {
                    loopStartSampleTime = playerTime.sampleTime
                }
                scheduleNextBuffer()
            }
        }
        if (!playerNode.playing) {
            playerNode.play()
            // Capture start sample time right after play() so first loop is accurate
            val renderTime = playerNode.lastRenderTime
            val playerTime = renderTime?.let { playerNode.playerTimeForNodeTime(it) }
            loopStartSampleTime = playerTime?.sampleTime ?: 0L
        }
    }

    override fun stopPlayback() {
        _isPlaying.value = false
        currentPosition = 0f
        scheduledFrames = 0L
        loopStartSampleTime = 0L
        playerNode.stop()
    }

    override fun setLoopSpeed(speed: Float) {
        loopSpeed = speed.coerceIn(0.1f, 4f)
        if (_isPlaying.value) { playerNode.stop(); scheduledFrames = 0L; loopStartSampleTime = 0L; scheduleNextBuffer(isLoopStart = true) }
    }

    override fun setStartPoint(normalized: Float) {
        startPoint = normalized.coerceIn(0f, endPoint - 0.01f)
        if (_isPlaying.value) { playerNode.stop(); scheduledFrames = 0L; loopStartSampleTime = 0L; scheduleNextBuffer(isLoopStart = true) }
    }

    override fun setEndPoint(normalized: Float) {
        endPoint = normalized.coerceIn(startPoint + 0.01f, 1f)
        if (_isPlaying.value) { playerNode.stop(); scheduledFrames = 0L; loopStartSampleTime = 0L; scheduleNextBuffer(isLoopStart = true) }
    }

    override fun release() {
        stopPlayback()
        engine.stop()
    }

    private fun buildWaveform(samples: FloatArray, bands: Int): FloatArray {
        if (samples.isEmpty()) return FloatArray(bands)
        val out = FloatArray(bands)
        val step = samples.size.toFloat() / bands
        for (i in 0 until bands) {
            val from = (i * step).roundToInt().coerceIn(0, samples.size - 1)
            val to = ((i + 1) * step).roundToInt().coerceIn(from + 1, samples.size)
            var peak = 0f
            for (j in from until to) peak = maxOf(peak, abs(samples[j]))
            out[i] = peak
        }
        return out
    }
}



