package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.usecase.CancelDownloadUseCase
import com.riox432.civitdeck.domain.usecase.ClearCompletedDownloadsUseCase
import com.riox432.civitdeck.domain.usecase.DeleteDownloadUseCase
import com.riox432.civitdeck.domain.usecase.EnqueueDownloadUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDownloadsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveModelDownloadsUseCase
import com.riox432.civitdeck.domain.usecase.PauseDownloadUseCase
import com.riox432.civitdeck.domain.usecase.ResumeDownloadUseCase
import com.riox432.civitdeck.domain.usecase.VerifyDownloadHashUseCase
import org.koin.dsl.module

val downloadDomainModule = module {
    factory { EnqueueDownloadUseCase(get()) }
    factory { ObserveDownloadsUseCase(get()) }
    factory { ObserveModelDownloadsUseCase(get()) }
    factory { CancelDownloadUseCase(get()) }
    factory { DeleteDownloadUseCase(get()) }
    factory { ClearCompletedDownloadsUseCase(get()) }
    factory { PauseDownloadUseCase(get()) }
    factory { ResumeDownloadUseCase(get()) }
    factory { VerifyDownloadHashUseCase(get()) }
}
