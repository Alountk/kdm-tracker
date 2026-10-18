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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kdmtracker.app.UiState
import io.kdmtracker.core.domain.HuntSpace
import io.kdmtracker.core.domain.HuntSpaceKind
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
        InstinctRule()
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
    HuntPanel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier =
                    Modifier
                        .clip(CircleShape)
                        .background(HuntRed)
                        .padding(6.dp),
            ) {
                Text(text = "pets", color = Color.White, fontSize = 14.sp)
            }
            Text(text = "Presa Activa", color = HuntInk, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(
                text = "Nivel $level",
                color = Color.White,
                fontSize = 12.sp,
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(HuntRed)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        Text(
            text = state.hunt?.quarry ?: "León Blanco",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
        )
        Text(text = "Variante Primera Historia", color = Color(0xFFB9B9BE), fontSize = 12.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = "Distancia", color = HuntInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(text = distance(state), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(text = "casillas", color = Color(0xFFB9B9BE), fontSize = 12.sp)
        }
        StatMatrix()
    }
}

private fun distance(state: UiState): String {
    val hunt = state.hunt ?: return "3"
    return (hunt.monsterPosition - hunt.position).coerceAtLeast(0).toString()
}

/** Region 2b — the monster stat matrix `Mov 6 / Res 8 / Vel - / Daño -`. */
@Composable
private fun StatMatrix() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatCell("Mov", "6", Modifier.weight(1f))
        StatCell("Res", "8", Modifier.weight(1f))
        StatCell("Vel", "-", Modifier.weight(1f))
        StatCell("Daño", "-", Modifier.weight(1f))
    }
}

@Composable
private fun StatCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(8.dp))
                .background(HuntDeepRed)
                .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = label, color = HuntInk, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(text = value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

/** Region 3 — the active-instinct rule. */
@Composable
private fun InstinctRule() {
    HuntPanel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = "psychology", color = HuntInk, fontSize = 16.sp)
            Text(text = "Instinto: Olfateo", color = HuntInk, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Text(
            text =
                "El León Blanco olfatea el aire y finaliza su turno. " +
                    "Todos los supervivientes pasan a ser considerados amenazas activas.",
            color = Color(0xFFD8D8DC),
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
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
                Text(text = "groups", color = HuntInk, fontSize = 14.sp)
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

/** A dark panel with a gold hairline border, matching the design's cards. */
@Composable
private fun HuntPanel(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF1C1C1F),
        contentColor = Color.White,
        shape = RoundedCornerShape(10.dp),
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
            Text(text = "auto_stories", color = HuntInk, fontSize = 16.sp)
            Text(text = "Evento de Caza #$index", color = HuntInk, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(
                text = "Tirada d10",
                color = HuntInk,
                fontSize = 11.sp,
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF2A2A2E))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        Text(
            text = "Bruma Olorosa y Viento Fétido",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )
        Text(
            text =
                "El hedor metálico a carne descompuesta empapa las túnicas. " +
                    "En la penumbra, una densa neblina borra las huellas de la bestia.",
            color = Color(0xFFD8D8DC),
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
        Text(text = "Tabla de Resultados", color = HuntInk, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        ResultRange("1 - 3", "Pánico en la Ceguera", "1 de daño al Cerebro")
        ResultRange("4 - 7", "Huellas Frescas", "+1 de Supervivencia")
        ResultRange("8 - 10", "Viento a Favor (Ventaja)", "+1 PE de Caza")

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
                Text(text = "casino  Tirar Dado", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ResultRange(
    range: String,
    title: String,
    effect: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF242427))
                .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = range,
            color = HuntInk,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.Top),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(text = effect, color = Color(0xFFD8D8DC), fontSize = 12.sp, lineHeight = 15.sp)
        }
    }
}

/** Region 7 — the three hunt action buttons. */
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
    ) {
        Text(text = "arrow_forward  Avanzar Supervivientes (+1 Casilla)", fontSize = 13.sp)
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = onMoveMonster,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
        ) {
            Text(text = "directions_walk  Mover Bestia", fontSize = 12.sp)
        }
        OutlinedButton(
            onClick = onShowdown,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
        ) {
            Text(text = "swords  Enfrentamiento", fontSize = 12.sp)
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
