package com.riox432.civitdeck.di

import org.koin.core.module.Module

/**
 * Platform-specific Koin bindings for core-domain services that have an
 * `interface` in commonMain and a platform `actual` implementation class
 * (GenerationNotificationService, BackgroundMonitorStarter, AppLifecycleTracker).
 *
 * The on-device embedding models (ImageEmbeddingModel/TextEmbeddingModel) are bound
 * separately by `mlPlatformModule` in `:core:core-ml` so ML-free build flavors can
 * drop that binding — and ONNX Runtime — at the dependency level.
 */
expect val domainPlatformModule: Module
