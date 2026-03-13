package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.usecase.ActivatePluginUseCase
import com.riox432.civitdeck.domain.usecase.DeactivatePluginUseCase
import com.riox432.civitdeck.domain.usecase.GetPluginConfigUseCase
import com.riox432.civitdeck.domain.usecase.InstallPluginUseCase
import com.riox432.civitdeck.domain.usecase.ObserveInstalledPluginsUseCase
import com.riox432.civitdeck.domain.usecase.UninstallPluginUseCase
import com.riox432.civitdeck.domain.usecase.UpdatePluginConfigUseCase
import org.koin.dsl.module

val pluginDomainModule = module {
    factory { InstallPluginUseCase(get()) }
    factory { UninstallPluginUseCase(get()) }
    factory { ActivatePluginUseCase(get()) }
    factory { DeactivatePluginUseCase(get()) }
    factory { ObserveInstalledPluginsUseCase(get()) }
    factory { GetPluginConfigUseCase(get()) }
    factory { UpdatePluginConfigUseCase(get()) }
}
