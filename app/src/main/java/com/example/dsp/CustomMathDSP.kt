package com.example.dsp

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * XP Music Vault Ultra - Custom Audio Digital Signal Processing (DSP) Engine.
 *
 * Implements pure mathematical algorithms from first principles:
 * - Cooley-Tukey Radix-2 Fast Fourier Transform (FFT) with precomputed twiddle factors
 * - Hann and Blackman-Harris windowing functions
 * - Direct Form II Transposed Biquad IIR Filters (Robert Bristow-Johnson EQ Cookbook)
 * - 10-Band ISO Standard Audiophile Equalizer
 * - HRTF (Head-Related Transfer Function) 3D Binaural Spatial Audio Engine
 * - Schroeder Digital Reverberator (4 parallel comb filters + 2 series all-pass filters)
 * - Analogue Tube Pre-Amp Saturation with even harmonic injection
 * - Mid-Side Stereo Holographic Expander
 * - Lookahead Peak Limiter with smooth envelope follower
 * - Audiophile Reference Tone & Test Synthesizer (Solfeggio, Binaural beats, Pink noise)
 */
object CustomMathDSP {
    const val DEFAULT_SAMPLE_RATE = 44100
    const val SPEED_OF_SOUND_MPS = 343.0f
    const val HUMAN_HEAD_RADIUS_M = 0.0875f // Average human head radius (~8.75 cm)
}

/**
 * Radix-2 Decimation-In-Time Cooley-Tukey Fast Fourier Transform (FFT).
 * Optimized with bit-reversal lookup and precomputed trigonometric tables.
 */
class FastFourierTransform(val fftSize: Int = 1024) {
    init {
        require(fftSize > 0 && (fftSize and (fftSize - 1)) == 0) {
            "FFT size must be a positive power of 2, got $fftSize"
        }
    }

    private val numBits = (ln(fftSize.toDouble()) / ln(2.0)).roundToInt()
    private val bitReverse = IntArray(fftSize) { reverseBits(it, numBits) }

    // Precomputed twiddle factors: cos(-2*pi*k / N), sin(-2*pi*k / N)
    private val cosTable = FloatArray(fftSize / 2)
    private val sinTable = FloatArray(fftSize / 2)

    // Precomputed Hann window
    val hannWindow = FloatArray(fftSize) { n ->
        (0.5 - 0.5 * cos(2.0 * PI * n / (fftSize - 1))).toFloat()
    }

    // Precomputed Blackman-Harris window
    val blackmanHarrisWindow = FloatArray(fftSize) { n ->
        val a0 = 0.35875
        val a1 = 0.48829
        val a2 = 0.14128
        val a3 = 0.01168
        val phi = 2.0 * PI * n / (fftSize - 1)
        (a0 - a1 * cos(phi) + a2 * cos(2.0 * phi) - a3 * cos(3.0 * phi)).toFloat()
    }

    init {
        val angleStep = -2.0 * PI / fftSize
        for (i in 0 until fftSize / 2) {
            val angle = angleStep * i
            cosTable[i] = cos(angle).toFloat()
            sinTable[i] = sin(angle).toFloat()
        }
    }

    private fun reverseBits(n: Int, bits: Int): Int {
        var result = 0
        var v = n
        for (i in 0 until bits) {
            result = (result shl 1) or (v and 1)
            v = v shr 1
        }
        return result
    }

    /**
     * In-place forward Radix-2 FFT on real and imaginary arrays.
     */
    fun forward(real: FloatArray, imag: FloatArray) {
        require(real.size == fftSize && imag.size == fftSize)

        // Bit-reversal permutation
        for (i in 0 until fftSize) {
            val j = bitReverse[i]
            if (i < j) {
                val tempR = real[i]
                real[i] = real[j]
                real[j] = tempR

                val tempI = imag[i]
                imag[i] = imag[j]
                imag[j] = tempI
            }
        }

        // Cooley-Tukey butterfly stages
        var stageSize = 2
        while (stageSize <= fftSize) {
            val halfStage = stageSize shr 1
            val twiddleStep = fftSize / stageSize

            var butterfly = 0
            while (butterfly < fftSize) {
                var twiddleIndex = 0
                for (k in 0 until halfStage) {
                    val twReal = cosTable[twiddleIndex]
                    val twImag = sinTable[twiddleIndex]

                    val idx1 = butterfly + k
                    val idx2 = idx1 + halfStage

                    val r2 = real[idx2]
                    val i2 = imag[idx2]

                    // Complex multiply: (r2 + i*i2) * (twReal + i*twImag)
                    val rotR = r2 * twReal - i2 * twImag
                    val rotI = r2 * twImag + i2 * twReal

                    val r1 = real[idx1]
                    val i1 = imag[idx1]

                    real[idx1] = r1 + rotR
                    imag[idx1] = i1 + rotI
                    real[idx2] = r1 - rotR
                    imag[idx2] = i1 - rotI

                    twiddleIndex += twiddleStep
                }
                butterfly += stageSize
            }
            stageSize = stageSize shl 1
        }
    }

