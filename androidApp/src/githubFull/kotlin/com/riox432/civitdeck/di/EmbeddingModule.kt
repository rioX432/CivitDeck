package com.riox432.civitdeck.di

import org.koin.core.module.Module

/**
 * githubFull binds the real on-device embedding models from `:core:core-ml`
 * (ONNX Runtime on Android). See the fdroid source set for the ML-free counterpart.
 */
val embeddingModule: Module = mlPlatformModule
