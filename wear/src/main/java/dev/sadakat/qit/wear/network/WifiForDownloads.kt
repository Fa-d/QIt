package dev.sadakat.qit.wear.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.sadakat.qit.core.domain.model.Track
import dev.sadakat.qit.core.domain.repository.SurahDownloadState
import dev.sadakat.qit.core.domain.repository.SurahDownloads
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** The Android calls [WifiRequestStateMachine] triggers; kept behind an interface for tests. */
internal interface NetworkOps {
    fun acquireWifi()

    fun releaseWifi()
}

/**
 * Turns download activity into Wi-Fi requests: a rising edge (any surah starts downloading) asks
 * for a network once, a falling edge (none downloading any more) releases it, and everything in
 * between (progress updates, other surahs' states) leaves the request alone.
 */
internal class WifiRequestStateMachine(internal val networkOps: NetworkOps) {

    private var downloadsActive = false

    fun onStates(states: Map<Int, Map<Track, SurahDownloadState>>) {
        onDownloadsActive(states.values.any { tracks -> tracks.values.any { it is SurahDownloadState.Downloading } })
    }

    private fun onDownloadsActive(active: Boolean) {
        if (active == downloadsActive) return
        downloadsActive = active
        if (active) networkOps.acquireWifi() else networkOps.releaseWifi()
    }
}

/**
 * While surah downloads are running, asks for a Wi-Fi network and binds the process to it so
 * downloads do not crawl over the phone's Bluetooth proxy. Releases it when downloads finish.
 */
@Singleton
class WifiForDownloads @Inject constructor(
    @ApplicationContext context: Context,
    private val surahDownloads: SurahDownloads
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    private val machine = WifiRequestStateMachine(ConnectivityNetworkOps())

    /** Binds to every Wi-Fi network that arrives while we hold the request. */
    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            bindProcessToNetwork(network)
        }

        override fun onLost(network: Network) {
            bindProcessToNetwork(null)
        }
    }

    /** Starts watching download state. Called once from [dev.sadakat.qit.wear.WearApplication]. */
    fun start() {
        scope.launch {
            try {
                surahDownloads.states.collect(machine::onStates)
            } catch (e: Throwable) {
                // Best-effort optimization: if download state is unavailable, downloads simply run
                // on the default network. Never take app startup down with us.
                Log.w(TAG, "Not watching download state; Wi-Fi binding disabled", e)
            }
        }
    }

    /** The real [NetworkOps]: request/unregister a Wi-Fi network via the system connectivity service. */
    private inner class ConnectivityNetworkOps : NetworkOps {

        override fun acquireWifi() {
            val manager = connectivityManager ?: return
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            try {
                manager.requestNetwork(request, callback)
            } catch (e: SecurityException) {
                Log.w(TAG, "Not allowed to request a Wi-Fi network", e)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Invalid Wi-Fi network request", e)
            }
        }

        override fun releaseWifi() {
            bindProcessToNetwork(null)
            val manager = connectivityManager ?: return
            try {
                manager.unregisterNetworkCallback(callback)
            } catch (e: IllegalArgumentException) {
                // Nothing registered (e.g. request failed): nothing to do.
                Log.i(TAG, "No Wi-Fi request to release (${e.message})")
            }
        }
    }

    private fun bindProcessToNetwork(network: Network?) {
        try {
            connectivityManager?.bindProcessToNetwork(network)
        } catch (e: SecurityException) {
            Log.w(TAG, "Not allowed to bind the process to the network", e)
        }
    }
}

private const val TAG = "WifiForDownloads"
