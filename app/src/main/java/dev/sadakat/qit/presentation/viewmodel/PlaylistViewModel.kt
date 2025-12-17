package dev.sadakat.qit.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sadakat.qit.application.usecase.playlist.*
import dev.sadakat.qit.shared.domain.entity.Playlist
import dev.sadakat.qit.shared.domain.entity.PlaylistId
import dev.sadakat.qit.shared.domain.entity.Song
import dev.sadakat.qit.shared.domain.entity.SongId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Playlist management
 * Uses use cases following Clean Architecture
 */
@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val getAllPlaylistsUseCase: GetAllPlaylistsUseCase,
    private val getPlaylistWithSongsUseCase: GetPlaylistWithSongsUseCase,
    private val createPlaylistUseCase: CreatePlaylistUseCase,
    private val deletePlaylistUseCase: DeletePlaylistUseCase,
    private val addSongToPlaylistUseCase: AddSongToPlaylistUseCase,
    private val removeSongFromPlaylistUseCase: dev.sadakat.qit.application.usecase.playlist.RemoveSongFromPlaylistUseCase,
    private val getAllSongsUseCase: dev.sadakat.qit.application.usecase.music.GetAllSongsUseCase
) : ViewModel() {

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
    val selectedPlaylist: StateFlow<Playlist?> = _selectedPlaylist.asStateFlow()

    private val _playlistSongs = MutableStateFlow<List<Song>>(emptyList())
    val playlistSongs: StateFlow<List<Song>> = _playlistSongs.asStateFlow()

    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())
    val allSongs: StateFlow<List<Song>> = _allSongs.asStateFlow()

    init {
        loadPlaylists()
        loadAllSongs()
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
            val result = getPlaylistWithSongsUseCase(PlaylistId.from(playlistId))

            result.onSuccess { (playlist, songs) ->
                _selectedPlaylist.value = playlist
                _playlistSongs.value = songs
            }
        }
    }

    fun createPlaylist(name: String, description: String? = null) {
        viewModelScope.launch {
            createPlaylistUseCase(
                CreatePlaylistUseCase.Params(
                    name = name,
                    description = description
                )
            )
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            deletePlaylistUseCase(PlaylistId.from(playlistId))
        }
    }

    fun addSongToPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            addSongToPlaylistUseCase(
                AddSongToPlaylistUseCase.Params(
                    playlistId = PlaylistId.from(playlistId),
                    songId = SongId.from(songId)
                )
            ).onSuccess {
                // Reload playlist songs after adding
                selectPlaylist(playlistId)
            }
        }
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            removeSongFromPlaylistUseCase(
                dev.sadakat.qit.application.usecase.playlist.RemoveSongFromPlaylistUseCase.Params(
                    playlistId = PlaylistId.from(playlistId),
                    songId = SongId.from(songId)
                )
            ).onSuccess {
                // Reload playlist songs after removing
                selectPlaylist(playlistId)
            }
        }
    }

    fun loadAllSongs() {
        viewModelScope.launch {
            getAllSongsUseCase().collect { songs ->
                _allSongs.value = songs
            }
        }
    }

    fun clearSelectedPlaylist() {
        _selectedPlaylist.value = null
        _playlistSongs.value = emptyList()
    }
}
