package dev.sadakat.qit.wear.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sadakat.qit.shared.domain.entity.Playlist
import dev.sadakat.qit.shared.domain.entity.PlaylistId
import dev.sadakat.qit.shared.domain.entity.Song
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.wear.application.usecase.playlist.GetAllPlaylistsUseCase
import dev.sadakat.qit.wear.application.usecase.playlist.GetPlaylistSongsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Music Library screen
 * Provides a flat list of all songs grouped by playlist
 */
@HiltViewModel
class MusicLibraryViewModel @Inject constructor(
    private val getAllPlaylistsUseCase: GetAllPlaylistsUseCase,
    private val getPlaylistSongsUseCase: GetPlaylistSongsUseCase
) : ViewModel() {

    private val _libraryItems = MutableStateFlow<List<MusicLibraryItem>>(emptyList())
    val libraryItems: StateFlow<List<MusicLibraryItem>> = _libraryItems.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadMusicLibrary()
    }

    /**
     * Loads all playlists and their songs into a flat list
     */
    fun loadMusicLibrary() {
        viewModelScope.launch {
            _isLoading.value = true

            getAllPlaylistsUseCase().collect { playlists ->
                val items = mutableListOf<MusicLibraryItem>()

                playlists.forEach { playlist ->
                    // Add playlist header
                    items.add(MusicLibraryItem.PlaylistHeader(playlist))

                    // Load and add songs for this playlist
                    getPlaylistSongsUseCase(playlist.id).collect { songs ->
                        songs.forEach { song ->
                            items.add(MusicLibraryItem.SongItem(song, playlist.name))
                        }
                    }
                }

                _libraryItems.value = items
                _isLoading.value = false
            }
        }
    }

    /**
     * Refresh the library
     */
    fun refresh() {
        loadMusicLibrary()
    }
}

/**
 * Sealed class representing items in the music library list
 */
sealed class MusicLibraryItem {
    /**
     * Playlist header/section divider
     */
    data class PlaylistHeader(val playlist: Playlist) : MusicLibraryItem()

    /**
     * Song item with parent playlist info
     */
    data class SongItem(val song: Song, val playlistName: String) : MusicLibraryItem()
}
