package com.riox432.civitdeck.domain.model

/**
 * Base exception hierarchy for the CivitDeck domain layer.
 * Use these instead of catching generic [Exception] at boundary code.
 */
sealed class DomainException(message: String?, cause: Throwable? = null) : Exception(message, cause) {

    /** Network-related failures: timeout, connectivity, HTTP errors. */
    class NetworkException(message: String?, cause: Throwable? = null) : DomainException(message, cause)

    /** Local file I/O failures: read/write errors, permission denied. */
    class StorageException(message: String?, cause: Throwable? = null) : DomainException(message, cause)

    /** Database operation failures: query errors, migration issues. */
    class DatabaseException(message: String?, cause: Throwable? = null) : DomainException(message, cause)

    /** Authentication failures: invalid/expired API key. */
    class AuthException(message: String?, cause: Throwable? = null) : DomainException(message, cause)
}
