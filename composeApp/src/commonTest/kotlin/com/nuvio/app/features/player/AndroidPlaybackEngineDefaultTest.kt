package com.nuvio.app.features.player

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Android defaults to the libmpv engine, not Auto (which starts on ExoPlayer).
 *
 * ExoPlayer macroblocks 4K / high-bitrate H.264 on Qualcomm hardware decoders while libmpv decodes
     * the same streams clean (device-confirmed; see research/nuvio-x-4k-avc-macroblocking). The pre-change
 * default was [AndroidPlaybackEngine.Auto]; asserting [AndroidPlaybackEngine.Libmpv] here is what
 * stops the default silently regressing to ExoPlayer.
 *
 * A libmpv default is only safe while the video-output default is `gpu` (not `gpu-next`): gpu-next is
 * libplacebo v7.360.0, which leaks one sync_file fd per rendered frame and dies on EMFILE. The second
 * assertion locks that precondition — flipping it back to gpu-next would make every fresh install leak.
 */
class AndroidPlaybackEngineDefaultTest {

    @Test
    fun `android default engine is libmpv`() {
        assertEquals(
            AndroidPlaybackEngine.Libmpv,
            PlayerSettingsUiState().androidPlaybackEngine,
            "Android must default to libmpv — ExoPlayer macroblocks 4K/high-bitrate H.264 on Qualcomm",
        )
    }

    @Test
    fun `libmpv video output default stays gpu so the libmpv default cannot leak fds`() {
        assertEquals(
            AndroidLibmpvVideoOutput.Gpu,
            PlayerSettingsUiState().androidLibmpvVideoOutput,
            "gpu-next leaks a sync_file fd per frame; a libmpv default demands the gpu renderer",
        )
    }
}
