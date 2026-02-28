package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.usecase.ObserveDefaultSortOrderUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDefaultTimePeriodUseCase
import com.riox432.civitdeck.domain.usecase.ObserveGridColumnsUseCase
import com.riox432.civitdeck.domain.usecase.ObservePowerUserModeUseCase
import com.riox432.civitdeck.domain.usecase.SetDefaultSortOrderUseCase
import com.riox432.civitdeck.domain.usecase.SetDefaultTimePeriodUseCase
import com.riox432.civitdeck.domain.usecase.SetGridColumnsUseCase
import com.riox432.civitdeck.domain.usecase.SetPowerUserModeUseCase
import org.koin.dsl.module

val settingsDomainModule = module {
    factory { ObserveDefaultSortOrderUseCase(get()) }
    factory { SetDefaultSortOrderUseCase(get()) }
    factory { ObserveDefaultTimePeriodUseCase(get()) }
    factory { SetDefaultTimePeriodUseCase(get()) }
    factory { ObserveGridColumnsUseCase(get()) }
    factory { SetGridColumnsUseCase(get()) }
    factory { ObservePowerUserModeUseCase(get()) }
    factory { SetPowerUserModeUseCase(get()) }
}
