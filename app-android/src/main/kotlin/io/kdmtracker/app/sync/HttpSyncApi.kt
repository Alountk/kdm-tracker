package io.kdmtracker.app.sync

import io.kdmtracker.core.sync.CampaignSummary
import io.kdmtracker.core.sync.ChangeSet
import io.kdmtracker.core.sync.PullResponse
import io.kdmtracker.core.sync.SyncApi
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Development default base URL: the Android emulator reaches the host machine's sync hub through the
 * special `10.0.2.2` alias. A real deployment would inject a configured URL instead.
 */
const val DEFAULT_SYNC_BASE_URL: String = "http://10.0.2.2:8080"

/** Raised when the sync hub answers with a non-2xx status; carries the HTTP [status]. */
class SyncHttpException(
    val status: Int,
    message: String,
) : IOException(message)

/**
 * [SyncApi] over `java.net.HttpURLConnection` (no HTTP framework).
 *
 * Encodes/decodes the `:core` wire types with kotlinx-serialization. Each request carries the
 * bearer token supplied by [tokenProvider] (read fresh per call, so a rotated token is picked up
 * without rebuilding the client); a `null` token sends no `Authorization` header. The token is only
 * ever placed in the header and is never logged.
 */
class HttpSyncApi(
    private val baseUrl: String,
    private val tokenProvider: () -> String?,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : SyncApi {
    override fun listCampaigns(): List<CampaignSummary> {
        val connection = openConnection("GET", "/api/v1/campaigns")
        return json.decodeFromString(
            ListSerializer(CampaignSummary.serializer()),
            connection.readBody(),
        )
    }

    override fun createCampaign(name: String): CampaignSummary {
        val connection = openConnection("POST", "/api/v1/campaigns")
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        val body = buildJsonObject { put("name", name) }.toString()
        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        // The app module does not apply the kotlinx-serialization compiler plugin, so the response
        // is read from the JSON tree instead of a generated DTO (it mirrors the server's
        // `CampaignResponse`: id, name, role, inviteCode).
        val response = json.parseToJsonElement(connection.readBody()).jsonObject
        return CampaignSummary(
            id = response.getValue("id").jsonPrimitive.content,
            name = response.getValue("name").jsonPrimitive.content,
            role = response.getValue("role").jsonPrimitive.content,
            ownerUsername = "",
            sharedWith = emptyList(),
        )
    }

    override fun pull(
        campaignId: String,
        since: Long,
    ): PullResponse {
        val connection = openConnection("GET", "/api/v1/campaigns/$campaignId/changes?since=$since")
        return json.decodeFromString(PullResponse.serializer(), connection.readBody())
    }

    override fun push(
        campaignId: String,
        changeSet: ChangeSet,
    ): Long {
        val connection = openConnection("POST", "/api/v1/campaigns/$campaignId/changes")
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        val body = json.encodeToString(ChangeSet.serializer(), changeSet)
        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        return json
            .parseToJsonElement(connection.readBody())
            .jsonObject
            .getValue("latestRev")
            .jsonPrimitive
            .long
    }

    private fun openConnection(
        method: String,
        path: String,
    ): HttpURLConnection {
        val connection =
            (URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = CONNECT_TIMEOUT_MILLIS
                readTimeout = READ_TIMEOUT_MILLIS
                setRequestProperty("Accept", "application/json")
            }
        tokenProvider()?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
        return connection
    }

    private fun HttpURLConnection.readBody(): String {
        val status = responseCode
        val stream = if (status in SUCCESS_RANGE) inputStream else errorStream
        val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        if (status !in SUCCESS_RANGE) {
            throw SyncHttpException(status, "Sync hub returned HTTP $status for $requestMethod $url")
        }
        return body
    }

    private companion object {
        val SUCCESS_RANGE = 200..299
        const val CONNECT_TIMEOUT_MILLIS = 10_000
        const val READ_TIMEOUT_MILLIS = 30_000
    }
}
