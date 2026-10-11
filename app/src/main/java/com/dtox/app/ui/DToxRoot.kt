package com.dtox.app.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dtox.app.data.AppInfo
import com.dtox.app.data.Prefs
import com.dtox.app.data.loadLaunchableApps
import com.dtox.app.util.pinGateShortcut
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class Screen { HOME, PICKER, SETTINGS }

@Composable
fun DToxRoot(
    gateRequest: String?,
    onClearGate: () -> Unit,
    onRequestGate: (String) -> Unit,
    onLaunchApp: (String) -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }

    val appsState by produceState<List<AppInfo>?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { loadLaunchableApps(context) }
    }

    var gated by remember { mutableStateOf(prefs.gatedApps) }
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }

    BackHandler(enabled = gateRequest != null) { onClearGate() }
    BackHandler(enabled = gateRequest == null && screen != Screen.HOME) { screen = Screen.HOME }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        val allApps = appsState
        when {
            gateRequest != null -> {
                val gateApp = allApps?.firstOrNull { it.pkg == gateRequest }
                when {
                    gateApp != null -> BreathScreen(
                        app = gateApp,
                        prefs = prefs,
                        onOpenApp = {
                            prefs.recordPause(prefs.breathSeconds)
                            onLaunchApp(gateApp.pkg)
                        },
                        onNotNow = onClearGate,
                    )

                    allApps == null -> MessageScreen("Loading…")

                    else -> MessageScreen("This app isn't installed anymore.") { onClearGate() }
                }
            }

            screen == Screen.PICKER && allApps != null -> PickerScreen(
                apps = allApps,
                gated = gated,
                onToggle = { pkg ->
                    gated = if (gated.contains(pkg)) gated - pkg else gated + pkg
                    prefs.gatedApps = gated
                },
                onBack = { screen = Screen.HOME },
            )

            screen == Screen.SETTINGS -> SettingsScreen(
                prefs = prefs,
                onBack = { screen = Screen.HOME },
            )

            else -> HomeScreen(
                apps = allApps?.filter { gated.contains(it.pkg) }.orEmpty(),
                loading = allApps == null,
                pausesToday = prefs.pausesToday(),
                totalPauses = prefs.totalPauses,
                totalSeconds = prefs.totalSeconds,
                onSettings = { screen = Screen.SETTINGS },
                onPickApps = { screen = Screen.PICKER },
                onStartGate = { app -> onRequestGate(app.pkg) },
                onPin = { app ->
                    val ok = pinGateShortcut(context, app)
                    if (!ok) {
                        Toast.makeText(
                            context,
                            "Your launcher doesn't support pinned shortcuts.",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                },
            )
        }
    }
}

@Composable
private fun MessageScreen(text: String, onClose: (() -> Unit)? = null) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (onClose != null) {
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onClose) { Text("Close") }
            }
        }
    }
}
