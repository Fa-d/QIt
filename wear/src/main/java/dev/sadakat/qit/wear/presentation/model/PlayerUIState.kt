package dev.sadakat.qit.wear.presentation.model

/**
 * Sealed classes for connection state and streaming mode.
 */
sealed class ConnectionState {
    object Connected : ConnectionState()
    object Connecting : ConnectionState()
    data class Disconnected(val reason: String) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

enum class StreamingMode {
    Streaming,
    Offline,
    Unknown
}