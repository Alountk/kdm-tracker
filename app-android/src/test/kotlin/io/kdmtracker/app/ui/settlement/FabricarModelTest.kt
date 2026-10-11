package io.kdmtracker.app.ui.settlement

import io.kdmtracker.app.CraftRecipeView
import io.kdmtracker.app.Screen
import io.kdmtracker.app.UiState
import io.kdmtracker.core.content.ContentCatalog
import io.kdmtracker.core.content.Core16Content
import io.kdmtracker.core.content.Legacy15Content
import io.kdmtracker.core.domain.ActionKind
import io.kdmtracker.core.domain.GearId
import io.kdmtracker.core.domain.GearRecipe
import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.Innovation
import io.kdmtracker.core.domain.InnovationCollection
import io.kdmtracker.core.domain.InnovationId
import io.kdmtracker.core.domain.LocalizedText
import io.kdmtracker.core.domain.LocationId
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
import io.kdmtracker.core.rules.CraftCheck
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pins the Fabricar section's model: status derivation, cost-chip resolution, the single
 * appearance of a ready build upgrade, and the header partition over the real merged catalog.
 */
class FabricarModelTest {
    private fun location(
        id: String,
        es: String,
        requires: List<LocationId> = emptyList(),
        actions: List<SettlementAction> = emptyList(),
        buildEndeavors: Int = 0,
        buildCost: Map<ResourceId, Int> = emptyMap(),
    ) = SettlementLocation(
        id = LocationId(id),
        names = LocalizedText(es = es),
        requires = requires,
        actions = actions,
        buildEndeavors = buildEndeavors,
        buildCost = buildCost,
    )

    /** The catalog the app actually ships: the 1.6 seed merged with the ingested 1.5 pack. */
    private fun shippedLocations(): List<SettlementLocation> =
        ContentCatalog(listOf(Core16Content.pack, Legacy15Content.pack)).locationDefinitions

    private fun record(unlocked: List<Innovation> = emptyList()) =
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

    private fun recipe(
        gearId: String,
        cost: Map<ResourceId, Int> = emptyMap(),
        requires: List<InnovationId> = emptyList(),
        requiresText: String? = null,
    ) = GearRecipe(GearId(gearId), resourceCost = cost, requires = requires, requiresText = requiresText)

    private fun view(
        locationName: String,
        gear: GearRecipe,
        craftable: Boolean,
        missing: String? = null,
    ) = CraftRecipeView(locationName, gear, CraftCheck(craftable, missing), gearName = gear.gearId.value)

    private val taller = location("bone_smith", "Taller de Huesos")
    private val cumulo =
        location(
            "lantern_hoard",
            "Cúmulo de Linternas",
            actions =
                listOf(
                    SettlementAction(names = LocalizedText(es = "Innovar"), kind = ActionKind.INNOVATE),
                ),
        )
    private val peleteria = location("skinnery", "Peletería")
    private val armeriaReady =
        location(
            "weapon_crafter",
            "Armería",
            requires = listOf(LocationId("bone_smith")),
            buildEndeavors = 1,
            buildCost = mapOf(ResourceId("bone.monster") to 3),
        )
    private val curtidiaPartial =
        location(
            "leather_worker",
            "Curtiduría",
            requires = listOf(LocationId("bone_smith"), LocationId("stone_circle")),
        )
    private val organGrinder =
        location(
            "organ_grinder",
            "Molino de Órganos",
            requires = listOf(LocationId("lantern_hoard")),
            buildEndeavors = 1,
        )

    private fun state(
        catalog: List<SettlementLocation>,
        built: List<String>,
        craft: List<CraftRecipeView> = emptyList(),
        storage: List<StorageLine> = emptyList(),
        resources: List<ResourceDefinition> = emptyList(),
        innovations: List<Innovation> = emptyList(),
        unlocked: List<Innovation> = emptyList(),
    ) = UiState(
        screen = Screen.Settlement,
        lanternYear = 1,
        phase = SettlementPhase.SETTLEMENT,
        settlementName = "Test",
        currentStep = 7,
        stepTitle = "Desarrollo",
        population = 4,
        endeavors = 4,
        steps = emptyList(),
        reachableSteps = emptySet(),
        lastTransition = null,
        record = record(unlocked),
        catalogLocations = catalog,
        builtLocations = built,
        craftRecipes = craft,
        storageLines = storage,
        resourceDefinitions = resources,
        innovationDefinitions = innovations,
    )

    @Test
    fun aCraftableRecipeIsCraftable() {
        val model =
            FabricarModel.of(
                state(
                    catalog = listOf(taller),
                    built = listOf("Taller de Huesos"),
                    craft = listOf(view("Taller de Huesos", recipe("daga"), craftable = true)),
                ),
            )

        val row =
            model.built
                .single()
                .recipes
                .single()
        assertEquals(RecipeStatus.CRAFTABLE, row.status)
        assertNull(row.missingInnovation)
    }

