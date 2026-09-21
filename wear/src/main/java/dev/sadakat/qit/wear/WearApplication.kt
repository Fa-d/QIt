package dev.sadakat.qit.wear

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.HiltAndroidApp
import dev.sadakat.qit.shared.constants.WearPaths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@HiltAndroidApp
class WearApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Node ids this app instance has already announced its version to.
     * Prevents duplicate announcements when the phone becomes reachable
     * repeatedly. Concurrent set: mutated from both the onCreate launch and
     * the capability listener's IO coroutine.
     */
    private val announcedNodeIds: MutableSet<String> = ConcurrentHashMap.newKeySet()

    override fun onCreate() {
        super.onCreate()
        announceVersionToPhone()
        registerPhoneReachabilityListener()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    /**
     * Announces the watch app version to the phone app whenever it becomes
     * reachable.
     *
     * The onCreate-only announcement is lost when the phone is not yet
     * reachable at process start; the capability listener below re-announces
     * once the phone appears (each node is announced to only once).
     */
    private fun registerPhoneReachabilityListener() {
        try {
            val capabilityClient = Wearable.getCapabilityClient(this)
            capabilityClient.addListener(
                { capabilityInfo ->
                    val reachableNodeIds = capabilityInfo.nodes
                        .filter { it.isNearby }
                        .map { it.id }
                    if (reachableNodeIds.isNotEmpty()) {
                        applicationScope.launch { announceVersionToNodes(reachableNodeIds) }
                    }
                },
                WearPaths.CAPABILITY_PHONE_APP
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register phone reachability listener", e)
        }
    }

    /**
     * Announces the watch app version to all currently connected phones.
     */
    private fun announceVersionToPhone() {
        applicationScope.launch {
            try {
                val nodeClient = Wearable.getNodeClient(this@WearApplication)
                val nodes = nodeClient.connectedNodes.await()
                announceVersionToNodes(nodes.map { it.id })
            } catch (e: Exception) {
                Log.e(TAG, "Failed to announce version to phone", e)
            }
        }
    }

    private suspend fun announceVersionToNodes(nodeIds: List<String>) {
        try {
            val versionName = packageManager.getPackageInfo(packageName, 0).versionName ?: "unknown"
            val messageClient = Wearable.getMessageClient(this@WearApplication)
            for (nodeId in nodeIds) {
                // Skip nodes that were already announced to (e.g. the phone
                // toggled unreachable/reachable).
                if (!announcedNodeIds.add(nodeId)) continue
                messageClient.sendMessage(
                    nodeId,
                    WearPaths.WATCH_VERSION_ANNOUNCEMENT,
                    versionName.toByteArray()
                ).await()
                Log.d(TAG, "Announced version $versionName to phone node $nodeId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to announce version to phone", e)
        }
    }

    companion object {
        private const val TAG = "WearApplication"
    }
}
