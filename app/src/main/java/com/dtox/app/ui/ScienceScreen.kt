package com.dtox.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Plain-language summary of the research the exercises are built on, plus
 * honest caveats. Text only — no new dependencies, no claims we can't back.
 */
@Composable
fun ScienceScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 20.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(DToxIcons.ArrowBack, contentDescription = "Back")
            }
            Text("The science", style = MaterialTheme.typography.titleLarge)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text(
                text = "Evidence-informed, not magic",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Every exercise here maps to techniques studied in clinical " +
                    "research. The effects are real but modest — this is a tool, not a cure.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ScienceSection(
                title = "Slow breathing & cyclic sighs",
                body = "Slow breathing around six breaths a minute reliably shifts the " +
                    "nervous system toward calm and raises heart-rate variability " +
                    "(Zaccaro et al., 2018, Frontiers in Human Neuroscience). In a 2023 " +
                    "Stanford study (Balban et al., Cell Reports Medicine), one month of " +
                    "daily cyclic sighing improved mood and lowered anxiety more than " +
                    "mindfulness meditation — the long exhale does the work.",
            )

            ScienceSection(
                title = "Body scan",
                body = "Body-scan practice is a core part of mindfulness-based stress " +
                    "reduction (MBSR). Studies show lower cortisol and better emotional " +
                    "regulation (Ditto et al., 2006), and training attention on the body " +
                    "strengthens interoceptive awareness (Farb et al., 2013).",
            )

            ScienceSection(
                title = "Grounding, 5·4·3·2·1",
                body = "A standard clinical technique for acute anxiety and panic: it " +
                    "interrupts rumination by moving attention to the senses. Widely used " +
                    "in trauma-informed care; its evidence is strong clinical practice " +
                    "rather than large controlled trials.",
            )

            ScienceSection(
                title = "Gratitude",
                body = "Noticing one good thing improves mood and even sleep in controlled " +
                    "studies (Emmons & McCullough, 2003). Effects are small but reliable, " +
                    "and it often works better a few times a week than every single day.",
            )

            ScienceSection(
                title = "What the research overall shows",
                body = "Meta-analyses of meditation programs (Goyal et al., 2014, JAMA " +
                    "Internal Medicine) find moderate evidence for reduced anxiety, " +
                    "depression and pain, and lower evidence for stress and quality of " +
                    "life. Structural brain changes have been observed after about eight " +
                    "weeks of consistent practice (Hölzel et al., 2011).",
            )

            Spacer(Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Honest caveats", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Effect sizes are modest. The field has publication bias, " +
                            "and responses vary a lot between people. Nothing here replaces " +
                            "therapy for clinical anxiety or depression. If you're struggling, " +
                            "please talk to a professional.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = "No tracking. What you practice here stays on this phone.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ScienceSection(title: String, body: String) {
    Spacer(Modifier.height(18.dp))
    Text(text = title, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(6.dp))
    Text(
        text = body,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
