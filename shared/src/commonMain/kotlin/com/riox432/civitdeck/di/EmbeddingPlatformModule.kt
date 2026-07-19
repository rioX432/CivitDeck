package com.riox432.civitdeck.di

import org.koin.core.module.Module

/**
 * Composition hook for the on-device embedding bindings, resolved at the app's
 * composition root rather than in `:core:core-domain` (which must stay ML-free).
 *
 * iOS and Desktop bind the real `:core:core-ml` implementations here. Android leaves
 * this empty and lets the active build flavor supply the binding — `githubFull` wires
 * `:core:core-ml`, while `fdroid` supplies unavailable no-ops and never depends on it.
 */
expect val embeddingPlatformModule: Module
