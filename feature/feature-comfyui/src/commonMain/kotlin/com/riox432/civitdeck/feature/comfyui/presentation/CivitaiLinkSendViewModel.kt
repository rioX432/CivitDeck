package com.riox432.civitdeck.feature.comfyui.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.CivitaiLinkResource
import com.riox432.civitdeck.domain.model.CivitaiLinkStatus
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ObserveCivitaiLinkStatusUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.SendResourceToPCUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CivitaiLinkSendViewModel(
    observeStatus: ObserveCivitaiLinkStatusUseCase,
    private val sendResource: SendResourceToPCUseCase,
) : ViewModel() {

    val status: StateFlow<CivitaiLinkStatus> = observeStatus()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CivitaiLinkStatus.Disconnected)

    fun sendToPC(resource: CivitaiLinkResource) {
        viewModelScope.launch { sendResource(resource) }
    }
}
