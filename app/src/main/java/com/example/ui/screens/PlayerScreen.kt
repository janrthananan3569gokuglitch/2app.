package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SpatialAudio
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MusicPlayerViewModel
import com.example.ui.RepeatMode
import com.example.ui.components.HolographicVinylCore
import com.example.ui.components.OscilloscopeVisualizer
import com.example.ui.components.SpectrumVisualizer
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

enum class VisualizerMode {
    VINYL,
    SPECTRUM,
    OSCILLOSCOPE
}

@Composable
fun PlayerScreen(
    viewModel: MusicPlayerViewModel,
    onNavigateToEq: () -> Unit,
    onNavigateToSpatial: () -> Unit,
    onNavigateToFx: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val positionMs by viewModel.playbackPositionMs.collectAsStateWithLifecycle()
    val durationMs by viewModel.durationMs.collectAsStateWithLifecycle()
    val spectrumBands by viewModel.spectrumBands.collectAsStateWithLifecycle()
    val waveformPoints by viewModel.waveformPoints.collectAsStateWithLifecycle()
    val isShuffle by viewModel.isShuffleEnabled.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()

    val isEqEnabled by viewModel.isEqEnabled.collectAsStateWithLifecycle()
    val activePresetName by viewModel.activePresetName.collectAsStateWithLifecycle()
    val isSpatialEnabled by viewModel.isSpatialEnabled.collectAsStateWithLifecycle()
    val isTubeEnabled by viewModel.isTubeEnabled.collectAsStateWithLifecycle()
    val tubeDrive by viewModel.tubeDrive.collectAsStateWithLifecycle()
    val stereoWidth by viewModel.stereoWidth.collectAsStateWithLifecycle()
    val isDolbyAtmosActive by viewModel.isDolbyAtmosActive.collectAsStateWithLifecycle()

    var visualizerMode by remember { mutableStateOf(VisualizerMode.VINYL) }

    // Calculate RMS audio energy for dynamic pulsing
    val audioEnergy = remember(spectrumBands) {
        var sum = 0f
        for (i in 0 until minOf(16, spectrumBands.size)) {
            sum += spectrumBands[i]
        }
        (sum / 16f).coerceIn(0.1f, 1.0f)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VaultObsidianDark)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header: Audiophile Hardware Badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "XP MUSIC VAULT ULTRA",
                    color = NeonCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "NATIVE 64-BIT DSP AUDIO CORE",
                    color = TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = VaultSurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonAmber.copy(alpha = 0.6f))
            ) {
                Text(
                    text = "24-BIT / 96kHz",
                    color = NeonAmber,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action Strip: Visualizer Mode Selector Chips + Dolby Atmos 3D Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(VaultSurfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                VisualizerMode.values().forEach { mode ->
                    val isSelected = visualizerMode == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) NeonCyan else Color.Transparent)
                            .clickable { visualizerMode = mode }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = mode.name,
                            color = if (isSelected) VaultObsidianDark else TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Dolby Atmos 3D Surround Button
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isDolbyAtmosActive) NeonCyan.copy(alpha = 0.2f) else VaultSurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isDolbyAtmosActive) NeonCyan else VaultCardBorder
                ),
                modifier = Modifier
                    .clickable { viewModel.toggleDolbyAtmos() }
                    .testTag("dolby_atmos_toggle")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isDolbyAtmosActive) "DOLBY: 3D ON" else "DOLBY: OFF",
                        color = if (isDolbyAtmosActive) NeonCyan else TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Center Hero Display (Vinyl with Spectrum Bars / Spectrum / Oscilloscope)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = visualizerMode,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                label = "visualizer_swap"
            ) { mode ->
                when (mode) {
                    VisualizerMode.VINYL -> {
                        HolographicVinylCore(
                            isPlaying = isPlaying,
                            audioEnergy = audioEnergy,
                            spectrumBands = spectrumBands,
                            paletteSeed = currentTrack?.title.hashCode()
                        )
                    }
                    VisualizerMode.SPECTRUM -> {
                        SpectrumVisualizer(
                            bands = spectrumBands,
                            paletteSeed = currentTrack?.title.hashCode(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(230.dp)
                        )
                    }
                    VisualizerMode.OSCILLOSCOPE -> {
                        OscilloscopeVisualizer(
                            waveform = waveformPoints,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(230.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Track Details & Vault Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentTrack?.title ?: "Select a Track from Library",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${currentTrack?.artist ?: "XP Native Audio"} • ${currentTrack?.album ?: "Ultra Sound Engine"}",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (currentTrack?.isVaulted == true) {
                    Surface(
                        shape = CircleShape,
                        color = NeonPlasmaPink.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonPlasmaPink)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Vaulted Audio",
                            tint = NeonPlasmaPink,
                            modifier = Modifier
                                .padding(6.dp)
                                .size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                currentTrack?.let { track ->
                    IconButton(
                        onClick = { viewModel.toggleFavorite(track) },
                        modifier = Modifier.testTag("toggle_favorite_button")
                    ) {
                        Icon(
                            imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (track.isFavorite) NeonPlasmaPink else TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Waveform Progress Scrubber with Interactive Touch Scrubbing
        var isScrubbing by remember { mutableStateOf(false) }
        var scrubPosition by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
        val displayedProgress = if (isScrubbing) {
            scrubPosition
        } else {
            if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
        }

        Slider(
            value = displayedProgress,
            onValueChange = {
                isScrubbing = true
                scrubPosition = it
            },
            onValueChangeFinished = {
                val targetMs = (scrubPosition * durationMs).toLong()
                viewModel.seekTo(targetMs)
                isScrubbing = false
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("playback_progress_slider"),
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
                text = formatDuration(if (isScrubbing) (scrubPosition * durationMs).toLong() else positionMs),
                color = TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = formatDuration(durationMs),
                color = TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Master Audiophile Transport Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle Button
            IconButton(
                onClick = { viewModel.toggleShuffle() },
                modifier = Modifier
                    .testTag("shuffle_button")
                    .size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (isShuffle) NeonCyan else TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Previous Button
            IconButton(
                onClick = { viewModel.playPrevious() },
                modifier = Modifier
                    .testTag("previous_button")
                    .size(46.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous Track",
                    tint = TextPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Seek Backward 10 Seconds Button
            IconButton(
                onClick = { viewModel.seekBackward10() },
                modifier = Modifier
                    .testTag("seek_backward_10_button")
                    .size(46.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Replay10,
                    contentDescription = "Rewind 10 Seconds",
                    tint = NeonAmber,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Big Play/Pause Button with Glowing Neon Rim
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(NeonCyan, Color(0xFF007A99))
                        )
                    )
                    .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                    .clickable { viewModel.togglePlayPause() }
                    .testTag("play_pause_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = VaultObsidianDark,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Seek Forward 10 Seconds Button
            IconButton(
                onClick = { viewModel.seekForward10() },
                modifier = Modifier
                    .testTag("seek_forward_10_button")
                    .size(46.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Forward10,
                    contentDescription = "Skip Forward 10 Seconds",
                    tint = NeonAmber,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Next Button
            IconButton(
                onClick = { viewModel.playNext() },
                modifier = Modifier
                    .testTag("next_button")
                    .size(46.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next Track",
                    tint = TextPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Repeat Button
            IconButton(
                onClick = { viewModel.toggleRepeat() },
                modifier = Modifier
                    .testTag("repeat_button")
                    .size(44.dp)
            ) {
                Icon(
                    imageVector = if (repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                    contentDescription = "Repeat",
                    tint = if (repeatMode != RepeatMode.OFF) NeonCyan else TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick DSP Hardware Status Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Equalizer Quick Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToEq() }
                    .testTag("quick_card_eq"),
                colors = CardDefaults.cardColors(containerColor = VaultSurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, VaultCardBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "EQ",
                            tint = if (isEqEnabled) NeonCyan else TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isEqEnabled) "ON" else "BYPASS",
                            color = if (isEqEnabled) NeonCyan else TextTertiary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "10-BAND EQ",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = activePresetName,
                        color = TextSecondary,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 3D Spatial Quick Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToSpatial() }
                    .testTag("quick_card_spatial"),
                colors = CardDefaults.cardColors(containerColor = VaultSurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, VaultCardBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SpatialAudio,
                            contentDescription = "Spatial",
                            tint = if (isSpatialEnabled) NeonAmber else TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isSpatialEnabled) "ACTIVE" else "OFF",
                            color = if (isSpatialEnabled) NeonAmber else TextTertiary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "HRTF 3D AUDIO",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Binaural Orbit",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }

            // Tube Warmth Quick Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToFx() }
                    .testTag("quick_card_tube"),
                colors = CardDefaults.cardColors(containerColor = VaultSurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, VaultCardBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SurroundSound,
                            contentDescription = "Tube FX",
                            tint = if (isTubeEnabled) NeonPlasmaPink else TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isTubeEnabled) "DRIVE" else "CLEAN",
                            color = if (isTubeEnabled) NeonPlasmaPink else TextTertiary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "TUBE WARMTH",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.1f", tubeDrive)}x Saturation",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
