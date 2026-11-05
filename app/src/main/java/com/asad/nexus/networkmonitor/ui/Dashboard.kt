package com.asad.nexus.networkmonitor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Tab host: Home, Apps, Settings. */
@Composable
fun Dashboard() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(
        Modifier
            .fillMaxSize()
            .background(NexusBg)
            .systemBarsPadding()
            .imePadding(),
    ) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> HomeScreen()
                1 -> AppsScreen()
                else -> SettingsScreen()
            }
        }
        Row(
            Modifier.fillMaxWidth().background(NexusSurface).padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("Home", "Apps", "Settings").forEachIndexed { i, title ->
                val sel = i == tab
                TextButton(onClick = { tab = i }, modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        color = if (sel) NexusCyan else NexusMuted,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}
