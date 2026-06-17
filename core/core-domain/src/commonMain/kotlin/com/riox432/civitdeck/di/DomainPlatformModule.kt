package com.riox432.civitdeck.di

import org.koin.core.module.Module

/**
 * Platform-specific Koin bindings for core-domain services that have an
 * `interface` in commonMain and a platform `actual` implementation class
 * (ImageEmbeddingModel, TextEmbeddingModel, GenerationNotificationService,
 * BackgroundMonitorStarter, AppLifecycleTracker).
 */
expect val domainPlatformModule: Module
