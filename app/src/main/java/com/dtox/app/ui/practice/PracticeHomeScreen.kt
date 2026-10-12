package com.dtox.app.ui.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dtox.app.data.Exercise
import com.dtox.app.data.Exercises
import com.dtox.app.data.PracticeStore
import com.dtox.app.ui.DToxIcons
import kotlin.math.roundToInt

@Composable
fun PracticeHomeScreen(
    gems: Int,
    streak: Int,
    sessions: Int,
    zenMode: Boolean,
    wilting: Boolean,
    onBack: () -> Unit,
    onStartExercise: (Exercise) -> Unit,
    onShop: () -> Unit,
    onScience: () -> Unit,
) {
    val stage = PracticeStore.plantStage(sessions)

    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 20.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(DToxIcons.ArrowBack, contentDescription = "Back")
            }
            Text("Practice", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.weight(1f))
            if (!zenMode) {
                Text(
                    text = "💎 $gems",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (!zenMode) {
                item(key = "plant") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            PlantView(
                                stage = stage,
                                wilting = wilting,
                                modifier = Modifier.fillMaxWidth().height(130.dp),
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = PracticeStore.plantStageName(stage),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = if (streak > 1) {
                                    "Day $streak streak 🔥 · $sessions sessions"
                                } else {
                                    "$sessions sessions"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            items(Exercises, key = { it.id }) { exercise ->
                ExerciseRow(
                    exercise = exercise,
                    zenMode = zenMode,
                    onClick = { onStartExercise(exercise) },
                )
            }

            item(key = "links") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (!zenMode) {
                        OutlinedButton(onClick = onShop, modifier = Modifier.weight(1f)) {
                            Text("Shop")
                        }
                    }
                    OutlinedButton(onClick = onScience, modifier = Modifier.weight(1f)) {
                        Text("The science")
                    }
                }
            }

            item(key = "note") {
                Text(
                    text = "Gems are earned by practicing — never sold. Everything stays on this phone.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun ExerciseRow(exercise: Exercise, zenMode: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = exercise.emoji, fontSize = 26.sp)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(exercise.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = exercise.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                if (!zenMode) {
                    Text(
                        text = "💎 +${exercise.baseGems}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = formatDuration(exercise.durationSeconds),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatDuration(seconds: Int): String = when {
    seconds < 60 -> "~${seconds}s"
    else -> "~${(seconds / 60f).roundToInt()} min"
}
