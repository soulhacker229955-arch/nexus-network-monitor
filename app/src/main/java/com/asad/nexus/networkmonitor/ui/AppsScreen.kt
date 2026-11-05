package com.asad.nexus.networkmonitor.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asad.nexus.networkmonitor.NexusApp
import com.asad.nexus.networkmonitor.data.Prefs
import com.asad.nexus.networkmonitor.engine.AppIdentity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AppsScreen() {
    val ctx = LocalContext.current
    val cache = (ctx.applicationContext as NexusApp).appInfoCache
    val all by produceState<List<AppIdentity>?>(initialValue = null) {
        value = withContext(Dispatchers.Default) { cache.installedApps() }
    }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(Prefs.selectedApps(ctx)) }
    var onlySelected by remember { mutableStateOf(Prefs.bool(ctx, Prefs.K_ONLY_SELECTED, false)) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Spacer(Modifier.width(1.dp))
        Text("Apps in notification", color = NexusCyan, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        ChoiceRow(
            options = listOf("Top traffic apps", "Only selected apps"),
            selected = if (onlySelected) 1 else 0,
        ) {
            onlySelected = it == 1
            Prefs.setBool(ctx, Prefs.K_ONLY_SELECTED, onlySelected)
        }
        Text(
            if (onlySelected) {
                "The notification shows only the apps ticked below (idle apps show 0 B/s)."
            } else {
                "The notification shows whichever apps use the most data right now."
            },
            color = NexusMuted, fontSize = 12.sp,
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search apps") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${selected.size} selected", color = NexusText, modifier = Modifier.weight(1f))
            TextButton(onClick = {
                selected = emptySet()
                Prefs.setSelectedApps(ctx, selected)
            }) { Text("Clear") }
        }

        val list = all
        if (list == null) {
            Text("Loading apps...", color = NexusMuted)
        } else {
            val filtered = remember(list, query) {
                if (query.isBlank()) list else list.filter {
                    it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(filtered, key = { it.packageName }) { a ->
                    val checked = a.packageName in selected
                    PickRow(a, checked) { now ->
                        selected = if (now) selected + a.packageName else selected - a.packageName
                        Prefs.setSelectedApps(ctx, selected)
                    }
                }
            }
        }
    }
}

@Composable
private fun PickRow(a: AppIdentity, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(a.packageName, size = 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(a.label, color = NexusText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(a.packageName, color = NexusMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Checkbox(checked = checked, onCheckedChange = onToggle)
    }
}
