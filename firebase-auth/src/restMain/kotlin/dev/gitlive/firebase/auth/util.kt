package dev.gitlive.firebase.auth

import kotlin.coroutines.cancellation.CancellationException

internal fun <T> Result<T>.ignoreCancellation() = onFailure {
    if (it is CancellationException)
        throw it
}