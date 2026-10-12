package io.kdmtracker.app

import io.kdmtracker.app.ui.settlement.InnovationsModel
import io.kdmtracker.app.ui.settlement.PhaseState
import io.kdmtracker.app.ui.settlement.ResourceNeed
import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.Innovation
import io.kdmtracker.core.domain.InnovationCollection
import io.kdmtracker.core.domain.InnovationId
import io.kdmtracker.core.domain.LocalizedText
import io.kdmtracker.core.domain.PrincipleCategory
import io.kdmtracker.core.domain.ResourceCategory
import io.kdmtracker.core.domain.ResourceDefinition
import io.kdmtracker.core.domain.ResourceId
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.SettlementState
import io.kdmtracker.core.domain.StorageLine
import io.kdmtracker.core.domain.SyncMeta
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.rules.InnovationStatus
import io.kdmtracker.core.rules.InnovationView
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins the Develop step's Innovations panel model: the draw text must come from the ruleset (so a
 * house rule changes it), the hand is derived from the classified views, and the four phase states
 * resolve in a fixed order. The locations side of the old panel lives in `FabricarModelTest`.
 */
class DevelopPanelsTest {
    private fun innovation(
        id: String,
        es: String = id,
        description: String = "Efecto de $id.",
        consequences: List<String> = emptyList(),
        category: String = "Arte",
        survivalBonus: Int = 0,
    ) = Innovation(InnovationId(id), LocalizedText(es = es), description, consequences, survivalBonus, category)

    private fun view(
        card: Innovation,
        status: InnovationStatus,
        used: Boolean = false,
        endeavorCost: Int = 0,
        resourceCost: Map<ResourceId, Int> = emptyMap(),
    ) = InnovationView(card, status, used = used, endeavorCost = endeavorCost, resourceCost = resourceCost)

    private fun resource(
        id: String,
        es: String,
    ) = ResourceDefinition(ResourceId(id), LocalizedText(es = es), ResourceCategory.BASIC)

    private fun record(unlocked: List<Innovation>) =
        SettlementRecord(
            settlement =
                Settlement(
                    id = Ulid.generate(1L) { 0 },
                    campaignId = Ulid.generate(2L) { 0 },
                    name = "Test",
                    lanternYear = 1,
                    phase = SettlementPhase.SETTLEMENT,
                    currentStep = 7,
                    endeavors = 4,
                    population = 4,
                    settlementEvent = null,
                    sync = SyncMeta(HybridLogicalClock(0, 0)),
                ),
            state = SettlementState(innovations = InnovationCollection(unlocked)),
        )

    private fun state(
        definitions: List<Innovation> = emptyList(),
        views: List<InnovationView> = emptyList(),
        deck: List<String> = emptyList(),
        principles: Map<PrincipleCategory, String> = emptyMap(),
        draw: Int = 2,
        keep: Int = 1,
        endeavors: Int = 4,
        storage: List<StorageLine> = emptyList(),
        resources: List<ResourceDefinition> = emptyList(),
        unlocked: List<Innovation> = emptyList(),
        campaign: Boolean = true,
    ) = UiState(
        screen = Screen.Settlement,
        lanternYear = 1,
        phase = SettlementPhase.SETTLEMENT,
        settlementName = "Test",
        currentStep = 7,
        stepTitle = "Desarrollo",
        population = 4,
        endeavors = endeavors,
        steps = emptyList(),
        reachableSteps = emptySet(),
        lastTransition = null,
        record = if (campaign) record(unlocked) else null,
        innovationDefinitions = definitions,
        innovationViews = views,
        innovationDeck = deck,
        innovationDraw = draw,
        innovationKeep = keep,
        chosenPrinciples = principles,
        storageLines = storage,
        resourceDefinitions = resources,
    )

    @Test
    fun theDrawLabelFollowsTheRulesetNotALiteral() {
        assertEquals("Robar 2, elegir 1", InnovationsModel.of(state(draw = 2, keep = 1)).drawLabel)
        assertEquals("Robar 4, elegir 1", InnovationsModel.of(state(draw = 4, keep = 1)).drawLabel)
    }

    @Test
    fun theDeckSizeCountsTheCards() {
        assertEquals(14, InnovationsModel.of(state(deck = List(14) { "i$it" })).deckSize)
    }

    @Test
    fun aPrincipleSlotShowsTheChoiceLabelNotTheCategoryPrefix() {
        val chosen = mapOf(PrincipleCategory.NEW_LIFE to "The First Harvest → Cannibalize")

        val model = InnovationsModel.of(state(principles = chosen))

        assertEquals(listOf("The First Harvest → Cannibalize"), model.principleSlots)
        assertEquals(2, model.emptySlots)
    }

    @Test
    fun aCampaignWithNothingUnlockedIsTheEmptyState() {
        val model = InnovationsModel.of(state(views = listOf(view(innovation("lenguaje"), InnovationStatus.UNLOCKED))))

        assertEquals(emptyList(), model.hand)
        assertEquals(PhaseState.NO_CARDS, model.phaseState)
    }

    @Test
    fun aStateWithoutACampaignIsTheEmptyState() {
        val model =
            InnovationsModel.of(
                state(views = listOf(view(innovation("pintura"), InnovationStatus.AVAILABLE)), campaign = false),
            )

        assertEquals(emptyList(), model.hand)
        assertEquals(PhaseState.NO_CARDS, model.phaseState)
    }

