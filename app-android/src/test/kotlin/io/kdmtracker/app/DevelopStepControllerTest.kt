package io.kdmtracker.app

import io.kdmtracker.core.content.ContentBundle
import io.kdmtracker.core.content.ContentCatalog
import io.kdmtracker.core.content.ContentPack
import io.kdmtracker.core.content.resourceDefinition
import io.kdmtracker.core.domain.ActionKind
import io.kdmtracker.core.domain.Innovation
import io.kdmtracker.core.domain.InnovationId
import io.kdmtracker.core.domain.LocalizedText
import io.kdmtracker.core.domain.LocationId
import io.kdmtracker.core.domain.LocationSet
import io.kdmtracker.core.domain.ResourceCategory
import io.kdmtracker.core.domain.ResourceEntry
import io.kdmtracker.core.domain.ResourceId
import io.kdmtracker.core.domain.SettlementAction
import io.kdmtracker.core.domain.SettlementLocation
import io.kdmtracker.core.persistence.InMemoryCampaignStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pins the controller's Step 7 (Develop) actions: Innovate, Build, spend one Endeavor and append a
 * resource entry. Each action delegates to the matching `:core` use-case, persists an `Ok` result
 * through [SettlementRepository.update], and surfaces a `Blocked` reason without changing the
 * record.
 *
 * The scenarios are JVM-only: the controller is handed a composed [ContentCatalog] whose Lantern
 * Hoard grants the Innovate action, so the use-cases exercise real costs and prerequisites over the
 * held [io.kdmtracker.core.domain.SettlementRecord].
 */
class DevelopStepControllerTest {
    private val innovateAction =
        SettlementAction(
            names = LocalizedText(es = "Innovar", en = "Innovate"),
            kind = ActionKind.INNOVATE,
            endeavorCost = 1,
            resourceCost = mapOf(ResourceId("Bone") to 1, ResourceId("Organ") to 1, ResourceId("Hide") to 1),
            oncePerPhase = true,
            draw = 2,
            keep = 1,
            effect = "Draw 2 innovation cards, keep 1.",
        )

    private val ammonia =
        Innovation(
            id = InnovationId("ammonia"),
            names = LocalizedText(es = "Ammonia"),
            description = "Acids for tanning.",
        )

    private val catalog =
        ContentCatalog(
            listOf(
                ContentPack(
                    id = "test",
                    version = "1.0.0",
                    name = "Test",
                    bundle =
                        ContentBundle(
                            innovations = listOf(ammonia),
                            locations =
                                listOf(
                                    SettlementLocation(
                                        LocationId("lantern_hoard"),
                                        LocalizedText(es = "Lantern Hoard"),
                                        actions = listOf(innovateAction),
                                    ),
                                    SettlementLocation(
                                        LocationId("bone_smith"),
                                        LocalizedText(es = "Bone Smith"),
                                        requires = listOf(LocationId("lantern_hoard")),
                                    ),
                                    SettlementLocation(
                                        LocationId("weapon_crafter"),
                                        LocalizedText(es = "Weapon Crafter"),
                                        requires = listOf(LocationId("bone_smith")),
                                    ),
                                    SettlementLocation(
                                        LocationId("skinnery"),
                                        LocalizedText(es = "Skinnery"),
                                        buildEndeavors = 1,
                                        buildCost = mapOf(ResourceId("Bone") to 1),
                                    ),
                                ),
                            resources =
                                listOf(
                                    resourceDefinition("Bone", ResourceCategory.BASIC),
                                    resourceDefinition("Organ", ResourceCategory.BASIC),
                                    resourceDefinition("Hide", ResourceCategory.BASIC),
                                ),
                        ),
                ),
            ),
        )

    private fun storeWith(
        endeavors: Int,
        bone: Int,
        organ: Int,
        hide: Int,
    ): InMemoryCampaignStore {
        val store = InMemoryCampaignStore()
        val repository = SettlementRepository(store)
        repository.update(
            repository.record.copy(
                settlement = repository.settlement.copy(endeavors = endeavors),
                state =
                    repository.record.state.copy(
                        resourceEntries =
                            listOf(
                                ResourceEntry(ResourceId("Bone"), bone),
                                ResourceEntry(ResourceId("Organ"), organ),
                                ResourceEntry(ResourceId("Hide"), hide),
                            ),
                    ),
            ),
        )
        return store
    }

