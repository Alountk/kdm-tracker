package io.kdmtracker.app.ui.settlement

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kdmtracker.app.ui.settlement.common.Symbol
import io.kdmtracker.app.ui.settlement.common.topHairline
import io.kdmtracker.app.ui.theme.KdmPalette
import io.kdmtracker.app.ui.theme.KdmType

/** One built location: identity, action, expandable catalog and ready upgrades. */
@Composable
internal fun BuiltCard(
    card: BuiltLocationCard,
    onCraft: (String, String) -> Unit,
    onBuild: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(card.recipes.isNotEmpty()) }
    val shape = MaterialTheme.shapes.small
    val borderColor =
        if (expanded) {
            KdmPalette.gold.copy(alpha = 0.4f)
        } else {
            KdmPalette.outlineVariant.copy(alpha = 0.3f)
        }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer, shape)
                .border(1.dp, borderColor, shape)
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = card.category, style = microBold.widest, color = MaterialTheme.colorScheme.secondary)
                    Symbol(
                        name = if (expanded) "check_circle" else "verified",
                        color =
                            if (expanded) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                        size = 14.sp,
                    )
                }
                Text(
                    text = card.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            ConstructedBadge(expanded = expanded)
        }

        card.description?.let { description ->
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (card.recipes.isNotEmpty()) {
            CatalogRow(size = card.catalogSize, expanded = expanded, onToggle = { expanded = !expanded })
        }
        if (expanded || card.upgrades.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (expanded) {
                    card.recipes.forEach { recipe ->
                        RecipeLine(card = card, row = recipe, onCraft = onCraft)
                    }
                }
                card.upgrades.forEach { upgrade ->
                    UpgradeLine(card = card, upgrade = upgrade, onBuild = onBuild)
                }
            }
        }

        if (card.actionLabel != null) {
            val cost = card.actionEndeavorCost
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .topHairline(KdmPalette.outlineVariant.copy(alpha = 0.2f))
                        .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (cost != null && cost > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Symbol("emergency", MaterialTheme.colorScheme.secondary, 16.sp)
                        Text(
                            text = "Costo: $cost Esfuerzo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** The `CATÁLOGO DE FABRICACIÓN (N CARTAS)` header bar of an expandable location catalog. */
@Composable
private fun CatalogRow(
    size: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val shape = MaterialTheme.shapes.small
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, shape)
                .clickable(onClick = onToggle)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Symbol("handyman", MaterialTheme.colorScheme.secondary, 16.sp)
            Text(
                text = "CATÁLOGO DE FABRICACIÓN ($size CARTAS)",
                style = KdmType.labelCaps,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (expanded) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(6.dp)
                            .background(MaterialTheme.colorScheme.secondary, CircleShape),
                )
                Text(
                    text = "Desplegado",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Recogido",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                Symbol("arrow_drop_down", MaterialTheme.colorScheme.outline, 18.sp)
            }
        }
    }
}

/** The `CONSTRUIDO` badge: gold-tinted with a gold outline while the card is expanded. */
@Composable
private fun ConstructedBadge(expanded: Boolean) {
    if (expanded) {
        Pill(
            text = "CONSTRUIDO",
            container = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
            content = MaterialTheme.colorScheme.secondary,
            border = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
        )
    } else {
        Pill(
            text = "CONSTRUIDO",
            container = MaterialTheme.colorScheme.surfaceContainerHighest,
            content = MaterialTheme.colorScheme.secondary,
        )
    }
}
