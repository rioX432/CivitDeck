package com.riox432.civitdeck.feature.comfyui.di

import org.koin.core.module.Module

/**
 * Platform-specific Koin bindings for feature-comfyui services that have an
 * `interface` in commonMain and a platform `actual` implementation class
 * (LocalIpProvider, ImageSaver, MaskPngEncoder).
 */
expect val comfyuiPlatformModule: Module
