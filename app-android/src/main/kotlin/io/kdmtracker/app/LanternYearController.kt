package io.kdmtracker.app

import io.kdmtracker.core.content.ContentCatalog
import io.kdmtracker.core.domain.HuntSpaceKind
import io.kdmtracker.core.domain.HuntStatus
import io.kdmtracker.core.domain.MilestoneId
import io.kdmtracker.core.domain.PrincipleCategory
import io.kdmtracker.core.domain.ResourceEntry
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.ShowdownStatus
import io.kdmtracker.core.domain.Survivor
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.ruleset.Core16Ruleset
import io.kdmtracker.core.ruleset.Ruleset
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

/**
 * Mirrors `:core` results into a [UiState] for the UI. Plain Kotlin (no Android APIs) so it runs in
 * JVM tests. Each action delegates to the class that owns its feature: [YearFlowActions],
 * [SettlementActions], [SurvivorActions], [HuntPhaseActions] and [ShowdownActions].
 */
class LanternYearController(
    private val repository: SettlementRepository,
    ruleset: Ruleset = Core16Ruleset,
    catalog: ContentCatalog? = null,
    newSurvivorId: () -> Ulid = { Ulid.generate(System.currentTimeMillis()) { Random.nextInt() } },
) {
    private val session = ControllerSession(repository, ruleset, catalog, newSurvivorId)
    private val flow = YearFlowActions(session)
    private val settlement = SettlementActions(session)
    private val survivors = SurvivorActions(session)
    private val hunt = HuntPhaseActions(session)
    private val showdown = ShowdownActions(session)

    val state: StateFlow<UiState> = session.state

    fun cards(): List<CampaignCard> = repository.listCards()

    /** Returns `false` and changes nothing if the id is unknown. */
    fun selectCampaign(campaignId: String): Boolean {
        if (!repository.select(campaignId)) return false
        reload()
        return true
    }

    /** A hub failure escapes unchanged and the prior selection is kept. */
    fun createCampaign(name: String): CampaignCard {
        val card = repository.create(name)
        reload()
        return card
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

    fun advance() = flow.advance()

    fun failHunt() = flow.failHunt()

    fun previous() = flow.previous()

    fun selectStep(number: Int) = flow.selectStep(number)

    fun finishHunt(outcome: HuntStatus) = flow.finishHunt(outcome)

    fun finishShowdown(status: ShowdownStatus) = flow.finishShowdown(status)

    fun innovate(innovationName: String) = settlement.innovate(innovationName)

    fun build(locationName: String) = settlement.build(locationName)

    fun spendEndeavor() = settlement.spendEndeavor()

    fun addResource(entry: ResourceEntry) = settlement.addResource(entry)

    fun craft(
        locationName: String,
        gearName: String,
    ) = settlement.craft(locationName, gearName)

    fun recordDeaths(count: Int) = settlement.recordDeaths(count)

    fun recordBirths(count: Int) = settlement.recordBirths(count)

    fun choosePrinciple(
        category: PrincipleCategory,
        label: String,
    ) = settlement.choosePrinciple(category, label)

    fun resolveMilestone(id: MilestoneId) = settlement.resolveMilestone(id)

    fun recordSettlementEvent(name: String) = settlement.recordSettlementEvent(name)

    fun healSurvivors() = survivors.healSurvivors()

    fun markDeparting(ids: List<Ulid>) = survivors.markDeparting(ids)

    fun assignGear(
        survivorId: Ulid,
        gearName: String,
    ) = survivors.assignGear(survivorId, gearName)

    fun unassignGear(
        survivorId: Ulid,
        gearName: String,
    ) = survivors.unassignGear(survivorId, gearName)

    fun updateSurvivor(survivor: Survivor) = survivors.updateSurvivor(survivor)

    fun setUpHunt(
        quarry: String,
        level: Int,
        players: Int,
    ) = hunt.setUpHunt(quarry, level, players)

    fun markSpaceKind(
        index: Int,
        kind: HuntSpaceKind,
    ) = hunt.markSpaceKind(index, kind)

    fun advanceHunt() = hunt.advanceHunt()

    fun moveMonster() = hunt.moveMonster()

    fun retreatMonster() = hunt.retreatMonster()

    fun assignPlayer(
        survivorId: Ulid,
        player: Int,
    ) = hunt.assignPlayer(survivorId, player)

    fun assignHuntEvent(
        index: Int,
        eventName: String,
    ) = hunt.assignHuntEvent(index, eventName)

    fun resolveHuntEvent(
        index: Int,
        eventName: String,
    ) = hunt.resolveHuntEvent(index, eventName)

    fun editHuntRoll(
        index: Int,
        roll: Int,
    ) = hunt.editHuntRoll(index, roll)

    fun declareSpecialShowdown(enabled: Boolean) = showdown.declareSpecialShowdown(enabled)

    fun setUpShowdown(
        monster: String,
        level: Int,
        deck: List<String>,
    ) = showdown.setUpShowdown(monster, level, deck)

    fun drawShowdownCard() = showdown.drawShowdownCard()

    fun woundMonster() = showdown.woundMonster()

    fun grantHuntXp(survivorIds: List<Ulid>) = showdown.grantHuntXp(survivorIds)
}
