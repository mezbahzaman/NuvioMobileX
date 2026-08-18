package com.nuvio.app.features.player

/**
 * Remembers whether Picture-in-Picture should be on, so the answer survives arriving before there
 * is anything to apply it to.
 *
 * Why this exists: on iOS the PiP setting reaches a Swift bridge that only exists once the player
 * surface has composed. `LiveTvScreen` declares `ManagePlayerPictureInPicture` well above the
 * `PlatformPlayerSurface` that creates and registers that bridge, so on a cold open the enable call
 * lands with no target. The first version simply dropped it: the capture was never installed and
 * PiP silently did nothing — no error, no log, nothing to notice until you swipe home and no window
 * appears. Caught on an iOS simulator run, 2026-08-18.
 *
 * Pure and platform-free so it tests on both mobile runners without a bridge, a player, or UIKit —
 * the 56-method `NuvioPlayerBridge` is not something you want to fake.
 */
class PictureInPictureEnableLatch {
    private var desiredEnabled = false
    private var hasTarget = false

    /** What the UI last asked for, whether or not anything was listening at the time. */
    val desired: Boolean get() = desiredEnabled

    /**
     * Records what the UI wants.
     *
     * @return true when a target is attached and the caller should push the value to it now.
     *   False means it was latched for [attach] to replay later.
     */
    fun setDesired(enabled: Boolean): Boolean {
        desiredEnabled = enabled
        return hasTarget
    }

    /**
     * A target (the player bridge) became available.
     *
     * @return the value to replay onto it, which is the whole point of this class.
     */
    fun attach(): Boolean {
        hasTarget = true
        return desiredEnabled
    }

    /**
     * The target went away. The desired value is deliberately KEPT: the player closing does not mean
     * the user turned PiP off, and the next player must come up with the same setting.
     */
    fun detach() {
        hasTarget = false
    }
}
