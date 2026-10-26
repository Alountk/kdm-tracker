package io.kdmtracker.app.ui.settlement

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kdmtracker.app.ui.settlement.common.Symbol
import io.kdmtracker.app.ui.theme.KdmActionButton
import io.kdmtracker.app.ui.theme.KdmActionVariant
import io.kdmtracker.app.ui.theme.KdmPalette

/** One craftable gear row inside a built location's catalog. */
@Composable
internal fun RecipeLine(
    card: BuiltLocationCard,
    row: RecipeRow,
    onCraft: (String, String) -> Unit,
) {
    val blocked = row.status == RecipeStatus.MISSING_INNOVATION
    val shape = MaterialTheme.shapes.small
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .alpha(if (blocked) 0.75f else 1f)
                .background(
                    if (blocked) {
                        KdmPalette.surfaceLowest.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    shape,
                ).border(
                    1.dp,
                    KdmPalette.outlineVariant.copy(alpha = if (blocked) 0.2f else 0.3f),
                    shape,
                ).padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Symbol(
                    name = if (blocked) "lock" else "handyman",
                    color =
                        if (blocked) {
                            MaterialTheme.colorScheme.outline
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    size = 15.sp,
                )
                Text(
                    text = row.gearName,
                    style = rowTitle,
                    color =
                        if (blocked) {
                            MaterialTheme.colorScheme.outline
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    textDecoration = if (blocked) TextDecoration.LineThrough else null,
                )
                if (blocked && row.missingInnovation != null) {
                    RequiresBadge(innovation = row.missingInnovation)
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = "COSTE:", style = microNormal, color = MaterialTheme.colorScheme.outline)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    row.costChips.forEach { chip ->
                        Chip(text = chip)
                    }
                }
            }
            if (row.requiresText != null) {
                Text(
                    text = "Requiere: ${row.requiresText}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        when (row.status) {
            RecipeStatus.CRAFTABLE ->
                KdmActionButton(
                    text = "FABRICAR",
                    onClick = { onCraft(card.name, row.gearName) },
                    icon = "handyman",
                    variant = KdmActionVariant.GOLD,
                )
            RecipeStatus.OUT_OF_STOCK ->
                KdmActionButton(
                    text = "FABRICAR",
                    onClick = {},
                    icon = "handyman",
                    variant = KdmActionVariant.OUT_OF_STOCK,
                )
            RecipeStatus.MISSING_INNOVATION ->
                KdmActionButton(
                    text = "BLOQUEADO",
                    onClick = {},
                    icon = "lock",
                    variant = KdmActionVariant.BLOCKED,
                )
        }
    }
}

/** The inline "Construir X (MEJORA)" offer inside the location that unlocks it. */
@Composable
internal fun UpgradeLine(
    card: BuiltLocationCard,
    upgrade: UpgradeRow,
    onBuild: (String) -> Unit,
) {
    val shape = MaterialTheme.shapes.small
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, shape)
                .border(1.dp, KdmPalette.gold.copy(alpha = 0.3f), shape)
                .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Symbol("publish", MaterialTheme.colorScheme.secondary, 15.sp)
                Text(
                    text = "Construir ${upgrade.locationName}",
                    style = rowTitle,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "(MEJORA)",
                    style = microNormal,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = "COSTE:", style = microNormal, color = MaterialTheme.colorScheme.outline)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    upgrade.costChips.forEach { chip ->
                        Chip(text = chip)
                    }
                    if (upgrade.endeavors > 0) {
                        Chip(text = "${upgrade.endeavors} Esfuerzo")
                    }
                }
            }
        }
        KdmActionButton(
            text = "ERIGIR",
            onClick = { onBuild(upgrade.locationName) },
            icon = "publish",
            variant = KdmActionVariant.LIGHT,
        )
    }
}

/** The red "REQUIERE <innovation>" chip beside a gated recipe's name. */
@Composable
private fun RequiresBadge(innovation: String) {
    Text(
        text = "REQUIERE $innovation",
        style = microNormal,
        color = MaterialTheme.colorScheme.error,
        modifier =
            Modifier
                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f), MaterialTheme.shapes.small)
                .padding(horizontal = 4.dp),
    )
}
