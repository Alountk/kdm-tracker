package io.kdmtracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.kdmtracker.app.UiState
import io.kdmtracker.app.ui.theme.KdmType

/**
 * The step-1 body's verbatim design copy (`pasos_1_2_pretty.html`): the codex quote and the
 * checkbox row are static Spanish the render cannot derive, so they live here where
 * `SettlementStepBodiesTest` can pin them against the design source.
 */
internal object SetUpStepCopy {
    const val CODEX_LABEL = "Registro del Códice"

    const val CODEX_QUOTE =
        "\"Tras abatir al León Blanco, los supervivientes deambulan por la oscuridad perpetua, " +
            "atraídos por un suave resplandor que brilla cálido en el horizonte.\""

    const val CHECKBOX_TITLE = "Hoja de Asentamiento inicializada"

    fun checkboxSubtitle(settlementName: String): String = "Bautizado formalmente como $settlementName"
}

/**
 * Step 1 "Preparación" body: the design's codex card, the set-up instruction card and its
 * checked-and-disabled checkbox row.
 *
 * The frame header already carries `Paso 1. Preparación` and the footer owns the advance (the old
 * `Confirm Setup` is gone), so this is body content only: meta chip, narrative, instruction. The
 * settlement name derives from [state]; nothing here is interactive and no rules live here.
 */
@Composable
fun SetUpStepScreen(state: UiState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Meta row: the design's header-row chip (the numbered box and title belong to the frame).
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Box(
                modifier =
                    Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.small)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "REQUERIDO",
                    style = KdmType.labelCaps,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }

        // Codex card — the year-1 narrative, rendered inert.
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.medium)
                    .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Symbol("menu_book", MaterialTheme.colorScheme.secondary, 24)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = SetUpStepCopy.CODEX_LABEL.uppercase(),
                    style = KdmType.labelCaps,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    text = SetUpStepCopy.CODEX_QUOTE,
                    style = KdmType.ruleQuote.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        // Instruction card: the physical set-up paragraph and the milestone checkbox.
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text =
                    buildAnnotatedString {
                        append("Sitúa el ")
                        withStyle(
                            SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold),
                        ) {
                            append("tablero de asentamiento")
                        }
                        append(
                            " en el área de juego, junto a las cuadrículas de equipo y las hojas " +
                                "de los 4 supervivientes que vencieron en el prólogo.",
                        )
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SetUpCheckboxRow(settlementName = state.settlementName)
        }
    }
}

/**
 * The design's milestone checkbox, drawn checked and inert: the settlement sheet exists the moment
 * the campaign does, so the row records that fact and is not toggleable.
 */
@Composable
private fun SetUpCheckboxRow(settlementName: String) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest, MaterialTheme.shapes.small)
                .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(20.dp)
                    .background(MaterialTheme.colorScheme.secondary, MaterialTheme.shapes.small),
            contentAlignment = Alignment.Center,
        ) {
            Symbol("check", MaterialTheme.colorScheme.onSecondary, 16)
        }
        Column {
            Text(
                text = SetUpStepCopy.CHECKBOX_TITLE,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text =
                    buildAnnotatedString {
                        val subtitle = SetUpStepCopy.checkboxSubtitle(settlementName)
                        append(subtitle)
                        addStyle(
                            SpanStyle(color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold),
                            start = subtitle.length - settlementName.length,
                            end = subtitle.length,
                        )
                    },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
