package com.riox432.civitdeck.domain.service

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner

actual class AppLifecycleTracker {
    actual val isInForeground: Boolean
        get() = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(
            Lifecycle.State.RESUMED,
        )
}
