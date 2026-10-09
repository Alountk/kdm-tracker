package io.kdmtracker.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import io.kdmtracker.app.UiState
import io.kdmtracker.core.domain.MonsterCard
import io.kdmtracker.core.domain.MonsterCardKind
import io.kdmtracker.core.domain.ShowdownStatus
import io.kdmtracker.core.domain.SurvivorStatus
import io.kdmtracker.core.domain.Ulid

/**
 * Stateless Showdown Phase body.
 *
 * Before setup it offers a monster/level form; afterwards it tracks the monster's AI deck (the
 * monster's health), references the hit locations, and records the outcome plus the aftermath.
 * No state and no rules live here: everything is reported through the callbacks.
 */
@Composable
fun ShowdownScreen(
    state: UiState,
    onSetUpShowdown: (String, Int, List<String>) -> Unit,
    onDrawShowdownCard: () -> Unit,
    onWoundMonster: () -> Unit,
    onGrantHuntXp: (List<Ulid>) -> Unit,
    onFinishShowdown: (ShowdownStatus) -> Unit,
) {
    val showdown = state.showdown
    if (showdown == null || showdown.monster.isBlank()) {
        ShowdownSetup(state = state, onSetUpShowdown = onSetUpShowdown)
        return
    }

    val aiCards = state.monsterCards.filter { it.monster == showdown.monster && it.kind == MonsterCardKind.AI }
    val hitLocations =
        state.monsterCards.filter { it.monster == showdown.monster && it.kind == MonsterCardKind.HIT_LOCATION }
    val lastDrawnText = aiCards.firstOrNull { it.name == showdown.lastDrawn }?.text
    val party = state.survivors.filter { it.status == SurvivorStatus.DEPARTING }
    var showHitLocations by remember { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = state.settlementName, style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "Showdown · ${showdown.monster} · Level ${showdown.level}",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text =
                "AI deck ${showdown.aiDeck.size} · wounded ${showdown.wounds.size} · " +
                    "discard ${showdown.discard.size}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.actionError?.let { message ->
            Text(text = message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }

        showdown.lastDrawn?.let { name ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(text = name, style = MaterialTheme.typography.titleSmall)
                    Text(text = lastDrawnText.orEmpty(), style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onDrawShowdownCard, modifier = Modifier.weight(1f)) { Text("Draw") }
            Button(onClick = onWoundMonster, modifier = Modifier.weight(1f)) { Text("Wound") }
        }

        OutlinedButton(onClick = { showHitLocations = !showHitLocations }, modifier = Modifier.fillMaxWidth()) {
            Text(if (showHitLocations) "Hide hit locations" else "Hit locations (${hitLocations.size})")
        }
        if (showHitLocations) {
            hitLocations.forEach { card -> MonsterCardRow(card) }
        }

        Text(text = "Outcome", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onFinishShowdown(ShowdownStatus.VICTORY) }, modifier = Modifier.weight(1f)) {
                Text("Victory")
            }
            OutlinedButton(onClick = { onFinishShowdown(ShowdownStatus.DEFEAT) }, modifier = Modifier.weight(1f)) {
                Text("Defeat")
            }
        }

        Text(text = "Aftermath", style = MaterialTheme.typography.titleSmall)
        OutlinedButton(
            onClick = { onGrantHuntXp(party.map { it.id }) },
            enabled = party.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Grant +1 hunt XP to the party (${party.size})")
        }
    }
}

@Composable
private fun MonsterCardRow(card: MonsterCard) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = card.name, style = MaterialTheme.typography.titleSmall)
            Text(text = card.text, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ShowdownSetup(
    state: UiState,
    onSetUpShowdown: (String, Int, List<String>) -> Unit,
) {
    val monsters = state.quarries + state.nemeses
    var monster by remember(monsters) { mutableStateOf(monsters.firstOrNull().orEmpty()) }
    var level by remember { mutableIntStateOf(1) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(text = state.settlementName, style = MaterialTheme.typography.headlineMedium)
        Text(text = "Set up the showdown", style = MaterialTheme.typography.titleMedium)

        Text(text = "Monster", style = MaterialTheme.typography.titleSmall)
        monsters.forEach { name ->
            OutlinedButton(onClick = { monster = name }, modifier = Modifier.fillMaxWidth()) {
                Text(if (name == monster) "• $name" else name)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = "Level", modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { level = (level - 1).coerceAtLeast(1) }) { Text("−") }
            Text(text = level.toString(), style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = { level = (level + 1).coerceAtMost(3) }) { Text("+") }
        }

        val deck =
            state.monsterCards
                .filter { it.monster == monster && it.kind == MonsterCardKind.AI }
                .map { it.name }
        Button(
            onClick = { onSetUpShowdown(monster, level, deck) },
            enabled = monster.isNotBlank() && deck.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Set up (${deck.size} AI cards)")
        }
    }
}
