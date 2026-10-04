package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MusicPlayerViewModel
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPlasmaPink
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.VaultCardBorder
import com.example.ui.theme.VaultObsidianDark
import com.example.ui.theme.VaultSurfaceDark
import com.example.ui.theme.VaultSurfaceVariant
import java.util.Locale

@Composable
fun SoundFxScreen(
    viewModel: MusicPlayerViewModel,
    modifier: Modifier = Modifier
) {
    val isTubeEnabled by viewModel.isTubeEnabled.collectAsStateWithLifecycle()
    val tubeDrive by viewModel.tubeDrive.collectAsStateWithLifecycle()
    val tubeWarmthBias by viewModel.tubeWarmthBias.collectAsStateWithLifecycle()
    val bassExciter by viewModel.bassExciter.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()

    val glowAlpha by animateFloatAsState(
        targetValue = if (isTubeEnabled) (tubeDrive / 4.0f).coerceIn(0.3f, 1.0f) else 0.1f,
        animationSpec = tween(400),
        label = "filament_glow"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VaultObsidianDark)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Header
        Column {
            Text(
                text = "ANALOGUE TUBE PRE-AMP & HARMONICS",
                color = NeonPlasmaPink,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "TRIODE POLYNOMIAL SATURATION • EVEN HARMONIC INJECTION",
                color = TextTertiary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Vacuum Tube Visualizer & Power Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = VaultSurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, VaultCardBorder),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CLASS-A TRIODE TUBE STAGE",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (isTubeEnabled) "FILAMENT HEATED & ACTIVE" else "STANDBY BYPASS",
                            color = if (isTubeEnabled) NeonAmber else TextTertiary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Switch(
                        checked = isTubeEnabled,
                        onCheckedChange = { viewModel.toggleTubeEnabled() },
                        modifier = Modifier.testTag("toggle_tube_power"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonAmber,
                            checkedTrackColor = VaultSurfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Glowing Vacuum Tube Graphic
                Box(
                    modifier = Modifier
                        .size(160.dp, 130.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(VaultObsidianDark)
                        .border(1.dp, VaultCardBorder, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)

                        // Outer Glass Envelope
                        val glassWidth = 70.dp.toPx()
                        val glassHeight = 100.dp.toPx()
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.12f),
                            topLeft = Offset(center.x - glassWidth / 2f, center.y - glassHeight / 2f),
                            size = androidx.compose.ui.geometry.Size(glassWidth, glassHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx()),
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // Tube Base / Pins
                        drawLine(
                            color = BrushedMetal,
                            start = Offset(center.x - 20.dp.toPx(), center.y + glassHeight / 2f),
                            end = Offset(center.x + 20.dp.toPx(), center.y + glassHeight / 2f),
                            strokeWidth = 6.dp.toPx()
                        )

                        // Glowing Filament Heat Aura
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    NeonAmber.copy(alpha = 0.8f * glowAlpha),
                                    NeonPlasmaPink.copy(alpha = 0.4f * glowAlpha),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = 45.dp.toPx()
                            ),
                            center = center,
                            radius = 45.dp.toPx()
                        )

                        // Cathode & Plate Grids
                        val gridPath = Path().apply {
                            moveTo(center.x - 12.dp.toPx(), center.y - 20.dp.toPx())
                            lineTo(center.x + 12.dp.toPx(), center.y - 20.dp.toPx())
                            moveTo(center.x - 12.dp.toPx(), center.y)
                            lineTo(center.x + 12.dp.toPx(), center.y)
                            moveTo(center.x - 12.dp.toPx(), center.y + 20.dp.toPx())
                            lineTo(center.x + 12.dp.toPx(), center.y + 20.dp.toPx())
                        }
                        drawPath(
                            path = gridPath,
                            color = Color.White.copy(alpha = 0.35f),
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // Hot Filament Coil
                        val filamentPath = Path().apply {
                            moveTo(center.x, center.y - 25.dp.toPx())
                            lineTo(center.x, center.y + 25.dp.toPx())
                        }
                        drawPath(
                            path = filamentPath,
                            color = if (isTubeEnabled) Color(0xFFFFF0D0) else Color(0xFF4A3E30),
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tube Parameters
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = VaultSurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, VaultCardBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Saturation Drive
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "SATURATION DRIVE",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.2f", tubeDrive)}x",
                        color = NeonAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Slider(
                    value = tubeDrive,
                    onValueChange = { viewModel.setTubeDrive(it) },
                    valueRange = 1.0f..4.0f,
                    modifier = Modifier.testTag("tube_drive_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = NeonAmber,
                        activeTrackColor = NeonAmber,
                        inactiveTrackColor = VaultSurfaceVariant
                    )
                )

                // Warmth Bias (Even Harmonics)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "EVEN-HARMONIC WARMTH BIAS",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${(tubeWarmthBias * 100).toInt()}%",
                        color = NeonPlasmaPink,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Slider(
                    value = tubeWarmthBias,
                    onValueChange = { viewModel.setTubeWarmthBias(it) },
                    valueRange = 0.0f..0.4f,
                    modifier = Modifier.testTag("tube_warmth_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = NeonPlasmaPink,
                        activeTrackColor = NeonPlasmaPink,
                        inactiveTrackColor = VaultSurfaceVariant
                    )
                )

                // Sub-Bass Harmonic Exciter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "SUB-BASS HARMONIC EXCITER",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${(bassExciter * 100).toInt()}%",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Slider(
                    value = bassExciter,
                    onValueChange = { viewModel.setBassExciter(it) },
                    valueRange = 0.0f..1.0f,
                    modifier = Modifier.testTag("bass_exciter_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan,
                        inactiveTrackColor = VaultSurfaceVariant
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Dynamic Peak Limiter Specs Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = VaultSurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, VaultCardBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "MASTER DYNAMIC PEAK LIMITER",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Active Lookahead Peak Clamping (-0.2 dBFS threshold, 1ms attack, 50ms exponential recovery). Prevents inter-sample clipping and protects hardware drivers.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

val BrushedMetal = Color(0xFF2B3A52)
