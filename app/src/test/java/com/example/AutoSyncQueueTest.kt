package com.example

import com.example.data.repository.AutoSyncQueue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AutoSyncQueueTest {
    @Test fun changesAutomaticallyStartOneSerialSync() = runTest {
        var calls = 0
        val gate = CompletableDeferred<Unit>()
        val queue = AutoSyncQueue(backgroundScope, attempt = { calls++; if (calls == 1) gate.await() }, onFailure = { false })
        repeat(20) { queue.request() }
        runCurrent()
        assertEquals(1, calls)
        gate.complete(Unit)
        runCurrent()
        assertEquals(1, calls)
        queue.close()
    }

    @Test fun editsDuringUploadAreNotLost() = runTest {
        var calls = 0
        val gate = CompletableDeferred<Unit>()
        val queue = AutoSyncQueue(backgroundScope, attempt = { calls++; if (calls == 1) gate.await() }, onFailure = { false })
        queue.request(); runCurrent()
        repeat(10) { queue.request() }
        gate.complete(Unit); runCurrent()
        assertEquals(2, calls)
        queue.close()
    }

    @Test fun networkFailureRetriesWithoutManualRequest() = runTest {
        var calls = 0
        val queue = AutoSyncQueue(backgroundScope, attempt = { calls++; if (calls == 1) error("Offline") }, onFailure = { true })
        queue.request(); runCurrent()
        assertEquals(1, calls)
        advanceTimeBy(5_000); runCurrent()
        assertEquals(2, calls)
        queue.close()
    }

    @Test fun connectivityEventWakesBackoffImmediately() = runTest {
        var calls = 0
        val queue = AutoSyncQueue(backgroundScope, attempt = { calls++; if (calls == 1) error("Offline") }, onFailure = { true })
        queue.request(); runCurrent()
        advanceTimeBy(100)
        queue.request(); runCurrent()
        assertEquals(2, calls)
        queue.close()
    }

    @Test fun permissionFailureDoesNotCreateAnEndlessRetryLoop() = runTest {
        var calls = 0
        val queue = AutoSyncQueue(backgroundScope, attempt = { calls++; error("Permission denied") }, onFailure = { false })
        queue.request(); runCurrent()
        advanceTimeBy(120_000); runCurrent()
        assertEquals(1, calls)
        queue.request(); runCurrent()
        assertEquals(2, calls)
        queue.close()
    }

    @Test fun timedOutAttemptCanRetry() = runTest {
        var calls = 0
        val queue = AutoSyncQueue(backgroundScope, attempt = {
            calls++
            if (calls == 1) withTimeout(100) { delay(200) }
        }, onFailure = { true })
        queue.request(); runCurrent()
        advanceTimeBy(100); runCurrent()
        advanceTimeBy(5_000); runCurrent()
        assertEquals(2, calls)
        queue.close()
    }

    @Test fun signingOutCancelsPendingRetries() = runTest {
        var calls = 0
        val queue = AutoSyncQueue(backgroundScope, attempt = { calls++; error("Offline") }, onFailure = { true })
        queue.request(); runCurrent()
        queue.close()
        advanceTimeBy(120_000); runCurrent()
        assertEquals(1, calls)
    }
}
