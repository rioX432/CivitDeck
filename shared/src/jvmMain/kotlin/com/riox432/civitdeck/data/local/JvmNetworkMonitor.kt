package com.riox432.civitdeck.data.local

import com.riox432.civitdeck.domain.repository.NetworkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class JvmNetworkMonitor : NetworkRepository {

    private val _isOnline = MutableStateFlow(true)
    override val isOnline: Flow<Boolean> = _isOnline
}
