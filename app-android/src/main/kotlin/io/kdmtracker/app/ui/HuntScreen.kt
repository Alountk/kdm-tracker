package io.kdmtracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kdmtracker.app.UiState
import io.kdmtracker.app.ui.theme.KdmPalette
import io.kdmtracker.core.domain.HuntSpace
import io.kdmtracker.core.domain.HuntSpaceKind
import io.kdmtracker.core.domain.HuntState
import io.kdmtracker.core.domain.HuntStatus
import io.kdmtracker.core.domain.Survivor
import io.kdmtracker.core.domain.SurvivorStatus
import io.kdmtracker.core.domain.Ulid

private val HuntInk = Color(0xFFE9C349)
private val HuntRed = Color(0xFFB9332E)
private val HuntDeepRed = Color(0xFF8E1214)
private val HuntBg = Color(0xFF131315)

/**
 * The Hunting Phase body, rebuilt 1:1 from the Stitch design's seven regions over [state.hunt].
 *
 * Before setup it offers a quarry/level/players form; afterwards it renders, in order: the quarry
 * plinth, the active-instinct rule, the expedition chips, the 13-space board (header
 * `Casillas 0 - 12`), the active hunt-event card, and the three action buttons. No state and no
 * rules live here: everything is reported through the callbacks.
 */
@Composable
fun HuntScreen(
    state: UiState,
    onSetUpHunt: (String, Int, Int) -> Unit,
    onAssignPlayer: (Ulid, Int) -> Unit,
    onMarkSpaceKind: (Int, HuntSpaceKind) -> Unit,
    onAssignHuntEvent: (Int, String) -> Unit,
    onResolveHuntEvent: (Int, String) -> Unit,
    onAdvanceHunt: () -> Unit,
    onMoveMonster: () -> Unit,
    onFinishHunt: (HuntStatus) -> Unit,
) {
    val hunt = state.hunt
    if (hunt == null || hunt.spaces.isEmpty()) {
        HuntSetup(state = state, onSetUpHunt = onSetUpHunt)
        return
    }

    var selected by remember(hunt.position) { mutableStateOf(hunt.position) }
    val party = state.survivors.filter { it.status == SurvivorStatus.DEPARTING }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(HuntBg)
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        QuarryPlinth(state = state, level = hunt.level)
        ExpeditionChips(party = party, onAssignPlayer = onAssignPlayer, players = hunt.players)
        HuntBoard(hunt = hunt, selectedIndex = selected, onSelectIndex = { selected = it })
        HuntEventCard(
            space = hunt.spaces.getOrElse(selected) { HuntSpace() },
            index = selected,
            huntEvents = state.huntEvents,
            onAssign = { onAssignHuntEvent(selected, it) },
            onResolve = { onResolveHuntEvent(selected, it) },
            onMarkKind = { onMarkSpaceKind(selected, it) },
        )
        ActionButtons(
            onAdvance = onAdvanceHunt,
            onMoveMonster = onMoveMonster,
            onShowdown = { onFinishHunt(HuntStatus.QUARRY_FOUND) },
        )
    }
}

/** Region 2 — the quarry plinth: `Presa Activa`, level, quarry, variant, distance and stat matrix. */
@Composable
private fun QuarryPlinth(
    state: UiState,
    level: Int,
) {
    // The design renders this whole block in one card (`surface-container-low`), with the
    // `Distancia` badge pinned to the top-right of the header row.
    HuntPanel {
        val hunt = state.hunt ?: HuntState()
        val frame = HuntPlinthFrame.from(hunt.copy(level = level))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(KdmPalette.surfaceHighest),
                contentAlignment = Alignment.Center,
            ) {
                Symbol("pets", KdmPalette.gold, 26)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(text = frame.eyebrow, color = KdmPalette.gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Box(
                        modifier =
                            Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(KdmPalette.gold),
                    )
                    Text(text = frame.levelLabel, color = KdmPalette.ash, fontSize = 14.sp)
                }
                Text(text = frame.quarry, color = KdmPalette.onCanvas, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = frame.variant, color = KdmPalette.ash, fontSize = 11.sp)
            }
            DistanceBadge(distance = frame.distance)
        }
        StatMatrix()
        InstinctRule()
    }
}

/** The plinth's top-right `Distancia` badge: label, stat display, `casillas` unit, stacked. */
@Composable
private fun DistanceBadge(distance: String) {
    Column(
        modifier =
            Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(KdmPalette.surfaceHighest)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Distancia", color = KdmPalette.gold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Text(text = distance, color = KdmPalette.onCanvas, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Text(text = "casillas", color = KdmPalette.ash, fontSize = 11.sp)
    }
}

