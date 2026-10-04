package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.ui.theme.NeonEmerald
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
fun EqualizerScreen(
    viewModel: MusicPlayerViewModel,
    modifier: Modifier = Modifier
) {
    val isEqEnabled by viewModel.isEqEnabled.collectAsStateWithLifecycle()
    val bandGains by viewModel.eqBandGains.collectAsStateWithLifecycle()
    val preAmp by viewModel.preAmpGain.collectAsStateWithLifecycle()
    val presets by viewModel.eqPresets.collectAsStateWithLifecycle()
    val activePresetName by viewModel.activePresetName.collectAsStateWithLifecycle()

    var showSaveDialog by remember { mutableStateOf(false) }
    var newPresetName by remember { mutableStateOf("") }

    val bandLabels = remember {
        listOf("31Hz", "63Hz", "125Hz", "250Hz", "500Hz", "1kHz", "2kHz", "4kHz", "8kHz", "16kHz")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VaultObsidianDark)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "PARAMETRIC EQUALIZER",
                    color = NeonCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "10-BAND ISO BIQUAD IIR ENGINE",
                    color = TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { viewModel.resetEqFlat() },
                    modifier = Modifier.testTag("reset_flat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Flat",
                        tint = TextSecondary
                    )
                }

                IconButton(
                    onClick = { viewModel.toggleEqEnabled() },
                    modifier = Modifier.testTag("toggle_eq_power_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Toggle EQ",
                        tint = if (isEqEnabled) NeonCyan else TextTertiary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Live Frequency Response Curve Graph Canvas
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            shape = RoundedCornerShape(12.dp),
            color = VaultSurfaceDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, VaultCardBorder)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .testTag("eq_curve_canvas")
            ) {
                val width = size.width
                val height = size.height
                val midY = height / 2f

                // Reference Grid Lines (+12dB, 0dB, -12dB)
                drawLine(
                    color = VaultCardBorder,
                    start = Offset(0f, midY),
                    end = Offset(width, midY),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = VaultCardBorder.copy(alpha = 0.5f),
                    start = Offset(0f, height * 0.15f),
                    end = Offset(width, height * 0.15f),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = VaultCardBorder.copy(alpha = 0.5f),
                    start = Offset(0f, height * 0.85f),
                    end = Offset(width, height * 0.85f),
                    strokeWidth = 1.dp.toPx()
                )

                if (bandGains.size == 10 && isEqEnabled) {
                    val path = Path()
                    val fillPath = Path()
                    val stepX = width / 9f

                    fillPath.moveTo(0f, midY)

                    // Spline calculation for 10 bands
                    val points = ArrayList<Offset>()
                    for (i in 0 until 10) {
                        val g = bandGains[i].coerceIn(-12f, 12f)
                        val y = midY - (g / 12f) * (midY * 0.85f)
                        val x = i * stepX
                        points.add(Offset(x, y))
                    }

                    path.moveTo(points[0].x, points[0].y)
                    fillPath.lineTo(points[0].x, points[0].y)

                    for (i in 0 until points.size - 1) {
                        val p0 = points[maxOf(0, i - 1)]
                        val p1 = points[i]
                        val p2 = points[i + 1]
                        val p3 = points[minOf(points.size - 1, i + 2)]

                        // Catmull-Rom to Cubic Bezier
                        val c1x = p1.x + (p2.x - p0.x) / 6f
                        val c1y = p1.y + (p2.y - p0.y) / 6f
                        val c2x = p2.x - (p3.x - p1.x) / 6f
                        val c2y = p2.y - (p3.y - p1.y) / 6f

                        path.cubicTo(c1x, c1y, c2x, c2y, p2.x, p2.y)
                        fillPath.cubicTo(c1x, c1y, c2x, c2y, p2.x, p2.y)
                    }

                    fillPath.lineTo(width, midY)
                    fillPath.close()

                    // Draw filled glow area under curve
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(NeonCyan.copy(alpha = 0.25f), Color.Transparent),
                            startY = 0f,
                            endY = height
                        )
                    )

                    // Draw curve line
                    drawPath(
                        path = path,
                        color = NeonCyan,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw control points on each band
                    for (p in points) {
                        drawCircle(
                            color = NeonAmber,
                            center = p,
                            radius = 3.5.dp.toPx()
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Presets Chips Carousel
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "FACTORY & CUSTOM PRESETS",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showSaveDialog = true }
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Save Preset",
                    tint = NeonAmber,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "SAVE",
                    color = NeonAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { preset ->
                val isSelected = activePresetName == preset.name
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (isSelected) NeonCyan else VaultSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) NeonCyan else VaultCardBorder
                    ),
                    modifier = Modifier.clickable { viewModel.applyPreset(preset) }
                ) {
                    Text(
                        text = preset.name,
                        color = if (isSelected) VaultObsidianDark else TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Pre-Amp Slider Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PRE-AMP GAIN",
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = String.format(Locale.US, "%+.1f dB", preAmp),
                color = NeonAmber,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        Slider(
            value = preAmp,
            onValueChange = { viewModel.setPreAmpGain(it) },
            valueRange = -12f..12f,
            modifier = Modifier.testTag("preamp_slider"),
            colors = SliderDefaults.colors(
                thumbColor = NeonAmber,
                activeTrackColor = NeonAmber,
                inactiveTrackColor = VaultSurfaceVariant
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 10-Band Sliders Matrix
        Text(
            text = "10 FREQUENCY BANDS (-12dB to +12dB)",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(VaultSurfaceDark)
                .border(1.dp, VaultCardBorder, RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (i in 0 until 10) {
                val gain = bandGains.getOrElse(i) { 0f }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = bandLabels[i],
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.width(60.dp)
                    )

                    Slider(
                        value = gain,
                        onValueChange = { viewModel.setEqBandGain(i, it) },
                        valueRange = -12f..12f,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("eq_band_slider_$i"),
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = VaultSurfaceVariant
                        )
                    )

                    Text(
                        text = String.format(Locale.US, "%+.1f dB", gain),
                        color = if (gain > 0f) NeonAmber else if (gain < 0f) NeonPlasmaPink else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.width(65.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Save Preset Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = {
                Text(text = "Save Custom Preset", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                OutlinedTextField(
                    value = newPresetName,
                    onValueChange = { newPresetName = it },
                    label = { Text("Preset Name") },
                    singleLine = true,
                    modifier = Modifier.testTag("preset_name_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPresetName.isNotBlank()) {
                            viewModel.saveCustomPreset(newPresetName.trim())
                            newPresetName = ""
                            showSaveDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("Save", color = VaultObsidianDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = VaultSurfaceDark
        )
    }
}
