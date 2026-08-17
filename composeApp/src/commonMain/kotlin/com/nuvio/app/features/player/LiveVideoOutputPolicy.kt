package com.nuvio.app.features.player

/**
 * Which libmpv video output a playback session should use.
 *
 * Exists because mpv's GL renderer leaks one file descriptor per rendered frame on Android. With a
 * GL `vo`, `hwdec=auto` resolves to mediacodec through `hwdec_surfacetexture.c`, which imports each
 * decoded frame into a `GL_TEXTURE_EXTERNAL_OES` texture; that import hands out an acquire fence
 * whose close is the caller's responsibility. Proven from shipped telemetry on 2026-08-16
 * (research/mpv-fence-fd-leak.md): at process death 98% of all descriptors were `sync_file` fences —
 * 32064 / 31794 / 31483 / 30593 of ~32.7k — while dma-buf counts stayed at their normal 188–251.
 * Fences divided by seconds of playback gave 24.6 / 29.6 / 58.1 / 56.9 against streams running at
 * 25 / 30 / 60 / 57 fps: **the leak rate is the frame rate**. The process dies on EMFILE once 32768
 * accumulate — 9 minutes at 60fps, 22 at 25fps.
 *
 * `mediacodec_embed` hands MediaCodec output straight to the Android Surface with no GL stage, so
 * there is no import and no fence. It also removes a full per-frame RGBA copy from graphics memory
 * (research/graphics-memory.md), which is the same code path by a different symptom.
 *
 * Pure policy on purpose — it decides, it does not touch mpv. See [LivePlaybackFreezePolicy] for
 * the same shape.
 */
object LiveVideoOutputPolicy {

    /**
     * A fallback CHAIN, never a bare `mediacodec_embed`.
     *
     * `mediacodec_embed` can only present hardware-decoded frames. mpv gives up on hardware
     * decoding after three consecutive failed frames (`--hwdec-software-fallback=3`) and the
     * resulting `yuv420p` cannot go through that vo at all — a bare `mediacodec_embed` would show a
     * black screen on any device or codec that declines hwdec. mpv falls through a comma-separated
     * vo list, so the versatile renderer behind it catches exactly that case.
     */
    const val DIRECT_SURFACE_VO: String = "mediacodec_embed,gpu-next"

    /**
     * The docked Live TV surface gets the direct path; everything else keeps the user's renderer.
     *
     * Scoped this narrowly for three reasons, all of which make the restriction free rather than a
     * trade:
     *  - it is where 100% of the observed leaks happened ([LIVE_FREEZE_SURFACE_DOCKED]);
     *  - that screen hardcodes `PlayerResizeMode.Fit`, which maps to `panscan 0.0` +
     *    `video-aspect-override no` — exactly what a plain Surface does natively. `Fill`/`Zoom`
     *    drive mpv *renderer* properties that `mediacodec_embed` would silently ignore, and they
     *    exist only on the fullscreen player route, which is untouched here;
     *  - it draws every control in Compose (`useNativeController = false`, no external subtitles,
     *    no track-selection UI), so losing mpv's OSD costs nothing.
     *
     * Catch-up follows the house discriminator `live && !isCatchUpPlayback` (see `CatchUpPlayback`):
     * it is seekable archive content that can legitimately carry subtitles, and no catch-up session
     * has been observed leaking. Deliberately conservative — the fence counts in `fd_inventory` will
     * say whether it needs the direct path too.
     */
    /**
     * ⚠️ CURRENTLY A PASS-THROUGH — the direct path is DISABLED after failing on-device.
     *
     * Verified on an emulator (Android 16, 2026-08-16) playing a live channel on the docked screen:
     * the chain applied cleanly (`mpv vo applied: 'mediacodec_embed,gpu-next'`) and the picture was
     * **black**, with audio fine. mpv's log says why:
     *
     *     [autoconvert:error] Failed to create HW uploader for format yuv420p
     *     [autoconvert:error] can't find video conversion for yuv420p
     *     [vf:fatal] Cannot convert decoder/filter output to any format supported by the output.
     *     [cplayer:fatal] Could not initialize video chain.
     *
     * The fallback chain does NOT rescue this. A comma-separated `vo` list covers a vo that fails to
     * INITIALISE; it does not renegotiate when the decoder turns out to emit a software format.
     * `mediacodec_embed` can only present MediaCodec surface frames, so any software decode — an
     * absent/refused hwdec, or our own `vf=format=yuv420p` option when the yuv420p setting is on —
     * produces a black screen rather than a fallback.
     *
     * So the fence-leak fix needs a different mechanism (see research/mpv-fence-fd-leak.md).
     * Candidate worth measuring on real hardware: keep `vo=gpu-next` and use
     * `hwdec=mediacodec-copy` on the live path — frames come back through system memory instead of
     * a SurfaceTexture import, which is where the per-frame acquire fence comes from. That trades a
     * copy for the leak and must be proven with the fd census, not assumed — assuming is exactly
     * what produced the black screen above.
     *
     * Kept as a policy object rather than deleted: the decision belongs here, the tests below
     * document what was tried, and re-enabling is a one-line change once a candidate is verified.
     */
    fun videoOutputFor(
        isLive: Boolean,
        isCatchUpPlayback: Boolean,
        surface: String?,
        userPreference: String,
    ): String = userPreference

    /**
     * `sid` for [videoOutput], or null to leave mpv's own default selection alone.
     *
     * The direct path draws no OSD and no libass, so an auto-selected embedded DVB or teletext
     * track would simply not appear. The docked screen exposes no subtitle UI, so nobody could turn
     * one on or off there anyway — disabling selection explicitly makes that deliberate instead of
     * an accident of which renderer happened to be chosen.
     */
    fun subtitleSelectionFor(videoOutput: String): String? =
        if (videoOutput == DIRECT_SURFACE_VO) "no" else null
}
