package com.riox432.civitdeck.domain.repository

import kotlinx.coroutines.flow.Flow

interface NetworkRepository {
    val isOnline: Flow<Boolean>
}
