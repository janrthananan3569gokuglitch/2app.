package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Process
import android.util.Log
import com.example.dsp.BiquadFilter
import com.example.dsp.CustomMathDSP
import com.example.dsp.DynamicPeakLimiter
import com.example.dsp.FastFourierTransform
import com.example.dsp.ProceduralToneGenerator
import com.example.dsp.SchroederReverb
import com.example.dsp.SpatialAudioHRTF
import com.example.dsp.StereoHolographicExpander
import com.example.dsp.TenBandEqualizer
import com.example.dsp.TubeSaturator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.min

/**
 * High-performance Native Audio Engine with Real-Time DSP Pipeline.
 *
 * Utilizes low-latency AudioTrack streaming, MediaCodec native hardware/software decoding,
 * and executes custom DSP math on every audio frame buffer before outputting to DAC.
 */
class NativeAudioEngine(private val context: Context) {
    companion object {
        private const val TAG = "NativeAudioEngine"
        const val SAMPLE_RATE = 44100
        const val CHANNEL_COUNT = 2 // Stereo
        const val FRAME_SIZE_SAMPLES = 512 // Processing buffer size
        const val INTERLEAVED_BUFFER_SIZE = FRAME_SIZE_SAMPLES * CHANNEL_COUNT
        const val VISUALIZER_BANDS = 64
        const val WAVEFORM_POINTS = 128
    }

    // Audio DSP components
    val equalizer = TenBandEqualizer(SAMPLE_RATE)
    val spatialHrtf = SpatialAudioHRTF(SAMPLE_RATE)
    val reverb = SchroederReverb(SAMPLE_RATE)
    val tubeSaturator = TubeSaturator()
    val stereoExpander = StereoHolographicExpander()
    val peakLimiter = DynamicPeakLimiter(SAMPLE_RATE)
    private val fft = FastFourierTransform(1024)

    // Visualizer StateFlows
    private val _spectrumBands = MutableStateFlow(FloatArray(VISUALIZER_BANDS))
    val spectrumBands: StateFlow<FloatArray> = _spectrumBands.asStateFlow()

    private val _waveformPoints = MutableStateFlow(FloatArray(WAVEFORM_POINTS))
    val waveformPoints: StateFlow<FloatArray> = _waveformPoints.asStateFlow()

    // Playback state
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    // Internal execution thread control
    private var audioThread: Thread? = null
    private val isRunning = AtomicBoolean(false)
    private val isPaused = AtomicBoolean(false)

    // Current source mode
    private var activeProceduralType: ProceduralToneGenerator.SignalType? = null
    private var activeAudioUri: Uri? = null
    private var activeVaultBytes: ByteArray? = null

    // AudioTrack hardware sink
    private var audioTrack: AudioTrack? = null

    // FFT scratch buffers (pre-allocated to avoid GC memory churn)
    private val fftInput = FloatArray(1024)
    private val fftReal = FloatArray(1024)
    private val fftImag = FloatArray(1024)
    private val bandBuffer = FloatArray(VISUALIZER_BANDS)
    private val waveBuffer = FloatArray(WAVEFORM_POINTS)
    private var fftWritePos = 0

    init {
        // Initialize default DSP parameters
        spatialHrtf.isEnabled = true
        spatialHrtf.setPosition(0.0f, 0.0f, 1.5f)
        reverb.isEnabled = false
        tubeSaturator.isEnabled = false
        stereoExpander.isEnabled = true
    }

    @Synchronized
    fun playProcedural(type: ProceduralToneGenerator.SignalType) {
        stop()
        activeProceduralType = type
        activeAudioUri = null
        activeVaultBytes = null
        _durationMs.value = 300_000L // 5 minutes standard test session
        _currentPositionMs.value = 0L
        ProceduralToneGenerator.reset()
        startAudioLoop()
    }

    @Synchronized
    fun playUri(uri: Uri, duration: Long = 0L) {
        stop()
        activeProceduralType = null
        activeAudioUri = uri
        activeVaultBytes = null
        _durationMs.value = duration
        _currentPositionMs.value = 0L
        startAudioLoop()
    }

    @Synchronized
    fun playVaultAudio(decryptedBytes: ByteArray, duration: Long = 0L) {
        stop()
        activeProceduralType = null
        activeAudioUri = null
        activeVaultBytes = decryptedBytes
        _durationMs.value = duration
        _currentPositionMs.value = 0L
        startAudioLoop()
    }

