package io.kdmtracker.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kdmtracker.app.UiState
import io.kdmtracker.app.ui.showdown.AiCardSection
import io.kdmtracker.app.ui.showdown.EncounterSetup
import io.kdmtracker.app.ui.showdown.HitLocationsSection
import io.kdmtracker.app.ui.showdown.OutcomeSection
import io.kdmtracker.app.ui.showdown.ShowdownBoardModel
import io.kdmtracker.app.ui.showdown.ShowdownHeader
import io.kdmtracker.core.domain.ShowdownStatus
import io.kdmtracker.core.domain.SurvivorStatus
import io.kdmtracker.core.domain.Ulid

/**
 * Stateless Showdown Phase body.
 *
 * Before setup it offers a monster/level form; afterwards it tracks the encounter and records the
 * outcome plus the aftermath. No state and no rules live here: everything is reported through the
 * callbacks.
 */
@Composable
fun ShowdownScreen(
    state: UiState,
    onSetUpShowdown: (String, Int, List<String>) -> Unit,
    onDrawShowdownCard: () -> Unit,
    onWoundMonster: () -> Unit,
    onHealMonster: () -> Unit,
    onGrantHuntXp: (List<Ulid>) -> Unit,
    onRecordDeaths: (Int) -> Unit,
    onFinishShowdown: (ShowdownStatus) -> Unit,
) {
    val showdown = state.showdown
    if (showdown == null || showdown.monster.isBlank()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
        ) {
            EncounterSetup(state = state, onSetUpShowdown = onSetUpShowdown)
        }
        return
    }

    val model = ShowdownBoardModel.of(showdown, state.monsterCards)
    val party = state.survivors.filter { it.status == SurvivorStatus.DEPARTING }.map { it.id }
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ShowdownHeader(model = model, onWound = onWoundMonster, onHeal = onHealMonster)
        Column(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            state.actionError?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            AiCardSection(drawn = model.drawn, onDraw = onDrawShowdownCard)
            HitLocationsSection(locations = model.hitLocations, onWound = onWoundMonster)
            OutcomeSection(
                partyIds = party,
                onGrantHuntXp = onGrantHuntXp,
                onRecordDeaths = onRecordDeaths,
                onFinish = onFinishShowdown,
            )
        }
    }
}
