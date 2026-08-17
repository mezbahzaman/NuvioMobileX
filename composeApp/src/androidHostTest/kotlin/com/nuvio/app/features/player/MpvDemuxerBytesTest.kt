package com.nuvio.app.features.player

import com.nuvio.app.core.memory.MemoryTier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The demuxer budget was a flat 64+32MiB sized on API level only — the same 96MiB native
 * allocation on a 2GB phone as on an S24 Ultra. These pin the fleet's locked tiering
 * decision (NuvioTV's `demuxerBytesFor`): LOW = 48+16MiB, everything else = 64+32MiB.
 *
 * The back buffer is cut first because mpv's own manual says it "will simply use as much
 * memory this option allows" on network streams — a guaranteed-full allocation that docked
 * live never seeks into (rewind is catch-up, a separate stream).
 */
class MpvDemuxerBytesTest {

    private val mib = 1024L * 1024L

    @Test
    fun `low tier gets 48 MiB forward and 16 MiB back`() {
        val bytes = demuxerBytesFor(MemoryTier.LOW)
        assertEquals(48L * mib, bytes.maxBytes)
        assertEquals(16L * mib, bytes.maxBackBytes)
    }

    @Test
    fun `mid and high tiers get 64 MiB forward and 32 MiB back`() {
        for (tier in listOf(MemoryTier.MID, MemoryTier.HIGH)) {
            val bytes = demuxerBytesFor(tier)
            assertEquals(64L * mib, bytes.maxBytes, "$tier forward")
            assertEquals(32L * mib, bytes.maxBackBytes, "$tier back")
        }
    }

    @Test
    fun `every tier spends more on the forward window than on seek-back`() {
        for (tier in MemoryTier.entries) {
            val bytes = demuxerBytesFor(tier)
            assertTrue(
                bytes.maxBytes > bytes.maxBackBytes,
                "$tier must favour the forward window that absorbs network jitter",
            )
        }
    }

    @Test
    fun `no tier exceeds the old flat budget`() {
        for (tier in MemoryTier.entries) {
            val bytes = demuxerBytesFor(tier)
            assertTrue(bytes.maxBytes + bytes.maxBackBytes <= 96L * mib, "$tier total")
        }
    }
}
