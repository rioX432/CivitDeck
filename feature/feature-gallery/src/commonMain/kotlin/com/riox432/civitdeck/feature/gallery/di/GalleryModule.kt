package com.riox432.civitdeck.feature.gallery.di

import com.riox432.civitdeck.domain.repository.ImageRepository
import com.riox432.civitdeck.feature.gallery.data.repository.ImageRepositoryImpl
import com.riox432.civitdeck.feature.gallery.domain.usecase.GetImagesUseCase
import com.riox432.civitdeck.feature.gallery.presentation.ImageGalleryViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val galleryModule = module {
    single<ImageRepository> { ImageRepositoryImpl(get(), get(), get()) }
    factory { GetImagesUseCase(get()) }
    viewModel { params -> ImageGalleryViewModel(params.get(), get(), get(), get(), get(), get()) }
}
