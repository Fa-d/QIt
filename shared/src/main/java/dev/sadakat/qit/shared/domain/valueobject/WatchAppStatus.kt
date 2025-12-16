package dev.sadakat.qit.shared.domain.valueobject

import kotlinx.serialization.Serializable

/**
 * Comprehensive watch app status information
 */
@Serializable
data class WatchAppStatus(
    val isInstalled: Boolean,
    val isConnected: Boolean,
    val watchNodes: List<WatchNode>,
    val connectionDiagnostics: ConnectionDiagnostics,
    val appVersion: String? = null
) {
    companion object {
        fun notInstalled() = WatchAppStatus(
            isInstalled = false,
            isConnected = false,
            watchNodes = emptyList(),
            connectionDiagnostics = ConnectionDiagnostics.unavailable(),
            appVersion = null
        )

        fun installed(
            nodes: List<WatchNode>,
            diagnostics: ConnectionDiagnostics,
            version: String? = null
        ) = WatchAppStatus(
            isInstalled = nodes.isNotEmpty(),
            isConnected = nodes.any { it.isNearby },
            watchNodes = nodes,
            connectionDiagnostics = diagnostics,
            appVersion = version
        )
    }
}

@Serializable
data class WatchNode(
    val nodeId: String,
    val displayName: String,
    val isNearby: Boolean
)

@Serializable
data class ConnectionDiagnostics(
    val bluetoothEnabled: Boolean,
    val hasCapability: Boolean,
    val nodeCount: Int,
    val lastCheckTimestamp: Long,
    val errorMessage: String? = null
) {
    companion object {
        fun unavailable() = ConnectionDiagnostics(
            bluetoothEnabled = false,
            hasCapability = false,
            nodeCount = 0,
            lastCheckTimestamp = System.currentTimeMillis(),
            errorMessage = "Unable to check connection status"
        )
    }
}
