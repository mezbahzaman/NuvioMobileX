package com.nuvio.app.features.iptv.match

import com.nuvio.app.features.iptv.XtreamAccount
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.util.Collections
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * The hybrid poster pipeline (research/iptv-catalog-loading.md §12) on the KMP side:
 * enrichment written via [XtreamMatchIndex.updatePoster] must survive icon-less re-syncs,
 * and [PosterEnricher] must ask a panel about each item at most once per process, over the
 * real transport (local HttpServer serving B-provider-shaped get_vod_info payloads).
 */
class PosterPipelineTest {

    private var server: HttpServer? = null
    private val asked = Collections.synchronizedList(mutableListOf<String>())

    @BeforeTest
    fun setUpDb() {
        MatchDbDriver.openForTests =
            { androidx.sqlite.driver.bundled.BundledSQLiteDriver().open(":memory:") }
    }

    @AfterTest
    fun stop() {
        server?.stop(0)
    }

    /** Serves get_vod_info/get_series_info like the measured B provider; records every ask. */
    private fun servePanel(
        vodImage: (Int) -> String?,
        cover: (Int) -> String? = { null },
        failFirst: Int = 0,
    ): String {
        var failsLeft = failFirst
        val s = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        s.createContext("/player_api.php") { ex ->
            val q = (ex.requestURI.rawQuery ?: "").split("&").associate {
                val kv = it.split("=", limit = 2)
                kv[0] to URLDecoder.decode(kv.getOrElse(1) { "" }, "UTF-8")
            }
            val action = q["action"] ?: ""
            asked.add("$action:${q["vod_id"] ?: q["series_id"] ?: ""}")
            if (failsLeft > 0) {
                failsLeft--
                ex.sendResponseHeaders(500, -1)
                ex.close()
                return@createContext
            }
            val body = when (action) {
                "get_vod_info" -> {
                    val img = vodImage(q["vod_id"]!!.toInt())
                    """{"info":{"movie_image":${img?.let { "\"$it\"" } ?: "\"\""},"cover_big":"","plot":"x"},"movie_data":{"stream_id":${q["vod_id"]}}}"""
                }
                "get_series_info" -> {
                    val img = cover(q["series_id"]!!.toInt())
                    """{"info":{"cover":${img?.let { "\"$it\"" } ?: "\"\""},"plot":"x"},"episodes":{}}"""
                }
                else -> "[]"
            }.toByteArray()
            ex.responseHeaders.add("Content-Type", "application/json")
            ex.sendResponseHeaders(200, body.size.toLong())
            ex.responseBody.use { it.write(body) }
        }
        s.executor = null
        s.start()
        server = s
        return "http://127.0.0.1:${s.address.port}"
    }

    private fun account(baseUrl: String, id: String) =
        XtreamAccount(id = id, name = "t", baseUrl = baseUrl, username = "u", password = "p")

    private fun bare(sid: Int, name: String = "Movie $sid") =
        IndexedItem(sid = sid, name = name, year = 2020, tmdb = null, ext = "mp4", poster = null, categoryId = "7")

