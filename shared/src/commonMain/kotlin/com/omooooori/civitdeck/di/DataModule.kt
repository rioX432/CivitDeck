package com.omooooori.civitdeck.di

import com.omooooori.civitdeck.data.api.CivitAiApi
import com.omooooori.civitdeck.data.api.createHttpClient
import com.omooooori.civitdeck.data.repository.CreatorRepositoryImpl
import com.omooooori.civitdeck.data.repository.ImageRepositoryImpl
import com.omooooori.civitdeck.data.repository.ModelRepositoryImpl
import com.omooooori.civitdeck.data.repository.TagRepositoryImpl
import com.omooooori.civitdeck.domain.repository.CreatorRepository
import com.omooooori.civitdeck.domain.repository.ImageRepository
import com.omooooori.civitdeck.domain.repository.ModelRepository
import com.omooooori.civitdeck.domain.repository.TagRepository
import org.koin.dsl.module

val dataModule = module {
    single { createHttpClient() }
    single { CivitAiApi(get()) }

    single<ModelRepository> { ModelRepositoryImpl(get()) }
    single<ImageRepository> { ImageRepositoryImpl(get()) }
    single<CreatorRepository> { CreatorRepositoryImpl(get()) }
    single<TagRepository> { TagRepositoryImpl(get()) }
}
