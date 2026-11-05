package com.asad.nexus.networkmonitor.data

/** Interval-averaged speed of one app, in bytes per second. */
data class AppSpeed(
    val uid: Int,
    val packageName: String,
    val label: String,
    val rxBps: Long,
    val txBps: Long,
)

data class SpeedSnapshot(
    val totalRxBps: Long = 0L,
    val totalTxBps: Long = 0L,
    val totalRxBytes: Long = 0L,
    val totalTxBytes: Long = 0L,
    val apps: List<AppSpeed> = emptyList(),
    val networkLabel: String = "-",
    /** False when TrafficStats reports UNSUPPORTED on this device. */
    val totalAvailable: Boolean = true,
    /** False when Usage Access is missing or NetworkStatsManager failed. */
    val perAppAvailable: Boolean = false,
    /** Length of the interval (seconds) the per-app speeds are averaged over. */
    val appWindowSec: Int = 0,
    val running: Boolean = false,
)
