package io.kdmtracker.app

import io.kdmtracker.core.content.ContentCatalog
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.ruleset.Core16Ruleset
import io.kdmtracker.core.ruleset.Ruleset
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

/**
 * Mirrors `:core` results into a [UiState] for the UI. Plain Kotlin (no Android APIs) so it runs in
 * JVM tests. Feature actions live in [flow], [settlement], [survivors], [hunt] and [showdown];
 * this class keeps the campaign-level operations.
 */
class LanternYearController(
    private val repository: SettlementRepository,
    ruleset: Ruleset = Core16Ruleset,
    catalog: ContentCatalog? = null,
    newSurvivorId: () -> Ulid = { Ulid.generate(System.currentTimeMillis()) { Random.nextInt() } },
) {
    private val session = ControllerSession(repository, ruleset, catalog, newSurvivorId)
    internal val flow = YearFlowActions(session)
    internal val settlement = SettlementActions(session)
    internal val survivors = SurvivorActions(session)
    internal val hunt = HuntPhaseActions(session, flow)
    internal val showdown = ShowdownActions(session)

    val state: StateFlow<UiState> = session.state

    fun cards(): List<CampaignCard> = repository.listCards()

    /** Returns `false` and changes nothing if the id is unknown. */
    fun selectCampaign(campaignId: String): Boolean {
        if (!repository.select(campaignId)) return false
        reload()
        return true
    }

    /** A hub failure escapes unchanged and the prior selection is kept. */
    fun createCampaign(
        name: String,
        expansions: Set<String> = emptySet(),
    ): CampaignCard {
        val card = repository.create(name, expansions)
        reload()
        return card
    }

    /** A refused choice escapes as [IllegalArgumentException] and changes nothing. */
    fun changePacks(wanted: Collection<String>) {
        repository.changePacks(wanted)
        session.republish()
    }

    /**
     * A sync pass mutates the store behind the controller's back; without this the UI keeps rendering
     * the pre-sync frame.
     */
    fun reload() {
        repository.reload()
        session.republish()
    }

    fun nemesisFor(record: SettlementRecord): String? = record.currentNemesis()
}
