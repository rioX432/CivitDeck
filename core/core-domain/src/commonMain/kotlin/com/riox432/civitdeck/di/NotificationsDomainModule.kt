package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.usecase.CheckModelUpdatesUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNotificationsEnabledUseCase
import com.riox432.civitdeck.domain.usecase.ObservePollingIntervalUseCase
import com.riox432.civitdeck.domain.usecase.SetNotificationsEnabledUseCase
import com.riox432.civitdeck.domain.usecase.SetPollingIntervalUseCase
import org.koin.dsl.module

val notificationsDomainModule = module {
    factory { CheckModelUpdatesUseCase(get(), get(), get()) }
    factory { ObserveNotificationsEnabledUseCase(get()) }
    factory { SetNotificationsEnabledUseCase(get()) }
    factory { ObservePollingIntervalUseCase(get()) }
    factory { SetPollingIntervalUseCase(get()) }
}
