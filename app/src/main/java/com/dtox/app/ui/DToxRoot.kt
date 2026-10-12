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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.dtox.app.data.AppInfo
import com.dtox.app.data.PracticeStore
import com.dtox.app.data.Prefs
import com.dtox.app.data.exerciseById
import com.dtox.app.data.loadLaunchableApps
import com.dtox.app.ui.practice.ExercisePlayerScreen
import com.dtox.app.ui.practice.PracticeHomeScreen
import com.dtox.app.ui.practice.ProgressScreen
import com.dtox.app.ui.practice.ShopScreen
import com.dtox.app.ui.theme.AppTheme
import com.dtox.app.ui.theme.DToxTheme
import com.dtox.app.util.findActivity
import com.dtox.app.util.pinGateShortcut
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class Screen { HOME, PICKER, SETTINGS, PRACTICE, SHOP, SCIENCE, PROGRESS, PLAYER }

@Composable
fun DToxRoot(
    gateRequest: String?,
    onClearGate: () -> Unit,
    onRequestGate: (String) -> Unit,
    onLaunchApp: (String) -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    val store = remember { PracticeStore(context) }

    val appsState by produceState<List<AppInfo>?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { loadLaunchableApps(context) }
    }

    var gated by remember { mutableStateOf(prefs.gatedApps) }
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    var playerExerciseId by rememberSaveable { mutableStateOf("") }

    // Practice state mirrors, refreshed after player / shop actions.
    var gems by remember { mutableIntStateOf(store.gems) }
    var streak by remember { mutableIntStateOf(store.streak) }
    var sessions by remember { mutableIntStateOf(store.totalSessions) }
    var zenMode by remember { mutableStateOf(store.zenMode) }
    var themeId by remember { mutableStateOf(store.themeId) }

    fun refreshPractice() {
        gems = store.gems
        streak = store.streak
        sessions = store.totalSessions
        zenMode = store.zenMode
        themeId = store.themeId
    }

    val theme = AppTheme.from(themeId)

    // Match the system bars to the active theme (Paper is a light theme).
    val view = LocalView.current
    val activity = context.findActivity()
    SideEffect {
        val window = activity?.window ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = theme.light
        controller.isAppearanceLightNavigationBars = theme.light
    }

    BackHandler(enabled = gateRequest != null) { onClearGate() }
    BackHandler(enabled = gateRequest == null && screen != Screen.HOME) {
        screen = when (screen) {
            Screen.PICKER, Screen.SETTINGS -> Screen.HOME
            else -> Screen.PRACTICE
        }
        refreshPractice()
    }

    DToxTheme(theme = theme) {
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
                    store = store,
                    zenMode = zenMode,
                    onZenChange = { value ->
                        store.zenMode = value
                        zenMode = value
                    },
                    onBack = {
                        refreshPractice()
                        screen = Screen.HOME
                    },
                )

                screen == Screen.PRACTICE -> PracticeHomeScreen(
                    gems = gems,
                    streak = streak,
                    sessions = sessions,
                    zenMode = zenMode,
                    wilting = (store.daysSinceLastSession() ?: 0L) >= 3L,
                    onBack = { screen = Screen.HOME },
                    onStartExercise = { exercise ->
                        playerExerciseId = exercise.id
                        screen = Screen.PLAYER
                    },
                    onShop = { screen = Screen.SHOP },
                    onProgress = { screen = Screen.PROGRESS },
                    onScience = { screen = Screen.SCIENCE },
                )

                screen == Screen.PROGRESS -> ProgressScreen(
                    store = store,
                    onBack = {
                        refreshPractice()
                        screen = Screen.PRACTICE
                    },
                )

                screen == Screen.SHOP -> ShopScreen(
                    store = store,
                    gems = gems,
                    themeId = themeId,
                    onChanged = { refreshPractice() },
                    onBack = {
                        refreshPractice()
                        screen = Screen.PRACTICE
                    },
                )

                screen == Screen.SCIENCE -> ScienceScreen(onBack = { screen = Screen.PRACTICE })

                screen == Screen.PLAYER -> {
                    val exercise = exerciseById(playerExerciseId)
                    if (exercise != null) {
                        ExercisePlayerScreen(
                            exercise = exercise,
                            store = store,
                            zenMode = zenMode,
                            haptics = prefs.haptics,
                            onSessionRecorded = { refreshPractice() },
                            onGemsChanged = { refreshPractice() },
                            onExit = {
                                refreshPractice()
                                screen = Screen.PRACTICE
                            },
                        )
                    } else {
                        MessageScreen("Exercise not found") { screen = Screen.PRACTICE }
                    }
                }

                else -> HomeScreen(
                    apps = allApps?.filter { gated.contains(it.pkg) }.orEmpty(),
                    loading = allApps == null,
                    pausesToday = prefs.pausesToday(),
                    totalPauses = prefs.totalPauses,
                    totalSeconds = prefs.totalSeconds,
                    gems = gems,
                    sessions = sessions,
                    streak = streak,
                    onSettings = { screen = Screen.SETTINGS },
                    onPractice = { screen = Screen.PRACTICE },
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