    @Test
    fun aRecipeTheLedgerCannotCoverIsOutOfStockWithoutABadge() {
        val model =
            FabricarModel.of(
                state(
                    catalog = listOf(taller),
                    built = listOf("Taller de Huesos"),
                    craft =
                        listOf(
                            view(
                                "Taller de Huesos",
                                recipe("hacha", cost = mapOf(ResourceId("bone.monster") to 2)),
                                craftable = false,
                                missing = "insufficient bone monster",
                            ),
                        ),
                ),
            )

        val row =
            model.built
                .single()
                .recipes
                .single()
        assertEquals(RecipeStatus.OUT_OF_STOCK, row.status)
        assertNull(row.missingInnovation)
    }

    @Test
    fun aRecipeMissingAnInnovationShowsTheRequiresBadgeInSpanish() {
        val amoniaco = Innovation(InnovationId("amoniaco"), LocalizedText(es = "Amoníaco"), "…")
        val model =
            FabricarModel.of(
                state(
                    catalog = listOf(taller),
                    built = listOf("Taller de Huesos"),
                    craft =
                        listOf(
                            view(
                                "Taller de Huesos",
                                recipe("pico", requires = listOf(InnovationId("amoniaco"))),
                                craftable = false,
                                missing = "Pico de Hueso requires Amoníaco",
                            ),
                        ),
                    innovations = listOf(amoniaco),
                    unlocked = emptyList(),
                ),
            )

        val row =
            model.built
                .single()
                .recipes
                .single()
        assertEquals(RecipeStatus.MISSING_INNOVATION, row.status)
        assertEquals("Amoníaco", row.missingInnovation)
    }

    @Test
    fun costChipsResolveResourceIdsToSpanishNames() {
        val model =
            FabricarModel.of(
                state(
                    catalog = listOf(taller),
                    built = listOf("Taller de Huesos"),
                    craft =
                        listOf(
                            view(
                                "Taller de Huesos",
                                recipe(
                                    "daga",
                                    cost = mapOf(ResourceId("bone.monster") to 2, ResourceId("organ.monster") to 1),
                                ),
                                craftable = true,
                            ),
                        ),
                    resources =
                        listOf(
                            ResourceDefinition(
                                ResourceId("bone.monster"),
                                LocalizedText(es = "Hueso"),
                                ResourceCategory.BASIC,
                            ),
                            ResourceDefinition(
                                ResourceId("organ.monster"),
                                LocalizedText(es = "Órgano"),
                                ResourceCategory.BASIC,
                            ),
                        ),
                ),
            )

        assertEquals(
            listOf("2 Hueso", "1 Órgano"),
            model.built
                .single()
                .recipes
                .single()
                .costChips,
        )
    }

    @Test
    fun aBuiltLocationWithoutAnActionHasNoActionButton() {
        val model = FabricarModel.of(state(catalog = listOf(taller), built = listOf("Taller de Huesos")))

        assertNull(model.built.single().actionLabel)
    }

    @Test
    fun aBuiltLocationCarriesItsActionsEndeavorCost() {
        // The approved design shows "Costo: 1 Esfuerzo" next to the built card's action; the
        // model must carry the action's endeavor cost (null when the card has no action).
        val withAction = FabricarModel.of(state(catalog = listOf(cumulo), built = listOf("Cúmulo de Linternas")))
        val withoutAction = FabricarModel.of(state(catalog = listOf(taller), built = listOf("Taller de Huesos")))

        assertEquals(1, withAction.built.single().actionEndeavorCost)
        assertNull(withoutAction.built.single().actionEndeavorCost)
    }

    @Test
    fun aBuiltLocationsActionLabelIsItsOwnAction() {
        val model =
            FabricarModel.of(state(catalog = listOf(cumulo), built = listOf("Cúmulo de Linternas")))

        assertEquals("Innovar", model.built.single().actionLabel)
    }

    @Test
    fun aReadyUpgradeShowsInsideItsFirstRequiringBuiltLocationExactlyOnce() {
        val model =
            FabricarModel.of(
                state(
                    catalog = listOf(taller, peleteria, armeriaReady),
                    built = listOf("Taller de Huesos", "Peletería"),
                    resources =
                        listOf(
                            ResourceDefinition(
                                ResourceId("bone.monster"),
                                LocalizedText(es = "Hueso"),
                                ResourceCategory.BASIC,
                            ),
                        ),
                ),
            )

        val tallerCard = model.built.single { it.name == "Taller de Huesos" }
        assertEquals(listOf("Armería"), tallerCard.upgrades.map { it.locationName })
        assertEquals(listOf("3 Hueso"), tallerCard.upgrades.single().costChips)
        assertEquals(1, tallerCard.upgrades.single().endeavors)
        assertTrue(
            model.built
                .single { it.name == "Peletería" }
                .upgrades
                .isEmpty(),
        )
        assertTrue(model.buildable.none { it.name == "Armería" })
    }