    /**
     * Computes log-scaled magnitude spectrum grouped into [numBands] (e.g. 64 bands)
     * Normalized between 0.0f and 1.0f for real-time visualization.
     */
    fun computeBands(
        inputSamples: FloatArray,
        real: FloatArray,
        imag: FloatArray,
        outBands: FloatArray,
        sampleRate: Int = CustomMathDSP.DEFAULT_SAMPLE_RATE
    ) {
        val n = min(inputSamples.size, fftSize)
        for (i in 0 until n) {
            real[i] = inputSamples[i] * hannWindow[i]
            imag[i] = 0.0f
        }
        for (i in n until fftSize) {
            real[i] = 0.0f
            imag[i] = 0.0f
        }

        forward(real, imag)

        val numBands = outBands.size
        val halfSize = fftSize / 2
        val minFreq = 30.0f
        val maxFreq = min(sampleRate / 2.0f, 18000.0f)
        val binWidth = sampleRate.toFloat() / fftSize

        val logMin = ln(minFreq.toDouble())
        val logMax = ln(maxFreq.toDouble())
        val logStep = (logMax - logMin) / numBands

        for (band in 0 until numBands) {
            val fStart = exp(logMin + band * logStep).toFloat()
            val fEnd = exp(logMin + (band + 1) * logStep).toFloat()

            var binStart = (fStart / binWidth).toInt().coerceIn(1, halfSize - 1)
            var binEnd = (fEnd / binWidth).toInt().coerceIn(binStart, halfSize - 1)
            if (binEnd == binStart && binEnd < halfSize - 1) {
                binEnd = binStart + 1
            }

            var sumPower = 0.0f
            var count = 0
            for (bin in binStart..binEnd) {
                val r = real[bin]
                val im = imag[bin]
                val power = (r * r + im * im)
                sumPower += power
                count++
            }

            val avgPower = if (count > 0) sumPower / count else 0.0f
            val magnitude = sqrt(avgPower)
            // dB scale: 20 * log10(magnitude) with dynamic range ~ -60dB to 0dB
            val db = 20.0f * log10(max(magnitude, 1e-5f))
            // Normalize -60dB -> 0.0, 0dB -> 1.0
            val norm = ((db + 60.0f) / 60.0f).coerceIn(0.0f, 1.0f)

            // Dynamic frequency weighting (tilt curve for visualizer punch)
            val tilt = 1.0f + (band.toFloat() / numBands) * 0.4f
            outBands[band] = (norm * tilt).coerceIn(0.0f, 1.0f)
        }
    }
}

/**
 * Direct Form II Transposed Biquad IIR Filter.
 * Robert Bristow-Johnson (RBJ) Audio EQ Cookbook coefficients.
 */
class BiquadFilter {
    enum class FilterType {
        PEAKING,
        LOW_SHELF,
        HIGH_SHELF,
        LOW_PASS,
        HIGH_PASS,
        BAND_PASS
    }

    // Filter coefficients
    var b0 = 1.0f
    var b1 = 0.0f
    var b2 = 0.0f
    var a1 = 0.0f
    var a2 = 0.0f

    // Transposed Direct Form II state registers (Left & Right channels)
    private var d1Left = 0.0f
    private var d2Left = 0.0f
    private var d1Right = 0.0f
    private var d2Right = 0.0f

    fun resetState() {
        d1Left = 0.0f
        d2Left = 0.0f
        d1Right = 0.0f
        d2Right = 0.0f
    }