    private fun controller(store: InMemoryCampaignStore): LanternYearController =
        LanternYearController(SettlementRepository(store), catalog = catalog)

    @Test
    fun innovateSpendsTheCostAndMarksThePhase() {
        val controller = controller(storeWith(endeavors = 2, bone = 2, organ = 2, hide = 2))

        controller.innovate("Ammonia")

        val record = controller.state.value.record
        assertNotNull(record)
        assertEquals(1, record.settlement.endeavors)
        assertTrue(record.state.usedInPhase("lantern_hoard"))
        assertTrue(
            record.state.innovations.innovations
                .any { it.names.es == "Ammonia" },
        )
        assertEquals(1, record.storage().single { it.id.value == "Bone" }.quantity)
        assertNull(controller.state.value.actionError)
    }

    @Test
    fun aSecondInnovateIsBlockedWithAReasonAndChangesNothing() {
        val controller = controller(storeWith(endeavors = 2, bone = 2, organ = 2, hide = 2))
        controller.innovate("Ammonia")
        val afterFirst = controller.state.value.record

        controller.innovate("Ammonia")

        assertEquals(afterFirst, controller.state.value.record)
        val reason = controller.state.value.actionError
        assertNotNull(reason)
        assertTrue(reason.contains("already used"), "the block reason names the once-per-phase limit: $reason")
    }

    @Test
    fun buildBlocksWhenPrerequisitesAreUnmetAndChangesNothing() {
        val store = storeWith(endeavors = 2, bone = 2, organ = 2, hide = 2)
        // The seed builds Lantern Hoard and Armería (= Weapon Crafter); with the two names now
        // one id, the unmet-prerequisite scenario needs the built set cleared first.
        SettlementRepository(store).update(
            SettlementRepository(store).record.let { record ->
                record.copy(state = record.state.copy(locations = LocationSet()))
            },
        )
        val controller = controller(store)

        controller.build("Weapon Crafter")

        assertNotNull(controller.state.value.actionError)
        val record = controller.state.value.record
        assertNotNull(record)
        assertFalse(LocationId("weapon_crafter") in record.state.locations.built)
        assertFalse(
            LocationId("weapon_crafter") in
                SettlementRepository(store)
                    .record.state.locations.built,
        )
    }

    @Test
    fun aValidBuildPersistsAndAFreshRepositoryReadsTheChange() {
        val store = storeWith(endeavors = 2, bone = 2, organ = 2, hide = 2)
        val controller = controller(store)

        controller.build("Skinnery")

        assertNull(controller.state.value.actionError)
        val built = controller.state.value.record
        assertNotNull(built)
        assertTrue(LocationId("skinnery") in built.state.locations.built)
        assertEquals(1, built.settlement.endeavors)

        val fresh = SettlementRepository(store).record
        assertTrue(LocationId("skinnery") in fresh.state.locations.built)
        assertEquals(1, fresh.settlement.endeavors)
        assertEquals(1, fresh.storage().single { it.id.value == "Bone" }.quantity)
    }

    @Test
    fun aSpendWithInsufficientStorageIsBlocked() {
        val controller = controller(storeWith(endeavors = 2, bone = 1, organ = 0, hide = 0))
        val before = controller.state.value.record

        controller.addResource(ResourceEntry(ResourceId("Bone"), -5))

        val reason = controller.state.value.actionError
        assertNotNull(reason)
        assertTrue(reason.contains("Bone"), "the block reason names the overdrawn resource: $reason")
        assertEquals(before, controller.state.value.record)
    }

    @Test
    fun spendEndeavorSpendsOneAndPersists() {
        val store = storeWith(endeavors = 2, bone = 0, organ = 0, hide = 0)
        val controller = controller(store)

        controller.spendEndeavor()

        assertNull(controller.state.value.actionError)
        assertEquals(
            1,
            controller.state.value.record
                ?.settlement
                ?.endeavors,
        )
        assertEquals(1, SettlementRepository(store).record.settlement.endeavors)
    }

    @Test
    fun everyOkPersistsToAFreshRepository() {
        val store = storeWith(endeavors = 3, bone = 3, organ = 3, hide = 3)
        val controller = controller(store)

        controller.innovate("Ammonia")
        controller.build("Skinnery")
        controller.spendEndeavor()
        controller.addResource(ResourceEntry(ResourceId("Bone"), 2))

        assertNull(controller.state.value.actionError)
        assertEquals(controller.state.value.record, SettlementRepository(store).record)
    }
}
