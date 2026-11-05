package com.asad.nexus.networkmonitor.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asad.nexus.networkmonitor.NexusApp
import com.asad.nexus.networkmonitor.util.Permissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun safeStart(ctx: Context, intent: Intent) {
    try {
        ctx.startActivity(intent)
    } catch (e: Exception) {
        // This settings page does not exist on this device; nothing else to do.
    }
}

/** Light card: plain background and shape (no elevation layers), cheap to scroll. */
@Composable
fun NexusCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NexusSurface, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Composable
fun SectionTitle(text: String) {
    Text(text, color = NexusCyan, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
}

@Composable
fun SwitchRow(title: String, subtitle: String? = null, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = NexusText)
            if (subtitle != null) Text(subtitle, color = NexusMuted, fontSize = 12.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun ChoiceRow(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { i, label ->
            val m = Modifier.weight(1f)
            val pad = PaddingValues(horizontal = 4.dp)
            if (i == selected) {
                Button(onClick = { onSelect(i) }, modifier = m, contentPadding = pad) {
                    Text(label, fontSize = 12.sp, maxLines = 1)
                }
            } else {
                OutlinedButton(onClick = { onSelect(i) }, modifier = m, contentPadding = pad) {
                    Text(label, fontSize = 12.sp, maxLines = 1)
                }
            }
        }
    }
}

/** App icon loaded off the main thread (usually already cached by the engine). */
@Composable
fun AppIcon(packageName: String, size: Dp = 36.dp) {
    val cache = (LocalContext.current.applicationContext as NexusApp).appInfoCache
    val bmp by produceState<ImageBitmap?>(initialValue = null, packageName) {
        value = withContext(Dispatchers.IO) { cache.icon(packageName).asImageBitmap() }
    }
    val b = bmp
    if (b != null) {
        Image(bitmap = b, contentDescription = null, modifier = Modifier.size(size))
    } else {
        Spacer(Modifier.size(size))
    }
}

@Composable
private fun GuideStep(title: String, body: String) {
    Column {
        Text(title, color = NexusText, fontWeight = FontWeight.Medium, fontSize = 14.sp)
        Text(body, color = NexusMuted, fontSize = 12.sp)
    }
}

@Composable
fun GuideCard() {
    val ctx = LocalContext.current
    NexusCard {
        SectionTitle("Setup guide")
        GuideStep(
            "1. Usage Access",
            "Tap \"Usage Access\", find Nexus Network Monitor and switch it ON. " +
                "This lets Android share per-app traffic statistics with the app.",
        )
        GuideStep(
            "2. Notifications",
            "Allow notifications so the live speed notification can be shown.",
        )
        GuideStep(
            "3. If a setting is greyed out",
            "Open App info, tap the 3-dot menu (top right), choose \"Allow restricted settings\", " +
                "confirm with your PIN or fingerprint, then try the permission again. " +
                "Android blocks some permissions for apps installed outside the Play Store until you do this.",
        )
        GuideStep(
            "4. Keep it running (Realme / ColorOS)",
            "App info > Battery usage > Allow background activity. If you see Auto-launch, allow it too. " +
                "This is optional; it only stops the system from closing the monitor.",
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { safeStart(ctx, Permissions.appInfoIntent(ctx)) },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) { Text("App info", fontSize = 12.sp, maxLines = 1) }
            OutlinedButton(
                onClick = { safeStart(ctx, Permissions.usageAccessIntent()) },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) { Text("Usage Access", fontSize = 12.sp, maxLines = 1) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { safeStart(ctx, Permissions.notificationSettingsIntent(ctx)) },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) { Text("Notifications", fontSize = 12.sp, maxLines = 1) }
            OutlinedButton(
                onClick = { safeStart(ctx, Permissions.batterySettingsIntent()) },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) { Text("Battery", fontSize = 12.sp, maxLines = 1) }
        }
    }
}

@Composable
fun PermissionRow(title: String, purpose: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = NexusText)
            Text(purpose, color = NexusMuted, fontSize = 12.sp)
            Text("Not granted", color = NexusWarning, fontSize = 12.sp)
        }
        TextButton(onClick = onClick) { Text("Settings") }
    }
}