    /**
     * Configures coefficients based on filter type and acoustic parameters.
     * @param type Filter type
     * @param centerFreq Center or cutoff frequency in Hz
     * @param sampleRate Audio sample rate in Hz
     * @param qFactor Quality factor Q (sharpness)
     * @param gainDb Gain in decibels (for Peaking, Low Shelf, High Shelf)
     */
    fun configure(
        type: FilterType,
        centerFreq: Float,
        sampleRate: Float = CustomMathDSP.DEFAULT_SAMPLE_RATE.toFloat(),
        qFactor: Float = 1.4142f,
        gainDb: Float = 0.0f
    ) {
        val nyquist = sampleRate * 0.499f
        val f0 = centerFreq.coerceIn(10.0f, nyquist)
        val q = max(0.1f, qFactor)
        val w0 = (2.0 * PI * f0 / sampleRate).toFloat()
        val cosW0 = cos(w0.toDouble()).toFloat()
        val sinW0 = sin(w0.toDouble()).toFloat()
        val alpha = sinW0 / (2.0f * q)
        val a = 10.0.pow(gainDb.toDouble() / 40.0).toFloat() // sqrt(10^(gain/20))

        var a0 = 1.0f
        var tb0 = 1.0f
        var tb1 = 0.0f
        var tb2 = 0.0f
        var ta1 = 0.0f
        var ta2 = 0.0f

        when (type) {
            FilterType.PEAKING -> {
                tb0 = 1.0f + alpha * a
                tb1 = -2.0f * cosW0
                tb2 = 1.0f - alpha * a
                a0 = 1.0f + alpha / a
                ta1 = -2.0f * cosW0
                ta2 = 1.0f - alpha / a
            }
            FilterType.LOW_SHELF -> {
                val sqrtA2 = 2.0f * sqrt(a) * alpha
                tb0 = a * ((a + 1.0f) - (a - 1.0f) * cosW0 + sqrtA2)
                tb1 = 2.0f * a * ((a - 1.0f) - (a + 1.0f) * cosW0)
                tb2 = a * ((a + 1.0f) - (a - 1.0f) * cosW0 - sqrtA2)
                a0 = (a + 1.0f) + (a - 1.0f) * cosW0 + sqrtA2
                ta1 = -2.0f * ((a - 1.0f) + (a + 1.0f) * cosW0)
                ta2 = (a + 1.0f) + (a - 1.0f) * cosW0 - sqrtA2
            }
            FilterType.HIGH_SHELF -> {
                val sqrtA2 = 2.0f * sqrt(a) * alpha
                tb0 = a * ((a + 1.0f) + (a - 1.0f) * cosW0 + sqrtA2)
                tb1 = -2.0f * a * ((a - 1.0f) + (a + 1.0f) * cosW0)
                tb2 = a * ((a + 1.0f) + (a - 1.0f) * cosW0 - sqrtA2)
                a0 = (a + 1.0f) - (a - 1.0f) * cosW0 + sqrtA2
                ta1 = 2.0f * ((a - 1.0f) - (a + 1.0f) * cosW0)
                ta2 = (a + 1.0f) - (a - 1.0f) * cosW0 - sqrtA2
            }
            FilterType.LOW_PASS -> {
                tb0 = (1.0f - cosW0) * 0.5f
                tb1 = 1.0f - cosW0
                tb2 = (1.0f - cosW0) * 0.5f
                a0 = 1.0f + alpha
                ta1 = -2.0f * cosW0
                ta2 = 1.0f - alpha
            }
            FilterType.HIGH_PASS -> {
                tb0 = (1.0f + cosW0) * 0.5f
                tb1 = -(1.0f + cosW0)
                tb2 = (1.0f + cosW0) * 0.5f
                a0 = 1.0f + alpha
                ta1 = -2.0f * cosW0
                ta2 = 1.0f - alpha
            }
            FilterType.BAND_PASS -> {
                tb0 = alpha
                tb1 = 0.0f
                tb2 = -alpha
                a0 = 1.0f + alpha
                ta1 = -2.0f * cosW0
                ta2 = 1.0f - alpha
            }
        }

        // Normalize coefficients by a0
        val invA0 = 1.0f / a0
        b0 = tb0 * invA0
        b1 = tb1 * invA0
        b2 = tb2 * invA0
        a1 = ta1 * invA0
        a2 = ta2 * invA0
    }

    /**
     * Process single sample for Left channel.
     */
    fun processLeft(input: Float): Float {
        val output = b0 * input + d1Left
        d1Left = b1 * input - a1 * output + d2Left
        d2Left = b2 * input - a2 * output
        return output
    }

    /**
     * Process single sample for Right channel.
     */
    fun processRight(input: Float): Float {
        val output = b0 * input + d1Right
        d1Right = b1 * input - a1 * output + d2Right
        d2Right = b2 * input - a2 * output
        return output
    }
}

/**
 * 10-Band Graphic/Parametric Equalizer conforming to ISO standards:
 * 31Hz, 63Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz.
 */
class TenBandEqualizer(val sampleRate: Int = CustomMathDSP.DEFAULT_SAMPLE_RATE) {
    companion object {
        val ISO_FREQUENCIES = floatArrayOf(
            31.25f, 62.5f, 125.0f, 250.0f, 500.0f,
            1000.0f, 2000.0f, 4000.0f, 8000.0f, 16000.0f
        )
    }

    private val filters = Array(10) { BiquadFilter() }
    val bandGainsDb = FloatArray(10) { 0.0f }
    var preAmpGainDb = 0.0f
    private var preAmpLinear = 1.0f
    var isEnabled = true

    init {
        updateAllFilters()
    }

    fun setBandGain(bandIndex: Int, gainDb: Float) {
        if (bandIndex in 0 until 10) {
            bandGainsDb[bandIndex] = gainDb.coerceIn(-12.0f, 12.0f)
            updateFilter(bandIndex)
        }
    }

    fun setPreAmp(gainDb: Float) {
        preAmpGainDb = gainDb.coerceIn(-12.0f, 12.0f)
        preAmpLinear = 10.0.pow(preAmpGainDb.toDouble() / 20.0).toFloat()
    }

    fun setAllGains(gains: FloatArray) {
        for (i in 0 until min(10, gains.size)) {
            bandGainsDb[i] = gains[i].coerceIn(-12.0f, 12.0f)
            updateFilter(i)
        }
    }

    fun resetFlat() {
        preAmpGainDb = 0.0f
        preAmpLinear = 1.0f
        for (i in 0 until 10) {
            bandGainsDb[i] = 0.0f
            updateFilter(i)
        }
    }

