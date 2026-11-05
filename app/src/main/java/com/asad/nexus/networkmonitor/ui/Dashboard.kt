package com.asad.nexus.networkmonitor.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.asad.nexus.networkmonitor.NexusApp
import com.asad.nexus.networkmonitor.data.AppSpeed
import com.asad.nexus.networkmonitor.data.MonitorState
import com.asad.nexus.networkmonitor.data.Prefs
import com.asad.nexus.networkmonitor.data.SpeedSnapshot
import com.asad.nexus.networkmonitor.service.MonitorService
import com.asad.nexus.networkmonitor.util.Format
import com.asad.nexus.networkmonitor.util.Permissions

@Composable
fun Dashboard() {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as NexusApp
    val snap by MonitorState.flow.collectAsState()

    var usageOk by remember { mutableStateOf(Permissions.hasUsageAccess(ctx)) }
    var notifOk by remember { mutableStateOf(Permissions.hasNotifications(ctx)) }
    var compact by remember { mutableStateOf(Prefs.compact(ctx)) }
    var rows by remember { mutableIntStateOf(Prefs.maxRows(ctx)) }

    // Permissions can change while the app is in the background: re-check on every resume.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        usageOk = Permissions.hasUsageAccess(ctx)
        notifOk = Permissions.hasNotifications(ctx)
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { notifOk = Permissions.hasNotifications(ctx) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(NexusBg).systemBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text("NEXUS NETWORK MONITOR", color = NexusCyan, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Created by Asad", color = NexusMuted, fontSize = 12.sp)
            }
        }

        item { TotalsCard(snap) }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= 33 && !notifOk) {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        ContextCompat.startForegroundService(ctx, Intent(ctx, MonitorService::class.java))
                    },
                    enabled = !snap.running,
                    modifier = Modifier.weight(1f),
                ) { Text("Start monitoring") }
                OutlinedButton(
                    onClick = { ctx.stopService(Intent(ctx, MonitorService::class.java)) },
                    enabled = snap.running,
                    modifier = Modifier.weight(1f),
                ) { Text("Stop") }
            }
        }

        item {
            NexusCard {
                Text("Notification", color = NexusText, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Compact mode", color = NexusText, modifier = Modifier.weight(1f))
                    Switch(checked = compact, onCheckedChange = {
                        compact = it
                        Prefs.setCompact(ctx, it)
                    })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("App rows: $rows", color = NexusText, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = {
                        rows = (rows - 1).coerceAtLeast(1)
                        Prefs.setMaxRows(ctx, rows)
                    }) { Text("-") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = {
                        rows = (rows + 1).coerceAtMost(Prefs.MAX_ROWS)
                        Prefs.setMaxRows(ctx, rows)
                    }) { Text("+") }
                }
            }
        }

        item {
            NexusCard {
                Text("Permissions", color = NexusText, fontWeight = FontWeight.SemiBold)
                PermissionRow(
                    title = "Usage Access",
                    purpose = "Needed for per-app traffic statistics.",
                    granted = usageOk,
                    onClick = { ctx.startActivity(Permissions.usageAccessIntent()) },
                )
                PermissionRow(
                    title = "Notifications",
                    purpose = "Needed to show the live speed notification.",
                    granted = notifOk,
                    onClick = { ctx.startActivity(Permissions.notificationSettingsIntent(ctx)) },
                )
            }
        }

        item {
            Text("Apps with recent traffic", color = NexusText, fontWeight = FontWeight.SemiBold)
            Text(
                "Per-app values are interval averages from Android usage statistics. " +
                    "They can lag and are not true real-time speeds.",
                color = NexusMuted, fontSize = 12.sp,
            )
        }

        val emptyText = when {
            !snap.running -> "Start monitoring to see apps."
            !snap.perAppAvailable -> "Per-app data unavailable. Allow Usage Access."
            snap.appWindowSec == 0 -> "Measuring..."
            else -> "No app traffic in the last ${snap.appWindowSec}s."
        }
        if (snap.apps.isEmpty()) {
            item { Text(emptyText, color = NexusMuted) }
        } else {
            items(snap.apps, key = { it.uid }) { a -> AppRow(a, app) }
        }
    }
}

@Composable
private fun NexusCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NexusSurface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

@Composable
private fun TotalsCard(s: SpeedSnapshot) {
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
                    color = Color(0xFF3B82F6), fontFamily = FontFamily.Monospace,
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
private fun PermissionRow(title: String, purpose: String, granted: Boolean, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = NexusText)
            Text(purpose, color = NexusMuted, fontSize = 12.sp)
            Text(
                if (granted) "Granted" else "Not granted",
                color = if (granted) NexusSuccess else NexusWarning, fontSize = 12.sp,
            )
        }
        OutlinedButton(onClick = onClick) { Text("Settings") }
    }
}

@Composable
private fun AppRow(a: AppSpeed, app: NexusApp) {
    val icon = remember(a.packageName) { app.appInfoCache.icon(a.packageName).asImageBitmap() }
    NexusCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(a.label, color = NexusText, maxLines = 1)
                if (a.packageName.isNotEmpty()) {
                    Text(a.packageName, color = NexusMuted, fontSize = 11.sp, maxLines = 1)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "\u2193 ${Format.speed(a.rxBps)}",
                    color = NexusCyan, fontFamily = FontFamily.Monospace, fontSize = 13.sp,
                )
                Text(
                    "\u2191 ${Format.speed(a.txBps)}",
                    color = Color(0xFF3B82F6), fontFamily = FontFamily.Monospace, fontSize = 13.sp,
                )
            }
        }
    }
}
