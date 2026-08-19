package com.nuvio.app.features.player

/**
 * Which libmpv video output a playback session should use on Android.
 *
 * Exists because `vo=gpu-next` leaks one file descriptor per rendered frame on Android. gpu-next is
 * libplacebo, and the libplacebo we ship (v7.360.0) never calls `gl->DeleteSync` for the GL sync
 * object `gl_poll_callbacks()` creates each frame; on Android every GL sync is backed by a
 * `sync_file` fd, so the process leaks one per frame and dies on EMFILE at the 32768 ceiling —
 * ~22 min at 25fps, ~9 at 60. Proven twice: shipped telemetry 2026-08-16 (`sync_file` at 93-98% of
 * all descriptors, leak rate == frame rate; research/mpv-fence-fd-leak.md) and on-device 2026-08-18
 * on a Galaxy S24 Ultra, where a live gpu-next session climbed ~25 fences/s to 65% of the ceiling
 * while dma-buf stayed flat — and `gpu` on the same channel held a flat ~45 fences over 4 min.
 *
 * `vo=gpu` is mpv's own GL renderer and never goes through libplacebo, so it does not leak — NuvioTV
 * has always shipped `gpu` and holds 3-35 fences in the same telemetry window. Any session that runs
 * long enough hits the ceiling (a live channel, but equally a long movie), so the fix is not scoped
 * to live: every `gpu-next` request is served as `gpu` until we can ship libplacebo >= v7.360.1.
 *
 * `hwdec` is NOT the axis. A device test with `hwdec=mediacodec-copy` (frames copied to system RAM
 * instead of a `GL_TEXTURE_EXTERNAL_OES` SurfaceTexture import) still leaked at frame rate, because
 * the frames still render through gpu-next. See [PlayerSettingsRepository]'s androidLibmpvVideoOutput
 * KDoc for the upstream fix.
 *
 * Pure policy on purpose — it decides, it does not touch mpv. See [LivePlaybackFreezePolicy] for the
 * same shape.
 */
object LiveVideoOutputPolicy {

    /** mpv's own GL renderer. No libplacebo, so no per-frame sync-fence leak. */
    const val GPU: String = "gpu"

    /** libplacebo renderer. Higher quality, but v7.360.0 leaks a `sync_file` fd per frame on Android. */
    const val GPU_NEXT: String = "gpu-next"

    /**
     * The `vo` for a playback session, sanitizing the leaking `gpu-next` to `gpu` on Android.
     *
     * gpu-next crashes any session long enough to hit the fd ceiling — live OR a long movie — so
     * every `gpu-next` request is downgraded to `gpu` (mpv's own renderer: no libplacebo, no leak,
     * and a full-featured renderer NuvioTV already ships). Any other preference is already safe and
     * passes through untouched.
     *
     * The session-shape parameters are part of the call site's `PlatformPlayerSurface` contract but
     * are not consulted here: the leak is per rendered frame, not per content type, so live, VOD and
     * catch-up are all downgraded alike.
     *
     * This is a stopgap for our libplacebo v7.360.0. Once libmpv bundles libplacebo >= v7.360.1,
     * delete this and pass the preference straight through — gpu-next is safe from then on.
     */
    @Suppress("UNUSED_PARAMETER")
    fun videoOutputFor(
        isLive: Boolean,
        isCatchUpPlayback: Boolean,
        surface: String?,
        userPreference: String,
    ): String = if (userPreference == GPU_NEXT) GPU else userPreference
}
