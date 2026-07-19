package com.riox432.civitdeck.di

import org.koin.core.module.Module

/**
 * Platform-specific Koin bindings for the on-device embedding models.
 *
 * Lives in `:core:core-ml` (not `:core:core-domain`) so that Android build flavors
 * without ML — e.g. the F-Droid flavor — can drop the entire module, and with it the
 * ONNX Runtime dependency and native libraries, at the dependency level.
 */
expect val mlPlatformModule: Module
