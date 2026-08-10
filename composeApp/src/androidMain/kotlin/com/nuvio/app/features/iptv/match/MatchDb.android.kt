package com.nuvio.app.features.iptv.match

import android.content.Context
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver

internal actual object MatchDbDriver {
    private var dbPath: String? = null

    /** Host unit tests have no Context — they install a bundled in-memory driver here. */
    internal var openForTests: (() -> SQLiteConnection)? = null

    /** Called once at app startup (MainActivity), like XtreamAccountStorage.initialize. */
    fun initialize(context: Context) {
        dbPath = context.getDatabasePath("xtream_match.db").also { it.parentFile?.mkdirs() }.absolutePath
    }

    actual fun openConnection(): SQLiteConnection =
        openForTests?.invoke()
            ?: AndroidSQLiteDriver().open(checkNotNull(dbPath) { "MatchDbDriver.initialize(context) not called" })
}
