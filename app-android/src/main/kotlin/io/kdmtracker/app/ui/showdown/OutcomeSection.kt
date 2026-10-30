package io.kdmtracker.app.ui.showdown

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kdmtracker.app.ui.components.SheetPanel
import io.kdmtracker.app.ui.components.StoneButton
import io.kdmtracker.app.ui.theme.KdmActionButton
import io.kdmtracker.app.ui.theme.KdmActionVariant
import io.kdmtracker.app.ui.theme.KdmType
import io.kdmtracker.core.domain.ShowdownStatus
import io.kdmtracker.core.domain.Ulid

/**
 * How the encounter ends and what it leaves behind: the victory/defeat outcome, the hunt XP for
 * the departing party and the deaths to record. The design has no panel for these, so they sit
 * below its sections in the same panel style.
 */
@Composable
internal fun OutcomeSection(
    partyIds: List<Ulid>,
    onGrantHuntXp: (List<Ulid>) -> Unit,
    onRecordDeaths: (Int) -> Unit,
    onFinish: (ShowdownStatus) -> Unit,
) {
    var deaths by rememberSaveable { mutableIntStateOf(0) }
    SheetPanel(title = "Desenlace", icon = "swords") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KdmActionButton(
                text = "Victoria",
                onClick = { onFinish(ShowdownStatus.VICTORY) },
                modifier = Modifier.weight(1f),
                icon = "check_circle",
                variant = KdmActionVariant.GOLD,
            )
            KdmActionButton(
                text = "Derrota",
                onClick = { onFinish(ShowdownStatus.DEFEAT) },
                modifier = Modifier.weight(1f),
                icon = "skull",
                variant = KdmActionVariant.DARK,
            )
        }
        KdmActionButton(
            text = "+1 PX de caza al grupo (${partyIds.size})",
            onClick = { onGrantHuntXp(partyIds) },
            modifier = Modifier.fillMaxWidth(),
            enabled = partyIds.isNotEmpty(),
            variant = KdmActionVariant.DARK,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "MUERTES",
                style = KdmType.labelCaps,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            StoneButton("−", onClick = { deaths = (deaths - 1).coerceAtLeast(0) })
            Text(text = deaths.toString(), style = KdmType.statDisplay, color = MaterialTheme.colorScheme.error)
            StoneButton("+", onClick = { deaths += 1 })
            KdmActionButton(
                text = "Registrar",
                onClick = {
                    onRecordDeaths(deaths)
                    deaths = 0
                },
                enabled = deaths > 0,
                variant = KdmActionVariant.DARK,
            )
        }
    }
}
