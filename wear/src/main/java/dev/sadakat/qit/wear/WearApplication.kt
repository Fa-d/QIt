package dev.sadakat.qit.wear

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.HiltAndroidApp
import dev.sadakat.qit.shared.constants.WearPaths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltAndroidApp
class WearApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        announceVersionToPhone()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    /**
     * Announces the watch app version to the phone app
     */
    private fun announceVersionToPhone() {
        applicationScope.launch {
            try {
                val versionName = packageManager.getPackageInfo(packageName, 0).versionName ?: "unknown"
                val messageClient = Wearable.getMessageClient(this@WearApplication)
                val nodeClient = Wearable.getNodeClient(this@WearApplication)

                val nodes = nodeClient.connectedNodes.await()
                for (node in nodes) {
                    messageClient.sendMessage(
                        node.id,
                        WearPaths.WATCH_VERSION_ANNOUNCEMENT,
                        versionName.toByteArray()
                    ).await()
                    Log.d(TAG, "Announced version $versionName to phone node ${node.id}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to announce version to phone", e)
            }
        }
    }

    companion object {
        private const val TAG = "WearApplication"
    }
}
