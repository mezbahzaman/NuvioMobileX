package com.nuvio.app.features.radar

import co.touchlab.kermit.Logger
import com.nuvio.app.core.network.SupabaseProvider
import io.github.jan.supabase.functions.functions
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.put

/**
 * Calls the radar-fixtures Supabase edge function (TheSportsDB proxy + cache). The paid API
 * key never ships in the app — this is the only fixtures endpoint the client knows.
 * verify_jwt=false server-side, so this works in the local-anonymous (signed-out) state too.
 */
internal object RadarFixturesClient {
    private val log = Logger.withTag("RadarFixturesClient")
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetch(
        leagueIds: Collection<String>,
        livescoreSports: Collection<String>,
        teamIds: Collection<String> = emptyList(),
    ): RadarFixturesResponse? {
        if (leagueIds.isEmpty() && livescoreSports.isEmpty() && teamIds.isEmpty()) return null
        return try {
            val body = buildJsonObject {
                put("league_ids", buildJsonArray { leagueIds.forEach { add(it) } })
                put("livescore_sports", buildJsonArray { livescoreSports.forEach { add(it) } })
                put("team_ids", buildJsonArray { teamIds.forEach { add(it) } })
            }
            log.d { "fetch — calling radar-fixtures with ${leagueIds.size} leagues, ${livescoreSports.size} sports, ${teamIds.size} teams" }
            val response = SupabaseProvider.client.functions.invoke(
                function = "radar-fixtures",
                body = body,
            )
            val text = response.bodyAsText()
            log.d { "fetch — response status=${response.status} bodyLength=${text.length}" }
            val parsed = json.decodeFromString<RadarFixturesResponse>(text)
            log.d { "fetch — parsed: ${parsed.fixtures.size} league keys, total ${parsed.fixtures.values.sumOf { it.size }} fixtures" }
            parsed
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.e(e) { "fetch — FAILED" }
            null
        }
    }

    /** Broadcaster listings for one event (server caches 12h; empty on any failure). */
    suspend fun fetchTv(eventId: String): List<RadarTvStation> = try {
        val body = buildJsonObject {
            put("tv_event_ids", buildJsonArray { add(eventId) })
        }
        val response = SupabaseProvider.client.functions.invoke(
            function = "radar-fixtures",
            body = body,
        )
        json.decodeFromString<RadarFixturesResponse>(response.bodyAsText()).tv[eventId].orEmpty()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        log.e(e) { "tv fetch — FAILED" }
        emptyList()
    }
}
