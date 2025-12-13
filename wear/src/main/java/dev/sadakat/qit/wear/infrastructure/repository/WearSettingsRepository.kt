package dev.sadakat.qit.wear.infrastructure.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.sadakat.qit.shared.domain.repository.SettingsRepository
import dev.sadakat.qit.shared.domain.valueobject.AudioQuality
import dev.sadakat.qit.shared.domain.valueobject.StreamingMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wear_settings")

/**
 * Watch-side implementation of SettingsRepository using DataStore
 */
@Singleton
class WearSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    companion object {
        private const val TAG = "WearSettingsRepo"

        // Preference keys
        private val STREAMING_QUALITY = stringPreferencesKey("streaming_quality")
        private val DOWNLOAD_QUALITY = stringPreferencesKey("download_quality")
        private val STREAMING_MODE = stringPreferencesKey("streaming_mode")
        private val AUTO_DOWNLOAD_WIFI = booleanPreferencesKey("auto_download_wifi")
        private val AUTO_SYNC_ENABLED = booleanPreferencesKey("auto_sync_enabled")
        private val MAX_STORAGE_DOWNLOADS = longPreferencesKey("max_storage_downloads")

        // Default values (optimized for watch constraints)
        private val DEFAULT_STREAMING_QUALITY = AudioQuality.LOW // Low for streaming to save bandwidth
        private val DEFAULT_DOWNLOAD_QUALITY = AudioQuality.MEDIUM
        private val DEFAULT_STREAMING_MODE = StreamingMode.REAL_TIME
        private const val DEFAULT_AUTO_DOWNLOAD_WIFI = false
        private const val DEFAULT_AUTO_SYNC_ENABLED = true
        private const val DEFAULT_MAX_STORAGE = 500L * 1024 * 1024 // 500 MB default for watch
    }

    private val dataStore = context.dataStore

    override suspend fun getStreamingQuality(): AudioQuality {
        return try {
            val prefs = dataStore.data.first()
            val qualityStr = prefs[STREAMING_QUALITY]
            qualityStr?.let { AudioQuality.valueOf(it) } ?: DEFAULT_STREAMING_QUALITY
        } catch (e: Exception) {
            Log.e(TAG, "Error getting streaming quality", e)
            DEFAULT_STREAMING_QUALITY
        }
    }

    override suspend fun setStreamingQuality(quality: AudioQuality): Result<Unit> {
        return try {
            dataStore.edit { prefs ->
                prefs[STREAMING_QUALITY] = quality.name
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting streaming quality", e)
            Result.failure(e)
        }
    }

    override fun observeStreamingQuality(): Flow<AudioQuality> {
        return dataStore.data
            .catch { e ->
                if (e is IOException) {
                    Log.e(TAG, "Error reading streaming quality", e)
                    emit(emptyPreferences())
                } else {
                    throw e
                }
            }
            .map { prefs ->
                val qualityStr = prefs[STREAMING_QUALITY]
                qualityStr?.let { AudioQuality.valueOf(it) } ?: DEFAULT_STREAMING_QUALITY
            }
    }

    override suspend fun getDownloadQuality(): AudioQuality {
        return try {
            val prefs = dataStore.data.first()
            val qualityStr = prefs[DOWNLOAD_QUALITY]
            qualityStr?.let { AudioQuality.valueOf(it) } ?: DEFAULT_DOWNLOAD_QUALITY
        } catch (e: Exception) {
            Log.e(TAG, "Error getting download quality", e)
            DEFAULT_DOWNLOAD_QUALITY
        }
    }

    override suspend fun setDownloadQuality(quality: AudioQuality): Result<Unit> {
        return try {
            dataStore.edit { prefs ->
                prefs[DOWNLOAD_QUALITY] = quality.name
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting download quality", e)
            Result.failure(e)
        }
    }

    override fun observeDownloadQuality(): Flow<AudioQuality> {
        return dataStore.data
            .catch { e ->
                if (e is IOException) {
                    Log.e(TAG, "Error reading download quality", e)
                    emit(emptyPreferences())
                } else {
                    throw e
                }
            }
            .map { prefs ->
                val qualityStr = prefs[DOWNLOAD_QUALITY]
                qualityStr?.let { AudioQuality.valueOf(it) } ?: DEFAULT_DOWNLOAD_QUALITY
            }
    }

    override suspend fun getStreamingMode(): StreamingMode {
        return try {
            val prefs = dataStore.data.first()
            val modeStr = prefs[STREAMING_MODE]
            modeStr?.let { StreamingMode.valueOf(it) } ?: DEFAULT_STREAMING_MODE
        } catch (e: Exception) {
            Log.e(TAG, "Error getting streaming mode", e)
            DEFAULT_STREAMING_MODE
        }
    }

    override suspend fun setStreamingMode(mode: StreamingMode): Result<Unit> {
        return try {
            dataStore.edit { prefs ->
                prefs[STREAMING_MODE] = mode.name
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting streaming mode", e)
            Result.failure(e)
        }
    }

    override suspend fun getAutoDownloadOnWiFi(): Boolean {
        return try {
            val prefs = dataStore.data.first()
            prefs[AUTO_DOWNLOAD_WIFI] ?: DEFAULT_AUTO_DOWNLOAD_WIFI
        } catch (e: Exception) {
            Log.e(TAG, "Error getting auto download setting", e)
            DEFAULT_AUTO_DOWNLOAD_WIFI
        }
    }

    override suspend fun setAutoDownloadOnWiFi(enabled: Boolean): Result<Unit> {
        return try {
            dataStore.edit { prefs ->
                prefs[AUTO_DOWNLOAD_WIFI] = enabled
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting auto download", e)
            Result.failure(e)
        }
    }

    override suspend fun getAutoSyncEnabled(): Boolean {
        return try {
            val prefs = dataStore.data.first()
            prefs[AUTO_SYNC_ENABLED] ?: DEFAULT_AUTO_SYNC_ENABLED
        } catch (e: Exception) {
            Log.e(TAG, "Error getting auto sync setting", e)
            DEFAULT_AUTO_SYNC_ENABLED
        }
    }

    override suspend fun setAutoSyncEnabled(enabled: Boolean): Result<Unit> {
        return try {
            dataStore.edit { prefs ->
                prefs[AUTO_SYNC_ENABLED] = enabled
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting auto sync", e)
            Result.failure(e)
        }
    }

    override suspend fun getMaxStorageForDownloads(): Long {
        return try {
            val prefs = dataStore.data.first()
            prefs[MAX_STORAGE_DOWNLOADS] ?: DEFAULT_MAX_STORAGE
        } catch (e: Exception) {
            Log.e(TAG, "Error getting max storage", e)
            DEFAULT_MAX_STORAGE
        }
    }

    override suspend fun setMaxStorageForDownloads(bytes: Long): Result<Unit> {
        return try {
            dataStore.edit { prefs ->
                prefs[MAX_STORAGE_DOWNLOADS] = bytes
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting max storage", e)
            Result.failure(e)
        }
    }
}
