package dev.sadakat.qit.infrastructure.persistence.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.sadakat.qit.shared.domain.repository.SettingsRepository
import dev.sadakat.qit.shared.domain.valueobject.AudioQuality
import dev.sadakat.qit.shared.domain.valueobject.StreamingMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * DataStore-based implementation of SettingsRepository
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStoreSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private object PreferenceKeys {
        val STREAMING_QUALITY = stringPreferencesKey("streaming_quality")
        val DOWNLOAD_QUALITY = stringPreferencesKey("download_quality")
        val STREAMING_MODE = stringPreferencesKey("streaming_mode")
        val AUTO_DOWNLOAD_WIFI = booleanPreferencesKey("auto_download_wifi")
        val AUTO_SYNC_ENABLED = booleanPreferencesKey("auto_sync_enabled")
        val MAX_STORAGE_DOWNLOADS = longPreferencesKey("max_storage_downloads")
    }

    override suspend fun getStreamingQuality(): AudioQuality {
        val prefs = context.dataStore.data.first()
        val qualityStr = prefs[PreferenceKeys.STREAMING_QUALITY] ?: "MEDIUM"
        return parseQuality(qualityStr)
    }

    override suspend fun setStreamingQuality(quality: AudioQuality): Result<Unit> {
        return try {
            context.dataStore.edit { prefs ->
                prefs[PreferenceKeys.STREAMING_QUALITY] = quality.name
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeStreamingQuality(): Flow<AudioQuality> {
        return context.dataStore.data.map { prefs ->
            val qualityStr = prefs[PreferenceKeys.STREAMING_QUALITY] ?: "MEDIUM"
            parseQuality(qualityStr)
        }
    }

    override suspend fun getDownloadQuality(): AudioQuality {
        val prefs = context.dataStore.data.first()
        val qualityStr = prefs[PreferenceKeys.DOWNLOAD_QUALITY] ?: "HIGH"
        return parseQuality(qualityStr)
    }

    override suspend fun setDownloadQuality(quality: AudioQuality): Result<Unit> {
        return try {
            context.dataStore.edit { prefs ->
                prefs[PreferenceKeys.DOWNLOAD_QUALITY] = quality.name
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeDownloadQuality(): Flow<AudioQuality> {
        return context.dataStore.data.map { prefs ->
            val qualityStr = prefs[PreferenceKeys.DOWNLOAD_QUALITY] ?: "HIGH"
            parseQuality(qualityStr)
        }
    }

    override suspend fun getStreamingMode(): StreamingMode {
        val prefs = context.dataStore.data.first()
        val modeStr = prefs[PreferenceKeys.STREAMING_MODE] ?: "PROGRESSIVE"
        return parseStreamingMode(modeStr)
    }

    override suspend fun setStreamingMode(mode: StreamingMode): Result<Unit> {
        return try {
            context.dataStore.edit { prefs ->
                prefs[PreferenceKeys.STREAMING_MODE] = mode.name
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAutoDownloadOnWiFi(): Boolean {
        val prefs = context.dataStore.data.first()
        return prefs[PreferenceKeys.AUTO_DOWNLOAD_WIFI] ?: false
    }

    override suspend fun setAutoDownloadOnWiFi(enabled: Boolean): Result<Unit> {
        return try {
            context.dataStore.edit { prefs ->
                prefs[PreferenceKeys.AUTO_DOWNLOAD_WIFI] = enabled
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAutoSyncEnabled(): Boolean {
        val prefs = context.dataStore.data.first()
        return prefs[PreferenceKeys.AUTO_SYNC_ENABLED] ?: true
    }

    override suspend fun setAutoSyncEnabled(enabled: Boolean): Result<Unit> {
        return try {
            context.dataStore.edit { prefs ->
                prefs[PreferenceKeys.AUTO_SYNC_ENABLED] = enabled
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMaxStorageForDownloads(): Long {
        val prefs = context.dataStore.data.first()
        // Default: 2GB
        return prefs[PreferenceKeys.MAX_STORAGE_DOWNLOADS] ?: (2L * 1024 * 1024 * 1024)
    }

    override suspend fun setMaxStorageForDownloads(bytes: Long): Result<Unit> {
        return try {
            context.dataStore.edit { prefs ->
                prefs[PreferenceKeys.MAX_STORAGE_DOWNLOADS] = bytes
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseQuality(str: String): AudioQuality {
        return when (str) {
            "LOW" -> AudioQuality.LOW
            "MEDIUM" -> AudioQuality.MEDIUM
            "HIGH" -> AudioQuality.HIGH
            "ORIGINAL" -> AudioQuality.ORIGINAL
            else -> AudioQuality.MEDIUM
        }
    }

    private fun parseStreamingMode(str: String): StreamingMode {
        return when (str) {
            "REAL_TIME" -> StreamingMode.REAL_TIME
            "PROGRESSIVE" -> StreamingMode.PROGRESSIVE
            "LOCAL" -> StreamingMode.LOCAL
            else -> StreamingMode.PROGRESSIVE
        }
    }
}
