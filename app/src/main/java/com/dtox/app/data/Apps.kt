package com.dtox.app.data

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable

data class AppInfo(
    val pkg: String,
    val label: String,
    val icon: Drawable,
)

/**
 * Every app that shows up in the launcher. Safe and permission-free: the
 * manifest declares a `<queries>` filter for LAUNCHER intents, so we never
 * need the sensitive QUERY_ALL_PACKAGES permission.
 */
fun loadLaunchableApps(context: Context): List<AppInfo> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    @Suppress("DEPRECATION")
    val resolved = pm.queryIntentActivities(intent, 0)

    val result = ArrayList<AppInfo>(resolved.size)
    val seen = HashSet<String>()
    for (info in resolved) {
        val activity = info.activityInfo ?: continue
        val pkg = activity.packageName
        if (pkg == context.packageName) continue
        if (!seen.add(pkg)) continue
        val application = activity.applicationInfo ?: continue
        result.add(
            AppInfo(
                pkg = pkg,
                label = application.loadLabel(pm).toString(),
                icon = application.loadIcon(pm),
            ),
        )
    }
    result.sortBy { it.label.lowercase() }
    return result
}
