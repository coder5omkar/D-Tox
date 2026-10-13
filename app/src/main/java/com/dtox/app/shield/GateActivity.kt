package com.dtox.app.shield

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dtox.app.data.AppInfo
import com.dtox.app.data.PracticeStore
import com.dtox.app.data.Prefs
import com.dtox.app.ui.BreathScreen
import com.dtox.app.ui.theme.AppTheme
import com.dtox.app.ui.theme.DToxTheme

/**
 * The breath gate that Shield mode shows on top of a gated app. Reuses the
 * exact same breathing UI as the rest of D-Tox.
 */
class GateActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pkg = intent?.getStringExtra(EXTRA_PACKAGE)
        val prefs = Prefs(this)

        if (pkg == null || !prefs.gatedApps.contains(pkg)) {
            finish()
            return
        }

        val app = runCatching {
            val info = packageManager.getApplicationInfo(pkg, 0)
            AppInfo(
                pkg = pkg,
                label = packageManager.getApplicationLabel(info).toString(),
                icon = info.loadIcon(packageManager),
            )
        }.getOrNull()

        if (app == null) {
            finish()
            return
        }

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        val theme = AppTheme.from(PracticeStore(this).themeId)

        setContent {
            DToxTheme(theme = theme) {
                BreathScreen(
                    app = app,
                    prefs = prefs,
                    onOpenApp = {
                        prefs.recordPause(prefs.breathSeconds)
                        grantGrace(prefs, pkg)
                        openTarget(pkg)
                    },
                    onNotNow = {
                        goHome()
                    },
                )
            }
        }
    }

    private fun grantGrace(prefs: Prefs, pkg: String) {
        prefs.shieldGracePackage = pkg
        prefs.shieldGraceUntil = SystemClock.elapsedRealtime() + GRACE_MS
    }

    private fun openTarget(pkg: String) {
        val launch = packageManager.getLaunchIntentForPackage(pkg)
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { startActivity(launch) }
        } else {
            Toast.makeText(this, "App not found", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    private fun goHome() {
        val home = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(home) }
        finish()
    }

    companion object {
        const val EXTRA_PACKAGE = "com.dtox.app.extra.SHIELD_PACKAGE"
        private const val GRACE_MS = 5 * 60_000L
    }
}
