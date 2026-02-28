package com.riox432.civitdeck.data.api

/**
 * Thrown when a network response cannot be parsed into the expected type.
 * Wraps framework-specific serialization errors (e.g. Ktor ContentConvertException)
 * so they do not leak into the domain layer.
 */
class DataParseException(message: String?, cause: Throwable? = null) : Exception(message, cause)
