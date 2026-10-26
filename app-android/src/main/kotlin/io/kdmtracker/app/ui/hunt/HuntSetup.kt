package io.kdmtracker.app.ui.hunt

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

@Composable
internal fun HuntSetup(
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
