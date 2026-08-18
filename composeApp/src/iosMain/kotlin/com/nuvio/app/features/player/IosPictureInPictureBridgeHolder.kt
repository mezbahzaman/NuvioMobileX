package com.nuvio.app.features.player

/**
 * Holds the player bridge that is currently on screen, so the Picture-in-Picture `expect`/`actual`
 * effects can reach it.
 *
 * `ManagePlayerPictureInPicture` and `rememberIsInPictureInPicture` are declared in commonMain and
 * take no bridge argument (Android reads PiP state off the Activity), but on iOS every PiP call has
 * to go through the Swift bridge that owns the player view controller. This is the seam.
 *
 * Single-slot rather than a stack: only one player surface is composed at a time on iOS, and a
 * stale entry would send PiP calls to a destroyed bridge. Registration is last-writer-wins and
 * unregistration only clears when the leaving bridge is still the registered one, so a handoff
 * during a screen transition cannot blank a bridge that has already taken over.
 */
object IosPictureInPictureBridgeHolder {
    private var current: NuvioPlayerBridge? = null

    /**
     * Last value asked for, remembered so it survives arriving before the bridge does.
     *
     * Ordering matters here: LiveTvScreen declares `ManagePlayerPictureInPicture` well above the
     * `PlatformPlayerSurface` that creates and registers the bridge, so on a cold open the enable
     * call lands while [current] is still null. Without this latch the flag was simply dropped and
     * the capture never installed — PiP silently did nothing, which is exactly what the simulator
     * run showed (no "PiP frame capture enabled" line at all).
     */
    private var desiredEnabled = false

    fun register(bridge: NuvioPlayerBridge) {
        current = bridge
        // Replay the latched setting: the bridge stores it and applies it to the player view
        // controller at construction, which must happen before viewDidLoad installs the capture.
        bridge.setPictureInPictureEnabled(desiredEnabled)
    }

    fun unregister(bridge: NuvioPlayerBridge) {
        if (current === bridge) current = null
    }

    /** True when a player is on screen and iOS reports PiP is usable on this device. */
    fun isSupported(): Boolean = current?.isPictureInPictureSupported() ?: false

    fun isActive(): Boolean = current?.isPictureInPictureActive() ?: false

    fun setEnabled(enabled: Boolean) {
        desiredEnabled = enabled
        current?.setPictureInPictureEnabled(enabled)
    }

    fun start() {
        current?.startPictureInPicture()
    }

    fun stop() {
        current?.stopPictureInPicture()
    }
}
