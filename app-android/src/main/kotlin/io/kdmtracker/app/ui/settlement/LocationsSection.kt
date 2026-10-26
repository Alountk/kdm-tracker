package io.kdmtracker.app.ui.settlement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kdmtracker.app.ui.settlement.common.PanelTitle
import io.kdmtracker.app.ui.settlement.common.Symbol
import io.kdmtracker.app.ui.theme.KdmPalette
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
