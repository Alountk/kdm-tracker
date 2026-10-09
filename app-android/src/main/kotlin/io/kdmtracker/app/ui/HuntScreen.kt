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
import io.kdmtracker.core.domain.HuntSpace
import io.kdmtracker.core.domain.HuntSpaceKind
import io.kdmtracker.core.domain.HuntStatus
import io.kdmtracker.core.domain.Survivor
import io.kdmtracker.core.domain.SurvivorStatus

/**
 * Stateless Hunting Phase body.
 *
 * Before setup it offers a quarry/level/players form; afterwards it renders the 13-space board, the
 * selected space's event panel, and the outcome actions (the fast path for table play). No state
 * and no rules live here: everything is reported through the callbacks.
 */
@Composable
fun HuntScreen(
    state: UiState,
    onSetUpHunt: (String, Int, Int) -> Unit,
    onMarkSpaceKind: (Int, HuntSpaceKind) -> Unit,
    onAssignHuntEvent: (Int, String) -> Unit,
    onResolveHuntEvent: (Int, String) -> Unit,
    onAdvanceHunt: () -> Unit,
    onFinishHunt: (HuntStatus) -> Unit,
) {
    val hunt = state.hunt
    if (hunt == null || hunt.spaces.isEmpty()) {
        HuntSetup(state = state, onSetUpHunt = onSetUpHunt)
        return
    }

    var selected by remember(hunt.position) { mutableStateOf(hunt.position) }
    val party = state.survivors.filter { it.status == SurvivorStatus.DEPARTING }

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
            text = "Hunt · ${hunt.quarry} · Level ${hunt.level} · ${hunt.players} player(s)",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Party: ${party.joinToString { survivor -> survivor.partyLabel(hunt.assignments[survivor.id]) }}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        HuntBoard(hunt = hunt, selectedIndex = selected, onSelectIndex = { selected = it })

        HuntEventPanel(
            space = hunt.spaces.getOrElse(selected) { HuntSpace() },
            index = selected,
            huntEvents = state.huntEvents,
            onAssign = { onAssignHuntEvent(selected, it) },
            onResolve = { onResolveHuntEvent(selected, it) },
            onMarkKind = { onMarkSpaceKind(selected, it) },
            onAdvance = onAdvanceHunt,
        )

        Text(text = "Outcome", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onFinishHunt(HuntStatus.QUARRY_FOUND) }, modifier = Modifier.weight(1f)) {
                Text("Find quarry")
            }
            OutlinedButton(onClick = { onFinishHunt(HuntStatus.AMBUSH) }, modifier = Modifier.weight(1f)) {
                Text("Ambush")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onFinishHunt(HuntStatus.RUN_AWAY) }, modifier = Modifier.weight(1f)) {
                Text("Run away")
            }
            OutlinedButton(onClick = { onFinishHunt(HuntStatus.FAILED) }, modifier = Modifier.weight(1f)) {
                Text("Hunt fails")
            }
        }
    }
}

@Composable
private fun HuntSetup(
    state: UiState,
    onSetUpHunt: (String, Int, Int) -> Unit,
) {
    var quarry by remember(state.quarries) { mutableStateOf(state.quarries.firstOrNull().orEmpty()) }
    var level by remember { mutableIntStateOf(1) }
    var players by remember { mutableIntStateOf(1) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(text = state.settlementName, style = MaterialTheme.typography.headlineMedium)
        Text(text = "Set up the hunt", style = MaterialTheme.typography.titleMedium)

        Text(text = "Quarry", style = MaterialTheme.typography.titleSmall)
        state.quarries.forEach { name ->
            OutlinedButton(onClick = { quarry = name }, modifier = Modifier.fillMaxWidth()) {
                Text(if (name == quarry) "• $name" else name)
            }
        }

        IntStepper(label = "Level", value = level, min = 1, max = 3, onChange = { level = it })
        IntStepper(label = "Players", value = players, min = 1, max = 4, onChange = { players = it })

        Button(
            onClick = { onSetUpHunt(quarry, level, players) },
            enabled = quarry.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Set up")
        }
    }
}

@Composable
private fun IntStepper(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    onChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = label, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = { onChange((value - 1).coerceAtLeast(min)) }) { Text("−") }
        Text(text = value.toString(), style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = { onChange((value + 1).coerceAtMost(max)) }) { Text("+") }
    }
}

private fun Survivor.partyLabel(player: Int?): String {
    val name = name.ifBlank { "Unnamed" }
    return if (player == null) name else "P$player:$name"
}
