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

    /** The enable/replay decision, extracted so it is testable without a 56-method bridge fake. */
    private val latch = PictureInPictureEnableLatch()

    fun register(bridge: NuvioPlayerBridge) {
        current = bridge
        // Replay the latched setting: the bridge stores it and applies it to the player view
        // controller at construction, which must happen before viewDidLoad installs the capture.
        bridge.setPictureInPictureEnabled(latch.attach())
    }

    fun unregister(bridge: NuvioPlayerBridge) {
        if (current === bridge) {
            current = null
            latch.detach()
        }
    }

    /** True when a player is on screen and iOS reports PiP is usable on this device. */
    fun isSupported(): Boolean = current?.isPictureInPictureSupported() ?: false

    fun isActive(): Boolean = current?.isPictureInPictureActive() ?: false

    fun setEnabled(enabled: Boolean) {
        if (latch.setDesired(enabled)) current?.setPictureInPictureEnabled(enabled)
    }

    fun start() {
        current?.startPictureInPicture()
    }

    fun stop() {
        current?.stopPictureInPicture()
    }
}