/** Region 2b — the monster stat matrix `Mov 6 / Res 8 / Vel - / Daño -`, a 4-cell grid on its own
 *  darker track, exactly as the design's `surface-container-lowest` wrapper renders it. */
@Composable
private fun StatMatrix() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(KdmPalette.surfaceLowest)
                .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        StatCell("Mov", "6", Modifier.weight(1f))
        StatCell("Res", "8", Modifier.weight(1f))
        StatCell("Vel", "-", Modifier.weight(1f), muted = true)
        StatCell("Daño", "-", Modifier.weight(1f), muted = true)
    }
}

@Composable
private fun StatCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    muted: Boolean = false,
) {
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(6.dp))
                .background(KdmPalette.surfaceLow)
                .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            color = if (muted) KdmPalette.ash else KdmPalette.onCanvas,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = value,
            color = if (muted) KdmPalette.ash.copy(alpha = 0.7f) else KdmPalette.onCanvas,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Region 3 — the active-instinct rule, rendered inside the plinth card (as in the design). */
@Composable
private fun InstinctRule() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(KdmPalette.surfaceContainer)
                .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Symbol("psychology", KdmPalette.gold, 16)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Instinto: Olfateo",
                color = KdmPalette.onCanvas,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
            Text(
                text =
                    "El León Blanco olfatea el aire y finaliza su turno. " +
                        "Todos los supervivientes pasan a ser considerados amenazas activas.",
                color = KdmPalette.ash,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Region 4 — the expedition: `Cazadores en Marcha` with a chip per departing survivor. */
@Composable
private fun ExpeditionChips(
    party: List<Survivor>,
    onAssignPlayer: (Ulid, Int) -> Unit,
    players: Int,
) {
    HuntPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Symbol("groups", HuntInk, 14)
                Text(text = "Cazadores en Marcha", color = HuntInk, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Text(text = "${party.size} Linternas encendidas", color = Color(0xFFB9B9BE), fontSize = 11.sp)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            party.take(4).forEach { survivor ->
                SurvivorChip(
                    survivor = survivor,
                    selected = true,
                    onClick = { onAssignPlayer(survivor.id, players) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SurvivorChip(
    survivor: Survivor,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val initials =
        survivor.name.trim().let {
            if (it.isEmpty()) {
                "??"
            } else {
                it
                    .split(Regex("\\s+"))
                    .map { w ->
                        w.first().uppercase()
                    }.take(2)
                    .joinToString("")
            }
        }
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (selected) HuntRed else Color(0xFF2A2A2E)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .padding(top = 6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF131315))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(text = initials, color = HuntInk, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Text(
            text = survivor.name.ifBlank { "Unnamed" },
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** A dark panel card with the design's `surface-container-low` fill. */
@Composable
private fun HuntPanel(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = KdmPalette.surfaceLow,
        contentColor = KdmPalette.onCanvas,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content,
        )
    }
}

/** Region 6 — the active hunt-event card, with its title, description and result table verbatim. */
@Composable
private fun HuntEventCard(
    space: HuntSpace,
    index: Int,
    huntEvents: List<io.kdmtracker.core.domain.HuntEvent>,
    onAssign: (String) -> Unit,
    onResolve: (String) -> Unit,
    onMarkKind: (HuntSpaceKind) -> Unit,
) {
    HuntPanel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Symbol("auto_stories", KdmPalette.gold, 16)
            Text(
                text = "Evento de Caza #$index",
                color = KdmPalette.gold,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
            Text(
                text = "Tirada d10",
                color = KdmPalette.onCanvas,
                fontSize = 11.sp,
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(KdmPalette.surfaceHighest)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        Text(
            text = "Bruma Olorosa y Viento Fétido",
            color = KdmPalette.onCanvas,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )
        Text(
            text =
                "\"El hedor metálico a carne descompuesta empapa las túnicas. " +
                    "En la penumbra, una densa neblina borra las huellas de la bestia.\"",
            color = KdmPalette.ash,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontStyle = FontStyle.Italic,
        )
        Text(text = "Tabla de Resultados", color = KdmPalette.gold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        ResultRange(
            "1 - 3",
            "Pánico en la Ceguera",
            "Todos los cazadores sufren ",
            "1 de daño al Cerebro",
            ". El monstruo avanza 1 casilla hacia ellos.",
            KdmPalette.blood,
        )
        ResultRange(
            "4 - 7",
            "Huellas Frescas",
            "El rastreador activo obtiene ",
            "+1 de Supervivencia",
            ". Los cazadores continúan la marcha sin desvíos.",
            KdmPalette.gold,
        )
        ResultRange(
            "8 - 10",
            "Viento a Favor (Ventaja)",
            "Descubrís mechones de pelaje ensangrentado. ",
            "+1 PE de Caza",
            " para el grupo y avanzáis 1 casilla adicional.",
            KdmPalette.onCanvas,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(text = "Resultado d10", color = HuntInk, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(text = "Sin tirar aún", color = Color(0xFFB9B9BE), fontSize = 12.sp)
            }
            Button(
                onClick = { /* d100 resolution is deferred to the backlog slice (S4) */ },
                enabled = false,
                shape = RoundedCornerShape(8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Symbol("casino", MaterialTheme.colorScheme.secondary, 18)
                    Text(text = "Tirar Dado", fontSize = 12.sp)
                }
            }
        }
    }
}

/** One result row: a boxed d10 range, its title, and its effect with the highlighted phrase. */
@Composable
private fun ResultRange(
    range: String,
    title: String,
    prefix: String,
    highlight: String,
    suffix: String,
    highlightColor: Color,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(KdmPalette.surfaceLowest)
                .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // The design's fixed range box (`w-8 h-8`) on the left of every row.
        Box(
            modifier =
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(KdmPalette.surfaceHigh),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = range, color = KdmPalette.onCanvas, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, color = KdmPalette.onCanvas, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(
                text =
                    buildAnnotatedString {
                        append(prefix)
                        withStyle(
                            SpanStyle(color = highlightColor, fontWeight = FontWeight.SemiBold),
                        ) { append(highlight) }
                        append(suffix)
                    },
                color = KdmPalette.ash,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
        }
    }
}

/** Region 7 — the three hunt action buttons: a full-width advance over a 2-up pair. */
@Composable
private fun ActionButtons(
    onAdvance: () -> Unit,
    onMoveMonster: () -> Unit,
    onShowdown: () -> Unit,
) {
    Button(
        onClick = onAdvance,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = KdmPalette.surfaceHigh,
                contentColor = KdmPalette.onCanvas,
            ),
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Symbol("arrow_forward", KdmPalette.gold, 20)
            Text(text = "Avanzar Supervivientes (+1 Casilla)", fontSize = 13.sp)
        }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // Mover Bestia: filled `surface-container-low`, ink on the right.
        Button(
            onClick = onMoveMonster,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = KdmPalette.surfaceLow,
                    contentColor = KdmPalette.ash,
                ),
        ) {
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Symbol("directions_walk", KdmPalette.blood, 18)
                Text(text = "Mover Bestia", fontSize = 12.sp)
            }
        }
        // Enfrentamiento: the design's red filled action.
        Button(
            onClick = onShowdown,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = KdmPalette.onCrimsonContainer, // #8E1214
                    contentColor = KdmPalette.crimsonContainer, // #FFDAD6
                ),
        ) {
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Symbol("swords", KdmPalette.crimsonContainer, 18)
                Text(text = "Enfrentamiento", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun HuntSetup(
    state: UiState,
    onSetUpHunt: (String, Int, Int) -> Unit,
) {
    var quarry by remember(state.quarries) { mutableStateOf(state.quarries.firstOrNull().orEmpty()) }
    var level by remember { mutableIntStateOf(1) }
    var players by remember { mutableIntStateOf(1) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(text = state.settlementName, style = MaterialTheme.typography.headlineMedium)
        Text(text = "Set up the hunt", style = MaterialTheme.typography.titleMedium)

        Text(text = "Quarry", style = MaterialTheme.typography.titleSmall)
        state.quarries.forEach { name ->
            OutlinedButton(onClick = { quarry = name }, modifier = Modifier.fillMaxWidth()) {
                Text(if (name == quarry) "• $name" else name)
            }
        }

        IntStepper(label = "Level", value = level, min = 1, max = 3, onChange = { level = it })
        IntStepper(label = "Players", value = players, min = 1, max = 4, onChange = { players = it })

        Button(
            onClick = { onSetUpHunt(quarry, level, players) },
            enabled = quarry.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Set up")
        }
    }
}

@Composable
private fun IntStepper(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    onChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = label, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = { onChange((value - 1).coerceAtLeast(min)) }) { Text("−") }
        Text(text = value.toString(), style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = { onChange((value + 1).coerceAtMost(max)) }) { Text("+") }
    }
}
