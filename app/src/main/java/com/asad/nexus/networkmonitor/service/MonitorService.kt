package com.asad.nexus.networkmonitor.service

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.asad.nexus.networkmonitor.NexusApp
import com.asad.nexus.networkmonitor.data.MonitorState
import com.asad.nexus.networkmonitor.data.Prefs
import com.asad.nexus.networkmonitor.data.SpeedSnapshot
import com.asad.nexus.networkmonitor.engine.AppInfoCache
import com.asad.nexus.networkmonitor.engine.NetworkEngine
import com.asad.nexus.networkmonitor.notification.SpeedNotificationBuilder
import com.asad.nexus.networkmonitor.util.AppSelector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** User-started foreground service. Never restarts itself (START_NOT_STICKY). */
class MonitorService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private lateinit var cache: AppInfoCache
    private lateinit var builder: SpeedNotificationBuilder
    private lateinit var engine: NetworkEngine

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        cache = (application as NexusApp).appInfoCache
        builder = SpeedNotificationBuilder(this, cache)
        engine = NetworkEngine(this, cache)
        builder.ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            val first = builder.build(SpeedSnapshot(), emptyList(), Prefs.config(this))
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(
                    SpeedNotificationBuilder.NOTIFICATION_ID, first,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                )
            } else {
                startForeground(SpeedNotificationBuilder.NOTIFICATION_ID, first)
            }
        } catch (e: Exception) {
            Log.w(TAG, "startForeground failed: ${e.javaClass.simpleName}")
            stopSelf()
            return START_NOT_STICKY
        }
        if (job?.isActive != true) {
            job = scope.launch { loop() }
        }
        return START_NOT_STICKY
    }

    private suspend fun loop() {
        val nm = getSystemService(NotificationManager::class.java)
        while (currentCoroutineContext().isActive) {
            var waitMs = 1000L
            try {
                val cfg = Prefs.config(this)
                waitMs = cfg.refreshMs
                val snap = engine.sample(topN = 40)
                MonitorState.set(snap.copy(running = true))
                val shown = AppSelector.select(
                    snap.apps, cfg.selected, cfg.onlySelected, cfg.sort, cfg.rows,
                ) { pkg -> cache.idleApp(pkg) }
                nm?.notify(
                    SpeedNotificationBuilder.NOTIFICATION_ID,
                    builder.build(snap, shown, cfg),
                )
            } catch (e: Exception) {
                Log.w(TAG, "sample failed: ${e.javaClass.simpleName}")
            }
            delay(waitMs)
        }
    }

    override fun onDestroy() {
        job?.cancel()
        scope.cancel()
        MonitorState.set(SpeedSnapshot(running = false))
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.asad.nexus.networkmonitor.STOP"
        private const val TAG = "MonitorService"
    }
}
