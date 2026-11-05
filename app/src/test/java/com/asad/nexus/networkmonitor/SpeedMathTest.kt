package com.asad.nexus.networkmonitor

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

    @Test fun formatBytes() {
        assertEquals("100 B", Format.bytes(100L))
        assertEquals("1.0 MB", Format.bytes(1_048_576L))
        assertEquals("1.00 GB", Format.bytes(1_073_741_824L))
    }
}
