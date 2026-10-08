package io.kdmtracker.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kdmtracker.app.MilestoneView
import io.kdmtracker.app.UiState
import io.kdmtracker.core.domain.MilestoneId
import io.kdmtracker.core.domain.PrincipleCategory

/**
 * Step 6 "Check Milestones" body.
 *
 * Lists the ruleset's milestones with their reached/resolved state, exposes the principle
 * choices for the principle milestones and a Resolve button for any reached, unresolved one.
 * Stateless: every list comes from [state] and every action is reported through a callback.
 */
@Composable
fun MilestonesStepScreen(
    state: UiState,
    onChoosePrinciple: (PrincipleCategory, String) -> Unit,
    onResolveMilestone: (MilestoneId) -> Unit,
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
        state.milestoneViews.forEach { milestone ->
            val category = milestone.choices.firstOrNull()?.category
            MilestoneCard(
                milestone = milestone,
                chosen = category?.let { state.chosenPrinciples[it] },
                onChoosePrinciple = onChoosePrinciple,
                onResolveMilestone = onResolveMilestone,
            )
        }
    }
}

@Composable
private fun MilestoneCard(
    milestone: MilestoneView,
    chosen: String?,
    onChoosePrinciple: (PrincipleCategory, String) -> Unit,
    onResolveMilestone: (MilestoneId) -> Unit,
) {
    val status =
        when {
            milestone.resolved -> "Resolved"
            milestone.reached -> "Reached"
            else -> "Not reached"
        }
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
            Text(text = "${milestone.description} · $status", style = MaterialTheme.typography.titleSmall)
            chosen?.let { Text(text = "Chosen: $it", style = MaterialTheme.typography.bodyMedium) }
            milestone.choices.forEach { choice ->
                OutlinedButton(
                    onClick = { onChoosePrinciple(choice.category, choice.label) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(choice.label)
                }
            }
            if (milestone.reached && !milestone.resolved) {
                Button(
                    onClick = { onResolveMilestone(milestone.id) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Resolve")
                }
            }
        }
    }
}