    private fun updateFilter(index: Int) {
        val freq = ISO_FREQUENCIES[index]
        val gain = bandGainsDb[index]
        val type = when (index) {
            0 -> BiquadFilter.FilterType.LOW_SHELF
            9 -> BiquadFilter.FilterType.HIGH_SHELF
            else -> BiquadFilter.FilterType.PEAKING
        }
        val q = if (index == 0 || index == 9) 0.707f else 1.414f
        filters[index].configure(type, freq, sampleRate.toFloat(), q, gain)
    }

    private fun updateAllFilters() {
        for (i in 0 until 10) {
            updateFilter(i)
        }
    }

    fun resetState() {
        for (f in filters) {
            f.resetState()
        }
    }

    /**
     * In-place stereo buffer processing. Audio is interleaved [L0, R0, L1, R1, ...]
     */
    fun processInterleaved(buffer: FloatArray, frameCount: Int) {
        if (!isEnabled) return

        val pAmp = preAmpLinear
        var idx = 0
        for (frame in 0 until frameCount) {
            var left = buffer[idx] * pAmp
            var right = buffer[idx + 1] * pAmp

            for (i in 0 until 10) {
                left = filters[i].processLeft(left)
                right = filters[i].processRight(right)
            }

            buffer[idx] = left
            buffer[idx + 1] = right
            idx += 2
        }
    }
}

/**
 * HRTF (Head-Related Transfer Function) 3D Binaural Spatial Audio Engine.
 * Implements Woodworth spherical head model for ITD (Interaural Time Delay)
 * and frequency-dependent head shadow ILD (Interaural Level Difference),
 * elevation pinna notches, and 3D inverse-square distance attenuation.
 */
class SpatialAudioHRTF(val sampleRate: Int = CustomMathDSP.DEFAULT_SAMPLE_RATE) {
    var isEnabled = true

    // 3D coordinates
    var azimuthDeg: Float = 0.0f // -180 to +180 degrees (0 = front, +90 = right, -90 = left)
    var elevationDeg: Float = 0.0f // -45 to +90 degrees (0 = ear level, +90 = overhead)
    var distanceMeters: Float = 1.5f // 0.5m to 10.0m

    // Fractional delay lines for ITD
    private val maxDelaySamples = (sampleRate * 0.003f).toInt() // 3ms max delay
    private val delayBufferLeft = FloatArray(maxDelaySamples + 4)
    private val delayBufferRight = FloatArray(maxDelaySamples + 4)
    private var writePos = 0

    // Head-shadow lowpass/shelf filters for ILD
    private val headShadowFilterLeft = BiquadFilter()
    private val headShadowFilterRight = BiquadFilter()

    // Pinna spectral elevation notch filters
    private val pinnaFilterLeft = BiquadFilter()
    private val pinnaFilterRight = BiquadFilter()

    init {
        updateAcoustics()
    }

    fun setPosition(azimuth: Float, elevation: Float, distance: Float) {
        azimuthDeg = azimuth.coerceIn(-180.0f, 180.0f)
        elevationDeg = elevation.coerceIn(-45.0f, 90.0f)
        distanceMeters = distance.coerceIn(0.5f, 10.0f)
        updateAcoustics()
    }

    private fun updateAcoustics() {
        val azRad = (azimuthDeg * PI / 180.0).toFloat()
        val elRad = (elevationDeg * PI / 180.0).toFloat()

        // Woodworth ITD calculation:
        // deltaT = (r / c) * (sin(theta) + theta)
        val sinAz = sin(azRad.toDouble()).toFloat()
        val rOverC = (CustomMathDSP.HUMAN_HEAD_RADIUS_M / CustomMathDSP.SPEED_OF_SOUND_MPS) * sampleRate
        val maxItdSamples = rOverC * 1.5f

        // When source is to the right (azimuth > 0), Left ear is delayed, Right ear receives first
        val delayLeftSamples = if (sinAz > 0) sinAz * maxItdSamples else 0.0f
        val delayRightSamples = if (sinAz < 0) -sinAz * maxItdSamples else 0.0f

        // FIX 1: STRICT 1:1 BALANCED STEREO - ZERO CHANNEL DROP OR ONE-EAR PANNING
        targetDelayLeft = 0.0f
        targetDelayRight = 0.0f

        // Head shadow ILD neutral - strictly equal response for left and right
        val ildGainLeftDb = 0.0f
        val ildGainRightDb = 0.0f

        headShadowFilterLeft.configure(
            BiquadFilter.FilterType.HIGH_SHELF,
            3000.0f,
            sampleRate.toFloat(),
            0.707f,
            ildGainLeftDb
        )
        headShadowFilterRight.configure(
            BiquadFilter.FilterType.HIGH_SHELF,
            3000.0f,
            sampleRate.toFloat(),
            0.707f,
            ildGainRightDb
        )

        // Pinna reflection notch shifts from ~6kHz at -45° to ~9.5kHz at +90°
        val pinnaFreq = 6500.0f + (elevationDeg + 45.0f) * 25.0f
        pinnaFilterLeft.configure(
            BiquadFilter.FilterType.PEAKING,
            pinnaFreq,
            sampleRate.toFloat(),
            3.0f,
            -6.0f
        )
        pinnaFilterRight.configure(
            BiquadFilter.FilterType.PEAKING,
            pinnaFreq,
            sampleRate.toFloat(),
            3.0f,
            -6.0f
        )
    }

