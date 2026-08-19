package com.nuvio.app.arch

/**
 * Frozen crossing surface as of Phase 0 (2026-08-18). Generated from the exact R2b+R2d rule, NOT
 * hand-listed. The ratchet: this set only SHRINKS — each decomposition seam removes its files as it
 * ports them behind an extension point. A PR that adds a NEW crossing goes red. Do not add entries
 * to silence a rule; fix the crossing.
 */
object ArchBaseline {
    val crossings: Set<String> = setOf(
        "androidHostTest/kotlin/com/nuvio/app/features/player/MpvDemuxerBytesTest.kt",
        "androidHostTest/kotlin/com/nuvio/app/features/player/PlayerTargetBufferBytesTest.kt",
        "androidMain/kotlin/com/nuvio/app/MainActivity.kt",
        "androidMain/kotlin/com/nuvio/app/NuvioApplication.kt",
        "androidMain/kotlin/com/nuvio/app/core/ui/PlatformImageLoader.android.kt",
        "androidMain/kotlin/com/nuvio/app/features/player/PlayerEngine.android.kt",
        "commonMain/kotlin/com/nuvio/app/App.kt",
        "commonMain/kotlin/com/nuvio/app/core/storage/LocalAccountDataCleaner.kt",
        "commonMain/kotlin/com/nuvio/app/core/ui/ShelfComponents.kt",
        "commonMain/kotlin/com/nuvio/app/features/details/MetaDetailsRepository.kt",
        "commonMain/kotlin/com/nuvio/app/features/home/HomeScreen.kt",
        "commonMain/kotlin/com/nuvio/app/features/home/components/HomeCatalogSection.kt",
        "commonMain/kotlin/com/nuvio/app/features/player/PlayerScreenRuntimeEffects.kt",
        "commonMain/kotlin/com/nuvio/app/features/player/PlayerStreamsRepository.kt",
        "commonMain/kotlin/com/nuvio/app/features/profiles/ProfileRepository.kt",
        "commonMain/kotlin/com/nuvio/app/features/search/SearchRepository.kt",
        "commonMain/kotlin/com/nuvio/app/features/settings/AdvancedSettingsPage.kt",
        "commonMain/kotlin/com/nuvio/app/features/settings/SettingsScreen.kt",
        "commonMain/kotlin/com/nuvio/app/features/streams/StreamsRepository.kt",
        "commonMain/kotlin/com/nuvio/app/features/watchprogress/WatchProgressRepository.kt",
        "iosMain/kotlin/com/nuvio/app/core/ui/PlatformImageLoader.ios.kt",
        "iosMain/kotlin/com/nuvio/app/features/player/PlayerEngine.ios.kt",
    )
}
