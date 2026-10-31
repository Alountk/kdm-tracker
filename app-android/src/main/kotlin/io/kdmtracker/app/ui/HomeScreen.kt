package io.kdmtracker.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kdmtracker.app.CampaignCard

/**
 * Stateless rendering of the Home: one tappable card per campaign and a "New settlement" form.
 *
 * Each card shows `name · Lantern Year N · Population P` and the campaign's sharing badge
 * [CampaignCard.banner], so an owner-alone, owner-with-others and shared-with-me campaign are told
 * apart at a glance. The screen owns no state or rules: selections and creates are
 * reported through the callbacks; sync and sign-in live on the Account screen.
 */
@Composable
fun HomeScreen(
    cards: List<CampaignCard>,
    onSelect: (String) -> Unit,
    onCreate: (String, Set<String>) -> Unit,
    newSettlementName: String,
    onNewSettlementName: (String) -> Unit,
    error: String?,
    expansions: List<ExpansionOption> = emptyList(),
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Settlements", style = MaterialTheme.typography.headlineMedium)

        error?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        NewSettlementForm(
            name = newSettlementName,
            onNameChange = onNewSettlementName,
            onCreate = onCreate,
            expansions = expansions,
        )

        if (cards.isEmpty()) {
            Text(
                text = "No settlements yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            cards.forEach { card ->
                CampaignCardItem(card = card, onSelect = onSelect)
            }
        }
    }
}

@Composable
private fun CampaignCardItem(
    card: CampaignCard,
    onSelect: (String) -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onSelect(card.campaignId) },
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "${card.name} · Lantern Year ${card.lanternYear} · Population ${card.population}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = card.banner.label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NewSettlementForm(
    name: String,
    onNameChange: (String) -> Unit,
    onCreate: (String, Set<String>) -> Unit,
    expansions: List<ExpansionOption>,
) {
    var picked by rememberSaveable { mutableStateOf(emptySet<String>()) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("New settlement") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = { onCreate(name, picked) },
                enabled = name.isNotBlank(),
            ) {
                Text("Create")
            }
        }
        if (expansions.isNotEmpty()) {
            Text(
                text = "Expansions",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                expansions.forEach { option ->
                    FilterChip(
                        selected = option.id in picked,
                        onClick = { picked = toggledExpansion(picked, option.id) },
                        label = { Text(option.name) },
                    )
                }
            }
        }
    }
}
