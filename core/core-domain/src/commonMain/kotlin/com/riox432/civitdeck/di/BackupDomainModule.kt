package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.usecase.CreateBackupUseCase
import com.riox432.civitdeck.domain.usecase.ParseBackupUseCase
import com.riox432.civitdeck.domain.usecase.RestoreBackupUseCase
import org.koin.dsl.module

val backupDomainModule = module {
    factory { CreateBackupUseCase(get()) }
    factory { RestoreBackupUseCase(get()) }
    factory { ParseBackupUseCase(get()) }
}
