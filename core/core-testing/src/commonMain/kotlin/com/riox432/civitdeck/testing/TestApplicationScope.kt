package com.riox432.civitdeck.testing

import com.riox432.civitdeck.domain.util.ApplicationScope
import kotlinx.coroutines.test.TestScope

/**
 * Builds an [ApplicationScope] backed by a [TestScope] so fire-and-forget work
 * launched into the app-lifetime scope (e.g. `onCleared` end-view tracking) runs
 * on the test's virtual-time scheduler instead of a real background dispatcher.
 *
 * [ApplicationScope] already accepts any [kotlinx.coroutines.CoroutineScope]; this
 * is a thin, intention-revealing factory for ViewModel tests.
 */
fun testApplicationScope(scope: TestScope): ApplicationScope = ApplicationScope(scope)
