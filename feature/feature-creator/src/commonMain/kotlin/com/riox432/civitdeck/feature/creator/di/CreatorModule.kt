package com.riox432.civitdeck.feature.creator.di

import com.riox432.civitdeck.domain.repository.CreatorRepository
import com.riox432.civitdeck.feature.creator.data.repository.CreatorRepositoryImpl
import com.riox432.civitdeck.feature.creator.domain.usecase.GetCreatorModelsUseCase
import com.riox432.civitdeck.feature.creator.presentation.CreatorProfileViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val creatorModule = module {
    single<CreatorRepository> { CreatorRepositoryImpl(get()) }
    factory { GetCreatorModelsUseCase(get()) }
    viewModel { params -> CreatorProfileViewModel(params.get(), get()) }
}
