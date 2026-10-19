package io.kdmtracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kdmtracker.app.UiState
import io.kdmtracker.app.ui.theme.KdmFontFamilies
import io.kdmtracker.app.ui.theme.KdmPalette
import io.kdmtracker.core.domain.HuntSpace
import io.kdmtracker.core.domain.HuntSpaceKind
import io.kdmtracker.core.domain.HuntState
import io.kdmtracker.core.domain.HuntStatus
import io.kdmtracker.core.domain.Survivor
import io.kdmtracker.core.domain.SurvivorStatus
import io.kdmtracker.core.domain.Ulid

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
            drawnEvent = hunt.drawnEvent,
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
                    Text(
                        text = frame.eyebrow.uppercase(),
                        color = KdmPalette.gold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                    Box(
                        modifier =
                            Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(KdmPalette.gold),
                    )
                    Text(text = frame.levelLabel.uppercase(), color = KdmPalette.ash, fontSize = 14.sp)
                }
                Text(
                    text = frame.quarry,
                    color = KdmPalette.onCanvas,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    fontFamily = KdmFontFamilies.playfair,
                )
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
        Text(text = "Distancia".uppercase(), color = KdmPalette.gold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
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
                text = "Instinto: Olfateo".uppercase(),
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
    // The design renders this region directly on the `surface` (no wrapper card): a header row
    // with the label in `on-surface-variant` and the lantern count in `secondary` (gold), then a
    // grid of independent `surface-container-low` chips separated by gaps.
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Symbol("groups", KdmPalette.ash, 14)
                Text(
                    text = "Cazadores en Marcha".uppercase(),
                    color = KdmPalette.ash,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
            }
            Text(text = "${party.size} Linternas encendidas", color = KdmPalette.gold, fontSize = 11.sp)
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
    // The design's expedition chip: `surface-container-low` tile, a `surface-container-highest`
    // avatar with the survivor's initials, the name in `text-primary`, and the insanity (psychology)
    // / survival (favorite) counters in `text-secondary` / `text-error`.
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(8.dp))
                .background(KdmPalette.surfaceLow)
                .clickable(onClick = onClick)
                .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(KdmPalette.surfaceHighest),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = initials, color = KdmPalette.onCanvas, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
        Text(
            text = survivor.name.ifBlank { "Unnamed" },
            color = KdmPalette.onCanvas,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Symbol("psychology", KdmPalette.gold, 11)
            Text(
                text = "${survivor.insanity}",
                color = KdmPalette.onCanvas,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
            Symbol("favorite", KdmPalette.blood, 11)
            Text(
                text = "${survivor.survival}",
                color = KdmPalette.onCanvas,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
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
    drawnEvent: io.kdmtracker.core.domain.HuntEvent?,
    onAssign: (String) -> Unit,
    onResolve: (String) -> Unit,
    onMarkKind: (HuntSpaceKind) -> Unit,
) {
    HuntPanel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Symbol("auto_stories", KdmPalette.gold, 16)
            Text(
                text = "Evento de Caza #$index".uppercase(),
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

        // The card the group drew on their last advance (S5): its real name and rules text.
        if (drawnEvent != null) {
            Text(
                text = drawnEvent.name,
                color = KdmPalette.onCanvas,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                fontFamily = KdmFontFamilies.playfair,
            )
            Text(
                text = drawnEvent.text,
                color = KdmPalette.ash,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
            // Resolve by hand: record the drawn card on this space and mark it resolved.
            val alreadyOnSpace = drawnEvent.name in space.events
            val isResolved = drawnEvent.name in space.resolved
            OutlinedButton(
                onClick = {
                    if (!alreadyOnSpace) onAssign(drawnEvent.name)
                    if (!isResolved) onResolve(drawnEvent.name)
                },
                enabled = !isResolved,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isResolved) "Resuelta ✓" else "Marcar resuelta")
            }
        } else {
            Text(
                text = "Avanza el grupo para robar una carta de evento de caza.",
                color = KdmPalette.ash,
                fontSize = 12.sp,
            )
        }

        // The selected space's resolved history (S5): visible when navigating backward.
        if (space.events.isNotEmpty()) {
            Text(
                text = "Historial de la casilla $index".uppercase(),
                color = KdmPalette.onCanvas,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp),
            )
            space.events.forEach { name ->
                val resolved = name in space.resolved
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(KdmPalette.surfaceHighest)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = name,
                        color = if (resolved) KdmPalette.ash else KdmPalette.onCanvas,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = if (resolved) "Resuelta" else "Pendiente",
                        color = if (resolved) KdmPalette.gold else KdmPalette.blood,
                        fontSize = 11.sp,
                    )
                }
            }
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
            Text(text = "Avanzar Supervivientes (+1 Casilla)".uppercase(), fontSize = 13.sp)
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
                Text(text = "Mover Bestia".uppercase(), fontSize = 12.sp)
            }
        }
        // Enfrentamiento: the design's red filled action.
        Button(
            onClick = onShowdown,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = HuntDeepRed, // design's on-tertiary-fixed-variant #8E1214
                    contentColor = KdmPalette.crimsonContainer, // #FFDAD6
                ),
        ) {
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Symbol("swords", KdmPalette.crimsonContainer, 18)
                Text(text = "Enfrentamiento".uppercase(), fontSize = 12.sp)
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
