package com.riox432.civitdeck.di

import org.koin.core.parameter.ParametersDefinition
import org.koin.mp.KoinPlatform.getKoin

/**
 * Central DI accessor for iOS.
 *
 * All getter / factory functions are defined as extension functions in
 * domain-specific files:
 * - KoinHelper+Search.kt      — search, discovery, embedding, search history
 * - KoinHelper+Detail.kt      — model detail, creator, browsing history, hidden, notes, tags, feed
 * - KoinHelper+ComfyUI.kt     — ComfyUI, SD WebUI, Civitai Link, external server
 * - KoinHelper+Collections.kt — favorites, collections, local files, dataset
 * - KoinHelper+Settings.kt    — settings, NSFW, auth, notifications, cache, prompts, backup, plugins, themes, hashtags
 * - KoinHelper+ViewModels.kt  — all ViewModel factory functions, downloads
 */
object KoinHelper

/**
 * Generic DI resolution helper. Reduces `getKoin().get()` boilerplate
 * in every typed accessor to a single `resolve()` call.
 *
 * Note: `inline reified` cannot be exported to ObjC/Swift, so typed
 * extension functions still exist as the public API surface for iOS.
 */
inline fun <reified T : Any> KoinHelper.resolve(
    noinline parameters: ParametersDefinition? = null,
): T = getKoin().get(parameters = parameters)
