package com.riox432.civitdeck.testing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

/**
 * Drives a [ViewModel] through its full lifecycle so `onCleared()` runs.
 *
 * `ViewModel.onCleared()` is protected and `clear()` is library-internal, so the
 * only portable way to trigger teardown in a multiplatform test is to register
 * the instance in a [ViewModelStore] and clear the store.
 *
 * The instance must already be constructed; this just attaches it to a store and
 * tears it down, invoking `onCleared()`.
 */
inline fun <reified VM : ViewModel> VM.clearForTest() {
    val store = ViewModelStore()
    val provider = ViewModelProvider.create(
        store = store,
        factory = viewModelFactory { initializer { this@clearForTest } },
    )
    // Force creation/registration of the instance in the store, then clear it.
    provider[VM::class]
    store.clear()
}
