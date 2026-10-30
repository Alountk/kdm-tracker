package io.kdmtracker.app.ui.showdown

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import io.kdmtracker.core.domain.MonsterCardKind

/** The monster/level form; pre-selects [UiState.nemesis] on a nemesis year. */
@Composable
fun EncounterSetup(
    state: UiState,
    onSetUpShowdown: (String, Int, List<String>) -> Unit,
) {
    val monsters = state.quarries + state.nemeses
    var monster by remember(monsters, state.nemesis) {
        mutableStateOf(state.nemesis?.takeIf { it in monsters } ?: monsters.firstOrNull().orEmpty())
    }
    var level by remember { mutableIntStateOf(1) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(text = state.settlementName, style = MaterialTheme.typography.headlineMedium)
        Text(text = "Preparar el enfrentamiento", style = MaterialTheme.typography.titleMedium)

        Text(text = "Monstruo", style = MaterialTheme.typography.titleSmall)
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
            Text(text = "Nivel", modifier = Modifier.weight(1f))
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
            Text("Preparar (${deck.size} cartas de IA)")
        }
    }
}
