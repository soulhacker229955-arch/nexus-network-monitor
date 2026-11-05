package com.asad.nexus.networkmonitor.util

object SpeedMath {
    /**
     * Bytes per second from two counter readings and the elapsed monotonic time.
     * Returns 0 for counter resets (negative diff), no traffic, or invalid elapsed time.
     */
    fun bytesPerSecond(prev: Long, cur: Long, elapsedNanos: Long): Long {
        if (elapsedNanos <= 0L) return 0L
        val diff = cur - prev
        if (diff <= 0L) return 0L
        return (diff.toDouble() * 1_000_000_000.0 / elapsedNanos.toDouble()).toLong()
    }
}
