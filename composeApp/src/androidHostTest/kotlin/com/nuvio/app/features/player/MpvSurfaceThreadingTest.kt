package com.nuvio.app.features.player

import android.view.SurfaceHolder
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Guards the "no mpv call ever runs on Main" rule where it is easiest to lose: the
 * SurfaceView callbacks, which run synchronously inside `View.layout`.
 *
 * `BaseMPVView.surfaceChanged` writes `android-surface-size` with a plain
 * `mpv_set_property`. That takes the mpv core lock, which a live demuxer holds for seconds,
 * so inheriting it stalls the main thread every time the surface resizes:
 *
 *   main  pthread_cond_wait <- mpv_set_property <- MPV.setPropertyString
 *         <- SurfaceView.updateSurface <- SurfaceView.setFrame <- View.layout
 *
 * That was a reproducible ANR on the docked <-> fullscreen toggle, where the surface
 * resizes on every transition. Deleting our override silently reintroduces it — the app
 * still compiles and still plays — so assert the override exists.
 */
class MpvSurfaceThreadingTest {

    @Test
    fun surfaceChangedIsOverriddenSoTheResizeWriteNeverRunsOnMain() {
        val viewClass = Class.forName("com.nuvio.app.features.player.NuvioLibmpvView")

        // getDeclaredMethod only finds methods declared on this class, so this throws
        // NoSuchMethodException the moment the override is removed and BaseMPVView's
        // blocking version is inherited again.
        val declaringClass = viewClass.getDeclaredMethod(
            "surfaceChanged",
            SurfaceHolder::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
        ).declaringClass

        assertEquals(
            viewClass,
            declaringClass,
            "surfaceChanged must stay overridden on NuvioLibmpvView: inheriting " +
                "BaseMPVView's version puts a blocking mpv_set_property on the main thread, " +
                "which ANRs whenever the surface resizes (docked <-> fullscreen).",
        )
    }

    @Test
    fun surfaceAttachAndDetachAreOverriddenSoLifecycleCallsNeverRunOnMain() {
        val viewClass = Class.forName("com.nuvio.app.features.player.NuvioLibmpvView")

        listOf("surfaceCreated", "surfaceDestroyed").forEach { methodName ->
            val declaringClass = viewClass.getDeclaredMethod(
                methodName,
                SurfaceHolder::class.java,
            ).declaringClass

            assertEquals(
                viewClass,
                declaringClass,
                "$methodName must stay overridden: BaseMPVView performs synchronous native " +
                    "surface lifecycle calls on Main instead of the serialized mpv control queue.",
            )
        }
    }
}