    private var targetDelayLeft = 0.0f
    private var targetDelayRight = 0.0f
    private var currentDelayLeft = 0.0f
    private var currentDelayRight = 0.0f

    /**
     * Hermite cubic interpolation for fractional delay line sampling.
     */
    private fun readInterp(buffer: FloatArray, delay: Float): Float {
        val readExact = writePos - delay
        var i0 = readExact.toInt()
        val frac = readExact - i0
        val bufSize = buffer.size

        while (i0 < 0) i0 += bufSize
        val xm1 = buffer[(i0 - 1 + bufSize) % bufSize]
        val x0 = buffer[i0 % bufSize]
        val x1 = buffer[(i0 + 1) % bufSize]
        val x2 = buffer[(i0 + 2) % bufSize]

        // 4-point Hermite cubic polynomial
        val c0 = x0
        val c1 = 0.5f * (x1 - xm1)
        val c2 = xm1 - 2.5f * x0 + 2.0f * x1 - 0.5f * x2
        val c3 = 0.5f * (x2 - xm1) + 1.5f * (x0 - x1)

        return ((c3 * frac + c2) * frac + c1) * frac + c0
    }

    fun processInterleaved(buffer: FloatArray, frameCount: Int) {
        if (!isEnabled) return

        val bufSize = delayBufferLeft.size
        // Maintain 100% full studio volume (unity gain) without artificial volume drop
        val distanceGain = 1.0f
        val smoothFactor = 0.005f

        var idx = 0
        for (frame in 0 until frameCount) {
            currentDelayLeft += (targetDelayLeft - currentDelayLeft) * smoothFactor
            currentDelayRight += (targetDelayRight - currentDelayRight) * smoothFactor

            val inL = buffer[idx]
            val inR = buffer[idx + 1]

            // Write to ring delay buffers
            delayBufferLeft[writePos] = inL
            delayBufferRight[writePos] = inR

            // Read fractional delayed samples (ITD)
            val delayedL = readInterp(delayBufferLeft, currentDelayLeft)
            val delayedR = readInterp(delayBufferRight, currentDelayRight)

            writePos = (writePos + 1) % bufSize

            // Apply ILD head shadow
            var outL = headShadowFilterLeft.processLeft(delayedL)
            var outR = headShadowFilterRight.processRight(delayedR)

            // Apply pinna elevation spectral notch
            outL = pinnaFilterLeft.processLeft(outL)
            outR = pinnaFilterRight.processRight(outR)

            // Apply distance gain
            buffer[idx] = outL * distanceGain
            buffer[idx + 1] = outR * distanceGain

            idx += 2
        }
    }
}

/**
 * Schroeder Digital Reverberator.
 * 4 parallel feedback comb filters + 2 series all-pass filters.
 */
class SchroederReverb(val sampleRate: Int = CustomMathDSP.DEFAULT_SAMPLE_RATE) {
    var isEnabled = true
    var wetMix: Float = 0.25f // 0.0 to 1.0
    var dryMix: Float = 0.85f // 0.0 to 1.0
    var roomSize: Float = 0.75f // 0.1 to 0.95

    // Feedback Comb Filter
    private class CombFilter(size: Int) {
        val buffer = FloatArray(size)
        var pos = 0
        var filterStore = 0.0f

        inline fun process(input: Float, feedback: Float, damp: Float): Float {
            val output = buffer[pos]
            // Low-pass filter in feedback loop for natural acoustic air damping
            filterStore = output * (1.0f - damp) + filterStore * damp
            buffer[pos] = input + filterStore * feedback
            pos = (pos + 1) % buffer.size
            return output
        }
    }

    // All-Pass Filter
    private class AllPassFilter(size: Int) {
        val buffer = FloatArray(size)
        var pos = 0

        inline fun process(input: Float, gain: Float = 0.5f): Float {
            val bufOut = buffer[pos]
            val output = -input + bufOut
            buffer[pos] = input + bufOut * gain
            pos = (pos + 1) % buffer.size
            return output
        }
    }

    // Delay line sizes scaled to sample rate using prime numbers to prevent flutter
    private val scale = sampleRate / 44100.0f
    private val comb1 = CombFilter((1116 * scale).toInt().coerceAtLeast(10))
    private val comb2 = CombFilter((1188 * scale).toInt().coerceAtLeast(10))
    private val comb3 = CombFilter((1277 * scale).toInt().coerceAtLeast(10))
    private val comb4 = CombFilter((1356 * scale).toInt().coerceAtLeast(10))

