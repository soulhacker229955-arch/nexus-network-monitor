package com.asad.nexus.networkmonitor.data

import android.content.Context

/** Small local settings store (phase 1). Stays on the device. */
object Prefs {
    private const val FILE = "nexus_prefs"
    private const val KEY_COMPACT = "compact"
    private const val KEY_ROWS = "rows"
    const val MAX_ROWS = 5

    private fun sp(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun compact(c: Context): Boolean = sp(c).getBoolean(KEY_COMPACT, false)
    fun setCompact(c: Context, v: Boolean) = sp(c).edit().putBoolean(KEY_COMPACT, v).apply()

    fun maxRows(c: Context): Int = sp(c).getInt(KEY_ROWS, 4).coerceIn(1, MAX_ROWS)
    fun setMaxRows(c: Context, v: Int) = sp(c).edit().putInt(KEY_ROWS, v.coerceIn(1, MAX_ROWS)).apply()
}
