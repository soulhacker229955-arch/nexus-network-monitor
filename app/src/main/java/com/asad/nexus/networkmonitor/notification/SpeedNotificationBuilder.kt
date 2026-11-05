package com.asad.nexus.networkmonitor.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.asad.nexus.networkmonitor.MainActivity
import com.asad.nexus.networkmonitor.R
import com.asad.nexus.networkmonitor.data.AppSpeed
import com.asad.nexus.networkmonitor.data.NotifConfig
import com.asad.nexus.networkmonitor.data.Prefs
import com.asad.nexus.networkmonitor.data.SpeedSnapshot
import com.asad.nexus.networkmonitor.engine.AppInfoCache
import com.asad.nexus.networkmonitor.service.MonitorService
import com.asad.nexus.networkmonitor.util.Format

class SpeedNotificationBuilder(
    private val context: Context,
    private val cache: AppInfoCache,
) {
    private class Row(val row: Int, val icon: Int, val name: Int, val speed: Int)

    private val rows = listOf(
        Row(R.id.row1, R.id.icon1, R.id.name1, R.id.speed1),
        Row(R.id.row2, R.id.icon2, R.id.name2, R.id.speed2),
        Row(R.id.row3, R.id.icon3, R.id.name3, R.id.speed3),
        Row(R.id.row4, R.id.icon4, R.id.name4, R.id.speed4),
        Row(R.id.row5, R.id.icon5, R.id.name5, R.id.speed5),
    )

    private val openIntent: PendingIntent by lazy {
        PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private val stopIntent: PendingIntent by lazy {
        PendingIntent.getService(
            context, 1,
            Intent(context, MonitorService::class.java).setAction(MonitorService.ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    fun ensureChannel() {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID, "Network speed", NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Live total and per-app network speed"
            setShowBadge(false)
        }
        nm.createNotificationChannel(channel)
    }

    fun build(s: SpeedSnapshot, apps: List<AppSpeed>, cfg: NotifConfig): Notification {
        val title = if (cfg.showTotal) totalLine(s, cfg) else "Nexus Network Monitor"
        val status = statusLine(s, cfg)

        val collapsed = RemoteViews(context.packageName, R.layout.notif_collapsed).apply {
            setTextViewText(R.id.title, title)
            setTextViewText(R.id.status, status)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(smallIcon(s, cfg.iconMode))
            .setContentTitle(title)
            .setContentText(status)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(collapsed)
            .setContentIntent(openIntent)
            .addAction(0, "Stop", stopIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (!cfg.compact && cfg.showApps) {
            val expanded = RemoteViews(context.packageName, R.layout.notif_expanded)
            expanded.setTextViewText(R.id.title, title)
            expanded.setTextViewText(R.id.status, status)
            rows.forEachIndexed { i, r ->
                val app = apps.getOrNull(i)
                if (app != null && i < cfg.rows) {
                    expanded.setViewVisibility(r.row, View.VISIBLE)
                    expanded.setImageViewBitmap(r.icon, cache.icon(app.packageName))
                    expanded.setTextViewText(r.name, app.label)
                    expanded.setTextViewText(r.speed, speedText(app.rxBps, app.txBps, cfg, " "))
                } else {
                    expanded.setViewVisibility(r.row, View.GONE)
                }
            }
            builder.setCustomBigContentView(expanded)
        }
        return builder.build()
    }

    /** Status bar icon: the live speed number (like a speed indicator), or a fixed icon. */
    private fun smallIcon(s: SpeedSnapshot, mode: Int): IconCompat {
        if (mode == Prefs.ICON_OFF || !s.totalAvailable) {
            return IconCompat.createWithResource(context, R.drawable.ic_stat_speed)
        }
        val bps = when (mode) {
            Prefs.ICON_UP -> s.totalTxBps
            Prefs.ICON_COMBINED -> s.totalRxBps + s.totalTxBps
            else -> s.totalRxBps
        }
        val (value, unit) = Format.shortSpeed(bps)
        return IconCompat.createWithBitmap(StatusIcon.render(value, unit))
    }

    private fun speedText(rx: Long, tx: Long, cfg: NotifConfig, sep: String): String {
        val parts = ArrayList<String>(2)
        if (cfg.showDown) parts.add("\u2193 ${Format.speed(rx)}")
        if (cfg.showUp) parts.add("\u2191 ${Format.speed(tx)}")
        return parts.joinToString(sep)
    }

    private fun totalLine(s: SpeedSnapshot, cfg: NotifConfig): String =
        "Total " + speedText(s.totalRxBps, s.totalTxBps, cfg, " | ")

    private fun statusLine(s: SpeedSnapshot, cfg: NotifConfig): String = when {
        !s.totalAvailable -> "Total speed not supported on this device"
        !cfg.showApps -> s.networkLabel
        !s.perAppAvailable -> "${s.networkLabel} \u00B7 Per-app: allow Usage Access in the app"
        s.appWindowSec == 0 -> "${s.networkLabel} \u00B7 Measuring apps..."
        else -> "${s.networkLabel} \u00B7 Apps: avg of last ${s.appWindowSec}s (may lag)"
    }

    companion object {
        const val CHANNEL_ID = "nexus_speed"
        const val NOTIFICATION_ID = 1001
    }
}
