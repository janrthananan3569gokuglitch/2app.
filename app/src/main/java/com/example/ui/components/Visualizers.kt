package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonPlasmaPink
import com.example.ui.theme.VaultCardBorder
import com.example.ui.theme.VaultObsidianDark
import com.example.ui.theme.VaultSurfaceDark
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * 64-Band Real-Time Fast Fourier Transform (FFT) Neon Spectrum Analyzer.
 * Features peak-hold ballistic needles, gradient frequency bars, and hardware canvas acceleration.
 */
@Composable
fun SpectrumVisualizer(
    bands: FloatArray,
    paletteSeed: Int = 0,
    modifier: Modifier = Modifier
) {
    // Dynamic 5-Palette Morphing System
    val paletteIndex = kotlin.math.abs(paletteSeed) % 5
    val (targetPeak, targetMid, targetBase) = when (paletteIndex) {
        1 -> Triple(Color(0xFFFFFFFF), Color(0xFFFF2A00), Color(0xFF8B0000)) // Crimson Phonk Fire
        2 -> Triple(Color(0xFF00FFCC), Color(0xFF00B4D8), Color(0xFF7B2CBF)) // Deep Aurora
        3 -> Triple(Color(0xFFFFFFFF), Color(0xFFFFD700), Color(0xFFFF8C00)) // Obsidian Gold
        4 -> Triple(Color(0xFFF72585), Color(0xFF7209B7), Color(0xFF06D6A0)) // Acid Emerald
        else -> Triple(NeonPlasmaPink, NeonAmber, NeonCyan)                  // Cyberpunk Synth
    }

    // Fluid 1250ms color transitions on song change
    val morphPeak by animateColorAsState(targetPeak, tween(1250, easing = FastOutSlowInEasing), label = "morphPeak")
    val morphMid by animateColorAsState(targetMid, tween(1250, easing = FastOutSlowInEasing), label = "morphMid")
    val morphBase by animateColorAsState(targetBase, tween(1250, easing = FastOutSlowInEasing), label = "morphBase")

    // Peak hold positions and decay state
    val numBands = bands.size
    val peakLevels = remember(numBands) { FloatArray(numBands) }

    LaunchedEffect(bands) {
        val gravity = 0.04f
        for (i in 0 until numBands) {
            val curr = bands[i]
            if (curr > peakLevels[i]) {
                peakLevels[i] = curr
            } else {
                peakLevels[i] = max(0.0f, peakLevels[i] - gravity)
            }
        }
    }

    Canvas(
        modifier = modifier
            .testTag("spectrum_visualizer_canvas")
            .clip(RoundedCornerShape(12.dp))
            .background(VaultSurfaceDark)
            .border(1.dp, VaultCardBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 6.dp, vertical = 8.dp)
    ) {
        val width = size.width
        val height = size.height
        if (numBands == 0) return@Canvas

        val barSpacing = 2.dp.toPx()
        val totalSpacing = barSpacing * (numBands - 1)
        val barWidth = max(1.5f, (width - totalSpacing) / numBands)

        // Draw background grid lines (decibel references)
        val gridLines = 4
        for (g in 1..gridLines) {
            val y = height * (g.toFloat() / (gridLines + 1))
            drawLine(
                color = VaultCardBorder.copy(alpha = 0.4f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw frequency bars with morphed palette
        for (i in 0 until numBands) {
            val barVal = bands[i].coerceIn(0.02f, 1.0f)
            val barHeight = barVal * height
            val x = i * (barWidth + barSpacing)
            val y = height - barHeight

            val gradientBrush = Brush.verticalGradient(
                colors = listOf(
                    morphPeak,
                    morphMid,
                    morphBase
                ),
                startY = y,
                endY = height
            )

            drawRoundRect(
                brush = gradientBrush,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
            )

            // Draw Peak Hold Needle
            val peakVal = peakLevels[i].coerceIn(0.02f, 1.0f)
            val peakY = height - (peakVal * height)
            drawLine(
                color = Color.White.copy(alpha = 0.9f),
                start = Offset(x, peakY),
                end = Offset(x + barWidth, peakY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * High-Speed Cathode-Ray Style Oscilloscope Time-Domain Waveform Visualizer.
 */
@Composable
fun OscilloscopeVisualizer(
    waveform: FloatArray,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .testTag("oscilloscope_visualizer_canvas")
            .clip(RoundedCornerShape(12.dp))
            .background(VaultSurfaceDark)
            .border(1.dp, VaultCardBorder, RoundedCornerShape(12.dp))
            .padding(6.dp)
    ) {
        val width = size.width
        val height = size.height
        val midY = height / 2f
        val points = waveform.size
        if (points < 2) return@Canvas

        // Draw oscilloscope graticule
        drawLine(
            color = VaultCardBorder,
            start = Offset(0f, midY),
            end = Offset(width, midY),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = VaultCardBorder,
            start = Offset(width / 2f, 0f),
            end = Offset(width / 2f, height),
            strokeWidth = 1.dp.toPx()
        )

        // Draw waveform path
        val path = Path()
        val stepX = width / (points - 1)

        val firstY = midY - (waveform[0] * midY * 0.9f)
        path.moveTo(0f, firstY.coerceIn(0f, height))

        for (i in 1 until points) {
            val x = i * stepX
            val y = (midY - (waveform[i] * midY * 0.9f)).coerceIn(0f, height)
            path.lineTo(x, y)
        }

        // Draw glow line and crisp line
        drawPath(
            path = path,
            color = NeonEmerald.copy(alpha = 0.35f),
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )
        drawPath(
            path = path,
            color = NeonEmerald,
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

/**
 * Holographic Vinyl Vault Core.
 * Rotates dynamically with spinning vinyl grooves and dynamic neon audio pulse.
 */
@Composable
fun HolographicVinylCore(
    isPlaying: Boolean,
    audioEnergy: Float,
    spectrumBands: FloatArray = FloatArray(0),
    paletteSeed: Int = 0,
    modifier: Modifier = Modifier
) {
    // Dynamic 5-Palette Morphing System for Vinyl
    val paletteIndex = kotlin.math.abs(paletteSeed) % 5
    val (targetPeak, targetMid, targetBase) = when (paletteIndex) {
        1 -> Triple(Color(0xFFFFFFFF), Color(0xFFFF2A00), Color(0xFF8B0000)) // Crimson Phonk Fire
        2 -> Triple(Color(0xFF00FFCC), Color(0xFF00B4D8), Color(0xFF7B2CBF)) // Deep Aurora
        3 -> Triple(Color(0xFFFFFFFF), Color(0xFFFFD700), Color(0xFFFF8C00)) // Obsidian Gold
        4 -> Triple(Color(0xFFF72585), Color(0xFF7209B7), Color(0xFF06D6A0)) // Acid Emerald
        else -> Triple(NeonPlasmaPink, NeonAmber, NeonCyan)                  // Cyberpunk Synth
    }

    val morphPeak by animateColorAsState(targetPeak, tween(1250, easing = FastOutSlowInEasing), label = "vinylPeak")
    val morphMid by animateColorAsState(targetMid, tween(1250, easing = FastOutSlowInEasing), label = "vinylMid")
    val morphBase by animateColorAsState(targetBase, tween(1250, easing = FastOutSlowInEasing), label = "vinylBase")

    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val currentRotation = if (isPlaying) rotation else 0f

    Box(
        modifier = modifier
            .testTag("holographic_vinyl_core")
            .size(240.dp),
        contentAlignment = Alignment.Center
    ) {
        // Dynamic Outer Neon Energy Aura & Radial Spectrum Equalizer Bars
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.width * 0.44f
            val pulseRadius = baseRadius + (audioEnergy * 14.dp.toPx())

            // Glow Aura with Morphed Palette
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        morphBase.copy(alpha = 0.35f * audioEnergy.coerceIn(0.1f, 1f)),
                        morphPeak.copy(alpha = 0.15f * audioEnergy.coerceIn(0.1f, 1f)),
                        Color.Transparent
                    ),
                    center = center,
                    radius = pulseRadius
                ),
                center = center,
                radius = pulseRadius
            )

            // Radial Audio EQ Spectrum Bars pulsing to the music
            val numBands = minOf(64, spectrumBands.size)
            if (numBands > 0) {
                val barBaseR = size.width * 0.43f
                for (i in 0 until numBands) {
                    val amp = spectrumBands[i].coerceIn(0.04f, 1.0f)
                    val barLen = (amp * 20.dp.toPx())
                    val angle = (i.toFloat() / numBands) * (2.0 * Math.PI)
                    val cosA = kotlin.math.cos(angle).toFloat()
                    val sinA = kotlin.math.sin(angle).toFloat()

                    val start = Offset(center.x + cosA * barBaseR, center.y + sinA * barBaseR)
                    val end = Offset(center.x + cosA * (barBaseR + barLen), center.y + sinA * (barBaseR + barLen))

                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(morphBase, morphPeak),
                            start = start,
                            end = end
                        ),
                        start = start,
                        end = end,
                        strokeWidth = 2.4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        // Spinning Vinyl Disc with Grooves
        Canvas(
            modifier = Modifier
                .size(210.dp)
                .rotate(currentRotation)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.width / 2f

            // Vinyl Base
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF1E2638), Color(0xFF0A0F1A), Color(0xFF04060A)),
                    center = center,
                    radius = outerRadius
                ),
                center = center,
                radius = outerRadius
            )

            // Vinyl Sound Grooves
            val grooveCount = 8
            for (g in 1..grooveCount) {
                val r = outerRadius * (0.42f + g * 0.065f)
                drawCircle(
                    color = Color.White.copy(alpha = 0.06f),
                    center = center,
                    radius = r,
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // High-Tech Light Sheen / Reflection Angle
            val sheenPath = Path().apply {
                moveTo(center.x, center.y)
                lineTo(center.x + outerRadius * 0.8f, center.y - outerRadius * 0.8f)
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(
                        center.x - outerRadius,
                        center.y - outerRadius,
                        center.x + outerRadius,
                        center.y + outerRadius
                    ),
                    startAngleDegrees = -45f,
                    sweepAngleDegrees = 20f,
                    forceMoveTo = false
                )
                close()
            }
            drawPath(
                path = sheenPath,
                color = Color.White.copy(alpha = 0.05f)
            )

            // Inner Vault Center Disc with Morphed Accent
            val centerDiscRadius = outerRadius * 0.38f
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(morphMid, morphPeak),
                    start = Offset(center.x - centerDiscRadius, center.y - centerDiscRadius),
                    end = Offset(center.x + centerDiscRadius, center.y + centerDiscRadius)
                ),
                center = center,
                radius = centerDiscRadius
            )

            // Vault Lock Center Spindle
            drawCircle(
                color = VaultObsidianDark,
                center = center,
                radius = 12.dp.toPx()
            )
            drawCircle(
                color = morphBase,
                center = center,
                radius = 4.dp.toPx()
            )
        }
    }
}

/**
 * Interactive 3D Spatial Audio Radar Soundstage Canvas.
 * Displays the listener's head in the center and draggable audio sound orb in 3D orbit.
 */
@Composable
fun SpatialRadarSoundstage(
    azimuthDeg: Float,
    distanceMeters: Float,
    onPositionChanged: (azimuth: Float, distance: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .testTag("spatial_radar_soundstage")
            .clip(RoundedCornerShape(16.dp))
            .background(VaultSurfaceDark)
            .border(1.dp, VaultCardBorder, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = min(size.width, size.height) * 0.44f

        // Radar Distance Concentric Circles (0.5m, 1.5m, 3m, 5m)
        val rings = 4
        for (r in 1..rings) {
            val ringRadius = maxRadius * (r.toFloat() / rings)
            drawCircle(
                color = VaultCardBorder.copy(alpha = 0.5f),
                center = center,
                radius = ringRadius,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Radar Crosshairs (Azimuth 0°, 90°, 180°, -90°)
        drawLine(
            color = VaultCardBorder.copy(alpha = 0.5f),
            start = Offset(center.x, center.y - maxRadius),
            end = Offset(center.x, center.y + maxRadius),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = VaultCardBorder.copy(alpha = 0.5f),
            start = Offset(center.x - maxRadius, center.y),
            end = Offset(center.x + maxRadius, center.y),
            strokeWidth = 1.dp.toPx()
        )

        // Center Listener Head Icon
        drawCircle(
            color = Color(0xFF1E2D4A),
            center = center,
            radius = 16.dp.toPx()
        )
        // Nose pointer (facing forward / up)
        val nosePath = Path().apply {
            moveTo(center.x - 5.dp.toPx(), center.y - 12.dp.toPx())
            lineTo(center.x, center.y - 22.dp.toPx())
            lineTo(center.x + 5.dp.toPx(), center.y - 12.dp.toPx())
            close()
        }
        drawPath(path = nosePath, color = NeonCyan)
        // Left Ear and Right Ear
        drawCircle(
            color = NeonCyan,
            center = Offset(center.x - 18.dp.toPx(), center.y),
            radius = 3.dp.toPx()
        )
        drawCircle(
            color = NeonCyan,
            center = Offset(center.x + 18.dp.toPx(), center.y),
            radius = 3.dp.toPx()
        )

        // Calculate Position of Audio Orb
        // 0 degrees = Front (up, -y), 90 degrees = Right (+x), etc.
        // angle in radians from -Y axis clockwise
        val azRad = (azimuthDeg - 90.0) * (PI / 180.0)
        val normDist = (distanceMeters.coerceIn(0.5f, 5.0f) / 5.0f)
        val orbRadius = normDist * maxRadius
        val orbX = center.x + (orbRadius * cos(azRad)).toFloat()
        val orbY = center.y + (orbRadius * sin(azRad)).toFloat()

        // Soundwave ripples radiating from audio orb
        drawCircle(
            color = NeonAmber.copy(alpha = 0.25f),
            center = Offset(orbX, orbY),
            radius = 22.dp.toPx(),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Glowing Audio Orb
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, NeonAmber, NeonPlasmaPink),
                center = Offset(orbX, orbY),
                radius = 14.dp.toPx()
            ),
            center = Offset(orbX, orbY),
            radius = 12.dp.toPx()
        )
    }
}
