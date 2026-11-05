package com.asad.nexus.networkmonitor.util

import java.util.Locale

object Format {
    fun speed(bps: Long): String {
        val b = bps.coerceAtLeast(0L).toDouble()
        return when {
            b >= 1024.0 * 1024.0 -> String.format(Locale.US, "%.2f MB/s", b / (1024.0 * 1024.0))
            b >= 1024.0 -> String.format(Locale.US, "%.0f KB/s", b / 1024.0)
            else -> "${bps.coerceAtLeast(0L)} B/s"
        }
    }

    fun bytes(n: Long): String {
        val b = n.coerceAtLeast(0L).toDouble()
        return when {
            b >= 1024.0 * 1024.0 * 1024.0 -> String.format(Locale.US, "%.2f GB", b / (1024.0 * 1024.0 * 1024.0))
            b >= 1024.0 * 1024.0 -> String.format(Locale.US, "%.1f MB", b / (1024.0 * 1024.0))
            b >= 1024.0 -> String.format(Locale.US, "%.0f KB", b / 1024.0)
            else -> "${n.coerceAtLeast(0L)} B"
        }
    }
}
