package io.kdmtracker.app.ui.settlement

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.kdmtracker.app.ui.theme.KdmActionButton
import io.kdmtracker.app.ui.theme.KdmActionVariant
import io.kdmtracker.app.ui.theme.KdmFontFamilies
import io.kdmtracker.app.ui.theme.KdmPalette
import io.kdmtracker.app.ui.theme.KdmSymbols
import io.kdmtracker.app.ui.theme.KdmType

/**
 * The Develop step's "Localizaciones de Asentamiento" section — the Fabricar flow of the
 * Stitch design: built cards carrying their crafting catalog, their action and their inline
 * build upgrade, plus the not-yet-built cards.
 *
 * Everything is rendered from [FabricarModel]; every tap is reported through the callbacks —
 * the section owns no rules and no state beyond the catalog's expand/collapse.
 *
 * Container, badge, wrapper and button metrics follow the mock's Tailwind classes one for one:
 * the expanded card is the only one with a gold border, its `CONSTRUIDO` badge is the only one
 * with a gold-tinted container, and every button is `px-3 py-1.5` over label-caps.
 */
@Composable
fun LocationsSection(
    model: FabricarModel,
    onBuild: (String) -> Unit,
    onCraft: (String, String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PanelTitle(
            icon = "domain",
            title = "Localizaciones de Asentamiento",
            trailing = "${model.builtCount} CONSTRUIDAS · ${model.pendingCount} PENDIENTES",
        )
        model.built.forEach { card ->
            BuiltCard(card = card, onCraft = onCraft, onBuild = onBuild)
        }
        model.buildable.forEach { card ->
            BuildableCard(card = card, onBuild = onBuild)
        }
        // The design's special-showdown warning: a boxed icon beside three lines, in its own card.
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.small)
                    .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier =
                    Modifier
                        .background(KdmPalette.onCrimsonContainer, MaterialTheme.shapes.small)
                        .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Symbol("warning", KdmPalette.onCrimson, 20.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Regla de Año Linterna 1",
                    style = KdmType.labelCaps,
                    color = KdmPalette.blood,
                    letterSpacing = 0.1f.sp,
                )
                Text(
                    text = "Sáltate el Paso 9",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text =
                        "\"Sáltate el paso 9 en la primera fase de asentamiento. " +
                            "Los supervivientes aún no conocen las némesis.\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** One built location: identity, action, expandable catalog and ready upgrades. */
@Composable
private fun BuiltCard(
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

/** One craftable gear row inside a built location's catalog. */
@Composable
private fun RecipeLine(
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
private fun UpgradeLine(
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

/** One "Por erigir" card for a location that is not built and has no ready upgrade. */
@Composable
private fun BuildableCard(
    card: BuildableLocationCard,
    onBuild: (String) -> Unit,
) {
    val shape = MaterialTheme.shapes.small
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .alpha(0.9f)
                .background(MaterialTheme.colorScheme.surfaceContainer, shape)
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "POR ERIGIR",
                    style = microBold.widest,
                    color = MaterialTheme.colorScheme.outline,
                )
                Text(
                    text = card.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Pill(
                text = "NO CONSTRUIDO",
                container = MaterialTheme.colorScheme.surfaceContainerLow,
                content = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        card.description?.let { description ->
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (card.costChips.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                card.costChips.forEach { chip ->
                    Chip(text = chip)
                }
            }
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .topHairline(KdmPalette.outlineVariant.copy(alpha = 0.2f))
                    .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                if (card.requires.isNotEmpty()) {
                    Text(
                        text = "Requiere: ${card.requires.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (card.endeavors > 0) {
                    Text(
                        text = "${card.endeavors} Esfuerzo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            KdmActionButton(
                text = "CONSTRUIR",
                onClick = { onBuild(card.name) },
                icon = "handyman",
                variant = KdmActionVariant.LIGHT,
            )
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

/** A small status pill: label-caps on a tinted container with an optional hairline. */
@Composable
private fun Pill(
    text: String,
    container: Color,
    content: Color,
    border: Color? = null,
) {
    val shape = MaterialTheme.shapes.small
    val modifier =
        if (border != null) {
            Modifier.background(container, shape).border(1.dp, border, shape)
        } else {
            Modifier.background(container, shape)
        }
    Text(
        text = text,
        style = KdmType.labelCaps,
        color = content,
        modifier = modifier.padding(horizontal = 8.dp, vertical = 2.dp),
    )
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

/** A small cost chip: "N Nombre". */
@Composable
private fun Chip(text: String) {
    Text(
        text = text,
        style = microBold,
        color = MaterialTheme.colorScheme.primary,
        modifier =
            Modifier
                .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.small)
                .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** Section title row: glyph + uppercase title, trailing value on the right. */
@Composable
private fun PanelTitle(
    icon: String,
    title: String,
    trailing: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Symbol(icon, MaterialTheme.colorScheme.secondary, 18.sp)
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = trailing.uppercase(),
            style = KdmType.labelCaps,
            color = MaterialTheme.colorScheme.secondary,
            modifier =
                Modifier
                    .background(MaterialTheme.colorScheme.onSecondaryContainer, MaterialTheme.shapes.small)
                    .border(1.dp, MaterialTheme.colorScheme.secondary, MaterialTheme.shapes.small)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

/** A Material Symbol glyph rendered from the bundled subset font. */
@Composable
private fun Symbol(
    name: String,
    color: Color,
    size: TextUnit,
) {
    Text(
        text = KdmSymbols.glyph(name),
        // Google's hosted Material Symbols CSS pins `line-height: 1`; without it the symbol
        // font's own ~1.7em line box pads every row that carries an icon.
        style = TextStyle(fontFamily = KdmSymbols.family, fontSize = size, lineHeight = size),
        color = color,
    )
}

/** The mock's row title: `font-headline-sm text-[13px] font-bold` — Playfair, not Inter. */
private val rowTitle =
    TextStyle(
        fontFamily = KdmFontFamilies.playfair,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Bold,
    )

/** The mock's micro type: 10px Inter, plain and bold, without label-caps' tracking. */
private val microNormal =
    TextStyle(fontFamily = KdmFontFamilies.inter, fontSize = 10.sp, lineHeight = 14.sp)

private val microBold =
    TextStyle(
        fontFamily = KdmFontFamilies.inter,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Bold,
    )

/** `tracking-widest`: label-caps' 0.08em widened to the mock's 0.1em. */
private val TextStyle.widest: TextStyle
    get() = copy(letterSpacing = 0.1f.em)

/** A 1dp hairline across the top of the row — the mock's `border-t`. */
private fun Modifier.topHairline(color: Color): Modifier =
    drawBehind {
        drawLine(
            color = color,
            start = Offset(0f, 0.5f),
            end = Offset(size.width, 0.5f),
            strokeWidth = 1.dp.toPx(),
        )
    }
