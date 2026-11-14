package com.studymate.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/** Adds an anonymous mobile session after a protected request receives 401, and renews expired sessions. */
internal class MobileSessionAuthenticator(
    private val baseUrl: HttpUrl,
    private val sessionClient: OkHttpClient,
    private val tokenStore: SessionTokenStore,
    private val json: Json,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : Authenticator {

    @Synchronized
    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= MAX_AUTH_ATTEMPTS) return null

        val requestToken = response.request.header(AUTHORIZATION)
            ?.takeIf { it.startsWith(BEARER_PREFIX) }
            ?.removePrefix(BEARER_PREFIX)
        val saved = tokenStore.get()
        val savedIsFresh = saved != null && saved.expiresAtEpochMillis > nowMillis() + EXPIRY_SKEW_MILLIS

        // Another concurrent request may already have renewed the token.
        if (savedIsFresh && saved!!.accessToken != requestToken) {
            return response.request.newBuilder()
                .header(AUTHORIZATION, "$BEARER_PREFIX${saved.accessToken}")
                .build()
        }

        val session = createSession() ?: run {
            tokenStore.clear()
            return null
        }
        tokenStore.save(session)
        return response.request.newBuilder()
            .header(AUTHORIZATION, "$BEARER_PREFIX${session.accessToken}")
            .build()
    }

    private fun createSession(): MobileSessionToken? {
        val request = Request.Builder()
            .url(baseUrl.resolve(SESSION_PATH) ?: return null)
            .post(ByteArray(0).toRequestBody(JSON_MEDIA_TYPE))
            .build()

        return try {
            sessionClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string()?.takeIf { it.isNotBlank() } ?: return null
                val session = json.decodeFromString<MobileSessionResponse>(body)
                if (session.accessToken.isBlank() || session.expiresIn <= 0) return null
                MobileSessionToken(
                    accessToken = session.accessToken,
                    expiresAtEpochMillis = nowMillis() + session.expiresIn * MILLIS_PER_SECOND,
                )
            }
        } catch (_: IOException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    @Serializable
    private data class MobileSessionResponse(
        @SerialName("access_token") val accessToken: String,
        @SerialName("expires_in") val expiresIn: Long,
    )

    private companion object {
        const val SESSION_PATH = "v1/auth/session"
        const val AUTHORIZATION = "Authorization"
        const val BEARER_PREFIX = "Bearer "
        const val MAX_AUTH_ATTEMPTS = 2
        const val EXPIRY_SKEW_MILLIS = 30_000L
        const val MILLIS_PER_SECOND = 1_000L
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}

internal class MobileBearerInterceptor(
    private val tokenStore: SessionTokenStore,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : okhttp3.Interceptor {
    override fun intercept(chain: okhttp3.Interceptor.Chain): Response {
        val request = chain.request()
        val session = tokenStore.get()
            ?.takeIf { it.expiresAtEpochMillis > nowMillis() + SESSION_EXPIRY_SKEW_MILLIS }
        val authenticated = if (session == null) {
            request
        } else {
            request.newBuilder().header(AUTHORIZATION, "$BEARER_PREFIX${session.accessToken}").build()
        }
        return chain.proceed(authenticated)
    }

    private companion object {
        const val AUTHORIZATION = "Authorization"
        const val BEARER_PREFIX = "Bearer "
        const val SESSION_EXPIRY_SKEW_MILLIS = 30_000L
    }
}
