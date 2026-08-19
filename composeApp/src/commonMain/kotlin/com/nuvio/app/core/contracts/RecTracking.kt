package com.nuvio.app.core.contracts

/**
 * Neutral rec-telemetry port (seam S11, partial). Shared code reports playback progress with raw
 * primitives; the fork adapter derives the rec content-type and forwards to core/rec. Keeps shared
 * code off features/core.rec.
 */
interface RecPlaybackReporter {
    fun onProgress(
        itemId: String,
        contentType: String,
        season: Int?,
        episode: Int?,
        positionMs: Long,
        durationMs: Long,
    )
}

object RecTrackingAccess {
    private var reporterInstance: RecPlaybackReporter? = null
    val reporter: RecPlaybackReporter
        get() = reporterInstance ?: error("RecPlaybackReporter not registered — see FeatureWiring")
    fun register(reporter: RecPlaybackReporter) { reporterInstance = reporter }
}
