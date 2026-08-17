package com.nuvio.app.features.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The docked Live TV surface must not render through mpv's GL path.
 *
 * Root cause (research/mpv-fence-fd-leak.md, proven from shipped telemetry 2026-08-16): with a GL
 * `vo`, `hwdec=auto` resolves to mediacodec via `hwdec_surfacetexture.c`, whose per-frame acquire
 * fence transfers ownership to the caller. One descriptor leaks per rendered frame. Measured leak
 * rates at death were 24.6 / 29.6 / 58.1 / 56.9 fences per second against streams of 25 / 30 / 60 /
 * 57 fps — the leak rate IS the frame rate — ending in EMFILE at the 32768 ceiling. Every single
 * case was `playback_kind = live` on `playback_surface = livetv_docked`.
 */
class LiveVideoOutputPolicyTest {

    private val userPref = "gpu-next"

    /**
     * DISABLED after on-device verification, and this test now pins the disabled state on purpose.
     *
     * Playing live on the docked screen with `mediacodec_embed,gpu-next` produced a BLACK PICTURE on
     * an emulator (audio fine): mpv reported `Failed to create HW uploader for format yuv420p` ->
     * `Cannot convert decoder/filter output to any format supported by the output` -> `Could not
     * initialize video chain`. A vo fallback chain covers a vo that fails to INITIALISE; it does not
     * renegotiate when the decoder emits a software format. Shipping it would have replaced a
     * 9-minute crash with an immediate black screen for anyone whose device declines hwdec.
     *
     * Flip this test back when a candidate is verified on real hardware WITH the fd census.
     */
    @Test
    fun `docked live keeps the user's renderer until a verified alternative exists`() {
        assertEquals(
            userPref,
            LiveVideoOutputPolicy.videoOutputFor(
                isLive = true,
                isCatchUpPlayback = false,
                surface = LIVE_FREEZE_SURFACE_DOCKED,
                userPreference = userPref,
            ),
            "mediacodec_embed black-screens on software decode; see LiveVideoOutputPolicy",
        )
    }

    /**
     * `mediacodec_embed` renders ONLY hardware-decoded frames. mpv drops to software decoding after
     * 3 failed frames (`--hwdec-software-fallback=3`) and cannot show `yuv420p` through that vo, so
     * a BARE `mediacodec_embed` black-screens any device or codec that declines hwdec. The vo must
     * always be a fallback chain ending in a versatile renderer.
     */
    @Test
    fun `the direct output is a fallback chain never a bare vo`() {
        val vo = LiveVideoOutputPolicy.DIRECT_SURFACE_VO
        assertTrue(vo.startsWith("mediacodec_embed,"), "hardware path must come first, got '$vo'")
        val fallback = vo.substringAfter(',')
        assertTrue(
            fallback == "gpu-next" || fallback == "gpu",
            "software fallback must be a versatile renderer, got '$fallback'",
        )
    }

    /** The fullscreen player keeps the GL renderer: it needs panscan, OSD and libass. */
    @Test
    fun `the fullscreen player keeps the user's renderer`() {
        assertEquals(
            userPref,
            LiveVideoOutputPolicy.videoOutputFor(
                isLive = true,
                isCatchUpPlayback = false,
                surface = LIVE_FREEZE_SURFACE_PLAYER,
                userPreference = userPref,
            ),
        )
    }

    @Test
    fun `vod keeps the user's renderer`() {
        assertEquals(
            userPref,
            LiveVideoOutputPolicy.videoOutputFor(
                isLive = false,
                isCatchUpPlayback = false,
                surface = LIVE_FREEZE_SURFACE_DOCKED,
                userPreference = userPref,
            ),
        )
    }

    /**
     * Catch-up is archive content arriving down the live pipe. It follows the house discriminator
     * `live && !isCatchUpPlayback` (see CatchUpPlayback): it is seekable, it can carry subtitles,
     * and no catch-up session has been observed leaking. Deliberately conservative — telemetry will
     * say whether it needs the direct path too.
     */
    @Test
    fun `catch-up keeps the user's renderer`() {
        assertEquals(
            userPref,
            LiveVideoOutputPolicy.videoOutputFor(
                isLive = true,
                isCatchUpPlayback = true,
                surface = LIVE_FREEZE_SURFACE_DOCKED,
                userPreference = userPref,
            ),
        )
    }

    /** An unknown or absent surface must never silently opt into the restricted renderer. */
    @Test
    fun `an unknown surface keeps the user's renderer`() {
        assertEquals(
            userPref,
            LiveVideoOutputPolicy.videoOutputFor(
                isLive = true,
                isCatchUpPlayback = false,
                surface = null,
                userPreference = userPref,
            ),
        )
        assertEquals(
            userPref,
            LiveVideoOutputPolicy.videoOutputFor(
                isLive = true,
                isCatchUpPlayback = false,
                surface = "something_new",
                userPreference = userPref,
            ),
        )
    }

    /** A user preference of plain `gpu` must be honoured as the fallback leg, not overwritten. */
    @Test
    fun `a gpu preference is preserved off the docked surface`() {
        assertEquals(
            "gpu",
            LiveVideoOutputPolicy.videoOutputFor(
                isLive = true,
                isCatchUpPlayback = false,
                surface = LIVE_FREEZE_SURFACE_PLAYER,
                userPreference = "gpu",
            ),
        )
    }

    /**
     * mediacodec_embed draws no OSD and no libass, so an auto-selected embedded DVB/teletext track
     * would silently vanish. The docked screen exposes no subtitle UI at all, so the only honest
     * behaviour is to disable selection explicitly rather than let it depend on the renderer.
     */
    @Test
    fun `subtitle selection is disabled only where the renderer cannot draw it`() {
        assertEquals(
            "no",
            LiveVideoOutputPolicy.subtitleSelectionFor(LiveVideoOutputPolicy.DIRECT_SURFACE_VO),
            "the direct path cannot render subtitles, so do not silently select a track",
        )
        assertNull(
            LiveVideoOutputPolicy.subtitleSelectionFor("gpu-next"),
            "the GL renderer keeps mpv's own default track selection",
        )
    }
}