    @Test
    fun theOfferHonoursTheRulesetDrawAndKeep() {
        val cards = List(6) { innovation("card$it") }
        val model =
            InnovationsModel.of(
                state(
                    definitions = cards,
                    views =
                        cards.map {
                            view(it, InnovationStatus.AVAILABLE)
                        },
                    draw = 4,
                    keep = 1,
                ),
            )

        assertEquals(4, model.hand.size)
        assertEquals("Robar 4, elegir 1", model.drawLabel)
    }

    @Test
    fun aMissingLedgerLineReadsAsZeroNotAsAnError() {
        val paint = innovation("pintura")
        val model =
            InnovationsModel.of(
                state(
                    definitions = listOf(paint),
                    views =
                        listOf(
                            view(
                                paint,
                                InnovationStatus.AVAILABLE,
                                endeavorCost = 1,
                                resourceCost =
                                    mapOf(
                                        ResourceId("hide.leather") to 1,
                                    ),
                            ),
                        ),
                    resources = listOf(resource("hide.leather", "Pellejo")),
                    storage = emptyList(),
                ),
            )

        assertEquals(listOf(ResourceNeed("Pellejo", have = 0, need = 1)), model.cost.resources)
        assertFalse(model.cost.ok, "an empty ledger cannot fund the action")
        assertTrue(model.cost.reason.contains("1 Pellejo"), model.cost.reason)
    }

    @Test
    fun usedThisPhaseBeatsEveryOtherState() {
        val paint = innovation("pintura")
        val model =
            InnovationsModel.of(
                state(
                    views = listOf(view(paint, InnovationStatus.AVAILABLE, used = true, endeavorCost = 99)),
                    endeavors = 0,
                ),
            )

        assertTrue(model.hand.isNotEmpty())
        assertEquals(PhaseState.USED_THIS_PHASE, model.phaseState)
    }

    @Test
    fun unaffordableBeatsTheOffer() {
        val paint = innovation("pintura")
        val model =
            InnovationsModel.of(
                state(views = listOf(view(paint, InnovationStatus.AVAILABLE, endeavorCost = 1)), endeavors = 0),
            )

        assertTrue(model.hand.isNotEmpty())
        assertEquals(PhaseState.UNAFFORDABLE, model.phaseState)
    }

    @Test
    fun theSubtitleNamesTheParentOrTheInitialInnovation() {
        val tongue = innovation("language", es = "Lenguaje", consequences = listOf("Pintura"))
        val paint = innovation("pintura", es = "Pintura")

        val model =
            InnovationsModel.of(
                state(definitions = listOf(tongue, paint), views = listOf(view(paint, InnovationStatus.AVAILABLE))),
            )

        assertEquals("consecuencia de Lenguaje", model.hand.single().subtitle)

        val orphan =
            InnovationsModel.of(
                state(definitions = listOf(paint), views = listOf(view(paint, InnovationStatus.AVAILABLE))),
            )
        assertEquals("innovación inicial", orphan.hand.single().subtitle)
    }

    @Test
    fun theFootnoteFollowsTheConsequences() {
        val branching = innovation("choza", es = "Choza", consequences = listOf("Cama"))
        val leaf = innovation("cama", es = "Cama")

        val model =
            InnovationsModel.of(
                state(
                    definitions = listOf(branching, leaf),
                    views = listOf(view(branching, InnovationStatus.AVAILABLE), view(leaf, InnovationStatus.AVAILABLE)),
                ),
            )

        assertEquals("Añade consecuencias de Choza al mazo", model.hand.first { it.name == "Choza" }.consequenceNote)
        assertEquals(null, model.hand.first { it.name == "Cama" }.consequenceNote)
    }

    @Test
    fun theCardPassesTheDefinitionThrough() {
        val card =
            innovation(
                "choza",
                es = "Choza",
                description = "Acepta este paisaje.",
                category = "Hogar",
                survivalBonus = 1,
            )

        val model =
            InnovationsModel.of(
                state(definitions = listOf(card), views = listOf(view(card, InnovationStatus.AVAILABLE))),
            )

        assertEquals("Choza", model.hand.single().name)
        assertEquals("Hogar", model.hand.single().category)
        assertEquals("Acepta este paisaje.", model.hand.single().effect)
        assertEquals(1, model.hand.single().survivalBonus)
    }

    @Test
    fun eachChipGroupCountsItsOwnDimension() {
        val art = innovation("pintura", es = "Pintura", category = "Arte")
        val art2 = innovation("escultura", es = "Escultura", category = "Arte")
        val home = innovation("choza", es = "Choza", category = "Hogar")

        val model =
            InnovationsModel.of(
                state(
                    definitions = listOf(art, art2, home),
                    views = listOf(view(art, InnovationStatus.AVAILABLE)),
                    unlocked = listOf(art),
                ),
            )

        assertEquals(listOf("Todas" to 3, "Adquiridas" to 1, "No adquiridas" to 2), model.acquisitionChips)
        assertEquals(listOf("Arte" to 2, "Hogar" to 1), model.categoryChips)
    }
}
