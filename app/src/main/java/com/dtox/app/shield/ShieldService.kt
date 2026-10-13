package com.dtox.app.shield

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.dtox.app.MainActivity
import com.dtox.app.R
import com.dtox.app.data.Prefs

/**
 * Shield mode: watches which app comes to the foreground and puts D-Tox's
 * breath gate in front of the gated ones — no matter how they were opened
 * (drawer, recents, search, links).
 *
 * Call-safe: dialer/telecom packages are never gated, and the watcher pauses
 * while a call is active or the screen is off. No accessibility service is
 * involved — just usage access + the overlay permission, both visible and
 * revocable in system settings.
 */
class ShieldService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private var lastGateAt = 0L
    private lateinit var prefs: Prefs

    private val skipPackages = setOf(
        "com.android.dialer",
        "com.google.android.dialer",
        "com.android.incallui",
        "com.android.server.telecom",
        "com.android.phone",
        "com.android.systemui",
        "com.samsung.android.incallui",
        "com.samsung.android.dialer",
        "com.miui.voiceassist",
    )

    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            runCatching { checkForeground() }
            handler.postDelayed(this, 800L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!running) {
            running = true
            createChannel()
            startForeground(NOTIFICATION_ID, buildNotification())
            handler.post(tick)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        handler.removeCallbacks(tick)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun checkForeground() {
        if (!prefs.shieldEnabled) {
            stopSelf()
            return
        }

        val power = getSystemService(PowerManager::class.java)
        if (power?.isInteractive != true) return

        val audio = getSystemService(AudioManager::class.java)
        val mode = audio?.mode ?: AudioManager.MODE_NORMAL
        if (mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION) return

        val usage = getSystemService(UsageStatsManager::class.java) ?: return
        val now = System.currentTimeMillis()
        val events: UsageEvents = usage.queryEvents(now - 2500, now) ?: return
        var lastPackage: String? = null
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastPackage = event.packageName
            }
        }
        val pkg = lastPackage ?: return
        if (pkg == packageName) return // our own gate screen
        if (pkg in skipPackages) return

        if (!prefs.gatedApps.contains(pkg)) {
            if (prefs.shieldGracePackage.isNotEmpty()) prefs.shieldGracePackage = ""
            return
        }

        val graceActive = prefs.shieldGracePackage == pkg &&
            SystemClock.elapsedRealtime() < prefs.shieldGraceUntil
        if (graceActive) return

        // Avoid a double launch in the moment before our activity is on top.
        if (SystemClock.elapsedRealtime() - lastGateAt < 2500L) return
        lastGateAt = SystemClock.elapsedRealtime()

        val gate = Intent(this, GateActivity::class.java)
            .putExtra(GateActivity.EXTRA_PACKAGE, pkg)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        startActivity(gate)
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Shield",
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setContentTitle("D-Tox shield is on")
            .setContentText("A breath comes before your gated apps open")
            .setContentIntent(open)
            .setOngoing(true)
            .setShowWhen(false)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "dtox_shield"
        private const val NOTIFICATION_ID = 42

        fun start(context: Context) {
            runCatching { context.startForegroundService(Intent(context, ShieldService::class.java)) }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ShieldService::class.java))
        }
    }
}
