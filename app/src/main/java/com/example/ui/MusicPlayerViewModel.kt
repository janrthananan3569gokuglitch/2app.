package com.example.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.NativeAudioEngine
import com.example.data.AppDatabase
import com.example.data.EqPresetEntity
import com.example.data.MusicRepository
import com.example.data.TrackEntity
import com.example.dsp.ProceduralToneGenerator
import com.example.service.AudioVaultService
import com.example.vault.VaultCrypto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class RepeatMode {
    OFF, ALL, ONE
}

class MusicPlayerViewModel(application: Application) : AndroidViewModel(application) {
    val database = AppDatabase.getInstance(application)
    val vaultCrypto = VaultCrypto(application)
    val repository = MusicRepository(application, database, vaultCrypto)
    val audioEngine = NativeAudioEngine(application)

    // Current Track and Playback
    private val _currentTrack = MutableStateFlow<TrackEntity?>(null)
    val currentTrack: StateFlow<TrackEntity?> = _currentTrack.asStateFlow()

    val isPlaying: StateFlow<Boolean> = audioEngine.isPlaying
    val playbackPositionMs: StateFlow<Long> = audioEngine.currentPositionMs
    val durationMs: StateFlow<Long> = audioEngine.durationMs
    val spectrumBands: StateFlow<FloatArray> = audioEngine.spectrumBands
    val waveformPoints: StateFlow<FloatArray> = audioEngine.waveformPoints

