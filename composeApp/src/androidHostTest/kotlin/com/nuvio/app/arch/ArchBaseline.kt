package com.nuvio.app.arch

/**
 * Frozen crossing surface as of Phase 0 (2026-08-18). Generated from the exact R2b+R2d rule, NOT
 * hand-listed. The ratchet: this set only SHRINKS — each decomposition seam removes its files as it
 * ports them behind an extension point. A PR that adds a NEW crossing goes red. Do not add entries
 * to silence a rule; fix the crossing.
 *
 * FULLY DRAINED 2026-08-19 (S10a memory, S10b rejoinsLiveEdge, S10c Android startup registry): every
 * commonMain and platform crossing now goes through an extension point. The set is EMPTY — the
 * firewall is absolute from here: any new fork reference from non-fork/non-wiring code goes red.
 */
object ArchBaseline {
    val crossings: Set<String> = emptySet()
}
