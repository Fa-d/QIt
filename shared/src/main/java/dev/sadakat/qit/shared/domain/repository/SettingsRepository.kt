package dev.sadakat.qit.shared.domain.repository

import dev.sadakat.qit.shared.domain.valueobject.AudioQuality
import dev.sadakat.qit.shared.domain.valueobject.PlaybackDestination
import dev.sadakat.qit.shared.domain.valueobject.StreamingMode
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository interface for user settings
 */
interface SettingsRepository {

    /**
     * Gets the preferred streaming quality
     */
    suspend fun getStreamingQuality(): AudioQuality

    /**
     * Sets the preferred streaming quality
     */
    suspend fun setStreamingQuality(quality: AudioQuality): Result<Unit>

    /**
     * Observes streaming quality changes
     */
    fun observeStreamingQuality(): Flow<AudioQuality>

    /**
     * Gets the preferred download quality
     */
    suspend fun getDownloadQuality(): AudioQuality

    /**
     * Sets the preferred download quality
     */
    suspend fun setDownloadQuality(quality: AudioQuality): Result<Unit>

    /**
     * Observes download quality changes
     */
    fun observeDownloadQuality(): Flow<AudioQuality>

    /**
     * Gets the preferred streaming mode
     */
    suspend fun getStreamingMode(): StreamingMode

    /**
     * Sets the preferred streaming mode
     */
    suspend fun setStreamingMode(mode: StreamingMode): Result<Unit>

    /**
     * Gets auto-download on WiFi setting
     */
    suspend fun getAutoDownloadOnWiFi(): Boolean

    /**
     * Sets auto-download on WiFi setting
     */
    suspend fun setAutoDownloadOnWiFi(enabled: Boolean): Result<Unit>

    /**
     * Gets auto-sync enabled setting
     */
    suspend fun getAutoSyncEnabled(): Boolean

    /**
     * Sets auto-sync enabled setting
     */
    suspend fun setAutoSyncEnabled(enabled: Boolean): Result<Unit>

    /**
     * Gets maximum storage for downloads (in bytes)
     */
    suspend fun getMaxStorageForDownloads(): Long

    /**
     * Sets maximum storage for downloads
     */
    suspend fun setMaxStorageForDownloads(bytes: Long): Result<Unit>

    /**
     * Gets the default playback destination (Phone or Watch)
     */
    suspend fun getPlaybackDestination(): PlaybackDestination

    /**
     * Sets the default playback destination
     */
    suspend fun setPlaybackDestination(destination: PlaybackDestination): Result<Unit>

    /**
     * Observes playback destination changes
     */
    fun observePlaybackDestination(): Flow<PlaybackDestination>
}
