package dev.sadakat.qit.shared.domain.entity

/**
 * Domain Aggregate Root representing a Playlist
 * Maintains consistency and enforces business rules for songs
 */
data class Playlist(
    val id: PlaylistId,
    val name: String,
    val description: String?,
    private val songIds: List<SongId>,
    val createdAt: Long,
    val updatedAt: Long,
    val coverArtUri: String?
) {

    init {
        require(name.isNotBlank()) { "Playlist name cannot be blank" }
        require(songIds.size <= MAX_SONGS) { "Playlist cannot exceed $MAX_SONGS songs" }
    }

    /**
     * Returns immutable list of song IDs
     */
    fun getSongIds(): List<SongId> = songIds.toList()

    /**
     * Returns the number of songs in the playlist
     */
    fun songCount(): Int = songIds.size

    /**
     * Checks if the playlist is empty
     */
    fun isEmpty(): Boolean = songIds.isEmpty()

    /**
     * Checks if the playlist contains a specific song
     */
    fun containsSong(songId: SongId): Boolean = songIds.contains(songId)

    /**
     * Adds a song to the playlist
     * Returns a new Playlist instance (immutable)
     */
    fun addSong(songId: SongId): Playlist {
        require(!containsSong(songId)) { "Song already exists in playlist" }
        require(songIds.size < MAX_SONGS) { "Playlist has reached maximum capacity" }

        return copy(
            songIds = songIds + songId,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Removes a song from the playlist
     * Returns a new Playlist instance (immutable)
     */
    fun removeSong(songId: SongId): Playlist {
        require(containsSong(songId)) { "Song does not exist in playlist" }

        return copy(
            songIds = songIds.filterNot { it == songId },
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Adds multiple songs to the playlist
     */
    fun addSongs(newSongIds: List<SongId>): Playlist {
        val uniqueNewSongs = newSongIds.filterNot { containsSong(it) }
        require(songIds.size + uniqueNewSongs.size <= MAX_SONGS) {
            "Adding ${uniqueNewSongs.size} songs would exceed maximum capacity"
        }

        return copy(
            songIds = songIds + uniqueNewSongs,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Reorders a song in the playlist
     */
    fun moveSong(songId: SongId, toIndex: Int): Playlist {
        require(containsSong(songId)) { "Song does not exist in playlist" }
        require(toIndex in songIds.indices) { "Invalid target index" }

        val mutableList = songIds.toMutableList()
        val fromIndex = mutableList.indexOf(songId)
        mutableList.removeAt(fromIndex)
        mutableList.add(toIndex, songId)

        return copy(
            songIds = mutableList.toList(),
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Clears all songs from the playlist
     */
    fun clearSongs(): Playlist {
        return copy(
            songIds = emptyList(),
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Updates the playlist metadata
     */
    fun updateMetadata(
        newName: String? = null,
        newDescription: String? = null,
        newCoverArtUri: String? = null
    ): Playlist {
        val updatedName = newName ?: this.name
        require(updatedName.isNotBlank()) { "Playlist name cannot be blank" }

        return copy(
            name = updatedName,
            description = newDescription ?: this.description,
            coverArtUri = newCoverArtUri ?: this.coverArtUri,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Checks if the playlist can accept more songs
     */
    fun canAddMoreSongs(): Boolean = songIds.size < MAX_SONGS

    /**
     * Returns the number of songs that can still be added
     */
    fun remainingCapacity(): Int = MAX_SONGS - songIds.size

    companion object {
        const val MAX_SONGS = 1000

        /**
         * Creates a new empty Playlist
         */
        fun create(
            name: String,
            description: String? = null,
            coverArtUri: String? = null
        ): Playlist {
            require(name.isNotBlank()) { "Playlist name cannot be blank" }

            return Playlist(
                id = PlaylistId.generate(),
                name = name,
                description = description,
                songIds = emptyList(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                coverArtUri = coverArtUri
            )
        }

        /**
         * Creates a new Playlist with initial songs
         */
        fun createWithSongs(
            name: String,
            description: String? = null,
            initialSongs: List<SongId>,
            coverArtUri: String? = null
        ): Playlist {
            require(name.isNotBlank()) { "Playlist name cannot be blank" }
            require(initialSongs.size <= MAX_SONGS) { "Cannot create playlist with more than $MAX_SONGS songs" }

            val uniqueSongs = initialSongs.distinct()
            require(uniqueSongs.size == initialSongs.size) { "Duplicate songs in initial list" }

            val now = System.currentTimeMillis()
            return Playlist(
                id = PlaylistId.generate(),
                name = name,
                description = description,
                songIds = uniqueSongs,
                createdAt = now,
                updatedAt = now,
                coverArtUri = coverArtUri
            )
        }
    }
}
