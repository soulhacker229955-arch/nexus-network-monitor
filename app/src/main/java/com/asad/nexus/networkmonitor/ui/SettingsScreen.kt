package com.asad.nexus.networkmonitor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asad.nexus.networkmonitor.data.Prefs

@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    var compact by remember { mutableStateOf(Prefs.bool(ctx, Prefs.K_COMPACT, false)) }
    var showTotal by remember { mutableStateOf(Prefs.bool(ctx, Prefs.K_SHOW_TOTAL, true)) }
    var showApps by remember { mutableStateOf(Prefs.bool(ctx, Prefs.K_SHOW_APPS, true)) }
    var showDown by remember { mutableStateOf(Prefs.bool(ctx, Prefs.K_SHOW_DOWN, true)) }
    var showUp by remember { mutableStateOf(Prefs.bool(ctx, Prefs.K_SHOW_UP, true)) }
    var rows by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.K_ROWS, 4).coerceIn(1, Prefs.MAX_ROWS)) }
    var sort by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.K_SORT, Prefs.SORT_TOTAL)) }
    var iconMode by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.K_ICON, Prefs.ICON_DOWN)) }
    var refresh by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.K_REFRESH_S, 1).coerceIn(1, 3)) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Settings", color = NexusCyan, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        item {
            NexusCard {
                SectionTitle("Notification")
                SwitchRow(
                    "Show total speed", "Total download / upload line at the top",
                    showTotal,
                ) { showTotal = it; Prefs.setBool(ctx, Prefs.K_SHOW_TOTAL, it) }
                SwitchRow(
                    "Show apps", "App logos with their speed when expanded",
                    showApps,
                ) { showApps = it; Prefs.setBool(ctx, Prefs.K_SHOW_APPS, it) }
                SwitchRow(
                    "Compact mode", "Hide the expanded app list",
                    compact,
                ) { compact = it; Prefs.setBool(ctx, Prefs.K_COMPACT, it) }
                SwitchRow("Show download", null, showDown) {
                    if (it || showUp) {
                        showDown = it
                        Prefs.setBool(ctx, Prefs.K_SHOW_DOWN, it)
                    }
                }
                SwitchRow("Show upload", null, showUp) {
                    if (it || showDown) {
                        showUp = it
                        Prefs.setBool(ctx, Prefs.K_SHOW_UP, it)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("App rows: $rows", color = NexusText, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = {
                        rows = (rows - 1).coerceAtLeast(1)
                        Prefs.setInt(ctx, Prefs.K_ROWS, rows)
                    }) { Text("-") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = {
                        rows = (rows + 1).coerceAtMost(Prefs.MAX_ROWS)
                        Prefs.setInt(ctx, Prefs.K_ROWS, rows)
                    }) { Text("+") }
                }
                Text("Sort apps by", color = NexusText)
                ChoiceRow(listOf("Total", "Download", "Upload"), sort) {
                    sort = it
                    Prefs.setInt(ctx, Prefs.K_SORT, it)
                }
                Text("Choose which apps to show in the Apps tab.", color = NexusMuted, fontSize = 12.sp)
            }
        }

        item {
            NexusCard {
                SectionTitle("Status bar speed")
                Text(
                    "Shows the live speed number in the status bar, next to the clock.",
                    color = NexusMuted, fontSize = 12.sp,
                )
                ChoiceRow(listOf("Download", "Upload", "Both", "Off"), iconMode) {
                    iconMode = it
                    Prefs.setInt(ctx, Prefs.K_ICON, it)
                }
                Text(
                    "Both = download + upload added together. Off = fixed arrow icon. " +
                        "If nothing shows, enable silent notification icons in system settings.",
                    color = NexusMuted, fontSize = 12.sp,
                )
            }
        }

        item {
            NexusCard {
                SectionTitle("Monitoring")
                Text("Refresh interval", color = NexusText)
                ChoiceRow(listOf("1 s", "2 s", "3 s"), refresh - 1) {
                    refresh = it + 1
                    Prefs.setInt(ctx, Prefs.K_REFRESH_S, refresh)
                }
                Text(
                    "Longer intervals use less battery. Per-app speed is always an average over about 3 seconds.",
                    color = NexusMuted, fontSize = 12.sp,
                )
            }
        }

        item { GuideCard() }

        item {
            NexusCard {
                SectionTitle("About")
                Text("Nexus Network Monitor 0.2.0", color = NexusText)
                Text("Created by Asad", color = NexusMuted, fontSize = 12.sp)
                Text(
                    "Privacy: all measurements stay on your device. The app has no internet permission " +
                        "and sends nothing anywhere.",
                    color = NexusMuted, fontSize = 12.sp,
                )
            }
        }
    }
}
