package com.studymate.data.remote

import com.studymate.data.remote.dto.ApiErrorEnvelope
import com.studymate.domain.common.AppResult
import com.studymate.domain.common.DomainError
import com.studymate.domain.common.ErrorCode
import java.io.IOException
import java.io.InterruptedIOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException

/**
 * Gọi API và chuyển mọi exception thành [DomainError]. Không nuốt [CancellationException]
 * để huỷ coroutine vẫn hoạt động đúng.
 */
internal suspend inline fun <T> apiCall(json: Json, crossinline block: suspend () -> T): AppResult<T> =
    try {
        AppResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        AppResult.Failure(e.toDomainError(json))
    } catch (e: InterruptedIOException) {
        AppResult.Failure(DomainError.Timeout)
    } catch (e: IOException) {
        AppResult.Failure(DomainError.Network)
    } catch (e: SerializationException) {
        AppResult.Failure(DomainError.Unknown(e))
    } catch (e: IllegalArgumentException) {
        AppResult.Failure(DomainError.Unknown(e))
    }

internal fun HttpException.toDomainError(json: Json): DomainError {
    val body = runCatching { response()?.errorBody()?.string() }.getOrNull()
    val parsed = body
        ?.takeIf { it.isNotBlank() }
        ?.let { runCatching { json.decodeFromString(ApiErrorEnvelope.serializer(), it).error }.getOrNull() }
    val code = parsed?.code?.let(ErrorCode::fromWire)?.takeIf { it != ErrorCode.UNKNOWN } ?: codeForStatus(code())
    return DomainError.Api(code = code, message = parsed?.message)
}

internal fun codeForStatus(status: Int): ErrorCode = when (status) {
    401, 403 -> ErrorCode.UNAUTHORIZED
    404 -> ErrorCode.NOT_FOUND
    409 -> ErrorCode.NOT_READY
    413 -> ErrorCode.FILE_TOO_LARGE
    415 -> ErrorCode.UNSUPPORTED_TYPE
    429 -> ErrorCode.RATE_LIMITED
    501 -> ErrorCode.NOT_IMPLEMENTED
    503 -> ErrorCode.LLM_UNAVAILABLE
    in 500..599 -> ErrorCode.INTERNAL
    else -> ErrorCode.UNKNOWN
}
