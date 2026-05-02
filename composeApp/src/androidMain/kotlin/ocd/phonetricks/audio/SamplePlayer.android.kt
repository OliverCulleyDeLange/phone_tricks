package ocd.phonetricks.audio

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

actual fun createSamplePlayer(): SamplePlayer = AndroidSamplePlayer()

class AndroidSamplePlayer : SamplePlayer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_FLOAT
    private val minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        .coerceAtLeast(4096)

    private var rawSample: FloatArray = FloatArray(0)
    private var recordJob: Job? = null
    private var playJob: Job? = null
    private var activeTrack: AudioTrack? = null

    @Volatile private var loopLenSamples: Int = 1
    @Volatile private var loopStartFrame: Long = 0L

    private var loopSpeed = 1f
    private var startPoint = 0f
    private var endPoint = 1f

    private val _isRecording = MutableStateFlow(false)
    override val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _hasSample = MutableStateFlow(false)
    override val hasSample: StateFlow<Boolean> = _hasSample.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _waveformData = MutableStateFlow(FloatArray(0))
    override val waveformData: StateFlow<FloatArray> = _waveformData.asStateFlow()

    override fun getPlayPosition(): Float {
        val track = activeTrack ?: return 0f
        val head = track.playbackHeadPosition.toLong() and 0xFFFFFFFFL
        val len = loopLenSamples.coerceAtLeast(1)
        val posInLoop = (head - loopStartFrame).coerceAtLeast(0L)
        return (posInLoop.toFloat() / len).coerceIn(0f, 1f)
    }

    @SuppressLint("MissingPermission")
    override fun startRecording() {
        stopPlayback()
        val previous = recordJob
        recordJob = scope.launch {
            // Wait for the previous recorder to fully release the mic
            // before acquiring it again — otherwise AudioRecord
            // construction can fail with the mic still held.
            previous?.cancelAndJoin()
            val recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate, channelConfig, audioFormat, minBufSize * 4
            )
            val chunks = mutableListOf<FloatArray>()
            val buf = FloatArray(minBufSize)
            recorder.startRecording()
            _isRecording.value = true
            try {
                while (_isRecording.value) {
                    val read = recorder.read(buf, 0, buf.size, AudioRecord.READ_BLOCKING)
                    if (read > 0) chunks.add(buf.copyOf(read))
                }
            } finally {
                recorder.stop()
                recorder.release()
                rawSample = FloatArray(chunks.sumOf { it.size }).also { dst ->
                    var pos = 0
                    chunks.forEach { it.copyInto(dst, pos); pos += it.size }
                }
                _waveformData.value = buildWaveform(rawSample, 512)
                _hasSample.value = rawSample.isNotEmpty()
            }
        }
    }

    override fun stopRecordingAndPlay() {
        _isRecording.value = false
        recordJob?.let { job ->
            scope.launch {
                job.join()
                startLoop()
            }
        }
    }

    private fun startLoop() {
        val previous = playJob
        playJob = scope.launch {
            previous?.cancelAndJoin()
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                // Smaller buffer = less playback latency in position reporting
                .setBufferSizeInBytes(minBufSize * 2 * 4)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            activeTrack = track
            track.play()
            loopStartFrame = 0L
            _isPlaying.value = true
            val writeBuf = FloatArray(minBufSize)
            var readPos = 0f
            try {
                while (_isPlaying.value) {
                    val sample = rawSample
                    if (sample.isEmpty()) break
                    val start = (startPoint * sample.size).roundToInt().coerceIn(0, sample.size - 1)
                    val end = (endPoint * sample.size).roundToInt().coerceIn(start + 1, sample.size)
                    val len = end - start
                    val newLoopLen = (len.toFloat() / loopSpeed).roundToInt().coerceAtLeast(1)
                    if (newLoopLen != loopLenSamples) {
                        // Loop region or speed changed — snap loopStartFrame to current head
                        loopStartFrame = track.playbackHeadPosition.toLong() and 0xFFFFFFFFL
                        loopLenSamples = newLoopLen
                    }

                    var wrapped = false
                    for (i in writeBuf.indices) {
                        val idx = start + (readPos % len).roundToInt()
                        writeBuf[i] = sample[idx.coerceIn(start, end - 1)]
                        readPos += loopSpeed
                        if (readPos >= len) {
                            readPos -= len
                            wrapped = true
                        }
                    }
                    track.write(writeBuf, 0, writeBuf.size, AudioTrack.WRITE_BLOCKING)
                    if (wrapped) {
                        loopStartFrame = track.playbackHeadPosition.toLong() and 0xFFFFFFFFL
                    }
                }
            } finally {
                track.stop()
                track.release()
                activeTrack = null
                _isPlaying.value = false
            }
        }
    }

    override fun stopPlayback() {
        _isPlaying.value = false
        val previous = playJob
        playJob = null
        // Let the coroutine's finally block release the AudioTrack —
        // clearing activeTrack here would race with track.write().
        if (previous != null) scope.launch { previous.cancelAndJoin() }
    }

    override fun setLoopSpeed(speed: Float) { loopSpeed = speed.coerceIn(0.1f, 4f) }
    override fun setStartPoint(normalized: Float) { startPoint = normalized.coerceIn(0f, endPoint - 0.01f) }
    override fun setEndPoint(normalized: Float) { endPoint = normalized.coerceIn(startPoint + 0.01f, 1f) }

    override fun release() {
        stopPlayback()
        recordJob?.cancel()
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
