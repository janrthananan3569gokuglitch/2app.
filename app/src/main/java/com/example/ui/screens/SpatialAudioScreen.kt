package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SpatialAudio
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MusicPlayerViewModel
import com.example.ui.components.SpatialRadarSoundstage
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
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

@Composable
fun SpatialAudioScreen(
    viewModel: MusicPlayerViewModel,
    modifier: Modifier = Modifier
) {
    val isSpatialEnabled by viewModel.isSpatialEnabled.collectAsStateWithLifecycle()
    val azimuth by viewModel.spatialAzimuth.collectAsStateWithLifecycle()
    val elevation by viewModel.spatialElevation.collectAsStateWithLifecycle()
    val distance by viewModel.spatialDistance.collectAsStateWithLifecycle()

    val isReverbEnabled by viewModel.isReverbEnabled.collectAsStateWithLifecycle()
    val reverbWet by viewModel.reverbWet.collectAsStateWithLifecycle()
    val reverbRoomSize by viewModel.reverbRoomSize.collectAsStateWithLifecycle()

    val stereoWidth by viewModel.stereoWidth.collectAsStateWithLifecycle()

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
                    text = "3D HRTF BINAURAL SPATIAL ENGINE",
                    color = NeonCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "WOODWORTH ITD • PINNA NOTCH • ILD SPHERICAL",
                    color = TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(
                onClick = { viewModel.toggleSpatialEnabled() },
                modifier = Modifier.testTag("toggle_spatial_power")
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = "Toggle Spatial Audio",
                    tint = if (isSpatialEnabled) NeonAmber else TextTertiary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Interactive 2D/3D Radar Soundstage
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .pointerInput(Unit) {
                    fun updateFromOffset(x: Float, y: Float) {
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val dx = x - centerX
                        val dy = y - centerY

                        // Calculate angle: 0 degrees is up (-dy)
                        val angleRad = atan2(dx, -dy)
                        val angleDeg = (angleRad * 180.0 / PI).toFloat()

                        val distPx = sqrt(dx * dx + dy * dy)
                        val maxRadius = minOf(size.width, size.height) * 0.44f
                        val normDist = (distPx / maxRadius).coerceIn(0.1f, 1.0f)
                        val distMeters = (normDist * 5.0f).coerceIn(0.5f, 5.0f)

                        viewModel.setSpatialPosition(angleDeg, elevation, distMeters)
                    }

                    detectTapGestures { offset ->
                        updateFromOffset(offset.x, offset.y)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val dx = change.position.x - centerX
                        val dy = change.position.y - centerY

                        val angleRad = atan2(dx, -dy)
                        val angleDeg = (angleRad * 180.0 / PI).toFloat()

                        val distPx = sqrt(dx * dx + dy * dy)
                        val maxRadius = minOf(size.width, size.height) * 0.44f
                        val normDist = (distPx / maxRadius).coerceIn(0.1f, 1.0f)
                        val distMeters = (normDist * 5.0f).coerceIn(0.5f, 5.0f)

                        viewModel.setSpatialPosition(angleDeg, elevation, distMeters)
                    }
                }
        ) {
            SpatialRadarSoundstage(
                azimuthDeg = azimuth,
                distanceMeters = distance,
                onPositionChanged = { az, dist -> viewModel.setSpatialPosition(az, elevation, dist) },
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Precision 3D Position Sliders
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
                // Azimuth Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "AZIMUTH (ORBIT ANGLE)",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${String.format(Locale.US, "%+.0f", azimuth)}°",
                        color = NeonAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Slider(
                    value = azimuth,
                    onValueChange = { viewModel.setSpatialPosition(it, elevation, distance) },
                    valueRange = -180f..180f,
                    modifier = Modifier.testTag("azimuth_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = NeonAmber,
                        activeTrackColor = NeonAmber,
                        inactiveTrackColor = VaultSurfaceVariant
                    )
                )

                // Elevation Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "ELEVATION (PINNA NOTCH)",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${String.format(Locale.US, "%+.0f", elevation)}°",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Slider(
                    value = elevation,
                    onValueChange = { viewModel.setSpatialPosition(azimuth, it, distance) },
                    valueRange = -45f..90f,
                    modifier = Modifier.testTag("elevation_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan,
                        inactiveTrackColor = VaultSurfaceVariant
                    )
                )

                // Distance Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "SOUNDSTAGE DISTANCE",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.1f", distance)} m",
                        color = NeonPlasmaPink,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Slider(
                    value = distance,
                    onValueChange = { viewModel.setSpatialPosition(azimuth, elevation, it) },
                    valueRange = 0.5f..5.0f,
                    modifier = Modifier.testTag("distance_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = NeonPlasmaPink,
                        activeTrackColor = NeonPlasmaPink,
                        inactiveTrackColor = VaultSurfaceVariant
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Schroeder Reverb Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = VaultSurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, VaultCardBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SCHROEDER REVERBERATOR",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "4 Comb Filters + 2 All-Pass Matrix",
                            color = TextTertiary,
                            fontSize = 10.sp
                        )
                    }

                    Switch(
                        checked = isReverbEnabled,
                        onCheckedChange = { viewModel.toggleReverbEnabled() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonCyan,
                            checkedTrackColor = VaultSurfaceVariant
                        )
                    )
                }

                if (isReverbEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "WET / DRY MIX",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${(reverbWet * 100).toInt()}%",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Slider(
                        value = reverbWet,
                        onValueChange = { viewModel.setReverbWet(it) },
                        valueRange = 0.0f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = VaultSurfaceVariant
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "ROOM DECAY SIZE",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${(reverbRoomSize * 100).toInt()}%",
                            color = NeonAmber,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Slider(
                        value = reverbRoomSize,
                        onValueChange = { viewModel.setReverbRoomSize(it) },
                        valueRange = 0.1f..0.92f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonAmber,
                            activeTrackColor = NeonAmber,
                            inactiveTrackColor = VaultSurfaceVariant
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Stereo Holographic Expander Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = VaultSurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, VaultCardBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "MID-SIDE STEREO HOLOGRAPHIC SPREAD",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${(stereoWidth * 100).toInt()}%",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Slider(
                    value = stereoWidth,
                    onValueChange = { viewModel.setStereoWidth(it) },
                    valueRange = 0.0f..2.5f,
                    modifier = Modifier.testTag("stereo_width_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan,
                        inactiveTrackColor = VaultSurfaceVariant
                    )
                )
                Text(
                    text = "0% Mono • 100% Standard Stereo • 250% Hyper Holographic Field",
                    color = TextTertiary,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
