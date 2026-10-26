package io.kdmtracker.app.ui.hunt

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kdmtracker.app.UiState
import io.kdmtracker.core.domain.HuntSpace
import io.kdmtracker.core.domain.HuntSpaceKind
import io.kdmtracker.core.domain.HuntStatus
import io.kdmtracker.core.domain.SurvivorStatus
import io.kdmtracker.core.domain.Ulid

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
    onEditHuntRoll: (Int, Int) -> Unit,
    onAdvanceHunt: () -> Unit,
    onMoveMonster: () -> Unit,
    onRetreatMonster: () -> Unit,
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
            // The drawn card belongs to the party's current space, so resolving it always writes
            // there — navigating to another space never re-targets the card (it stays a history
            // view). onMarkKind still edits the selected space, as the mark picker is per-space.
            partySpace = hunt.spaces.getOrElse(hunt.position) { HuntSpace() },
            huntEvents = state.huntEvents,
            drawnEvent = hunt.drawnEvent,
            atStart = hunt.position == 0,
            stepRoll = hunt.spaces.getOrElse(selected) { HuntSpace() }.roll,
            onEditRoll = { onEditHuntRoll(selected, it) },
            onAssign = { onAssignHuntEvent(hunt.position, it) },
            onResolve = { onResolveHuntEvent(hunt.position, it) },
            onMarkKind = { onMarkSpaceKind(selected, it) },
        )
        ActionButtons(
            onAdvance = onAdvanceHunt,
            onMoveMonster = onMoveMonster,
            onRetreatMonster = onRetreatMonster,
            onShowdown = { onFinishHunt(HuntStatus.QUARRY_FOUND) },
            // S7: the advance gate — a drawn card not yet resolved on the party's space blocks it.
            advanceBlocked =
                run {
                    val drawn = hunt.drawnEvent
                    drawn != null &&
                        drawn.name !in hunt.spaces.getOrElse(hunt.position) { HuntSpace() }.resolved
                },
        )
    }
}
