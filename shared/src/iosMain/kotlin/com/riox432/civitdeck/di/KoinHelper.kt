package com.riox432.civitdeck.di

/**
 * Central DI accessor for iOS.
 *
 * All getter / factory functions are defined as extension functions in
 * domain-specific files:
 * - KoinHelper+Search.kt      — search, discovery, embedding, search history
 * - KoinHelper+Detail.kt      — model detail, creator, browsing history, hidden, notes, tags, reviews, feed
 * - KoinHelper+ComfyUI.kt     — ComfyUI, SD WebUI, Civitai Link, external server
 * - KoinHelper+Collections.kt — favorites, collections, local files, dataset
 * - KoinHelper+Settings.kt    — settings, NSFW, auth, notifications, cache, prompts, backup, plugins, themes, hashtags
 * - KoinHelper+ViewModels.kt  — all ViewModel factory functions, downloads
 */
object KoinHelper
