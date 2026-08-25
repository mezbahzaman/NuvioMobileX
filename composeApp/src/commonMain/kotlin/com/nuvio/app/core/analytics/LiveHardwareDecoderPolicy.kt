package com.nuvio.app.core.analytics

/**
 * Whether live playback should open on the software engine (libmpv) instead of ExoPlayer's hardware
 * decoder, for a device whose video decoder video-stalls on live MPEG-TS far above the fleet
 * baseline.
 *
 * Derived from PostHog fleet telemetry (project 494529, 30 d, 2026-08-25). Live freeze rate =
 * `(live_preview_stall + live_playback_freeze) / playback_started`: Google TV Streamer (MediaTek
 * MT8696) 0.56, Fire TV 4K Max / AFTMM (MediaTek MT8696) 1.0, Skyworth UHD GTV STB 0.45, onn. 4K
 * Streaming Box (Amlogic) 0.38 — vs onn. 4K **Pro** only 0.08.
 *
 * The freeze is a plain `video_stalled` on raw `.ts` (not backward-PTS, `position_jumped_back = 0`)
 * and is not codec-specific — it correlates with the **hardware decoder / SoC**, so the neutral
 * capability to gate on is the decoder's identity (its name), with a small device-model allowlist
 * for the worst non-MediaTek offender. Deliberately narrow and tunable: libmpv carries a live
 * startup cost, so this only fires for the worst decoders/devices.
 *
 * On mobile the Android default engine is already libmpv, so this only bites for users who
 * explicitly picked Auto/ExoPlayer. Scope: live only. Pure: name + model passed in (the Android
 * caller resolves them from `MediaCodecList` / `Build.MODEL`), so it is unit-tested without a device.
 */
object LiveHardwareDecoderPolicy {

    /** Decoder-name fragments (lower-cased, `contains`) for SoC decoder families that video-stall on
     *  live `.ts` above the fleet baseline. MediaTek is the worst (both MT8696 devices top the fleet;
     *  `c2.mtk.*` / `OMX.MTK.*`), with documented live-TS/HEVC decoder bugs. */
    private val PROBLEM_DECODER_FRAGMENTS = listOf("c2.mtk.", "omx.mtk.", "mediatek")

    /** Exact device models (case-insensitive) that top the fleet but share a decoder name with
     *  well-behaved siblings (the Amlogic `onn. 4K Streaming Box` at 0.38 vs the Pro at 0.08, both
     *  `c2.amlogic.*`). Extend as telemetry warrants; keep it minimal. */
    private val PROBLEM_DEVICE_MODELS = setOf("onn. 4k streaming box")

    /**
     * True when live should open on libmpv for this device/decoder.
     *
     * @param deviceModel `Build.MODEL`, or null if unknown.
     * @param videoDecoderName the name of the video decoder that would be selected (e.g.
     *   `c2.mtk.avc.decoder`), or null if it could not be resolved.
     */
    fun preferLibmpvForLive(deviceModel: String?, videoDecoderName: String?): Boolean {
        val decoder = videoDecoderName?.lowercase()?.trim().orEmpty()
        if (decoder.isNotEmpty() && PROBLEM_DECODER_FRAGMENTS.any { decoder.contains(it) }) return true

        val model = deviceModel?.lowercase()?.trim().orEmpty()
        return model.isNotEmpty() && model in PROBLEM_DEVICE_MODELS
    }
}
