package com.asad.nexus.networkmonitor.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Latest measurement published by the service, observed by the UI. */
object MonitorState {
    private val _flow = MutableStateFlow(SpeedSnapshot())
    val flow: StateFlow<SpeedSnapshot> = _flow.asStateFlow()

    fun set(snapshot: SpeedSnapshot) {
        _flow.value = snapshot
    }
}
