package cc.tomko.outify.ui.viewmodel.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.tomko.outify.core.model.Track
import cc.tomko.outify.core.Spirc.SpircWrapper
import cc.tomko.outify.data.metadata.TrackMetadataHelper
import cc.tomko.outify.playback.PlaybackStateHolder
import cc.tomko.outify.ui.GlobalPopupController
import cc.tomko.outify.ui.PopupSpec
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class MiniPlayerViewModel @Inject constructor(
    private val playbackStateHolder: PlaybackStateHolder,
    private val trackMetadataHelper: TrackMetadataHelper,
    private val json: Json,
    val spirc: SpircWrapper,
) : ViewModel() {
    private val _positionMs =
        MutableStateFlow(playbackStateHolder.estimatePosition().inWholeMilliseconds)
    val positionMs = _positionMs.asStateFlow()

    private val _previousTrack = MutableStateFlow<Track?>(null)
    val previousTrack: StateFlow<Track?> = _previousTrack.asStateFlow()

    private val _nextTrack = MutableStateFlow<Track?>(null)
    val nextTrack: StateFlow<Track?> = _nextTrack.asStateFlow()

    private val _hasPrevious = MutableStateFlow(false)
    val hasPrevious: StateFlow<Boolean> = _hasPrevious.asStateFlow()

    private val _hasNext = MutableStateFlow(false)
    val hasNext: StateFlow<Boolean> = _hasNext.asStateFlow()

    val currentTrack: StateFlow<Track?> = playbackStateHolder.state
        .map { it.currentTrack }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val isPlaying: StateFlow<Boolean> = playbackStateHolder.state
        .map { it.isPlaying }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

    init {
        viewModelScope.launch {
            while (isActive) {
                _positionMs.value = playbackStateHolder.estimatePosition().inWholeMilliseconds
                delay(250L)
            }
        }
        viewModelScope.launch {
            currentTrack.collect { loadNeighbors() }
        }
    }

    private suspend fun loadNeighbors() {
        val (previous, next, hasNeighbors) = withContext(Dispatchers.IO) {
            val prevUri = decodeUris(spirc.previousTracks()).lastOrNull()
            val nextUri = decodeUris(spirc.nextTracks()).firstOrNull()
            val previousTrack = prevUri?.let { uri ->
                trackMetadataHelper.getTrackMetadata(listOf(uri)).firstOrNull()
            }
            val nextTrack = nextUri?.let { uri ->
                trackMetadataHelper.getTrackMetadata(listOf(uri)).firstOrNull()
            }
            Triple(previousTrack, nextTrack, (prevUri != null) to (nextUri != null))
        }
        _previousTrack.value = previous
        _nextTrack.value = next
        _hasPrevious.value = hasNeighbors.first
        _hasNext.value = hasNeighbors.second
    }

    fun skipPrevious() {
        viewModelScope.launch(Dispatchers.IO) { spirc.playerPrevious() }
    }

    fun skipNext() {
        viewModelScope.launch(Dispatchers.IO) { spirc.playerNext() }
    }

    private fun decodeUris(raw: String): List<String> {
        return try {
            json.decodeFromString<List<String>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun openDevices() {
        GlobalPopupController.show(PopupSpec.PlaybackDevices())
    }

    fun isBuffering(): Flow<Boolean> =
        playbackStateHolder.state
            .map { it.isBuffering }
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = false
            )

    fun isActiveDevice(): Flow<Boolean> =
        playbackStateHolder.state
            .map { it.isActiveDevice }

    fun setTrack(track: Track?) {
        if (track == null) {
            spirc.playerPause()
        }
        playbackStateHolder.setTrack(track)
    }
}
