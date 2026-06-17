package com.riox432.civitdeck.feature.comfyui.di

import com.riox432.civitdeck.data.image.ImageSaver
import com.riox432.civitdeck.data.image.ImageSaverImpl
import com.riox432.civitdeck.feature.comfyui.data.encoder.MaskPngEncoder
import com.riox432.civitdeck.feature.comfyui.data.encoder.MaskPngEncoderImpl
import com.riox432.civitdeck.feature.comfyui.data.repository.LocalIpProvider
import com.riox432.civitdeck.feature.comfyui.data.repository.LocalIpProviderImpl
import org.koin.core.module.Module
import org.koin.dsl.module

actual val comfyuiPlatformModule: Module = module {
    single<LocalIpProvider> { LocalIpProviderImpl() }
    factory<ImageSaver> { ImageSaverImpl(get()) }
    single<MaskPngEncoder> { MaskPngEncoderImpl() }
}