    fun pause() {
        isPaused.set(true)
        _isPlaying.value = false
        try {
            audioTrack?.pause()
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing audioTrack: ${e.message}")
        }
    }

    fun resume() {
        if (!isRunning.get()) {
            if (activeProceduralType != null || activeAudioUri != null || activeVaultBytes != null) {
                startAudioLoop()
            }
            return
        }
        isPaused.set(false)
        _isPlaying.value = true
        try {
            audioTrack?.play()
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming audioTrack: ${e.message}")
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            resume()
        }
    }

    private val seekRequestedMs = java.util.concurrent.atomic.AtomicLong(-1L)

    fun seekTo(positionMs: Long) {
        val maxDuration = max(1L, _durationMs.value)
        val clamped = positionMs.coerceIn(0L, maxDuration)
        seekRequestedMs.set(clamped)
        _currentPositionMs.value = clamped
    }

    fun seekRelative(deltaMs: Long) {
        val target = (_currentPositionMs.value + deltaMs).coerceIn(0L, max(1L, _durationMs.value))
        seekTo(target)
    }

    @Synchronized
    fun stop() {
        isRunning.set(false)
        isPaused.set(false)
        _isPlaying.value = false

        audioThread?.interrupt()
        try {
            audioThread?.join(300)
        } catch (_: InterruptedException) {}
        audioThread = null

        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audioTrack: ${e.message}")
        }
        audioTrack = null

