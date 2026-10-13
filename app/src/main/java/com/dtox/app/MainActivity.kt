package com.dtox.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dtox.app.ui.DToxRoot
import com.dtox.app.data.Prefs
import com.dtox.app.shield.ShieldManager
import com.dtox.app.shield.ShieldService

class MainActivity : ComponentActivity() {

    /**
     * Package of the app currently being gated, or null for the normal
     * home UI. Set from the pinned shortcut intent (cold start or
     * onNewIntent) or from a tap on a tile inside D-Tox.
     */
    private var gateRequest: String? by mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        // Re-arm shield mode whenever the app is opened (also covers reboot).
        if (Prefs(this).shieldEnabled && ShieldManager.hasUsageAccess(this)) {
            ShieldService.start(this)
        }

        gateRequest = if (savedInstanceState != null) {
            savedInstanceState.getString(KEY_GATE)
        } else {
            intent?.getStringExtra(EXTRA_GATE_PACKAGE)
        }

        setContent {
            DToxRoot(
                gateRequest = gateRequest,
                onClearGate = { gateRequest = null },
                onRequestGate = { pkg -> gateRequest = pkg },
                onLaunchApp = { pkg -> openApp(pkg) },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(EXTRA_GATE_PACKAGE)?.let { gateRequest = it }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_GATE, gateRequest)
    }

    private fun openApp(pkg: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
        if (launchIntent == null) {
            Toast.makeText(this, getString(R.string.app_not_found), Toast.LENGTH_SHORT).show()
            gateRequest = null
            return
        }
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(launchIntent)
            finish()
        } catch (_: ActivityNotFoundException) {
            gateRequest = null
        }
    }

    companion object {
        const val EXTRA_GATE_PACKAGE = "com.dtox.app.extra.GATE_PACKAGE"
        private const val KEY_GATE = "gate_request"
    }
}