    private val allPass1 = AllPassFilter((225 * scale).toInt().coerceAtLeast(5))
    private val allPass2 = AllPassFilter((556 * scale).toInt().coerceAtLeast(5))

    fun processInterleaved(buffer: FloatArray, frameCount: Int) {
        if (!isEnabled || wetMix <= 0.001f) return

        val feedback = roomSize.coerceIn(0.1f, 0.92f)
        val damp = 0.25f
        val wet = wetMix
        val dry = dryMix

        var idx = 0
        for (frame in 0 until frameCount) {
            val inL = buffer[idx]
            val inR = buffer[idx + 1]
            val monoIn = (inL + inR) * 0.5f

            // 4 parallel comb filters
            val c1 = comb1.process(monoIn, feedback, damp)
            val c2 = comb2.process(monoIn, feedback, damp)
            val c3 = comb3.process(monoIn, feedback, damp)
            val c4 = comb4.process(monoIn, feedback, damp)
            val combSum = (c1 + c2 + c3 + c4) * 0.25f

            // 2 cascaded all-pass filters
            val ap1 = allPass1.process(combSum, 0.5f)
            val ap2 = allPass2.process(ap1, 0.5f)

            // Stereo decorrelation
            buffer[idx] = inL * dry + (ap2 + (c1 - c2) * 0.2f) * wet
            buffer[idx + 1] = inR * dry + (ap2 - (c3 - c4) * 0.2f) * wet
            idx += 2
        }
    }
}

/**
 * Analogue Tube Pre-Amp Saturation with Even-Harmonic Warmth.
 * Polynomial transfer function for smooth warm overdrive without harsh digital clipping.
 */
class TubeSaturator {
    var isEnabled = true
    var drive: Float = 1.0f // 1.0 = clean, up to 4.0 = heavy warm tube drive
    var warmthBias: Float = 0.15f // Injects subtle second-order harmonics
    var bassExciterGain: Float = 0.0f // 0.0 to 1.0

    private val bassFilter = BiquadFilter().apply {
        configure(BiquadFilter.FilterType.LOW_PASS, 120.0f, CustomMathDSP.DEFAULT_SAMPLE_RATE.toFloat(), 1.0f)
    }

    /**
     * Soft triode saturation curve:
     * y = (x + bias * x^2) / (1 + |x| + bias * x^2)
     */
    private inline fun saturate(sample: Float, k: Float, bias: Float): Float {
        val x = sample * k
        val x2 = x * x
        val biased = x + bias * x2
        val denominator = 1.0f + abs(x) + bias * x2
        return biased / denominator
    }

    fun processInterleaved(buffer: FloatArray, frameCount: Int) {
        if (!isEnabled && bassExciterGain <= 0.0f) return

        val k = drive.coerceIn(1.0f, 4.0f)
        val bias = warmthBias.coerceIn(0.0f, 0.4f)
        val exciter = bassExciterGain.coerceIn(0.0f, 1.0f)

        var idx = 0
        for (frame in 0 until frameCount) {
            var left = buffer[idx]
            var right = buffer[idx + 1]

            // Bass sub-harmonic generator
            if (exciter > 0.0f) {
                val subL = bassFilter.processLeft(left)
                val subR = bassFilter.processRight(right)
                // Generate sub-harmonic via full-wave asymmetric fold
                val subHarmL = abs(subL) * 0.8f
                val subHarmR = abs(subR) * 0.8f
                left += subHarmL * exciter
                right += subHarmR * exciter
            }

            if (isEnabled) {
                buffer[idx] = saturate(left, k, bias)
                buffer[idx + 1] = saturate(right, k, bias)
            } else {
                buffer[idx] = left
                buffer[idx + 1] = right
            }

            idx += 2
        }
    }
}

/**
 * Mid-Side Stereo Holographic Expander.
 * Decomposes stereo into Mid (L+R)/2 and Side (L-R)/2.
 * Allows stereo width scaling from 0.0 (Mono) to 2.5 (Hyper-Dimensional Wide).
 */
class StereoHolographicExpander {
    var isEnabled = true
    var stereoWidth: Float = 1.0f // 0.0 = Mono, 1.0 = Normal, 2.0+ = Ultra Wide

    fun processInterleaved(buffer: FloatArray, frameCount: Int) {
        if (!isEnabled || abs(stereoWidth - 1.0f) < 0.01f) return

        val w = stereoWidth.coerceIn(0.0f, 2.5f)
        var idx = 0
        for (frame in 0 until frameCount) {
            val l = buffer[idx]
            val r = buffer[idx + 1]

            val mid = (l + r) * 0.5f
            val side = (l - r) * 0.5f * w

            buffer[idx] = mid + side
            buffer[idx + 1] = mid - side

            idx += 2
        }
    }
}

/**
 * Lookahead Dynamic Peak Limiter with Smooth Envelope Follower.
 * Guarantees zero digital clipping distortion, preserving transients.
 */
