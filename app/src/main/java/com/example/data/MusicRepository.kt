package com.example.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.example.dsp.ProceduralToneGenerator
import com.example.vault.VaultCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class MusicRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val vaultCrypto: VaultCrypto
) {
    private val trackDao = database.trackDao()
    private val eqPresetDao = database.eqPresetDao()
    private val playlistDao = database.playlistDao()

    val libraryTracks: Flow<List<TrackEntity>> = trackDao.getAllLibraryTracks()
    val vaultedTracks: Flow<List<TrackEntity>> = trackDao.getVaultedTracks()
    val favoriteTracks: Flow<List<TrackEntity>> = trackDao.getFavoriteTracks()
    val eqPresets: Flow<List<EqPresetEntity>> = eqPresetDao.getAllPresets()
    val playlists: Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()

    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        // 1. Seed factory Audiophile Procedural Tracks
        val proceduralTracks = listOf(
            TrackEntity(
                title = "Cyberpunk Benchmark",
                artist = "XP Native DSP Core",
                album = "God-Tier Audio Lab",
                durationMs = 180_000L,
                filePath = "procedural://CYBERPUNK_GROOVE",
                isProcedural = true,
                proceduralType = ProceduralToneGenerator.SignalType.CYBERPUNK_GROOVE.name,
                bitrateKbps = 1411,
                sampleRateHz = 44100
            ),
            TrackEntity(
                title = "528 Hz Cellular Transformation",
                artist = "Ancient Solfeggio Scale",
                album = "Harmonic Miracles",
                durationMs = 300_000L,
                filePath = "procedural://SOLFEGGIO_528HZ",
                isProcedural = true,
                proceduralType = ProceduralToneGenerator.SignalType.SOLFEGGIO_528HZ.name,
                bitrateKbps = 1411,
                sampleRateHz = 44100
            ),
            TrackEntity(
                title = "432 Hz Pure Natural Tuning",
                artist = "Verdi Concert Pitch",
                album = "Acoustic Resonance",
                durationMs = 300_000L,
                filePath = "procedural://VERDI_432HZ",
                isProcedural = true,
                proceduralType = ProceduralToneGenerator.SignalType.VERDI_432HZ.name,
                bitrateKbps = 1411,
                sampleRateHz = 44100
            ),
            TrackEntity(
                title = "Binaural Theta Flow (6 Hz)",
                artist = "XP Brainwave Synthesizer",
                album = "Deep Meditation Lab",
                durationMs = 360_000L,
                filePath = "procedural://BINAURAL_THETA",
                isProcedural = true,
                proceduralType = ProceduralToneGenerator.SignalType.BINAURAL_THETA.name,
                bitrateKbps = 1411,
                sampleRateHz = 44100
            ),
            TrackEntity(
                title = "Binaural Alpha Focus (10 Hz)",
                artist = "XP Brainwave Synthesizer",
                album = "Cognitive Clarity",
                durationMs = 360_000L,
                filePath = "procedural://BINAURAL_ALPHA",
                isProcedural = true,
                proceduralType = ProceduralToneGenerator.SignalType.BINAURAL_ALPHA.name,
                bitrateKbps = 1411,
                sampleRateHz = 44100
            ),
            TrackEntity(
                title = "Pink Noise Spectral Calibration",
                artist = "Kellet DSP Generator",
                album = "Acoustic Reference",
                durationMs = 120_000L,
                filePath = "procedural://PINK_NOISE",
                isProcedural = true,
                proceduralType = ProceduralToneGenerator.SignalType.PINK_NOISE.name,
                bitrateKbps = 1411,
                sampleRateHz = 44100
            ),
            TrackEntity(
                title = "20Hz-20kHz Frequency Sweep",
                artist = "XP Precision Vector Lab",
                album = "10-Band EQ Calibration",
                durationMs = 120_000L,
                filePath = "procedural://FREQUENCY_SWEEP",
                isProcedural = true,
                proceduralType = ProceduralToneGenerator.SignalType.FREQUENCY_SWEEP.name,
                bitrateKbps = 1411,
                sampleRateHz = 44100
            )
        )

        for (t in proceduralTracks) {
            val existing = trackDao.getTrackByPath(t.filePath)
            if (existing == null) {
                trackDao.insertTrack(t)
            }
        }

        // 2. Seed Factory EQ Presets
        val presets = listOf(
            EqPresetEntity(
                name = "Flat (Direct Bypass)",
                isBuiltIn = true,
                preAmpGain = 0.0f,
                bandGains = "0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0",
                tubeWarmth = 1.0f,
                stereoWidth = 1.0f
            ),
            EqPresetEntity(
                name = "Audiophile Reference",
                isBuiltIn = true,
                preAmpGain = -1.0f,
                bandGains = "2.0,1.5,0.5,0.0,-0.5,0.5,1.0,2.0,3.0,3.5",
                tubeWarmth = 1.2f,
                stereoWidth = 1.2f
            ),
            EqPresetEntity(
                name = "Cyberpunk Bass Cannon",
                isBuiltIn = true,
                preAmpGain = -2.0f,
                bandGains = "8.0,7.0,5.0,2.5,0.0,0.5,1.5,3.0,4.5,5.0",
                tubeWarmth = 1.6f,
                stereoWidth = 1.35f
            ),
            EqPresetEntity(
                name = "Vocal Purity",
                isBuiltIn = true,
                preAmpGain = -1.0f,
                bandGains = "-2.0,-1.0,0.0,2.0,4.0,4.5,3.5,2.0,1.0,0.5",
                tubeWarmth = 1.0f,
                stereoWidth = 1.1f
            ),
            EqPresetEntity(
                name = "Vinyl Warmth",
                isBuiltIn = true,
                preAmpGain = -1.5f,
                bandGains = "4.0,3.5,2.5,1.5,0.5,0.0,-0.5,-1.5,-3.0,-4.5",
                tubeWarmth = 2.2f,
                stereoWidth = 1.15f
            ),
            EqPresetEntity(
                name = "Rock Stadium",
                isBuiltIn = true,
                preAmpGain = -1.5f,
                bandGains = "5.0,4.0,2.0,-1.0,-1.5,1.0,3.0,4.5,5.0,5.5",
                tubeWarmth = 1.4f,
                stereoWidth = 1.4f
            ),
            EqPresetEntity(
                name = "3D Holographic Cinema",
                isBuiltIn = true,
                preAmpGain = -2.0f,
                bandGains = "3.5,2.5,1.0,0.0,1.0,2.0,3.0,4.5,6.0,7.0",
                tubeWarmth = 1.3f,
                spatialAzimuth = 45.0f,
                spatialElevation = 15.0f,
                spatialDistance = 2.0f,
                reverbWet = 0.35f,
                stereoWidth = 1.8f
            )
        )

        for (p in presets) {
            eqPresetDao.insertPreset(p)
        }
    }

    /**
     * Imports a single physical audio file from disk, reading its metadata via MediaMetadataRetriever.
     */
    private suspend fun importPhysicalAudioFile(file: File, retriever: android.media.MediaMetadataRetriever) {
        if (!file.exists() || !file.isFile || file.length() < 1024) return
        val path = file.absolutePath
        val existing = trackDao.getTrackByPath(path)
        if (existing != null) return

        var title = file.nameWithoutExtension
        var artist = "Unknown Artist"
        var album = file.parentFile?.name ?: "Local Vault"
        var durationMs = 0L
        var bitrate = 320
        var sampleRate = 44100

        try {
            retriever.setDataSource(path)
            val metaTitle = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE)
            val metaArtist = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val metaAlbum = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ALBUM)
            val metaDur = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            val metaBitrate = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_BITRATE)
            val metaSampleRate = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)

            if (!metaTitle.isNullOrBlank()) title = metaTitle
            if (!metaArtist.isNullOrBlank()) artist = metaArtist
            if (!metaAlbum.isNullOrBlank()) album = metaAlbum
            durationMs = metaDur?.toLongOrNull() ?: 0L
            bitrate = (metaBitrate?.toIntOrNull() ?: 320000) / 1000
            sampleRate = metaSampleRate?.toIntOrNull() ?: 44100
        } catch (e: Exception) {
            Log.w("MusicRepository", "Notice: Metadata extraction skipped for $path: ${e.message}")
        }

        trackDao.insertTrack(
            TrackEntity(
                title = title,
                artist = artist,
                album = album,
                durationMs = durationMs,
                filePath = path,
                bitrateKbps = if (bitrate <= 0) 320 else bitrate,
                sampleRateHz = if (sampleRate <= 0) 44100 else sampleRate
            )
        )
    }

    /**
     * Scans any physical folder on disk recursively for audio files.
     */
    suspend fun scanFolder(folder: File): Int = withContext(Dispatchers.IO) {
        if (!folder.exists() || !folder.isDirectory) return@withContext 0
        var addedCount = 0
        val retriever = android.media.MediaMetadataRetriever()
        try {
            val audioExtensions = setOf("mp3", "flac", "wav", "m4a", "ogg", "aac", "opus", "aiff")
            folder.walkTopDown().maxDepth(6).forEach { file ->
                if (file.isFile && file.extension.lowercase() in audioExtensions) {
                    val path = file.absolutePath
                    if (trackDao.getTrackByPath(path) == null) {
                        importPhysicalAudioFile(file, retriever)
                        addedCount++
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MusicRepository", "scanFolder error: ${e.message}")
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
        addedCount
    }

    /**
     * Scans a custom folder path string, e.g. /storage/3263-3638/phonks.
     */
    suspend fun scanCustomPath(path: String): Int = withContext(Dispatchers.IO) {
        val folder = File(path)
        if (folder.exists() && folder.isDirectory) {
            scanFolder(folder)
        } else {
            0
        }
    }

    private suspend fun processCursorTracks(cursor: android.database.Cursor) {
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

        while (cursor.moveToNext()) {
            val mediaId = cursor.getLong(idCol)
            val title = cursor.getString(titleCol) ?: "Unknown Title"
            val artist = cursor.getString(artistCol) ?: "Unknown Artist"
            val album = cursor.getString(albumCol) ?: "Unknown Album"
            val duration = cursor.getLong(durationCol)
            val dataPath = cursor.getString(dataCol) ?: ""

            val contentUri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                mediaId
            ).toString()

            val finalPath = if (dataPath.isNotBlank() && File(dataPath).exists()) dataPath else contentUri

            val existing = trackDao.getTrackByPath(finalPath) ?: trackDao.getTrackByPath(contentUri)
            if (existing == null) {
                trackDao.insertTrack(
                    TrackEntity(
                        title = title,
                        artist = artist,
                        album = album,
                        durationMs = duration,
                        filePath = finalPath,
                        bitrateKbps = 320,
                        sampleRateHz = 44100
                    )
                )
            }
        }
    }

    /**
     * Scans MediaStore and physical storage for local audio files.
     * Prioritizes /storage/3263-3638/phonks, and also scans device audio files so library is never empty.
     */
    suspend fun scanDeviceAudioFiles() = withContext(Dispatchers.IO) {
        // 1. Direct Physical File System Scan for /storage/3263-3638/phonks
        val phonksFolder = File("/storage/3263-3638/phonks")
        if (phonksFolder.exists() && phonksFolder.isDirectory) {
            scanFolder(phonksFolder)
        }

        // 2. Query MediaStore with strict target folder filter
        try {
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA
            )

            val targetFolder = "/storage/3263-3638/phonks"
            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DATA} LIKE ?"
            val selectionArgs = arrayOf("%$targetFolder/%")
            val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                processCursorTracks(cursor)
            }
        } catch (e: Exception) {
            Log.e("MusicRepository", "MediaStore scan exception: ${e.message}")
        }

        // 3. Scan common storage music folders and general MediaStore
        try {
            val commonFolders = listOf(
                File("/storage/emulated/0/Music"),
                File("/storage/emulated/0/Download"),
                File("/storage/emulated/0/Audio"),
                File(context.getExternalFilesDir(null), "Music")
            )
            for (f in commonFolders) {
                if (f.exists() && f.isDirectory) {
                    scanFolder(f)
                }
            }

            // General MediaStore query
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.DATA
                ),
                "${MediaStore.Audio.Media.IS_MUSIC} != 0",
                null,
                "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                processCursorTracks(cursor)
            }
        } catch (e: Exception) {
            Log.e("MusicRepository", "General MediaStore scan exception: ${e.message}")
        }
    }

    suspend fun toggleFavorite(trackId: Long, currentFav: Boolean) = withContext(Dispatchers.IO) {
        trackDao.updateFavorite(trackId, !currentFav)
    }

    /**
     * Moves a song into the secure encrypted audio vault.
     */
    suspend fun vaultTrack(track: TrackEntity, pin: String): Boolean = withContext(Dispatchers.IO) {
        val vaultFileName = "vault_${System.currentTimeMillis()}_${track.id}.enc"
        var success = false

        if (track.isProcedural) {
            // For procedural, we tag it directly as vaulted
            trackDao.setVaultStatus(track.id, true, vaultFileName)
            success = true
        } else {
            try {
                // If local file path
                val file = if (track.filePath.startsWith("content://")) {
                    // Read content URI into temp cache file then encrypt
                    val temp = File(context.cacheDir, "temp_vault_${track.id}")
                    context.contentResolver.openInputStream(Uri.parse(track.filePath))?.use { input ->
                        temp.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    temp
                } else {
                    File(track.filePath)
                }

                if (file.exists()) {
                    val encrypted = vaultCrypto.encryptFileToVault(file, pin, vaultFileName)
                    if (encrypted != null) {
                        trackDao.setVaultStatus(track.id, true, vaultFileName)
                        success = true
                    }
                    if (file.name.startsWith("temp_vault_")) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                Log.e("MusicRepository", "Failed to vault track: ${e.message}")
            }
        }
        success
    }

    /**
     * Removes a track from the encrypted vault back to the library.
     */
    suspend fun unvaultTrack(track: TrackEntity): Boolean = withContext(Dispatchers.IO) {
        if (track.vaultFileName.isNotEmpty()) {
            vaultCrypto.deleteFromVault(track.vaultFileName)
        }
        trackDao.setVaultStatus(track.id, false, "")
        true
    }

    suspend fun deleteTrack(track: TrackEntity) = withContext(Dispatchers.IO) {
        if (track.isVaulted && track.vaultFileName.isNotEmpty()) {
            vaultCrypto.deleteFromVault(track.vaultFileName)
        }
        trackDao.deleteTrack(track)
    }

    suspend fun saveCustomPreset(preset: EqPresetEntity): Long = withContext(Dispatchers.IO) {
        eqPresetDao.insertPreset(preset)
    }

    suspend fun deleteCustomPreset(id: Long) = withContext(Dispatchers.IO) {
        eqPresetDao.deleteCustomPreset(id)
    }
}
