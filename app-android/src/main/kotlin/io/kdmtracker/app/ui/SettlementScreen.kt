package io.kdmtracker.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kdmtracker.app.UiState
import io.kdmtracker.app.ui.settlement.FabricarModel
import io.kdmtracker.app.ui.settlement.InnovationsModel
import io.kdmtracker.app.ui.settlement.InnovationsPanel
import io.kdmtracker.app.ui.settlement.LocationsSection
import io.kdmtracker.app.ui.settlement.SettlementSummary
import io.kdmtracker.app.ui.settlement.SettlementSummaryModel
import io.kdmtracker.app.ui.settlement.StepFrameFooter
import io.kdmtracker.app.ui.settlement.StepFrameHeader
import io.kdmtracker.app.ui.settlement.StepFrameModel
import io.kdmtracker.core.domain.MilestoneId
import io.kdmtracker.core.domain.PrincipleCategory
import io.kdmtracker.core.domain.ResourceEntry
import io.kdmtracker.core.domain.ResourceId
import io.kdmtracker.core.domain.Survivor
import io.kdmtracker.core.domain.Ulid

/**
 * Stateless rendering of the Settlement Phase for one [UiState] frame.
 *
 * Step 1 (Set Up) hands the whole frame to [SetUpStepScreen]: it is read-only content with its own
 * confirm action. Every other step shows the cockpit — a header, a **thin top step bar** (the 11
 * steps as numbered chips, the current one highlighted and unreachable ones disabled, so the
 * navigation never eats half the width), a full-width body and the back/next controls. Step 7
 * (Develop) renders its own panels inline. All navigation is reported through the
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
        onAdjust = { id, delta -> onAddResource(ResourceEntry(id, delta)) },
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
            7 -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    InnovationsPanel(model = InnovationsModel.of(state = state), onConfirm = onInnovate)
                    LocationsSection(
                        model = FabricarModel.of(state),
                        onBuild = onBuild,
                        onCraft = onCraft,
                        onSpendEndeavor = onSpendEndeavor,
                    )
                }
            }
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
    onAdjust: (ResourceId, Int) -> Unit = { _, _ -> },
    body: @Composable () -> Unit,
) {
    val frame = state.steps.firstOrNull { it.number == state.currentStep }?.let { StepFrameModel.of(it, state.steps) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SettlementSummary(
                model =
                    SettlementSummaryModel.of(
                        survivalLimit = state.survivalLimit,
                        endeavors = state.endeavors,
                        maxEndeavors = state.endeavorBreakdown.total,
                        resources =
                            SettlementSummaryModel.basicResources(
                                // The design's grid is exactly these four, in this order: the catalog
                                // carries more basic resources (and, from the legacy pack, English
                                // duplicates of them), which would otherwise flood the row.
                                definitions =
                                    SettlementSummaryModel.DESIGN_ORDER.mapNotNull { name ->
                                        state.resourceDefinitions.firstOrNull { it.names.es == name }
                                    },
                                storage = state.storageLines,
                            ),
                    ),
                year = state.lanternYear,
                phaseLabel = "Fase Activa",
                onAdjust = onAdjust,
            )

            frame?.let { StepFrameHeader(model = it) }

            body()

            state.lastTransition?.let { transition ->
                Text(
                    text = transition,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // The design puts the actions at the END of the step's content, not in a pinned bar.
            frame?.let {
                StepFrameFooter(
                    model = it,
                    endeavors = state.endeavors,
                    onAdvance = onAdvance,
                    onRestore = {},
                )
            }
        }
    }
}