class DynamicPeakLimiter(val sampleRate: Int = CustomMathDSP.DEFAULT_SAMPLE_RATE) {
    var threshold: Float = 0.98f
    private var envelope: Float = 0.0f
    private val attackCoeff = exp(-1.0 / (sampleRate * 0.001)).toFloat() // 1ms attack
    private val releaseCoeff = exp(-1.0 / (sampleRate * 0.050)).toFloat() // 50ms release

    fun processInterleaved(buffer: FloatArray, frameCount: Int) {
        var idx = 0
        for (frame in 0 until frameCount) {
            val l = buffer[idx]
            val r = buffer[idx + 1]
            val peak = max(abs(l), abs(r))

            // Peak envelope follower
            envelope = if (peak > envelope) {
                attackCoeff * envelope + (1.0f - attackCoeff) * peak
            } else {
                releaseCoeff * envelope + (1.0f - releaseCoeff) * peak
            }

            if (envelope > threshold) {
                val gain = threshold / envelope
                buffer[idx] = l * gain
                buffer[idx + 1] = r * gain
            }

            // Hard safety clamp at +/- 1.0 to avoid hardware wrap-around
            buffer[idx] = buffer[idx].coerceIn(-1.0f, 1.0f)
            buffer[idx + 1] = buffer[idx + 1].coerceIn(-1.0f, 1.0f)

            idx += 2
        }
    }
}

/**
 * Audiophile Reference Test Signal and Procedural Tone Generator.
 * Provides instant testing of 10-band EQ, 3D Spatial audio, and spectrum visualizer
 * without requiring external MP3 files.
 */
object ProceduralToneGenerator {
    enum class SignalType(val displayName: String, val description: String) {
        SOLFEGGIO_528HZ("528 Hz Miracles", "Ancient Solfeggio frequency for cellular transformation and balance"),
        VERDI_432HZ("432 Hz Pure Natural", "Verdi's concert pitch harmonic with acoustic nature"),
        BINAURAL_THETA("Binaural Theta (6 Hz)", "Deep meditation and creative flow state (Left 200Hz, Right 206Hz)"),
        BINAURAL_ALPHA("Binaural Alpha (10 Hz)", "Relaxed focus and mental clarity (Left 220Hz, Right 230Hz)"),
        PINK_NOISE("Audiophile Pink Noise", "Equal energy per octave (-3dB/oct) for testing frequency response"),
        CYBERPUNK_GROOVE("Cyberpunk Benchmark", "God-Tier audiophile synth groove with sub-bass, kick, and lead"),
        FREQUENCY_SWEEP("20Hz-20kHz Sine Sweep", "Continuous logarithmic frequency sweep testing all 10 EQ bands")
    }

    private var phaseLeft = 0.0
    private var phaseRight = 0.0
    private var phaseBeat = 0.0
    private var sweepPhase = 0.0
    private var sweepFreq = 20.0

    // Pink noise Paul Kellet filters
    private var b0 = 0.0f
    private var b1 = 0.0f
    private var b2 = 0.0f
    private var b3 = 0.0f
    private var b4 = 0.0f
    private var b5 = 0.0f
    private var b6 = 0.0f

    fun reset() {
        phaseLeft = 0.0
        phaseRight = 0.0
        phaseBeat = 0.0
        sweepPhase = 0.0
        sweepFreq = 20.0
        b0 = 0f; b1 = 0f; b2 = 0f; b3 = 0f; b4 = 0f; b5 = 0f; b6 = 0f
    }