    @Test
    fun aPartiallyReadyLocationStaysInBuildableAndNeverBecomesAnUpgrade() {
        val model =
            FabricarModel.of(
                state(catalog = listOf(taller, curtidiaPartial), built = listOf("Taller de Huesos")),
            )

        assertTrue(model.built.all { it.upgrades.isEmpty() })
        assertEquals(listOf("Curtiduría"), model.buildable.map { it.name })
    }

    @Test
    fun theMergedCatalogPartitionsIntoBuiltAndPendingWithEachLocationExactlyOnce() {
        val catalog = ContentCatalog(listOf(Core16Content.pack, Legacy15Content.pack)).locationDefinitions
        val model =
            FabricarModel.of(
                state(catalog = catalog, built = listOf("Cúmulo de Linternas", "Armería")),
            )

        assertEquals(2, model.builtCount)
        assertEquals(17, model.pendingCount)
        assertEquals(19, model.builtCount + model.pendingCount)

        val pendingNames =
            catalog.filter { it.names.es !in listOf("Cúmulo de Linternas", "Armería") }.map { it.names.es }
        val displayed =
            model.buildable.map { it.name } +
                model.built.flatMap { card -> card.upgrades.map { it.locationName } }
        assertEquals(pendingNames.sorted(), displayed.sorted())
        assertEquals(displayed.size, displayed.toSet().size, "no location may appear twice")
    }

    @Test
    fun theHeaderCountsExcludeNothingFromTheCatalog() {
        val model =
            FabricarModel.of(
                state(
                    catalog = listOf(taller, peleteria, armeriaReady, curtidiaPartial),
                    built = listOf("Taller de Huesos"),
                ),
            )

        assertEquals(1, model.builtCount)
        assertEquals(3, model.pendingCount)
        assertEquals(4, model.builtCount + model.pendingCount)
    }

    @Test
    fun theFoundingLocationIsTheFoundationalCategory() {
        val model =
            FabricarModel.of(
                state(catalog = listOf(cumulo), built = listOf("Cúmulo de Linternas")),
            )

        assertEquals("FUNDACIONAL", model.built.single().category)
    }

    @Test
    fun aLocationNeedingNothingBeyondTheFoundingLocationIsPrimary() {
        val primaryOnHoard =
            location("organ_grinder", "Molino de Órganos", requires = listOf(LocationId("lantern_hoard")))
        val model =
            FabricarModel.of(
                state(
                    catalog = listOf(cumulo, taller, primaryOnHoard),
                    built = listOf("Cúmulo de Linternas", "Taller de Huesos", "Molino de Órganos"),
                ),
            )

        assertEquals("ARTESANÍA PRIMARIA", model.built.single { it.name == "Taller de Huesos" }.category)
        assertEquals("ARTESANÍA PRIMARIA", model.built.single { it.name == "Molino de Órganos" }.category)
    }

    @Test
    fun aLocationThatNeedsAnotherLocationIsDerived() {
        val model =
            FabricarModel.of(
                state(
                    catalog = listOf(taller, armeriaReady),
                    built = listOf("Taller de Huesos", "Armería"),
                ),
            )

        assertEquals("DERIVADA", model.built.single { it.name == "Armería" }.category)
    }

    @Test
    fun theLocationsTheDesignDescribesCarryItsProse() {
        val model =
            FabricarModel.of(
                state(catalog = shippedLocations(), built = listOf("Cúmulo de Linternas", "Taller de Huesos")),
            )

        val hoard = model.built.single { it.name == "Cúmulo de Linternas" }
        assertNotNull(hoard.description)
        assertTrue(hoard.description.startsWith("Un mar de linternas metálicas frías"))

        val taller = model.built.single { it.name == "Taller de Huesos" }
        assertNotNull(taller.description)
        assertTrue(taller.description.startsWith("Permite tallar armas blancas"))
    }

    @Test
    fun aLocationWithoutDesignProseRendersNoDescriptionLine() {
        val model =
            FabricarModel.of(
                state(catalog = listOf(peleteria), built = listOf("Peletería")),
            )

        assertNull(model.built.single().description)
    }

    @Test
    fun everyLocationTheSectionRendersCarriesItsProse() {
        val model = FabricarModel.of(state(catalog = shippedLocations(), built = listOf("Cúmulo de Linternas")))

        val silent =
            (model.built.map { it.name to it.description } + model.buildable.map { it.name to it.description })
                .filter { (_, description) -> description.isNullOrBlank() }
                .map { (name, _) -> name }

        assertEquals(emptyList(), silent, "every card in the section must draw its description line")
        assertEquals(19, model.builtCount + model.pendingCount)
    }
}
