package com.asad.nexus.networkmonitor

import com.asad.nexus.networkmonitor.data.AppSpeed
import com.asad.nexus.networkmonitor.data.Prefs
import com.asad.nexus.networkmonitor.util.AppSelector
import com.asad.nexus.networkmonitor.util.Format
import com.asad.nexus.networkmonitor.util.SpeedMath
import org.junit.Assert.assertEquals
import org.junit.Test

class SpeedMathTest {
    @Test fun normalDelta() {
        assertEquals(1000L, SpeedMath.bytesPerSecond(0L, 2000L, 2_000_000_000L))
    }

    @Test fun counterResetGivesZero() {
        assertEquals(0L, SpeedMath.bytesPerSecond(5000L, 100L, 1_000_000_000L))
    }

    @Test fun noTrafficGivesZero() {
        assertEquals(0L, SpeedMath.bytesPerSecond(100L, 100L, 1_000_000_000L))
    }

    @Test fun zeroElapsedGivesZero() {
        assertEquals(0L, SpeedMath.bytesPerSecond(0L, 100L, 0L))
        assertEquals(0L, SpeedMath.bytesPerSecond(0L, 100L, -5L))
    }

    @Test fun formatSpeed() {
        assertEquals("512 B/s", Format.speed(512L))
        assertEquals("2 KB/s", Format.speed(2048L))
        assertEquals("1.50 MB/s", Format.speed(1_572_864L))
        assertEquals("0 B/s", Format.speed(-10L))
    }

    @Test fun shortSpeed() {
        assertEquals("68" to "KB/s", Format.shortSpeed(69_632L))
        assertEquals("1.5" to "MB/s", Format.shortSpeed(1_572_864L))
        assertEquals("15" to "MB/s", Format.shortSpeed(15L * 1_048_576L))
        assertEquals("500" to "B/s", Format.shortSpeed(500L))
    }

    @Test fun formatBytes() {
        assertEquals("100 B", Format.bytes(100L))
        assertEquals("1.0 MB", Format.bytes(1_048_576L))
        assertEquals("1.00 GB", Format.bytes(1_073_741_824L))
    }

    private fun app(pkg: String, rx: Long, tx: Long) = AppSpeed(1, pkg, pkg, rx, tx)

    @Test fun topTrafficSortsAndLimits() {
        val apps = listOf(app("a", 10, 0), app("b", 50, 0), app("c", 30, 0))
        val r = AppSelector.select(apps, emptySet(), false, Prefs.SORT_TOTAL, 2) { app(it, 0, 0) }
        assertEquals(listOf("b", "c"), r.map { it.packageName })
    }

    @Test fun selectedModeKeepsIdleApps() {
        val apps = listOf(app("a", 10, 0), app("b", 50, 0))
        val r = AppSelector.select(apps, setOf("a", "z"), true, Prefs.SORT_TOTAL, 5) { app(it, 0, 0) }
        assertEquals(listOf("a", "z"), r.map { it.packageName })
    }

    @Test fun sortByUpload() {
        val apps = listOf(app("a", 100, 1), app("b", 1, 9))
        val r = AppSelector.select(apps, emptySet(), false, Prefs.SORT_UP, 5) { app(it, 0, 0) }
        assertEquals("b", r.first().packageName)
    }
}
