package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Waves
import java.io.File
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.TrackEntity
import com.example.ui.MusicPlayerViewModel
import com.example.ui.theme.ErrorRed
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

@Composable
fun LibraryScreen(
    viewModel: MusicPlayerViewModel,
    onTrackSelected: (TrackEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("ALL SONGS", "FOLDERS", "VAULT", "AUDIOPHILE LAB", "FAVORITES")

    val libraryTracks by viewModel.libraryTracks.collectAsStateWithLifecycle()
    val vaultedTracks by viewModel.vaultedTracks.collectAsStateWithLifecycle()
    val favoriteTracks by viewModel.favoriteTracks.collectAsStateWithLifecycle()
    val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsStateWithLifecycle()
    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFormatFilter by remember { mutableStateOf("ALL") }
    val filterModes = listOf("ALL", "PHONKS", "FLAC", "MP3", "HI-RES")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VaultObsidianDark)
    ) {
        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = VaultSurfaceDark,
            contentColor = NeonCyan,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = NeonCyan,
                    height = 2.5.dp
                )
            }
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    modifier = Modifier.testTag("library_tab_$index"),
                    text = {
                        Text(
                            text = title,
                            fontSize = 10.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Black else FontWeight.Bold,
                            color = if (selectedTab == index) NeonCyan else TextSecondary
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> {
                // Tab 0: Device Audio Library
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    // Search Bar and Rescan Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search songs, artists...", color = TextTertiary, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = TextSecondary)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("library_search_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = VaultCardBorder,
                                focusedContainerColor = VaultSurfaceDark,
                                unfocusedContainerColor = VaultSurfaceDark
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { viewModel.rescanLibrary() },
                            modifier = Modifier
                                .testTag("rescan_library_button")
                                .clip(RoundedCornerShape(12.dp))
                                .background(VaultSurfaceDark)
                                .border(1.dp, VaultCardBorder, RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Rescan Media",
                                tint = NeonCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Format Filter Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        filterModes.forEach { mode ->
                            val isSel = selectedFormatFilter == mode
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) NeonCyan.copy(alpha = 0.2f) else VaultSurfaceDark,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) NeonCyan else VaultCardBorder),
                                modifier = Modifier.clickable { selectedFormatFilter = mode }
                            ) {
                                Text(
                                    text = mode,
                                    color = if (isSel) NeonCyan else TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSel) FontWeight.Black else FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val filteredTracks = libraryTracks.filter { track ->
                        if (track.isProcedural) return@filter false
                        val matchesQuery = track.title.contains(searchQuery, ignoreCase = true) ||
                            track.artist.contains(searchQuery, ignoreCase = true) ||
                            track.album.contains(searchQuery, ignoreCase = true)
                        if (!matchesQuery) return@filter false

                        when (selectedFormatFilter) {
                            "PHONKS" -> track.filePath.contains("phonks", ignoreCase = true) ||
                                        track.title.contains("phonk", ignoreCase = true) ||
                                        track.artist.contains("phonk", ignoreCase = true)
                            "FLAC" -> track.filePath.endsWith(".flac", ignoreCase = true) || track.bitrateKbps >= 1000
                            "MP3" -> track.filePath.endsWith(".mp3", ignoreCase = true) || track.filePath.contains("content://")
                            "HI-RES" -> track.sampleRateHz >= 48000 || track.bitrateKbps >= 320
                            else -> true
                        }
                    }

                    if (filteredTracks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = TextTertiary,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No audio files matching filter",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Tap 'FOLDERS' to scan /storage/3263-3638/phonks or AUDIOPHILE LAB for test tracks",
                                    color = NeonAmber,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredTracks, key = { it.id }) { track ->
                                TrackRowItem(
                                    track = track,
                                    isCurrent = currentTrack?.id == track.id,
                                    isPlaying = isPlaying,
                                    onClick = {
                                        viewModel.playTrack(track)
                                        onTrackSelected(track)
                                    },
                                    onFavoriteClick = { viewModel.toggleFavorite(track) },
                                    onVaultClick = { viewModel.vaultTrack(track) }
                                )
                            }
                        }
                    }
                }
            }

            1 -> {
                // Tab 1: Storage Folder Explorer
                FolderBrowserContent(
                    tracks = libraryTracks.filter { !it.isProcedural },
                    currentTrack = currentTrack,
                    isPlaying = isPlaying,
                    viewModel = viewModel,
                    onTrackSelected = onTrackSelected
                )
            }

            2 -> {
                // Tab 2: Encrypted Audio Vault
                if (!isVaultUnlocked) {
                    VaultPinKeypad(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    VaultUnlockedContent(
                        vaultedTracks = vaultedTracks,
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        onPlay = {
                            viewModel.playTrack(it)
                            onTrackSelected(it)
                        },
                        onUnvault = { viewModel.unvaultTrack(it) },
                        onLock = { viewModel.lockVault() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            3 -> {
                // Tab 3: Audiophile Lab
                val proceduralTracks = libraryTracks.filter { it.isProcedural }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = "HIGH-PRECISION PROCEDURAL AUDIO SYNTHESIZERS",
                            color = NeonAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Pure trigonometric & filtered DSP signals running in real-time. Test your speakers, headphones, and the 10-Band EQ.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    items(proceduralTracks, key = { it.id }) { track ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.playTrack(track)
                                    onTrackSelected(track)
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (currentTrack?.id == track.id) VaultSurfaceVariant else VaultSurfaceDark
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (currentTrack?.id == track.id) NeonCyan else VaultCardBorder
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(NeonAmber.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (currentTrack?.id == track.id && isPlaying) Icons.Default.GraphicEq else Icons.Default.Waves,
                                            contentDescription = null,
                                            tint = NeonAmber,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = track.title,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "${track.artist} • ${track.album}",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = NeonCyan.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan)
                                ) {
                                    Text(
                                        text = "64-BIT DSP",
                                        color = NeonCyan,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            4 -> {
                // Tab 4: Favorites
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    if (favoriteTracks.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = TextTertiary,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No favorite tracks yet",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Tap the heart icon on any song to save it here",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(favoriteTracks, key = { it.id }) { track ->
                                TrackRowItem(
                                    track = track,
                                    isCurrent = currentTrack?.id == track.id,
                                    isPlaying = isPlaying,
                                    onClick = {
                                        viewModel.playTrack(track)
                                        onTrackSelected(track)
                                    },
                                    onFavoriteClick = { viewModel.toggleFavorite(track) },
                                    onVaultClick = { viewModel.vaultTrack(track) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrackRowItem(
    track: TrackEntity,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onVaultClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("track_row_${track.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) VaultSurfaceVariant else VaultSurfaceDark
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isCurrent) NeonCyan else VaultCardBorder
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isCurrent) NeonCyan else VaultSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCurrent && isPlaying) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (isCurrent) VaultObsidianDark else TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = if (isCurrent) NeonCyan else TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${track.artist} • ${track.album}",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (track.isFavorite) NeonPlasmaPink else TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onVaultClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Vault Song",
                    tint = TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Cyberpunk Numeric Security Keypad to unlock encrypted vault.
 */
@Composable
fun VaultPinKeypad(
    viewModel: MusicPlayerViewModel,
    modifier: Modifier = Modifier
) {
    val pinInput by viewModel.vaultPinInput.collectAsStateWithLifecycle()
    val vaultError by viewModel.vaultError.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(NeonPlasmaPink.copy(alpha = 0.15f))
                .border(2.dp, NeonPlasmaPink, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Vault Security",
                tint = NeonPlasmaPink,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "AUDIO VAULT ENCRYPTION",
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "Enter 4-Digit Security Master PIN",
            color = TextSecondary,
            fontSize = 12.sp
        )
        Text(
            text = "(Factory Default PIN: 1234)",
            color = NeonAmber,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 4 PIN Dots
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            for (i in 0 until 4) {
                val filled = i < pinInput.length
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(if (filled) NeonPlasmaPink else Color.Transparent)
                        .border(1.5.dp, if (filled) NeonPlasmaPink else VaultCardBorder, CircleShape)
                )
            }
        }

        if (vaultError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = vaultError ?: "",
                color = ErrorRed,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Keypad Grid
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "DEL")
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (row in keys) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    for (key in row) {
                        if (key.isEmpty()) {
                            Spacer(modifier = Modifier.size(68.dp))
                        } else if (key == "DEL") {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(VaultSurfaceDark)
                                    .border(1.dp, VaultCardBorder, CircleShape)
                                    .clickable { viewModel.onPinBackspace() }
                                    .testTag("pin_keypad_del"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Backspace,
                                    contentDescription = "Delete",
                                    tint = TextPrimary
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(VaultSurfaceDark)
                                    .border(1.dp, VaultCardBorder, CircleShape)
                                    .clickable { viewModel.onPinDigit(key) }
                                    .testTag("pin_keypad_$key"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = key,
                                    color = TextPrimary,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VaultUnlockedContent(
    vaultedTracks: List<TrackEntity>,
    currentTrack: TrackEntity?,
    isPlaying: Boolean,
    onPlay: (TrackEntity) -> Unit,
    onUnvault: (TrackEntity) -> Unit,
    onLock: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LockOpen,
                    contentDescription = null,
                    tint = NeonEmerald,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "VAULT UNLOCKED (AES-256 SESSION)",
                    color = NeonEmerald,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }

            Button(
                onClick = onLock,
                colors = ButtonDefaults.buttonColors(containerColor = VaultSurfaceVariant),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("LOCK VAULT", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (vaultedTracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "The Encrypted Vault is Empty",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Go to 'DEVICE AUDIO' and tap the lock icon on any track to isolate it here.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(vaultedTracks, key = { it.id }) { track ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlay(track) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (currentTrack?.id == track.id) VaultSurfaceVariant else VaultSurfaceDark
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (currentTrack?.id == track.id) NeonPlasmaPink else VaultCardBorder
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(NeonPlasmaPink.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (currentTrack?.id == track.id && isPlaying) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = NeonPlasmaPink,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "ENCRYPTED AES-GCM • ${track.artist}",
                                    color = NeonPlasmaPink,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Button(
                                onClick = { onUnvault(track) },
                                colors = ButtonDefaults.buttonColors(containerColor = VaultSurfaceVariant),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("RESTORE", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

data class StorageFolderInfo(
    val folderPath: String,
    val folderName: String,
    val tracks: List<TrackEntity>
)

@Composable
fun FolderBrowserContent(
    tracks: List<TrackEntity>,
    currentTrack: TrackEntity?,
    isPlaying: Boolean,
    viewModel: MusicPlayerViewModel,
    onTrackSelected: (TrackEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var openedFolder by remember { mutableStateOf<StorageFolderInfo?>(null) }
    var showScanDialog by remember { mutableStateOf(false) }
    var customPathInput by remember { mutableStateOf("/storage/3263-3638/phonks") }
    var scanNotice by remember { mutableStateOf<String?>(null) }

    // Intercept back button when inside a folder to return to root folders
    BackHandler(enabled = openedFolder != null) {
        openedFolder = null
    }

    // Derive folders from loaded tracks and include strict target folder
    val folders = remember(tracks) {
        val map = mutableMapOf<String, MutableList<TrackEntity>>()
        val targetLockedPath = "/storage/3263-3638/phonks"

        // Ensure target folder is always represented
        map[targetLockedPath] = mutableListOf()

        tracks.forEach { track ->
            val p = track.filePath
            val folderKey = when {
                p.startsWith("/") -> File(p).parent ?: targetLockedPath
                p.contains("phonks", ignoreCase = true) -> targetLockedPath
                else -> "Device Music Storage"
            }
            map.getOrPut(folderKey) { mutableListOf() }.add(track)
        }

        map.map { (path, folderTracks) ->
            val name = if (path.startsWith("/")) {
                val f = File(path)
                f.name.ifEmpty { path }
            } else {
                path
            }
            StorageFolderInfo(folderPath = path, folderName = name, tracks = folderTracks)
        }.sortedWith(
            compareByDescending<StorageFolderInfo> { it.folderPath == targetLockedPath }
                .thenByDescending { it.tracks.size }
                .thenBy { it.folderName }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        if (openedFolder == null) {
            // Folders Root List
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "FOLDER EXPLORER",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${folders.size} storage locations • Direct playback",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { showScanDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = VaultSurfaceDark),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SCAN PATH", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (scanNotice != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NeonCyan.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = scanNotice ?: "", color = TextPrimary, fontSize = 11.sp)
                        IconButton(onClick = { scanNotice = null }, modifier = Modifier.size(24.dp)) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(folders, key = { it.folderPath }) { folder ->
                    val isLockedPhonks = folder.folderPath == "/storage/3263-3638/phonks"
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { openedFolder = folder },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isLockedPhonks) VaultSurfaceVariant else VaultSurfaceDark
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isLockedPhonks) NeonAmber else VaultCardBorder
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isLockedPhonks) NeonAmber.copy(alpha = 0.15f) else NeonCyan.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isLockedPhonks) Icons.Default.FolderOpen else Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = if (isLockedPhonks) NeonAmber else NeonCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = folder.folderName,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    if (isLockedPhonks) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = NeonAmber.copy(alpha = 0.2f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonAmber)
                                        ) {
                                            Text(
                                                text = "STRICT LOCK",
                                                color = NeonAmber,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = folder.folderPath,
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = "${folder.tracks.size} tracks available",
                                    color = if (folder.tracks.isNotEmpty()) NeonCyan else TextTertiary,
                                    fontSize = 11.sp
                                )
                            }

                            if (folder.tracks.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        viewModel.playAllInList(folder.tracks)
                                        onTrackSelected(folder.tracks.first())
                                    },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(NeonCyan.copy(alpha = 0.15f))
                                        .size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play Folder",
                                        tint = NeonCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Opened Folder Drill-Down
            val currentFolder = openedFolder!!
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { openedFolder = null },
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(VaultSurfaceDark)
                        .border(1.dp, VaultCardBorder, RoundedCornerShape(8.dp))
                        .size(36.dp)
                ) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = NeonCyan, modifier = Modifier.size(18.dp))
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentFolder.folderName,
                        color = TextPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                    Text(
                        text = currentFolder.folderPath,
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (currentFolder.tracks.isNotEmpty()) {
                    Button(
                        onClick = {
                            viewModel.playAllInList(currentFolder.tracks)
                            onTrackSelected(currentFolder.tracks.first())
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = VaultObsidianDark, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PLAY ALL", color = VaultObsidianDark, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (currentFolder.tracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(52.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = "No audio files detected in this folder yet", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = {
                                viewModel.scanCustomFolder(currentFolder.folderPath) { count ->
                                    scanNotice = "Scan complete: found $count new tracks"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VaultSurfaceDark),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("SCAN THIS FOLDER NOW", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(currentFolder.tracks, key = { it.id }) { track ->
                        TrackRowItem(
                            track = track,
                            isCurrent = currentTrack?.id == track.id,
                            isPlaying = isPlaying,
                            onClick = {
                                viewModel.playTrack(track)
                                onTrackSelected(track)
                            },
                            onFavoriteClick = { viewModel.toggleFavorite(track) },
                            onVaultClick = { viewModel.vaultTrack(track) }
                        )
                    }
                }
            }
        }
    }

    // Custom Path Scan Dialog
    if (showScanDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showScanDialog = false },
            containerColor = VaultSurfaceDark,
            title = {
                Text("SCAN STORAGE FOLDER", color = NeonCyan, fontWeight = FontWeight.Black, fontSize = 14.sp)
            },
            text = {
                Column {
                    Text("Enter the directory path on device to scan for audio files:", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = customPathInput,
                        onValueChange = { customPathInput = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = VaultCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { customPathInput = "/storage/3263-3638/phonks" },
                            colors = ButtonDefaults.buttonColors(containerColor = VaultSurfaceVariant),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("/phonks", color = NeonAmber, fontSize = 10.sp)
                        }
                        Button(
                            onClick = { customPathInput = "/storage/emulated/0/Music" },
                            colors = ButtonDefaults.buttonColors(containerColor = VaultSurfaceVariant),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("/Music", color = NeonCyan, fontSize = 10.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val path = customPathInput.trim()
                        showScanDialog = false
                        viewModel.scanCustomFolder(path) { count ->
                            scanNotice = "Scanned $path: found $count new tracks"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("START SCAN", color = VaultObsidianDark, fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showScanDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = VaultSurfaceVariant)
                ) {
                    Text("CANCEL", color = TextSecondary)
                }
            }
        )
    }
}
