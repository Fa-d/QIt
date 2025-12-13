package dev.sadakat.qit.wear.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sadakat.qit.shared.domain.entity.Playlist
import dev.sadakat.qit.shared.domain.entity.PlaylistId
import dev.sadakat.qit.shared.domain.entity.Song
import dev.sadakat.qit.wear.application.usecase.playlist.GetAllPlaylistsUseCase
import dev.sadakat.qit.wear.application.usecase.playlist.GetPlaylistSongsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val getAllPlaylistsUseCase: GetAllPlaylistsUseCase,
    private val getPlaylistSongsUseCase: GetPlaylistSongsUseCase
) : ViewModel() {

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
    val selectedPlaylist: StateFlow<Playlist?> = _selectedPlaylist.asStateFlow()

    private val _playlistSongs = MutableStateFlow<List<Song>>(emptyList())
    val playlistSongs: StateFlow<List<Song>> = _playlistSongs.asStateFlow()

    init {
        loadPlaylists()
    }

    fun loadPlaylists() {
        viewModelScope.launch {
            getAllPlaylistsUseCase().collect { playlists ->
                _playlists.value = playlists
            }
        }
    }

    fun selectPlaylist(playlistId: String) {
        viewModelScope.launch {
            // Find the playlist in the current list
            val playlist = _playlists.value.find { it.id.value == playlistId }
            _selectedPlaylist.value = playlist

            playlist?.let {
                // Load songs for the selected playlist
                getPlaylistSongsUseCase(PlaylistId(playlistId)).collect { songs ->
                    _playlistSongs.value = songs
                }
            }
        }
    }
}
