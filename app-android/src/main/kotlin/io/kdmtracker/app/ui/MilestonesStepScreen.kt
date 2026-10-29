package io.kdmtracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.kdmtracker.app.MilestoneView
import io.kdmtracker.app.UiState
import io.kdmtracker.app.ui.theme.KdmType
import io.kdmtracker.core.domain.MilestoneId
import io.kdmtracker.core.domain.PrincipleCategory

/** The step-6 body's static design copy, kept where `SettlementStepBodiesTest` can pin it. */
internal object MilestonesStepCopy {
    const val CODEX_QUOTE =
        "\"Después de derrotar al León Blanco, los supervivientes deambulan por la oscuridad, " +
            "atraídos por un suave resplandor que brilla cálido en el horizonte.\""

    const val CODEX_LABEL = "Códice de Supervivencia • Prólogo"
}

/**
 * Step 6 "Comprobar Hitos" body: the design's milestone cards and closing codex quote.
 *
 * Each card derives its chip from state ([milestoneStatus]) and its title, glyph and flavor
 * paragraph from the milestone id ([milestoneTitle] / [milestoneGlyph] / [milestoneFlavor] — only
 * the two milestones the design names carry its Spanish titles and copy; the rest keep the core
 * description) and keeps the interactive principle choice + Resolve for reached, unresolved
 * milestones (the design mock only shows the unreached state, but these actions stay reachable).
 * The design's own condition chips (`Pendiente (0 Bajas)`, `Población ≥ 15`) have no model and are
 * replaced by the derived status — a recorded derivation. The frame owns the step title.
 */
@Composable
fun MilestonesStepScreen(
    state: UiState,
    onChoosePrinciple: (PrincipleCategory, String) -> Unit,
    onResolveMilestone: (MilestoneId) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Meta row: the design's header-row active-count chip.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Box(
                modifier =
                    Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest, MaterialTheme.shapes.small)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "${activeMilestoneCount(state.milestoneViews)} Activos".uppercase(),
                    style = KdmType.labelCaps,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        state.milestoneViews.forEach { milestone ->
            MilestoneCard(
                milestone = milestone,
                onChoosePrinciple = onChoosePrinciple,
                onResolveMilestone = onResolveMilestone,
            )
        }

        // Closing codex quote — the design's narrative block between the last section and the CTA.
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest, MaterialTheme.shapes.medium)
                    .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = MilestonesStepCopy.CODEX_QUOTE,
                style = KdmType.ruleQuote.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                text = MilestonesStepCopy.CODEX_LABEL.uppercase(),
                style = KdmType.labelCaps,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** One milestone card: derived chip, optional flavor paragraph and the reachable actions. */
@Composable
private fun MilestoneCard(
    milestone: MilestoneView,
    onChoosePrinciple: (PrincipleCategory, String) -> Unit,
    onResolveMilestone: (MilestoneId) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest, MaterialTheme.shapes.medium)
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Symbol(milestoneGlyph(milestone.id), MaterialTheme.colorScheme.onSurfaceVariant, 18)
                Text(
                    text = milestoneTitle(milestone.id, milestone.description),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Box(
                modifier =
                    Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.small)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    text = milestoneStatus(milestone.reached, milestone.resolved).chip.uppercase(),
                    style = KdmType.labelCaps,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        milestoneFlavor(milestone.id)?.let { flavor ->
            Text(
                text = flavor,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (milestone.reached && !milestone.resolved) {
            milestone.choices.forEach { choice ->
                OutlinedButton(
                    onClick = { onChoosePrinciple(choice.category, choice.label) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(choice.label)
                }
            }
            Button(
                onClick = { onResolveMilestone(milestone.id) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Resolve")
            }
        }
    }
}
