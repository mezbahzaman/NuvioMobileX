package com.nuvio.app.features.livetv

/**
 * What the Live TV player overlay shows, docked and fullscreen.
 *
 * The overlay is two separate things and they do not deserve the same treatment. The chrome — back,
 * the LIVE badge, the fullscreen toggle — lives on the edges of the frame and is navigation, so
 * docked it stays put. The centred play/pause button sits squarely over the picture, so leaving it
 * up for the whole programme means watching live TV through a button.
 *
 * Extracted so the docked and fullscreen rules can be compared side by side in a test rather than
 * inferred from two `if (fullscreen)` branches several hundred lines apart.
 */
internal object LiveTvOverlayPolicy {

    /** How long the centred control stays up after it is shown, before hiding itself again. */
    const val AUTO_HIDE_DELAY_MS = 3500L

    data class Input(
        val fullscreen: Boolean,
        val isPlaying: Boolean,
        /** The current show/hide flag, flipped by tapping the picture and reset on rotation. */
        val controlsShown: Boolean,
    )

    data class Visibility(
        /** Back, the LIVE badge and the fullscreen toggle. */
        val chromeVisible: Boolean,
        /** The big centred play/pause button drawn over the video. */
        val centreControlVisible: Boolean,
        /** Whether an auto-hide countdown should be running right now. */
        val autoHideScheduled: Boolean,
        /** Whether tapping the picture should toggle the controls. */
        val tapTogglesControls: Boolean,
    )

    fun evaluate(input: Input): Visibility = Visibility(
        // Docked, the chrome is the frame around a small player rather than something covering it,
        // and reaching fullscreen or back should never need a tap to reveal it first.
        chromeVisible = if (input.fullscreen) input.controlsShown else true,
        // The centred button is over the picture in both layouts, so it hides in both. Docked used
        // to pin it on for the whole programme, which is watching live TV through a button.
        centreControlVisible = input.controlsShown,
        autoHideScheduled = input.controlsShown && input.isPlaying,
        tapTogglesControls = true,
    )
}
