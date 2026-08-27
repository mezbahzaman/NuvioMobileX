package com.nuvio.app.features.iptv

/**
 * Local, profile-scoped persistence of the Xtream accounts list as a JSON string.
 * Mirrors features/addons AddonStorage (SharedPreferences on Android, NSUserDefaults on iOS).
 *
 * ponytail: local only; Supabase cloud sync (like addons have) is the upgrade path.
 */
internal expect object XtreamAccountStorage {
    fun loadAccountsJson(profileId: Int): String?
    fun saveAccountsJson(profileId: Int, json: String)
    /** Recently-watched live channels (JSON), profile-scoped — for the Live TV Continue-Watching row. */
    fun loadRecentsJson(profileId: Int): String?
    fun saveRecentsJson(profileId: Int, json: String)
    /** Per-playlist last auto-refresh timestamps (JSON map id->epochMs), profile-scoped — P3 auto-refresh. */
    fun loadRefreshStateJson(profileId: Int): String?
    fun saveRefreshStateJson(profileId: Int, json: String)
    /** Hub's last provider + section tab (JSON XtreamHubSelection), profile-scoped — Fix 1 sticky
     *  selection. Device-local UI state: deliberately NOT part of the account sync payload. */
    fun loadHubSelectionJson(profileId: Int): String?
    fun saveHubSelectionJson(profileId: Int, json: String)
}