    // Shuffle & Repeat
    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.ALL)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    // 10-Band EQ State
    private val _eqBandGains = MutableStateFlow(FloatArray(10) { 0.0f })
    val eqBandGains: StateFlow<FloatArray> = _eqBandGains.asStateFlow()

    private val _preAmpGain = MutableStateFlow(0.0f)
    val preAmpGain: StateFlow<Float> = _preAmpGain.asStateFlow()

    private val _isEqEnabled = MutableStateFlow(true)
    val isEqEnabled: StateFlow<Boolean> = _isEqEnabled.asStateFlow()

    private val _activePresetName = MutableStateFlow("Flat (Direct Bypass)")
    val activePresetName: StateFlow<String> = _activePresetName.asStateFlow()

    // 3D Spatial Audio State
    private val _spatialAzimuth = MutableStateFlow(0.0f) // -180 to 180
    val spatialAzimuth: StateFlow<Float> = _spatialAzimuth.asStateFlow()

    private val _spatialElevation = MutableStateFlow(0.0f) // -45 to 90
    val spatialElevation: StateFlow<Float> = _spatialElevation.asStateFlow()

    private val _spatialDistance = MutableStateFlow(1.5f) // 0.5 to 10
    val spatialDistance: StateFlow<Float> = _spatialDistance.asStateFlow()

    private val _isSpatialEnabled = MutableStateFlow(true)
    val isSpatialEnabled: StateFlow<Boolean> = _isSpatialEnabled.asStateFlow()

    // Schroeder Reverb State
    private val _reverbWet = MutableStateFlow(0.20f)
    val reverbWet: StateFlow<Float> = _reverbWet.asStateFlow()

    private val _reverbRoomSize = MutableStateFlow(0.75f)
    val reverbRoomSize: StateFlow<Float> = _reverbRoomSize.asStateFlow()

    private val _isReverbEnabled = MutableStateFlow(false)
    val isReverbEnabled: StateFlow<Boolean> = _isReverbEnabled.asStateFlow()

    // Analogue Tube Pre-Amp & Saturation State
    private val _tubeDrive = MutableStateFlow(1.0f)
    val tubeDrive: StateFlow<Float> = _tubeDrive.asStateFlow()

    private val _tubeWarmthBias = MutableStateFlow(0.15f)
    val tubeWarmthBias: StateFlow<Float> = _tubeWarmthBias.asStateFlow()

    private val _bassExciter = MutableStateFlow(0.0f)
    val bassExciter: StateFlow<Float> = _bassExciter.asStateFlow()

    private val _isTubeEnabled = MutableStateFlow(false)
    val isTubeEnabled: StateFlow<Boolean> = _isTubeEnabled.asStateFlow()

    // Stereo Expander State
    private val _stereoWidth = MutableStateFlow(1.0f)
    val stereoWidth: StateFlow<Float> = _stereoWidth.asStateFlow()

    private val _isStereoExpanderEnabled = MutableStateFlow(true)
    val isStereoExpanderEnabled: StateFlow<Boolean> = _isStereoExpanderEnabled.asStateFlow()

    // Dolby Atmos State
    private val _isDolbyAtmosActive = MutableStateFlow(false)
    val isDolbyAtmosActive: StateFlow<Boolean> = _isDolbyAtmosActive.asStateFlow()

    fun toggleDolbyAtmos() {
        val newState = !_isDolbyAtmosActive.value
        _isDolbyAtmosActive.value = newState
        vaultService?.setDolbyAtmosEnabled(newState)
        if (newState) {
            audioEngine.spatialHrtf.isEnabled = true
            audioEngine.spatialHrtf.setPosition(0.0f, 0.0f, 1.0f)
            audioEngine.reverb.isEnabled = true
            audioEngine.reverb.wetMix = 0.25f
            audioEngine.reverb.roomSize = 0.75f
            audioEngine.stereoExpander.isEnabled = true
            audioEngine.stereoExpander.stereoWidth = 1.4f
        }
    }

    // Vault Security State
    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    private val _vaultPinInput = MutableStateFlow("")
    val vaultPinInput: StateFlow<String> = _vaultPinInput.asStateFlow()

    private val _vaultError = MutableStateFlow<String?>(null)
    val vaultError: StateFlow<String?> = _vaultError.asStateFlow()

    // Library Data Flows
    val libraryTracks: StateFlow<List<TrackEntity>> = repository.libraryTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vaultedTracks: StateFlow<List<TrackEntity>> = repository.vaultedTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteTracks: StateFlow<List<TrackEntity>> = repository.favoriteTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val eqPresets: StateFlow<List<EqPresetEntity>> = repository.eqPresets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var vaultService: AudioVaultService? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            vaultService = (service as? AudioVaultService.LocalBinder)?.getService()
            vaultService?.serviceCallback = object : AudioVaultService.ServicePlaybackCallback {
                override fun onPlay() = resume()
                override fun onPause() = pause()
                override fun onSkipNext() = playNext()
                override fun onSkipPrevious() = playPrevious()
                override fun onSeekTo(positionMs: Long) = seekTo(positionMs)
                override fun onSeekRelative(offsetMs: Long) = seekRelative(offsetMs)
                override fun onToggleDolby(enabled: Boolean) {
                    _isDolbyAtmosActive.value = enabled
                }
            }
            updateNotification()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            vaultService = null
        }
    }

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
            repository.scanDeviceAudioFiles()
        }

        val serviceIntent = Intent(application, AudioVaultService::class.java)
        try {
            application.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            Log.e("MusicPlayerViewModel", "Error binding AudioVaultService: ${e.message}")
        }

        viewModelScope.launch {
            audioEngine.isPlaying.collect { updateNotification() }
        }
        viewModelScope.launch {
            _currentTrack.collect { updateNotification() }
        }
        viewModelScope.launch {
            libraryTracks.collect { tracks ->
                if (_currentTrack.value == null && tracks.isNotEmpty()) {
                    _currentTrack.value = tracks.first()
                }
            }
        }
    }

    fun pause() {
        audioEngine.pause()
        updateNotification()
    }

    fun resume() {
        audioEngine.resume()
        updateNotification()
    }

    fun seekTo(positionMs: Long) {
        audioEngine.seekTo(positionMs)
        updateNotification()
    }

    fun seekForward10() {
        audioEngine.seekRelative(10000L)
        updateNotification()
    }

    fun seekBackward10() {
        audioEngine.seekRelative(-10000L)
        updateNotification()
    }

    fun seekRelative(offsetMs: Long) {
        audioEngine.seekRelative(offsetMs)
        updateNotification()
    }

    private fun updateNotification() {
        val track = _currentTrack.value ?: return
        vaultService?.updatePlaybackState(
            title = track.title,
            artist = track.artist,
            album = track.album,
            isPlaying = audioEngine.isPlaying.value,
            positionMs = audioEngine.currentPositionMs.value,
            durationMs = audioEngine.durationMs.value,
            filePath = track.filePath
        )
    }

    // Playback Controls
    fun playTrack(track: TrackEntity) {
        _currentTrack.value = track
        if (track.isVaulted) {
            // Decrypt in-memory using verified PIN
            val pin = _vaultPinInput.value.ifEmpty { "1234" }
            val decryptedBytes = vaultCrypto.decryptVaultAudioInMemory(track.vaultFileName, pin)
            if (decryptedBytes != null) {
                audioEngine.playVaultAudio(decryptedBytes, track.durationMs)
            } else {
                _vaultError.value = "Failed to decrypt vaulted audio with current session key"
            }
        } else if (track.isProcedural) {
            val signal = try {
                ProceduralToneGenerator.SignalType.valueOf(track.proceduralType)
            } catch (_: Exception) {
                ProceduralToneGenerator.SignalType.CYBERPUNK_GROOVE
            }
            audioEngine.playProcedural(signal)
        } else {
            val uri = Uri.parse(track.filePath)
            audioEngine.playUri(uri, track.durationMs)
        }
    }

    fun togglePlayPause() {
        if (_currentTrack.value == null) {
            val list = libraryTracks.value
            if (list.isNotEmpty()) {
                playTrack(list.first())
                return
            }
        }
        audioEngine.togglePlayPause()
    }

    fun playNext() {
        val list = if (_currentTrack.value?.isVaulted == true) vaultedTracks.value else libraryTracks.value
        if (list.isEmpty()) return
        val currentIdx = list.indexOfFirst { it.id == _currentTrack.value?.id }
        val nextIdx = if (_isShuffleEnabled.value) {
            (0 until list.size).random()
        } else {
            (currentIdx + 1) % list.size
        }
        playTrack(list[nextIdx])
    }

    fun playPrevious() {
        val list = if (_currentTrack.value?.isVaulted == true) vaultedTracks.value else libraryTracks.value
        if (list.isEmpty()) return
        val currentIdx = list.indexOfFirst { it.id == _currentTrack.value?.id }
        val prevIdx = if (currentIdx <= 0) list.size - 1 else currentIdx - 1
        playTrack(list[prevIdx])
    }

    fun toggleShuffle() {
        _isShuffleEnabled.value = !_isShuffleEnabled.value
    }

    fun toggleRepeat() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    fun toggleFavorite(track: TrackEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(track.id, track.isFavorite)
        }
    }

    // EQ Controls
    fun setEqBandGain(index: Int, gainDb: Float) {
        val newGains = _eqBandGains.value.clone()
        newGains[index] = gainDb
        _eqBandGains.value = newGains
        audioEngine.equalizer.setBandGain(index, gainDb)
        _activePresetName.value = "Custom"
    }

    fun setPreAmpGain(gainDb: Float) {
        _preAmpGain.value = gainDb
        audioEngine.equalizer.setPreAmp(gainDb)
    }

    fun toggleEqEnabled() {
        val newState = !_isEqEnabled.value
        _isEqEnabled.value = newState
        audioEngine.equalizer.isEnabled = newState
    }

    fun applyPreset(preset: EqPresetEntity) {
        _activePresetName.value = preset.name
        val gains = preset.parseGains()
        _eqBandGains.value = gains
        audioEngine.equalizer.setAllGains(gains)

        _preAmpGain.value = preset.preAmpGain
        audioEngine.equalizer.setPreAmp(preset.preAmpGain)

        _tubeDrive.value = preset.tubeWarmth
        audioEngine.tubeSaturator.drive = preset.tubeWarmth

        _spatialAzimuth.value = preset.spatialAzimuth
        _spatialElevation.value = preset.spatialElevation
        _spatialDistance.value = preset.spatialDistance
        audioEngine.spatialHrtf.setPosition(preset.spatialAzimuth, preset.spatialElevation, preset.spatialDistance)

        _reverbWet.value = preset.reverbWet
        audioEngine.reverb.wetMix = preset.reverbWet

        _stereoWidth.value = preset.stereoWidth
        audioEngine.stereoExpander.stereoWidth = preset.stereoWidth
    }

    fun resetEqFlat() {
        _activePresetName.value = "Flat (Direct Bypass)"
        _eqBandGains.value = FloatArray(10) { 0.0f }
        _preAmpGain.value = 0.0f
        audioEngine.equalizer.resetFlat()
    }

    fun saveCustomPreset(name: String) {
        viewModelScope.launch {
            val gainsStr = _eqBandGains.value.joinToString(",") { it.toString() }
            val preset = EqPresetEntity(
                name = name,
                isBuiltIn = false,
                preAmpGain = _preAmpGain.value,
                bandGains = gainsStr,
                tubeWarmth = _tubeDrive.value,
                spatialAzimuth = _spatialAzimuth.value,
                spatialElevation = _spatialElevation.value,
                spatialDistance = _spatialDistance.value,
                reverbWet = _reverbWet.value,
                stereoWidth = _stereoWidth.value
            )
            repository.saveCustomPreset(preset)
            _activePresetName.value = name
        }
    }

    // 3D Spatial Audio Controls
    fun setSpatialPosition(azimuth: Float, elevation: Float, distance: Float) {
        _spatialAzimuth.value = azimuth
        _spatialElevation.value = elevation
        _spatialDistance.value = distance
        audioEngine.spatialHrtf.setPosition(azimuth, elevation, distance)
    }

    fun toggleSpatialEnabled() {
        val newState = !_isSpatialEnabled.value
        _isSpatialEnabled.value = newState
        audioEngine.spatialHrtf.isEnabled = newState
    }

    // Reverb Controls
    fun setReverbWet(wet: Float) {
        _reverbWet.value = wet
        audioEngine.reverb.wetMix = wet
    }

    fun setReverbRoomSize(size: Float) {
        _reverbRoomSize.value = size
        audioEngine.reverb.roomSize = size
    }

    fun toggleReverbEnabled() {
        val newState = !_isReverbEnabled.value
        _isReverbEnabled.value = newState
        audioEngine.reverb.isEnabled = newState
    }

    // Tube Pre-Amp & Saturation Controls
    fun setTubeDrive(drive: Float) {
        _tubeDrive.value = drive
        audioEngine.tubeSaturator.drive = drive
    }

    fun setTubeWarmthBias(bias: Float) {
        _tubeWarmthBias.value = bias
        audioEngine.tubeSaturator.warmthBias = bias
    }

    fun setBassExciter(gain: Float) {
        _bassExciter.value = gain
        audioEngine.tubeSaturator.bassExciterGain = gain
    }

    fun toggleTubeEnabled() {
        val newState = !_isTubeEnabled.value
        _isTubeEnabled.value = newState
        audioEngine.tubeSaturator.isEnabled = newState
    }

    // Stereo Expander Controls
    fun setStereoWidth(width: Float) {
        _stereoWidth.value = width
        audioEngine.stereoExpander.stereoWidth = width
    }

    fun toggleStereoExpander() {
        val newState = !_isStereoExpanderEnabled.value
        _isStereoExpanderEnabled.value = newState
        audioEngine.stereoExpander.isEnabled = newState
    }

    // Vault PIN and Actions
    fun onPinDigit(digit: String) {
        if (_vaultPinInput.value.length < 4) {
            val updated = _vaultPinInput.value + digit
            _vaultPinInput.value = updated
            _vaultError.value = null
            if (updated.length == 4) {
                unlockVault(updated)
            }
        }
    }

    fun onPinBackspace() {
        if (_vaultPinInput.value.isNotEmpty()) {
            _vaultPinInput.value = _vaultPinInput.value.dropLast(1)
            _vaultError.value = null
        }
    }

    fun unlockVault(pin: String) {
        if (vaultCrypto.verifyPin(pin)) {
            _isVaultUnlocked.value = true
            _vaultError.value = null
        } else {
            _vaultError.value = "ACCESS DENIED: Invalid Master PIN"
            _vaultPinInput.value = ""
        }
    }

    fun lockVault() {
        _isVaultUnlocked.value = false
        _vaultPinInput.value = ""
        _vaultError.value = null
        if (_currentTrack.value?.isVaulted == true) {
            audioEngine.stop()
            _currentTrack.value = null
        }
    }

    fun vaultTrack(track: TrackEntity) {
        viewModelScope.launch {
            val pin = _vaultPinInput.value.ifEmpty { "1234" }
            val ok = repository.vaultTrack(track, pin)
            if (ok && _currentTrack.value?.id == track.id) {
                audioEngine.stop()
                _currentTrack.value = null
            }
        }
    }

    fun unvaultTrack(track: TrackEntity) {
        viewModelScope.launch {
            repository.unvaultTrack(track)
            if (_currentTrack.value?.id == track.id) {
                audioEngine.stop()
                _currentTrack.value = null
            }
        }
    }

    fun rescanLibrary() {
        viewModelScope.launch {
            repository.scanDeviceAudioFiles()
        }
    }

    fun scanCustomFolder(path: String, onResult: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val count = repository.scanCustomPath(path)
            onResult(count)
        }
    }

    fun playAllInList(tracks: List<TrackEntity>) {
        if (tracks.isNotEmpty()) {
            playTrack(tracks.first())
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.stop()
    }
}
