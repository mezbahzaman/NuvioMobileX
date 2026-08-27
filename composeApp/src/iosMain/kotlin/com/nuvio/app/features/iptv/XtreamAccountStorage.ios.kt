package com.nuvio.app.features.iptv

import platform.Foundation.NSUserDefaults

internal actual object XtreamAccountStorage {
    private const val accountsKey = "xtream_accounts"

    actual fun loadAccountsJson(profileId: Int): String? =
        NSUserDefaults.standardUserDefaults.stringForKey("${accountsKey}_$profileId")

    actual fun saveAccountsJson(profileId: Int, json: String) {
        NSUserDefaults.standardUserDefaults.setObject(json, forKey = "${accountsKey}_$profileId")
    }

    actual fun loadRecentsJson(profileId: Int): String? =
        NSUserDefaults.standardUserDefaults.stringForKey("xtream_live_recents_$profileId")

    actual fun saveRecentsJson(profileId: Int, json: String) {
        NSUserDefaults.standardUserDefaults.setObject(json, forKey = "xtream_live_recents_$profileId")
    }

    actual fun loadRefreshStateJson(profileId: Int): String? =
        NSUserDefaults.standardUserDefaults.stringForKey("xtream_refresh_state_$profileId")

    actual fun saveRefreshStateJson(profileId: Int, json: String) {
        NSUserDefaults.standardUserDefaults.setObject(json, forKey = "xtream_refresh_state_$profileId")
    }

    actual fun loadHubSelectionJson(profileId: Int): String? =
        NSUserDefaults.standardUserDefaults.stringForKey("xtream_hub_selection_$profileId")

    actual fun saveHubSelectionJson(profileId: Int, json: String) {
        NSUserDefaults.standardUserDefaults.setObject(json, forKey = "xtream_hub_selection_$profileId")
    }
}
