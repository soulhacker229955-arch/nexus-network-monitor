package com.asad.nexus.networkmonitor

import android.app.Application
import com.asad.nexus.networkmonitor.engine.AppInfoCache

class NexusApp : Application() {
    lateinit var appInfoCache: AppInfoCache
        private set

    override fun onCreate() {
        super.onCreate()
        appInfoCache = AppInfoCache(this)
    }
}
