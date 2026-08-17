package com.nuvio.app.features.player

import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pins the TS extractor flags that let ExoPlayer play raw IPTV live MPEG-TS.
 *
 * IPTV live `.ts` streams frequently lack Access Unit Delimiters and IDR keyframes at the join
 * point — you connect mid-GOP, because the stream has been running for hours. Without these flags
 * ExoPlayer waits for a keyframe that never arrives and buffers forever without reaching READY.
 * That symptom is exactly why mobile used to force libmpv for live, and forcing libmpv is what
 * dragged live through libplacebo (a per-frame fd leak) and a flat 96MB demuxer cache.
 *
 * Removing either flag makes live silently stop working — no crash, no error, just an infinite
 * spinner — so they are pinned here rather than left as a magic number at the call site.
 *
 * Same two flags NuvioTV sets (`PlayerMediaSourceFactory`), and a subset of StreamVault's
 * `liveMpegTsExtractorsFactory()`.
 */
class LiveTsExtractorFlagsTest {

    @Test
    fun `access unit detection is enabled`() {
        assertTrue(
            LIVE_TS_EXTRACTOR_FLAGS and DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS != 0,
            "without FLAG_DETECT_ACCESS_UNITS ExoPlayer cannot find frame boundaries in raw TS",
        )
    }

    @Test
    fun `non-IDR keyframes are accepted`() {
        assertTrue(
            LIVE_TS_EXTRACTOR_FLAGS and DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES != 0,
            "without FLAG_ALLOW_NON_IDR_KEYFRAMES a mid-GOP join buffers forever and never reaches READY",
        )
    }

    /** The pre-existing DTS audio flag must survive — it is why some providers' audio works at all. */
    @Test
    fun `HDMV DTS audio support is preserved`() {
        assertTrue(
            LIVE_TS_EXTRACTOR_FLAGS and DefaultTsPayloadReaderFactory.FLAG_ENABLE_HDMV_DTS_AUDIO_STREAMS != 0,
            "dropping FLAG_ENABLE_HDMV_DTS_AUDIO_STREAMS would silence DTS streams",
        )
    }

    /** Exactly the three we intend — a stray flag here changes parsing for every stream. */
    @Test
    fun `no unintended flags are set`() {
        val intended = DefaultTsPayloadReaderFactory.FLAG_ENABLE_HDMV_DTS_AUDIO_STREAMS or
            DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS or
            DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES
        assertEquals(intended, LIVE_TS_EXTRACTOR_FLAGS)
    }
}
