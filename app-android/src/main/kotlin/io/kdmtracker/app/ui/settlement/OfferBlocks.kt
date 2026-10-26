package io.kdmtracker.app.ui.settlement

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import io.kdmtracker.app.ui.settlement.common.Symbol
import io.kdmtracker.app.ui.settlement.common.microLabel
import io.kdmtracker.app.ui.theme.InnovarPalette
import io.kdmtracker.app.ui.theme.KdmType

/** The offer's header: what it costs today and the rule behind it. */
@Composable
internal fun OfferHeader(model: InnovationsModel) {
    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Symbol("style", MaterialTheme.colorScheme.secondary, 16.sp)
                Text(
                    text = model.offerTitle.uppercase(),
                    style = KdmType.labelCaps,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = model.ruleLine,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * State 5, exactly as the board draws it: a red panel with a `FALTAN RECURSOS` pill, four resource
 * boxes each showing `have / need` and `FALTA`/`OK`, and the `Motivo:` sentence underneath.
 */
@Composable
internal fun CostBlock(cost: CostView) {
    val shape = MaterialTheme.shapes.small
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(InnovarPalette.redBg, shape)
                .border(1.dp, InnovarPalette.red.copy(alpha = 0.5f), shape)
                .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Symbol("error", InnovarPalette.red, 16.sp)
            Text(
                text = "Coste de Innovación Insuficiente",
                style = MaterialTheme.typography.bodySmall,
                color = InnovarPalette.red,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "Faltan Recursos",
                style = microLabel,
                color = InnovarPalette.red,
                modifier =
                    Modifier
                        .background(InnovarPalette.redBg, shape)
                        .border(1.dp, InnovarPalette.red.copy(alpha = 0.4f), shape)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            (listOf(cost.endeavors) + cost.resources).forEach { need ->
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceContainer, shape)
                            .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = need.name,
                        style = microLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${need.have} / ${need.need}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (need.ok) "OK" else "FALTA",
                        style = microLabel,
                        color = if (need.ok) InnovarPalette.green else InnovarPalette.red,
                    )
                }
            }
        }

        Text(
            text = "Motivo: ${cost.reason}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
