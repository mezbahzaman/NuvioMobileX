package com.nuvio.app.arch

/**
 * Frozen crossing surface as of Phase 0 (2026-08-18). Generated from the exact R2b+R2d rule, NOT
 * hand-listed. The ratchet: this set only SHRINKS — each decomposition seam removes its files as it
 * ports them behind an extension point. A PR that adds a NEW crossing goes red. Do not add entries
 * to silence a rule; fix the crossing.
 *
 * S10a (2026-08-19) cleared the 5 memory-only crossings behind the MemoryPort; S10b cleared
 * PlayerEngine.android/ios (rejoinsLiveEdge moved to the neutral features.player domain). Remaining:
 * MainActivity (startup-DB init, S10c — needs the androidMain wiring exemption).
 */
object ArchBaseline {
    val crossings: Set<String> = setOf(
        "androidMain/kotlin/com/nuvio/app/MainActivity.kt",
    )
}
