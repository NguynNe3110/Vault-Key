package com.example.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select
import kotlin.coroutines.coroutineContext

/** Serializes automatic requests and retains a request made during an in-flight upload. */
@OptIn(ExperimentalCoroutinesApi::class)
class AutoSyncQueue(
    scope: CoroutineScope,
    private val initialRetryMillis: Long = 5_000,
    private val maximumRetryMillis: Long = 60_000,
    private val attempt: suspend () -> Unit,
    private val onFailure: (Throwable) -> Boolean
) {
    private val requests = Channel<Unit>(Channel.CONFLATED)
    private val job = scope.launch {
        for (request in requests) {
            var retryMillis = initialRetryMillis
            while (isActive) {
                try {
                    attempt()
                    break
                } catch (error: Throwable) {
                    if (error is CancellationException && error !is TimeoutCancellationException) throw error
                    coroutineContext.ensureActive()
                    if (!onFailure(error)) break
                    // A new edit or a connectivity event wakes retries immediately.
                    select<Unit> {
                        requests.onReceive { }
                        onTimeout(retryMillis) { }
                    }
                    retryMillis = (retryMillis * 2).coerceAtMost(maximumRetryMillis)
                }
            }
        }
    }

    fun request() { requests.trySend(Unit) }
    suspend fun close() { job.cancel(); job.join(); requests.close() }
}
