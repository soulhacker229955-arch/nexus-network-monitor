package com.asad.nexus.networkmonitor.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Latest measurement published by the service. Apps and running state are separate flows
 * so the UI list only recomposes when the per-app data actually changes.
 */
object MonitorState {
    private val _flow = MutableStateFlow(SpeedSnapshot())
    val flow: StateFlow<SpeedSnapshot> = _flow.asStateFlow()

    private val _apps = MutableStateFlow<List<AppSpeed>>(emptyList())
    val apps: StateFlow<List<AppSpeed>> = _apps.asStateFlow()

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    fun set(snapshot: SpeedSnapshot) {
        _flow.value = snapshot
        _apps.value = snapshot.apps
        _running.value = snapshot.running
    }
}
