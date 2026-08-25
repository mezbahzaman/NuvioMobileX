package com.nuvio.app.core.analytics

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins the telemetry-derived live hardware-decoder gate (PostHog 494529, 30 d, 2026-08-25): the
 * worst-freezing decoders/devices open live on libmpv, well-behaved ones stay on ExoPlayer, and the
 * default is conservative — all without a device or a `MediaCodecList`.
 */
class LiveHardwareDecoderPolicyTest {

    @Test
    fun `MediaTek Codec2 decoder opens live on libmpv`() {
        assertTrue(
            LiveHardwareDecoderPolicy.preferLibmpvForLive(
                deviceModel = "Google TV Streamer",
                videoDecoderName = "c2.mtk.avc.decoder",
            ),
            "c2.mtk.* is the MT8696 decoder (Google TV Streamer / Fire TV 4K Max) — the fleet's worst",
        )
    }

    @Test
    fun `legacy MediaTek OMX decoder opens live on libmpv`() {
        assertTrue(
            LiveHardwareDecoderPolicy.preferLibmpvForLive(
                deviceModel = "AFTMM",
                videoDecoderName = "OMX.MTK.VIDEO.DECODER.AVC",
            ),
        )
    }

    @Test
    fun `MediaTek gate is case-insensitive`() {
        assertTrue(
            LiveHardwareDecoderPolicy.preferLibmpvForLive(
                deviceModel = null,
                videoDecoderName = "C2.MTK.HEVC.DECODER",
            ),
        )
    }

    @Test
    fun `the Onn 4K Streaming Box opens live on libmpv by model`() {
        assertTrue(
            LiveHardwareDecoderPolicy.preferLibmpvForLive(
                deviceModel = "onn. 4K Streaming Box",
                videoDecoderName = "c2.amlogic.avc.decoder",
            ),
            "0.38 fleet freeze rate on Amlogic; decoder name is shared with well-behaved siblings",
        )
    }

    @Test
    fun `the Onn 4K Pro stays on ExoPlayer`() {
        assertFalse(
            LiveHardwareDecoderPolicy.preferLibmpvForLive(
                deviceModel = "onn. 4K Pro",
                videoDecoderName = "c2.amlogic.avc.decoder",
            ),
            "onn. 4K Pro freezes at only 0.08 — same Amlogic decoder but not a problem device",
        )
    }

    @Test
    fun `a Qualcomm decoder stays on ExoPlayer`() {
        assertFalse(
            LiveHardwareDecoderPolicy.preferLibmpvForLive(
                deviceModel = "SHIELD Android TV",
                videoDecoderName = "c2.qti.avc.decoder",
            ),
        )
    }

    @Test
    fun `a plain Amlogic device not on the allowlist stays on ExoPlayer`() {
        assertFalse(
            LiveHardwareDecoderPolicy.preferLibmpvForLive(
                deviceModel = "Chromecast",
                videoDecoderName = "c2.amlogic.avc.decoder",
            ),
            "Chromecast at 0.21 is not gated — only the worst offenders are",
        )
    }

    @Test
    fun `unknown decoder and model defaults to ExoPlayer`() {
        assertFalse(
            LiveHardwareDecoderPolicy.preferLibmpvForLive(deviceModel = null, videoDecoderName = null),
        )
    }

    @Test
    fun `blank inputs default to ExoPlayer`() {
        assertFalse(
            LiveHardwareDecoderPolicy.preferLibmpvForLive(deviceModel = "  ", videoDecoderName = "  "),
        )
    }
}
