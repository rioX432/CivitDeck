package com.riox432.civitdeck.di

import org.koin.core.module.Module
import org.koin.dsl.module

// Android supplies the embedding binding per build flavor (see androidApp's
// fdroid / githubFull source sets), so the shared composition root stays empty here.
actual val embeddingPlatformModule: Module = module { }
