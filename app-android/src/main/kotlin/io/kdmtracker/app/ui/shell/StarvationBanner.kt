package io.kdmtracker.app.ui.shell

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kdmtracker.app.ui.components.SheetPanel
import io.kdmtracker.app.ui.theme.KdmActionButton
import io.kdmtracker.app.ui.theme.KdmActionVariant
import io.kdmtracker.core.domain.ResourceDefinition
import io.kdmtracker.core.domain.ResourceId
import io.kdmtracker.core.domain.StorageLine

/** One stored resource the player can discard to pay the starvation debt. */
internal data class StarvationChoice(
    val id: ResourceId,
    val label: String,
)

/** The stored resources (quantity above zero) as choices, named from the catalog by id. */
internal fun starvationChoices(
    lines: List<StorageLine>,
    definitions: List<ResourceDefinition>,
): List<StarvationChoice> =
    lines
        .filter { it.quantity > 0 }
        .map { line ->
            val name = definitions.firstOrNull { it.id == line.id }?.names?.es ?: line.id.value
            StarvationChoice(line.id, "$name ×${line.quantity}")
        }

/**
 * The hunt starved the settlement: [owed] stored resources must go, and the player picks which, one
 * unit per tap. Shown on every shell screen until the debt is paid.
 */
@Composable
internal fun StarvationBanner(
    owed: Int,
    choices: List<StarvationChoice>,
    onDiscard: (ResourceId) -> Unit,
) {
    SheetPanel(title = "Hambruna", icon = "no_meals") {
        Text(
            text = "Descarta $owed recurso(s) del almacén.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            choices.forEach { choice ->
                KdmActionButton(
                    text = choice.label,
                    onClick = { onDiscard(choice.id) },
                    variant = KdmActionVariant.DARK,
                )
            }
        }
    }
}
