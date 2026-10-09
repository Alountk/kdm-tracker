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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kdmtracker.app.UiState
import io.kdmtracker.core.domain.Ulid

/**
 * Settlement Step 9 (a Special Showdown year). The player declares the special showdown; when
 * declared, the shared encounter tracker runs and the conclude action loops back to step 5.
 */
@Composable
fun SpecialShowdownStepScreen(
    state: UiState,
    onDeclareSpecialShowdown: (Boolean) -> Unit,
    onSetUpShowdown: (String, Int, List<String>) -> Unit,
    onDrawShowdownCard: () -> Unit,
    onWoundMonster: () -> Unit,
    onGrantHuntXp: (List<Ulid>) -> Unit,
    onRecordDeaths: (Int) -> Unit,
    onConclude: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Special Showdown", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Declare a showdown this year; it loops back to the death-count step when it concludes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = "Special Showdown this year", modifier = Modifier.weight(1f))
            Switch(checked = state.specialShowdown, onCheckedChange = onDeclareSpecialShowdown)
        }

        if (!state.specialShowdown) {
            return@Column
        }

        val showdown = state.showdown
        if (showdown == null || showdown.monster.isBlank()) {
            EncounterSetup(state = state, onSetUpShowdown = onSetUpShowdown)
            return@Column
        }

        EncounterPanel(
            state = state,
            onDrawShowdownCard = onDrawShowdownCard,
            onWoundMonster = onWoundMonster,
            onGrantHuntXp = onGrantHuntXp,
            onRecordDeaths = onRecordDeaths,
            outcome = {
                Button(onClick = onConclude, modifier = Modifier.fillMaxWidth()) {
                    Text("Conclude · back to Step 5")
                }
            },
        )
    }
}
