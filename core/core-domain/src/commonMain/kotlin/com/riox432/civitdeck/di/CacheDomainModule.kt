package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.usecase.ClearCacheUseCase
import com.riox432.civitdeck.domain.usecase.EvictCacheUseCase
import com.riox432.civitdeck.domain.usecase.GetCacheInfoUseCase
import com.riox432.civitdeck.domain.usecase.ObserveCacheSizeLimitUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNetworkStatusUseCase
import com.riox432.civitdeck.domain.usecase.ObserveOfflineCacheEnabledUseCase
import com.riox432.civitdeck.domain.usecase.SetCacheSizeLimitUseCase
import com.riox432.civitdeck.domain.usecase.SetOfflineCacheEnabledUseCase
import org.koin.dsl.module

val cacheDomainModule = module {
    factory { ClearCacheUseCase(get()) }
    factory { ObserveNetworkStatusUseCase(get()) }
    factory { GetCacheInfoUseCase(get()) }
    factory { EvictCacheUseCase(get()) }
    factory { ObserveOfflineCacheEnabledUseCase(get()) }
    factory { SetOfflineCacheEnabledUseCase(get()) }
    factory { ObserveCacheSizeLimitUseCase(get()) }
    factory { SetCacheSizeLimitUseCase(get()) }
}
