package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.usecase.AddModelDirectoryUseCase
import com.riox432.civitdeck.domain.usecase.ObserveLocalModelFilesUseCase
import com.riox432.civitdeck.domain.usecase.ObserveModelDirectoriesUseCase
import com.riox432.civitdeck.domain.usecase.ObserveOwnedModelHashesUseCase
import com.riox432.civitdeck.domain.usecase.RemoveModelDirectoryUseCase
import com.riox432.civitdeck.domain.usecase.ScanModelDirectoriesUseCase
import com.riox432.civitdeck.domain.usecase.VerifyModelHashUseCase
import org.koin.dsl.module

val localModelsDomainModule = module {
    factory { ObserveModelDirectoriesUseCase(get()) }
    factory { AddModelDirectoryUseCase(get()) }
    factory { RemoveModelDirectoryUseCase(get()) }
    factory { ObserveLocalModelFilesUseCase(get()) }
    factory { ScanModelDirectoriesUseCase(get()) }
    factory { VerifyModelHashUseCase(get()) }
    factory { ObserveOwnedModelHashesUseCase(get()) }
}
