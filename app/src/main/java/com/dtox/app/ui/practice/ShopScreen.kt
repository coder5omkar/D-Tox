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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dtox.app.data.PracticeStore
import com.dtox.app.data.ShopItem
import com.dtox.app.data.ShopItemType
import com.dtox.app.ui.DToxIcons

@Composable
fun ShopScreen(
    store: PracticeStore,
    gems: Int,
    themeId: String,
    onChanged: () -> Unit,
    onBack: () -> Unit,
) {
    var confirmItem by remember { mutableStateOf<ShopItem?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 20.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(DToxIcons.ArrowBack, contentDescription = "Back")
            }
            Text("Shop", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.weight(1f))
            Text(
                text = "💎 $gems",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }

        message?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                textAlign = TextAlign.Center,
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "themes-title") {
                SectionTitle("Themes")
            }
            items(ShopItem.ALL.filter { it.type == ShopItemType.THEME }, key = { it.id }) { item ->
                ShopRow(
                    item = item,
                    themeId = themeId,
                    owned = store.isUnlocked(item.id),
                    onBuy = { confirmItem = item },
                    onApply = {
                        store.themeId = item.value
                        onChanged()
                    },
                )
            }

            item(key = "patterns-title") {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    SectionTitle("Breathing patterns")
                    Text(
                        text = "Unlocked patterns appear in the gate settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(ShopItem.ALL.filter { it.type == ShopItemType.PATTERN }, key = { it.id }) { item ->
                ShopRow(
                    item = item,
                    themeId = themeId,
                    owned = store.isUnlocked(item.id),
                    onBuy = { confirmItem = item },
                    onApply = {},
                )
            }

            item(key = "note") {
                Text(
                    text = "Gems are earned by practicing. They are never sold, and nothing leaves your phone.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                )
            }
        }
    }

    val item = confirmItem
    if (item != null) {
        AlertDialog(
            onDismissRequest = { confirmItem = null },
            title = { Text("Unlock ${item.name}?") },
            text = { Text("Costs 💎 ${item.cost}. You have 💎 $gems.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val ok = store.unlock(item.id)
                        if (ok) {
                            if (item.type == ShopItemType.THEME) {
                                store.themeId = item.value
                            }
                            message = null
                            onChanged()
                        } else {
                            message = "Not enough gems yet — keep practicing 🌱"
                        }
                        confirmItem = null
                    },
                ) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmItem = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ShopRow(
    item: ShopItem,
    themeId: String,
    owned: Boolean,
    onBuy: () -> Unit,
    onApply: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            when {
                owned && item.type == ShopItemType.THEME && themeId == item.value -> Text(
                    text = "Active",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )

                owned && item.type == ShopItemType.THEME -> TextButton(onClick = onApply) { Text("Apply") }

                owned -> Text(
                    text = "Owned",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )

                else -> TextButton(onClick = onBuy) { Text("💎 ${item.cost}") }
            }
        }
    }
}
