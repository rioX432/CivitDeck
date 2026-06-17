package com.riox432.civitdeck.domain.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

// Kotlin/Native does not expose `Dispatchers.IO`; `Default` is the appropriate
// background dispatcher. Room KMP manages its own write threading, so the short
// DB write does not require a dedicated IO pool here.
actual val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
