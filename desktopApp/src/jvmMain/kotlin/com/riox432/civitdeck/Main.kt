package com.riox432.civitdeck

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
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
import java.util.prefs.Preferences

fun main() {
    initKoin {
        modules(desktopModule)
    }
    setupCoilImageLoader()

    application {
        val windowPrefs = remember { WindowPreferences.load() }
        val windowState by remember {
            mutableStateOf(
                WindowState(
                    size = DpSize(windowPrefs.width.dp, windowPrefs.height.dp),
                    position = if (windowPrefs.x >= 0 && windowPrefs.y >= 0) {
                        WindowPosition(windowPrefs.x.dp, windowPrefs.y.dp)
                    } else {
                        WindowPosition.PlatformDefault
                    },
                ),
            )
        }

        Window(
            onCloseRequest = {
                WindowPreferences.save(windowState)
                exitApplication()
            },
            title = "CivitDeck",
            state = windowState,
        ) {
            window.minimumSize = java.awt.Dimension(MIN_WINDOW_WIDTH, MIN_WINDOW_HEIGHT)

            DisposableEffect(Unit) {
                onDispose { WindowPreferences.save(windowState) }
            }

            DesktopApp()
        }
    }
}

private data class WindowPreferences(
    val width: Int,
    val height: Int,
    val x: Int,
    val y: Int,
) {
    companion object {
        private const val PREFS_NODE = "com.riox432.civitdeck.window"
        private const val KEY_WIDTH = "width"
        private const val KEY_HEIGHT = "height"
        private const val KEY_X = "x"
        private const val KEY_Y = "y"

        fun load(): WindowPreferences {
            val prefs = Preferences.userRoot().node(PREFS_NODE)
            return WindowPreferences(
                width = prefs.getInt(KEY_WIDTH, DEFAULT_WIDTH),
                height = prefs.getInt(KEY_HEIGHT, DEFAULT_HEIGHT),
                x = prefs.getInt(KEY_X, -1),
                y = prefs.getInt(KEY_Y, -1),
            )
        }

        fun save(state: WindowState) {
            val prefs = Preferences.userRoot().node(PREFS_NODE)
            prefs.putInt(KEY_WIDTH, state.size.width.value.toInt())
            prefs.putInt(KEY_HEIGHT, state.size.height.value.toInt())
            prefs.putInt(KEY_X, state.position.x.value.toInt())
            prefs.putInt(KEY_Y, state.position.y.value.toInt())
            prefs.flush()
        }
    }
}

private const val DEFAULT_WIDTH = 1200
private const val DEFAULT_HEIGHT = 800
private const val MIN_WINDOW_WIDTH = 800
private const val MIN_WINDOW_HEIGHT = 600

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
                    .maxSizeBytes(DISK_CACHE_SIZE)
                    .build()
            }
            .build()
    }
}

private const val DISK_CACHE_SIZE = 256L * 1024 * 1024 // 256 MB
