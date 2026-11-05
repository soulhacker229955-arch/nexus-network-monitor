package com.asad.nexus.networkmonitor.engine

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.SystemClock
import android.util.Log
import com.asad.nexus.networkmonitor.data.AppSpeed
import com.asad.nexus.networkmonitor.data.SpeedSnapshot
import com.asad.nexus.networkmonitor.util.Permissions
import com.asad.nexus.networkmonitor.util.SpeedMath

/**
 * Total speed: TrafficStats total counters, sampled on every call (monotonic clock).
 * Per-app speed: NetworkStatsManager cumulative per-UID bytes, queried every few seconds;
 * speed = byte difference between two queries / elapsed time. This is an interval average
 * and can lag, because Android flushes per-UID statistics in batches. It is NOT true
 * real-time per-app throughput (Android does not expose that without a VPN).
 */
class NetworkEngine(private val context: Context, private val cache: AppInfoCache) {
    private val nsm = context.getSystemService(NetworkStatsManager::class.java)
    private val cm = context.getSystemService(ConnectivityManager::class.java)
    private val startMs = System.currentTimeMillis()

    private var prevNanos = 0L
    private var prevRx = -1L
    private var prevTx = -1L
    private var totalRxBps = 0L
    private var totalTxBps = 0L

    private var prevAppNanos = 0L
    private var prevUid: Map<Int, LongArray>? = null
    private var apps: List<AppSpeed> = emptyList()
    private var appWindowSec = 0
    private var perAppOk = false

    fun sample(topN: Int): SpeedSnapshot {
        val nowNanos = SystemClock.elapsedRealtimeNanos()
        val rx = TrafficStats.getTotalRxBytes()
        val tx = TrafficStats.getTotalTxBytes()
        val totalOk = rx >= 0L && tx >= 0L

        if (totalOk && prevRx >= 0L && prevTx >= 0L) {
            val elapsed = nowNanos - prevNanos
            totalRxBps = SpeedMath.bytesPerSecond(prevRx, rx, elapsed)
            totalTxBps = SpeedMath.bytesPerSecond(prevTx, tx, elapsed)
        } else {
            totalRxBps = 0L
            totalTxBps = 0L
        }
        if (totalOk) {
            prevRx = rx
            prevTx = tx
        }
        prevNanos = nowNanos

        sampleApps(nowNanos, topN)

        return SpeedSnapshot(
            totalRxBps = totalRxBps,
            totalTxBps = totalTxBps,
            totalRxBytes = if (totalOk) rx else 0L,
            totalTxBytes = if (totalOk) tx else 0L,
            apps = apps,
            networkLabel = networkLabel(),
            totalAvailable = totalOk,
            perAppAvailable = perAppOk,
            appWindowSec = appWindowSec,
        )
    }

    private fun sampleApps(nowNanos: Long, topN: Int) {
        if (!Permissions.hasUsageAccess(context)) {
            perAppOk = false
            apps = emptyList()
            prevUid = null
            return
        }
        val last = prevUid
        if (last != null && nowNanos - prevAppNanos < APP_INTERVAL_NANOS) return

        val cur = queryUidTotals()
        if (cur == null) {
            perAppOk = false
            apps = emptyList()
            prevUid = null
            return
        }
        perAppOk = true
        if (last != null) {
            val elapsed = nowNanos - prevAppNanos
            appWindowSec = Math.round(elapsed / 1_000_000_000.0).toInt().coerceAtLeast(1)
            val list = ArrayList<AppSpeed>()
            for ((uid, c) in cur) {
                // A UID absent from the previous query had no traffic since start: baseline 0.
                val p = last[uid]
                val rxBps = SpeedMath.bytesPerSecond(p?.get(0) ?: 0L, c[0], elapsed)
                val txBps = SpeedMath.bytesPerSecond(p?.get(1) ?: 0L, c[1], elapsed)
                if (rxBps <= 0L && txBps <= 0L) continue
                val id = cache.resolve(uid)
                cache.icon(id.packageName)
                list.add(AppSpeed(uid, id.packageName, id.label, rxBps, txBps))
            }
            list.sortByDescending { it.rxBps + it.txBps }
            apps = list.take(topN)
        }
        prevUid = cur
        prevAppNanos = nowNanos
    }

    /** Cumulative rx/tx bytes per UID (Wi-Fi + mobile) since the engine started. */
    @Suppress("DEPRECATION")
    private fun queryUidTotals(): Map<Int, LongArray>? {
        val manager = nsm ?: return null
        val result = HashMap<Int, LongArray>()
        var anyOk = false
        val start = startMs - 60_000L
        val end = System.currentTimeMillis() + 60_000L
        for (type in intArrayOf(ConnectivityManager.TYPE_WIFI, ConnectivityManager.TYPE_MOBILE)) {
            try {
                val stats = manager.querySummary(type, null, start, end)
                val bucket = NetworkStats.Bucket()
                while (stats.hasNextBucket()) {
                    stats.getNextBucket(bucket)
                    if (bucket.uid < 0) continue
                    val arr = result.getOrPut(bucket.uid) { LongArray(2) }
                    arr[0] += bucket.rxBytes
                    arr[1] += bucket.txBytes
                }
                stats.close()
                anyOk = true
            } catch (e: Exception) {
                Log.w(TAG, "querySummary($type) failed: ${e.javaClass.simpleName}")
            }
        }
        return if (anyOk) result else null
    }

    private fun networkLabel(): String {
        val net = cm?.activeNetwork ?: return "Offline"
        val caps = cm?.getNetworkCapabilities(net) ?: return "Offline"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile data"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Other"
        }
    }

    companion object {
        private const val TAG = "NetworkEngine"
        private const val APP_INTERVAL_NANOS = 3_000_000_000L
    }
}
