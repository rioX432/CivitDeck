package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.usecase.EnrichModelImagesUseCase
import org.koin.dsl.module

val galleryDomainModule = module {
    factory { EnrichModelImagesUseCase(get()) }
}
