package com.nuvio.app.core.contracts

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.player.LiveReplayLaunch
import kotlinx.coroutines.flow.Flow

/**
 * Firewall ports for the fork tab/route screens that App.kt hosts (IPTV hub, the docked Live TV
 * screen). App.kt owns the navigation and supplies the callbacks; the fork provides the screen.
 * No-op default (null section) renders nothing — correct when the feature is absent.
 */
interface IptvHubContent {
    @Composable
    fun Render(
        modifier: Modifier,
        onPosterClick: (MetaPreview) -> Unit,
        onPlayLiveChannel: (String) -> Unit,
        onFavoriteLiveChannel: (String) -> Unit,
        onAddProvider: () -> Unit,
        scrollToTopRequests: Flow<Unit>,
    )
}

interface LiveTvContent {
    @Composable
    fun Render(
        initialContentId: String,
        initialTitle: String,
        initialLogo: String?,
        initialReplay: LiveReplayLaunch?,
        onBack: () -> Unit,
        modifier: Modifier,
    )
}

object IptvHubContentAccess {
    private var content: IptvHubContent? = null
    fun register(c: IptvHubContent) { content = c }
    fun current(): IptvHubContent? = content
    fun resetForTest() { content = null }
}

object LiveTvContentAccess {
    private var content: LiveTvContent? = null
    fun register(c: LiveTvContent) { content = c }
    fun current(): LiveTvContent? = content
    fun resetForTest() { content = null }
}
