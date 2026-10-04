package com.kagglecontroller.core.network

sealed class KaggleException(message: String?, cause: Throwable? = null) : Exception(message, cause) {
    class Unauthorized : KaggleException("Your Kaggle token was rejected. Sign in again with a fresh token.")
    class Forbidden(message: String?) : KaggleException(message ?: "Kaggle says you don't have permission.")
    class NotFound(message: String?) : KaggleException(message ?: "Not found on Kaggle.")
    class RateLimited(val retryAfterSeconds: Int?) :
        KaggleException("Kaggle is rate limiting requests. Try again in a moment.")
    class Offline(cause: Throwable? = null) : KaggleException("No connection to Kaggle.", cause)
    class Http(val code: Int, message: String?) : KaggleException(message ?: "Kaggle returned HTTP $code.")
    class Parse(message: String?) : KaggleException(message ?: "Unexpected response from Kaggle.")
}
