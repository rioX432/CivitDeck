package com.riox432.civitdeck.plugin.di

import com.riox432.civitdeck.plugin.InMemoryPluginRegistry
import com.riox432.civitdeck.plugin.PluginRegistry
import com.riox432.civitdeck.plugin.usecase.ActivateThemePluginUseCase
import com.riox432.civitdeck.plugin.usecase.GetActiveThemeUseCase
import com.riox432.civitdeck.plugin.usecase.ImportThemeUseCase
import com.riox432.civitdeck.plugin.usecase.ObserveThemePluginsUseCase
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val pluginModule = module {
    singleOf(::InMemoryPluginRegistry) { bind<PluginRegistry>() }

    // Theme plugin use cases
    factory { ImportThemeUseCase(get(), get()) }
    factory { GetActiveThemeUseCase(get()) }
    factory { ObserveThemePluginsUseCase(get()) }
    factory { ActivateThemePluginUseCase(get(), get()) }
}
