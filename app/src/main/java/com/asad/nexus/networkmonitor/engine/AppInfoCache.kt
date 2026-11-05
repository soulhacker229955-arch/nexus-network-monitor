package com.asad.nexus.networkmonitor.engine

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
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

    private fun load(uid: Int): AppIdentity {
        if (uid == 0) return AppIdentity("", "Root")
        if (uid == 1000) return AppIdentity("android", "Android System")
        val pkg = try {
            pm.getPackagesForUid(uid)?.firstOrNull()
        } catch (e: Exception) {
            null
        } ?: return AppIdentity("", "UID $uid")
        val label = try {
            pm.getApplicationInfo(pkg, 0).loadLabel(pm).toString()
        } catch (e: Exception) {
            pkg
        }
        return AppIdentity(pkg, label)
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
