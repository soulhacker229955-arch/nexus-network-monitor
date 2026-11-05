package com.asad.nexus.networkmonitor.engine

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import com.asad.nexus.networkmonitor.data.AppSpeed
import java.util.concurrent.ConcurrentHashMap

data class AppIdentity(val packageName: String, val label: String)

/** Maps UIDs to package names/labels and caches app icons as small bitmaps. */
class AppInfoCache(context: Context) {
    private val pm = context.packageManager
    private val iconPx = (40 * context.resources.displayMetrics.density).toInt().coerceAtLeast(24)
    private val identities = ConcurrentHashMap<Int, AppIdentity>()
    private val icons = ConcurrentHashMap<String, Bitmap>()

    fun resolve(uid: Int): AppIdentity = identities.getOrPut(uid) { load(uid) }

    fun icon(packageName: String): Bitmap = icons.getOrPut(packageName) { loadIcon(packageName) }

    fun labelFor(packageName: String): String = try {
        pm.getApplicationInfo(packageName, 0).loadLabel(pm).toString()
    } catch (e: Exception) {
        packageName
    }

    /** An app with no measured traffic right now (shown as 0 B/s). */
    fun idleApp(packageName: String): AppSpeed = AppSpeed(-1, packageName, labelFor(packageName), 0L, 0L)

    /** Apps that have a launcher icon, sorted by name. */
    @Suppress("DEPRECATION")
    fun installedApps(): List<AppIdentity> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val list = try {
            pm.queryIntentActivities(intent, 0)
        } catch (e: Exception) {
            emptyList()
        }
        return list.mapNotNull { ri ->
            val p = ri.activityInfo?.packageName ?: return@mapNotNull null
            AppIdentity(p, ri.loadLabel(pm).toString())
        }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
    }

    private fun load(uid: Int): AppIdentity {
        if (uid == 0) return AppIdentity("", "Root")
        if (uid == 1000) return AppIdentity("android", "Android System")
        val pkg = try {
            pm.getPackagesForUid(uid)?.firstOrNull()
        } catch (e: Exception) {
            null
        } ?: return AppIdentity("", "UID $uid")
        return AppIdentity(pkg, labelFor(pkg))
    }

    private fun loadIcon(packageName: String): Bitmap {
        if (packageName.isNotEmpty()) {
            try {
                return pm.getApplicationIcon(packageName).toBitmap(iconPx, iconPx)
            } catch (e: Exception) {
                // fall through to default icon
            }
        }
        return pm.defaultActivityIcon.toBitmap(iconPx, iconPx)
    }
}