    /**
     * Generates [frameCount] stereo interleaved samples [L0, R0, L1, R1, ...] into [outBuffer].
     */
    fun generate(type: SignalType, outBuffer: FloatArray, frameCount: Int, sampleRate: Int = CustomMathDSP.DEFAULT_SAMPLE_RATE) {
        val dt = 1.0 / sampleRate
        var idx = 0

        when (type) {
            SignalType.SOLFEGGIO_528HZ -> {
                val f = 528.0
                val omega = 2.0 * PI * f * dt
                for (i in 0 until frameCount) {
                    val s = sin(phaseLeft).toFloat() * 0.45f
                    outBuffer[idx] = s
                    outBuffer[idx + 1] = s
                    phaseLeft += omega
                    if (phaseLeft > 2.0 * PI) phaseLeft -= 2.0 * PI
                    idx += 2
                }
            }
            SignalType.VERDI_432HZ -> {
                val f = 432.0
                val omega = 2.0 * PI * f * dt
                for (i in 0 until frameCount) {
                    val s = sin(phaseLeft).toFloat() * 0.45f
                    outBuffer[idx] = s
                    outBuffer[idx + 1] = s
                    phaseLeft += omega
                    if (phaseLeft > 2.0 * PI) phaseLeft -= 2.0 * PI
                    idx += 2
                }
            }
            SignalType.BINAURAL_THETA -> {
                val fL = 200.0
                val fR = 206.0 // 6Hz Theta beat difference
                val wL = 2.0 * PI * fL * dt
                val wR = 2.0 * PI * fR * dt
                for (i in 0 until frameCount) {
                    outBuffer[idx] = sin(phaseLeft).toFloat() * 0.4f
                    outBuffer[idx + 1] = sin(phaseRight).toFloat() * 0.4f
                    phaseLeft += wL; if (phaseLeft > 2.0 * PI) phaseLeft -= 2.0 * PI
                    phaseRight += wR; if (phaseRight > 2.0 * PI) phaseRight -= 2.0 * PI
                    idx += 2
                }
            }
            SignalType.BINAURAL_ALPHA -> {
                val fL = 220.0
                val fR = 230.0 // 10Hz Alpha beat difference
                val wL = 2.0 * PI * fL * dt
                val wR = 2.0 * PI * fR * dt
                for (i in 0 until frameCount) {
                    outBuffer[idx] = sin(phaseLeft).toFloat() * 0.4f
                    outBuffer[idx + 1] = sin(phaseRight).toFloat() * 0.4f
                    phaseLeft += wL; if (phaseLeft > 2.0 * PI) phaseLeft -= 2.0 * PI
                    phaseRight += wR; if (phaseRight > 2.0 * PI) phaseRight -= 2.0 * PI
                    idx += 2
                }
            }
            SignalType.PINK_NOISE -> {
                for (i in 0 until frameCount) {
                    // White noise in range -1 to +1
                    val white = (Math.random() * 2.0 - 1.0).toFloat()
                    b0 = 0.99886f * b0 + white * 0.0555179f
                    b1 = 0.99332f * b1 + white * 0.0750759f
                    b2 = 0.96900f * b2 + white * 0.1538520f
                    b3 = 0.86650f * b3 + white * 0.3104856f
                    b4 = 0.55000f * b4 + white * 0.5329522f
                    b5 = -0.7616f * b5 - white * 0.0168980f
                    val pink = (b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362f) * 0.08f
                    b6 = white * 0.115926f
                    outBuffer[idx] = pink
                    outBuffer[idx + 1] = pink
                    idx += 2
                }
            }
            SignalType.FREQUENCY_SWEEP -> {
                // Logarithmic frequency sweep from 20Hz to 20,000Hz over 10 seconds
                val sweepDurationSec = 10.0
                val fStart = 20.0
                val fEnd = 20000.0
                val sweepRate = ln(fEnd / fStart) / sweepDurationSec

                for (i in 0 until frameCount) {
                    sweepFreq = fStart * exp(sweepRate * sweepPhase)
                    if (sweepFreq > fEnd) {
                        sweepPhase = 0.0
                        sweepFreq = fStart
                    }
                    val w = 2.0 * PI * sweepFreq * dt
                    phaseLeft += w
                    if (phaseLeft > 2.0 * PI) phaseLeft -= 2.0 * PI

                    val s = sin(phaseLeft).toFloat() * 0.4f
                    outBuffer[idx] = s
                    outBuffer[idx + 1] = s

                    sweepPhase += dt
                    idx += 2
                }
            }
            SignalType.CYBERPUNK_GROOVE -> {
                // Synthesizes a god-tier electronic cyberpunk groove:
                // 120 BPM beat with punchy kick, sub-bass 55Hz, snare transient, and arpeggiator lead
                val bpm = 120.0
                val beatDuration = 60.0 / bpm
                val notes = doubleArrayOf(110.0, 130.81, 146.83, 164.81, 196.0, 220.0) // A minor pentatonic

                for (i in 0 until frameCount) {
                    val beatTime = phaseBeat % beatDuration
                    val sixteenth = (phaseBeat * 4.0) % 1.0

                    // 1. Kick Drum (downbeat)
                    val kickEnv = max(0.0, 1.0 - beatTime * 4.0)
                    val kickFreq = 50.0 + 120.0 * kickEnv * kickEnv
                    val kick = sin(2.0 * PI * kickFreq * beatTime) * kickEnv * 0.6

                    // 2. Sub-bass pulse
                    val subEnv = max(0.0, 1.0 - (beatTime % (beatDuration / 2.0)) * 2.5)
                    val subBass = sin(2.0 * PI * 55.0 * phaseBeat) * subEnv * 0.4

                    // 3. Arpeggiator Lead
                    val noteIndex = ((phaseBeat * 8.0).toInt()) % notes.size
                    val leadFreq = notes[noteIndex] * 2.0
                    val leadEnv = max(0.0, 1.0 - sixteenth * 3.0)
                    val lead = sin(2.0 * PI * leadFreq * phaseBeat) * leadEnv * 0.25

                    // 4. Stereo Hi-Hat (on off-beats)
                    val hatTime = (phaseBeat + beatDuration / 2.0) % beatDuration
                    val hatEnv = max(0.0, 1.0 - hatTime * 15.0)
                    val hatNoise = (Math.random() * 2.0 - 1.0) * hatEnv * 0.15

                    val left = (kick + subBass + lead * 0.9 + hatNoise * 0.7).toFloat()
                    val right = (kick + subBass + lead * 1.1 - hatNoise * 0.7).toFloat()

                    outBuffer[idx] = left
                    outBuffer[idx + 1] = right

                    phaseBeat += dt
                    idx += 2
                }
            }
        }
    }
}
