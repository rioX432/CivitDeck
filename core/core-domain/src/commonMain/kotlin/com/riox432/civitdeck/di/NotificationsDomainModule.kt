package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.usecase.CheckAndStoreModelUpdatesUseCase
import com.riox432.civitdeck.domain.usecase.CheckModelUpdatesUseCase
import com.riox432.civitdeck.domain.usecase.GetModelUpdateNotificationsUseCase
import com.riox432.civitdeck.domain.usecase.GetUnreadNotificationCountUseCase
import com.riox432.civitdeck.domain.usecase.MarkAllNotificationsReadUseCase
import com.riox432.civitdeck.domain.usecase.MarkNotificationReadUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNotificationsEnabledUseCase
import com.riox432.civitdeck.domain.usecase.ObservePollingIntervalUseCase
import com.riox432.civitdeck.domain.usecase.SetNotificationsEnabledUseCase
import com.riox432.civitdeck.domain.usecase.SetPollingIntervalUseCase
import org.koin.dsl.module

val notificationsDomainModule = module {
    factory { CheckModelUpdatesUseCase(get(), get(), get(), get()) }
    factory { ObserveNotificationsEnabledUseCase(get()) }
    factory { SetNotificationsEnabledUseCase(get()) }
    factory { ObservePollingIntervalUseCase(get()) }
    factory { SetPollingIntervalUseCase(get()) }
    factory { GetModelUpdateNotificationsUseCase(get()) }
    factory { GetUnreadNotificationCountUseCase(get()) }
    factory { MarkNotificationReadUseCase(get()) }
    factory { MarkAllNotificationsReadUseCase(get()) }
    factory { CheckAndStoreModelUpdatesUseCase(get(), get()) }
}
