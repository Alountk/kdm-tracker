package io.kdmtracker.app.sync

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Exchanges a username/password for a bearer token.
 *
 * Kept as a tiny port so the sign-in flow is testable without a live hub, mirroring [SyncApi].
 */
fun interface SyncAuthApi {
    /** Returns the bearer token, or throws if the credentials are rejected or the hub is unreachable. */
    fun login(
        username: String,
        password: String,
    ): String
}

/**
 * [SyncAuthApi] over `java.net.HttpURLConnection` (no HTTP framework).
 *
 * `POST`s `{"username","password"}` to `/api/v1/auth/login` and reads the token back from
 * `{"token":"<jwt>"}`. The credentials are only ever placed in the request body and are never logged.
 */
class HttpSyncAuthApi(
    private val baseUrl: String,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : SyncAuthApi {
    override fun login(
        username: String,
        password: String,
    ): String {
        val connection =
            (URL(baseUrl.trimEnd('/') + LOGIN_PATH).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = CONNECT_TIMEOUT_MILLIS
                readTimeout = READ_TIMEOUT_MILLIS
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
            }
        val body =
            buildJsonObject {
                put("username", username)
                put("password", password)
            }.toString()
        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

        val status = connection.responseCode
        val stream = if (status in SUCCESS_RANGE) connection.inputStream else connection.errorStream
        val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        if (status !in SUCCESS_RANGE) {
            throw IOException("Sign-in failed: HTTP $status")
        }
        return json
            .parseToJsonElement(response)
            .jsonObject
            .getValue("token")
            .jsonPrimitive
            .content
    }

    private companion object {
        val SUCCESS_RANGE = 200..299
        const val LOGIN_PATH = "/api/v1/auth/login"
        const val CONNECT_TIMEOUT_MILLIS = 10_000
        const val READ_TIMEOUT_MILLIS = 30_000
    }
}
