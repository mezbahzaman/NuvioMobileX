package com.nuvio.app.features.player

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The live path must not render through `gpu-next`.
 *
 * Root cause (research/mpv-fence-fd-leak.md; re-proven on-device 2026-08-18): `gpu-next` is
 * libplacebo v7.360.0, which creates a GL sync per frame and never calls `DeleteSync`. On Android
 * every GL sync is a `sync_file` fd, so the process leaks one per rendered frame and dies on EMFILE
 * at 32768 — the leak rate is the frame rate. A live channel runs long enough to hit it; on a Galaxy
 * S24 Ultra a live gpu-next session climbed ~25 fences/s to 65% of the ceiling. `vo=gpu` (mpv's own
 * renderer, no libplacebo) does not leak, which is why NuvioTV — always `gpu` — stays at 3-35.
 *
 * `hwdec` is not the axis: a device test on `mediacodec-copy` still leaked, because the frames still
 * render through gpu-next. So the policy switches the renderer, not the decoder.
 */
class LiveVideoOutputPolicyTest {

    private val gpuNext = LiveVideoOutputPolicy.GPU_NEXT

    /**
     * THE regression test. A live session on `gpu-next` is the leak; it must be downgraded to `gpu`.
     * The pre-fix behaviour passed the preference straight through (`gpu-next`) — asserting `gpu`
     * here is what stops the leak silently returning.
     */
    @Test
    fun `a live gpu-next session is downgraded to gpu`() {
        assertEquals(
            "gpu",
            LiveVideoOutputPolicy.videoOutputFor(
                isLive = true,
                isCatchUpPlayback = false,
                surface = LIVE_FREEZE_SURFACE_DOCKED,
                userPreference = gpuNext,
            ),
            "live gpu-next leaks a sync_file fd per frame; downgrade to gpu (see LiveVideoOutputPolicy)",
        )
    }

    /**
     * The leak is renderer-wide (every rendered frame), not specific to the docked surface where it
     * was first observed — so fullscreen live on gpu-next is downgraded too. `surface` is not
     * consulted.
     */
    @Test
    fun `live gpu-next is downgraded regardless of surface`() {
        for (surface in listOf(LIVE_FREEZE_SURFACE_PLAYER, LIVE_FREEZE_SURFACE_DOCKED, null, "something_new")) {
            assertEquals(
                "gpu",
                LiveVideoOutputPolicy.videoOutputFor(
                    isLive = true,
                    isCatchUpPlayback = false,
                    surface = surface,
                    userPreference = gpuNext,
                ),
                "surface=$surface must not exempt live gpu-next from the downgrade",
            )
        }
    }

    /** VOD is downgraded too: a long movie on gpu-next hits the same fd ceiling as live. */
    @Test
    fun `vod gpu-next is also downgraded to gpu`() {
        assertEquals(
            "gpu",
            LiveVideoOutputPolicy.videoOutputFor(
                isLive = false,
                isCatchUpPlayback = false,
                surface = LIVE_FREEZE_SURFACE_DOCKED,
                userPreference = gpuNext,
            ),
            "a long movie on gpu-next leaks to EMFILE just like live",
        )
    }

    /** Catch-up renders frames too and can be long, so it is downgraded as well. */
    @Test
    fun `catch-up gpu-next is also downgraded to gpu`() {
        assertEquals(
            "gpu",
            LiveVideoOutputPolicy.videoOutputFor(
                isLive = true,
                isCatchUpPlayback = true,
                surface = LIVE_FREEZE_SURFACE_DOCKED,
                userPreference = gpuNext,
            ),
        )
    }

    /** A user already on plain `gpu` is safe and unchanged, on any path. */
    @Test
    fun `a gpu preference passes through untouched`() {
        assertEquals(
            "gpu",
            LiveVideoOutputPolicy.videoOutputFor(
                isLive = true,
                isCatchUpPlayback = false,
                surface = LIVE_FREEZE_SURFACE_DOCKED,
                userPreference = "gpu",
            ),
        )
    }

    /** Only `gpu-next` is the leaking renderer; any other preference is left alone even on live. */
    @Test
    fun `a non-gpu-next preference is never rewritten`() {
        assertEquals(
            "some_future_vo",
            LiveVideoOutputPolicy.videoOutputFor(
                isLive = true,
                isCatchUpPlayback = false,
                surface = LIVE_FREEZE_SURFACE_DOCKED,
                userPreference = "some_future_vo",
            ),
        )
    }

    /**
     * Pin the exact mpv strings. mpv validates a `vo` name only at runtime, so a typo here would
     * black-screen or silently keep the leaking renderer.
     */
    @Test
    fun `the vo constants are the exact mpv option strings`() {
        assertEquals("gpu", LiveVideoOutputPolicy.GPU)
        assertEquals("gpu-next", LiveVideoOutputPolicy.GPU_NEXT)
    }
}
