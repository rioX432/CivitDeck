package com.riox432.civitdeck

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import com.riox432.civitdeck.di.desktopModule
import com.riox432.civitdeck.di.initKoin
import okio.Path.Companion.toPath

fun main() {
    initKoin {
        modules(desktopModule)
    }
    setupCoilImageLoader()

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "CivitDeck",
            state = WindowState(size = DpSize(1200.dp, 800.dp)),
        ) {
            window.minimumSize = java.awt.Dimension(800, 600)
            DesktopApp()
        }
    }
}

private fun setupCoilImageLoader() {
    val cacheDir = System.getProperty("user.home") + "/.civitdeck/image_cache"
    SingletonImageLoader.setSafe {
        ImageLoader.Builder(PlatformContext.INSTANCE)
            .crossfade(true)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(PlatformContext.INSTANCE, percent = 0.15)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.toPath())
                    .maxSizeBytes(256L * 1024 * 1024) // 256 MB
                    .build()
            }
            .build()
    }
}
