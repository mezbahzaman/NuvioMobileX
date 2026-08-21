package com.nuvio.app.features.player

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins the PiP capability gate: API >= 26 alone is NOT enough -- the device must also expose
 * FEATURE_PICTURE_IN_PICTURE, else setPictureInPictureParams / enterPictureInPictureMode throw.
 */
class PictureInPictureSupportedTest {

    @Test
    fun requiresBothSdkAndFeature() {
        assertFalse(pictureInPictureSupported(sdkInt = 25, hasPictureInPictureFeature = true))
        // The exact field bug: API 26+ but the device lacks the PiP feature.
        assertFalse(pictureInPictureSupported(sdkInt = 26, hasPictureInPictureFeature = false))
        assertTrue(pictureInPictureSupported(sdkInt = 26, hasPictureInPictureFeature = true))
    }
}
