package com.riox432.civitdeck.data.local

import kotlinx.coroutines.flow.Flow

/**
 * Platform-specific network connectivity monitor.
 * Emits true when network is available, false when offline.
 */
interface NetworkMonitor {
    val isOnline: Flow<Boolean>
}
