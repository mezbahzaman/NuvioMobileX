package com.nuvio.app.features.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Regression cover for the iOS PiP wiring bug found on the simulator, 2026-08-18.
 *
 * The enable call reaches the Swift bridge before that bridge exists, because LiveTvScreen declares
 * ManagePlayerPictureInPicture above the player surface that registers it. The first version dropped
 * the value on the floor, so the frame capture was never installed and PiP silently did nothing.
 *
 * The first test below is the one that failed on the old behaviour.
 */
class PictureInPictureEnableLatchTest {

    @Test
    fun `enable arriving before the bridge is replayed on attach`() {
        val latch = PictureInPictureEnableLatch()

        // The cold-open order: the UI asks for PiP while nothing is listening yet.
        val appliedImmediately = latch.setDesired(true)
        assertFalse(appliedImmediately, "nothing is attached yet - there is no target to push to")

        // The player surface composes and registers its bridge.
        assertTrue(latch.attach(), "the latched request must be replayed onto the arriving bridge")
    }

    @Test
    fun `enable arriving after the bridge is applied immediately`() {
        val latch = PictureInPictureEnableLatch()
        latch.attach()

        assertTrue(latch.setDesired(true), "a target is attached so the caller should push it now")
        assertEquals(true, latch.desired, "and the value is remembered")
    }

    @Test
    fun `disable is latched the same way as enable`() {
        val latch = PictureInPictureEnableLatch()
        latch.setDesired(false)

        assertFalse(latch.attach(), "a bridge arriving must not switch PiP on by default")
    }

    @Test
    fun `the last request wins before a bridge attaches`() {
        val latch = PictureInPictureEnableLatch()
        latch.setDesired(true)
        latch.setDesired(false)

        assertFalse(latch.attach(), "replaying a superseded value would fight the user's setting")
    }

    @Test
    fun `the setting survives the player closing and reopening`() {
        val latch = PictureInPictureEnableLatch()
        latch.setDesired(true)
        latch.attach()

        // Player closes.
        latch.detach()
        assertEquals(true, latch.desired, "closing a player is not the user turning PiP off")

        // Next player opens and must come up with PiP still on.
        assertTrue(latch.attach(), "the next bridge gets the same setting replayed")
    }

    @Test
    fun `after detach a request is latched again rather than pushed`() {
        val latch = PictureInPictureEnableLatch()
        latch.attach()
        latch.detach()

        assertFalse(
            latch.setDesired(true),
            "with no bridge attached this must latch, not claim it was applied",
        )
        assertTrue(latch.attach(), "and replay when the next bridge arrives")
    }
}
