package io.kdmtracker.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.kdmtracker.app.UiState
import io.kdmtracker.core.rules.SettlementStep

/** Standard Material disabled alpha, so an unreachable step reads as non-interactive. */
private const val DISABLED_ALPHA = 0.38f

/**
 * Stateless rendering of the Settlement Phase for one [UiState] frame.
 *
 * Shows the lantern year, population and endeavors, a rail of the official settlement steps with
 * [UiState.currentStep] highlighted, a body area for the current step and the back/next controls.
 * The body is a "Pendiente" placeholder: the real Set Up / Develop screens arrive in later plans.
 * All navigation is reported through the callbacks; this composable owns no state and no rules.
 */
@Composable
fun SettlementScreen(
    state: UiState,
    onAdvance: () -> Unit,
    onPrevious: () -> Unit,
    onSelectStep: (Int) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettlementHeader(state)

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SettlementStepRail(
                steps = state.steps,
                currentStep = state.currentStep,
                reachableSteps = state.reachableSteps,
                onSelectStep = onSelectStep,
                modifier =
                    Modifier
                        .width(180.dp)
                        .verticalScroll(rememberScrollState()),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Step ${state.currentStep} · ${state.stepTitle}",
                    style = MaterialTheme.typography.titleLarge,
                )
                PendientePlaceholder(modifier = Modifier.fillMaxWidth())
                state.lastTransition?.let { transition ->
                    Text(
                        text = transition,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onPrevious, modifier = Modifier.weight(1f)) {
                Text("Previous")
            }
            Button(onClick = onAdvance, modifier = Modifier.weight(1f)) {
                Text("Next")
            }
        }
    }
}

@Composable
private fun SettlementHeader(state: UiState) {
    Column {
        Text(text = state.settlementName, style = MaterialTheme.typography.headlineMedium)
        Text(text = "Settlement", style = MaterialTheme.typography.titleMedium)
        Text(
            text =
                "Lantern Year ${state.lanternYear} · Population ${state.population} · " +
                    "Endeavors ${state.endeavors}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettlementStepRail(
    steps: List<SettlementStep>,
    currentStep: Int,
    reachableSteps: Set<Int>,
    onSelectStep: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        steps.forEach { step ->
            SettlementStepRailItem(
                step = step,
                selected = step.number == currentStep,
                enabled = step.number in reachableSteps,
                onClick = { onSelectStep(step.number) },
            )
        }
    }
}

@Composable
private fun SettlementStepRailItem(
    step: SettlementStep,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val containerColor =
        if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        }
    val contentColor =
        if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .selectable(
                    selected = selected,
                    enabled = enabled,
                    role = Role.Tab,
                    onClick = onClick,
                ),
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "${step.number}",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.width(20.dp),
            )
            Text(
                text = step.title,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
            )
        }
    }
}
