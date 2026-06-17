package com.riox432.civitdeck.domain.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * Platform IO dispatcher for offloading blocking work (DB / network writes).
 *
 * `Dispatchers.IO` is not visible from `commonMain`, so it is provided per
 * platform via `expect`/`actual`.
 */
expect val ioDispatcher: CoroutineDispatcher

/**
 * Application-lifetime [CoroutineScope] for fire-and-forget work that must
 * outlive the component that started it (e.g. a ViewModel's `onCleared`).
 *
 * Uses a [SupervisorJob] so a failure in one job does not cancel others, and
 * [ioDispatcher] because these tasks are typically DB or network writes.
 *
 * Registered as a Koin `single` so all platforms share one instance instead of
 * creating ad-hoc scopes in their entry points.
 *
 * Intended only for app-lifetime side effects — do NOT use it for regular
 * ViewModel work that should be cancelled when the screen closes.
 */
class ApplicationScope(
    private val delegate: CoroutineScope = CoroutineScope(SupervisorJob() + ioDispatcher),
) : CoroutineScope by delegate
