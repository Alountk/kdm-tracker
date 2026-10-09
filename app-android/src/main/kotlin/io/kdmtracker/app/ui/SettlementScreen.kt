package io.kdmtracker.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.kdmtracker.app.UiState
import io.kdmtracker.core.domain.MilestoneId
import io.kdmtracker.core.domain.PrincipleCategory
import io.kdmtracker.core.domain.ResourceEntry
import io.kdmtracker.core.domain.Survivor
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.rules.SettlementStep

/** Standard Material disabled alpha, so an unreachable step reads as non-interactive. */
private const val DISABLED_ALPHA = 0.38f

/**
 * Stateless rendering of the Settlement Phase for one [UiState] frame.
 *
 * Step 1 (Set Up) hands the whole frame to [SetUpStepScreen]: it is read-only content with its own
 * confirm action. Every other step shows the cockpit — a header, a **thin top step bar** (the 11
 * steps as numbered chips, the current one highlighted and unreachable ones disabled, so the
 * navigation never eats half the width), a full-width body and the back/next controls. Step 7
 * (Develop) supplies [DevelopStepScreen] as that body. All navigation is reported through the
 * callbacks; this composable owns no state and no rules.
 */
@Composable
fun SettlementScreen(
    state: UiState,
    onAdvance: () -> Unit,
    onPrevious: () -> Unit,
    onSelectStep: (Int) -> Unit,
    onInnovate: (String) -> Unit = {},
    onBuild: (String) -> Unit = {},
    onSpendEndeavor: () -> Unit = {},
    onAddResource: (ResourceEntry) -> Unit = {},
    onCraft: (String, String) -> Unit = { _, _ -> },
    onRecordEvent: (String) -> Unit = {},
    onDrawEvent: () -> Unit = {},
    onRecordDeaths: (Int) -> Unit = {},
    onRecordBirths: (Int) -> Unit = {},
    onChoosePrinciple: (PrincipleCategory, String) -> Unit = { _, _ -> },
    onResolveMilestone: (MilestoneId) -> Unit = {},
    onHealSurvivors: () -> Unit = {},
    onDepartingChange: (List<Ulid>) -> Unit = {},
    onAssignGear: (Ulid, String) -> Unit = { _, _ -> },
    onUnassignGear: (Ulid, String) -> Unit = { _, _ -> },
    onSaveSurvivor: (Survivor) -> Unit = {},
    onDeclareSpecialShowdown: (Boolean) -> Unit = {},
    onSetUpShowdown: (String, Int, List<String>) -> Unit = { _, _, _ -> },
    onDrawShowdownCard: () -> Unit = {},
    onWoundMonster: () -> Unit = {},
    onGrantHuntXp: (List<Ulid>) -> Unit = {},
) {
    if (state.currentStep == 1) {
        SetUpStepScreen(state = state, onConfirm = onAdvance)
        return
    }
    SettlementCockpit(
        state = state,
        onAdvance = onAdvance,
        onPrevious = onPrevious,
        onSelectStep = onSelectStep,
    ) {
        when (state.currentStep) {
            2 ->
                SurvivorsReturnStepScreen(
                    state = state,
                    onHeal = onHealSurvivors,
                    onSaveSurvivor = onSaveSurvivor,
                    onAssignGear = onAssignGear,
                    onUnassignGear = onUnassignGear,
                )
            4 -> TimelineStepScreen(state = state, onRecordEvent = onRecordEvent, onDrawEvent = onDrawEvent)
            5 ->
                DeathCountStepScreen(
                    state = state,
                    onRecordDeaths = onRecordDeaths,
                    onRecordBirths = onRecordBirths,
                    onChoosePrinciple = onChoosePrinciple,
                )
            6 ->
                MilestonesStepScreen(
                    state = state,
                    onChoosePrinciple = onChoosePrinciple,
                    onResolveMilestone = onResolveMilestone,
                )
            7 ->
                DevelopStepScreen(
                    state = state,
                    onInnovate = onInnovate,
                    onBuild = onBuild,
                    onSpendEndeavor = onSpendEndeavor,
                    onAddResource = onAddResource,
                    onCraft = onCraft,
                )
            8 ->
                PrepareDepartingStepScreen(
                    state = state,
                    onDepartingChange = onDepartingChange,
                    onAssignGear = onAssignGear,
                    onUnassignGear = onUnassignGear,
                    onSaveSurvivor = onSaveSurvivor,
                )
            9 ->
                SpecialShowdownStepScreen(
                    state = state,
                    onDeclareSpecialShowdown = onDeclareSpecialShowdown,
                    onSetUpShowdown = onSetUpShowdown,
                    onDrawShowdownCard = onDrawShowdownCard,
                    onWoundMonster = onWoundMonster,
                    onGrantHuntXp = onGrantHuntXp,
                    onRecordDeaths = onRecordDeaths,
                    onConclude = onAdvance,
                )
            10 -> ArchiveStepScreen(state = state)
            11 -> EndPhaseStepScreen(state = state)
            else -> PendientePlaceholder(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SettlementCockpit(
    state: UiState,
    onAdvance: () -> Unit,
    onPrevious: () -> Unit,
    onSelectStep: (Int) -> Unit,
    body: @Composable () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SettlementHeader(state)

        SettlementStepBar(
            steps = state.steps,
            currentStep = state.currentStep,
            reachableSteps = state.reachableSteps,
            onSelectStep = onSelectStep,
        )

        Text(
            text = "Step ${state.currentStep} · ${state.stepTitle}",
            style = MaterialTheme.typography.titleMedium,
        )

        Box(modifier = Modifier.weight(1f)) { body() }

        state.lastTransition?.let { transition ->
            Text(
                text = transition,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
    // The settlement name lives in the AppTopBar; only the derived stats are rendered here.
    Text(
        text =
            "Lantern Year ${state.lanternYear} · Population ${state.population} · " +
                "Endeavors ${state.endeavors} · Survival Limit ${state.survivalLimit}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SettlementStepBar(
    steps: List<SettlementStep>,
    currentStep: Int,
    reachableSteps: Set<Int>,
    onSelectStep: (Int) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        steps.forEach { step ->
            SettlementStepItem(
                step = step,
                selected = step.number == currentStep,
                enabled = step.number in reachableSteps,
                onClick = { onSelectStep(step.number) },
            )
        }
    }
}

@Composable
private fun SettlementStepItem(
    step: SettlementStep,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val containerColor =
        if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
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
        Text(
            text = "${step.number}",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}
