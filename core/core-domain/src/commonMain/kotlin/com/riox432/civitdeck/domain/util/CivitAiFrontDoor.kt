package com.riox432.civitdeck.domain.util

import com.riox432.civitdeck.domain.model.FrontDoorMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Shared source of truth for the CivitAI web/share link host.
 *
 * Exposes the currently selected front-door host (civitai.com / civitai.red)
 * as a [StateFlow]. Web-facing URL builders (model page, share, "open in
 * browser") read from here so the chosen front door is reflected everywhere.
 *
 * IMPORTANT: This affects web links ONLY. The REST API and download URLs are
 * unaffected and always stay on civitai.com.
 *
 * Registered as a Koin `single`; [start] is invoked once with the persisted
 * [FrontDoorMode] flow and an app-lifetime [CoroutineScope] to keep [webHost]
 * in sync with the user's setting.
 */
class CivitAiFrontDoor {
    private val _webHost = MutableStateFlow(FrontDoorMode.Sfw.webHost)

    /** Current web/share link host (e.g. https://civitai.com). */
    val webHost: StateFlow<String> = _webHost.asStateFlow()

    /**
     * Begins observing the persisted [FrontDoorMode] and updating [webHost].
     * Idempotent collection is the caller's responsibility — call once at app
     * startup from a long-lived scope.
     */
    fun start(modeFlow: Flow<FrontDoorMode>, scope: CoroutineScope) {
        scope.launch {
            modeFlow.collect { mode -> _webHost.value = mode.webHost }
        }
    }
}
