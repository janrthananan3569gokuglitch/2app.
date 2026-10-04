package com.example

import com.example.dsp.BiquadFilter
import com.example.dsp.DynamicPeakLimiter
import com.example.dsp.FastFourierTransform
import com.example.dsp.ProceduralToneGenerator
import com.example.dsp.SchroederReverb
import com.example.dsp.SpatialAudioHRTF
import com.example.dsp.TenBandEqualizer
import com.example.dsp.TubeSaturator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class CustomDspUnitTest {

    @Test
    fun testFastFourierTransform_sineTonePeak() {
        val fftSize = 1024
        val fft = FastFourierTransform(fftSize)
        val sampleRate = 44100

        // Generate 1000 Hz pure sine wave
        val input = FloatArray(fftSize) { i ->
            sin(2.0 * PI * 1000.0 * i / sampleRate).toFloat()
        }

        val real = FloatArray(fftSize)
        val imag = FloatArray(fftSize)
        val bands = FloatArray(64)

        fft.computeBands(input, real, imag, bands, sampleRate)

        // Find max band
        var maxBand = 0
        var maxVal = 0.0f
        for (i in bands.indices) {
            if (bands[i] > maxVal) {
                maxVal = bands[i]
                maxBand = i
            }
        }

        // 1000 Hz should peak around the mid bands in a 30Hz - 18kHz log scale (around band 30-40)
        assertTrue("Max band should be between 25 and 45 for 1kHz tone, got $maxBand", maxBand in 25..45)
        assertTrue("Magnitude at peak should be high", maxVal > 0.4f)
    }

    @Test
    fun testBiquadFilter_stabilityAndGain() {
        val filter = BiquadFilter()
        filter.configure(
            type = BiquadFilter.FilterType.PEAKING,
            centerFreq = 1000.0f,
            sampleRate = 44100.0f,
            qFactor = 1.414f,
            gainDb = 6.0f
        )

        var maxOut = 0.0f
        for (i in 0 until 1000) {
            val input = sin(2.0 * PI * 1000.0 * i / 44100.0).toFloat()
            val out = filter.processLeft(input)
            assertFalse("Filter output should not be NaN", out.isNaN())
            if (abs(out) > maxOut) maxOut = abs(out)
        }

        // With +6dB gain at center frequency, amplitude should be roughly ~2.0
        assertTrue("Peak gain should be near 2.0 (+6dB), got $maxOut", maxOut in 1.4f..2.5f)
    }

    @Test
    fun testTenBandEqualizer_allBandsProcess() {
        val eq = TenBandEqualizer(44100)
        eq.setBandGain(0, 4.0f) // 31 Hz
        eq.setBandGain(5, -3.0f) // 1 kHz
        eq.setBandGain(9, 5.0f) // 16 kHz

        val buffer = FloatArray(1024) { (Math.random() * 0.4 - 0.2).toFloat() }
        eq.processInterleaved(buffer, 512)

        for (sample in buffer) {
            assertFalse("EQ output should not be NaN", sample.isNaN())
            assertTrue("EQ output should remain bounded", abs(sample) < 5.0f)
        }
    }

    @Test
    fun testDynamicPeakLimiter_clampsSpikes() {
        val limiter = DynamicPeakLimiter(44100)
        limiter.threshold = 0.98f

        // Massive 5.0 amplitude input spike
        val buffer = FloatArray(512) { 5.0f }
        limiter.processInterleaved(buffer, 256)

        for (sample in buffer) {
            assertTrue("Limiter must clamp strictly within [-1.0, 1.0], got $sample", abs(sample) <= 1.0f)
        }
    }

    @Test
    fun testProceduralSynthesizer_generatesAudio() {
        val buffer = FloatArray(1024)
        ProceduralToneGenerator.generate(
            ProceduralToneGenerator.SignalType.CYBERPUNK_GROOVE,
            buffer,
            512,
            44100
        )

        var hasNonZero = false
        for (s in buffer) {
            if (abs(s) > 0.001f) {
                hasNonZero = true
                break
            }
        }
        assertTrue("Procedural synthesizer must produce audio data", hasNonZero)
    }

    @Test
    fun testSpatialAudioHRTF_positions() {
        val hrtf = SpatialAudioHRTF(44100)
        hrtf.setPosition(45.0f, 15.0f, 2.0f)

        val buffer = FloatArray(1024) { 0.3f }
        hrtf.processInterleaved(buffer, 512)

        for (s in buffer) {
            assertFalse("HRTF output should not be NaN", s.isNaN())
        }
    }
}
