package com.riox432.civitdeck.di

import org.koin.core.module.Module

/**
 * Platform-specific Koin bindings for core-database services that have an
 * `interface` in commonMain and a platform `actual` implementation class
 * (e.g. [com.riox432.civitdeck.data.scanner.FileScanner]).
 */
expect val databasePlatformModule: Module
