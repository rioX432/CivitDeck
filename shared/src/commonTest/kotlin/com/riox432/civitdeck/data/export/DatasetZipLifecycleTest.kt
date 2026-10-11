package com.riox432.civitdeck.data.export

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class DatasetZipLifecycleTest {
    @Test
    fun successfulCommitAndAbortAreIdempotent() {
        var commits = 0
        var discards = 0
        val transaction = DatasetZipTransaction({ commits++ }, { discards++ })
        transaction.write { }
        transaction.close()
        transaction.close()
        transaction.abort()
        assertEquals(1, commits)
        assertEquals(0, discards)
        assertFailsWith<IllegalStateException> { transaction.write { } }
    }

    @Test
    fun abortedArchiveCannotBeCommitted() {
        var commits = 0
        var discards = 0
        val transaction = DatasetZipTransaction({ commits++ }, { discards++ })
        transaction.abort()
        transaction.abort()
        assertFailsWith<IllegalStateException> { transaction.close() }
        assertFailsWith<IllegalStateException> { transaction.write { } }
        assertEquals(0, commits)
        assertEquals(1, discards)
    }

    @Test
    fun publicationFailureDiscardsOnceAndCannotBeRetried() {
        var discards = 0
        val failure = IllegalStateException("publication failed")
        val transaction = DatasetZipTransaction({ throw failure }, { discards++ })
        assertSame(failure, assertFailsWith<IllegalStateException> { transaction.close() })
        transaction.abort()
        assertFailsWith<IllegalStateException> { transaction.close() }
        assertEquals(1, discards)
    }

    @Test
    fun writeFailureDiscardsAndCannotBeCommitted() {
        var discards = 0
        val failure = IllegalArgumentException("write failed")
        val transaction = DatasetZipTransaction({ error("must not publish") }, { discards++ })
        assertSame(failure, assertFailsWith<IllegalArgumentException> { transaction.write { throw failure } })
        assertFailsWith<IllegalStateException> { transaction.close() }
        assertEquals(1, discards)
    }

    @Test
    fun cleanupFailureDoesNotReplaceOriginalFailure() {
        val failure = IllegalArgumentException("write failed")
        val cleanup = IllegalStateException("cleanup denied")
        val transaction = DatasetZipTransaction({ error("must not publish") }, { throw cleanup })
        assertSame(failure, assertFailsWith<IllegalArgumentException> { transaction.write { throw failure } })
        assertEquals(listOf(cleanup), failure.suppressedExceptions)
        assertFailsWith<IllegalStateException> { transaction.close() }
    }

    @Test
    fun cleanupFailureStillPreventsPublication() {
        val cleanup = IllegalArgumentException("cleanup denied")
        val transaction = DatasetZipTransaction({ error("must not publish") }, { throw cleanup })
        assertSame(cleanup, assertFailsWith<IllegalArgumentException> { transaction.abort() })
        assertFailsWith<IllegalStateException> { transaction.close() }
        transaction.abort()
    }
}