        // Clear visualizer
        _spectrumBands.value = FloatArray(VISUALIZER_BANDS)
        _waveformPoints.value = FloatArray(WAVEFORM_POINTS)
    }

    private fun initAudioTrack(): Boolean {
        val minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_FLOAT
        )

        val bufferSize = max(minBufferSize, INTERLEAVED_BUFFER_SIZE * 4 * 4) // 4 float buffers

        return try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.apply {
                setVolume(1.0f)
                setStereoVolume(1.0f, 1.0f)
                play()
            }
            val sessionId = audioTrack?.audioSessionId ?: 0
            if (sessionId > 0) {
                com.example.service.AudioVaultService.activeInstance?.attachDspChain(sessionId)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create float AudioTrack: ${e.message}")
            false
        }
    }

    private fun startAudioLoop() {
        if (isRunning.get()) return

        if (!initAudioTrack()) {
            Log.e(TAG, "AudioTrack init failed, cannot start audio loop")
            return
        }

        isRunning.set(true)
        isPaused.set(false)
        _isPlaying.value = true

        audioThread = Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
            try {
                if (activeProceduralType != null) {
                    runProceduralLoop()
                } else if (activeVaultBytes != null) {
                    runMemoryPcmLoop(activeVaultBytes!!)
                } else if (activeAudioUri != null) {
                    runDecoderLoop(activeAudioUri!!)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Audio loop exception: ${e.message}", e)
            } finally {
                _isPlaying.value = false
            }
        }, "XPAudioRenderThread").apply {
            start()
        }
    }

    /**
     * Procedural synthesis loop (Solfeggio, Binaural beats, Pink noise, Cyberpunk groove).
     */
    private fun runProceduralLoop() {
        val pcmBuffer = FloatArray(INTERLEAVED_BUFFER_SIZE)
        val signalType = activeProceduralType ?: return
        var framesGenerated = 0L

        while (isRunning.get()) {
            val seekTarget = seekRequestedMs.getAndSet(-1L)
            if (seekTarget >= 0) {
                framesGenerated = seekTarget * SAMPLE_RATE / 1000L
            }

            if (isPaused.get()) {
                try {
                    Thread.sleep(20)
                } catch (_: InterruptedException) {
                    break
                }
                continue
            }

            // 1. Synthesize next block of stereo audio
            ProceduralToneGenerator.generate(signalType, pcmBuffer, FRAME_SIZE_SAMPLES, SAMPLE_RATE)

            // 2. Execute Master DSP Chain
            processDspChain(pcmBuffer, FRAME_SIZE_SAMPLES)

            // 3. Update Visualizer
            feedVisualizer(pcmBuffer, FRAME_SIZE_SAMPLES)

            // 4. Stream to AudioTrack
            val written = audioTrack?.write(
                pcmBuffer,
                0,
                INTERLEAVED_BUFFER_SIZE,
                AudioTrack.WRITE_BLOCKING
            ) ?: 0

            if (written < 0) {
                Log.e(TAG, "AudioTrack write error: $written")
                break
            }

            framesGenerated += FRAME_SIZE_SAMPLES
            _currentPositionMs.value = (framesGenerated * 1000L / SAMPLE_RATE) % max(1L, _durationMs.value)
        }
    }

    /**
     * Streams and decodes device audio files (MP3, FLAC, M4A, WAV, AAC, OGG)
     * using Android MediaExtractor and MediaCodec.
     */
    private fun runDecoderLoop(uri: Uri) {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            val scheme = uri.scheme
            if (scheme == "content") {
                extractor.setDataSource(context, uri, null)
            } else {
                val filePath = uri.path ?: uri.toString()
                val file = File(filePath)
                if (file.exists() && file.isFile) {
                    val fis = java.io.FileInputStream(file)
                    try {
                        extractor.setDataSource(fis.fd)
                    } finally {
                        fis.close()
                    }
                } else {
                    extractor.setDataSource(context, uri, null)
                }
            }
            var audioTrackIndex = -1
            var format: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    format = f
                    break
                }
            }

            if (audioTrackIndex < 0 || format == null) {
                Log.e(TAG, "No audio track found in $uri")
                return
            }

            extractor.selectTrack(audioTrackIndex)
            val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) format.getLong(MediaFormat.KEY_DURATION) else 0L
            if (durationUs > 0) {
                _durationMs.value = durationUs / 1000L
            }

            val mime = format.getString(MediaFormat.KEY_MIME) ?: return
            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val info = MediaCodec.BufferInfo()
            var isEos = false
            val pcmAccumulator = FloatArray(INTERLEAVED_BUFFER_SIZE)
            var pcmAccumCount = 0

            while (isRunning.get() && !isEos) {
                val seekTarget = seekRequestedMs.getAndSet(-1L)
                if (seekTarget >= 0) {
                    try {
                        extractor.seekTo(seekTarget * 1000L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                        codec.flush()
                        pcmAccumCount = 0
                    } catch (e: Exception) {
                        Log.e(TAG, "Seek error: ${e.message}")
                    }
                }

                if (isPaused.get()) {
                    try {
                        Thread.sleep(20)
                    } catch (_: InterruptedException) {
                        break
                    }
                    continue
                }

                // Feed input to decoder
                val inIndex = codec.dequeueInputBuffer(10000L)
                if (inIndex >= 0) {
                    val inputBuf = codec.getInputBuffer(inIndex)
                    if (inputBuf != null) {
                        val sampleSize = extractor.readSampleData(inputBuf, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isEos = true
                        } else {
                            val sampleTimeUs = extractor.sampleTime
                            codec.queueInputBuffer(inIndex, 0, sampleSize, sampleTimeUs, 0)
                            extractor.advance()
                        }
                    }
                }

                // Retrieve decoded PCM from output
                val outIndex = codec.dequeueOutputBuffer(info, 10000L)
                if (outIndex >= 0) {
                    val outBuf = codec.getOutputBuffer(outIndex)
                    if (outBuf != null && info.size > 0) {
                        outBuf.position(info.offset)
                        outBuf.limit(info.offset + info.size)
                        outBuf.order(ByteOrder.LITTLE_ENDIAN)

                        // MediaCodec typically outputs 16-bit signed PCM
                        while (outBuf.remaining() >= 2) {
                            val sampleShort = outBuf.short
                            val sampleFloat = sampleShort / 32768.0f
                            pcmAccumulator[pcmAccumCount++] = sampleFloat

                            if (pcmAccumCount == INTERLEAVED_BUFFER_SIZE) {
                                // Full block ready: Run DSP
                                processDspChain(pcmAccumulator, FRAME_SIZE_SAMPLES)
                                feedVisualizer(pcmAccumulator, FRAME_SIZE_SAMPLES)

                                audioTrack?.write(pcmAccumulator, 0, INTERLEAVED_BUFFER_SIZE, AudioTrack.WRITE_BLOCKING)
                                pcmAccumCount = 0
                            }
                        }
                        _currentPositionMs.value = info.presentationTimeUs / 1000L
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                }

                if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Decoder loop error: ${e.message}", e)
        } finally {
            try {
                codec?.stop()
                codec?.release()
                extractor.release()
            } catch (_: Exception) {}
        }
    }

    /**
     * In-memory playback loop for raw PCM decrypted from the Vault.
     */
    private fun runMemoryPcmLoop(bytes: ByteArray) {
        val stream = ByteArrayInputStream(bytes)
        val byteBuf = ByteArray(INTERLEAVED_BUFFER_SIZE * 2) // 16-bit PCM: 2 bytes per sample
        val pcmBuffer = FloatArray(INTERLEAVED_BUFFER_SIZE)
        var totalBytesRead = 0L

        while (isRunning.get()) {
            if (isPaused.get()) {
                try {
                    Thread.sleep(20)
                } catch (_: InterruptedException) {
                    break
                }
                continue
            }

            val read = stream.read(byteBuf)
            if (read <= 0) {
                // Loop or stop
                break
            }

            val samplesToRead = read / 2
            val shortBuf = ByteBuffer.wrap(byteBuf, 0, read).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
            for (i in 0 until samplesToRead) {
                pcmBuffer[i] = shortBuf.get(i) / 32768.0f
            }
            for (i in samplesToRead until INTERLEAVED_BUFFER_SIZE) {
                pcmBuffer[i] = 0.0f
            }

            val frameCount = samplesToRead / CHANNEL_COUNT
            if (frameCount > 0) {
                processDspChain(pcmBuffer, frameCount)
                feedVisualizer(pcmBuffer, frameCount)
                audioTrack?.write(pcmBuffer, 0, frameCount * CHANNEL_COUNT, AudioTrack.WRITE_BLOCKING)
            }

            totalBytesRead += read
            val totalSamples = totalBytesRead / (CHANNEL_COUNT * 2)
            _currentPositionMs.value = totalSamples * 1000L / SAMPLE_RATE
        }
    }

    /**
     * Executes the master DSP processing chain:
     * 1. 10-Band Biquad Parametric Equalizer & Preamp
     * 2. Analogue Tube Warmth Saturation & Bass Exciter
     * 3. HRTF 3D Spatial Audio Positioning (ITD, ILD, Elevation, Distance)
     * 4. Schroeder Reverberator (Comb & All-pass diffusion)
     * 5. Mid-Side Stereo Holographic Expander
     * 6. Lookahead Peak Limiter (Clamps safely without clipping)
     */
    private fun processDspChain(buffer: FloatArray, frameCount: Int) {
        // Step 1: 10-Band EQ & Preamp
        equalizer.processInterleaved(buffer, frameCount)

        // Step 2: Tube Saturation
        tubeSaturator.processInterleaved(buffer, frameCount)

        // Step 3: HRTF 3D Spatial Audio
        spatialHrtf.processInterleaved(buffer, frameCount)

        // Step 4: Schroeder Reverb
        reverb.processInterleaved(buffer, frameCount)

        // Step 5: Stereo Holographic Expander
        stereoExpander.processInterleaved(buffer, frameCount)

        // Step 6: Master Dynamic Peak Limiter
        peakLimiter.processInterleaved(buffer, frameCount)
    }

    /**
     * Extracts time-domain waveform and computes FFT frequency spectrum.
     */
    private fun feedVisualizer(buffer: FloatArray, frameCount: Int) {
        // 1. Fill waveform snapshot (left channel)
        val waveStep = frameCount / WAVEFORM_POINTS
        if (waveStep > 0) {
            for (i in 0 until WAVEFORM_POINTS) {
                val sampleIdx = (i * waveStep) * CHANNEL_COUNT
                waveBuffer[i] = buffer[min(sampleIdx, buffer.size - 1)]
            }
            _waveformPoints.value = waveBuffer.clone()
        }

        // 2. Accumulate samples for FFT
        var inIdx = 0
        while (inIdx < frameCount * CHANNEL_COUNT && fftWritePos < 1024) {
            // Mono mix for spectrum
            fftInput[fftWritePos] = (buffer[inIdx] + buffer[inIdx + 1]) * 0.5f
            fftWritePos++
            inIdx += CHANNEL_COUNT
        }

        if (fftWritePos >= 1024) {
            fft.computeBands(fftInput, fftReal, fftImag, bandBuffer, SAMPLE_RATE)
            _spectrumBands.value = bandBuffer.clone()
            fftWritePos = 0
        }
    }
}
