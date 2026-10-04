package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val filePath: String,
    val isVaulted: Boolean = false,
    val vaultFileName: String = "",
    val isFavorite: Boolean = false,
    val isProcedural: Boolean = false,
    val proceduralType: String = "",
    val bitrateKbps: Int = 320,
    val sampleRateHz: Int = 44100,
    val addedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "eq_presets")
data class EqPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isBuiltIn: Boolean = false,
    val preAmpGain: Float = 0.0f,
    val bandGains: String, // Comma-delimited 10 band gains
    val tubeWarmth: Float = 1.0f,
    val spatialAzimuth: Float = 0.0f,
    val spatialElevation: Float = 0.0f,
    val spatialDistance: Float = 1.5f,
    val reverbWet: Float = 0.2f,
    val stereoWidth: Float = 1.0f
) {
    fun parseGains(): FloatArray {
        return try {
            val parts = bandGains.split(",")
            FloatArray(10) { i ->
                if (i < parts.size) parts[i].trim().toFloatOrNull() ?: 0.0f else 0.0f
            }
        } catch (_: Exception) {
            FloatArray(10)
        }
    }
}

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdTimestamp: Long = System.currentTimeMillis()
)
