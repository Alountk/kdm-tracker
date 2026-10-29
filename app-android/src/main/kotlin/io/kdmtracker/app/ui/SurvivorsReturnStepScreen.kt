package io.kdmtracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kdmtracker.app.UiState
import io.kdmtracker.app.ui.theme.KdmType
import io.kdmtracker.core.domain.Survivor
import io.kdmtracker.core.domain.SurvivorStatus

/**
 * Step 2 "Los Supervivientes Regresan" body: the design's two action modules and the stack of
 * returning-survivor cards.
 *
 * `Confirmar Curación de Heridas` is the modeled heal ([io.kdmtracker.core.usecase.ReturnSurvivors]
 * via the frame's controller) and replaces the English `Heal & clear tokens`. Everything the design
 * draws without a rule behind it is inert: the `+1 Supervivencia` bonus button and the heart
 * wound-tag button are drawn disabled (backlog), and the status line is derived from the wound
 * boxes by [survivorStatusLine]. Survivor editing stays in the Supervivientes tab, and the cockpit
 * already surfaces `state.actionError`, so neither appears here.
 */
@Composable
fun SurvivorsReturnStepScreen(
    state: UiState,
    onHeal: () -> Unit,
) {
    val living = state.survivors.filter { it.status != SurvivorStatus.DEAD }
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
                    text = "EN CURSO",
                    style = KdmType.labelCaps,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        ReturnBonusModule(survivalLimit = state.survivalLimit)
        HealingModule(onHeal = onHeal)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Supervivientes Vencedores (${living.size})".uppercase(),
                style = KdmType.labelCaps,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Listos para el Asentamiento",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        living.forEach { survivor ->
            ReturningSurvivorCard(survivor = survivor, survivalLimit = state.survivalLimit)
        }
    }
}

/** The design's `Bono de Regreso` module; the `+1` bonus itself is not modeled, so its button is inert. */
@Composable
private fun ReturnBonusModule(survivalLimit: Int) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Symbol("add_circle", MaterialTheme.colorScheme.secondary, 18)
                    Text(
                        text = "Bono de Regreso".uppercase(),
                        style = KdmType.labelCaps,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                Text(
                    text =
                        buildAnnotatedString {
                            append("Cada superviviente que regresa gana ")
                            withStyle(
                                SpanStyle(color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold),
                            ) {
                                append("+1 Supervivencia")
                            }
                            append(".")
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Column(
                modifier =
                    Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.small)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Límite Actual".uppercase(),
                    style = KdmType.labelCaps.copy(fontSize = 9.sp, lineHeight = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = survivalLimit.toString(),
                    style = KdmType.statDisplay.copy(fontSize = 18.sp, lineHeight = 22.sp),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Text(
            text =
                "Aviso reglamentario: No se puede superar el Límite de Supervivencia actual de la " +
                    "hoja de asentamiento ($survivalLimit).",
            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ModuleActionButton(
            text = "Aplicar +1 Supervivencia a los que Regresan",
            icon = "shield",
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            enabled = false,
            onClick = {},
        )
    }
}

/** The design's `Curación Automática` module; its confirm is the modeled heal. */
@Composable
private fun HealingModule(onHeal: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Symbol("healing", MaterialTheme.colorScheme.primary, 18)
            Text(
                text = "Curación Automática".uppercase(),
                style = KdmType.labelCaps,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text =
                "Todas las heridas leves y graves se curan de forma inmediata. Se borran todas " +
                    "las marcas en su registro de impacto.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                        MaterialTheme.shapes.small,
                    ).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Symbol("warning", MaterialTheme.colorScheme.error, 20)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Regla Inflexible".uppercase(),
                    style = KdmType.labelCaps,
                    color = MaterialTheme.colorScheme.error,
                )
                Text(
                    text =
                        buildAnnotatedString {
                            append("Las ")
                            withStyle(
                                SpanStyle(fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline),
                            ) {
                                append("lesiones permanentes NO se curan")
                            }
                            append(" jamás en este paso. Se mantienen registradas.")
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
        ModuleActionButton(
            text = "Confirmar Curación de Heridas",
            icon = "cleaning_services",
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.primary,
            enabled = true,
            onClick = onHeal,
        )
    }
}

/**
 * The design's full-width module action (`py-3` + label-caps + leading glyph). [enabled] is false
 * only for the controls the design draws but the rules do not model yet; the disabled colors follow
 * the frame footer's inert-button treatment.
 */
@Composable
private fun ModuleActionButton(
    text: String,
    icon: String,
    containerColor: Color,
    contentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
    ) {
        Symbol(icon, if (enabled) contentColor else MaterialTheme.colorScheme.onSurfaceVariant, 18)
        Text(
            text = text.uppercase(),
            style = KdmType.labelCaps,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * One returning survivor's design card: initial avatar, name, the wound-derived status line
 * ([survivorStatusLine], with the design's `check_circle` on the injury-free line) and the survival
 * stat. The heart wound-tag button is drawn disabled — no wound-tag model exists yet.
 */
@Composable
private fun ReturningSurvivorCard(
    survivor: Survivor,
    survivalLimit: Int,
) {
    val name = survivor.name.ifBlank { "Sin nombre" }
    val injured = survivor.wounds.values.any { it.light || it.heavy }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.medium)
                .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = name.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (!injured) {
                        Symbol("check_circle", MaterialTheme.colorScheme.secondary, 13)
                    }
                    Text(
                        text = survivorStatusLine(survivor.wounds),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Supervivencia".uppercase(),
                    style = KdmType.labelCaps.copy(fontSize = 9.sp, lineHeight = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = survivor.survival.toString(),
                        style = KdmType.statDisplay,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                        text = "/$survivalLimit",
                        style = TextStyle(fontSize = 11.sp, lineHeight = 14.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Box(
                modifier =
                    Modifier
                        .size(28.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center,
            ) {
                Symbol("favorite", MaterialTheme.colorScheme.secondary, 16)
            }
        }
    }
}
