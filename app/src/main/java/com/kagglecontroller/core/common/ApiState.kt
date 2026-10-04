package com.kagglecontroller.core.common

import com.kagglecontroller.core.network.KaggleException

/** Every screen renders one of these. No blank screens when the API fails (spec 54, 58). */
sealed interface ApiState<out T> {
    data object Idle : ApiState<Nothing>
    data object Loading : ApiState<Nothing>
    data class Success<T>(val data: T, val cached: Boolean = false) : ApiState<T>
    data class Error(val message: String, val code: Int? = null) : ApiState<Nothing>
    data object Offline : ApiState<Nothing>
    data class Unsupported(val reason: String, val webUrl: String? = null) : ApiState<Nothing>
    data object Unauthorized : ApiState<Nothing>
    data class Forbidden(val message: String) : ApiState<Nothing>
    data class RateLimited(val retryAfterSeconds: Int?) : ApiState<Nothing>
}

fun Throwable.toApiState(): ApiState<Nothing> = when (this) {
    is KaggleException.Unauthorized -> ApiState.Unauthorized
    is KaggleException.Forbidden -> ApiState.Forbidden(message ?: "You don't have permission to do that.")
    is KaggleException.RateLimited -> ApiState.RateLimited(retryAfterSeconds)
    is KaggleException.Offline -> ApiState.Offline
    is KaggleException.Http -> ApiState.Error(message ?: "Kaggle returned an error.", code)
    else -> ApiState.Error(message ?: "Something went wrong.")
}
