package com.dtox.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dtox.app.data.BreathPattern
import com.dtox.app.data.PracticeStore
import com.dtox.app.data.Prefs

@Composable
fun SettingsScreen(
    prefs: Prefs,
    store: PracticeStore,
    zenMode: Boolean,
    onZenChange: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    var breathSeconds by remember { mutableIntStateOf(prefs.breathSeconds) }
    var patternId by remember { mutableStateOf(prefs.patternId) }
    var haptics by remember { mutableStateOf(prefs.haptics) }
    var allowSkip by remember { mutableStateOf(prefs.allowSkip) }

    val patterns = BreathPattern.entries.filter { store.patternAvailable(it) }

    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(DToxIcons.ArrowBack, contentDescription = "Back")
            }
            Text("Settings", style = MaterialTheme.typography.titleLarge)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            SectionLabel("Gate breath length")
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(15, 20, 30, 45, 60).forEach { value ->
                    FilterChip(
                        selected = breathSeconds == value,
                        onClick = {
                            breathSeconds = value
                            prefs.breathSeconds = value
                        },
                        label = { Text("${value}s") },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("Gate breathing pattern")
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                patterns.forEach { pattern ->
                    FilterChip(
                        selected = patternId == pattern.id,
                        onClick = {
                            patternId = pattern.id
                            prefs.patternId = pattern.id
                        },
                        label = { Text(pattern.label) },
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "More patterns unlock in the Practice shop.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(20.dp))
            SwitchRow(
                title = "Vibrate on each phase",
                subtitle = "A small tick to follow, even with eyes closed",
                checked = haptics,
                onChange = {
                    haptics = it
                    prefs.haptics = it
                },
            )
            SwitchRow(
                title = "Allow opening early",
                subtitle = "Skip the rest of the gate breath — try to keep this off",
                checked = allowSkip,
                onChange = {
                    allowSkip = it
                    prefs.allowSkip = it
                },
            )

            Spacer(Modifier.height(24.dp))
            SectionLabel("Practice")
            SwitchRow(
                title = "Zen mode",
                subtitle = "Hide gems, streaks and the plant — just exercises",
                checked = zenMode,
                onChange = onZenChange,
            )

            Spacer(Modifier.height(24.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp)) {
                    Text("How the gate works", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "1. Choose the apps you want to slow down.\n" +
                            "2. Tap Pin and add the D-Tox shortcut to your home screen.\n" +
                            "3. Remove the original icon from your home screen.\n" +
                            "4. From now on, open those apps through the D-Tox shortcut.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp)) {
                    Text("D-Tox 1.2.0", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Zero permissions. No internet. No background services. " +
                            "No accounts. No subscriptions. Gems are earned by practicing — " +
                            "never sold. Nothing ever leaves this phone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
