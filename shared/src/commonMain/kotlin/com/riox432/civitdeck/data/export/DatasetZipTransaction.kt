package com.riox432.civitdeck.data.export

/** Single-owner lifecycle shared by the platform writers. */
internal class DatasetZipTransaction(
    private val publish: () -> Unit,
    private val discard: () -> Unit,
) {
    private var state = State.OPEN

    fun write(action: () -> Unit) {
        check(state == State.OPEN) { "Archive is no longer writable" }
        try {
            action()
        } catch (@Suppress("TooGenericExceptionCaught") failure: Exception) {
            discardAfterFailure(failure)
            throw failure
        }
    }

    fun close() {
        if (state == State.COMMITTED) return
        check(state == State.OPEN) { "Archive was aborted" }
        try {
            publish()
            state = State.COMMITTED
        } catch (@Suppress("TooGenericExceptionCaught") failure: Exception) {
            discardAfterFailure(failure)
            throw failure
        }
    }

    fun abort() {
        if (state != State.OPEN) return
        state = State.ABORTED
        discard()
    }

    private fun discardAfterFailure(failure: Exception) {
        try {
            abort()
        } catch (@Suppress("TooGenericExceptionCaught") cleanup: Exception) {
            failure.addSuppressed(cleanup)
        }
    }

    private enum class State { OPEN, COMMITTED, ABORTED }
}
