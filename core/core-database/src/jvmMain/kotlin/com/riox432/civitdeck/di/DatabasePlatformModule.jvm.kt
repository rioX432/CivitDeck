package com.riox432.civitdeck.di

import com.riox432.civitdeck.data.scanner.FileScanner
import com.riox432.civitdeck.data.scanner.FileScannerImpl
import org.koin.core.module.Module
import org.koin.dsl.module

actual val databasePlatformModule: Module = module {
    factory<FileScanner> { FileScannerImpl() }
}
