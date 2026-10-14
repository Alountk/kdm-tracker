package io.kdmtracker.app

import io.kdmtracker.app.ui.settlement.InnovarGate
import io.kdmtracker.app.ui.settlement.InnovationsModel
import io.kdmtracker.app.ui.settlement.PhaseState
import io.kdmtracker.app.ui.settlement.ResourceNeed
import io.kdmtracker.app.ui.settlement.SlotState
import io.kdmtracker.core.domain.ActionKind
import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.Innovation
import io.kdmtracker.core.domain.InnovationCollection
import io.kdmtracker.core.domain.InnovationId
import io.kdmtracker.core.domain.LocalizedText
import io.kdmtracker.core.domain.LocationId
import io.kdmtracker.core.domain.PrincipleCategory
import io.kdmtracker.core.domain.ResourceCategory
import io.kdmtracker.core.domain.ResourceDefinition
import io.kdmtracker.core.domain.ResourceId
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SettlementAction
import io.kdmtracker.core.domain.SettlementLocation
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

    private val freeHoard =
        SettlementLocation(
            LocationId("lantern_hoard"),
            LocalizedText(es = "Cúmulo de Linternas"),
            actions =
                listOf(
                    SettlementAction(
                        names = LocalizedText(es = "Innovar"),
                        kind = ActionKind.INNOVATE,
                        endeavorCost = 0,
                    ),
                ),
        )

    private fun state(
        definitions: List<Innovation> = emptyList(),
        locations: List<SettlementLocation> = listOf(freeHoard),
        built: List<String> = listOf("Cúmulo de Linternas"),
        lanternYear: Int = 1,
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
        lanternYear = lanternYear,
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
        catalogLocations = locations,
        builtLocations = built,
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

        assertFalse(model.cost.ok, "the action is unaffordable here")
        assertEquals(PhaseState.USED_THIS_PHASE, model.phaseState)
    }

    @Test
    fun unaffordableBeatsTheOffer() {
        val paint = innovation("pintura")
        val model =
            InnovationsModel.of(
                state(views = listOf(view(paint, InnovationStatus.AVAILABLE, endeavorCost = 1)), endeavors = 0),
            )

        assertFalse(model.cost.ok, "the action is unaffordable here")
        assertEquals(PhaseState.UNAFFORDABLE, model.phaseState)
    }

    @Test
    fun theModelCarriesTheClassifiedCatalogue() {
        val locked = innovation("alfa", es = "Alfa", category = "Ciencia")
        val owned = innovation("zeta", es = "Zeta", category = "Arte")

        val model =
            InnovationsModel.of(
                state(
                    definitions = listOf(locked, owned),
                    views =
                        listOf(
                            InnovationView(locked, InnovationStatus.NO_ACCESS, missingParent = "Lenguaje"),
                            view(owned, InnovationStatus.UNLOCKED),
                        ),
                ),
            )

        // Acquired first, then by name — the browser's order, not the definitions'.
        assertEquals(listOf("Zeta", "Alfa"), model.catalogue.map { it.name })
        assertEquals("ADQUIRIDA", model.catalogue[0].footer)
        assertEquals("SIN ACCESO", model.catalogue[1].footer)
        assertEquals("Lenguaje", model.catalogue[1].requires)
        assertEquals("Ciencia", model.catalogue[1].category)
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
    fun theFourthSlotAndTheRuleLineComeFromTheState() {
        val hoard =
            SettlementLocation(
                LocationId("lantern_hoard"),
                LocalizedText(es = "Cúmulo de Linternas"),
                actions = listOf(SettlementAction(names = LocalizedText(es = "Innovar"), kind = ActionKind.INNOVATE)),
            )

        val model =
            InnovationsModel.of(
                state(
                    locations = listOf(hoard),
                    built = listOf("Cúmulo de Linternas"),
                    draw = 2,
                    keep = 1,
                    lanternYear = 3,
                ),
            )

        assertEquals("Cúmulo de Linternas", model.activeDeck?.name)
        assertEquals("Innovar: Elige 1 de 2 Cartas", model.offerTitle)
        assertEquals(
            "Regla oficial KDM: Roba 2 innovaciones, quédate 1 y devuelve la otra al mazo.",
            model.ruleLine,
        )
        assertEquals(3, model.lanternYear)
    }

    @Test
    fun theRuleLineFollowsAHouseRuleThatKeepsMoreThanOne() {
        val model = InnovationsModel.of(state(draw = 4, keep = 3))

        assertEquals(
            "Regla oficial KDM: Roba 4 innovaciones, quédate 3 y devuelve el resto al mazo.",
            model.ruleLine,
        )
        assertEquals("Innovar: Elige 3 de 4 Cartas", model.offerTitle)
    }

    @Test
    fun aSettlementShortOnResourcesShowsOnlyTheCostBlock() {
        val hoard =
            SettlementLocation(
                LocationId("lantern_hoard"),
                LocalizedText(es = "Cúmulo de Linternas"),
                actions = listOf(SettlementAction(names = LocalizedText(es = "Innovar"), kind = ActionKind.INNOVATE)),
            )
        val model =
            InnovationsModel.of(
                state(
                    locations = listOf(hoard),
                    built = listOf("Cúmulo de Linternas"),
                    views = listOf(view(innovation("pintura"), InnovationStatus.AVAILABLE, endeavorCost = 1)),
                    endeavors = 0,
                ),
            )

        assertEquals(InnovarGate.SHORT_ON_RESOURCES, model.gate)
        assertEquals(emptyList(), model.hand, "a settlement that cannot pay holds no cards")
        assertEquals(listOf(ResourceNeed("Esfuerzo", have = 0, need = 1)), model.missing)
        assertTrue(model.motivo.startsWith("No es posible seleccionar"), model.motivo)
    }

    @Test
    fun aSettlementWithoutABuiltInnovateLocationShowsTheNoAccessBlock() {
        val model =
            InnovationsModel.of(
                state(built = emptyList(), views = listOf(view(innovation("pintura"), InnovationStatus.AVAILABLE))),
            )

        assertEquals(InnovarGate.NO_ACCESS, model.gate)
        assertEquals(emptyList(), model.hand)
    }

    @Test
    fun anInnovationUnlockedThisYearIsNew() {
        assertEquals(SlotState.NEW_THIS_YEAR, SlotState.of(unlockedInYear = 3, lanternYear = 3))
    }

    @Test
    fun anOlderInnovationIsOwned() {
        assertEquals(SlotState.OWNED, SlotState.of(unlockedInYear = 2, lanternYear = 3))
        assertEquals(
            SlotState.OWNED,
            SlotState.of(unlockedInYear = 0, lanternYear = 3),
            "a document from before the field existed reads as already owned",
        )
    }

    @Test
    fun theGridPaintsTheSlotsByTheirState() {
        val fresh = innovation("pintura", es = "Pintura")
        val old = innovation("lenguaje", es = "Lenguaje")
        val model =
            InnovationsModel.of(
                state(
                    definitions = listOf(old, fresh),
                    unlocked = listOf(old.copy(unlockedInYear = 1), fresh.copy(unlockedInYear = 3)),
                    lanternYear = 3,
                ),
            )

        assertEquals(SlotState.OWNED, model.slots.first { it.name == "Lenguaje" }.state)
        assertEquals(SlotState.NEW_THIS_YEAR, model.slots.first { it.name == "Pintura" }.state)
    }

    @Test
    fun theCostChipAbbreviatesResourcesToInitials() {
        val hoard =
            SettlementLocation(
                LocationId("lantern_hoard"),
                LocalizedText(es = "Cúmulo de Linternas"),
                actions =
                    listOf(
                        SettlementAction(
                            names = LocalizedText(es = "Innovar"),
                            kind = ActionKind.INNOVATE,
                            endeavorCost = 1,
                            resourceCost =
                                mapOf(
                                    ResourceId("bone") to 1,
                                    ResourceId("organ") to 1,
                                    ResourceId("hide") to 1,
                                ),
                        ),
                    ),
            )
        val model =
            InnovationsModel.of(
                state(
                    locations = listOf(hoard),
                    built = listOf("Cúmulo de Linternas"),
                    views =
                        listOf(
                            view(
                                innovation("pintura"),
                                InnovationStatus.AVAILABLE,
                                endeavorCost = 1,
                                resourceCost =
                                    mapOf(
                                        ResourceId("bone") to 1,
                                        ResourceId("organ") to 1,
                                        ResourceId("hide") to 1,
                                    ),
                            ),
                        ),
                    resources =
                        listOf(
                            resource("bone", "Hueso de Monstruo"),
                            resource("organ", "Órgano de Monstruo"),
                            resource("hide", "Piel de Monstruo"),
                        ),
                    storage =
                        listOf(
                            StorageLine(ResourceId("bone"), 1),
                            StorageLine(ResourceId("organ"), 1),
                            StorageLine(ResourceId("hide"), 1),
                        ),
                ),
            )

        assertEquals(1, model.costChip.endeavors)
        assertEquals(listOf("H", "Ó", "P"), model.costChip.resources)
        assertEquals("1H 1Ó 1P", model.costChip.abbreviated)
    }

    @Test
    fun theGridShowsTheChosenPrincipleAndTheNewestUnlock() {
        val principle = innovation("lenguaje", es = "Lenguaje")
        val oldA = innovation("escultura", es = "Escultura")
        val oldB = innovation("choza", es = "Choza")
        val fresh = innovation("pintura", es = "Pintura")

        val model =
            InnovationsModel.of(
                state(
                    definitions = listOf(principle, oldA, oldB, fresh),
                    unlocked =
                        listOf(
                            principle.copy(unlockedInYear = 1),
                            oldA.copy(unlockedInYear = 1),
                            oldB.copy(unlockedInYear = 2),
                            fresh.copy(unlockedInYear = 3),
                        ),
                    principles = mapOf(PrincipleCategory.NEW_LIFE to "Lenguaje"),
                    lanternYear = 3,
                ),
            )

        assertEquals("Lenguaje", model.slots.first().name, "the spec's cells lead with the chosen principle")
        assertEquals(
            "Pintura",
            model.slots.firstOrNull { it.name == "Pintura" }?.name,
            "the newest unlock must not be pushed past take(3) by three older ones",
        )
        assertEquals(SlotState.NEW_THIS_YEAR, model.slots.first { it.name == "Pintura" }.state)
        assertEquals(SlotState.OWNED, model.slots.first { it.name == "Lenguaje" }.state)
    }
}
