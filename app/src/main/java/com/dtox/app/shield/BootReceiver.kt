package com.dtox.app.shield

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dtox.app.data.Prefs

/** Re-arms Shield mode after a reboot, when Android lets background services start. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = Prefs(context)
        if (prefs.shieldEnabled && ShieldManager.hasUsageAccess(context)) {
            ShieldService.start(context)
        }
    }
}
