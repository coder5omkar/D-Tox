package com.dtox.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.dtox.app.data.AppInfo
import com.dtox.app.data.PracticeStore

@Composable
fun HomeScreen(
    apps: List<AppInfo>,
    loading: Boolean,
    pausesToday: Int,
    totalPauses: Int,
    totalSeconds: Long,
    gems: Int,
    sessions: Int,
    streak: Int,
    onSettings: () -> Unit,
    onPractice: () -> Unit,
    onPickApps: () -> Unit,
    onStartGate: (AppInfo) -> Unit,
    onPin: (AppInfo) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "D-Tox",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onSettings) {
                Icon(
                    imageVector = DToxIcons.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            GateSummaryCard(
                pausesToday = pausesToday,
                totalPauses = totalPauses,
                totalSeconds = totalSeconds,
                modifier = Modifier.weight(1f),
            )
            PracticeSummaryCard(
                gems = gems,
                sessions = sessions,
                streak = streak,
                onClick = onPractice,
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            text = "🌱 Your plant grows with every practice session — currently: " +
                PracticeStore.plantStageName(PracticeStore.plantStage(sessions)) +
                ". Tap Practice to see it.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
        )

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            apps.isEmpty() -> EmptyState(onPickApps)

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(apps, key = { it.pkg }) { app ->
                    GateRow(app = app, onStartGate = onStartGate, onPin = onPin)
                }
                item {
                    TextButton(onClick = onPickApps, modifier = Modifier.fillMaxWidth()) {
                        Icon(DToxIcons.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Choose apps")
                    }
                }
                item {
                    Text(
                        text = "Pin a D-Tox shortcut to your home screen, then remove the original app icon. " +
                            "No internet permission · No background services.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun GateSummaryCard(
    pausesToday: Int,
    totalPauses: Int,
    totalSeconds: Long,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(
                text = "GATE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            MiniStat(pausesToday.toString(), "today")
            MiniStat(totalPauses.toString(), "all time")
            MiniStat(formatSeconds(totalSeconds), "breathed")
        }
    }
}

@Composable
private fun PracticeSummaryCard(
    gems: Int,
    sessions: Int,
    streak: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "PRACTICE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "›",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            MiniStat("💎 $gems", "gems")
            MiniStat(sessions.toString(), "sessions")
            MiniStat(if (streak > 0) "$streak 🔥" else "—", "day streak")
        }
    }
}

@Composable
private fun MiniStat(value: String, label: String) {
    Row(
        modifier = Modifier.padding(vertical = 1.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 2.dp),
        )
    }
}

@Composable
private fun GateRow(app: AppInfo, onStartGate: (AppInfo) -> Unit, onPin: (AppInfo) -> Unit) {
    Card(
        onClick = { onStartGate(app) },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(app, 44.dp)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "One breath before opening",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = { onPin(app) }) { Text("Pin") }
        }
    }
}

@Composable
fun AppIcon(app: AppInfo, size: Dp) {
    val bitmap = remember(app.pkg) { app.icon.toBitmap(96, 96).asImageBitmap() }
    Image(
        bitmap = bitmap,
        contentDescription = app.label,
        modifier = Modifier.size(size),
    )
}

@Composable
private fun EmptyState(onPickApps: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "A gentler way in",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Pick the apps that pull you in. When you open them through D-Tox, " +
                "you take one quiet breath first — 30 seconds — before the app appears.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onPickApps) {
            Icon(DToxIcons.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Choose apps")
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "No internet permission\nNo background services\nNo accounts · No subscriptions",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private fun formatSeconds(total: Long): String = when {
    total <= 0L -> "0s"
    total < 60L -> "${total}s"
    total < 3600L -> "${total / 60}m"
    else -> "${total / 3600}h ${(total % 3600) / 60}m"
}