    private fun await(what: String, timeoutMs: Long = 5_000, cond: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            if (cond()) return
            Thread.sleep(20)
        }
        fail("timed out waiting for $what")
    }

    // ---- index persistence ---------------------------------------------------------------

    @Test
    fun `an icon-less re-sync does not wipe enrichment`() = runBlocking {
        val p = "prov-preserve"
        XtreamMatchIndex.rebuild(p, MatchKind.MOVIE, listOf(bare(1)))
        XtreamMatchIndex.updatePoster(p, MatchKind.MOVIE, 1, "https://img/1.jpg")
        val stats = XtreamMatchIndex.sync(p, MatchKind.MOVIE, listOf(bare(1)))
        assertEquals(0, stats.changed) // poster is not part of the fingerprint
        assertEquals("https://img/1.jpg", XtreamMatchIndex.item(p, MatchKind.MOVIE, 1)?.poster)
    }

    @Test
    fun `a renamed row keeps its enriched poster through sync AND streamed sync`() = runBlocking {
        val p = "prov-rename"
        XtreamMatchIndex.rebuild(p, MatchKind.MOVIE, listOf(bare(1, "Old"), bare(2, "Old2")))
        XtreamMatchIndex.updatePoster(p, MatchKind.MOVIE, 1, "https://img/1.jpg")
        XtreamMatchIndex.updatePoster(p, MatchKind.MOVIE, 2, "https://img/2.jpg")

        XtreamMatchIndex.sync(p, MatchKind.MOVIE, listOf(bare(1, "New"), bare(2, "Old2")))
        assertEquals("New", XtreamMatchIndex.item(p, MatchKind.MOVIE, 1)?.name)
        assertEquals("https://img/1.jpg", XtreamMatchIndex.item(p, MatchKind.MOVIE, 1)?.poster)

        val session = XtreamMatchIndex.beginSync(p, MatchKind.MOVIE)
        session.accept(bare(1, "New"))
        session.accept(bare(2, "Newer2"))
        session.finish()
        assertEquals("Newer2", XtreamMatchIndex.item(p, MatchKind.MOVIE, 2)?.name)
        assertEquals("https://img/2.jpg", XtreamMatchIndex.item(p, MatchKind.MOVIE, 2)?.poster)
    }

    @Test
    fun `a fresh bulk icon still lands on a changed row`() = runBlocking {
        val p = "prov-bulk"
        XtreamMatchIndex.rebuild(p, MatchKind.MOVIE, listOf(bare(1, "Old")))
        XtreamMatchIndex.sync(p, MatchKind.MOVIE, listOf(bare(1, "New").copy(poster = "https://bulk/new.jpg")))
        assertEquals("https://bulk/new.jpg", XtreamMatchIndex.item(p, MatchKind.MOVIE, 1)?.poster)
    }

    @Test
    fun `unchanged rows stay unchanged across a full sync round-trip`() = runBlocking {
        val p = "prov-fp"
        val items = listOf(
            IndexedItem(1, "A", 2020, 7, "mp4", poster = "p", categoryId = "9", epgId = "a.uk", hasArchive = true),
            IndexedItem(2, "B", null, null, null, poster = null, categoryId = null, epgId = null, hasArchive = false),
        )
        XtreamMatchIndex.rebuild(p, MatchKind.LIVE, items)
        val stats = XtreamMatchIndex.sync(p, MatchKind.LIVE, items)
        assertEquals(0, stats.changed)
        assertEquals(0, stats.added)
        assertEquals(0, stats.removed)
    }

    @Test
    fun `categories serve in the panel's arrival order, never alphabetized`() = runBlocking {
        val p = "prov-order"
        XtreamMatchIndex.rebuild(p, MatchKind.MOVIE, listOf(bare(1, "Zebra"), bare(2, "Apple"), bare(3, "Mango")))
        assertEquals(
            listOf("Zebra", "Apple", "Mango"),
            XtreamMatchIndex.itemsFor(p, MatchKind.MOVIE, "7", 0, 10).map { it.name }
        )
        // Panel reorders — next sync serves the new order; streamed path stamps the same way.
        XtreamMatchIndex.sync(p, MatchKind.MOVIE, listOf(bare(3, "Mango"), bare(1, "Zebra"), bare(2, "Apple")))
        assertEquals(
            listOf("Mango", "Zebra", "Apple"),
            XtreamMatchIndex.itemsFor(p, MatchKind.MOVIE, "7", 0, 10).map { it.name }
        )
        val session = XtreamMatchIndex.beginSync(p, MatchKind.MOVIE)
        listOf(bare(2, "Apple"), bare(3, "Mango"), bare(1, "Zebra")).forEach { session.accept(it) }
        session.finish()
        assertEquals(
            listOf("Apple", "Mango", "Zebra"),
            XtreamMatchIndex.itemsFor(p, MatchKind.MOVIE, "7", 0, 10).map { it.name }
        )
    }

    @Test
    fun `lastAddedAt bumps only when a sync ADDS items`() = runBlocking {
        val p = "prov-lastadded"
        XtreamMatchIndex.rebuild(p, MatchKind.MOVIE, listOf(bare(1), bare(2)))
        val afterBuild = XtreamMatchIndex.lastAddedAt(p, MatchKind.MOVIE)
        assertTrue(afterBuild > 0, "first build must count as additions")

        Thread.sleep(5)
        XtreamMatchIndex.sync(p, MatchKind.MOVIE, listOf(bare(1), bare(2)))
        assertEquals(afterBuild, XtreamMatchIndex.lastAddedAt(p, MatchKind.MOVIE))

        Thread.sleep(5)
        XtreamMatchIndex.sync(p, MatchKind.MOVIE, listOf(bare(1), bare(2), bare(3)))
        assertTrue(XtreamMatchIndex.lastAddedAt(p, MatchKind.MOVIE) > afterBuild)

        val beforeReset = XtreamMatchIndex.lastAddedAt(p, MatchKind.MOVIE)
        Thread.sleep(5)
        XtreamMatchIndex.distrustNegativeMappings(p)
        assertTrue(XtreamMatchIndex.lastAddedAt(p, MatchKind.MOVIE) > beforeReset)
    }

    @Test
    fun `probe returns indexed items by normalized name key`() = runBlocking {
        // Regression: probe()'s JOIN once selected 6 columns into the 9-column shared reader —
        // every tier-3 name match threw "column index out of range" and the provider silently
        // contributed no streams (shipped in v1.4.24; TV was unaffected).
        val p = "prov-probe"
        XtreamMatchIndex.rebuild(
            p, MatchKind.MOVIE,
            listOf(bare(1, "The Devil Wears Prada 2 (2026)"), bare(2, "Unrelated Movie"))
        )
        val hits = XtreamMatchIndex.probe(p, MatchKind.MOVIE, "the devil wears prada 2")
        assertEquals(listOf(1), hits.map { it.sid })
        assertEquals("The Devil Wears Prada 2 (2026)", hits.single().name)
    }

    // ---- enricher over the real transport --------------------------------------------------

    @Test
    fun `asks once per item ever and writes the answer through`() = runBlocking {
        val base = servePanel(vodImage = { "https://img/$it.jpg" })
        val acc = account(base, "acct-once")
        XtreamMatchIndex.rebuild(acc.id, MatchKind.MOVIE, listOf(bare(101)))

        PosterEnricher.enqueue(acc, MatchKind.MOVIE, listOf(101))
        await("write-through") {
            runBlocking { XtreamMatchIndex.item(acc.id, MatchKind.MOVIE, 101)?.poster != null }
        }
        assertEquals("https://img/101.jpg", XtreamMatchIndex.item(acc.id, MatchKind.MOVIE, 101)?.poster)

        PosterEnricher.enqueue(acc, MatchKind.MOVIE, listOf(101))
        Thread.sleep(300)
        assertEquals(1, asked.count { it == "get_vod_info:101" })
    }

    @Test
    fun `panel with no artwork is asked once and left alone`() = runBlocking {
        val base = servePanel(vodImage = { null })
        val acc = account(base, "acct-noart")
        XtreamMatchIndex.rebuild(acc.id, MatchKind.MOVIE, listOf(bare(201)))

        PosterEnricher.enqueue(acc, MatchKind.MOVIE, listOf(201))
        await("the one ask") { asked.contains("get_vod_info:201") }
        PosterEnricher.enqueue(acc, MatchKind.MOVIE, listOf(201))
        Thread.sleep(300)
        assertEquals(1, asked.count { it == "get_vod_info:201" })
        assertNull(XtreamMatchIndex.item(acc.id, MatchKind.MOVIE, 201)?.poster)
    }

    @Test
    fun `series artwork comes from get_series_info cover`() = runBlocking {
        val base = servePanel(vodImage = { null }, cover = { "https://img/ser-$it.jpg" })
        val acc = account(base, "acct-series")
        XtreamMatchIndex.rebuild(acc.id, MatchKind.SERIES, listOf(bare(301)))

        PosterEnricher.enqueue(acc, MatchKind.SERIES, listOf(301))
        await("series write-through") {
            runBlocking { XtreamMatchIndex.item(acc.id, MatchKind.SERIES, 301)?.poster != null }
        }
        assertEquals("https://img/ser-301.jpg", XtreamMatchIndex.item(acc.id, MatchKind.SERIES, 301)?.poster)
    }

    @Test
    fun `live is never enriched`() = runBlocking {
        val base = servePanel(vodImage = { "https://img/x.jpg" })
        val acc = account(base, "acct-live")
        PosterEnricher.enqueue(acc, MatchKind.LIVE, listOf(1, 2, 3))
        Thread.sleep(300)
        assertTrue(asked.isEmpty())
    }

    @Test
    fun `transport failure is retryable, not burned`() = runBlocking {
        val base = servePanel(vodImage = { "https://img/$it.jpg" }, failFirst = 1)
        val acc = account(base, "acct-retry")
        XtreamMatchIndex.rebuild(acc.id, MatchKind.MOVIE, listOf(bare(401)))

        PosterEnricher.enqueue(acc, MatchKind.MOVIE, listOf(401))
        await("the failing ask") { asked.count { it == "get_vod_info:401" } == 1 }
        // A transport failure must not read as "panel has no art" — the next window retries.
        PosterEnricher.enqueue(acc, MatchKind.MOVIE, listOf(401))
        await("retry writes through") {
            runBlocking { XtreamMatchIndex.item(acc.id, MatchKind.MOVIE, 401)?.poster != null }
        }
        assertEquals(2, asked.count { it == "get_vod_info:401" })
    }
}
