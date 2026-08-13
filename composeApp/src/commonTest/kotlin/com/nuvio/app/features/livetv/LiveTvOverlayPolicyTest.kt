package com.nuvio.app.features.livetv

import com.nuvio.app.features.livetv.LiveTvOverlayPolicy.Input
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LiveTvOverlayPolicyTest {

    /** Defaults describe a channel playing docked with the controls just shown. */
    private fun input(
        fullscreen: Boolean = false,
        isPlaying: Boolean = true,
        controlsShown: Boolean = true,
    ) = Input(fullscreen = fullscreen, isPlaying = isPlaying, controlsShown = controlsShown)

    @Test
    fun `fullscreen hides everything once the controls are dismissed`() {
        val v = LiveTvOverlayPolicy.evaluate(input(fullscreen = true, controlsShown = false))
        assertFalse(v.chromeVisible)
        assertFalse(v.centreControlVisible)
        assertFalse(v.autoHideScheduled)
    }

    @Test
    fun `fullscreen counts down while playing`() {
        assertTrue(LiveTvOverlayPolicy.evaluate(input(fullscreen = true)).autoHideScheduled)
    }

    @Test
    fun `nothing auto-hides while paused`() {
        assertFalse(
            LiveTvOverlayPolicy.evaluate(input(fullscreen = true, isPlaying = false)).autoHideScheduled,
        )
        assertFalse(LiveTvOverlayPolicy.evaluate(input(isPlaying = false)).autoHideScheduled)
    }

    /**
     * Docked, the chrome is navigation and sits on the edges of the frame, so it stays. This is the
     * deliberate part of "always shown when docked" and it is not what was reported.
     */
    @Test
    fun `docked keeps the edge chrome up`() {
        val v = LiveTvOverlayPolicy.evaluate(input(controlsShown = false))
        assertTrue(v.chromeVisible)
    }

    /**
     * Reported on Discord as "Persistent pause item in portrait mode when opening live channel":
     * "the pause button ... When it comes to landscapes, it disappears, but not in portraits."
     *
     * The centred play/pause button is drawn over the middle of the picture. Fullscreen fades it out
     * after a few seconds; docked it was pinned on for the entire programme, so a phone in portrait
     * showed live TV with a button parked in the middle of it.
     */
    @Test
    fun `docked auto-hides the centred control while playing`() {
        val v = LiveTvOverlayPolicy.evaluate(input())
        assertTrue(v.autoHideScheduled, "the docked centre control must hide itself while playing")
    }

    @Test
    fun `docked centre control follows the show flag`() {
        assertFalse(
            LiveTvOverlayPolicy.evaluate(input(controlsShown = false)).centreControlVisible,
            "hiding the docked centre control must actually hide it",
        )
        assertTrue(LiveTvOverlayPolicy.evaluate(input(controlsShown = true)).centreControlVisible)
    }

    /** Having hidden it, the viewer needs a way back — the same tap that works fullscreen. */
    @Test
    fun `docked tap brings the centred control back`() {
        assertTrue(LiveTvOverlayPolicy.evaluate(input(controlsShown = false)).tapTogglesControls)
    }

    @Test
    fun `docked and fullscreen agree once the controls are shown and playing`() {
        val docked = LiveTvOverlayPolicy.evaluate(input(fullscreen = false))
        val full = LiveTvOverlayPolicy.evaluate(input(fullscreen = true))
        assertEquals(full.centreControlVisible, docked.centreControlVisible)
        assertEquals(full.autoHideScheduled, docked.autoHideScheduled)
    }
}
