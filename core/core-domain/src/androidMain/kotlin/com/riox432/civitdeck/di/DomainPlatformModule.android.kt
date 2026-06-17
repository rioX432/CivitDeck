package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.ml.ImageEmbeddingModel
import com.riox432.civitdeck.domain.ml.ImageEmbeddingModelImpl
import com.riox432.civitdeck.domain.ml.TextEmbeddingModel
import com.riox432.civitdeck.domain.ml.TextEmbeddingModelImpl
import com.riox432.civitdeck.domain.service.AppLifecycleTracker
import com.riox432.civitdeck.domain.service.AppLifecycleTrackerImpl
import com.riox432.civitdeck.domain.service.BackgroundMonitorStarter
import com.riox432.civitdeck.domain.service.BackgroundMonitorStarterImpl
import com.riox432.civitdeck.domain.service.GenerationNotificationService
import com.riox432.civitdeck.domain.service.GenerationNotificationServiceImpl
import org.koin.core.module.Module
import org.koin.dsl.module

actual val domainPlatformModule: Module = module {
    single<ImageEmbeddingModel> { ImageEmbeddingModelImpl(get()) }
    single<TextEmbeddingModel> { TextEmbeddingModelImpl(get()) }
    single<GenerationNotificationService> { GenerationNotificationServiceImpl(get()) }
    single<AppLifecycleTracker> { AppLifecycleTrackerImpl() }
    single<BackgroundMonitorStarter> { BackgroundMonitorStarterImpl(get()) }
}
