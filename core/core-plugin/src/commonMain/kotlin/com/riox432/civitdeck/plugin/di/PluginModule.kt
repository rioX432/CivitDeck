package com.riox432.civitdeck.plugin.di

import com.riox432.civitdeck.plugin.InMemoryPluginRegistry
import com.riox432.civitdeck.plugin.PluginRegistry
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val pluginModule = module {
    singleOf(::InMemoryPluginRegistry) { bind<PluginRegistry>() }
}
