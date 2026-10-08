package io.kdmtracker.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kdmtracker.app.UiState
import io.kdmtracker.core.domain.Core16Milestones
import io.kdmtracker.core.domain.PrincipleCategory
import io.kdmtracker.core.ruleset.PrincipleChoice

/**
 * Step 5 "Update the Death Count" body.
 *
 * Records deaths (which lower the population) and births (the counter panel's second input, the
 * only place the New Life milestone can be fed), and surfaces the one-time `Principle: Death`
 * choice once the first death is recorded. Stateless beyond the two text fields; every action is
 * reported through a callback and no rule lives here.
 */
@Composable
fun DeathCountStepScreen(
    state: UiState,
    onRecordDeaths: (Int) -> Unit,
    onRecordBirths: (Int) -> Unit,
    onChoosePrinciple: (PrincipleCategory, String) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(text = state.stepTitle, style = MaterialTheme.typography.headlineMedium)
        Text(
            text =
                "Deaths ${state.deaths} · Births ${state.births} · Population ${state.population}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        CounterRow(label = "Deaths", onRecord = onRecordDeaths)
        CounterRow(label = "Births", onRecord = onRecordBirths)

        val death =
            state.milestoneViews.firstOrNull { it.id == Core16Milestones.PRINCIPLE_DEATH }
        if (death != null && death.reached) {
            PrincipleCard(
                title = "Principle: Death",
                choices = death.choices,
                chosen = state.chosenPrinciples[PrincipleCategory.DEATH],
                onChoosePrinciple = onChoosePrinciple,
            )
        }
    }
}

@Composable
private fun CounterRow(
    label: String,
    onRecord: (Int) -> Unit,
) {
    var amount by remember { mutableStateOf("1") }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it.filter(Char::isDigit) },
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        Button(
            onClick = { onRecord(amount.toIntOrNull() ?: 0) },
            modifier = Modifier.width(120.dp),
        ) {
            Text("Record")
        }
    }
}

@Composable
private fun PrincipleCard(
    title: String,
    choices: List<PrincipleChoice>,
    chosen: String?,
    onChoosePrinciple: (PrincipleCategory, String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            chosen?.let { Text(text = "Chosen: $it", style = MaterialTheme.typography.bodyMedium) }
            choices.forEach { choice ->
                OutlinedButton(
                    onClick = { onChoosePrinciple(choice.category, choice.label) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(choice.label)
                }
            }
        }
    }
}
