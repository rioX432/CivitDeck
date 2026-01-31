package com.omooooori.civitdeck.di

import com.omooooori.civitdeck.data.local.getDatabaseBuilder
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { getDatabaseBuilder() }
}
