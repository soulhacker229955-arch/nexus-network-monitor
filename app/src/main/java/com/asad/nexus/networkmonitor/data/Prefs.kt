package com.asad.nexus.networkmonitor.data

import android.content.Context

/** Everything the notification needs, read once per sample. */
data class NotifConfig(
    val compact: Boolean,
    val rows: Int,
    val showTotal: Boolean,
    val showApps: Boolean,
    val showDown: Boolean,
    val showUp: Boolean,
    val sort: Int,
    val onlySelected: Boolean,
    val selected: Set<String>,
    val iconMode: Int,
    val refreshMs: Long,
)

/** Small local settings store. Stays on the device. */
object Prefs {
    private const val FILE = "nexus_prefs"
    const val MAX_ROWS = 5

    const val SORT_TOTAL = 0
    const val SORT_DOWN = 1
    const val SORT_UP = 2

    const val ICON_DOWN = 0
    const val ICON_UP = 1
    const val ICON_COMBINED = 2
    const val ICON_OFF = 3

    const val K_COMPACT = "compact"
    const val K_ROWS = "rows"
    const val K_SHOW_TOTAL = "show_total"
    const val K_SHOW_APPS = "show_apps"
    const val K_SHOW_DOWN = "show_down"
    const val K_SHOW_UP = "show_up"
    const val K_SORT = "sort"
    const val K_ONLY_SELECTED = "only_selected"
    const val K_SELECTED = "selected_apps"
    const val K_ICON = "icon_mode"
    const val K_REFRESH_S = "refresh_s"

    private fun sp(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun bool(c: Context, key: String, def: Boolean): Boolean = sp(c).getBoolean(key, def)
    fun setBool(c: Context, key: String, v: Boolean) = sp(c).edit().putBoolean(key, v).apply()
    fun int(c: Context, key: String, def: Int): Int = sp(c).getInt(key, def)
    fun setInt(c: Context, key: String, v: Int) = sp(c).edit().putInt(key, v).apply()

    fun selectedApps(c: Context): Set<String> =
        sp(c).getStringSet(K_SELECTED, emptySet())?.toSet() ?: emptySet()

    fun setSelectedApps(c: Context, v: Set<String>) =
        sp(c).edit().putStringSet(K_SELECTED, HashSet(v)).apply()

    fun config(c: Context): NotifConfig = NotifConfig(
        compact = bool(c, K_COMPACT, false),
        rows = int(c, K_ROWS, 4).coerceIn(1, MAX_ROWS),
        showTotal = bool(c, K_SHOW_TOTAL, true),
        showApps = bool(c, K_SHOW_APPS, true),
        showDown = bool(c, K_SHOW_DOWN, true),
        showUp = bool(c, K_SHOW_UP, true),
        sort = int(c, K_SORT, SORT_TOTAL).coerceIn(0, 2),
        onlySelected = bool(c, K_ONLY_SELECTED, false),
        selected = selectedApps(c),
        iconMode = int(c, K_ICON, ICON_DOWN).coerceIn(0, 3),
        refreshMs = int(c, K_REFRESH_S, 1).coerceIn(1, 3) * 1000L,
    )
}
