package io.kdmtracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import io.kdmtracker.app.ui.theme.KdmSymbols
import io.kdmtracker.core.domain.MilestoneId
import io.kdmtracker.core.domain.PrincipleCategory
import io.kdmtracker.core.domain.ResourceEntry
import io.kdmtracker.core.domain.ResourceId
import io.kdmtracker.core.domain.Ulid

/**
 * Stateless rendering of the Settlement Phase for one [UiState] frame.
 *
 * Every step shows the cockpit — a header, a **thin top step bar** (the 11 steps as numbered chips,
 * the current one highlighted and unreachable ones disabled, so the navigation never eats half the
 * width), a full-width body and the back/next controls. Step 1 renders its own body like every
 * other step (the frame's advance replaces its old `Confirm Setup`), and step 7 (Develop) renders
 * its panels inline. All navigation is reported through the callbacks; this composable owns no
 * state and no rules.
 */
@Composable
fun SettlementScreen(
    state: UiState,
    onAdvance: () -> Unit,
    onPrevious: () -> Unit,
    onSelectStep: (Int) -> Unit,
    onInnovate: (String) -> Unit = {},
    onBuild: (String) -> Unit = {},
    onAddResource: (ResourceEntry) -> Unit = {},
    onCraft: (String, String) -> Unit = { _, _ -> },
    onRecordDeaths: (Int) -> Unit = {},
    onChoosePrinciple: (PrincipleCategory, String) -> Unit = { _, _ -> },
    onResolveMilestone: (MilestoneId) -> Unit = {},
    onHealSurvivors: () -> Unit = {},
    onDepartingChange: (List<Ulid>) -> Unit = {},
    onAssignGear: (Ulid, String) -> Unit = { _, _ -> },
    onUnassignGear: (Ulid, String) -> Unit = { _, _ -> },
) {
    // The summary's and step 10's resource adjust share one entry into the ledger.
    val adjust: (ResourceId, Int) -> Unit = { id, delta -> onAddResource(ResourceEntry(id, delta)) }
    SettlementCockpit(
        state = state,
        onAdvance = onAdvance,
        onPrevious = onPrevious,
        onSelectStep = onSelectStep,
        onAdjust = adjust,
    ) {
        when (state.currentStep) {
            1 -> SetUpStepScreen(state = state)
            2 ->
                SurvivorsReturnStepScreen(
                    state = state,
                    onHeal = onHealSurvivors,
                )
            3 -> GainEndeavorsStepScreen(state = state)
            4 -> TimelineStepScreen(state = state)
            5 ->
                DeathCountStepScreen(
                    state = state,
                    onRecordDeaths = onRecordDeaths,
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
                    )
                }
            }
            8 ->
                PrepareDepartingStepScreen(
                    state = state,
                    onDepartingChange = onDepartingChange,
                    onAssignGear = onAssignGear,
                    onUnassignGear = onUnassignGear,
                )
            9 -> SpecialShowdownStepScreen(state = state)
            10 -> ArchiveStepScreen(state = state, onAdjust = adjust)
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

            state.actionError?.let { reason ->
                // Every controller refusal — innovar, fabricar, construir, ajustar recursos — is
                // published here. Without this block they disappear the moment they happen.
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.errorContainer, MaterialTheme.shapes.small)
                            .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = KdmSymbols.glyph("warning"),
                        fontFamily = KdmSymbols.family,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

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
