package com.asad.nexus.networkmonitor.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.asad.nexus.networkmonitor.data.AppSpeed
import com.asad.nexus.networkmonitor.data.MonitorState
import com.asad.nexus.networkmonitor.service.MonitorService
import com.asad.nexus.networkmonitor.util.Format
import com.asad.nexus.networkmonitor.util.Permissions

private val UploadBlue = Color(0xFF3B82F6)

@Composable
fun HomeScreen() {
    val ctx = LocalContext.current
    var usageOk by remember { mutableStateOf(Permissions.hasUsageAccess(ctx)) }
    var notifOk by remember { mutableStateOf(Permissions.hasNotifications(ctx)) }
    var guideOpen by remember { mutableStateOf(false) }
    val apps by MonitorState.apps.collectAsState()

    // Permissions can change while the app is in the background: re-check on every resume.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        usageOk = Permissions.hasUsageAccess(ctx)
        notifOk = Permissions.hasNotifications(ctx)
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { notifOk = Permissions.hasNotifications(ctx) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text("NEXUS NETWORK MONITOR", color = NexusCyan, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Created by Asad", color = NexusMuted, fontSize = 12.sp)
            }
        }
        item { TotalsCard() }
        item {
            ControlRow(onNeedNotifPermission = {
                if (Build.VERSION.SDK_INT >= 33 && !notifOk) {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            })
        }
        if (!usageOk || !notifOk) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    NexusCard {
                        Text("Permissions needed", color = NexusWarning, fontWeight = FontWeight.SemiBold)
                        if (!usageOk) {
                            PermissionRow("Usage Access", "Needed for per-app traffic statistics.") {
                                safeStart(ctx, Permissions.usageAccessIntent())
                            }
                        }
                        if (!notifOk) {
                            PermissionRow("Notifications", "Needed to show the live speed notification.") {
                                safeStart(ctx, Permissions.notificationSettingsIntent(ctx))
                            }
                        }
                        TextButton(onClick = { guideOpen = !guideOpen }) {
                            Text(if (guideOpen) "Hide setup guide" else "Setup guide (English)")
                        }
                    }
                    if (guideOpen) GuideCard()
                }
            }
        } else {
            item { Text("\u2713 All permissions granted", color = NexusSuccess, fontSize = 12.sp) }
        }
        item {
            Column {
                Text("Apps with recent traffic", color = NexusText, fontWeight = FontWeight.SemiBold)
                Text(
                    "Per-app values are interval averages from Android usage statistics. " +
                        "They can lag and are not true real-time speeds.",
                    color = NexusMuted, fontSize = 12.sp,
                )
            }
        }
        if (apps.isEmpty()) {
            item { EmptyApps() }
        } else {
            items(apps.take(15), key = { it.uid }) { a -> AppRow(a) }
        }
    }
}

@Composable
private fun ControlRow(onNeedNotifPermission: () -> Unit) {
    val ctx = LocalContext.current
    val running by MonitorState.running.collectAsState()
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = {
                onNeedNotifPermission()
                ContextCompat.startForegroundService(ctx, Intent(ctx, MonitorService::class.java))
            },
            enabled = !running,
            modifier = Modifier.weight(1f),
        ) { Text("Start monitoring") }
        OutlinedButton(
            onClick = { ctx.stopService(Intent(ctx, MonitorService::class.java)) },
            enabled = running,
            modifier = Modifier.weight(1f),
        ) { Text("Stop") }
    }
}

@Composable
private fun TotalsCard() {
    val s by MonitorState.flow.collectAsState()
    NexusCard {
        val status = when {
            !s.running -> "Monitoring stopped"
            !s.totalAvailable -> "Total speed not supported on this device"
            else -> "Monitoring \u00B7 ${s.networkLabel}"
        }
        Text(status, color = if (s.running) NexusSuccess else NexusMuted, fontSize = 12.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("DOWNLOAD", color = NexusMuted, fontSize = 11.sp)
                Text(
                    if (s.running) Format.speed(s.totalRxBps) else "-",
                    color = NexusCyan, fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold, fontSize = 22.sp,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("UPLOAD", color = NexusMuted, fontSize = 11.sp)
                Text(
                    if (s.running) Format.speed(s.totalTxBps) else "-",
                    color = UploadBlue, fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold, fontSize = 22.sp,
                )
            }
        }
        if (s.running && s.totalAvailable) {
            Text(
                "Since boot: \u2193 ${Format.bytes(s.totalRxBytes)}  \u2191 ${Format.bytes(s.totalTxBytes)}",
                color = NexusMuted, fontFamily = FontFamily.Monospace, fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun EmptyApps() {
    val s by MonitorState.flow.collectAsState()
    val t = when {
        !s.running -> "Start monitoring to see apps."
        !s.perAppAvailable -> "Per-app data unavailable. Allow Usage Access."
        s.appWindowSec == 0 -> "Measuring..."
        else -> "No app traffic in the last ${s.appWindowSec}s."
    }
    Text(t, color = NexusMuted)
}

@Composable
private fun AppRow(a: AppSpeed) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NexusSurface, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(a.packageName)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(a.label, color = NexusText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (a.packageName.isNotEmpty()) {
                Text(a.packageName, color = NexusMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "\u2193 ${Format.speed(a.rxBps)}",
                color = NexusCyan, fontFamily = FontFamily.Monospace, fontSize = 13.sp,
            )
            Text(
                "\u2191 ${Format.speed(a.txBps)}",
                color = UploadBlue, fontFamily = FontFamily.Monospace, fontSize = 13.sp,
            )
        }
    }
}
