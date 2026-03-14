package com.riox432.civitdeck.util

/**
 * Removes and returns the last element, or returns null if the list is empty.
 */
fun <T> MutableList<T>.removeLastOrNull(): T? =
    if (isNotEmpty()) removeAt(lastIndex) else null
