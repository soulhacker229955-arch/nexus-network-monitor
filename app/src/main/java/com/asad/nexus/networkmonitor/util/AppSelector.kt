package com.asad.nexus.networkmonitor.util

import com.asad.nexus.networkmonitor.data.AppSpeed
import com.asad.nexus.networkmonitor.data.Prefs

/** Chooses which apps appear in the notification, sorted and limited. Pure logic. */
object AppSelector {
    fun select(
        apps: List<AppSpeed>,
        selected: Set<String>,
        onlySelected: Boolean,
        sort: Int,
        rows: Int,
        idle: (String) -> AppSpeed,
    ): List<AppSpeed> {
        val base: List<AppSpeed> = if (onlySelected) {
            val active = apps.filter { it.packageName in selected }
            val seen = active.map { it.packageName }.toSet()
            // Selected apps with no traffic are shown with 0 B/s (real: nothing was measured).
            active + selected.filter { it !in seen }.sorted().map { idle(it) }
        } else {
            apps
        }
        return base.sortedByDescending { key(it, sort) }.take(rows)
    }

    private fun key(a: AppSpeed, sort: Int): Long = when (sort) {
        Prefs.SORT_DOWN -> a.rxBps
        Prefs.SORT_UP -> a.txBps
        else -> a.rxBps + a.txBps
    }
}
