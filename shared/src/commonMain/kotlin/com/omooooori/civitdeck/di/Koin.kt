package com.omooooori.civitdeck.di

import org.koin.core.context.startKoin
import org.koin.dsl.module

val sharedModule = module {
    // Add shared dependencies here
}

fun initKoin() {
    startKoin {
        modules(sharedModule)
    }
}
