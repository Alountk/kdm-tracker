package io.kdmtracker.core.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PackRegistryTest {
    private val base = ContentPack(id = "base", version = "1", name = "Base")
    private val core = ContentPack(id = "core", version = "1", name = "Core")
    private val expansion = ContentPack(id = "gorm", version = "1", name = "Gorm", requires = listOf("core"))
    private val other = ContentPack(id = "other", version = "1", name = "Other", requires = listOf("gorm"))

    private val registry =
        PackRegistry(
            listOf(
                RegisteredPack(base, base = true),
                RegisteredPack(core, base = true),
                RegisteredPack(expansion),
                RegisteredPack(other),
            ),
        )

    private fun enabled(vararg chosen: String): List<String> {
        val selection = registry.select(chosen.toList())
        assertTrue(selection is PackSelection.Ok, "expected Ok but was $selection")
        return selection.enabled
    }

    @Test
    fun theShippedRegistryCarriesTheTwoCorePacksAsBaseAndNoExpansion() {
        val shipped = PackRegistry.shipped

        assertEquals(listOf(Core16Content.pack.id, Legacy15Content.pack.id), shipped.baseIds)
        assertTrue(shipped.optional.isEmpty())
    }

    @Test
    fun theShippedBasePacksComposeIntoACatalog() {
        val selection = PackRegistry.shipped.select(emptyList())

        assertTrue(selection is PackSelection.Ok)
        val packs = selection.enabled.map { checkNotNull(PackRegistry.shipped.find(it)) }
        assertTrue(ContentCatalogComposer.compose(packs) is CatalogResult.Ok)
    }

    @Test
    fun aCampaignWithNoChoiceEnablesTheBasePacksInRegistryOrder() {
        assertEquals(listOf("base", "core"), enabled())
    }

    @Test
    fun aChosenExpansionFollowsTheBasePacks() {
        assertEquals(listOf("base", "core", "gorm"), enabled("gorm"))
    }

    @Test
    fun theResultFollowsTheRegistryOrderWhateverTheOrderOfTheChoice() {
        assertEquals(listOf("base", "core", "gorm", "other"), enabled("other", "gorm"))
    }

    @Test
    fun choosingABasePackOrChoosingTwiceNeverDuplicatesIt() {
        assertEquals(listOf("base", "core", "gorm"), enabled("core", "gorm", "gorm"))
    }

    @Test
    fun anUnknownPackIsRefused() {
        assertEquals(PackSelection.Blocked("unknown content pack 'nope'"), registry.select(listOf("nope")))
    }

    @Test
    fun anExpansionWhoseRequirementIsNotEnabledIsRefused() {
        assertEquals(
            PackSelection.Blocked("content pack 'other' requires 'gorm'"),
            registry.select(listOf("other")),
        )
    }

    private fun changed(
        current: List<String>,
        vararg wanted: String,
    ): List<String> {
        val change = registry.change(current, wanted.toList())
        assertTrue(change is PackSelection.Ok, "expected Ok but was $change")
        return change.enabled
    }

    @Test
    fun anExpansionCanBeEnabledOnACampaignThatLacksIt() {
        assertEquals(listOf("base", "core", "gorm"), changed(listOf("base", "core"), "base", "core", "gorm"))
    }

    @Test
    fun keepingTheSamePacksChangesNothing() {
        assertEquals(listOf("base", "core", "gorm"), changed(listOf("base", "core", "gorm"), "gorm", "core", "base"))
    }

    @Test
    fun aPackTheCampaignAlreadyHasCannotBeDisabled() {
        assertEquals(
            PackSelection.Blocked("content pack 'gorm' is already enabled and cannot be disabled"),
            registry.change(listOf("base", "core", "gorm"), listOf("base", "core")),
        )
    }

    @Test
    fun aBasePackCannotBeDisabledEither() {
        assertEquals(
            PackSelection.Blocked("content pack 'base' is already enabled and cannot be disabled"),
            registry.change(listOf("base", "core"), listOf("core")),
        )
    }

    @Test
    fun aPackThisBuildDoesNotKnowIsKeptWhenAnotherIsEnabled() {
        assertEquals(
            listOf("base", "core", "future", "gorm"),
            changed(listOf("base", "core", "future"), "base", "core", "future", "gorm"),
        )
    }

    @Test
    fun enablingAnUnknownPackIsRefused() {
        assertEquals(
            PackSelection.Blocked("unknown content pack 'nope'"),
            registry.change(listOf("base", "core"), listOf("base", "core", "nope")),
        )
    }

    @Test
    fun enablingAnExpansionWhoseRequirementIsMissingIsRefused() {
        assertEquals(
            PackSelection.Blocked("content pack 'other' requires 'gorm'"),
            registry.change(listOf("base", "core"), listOf("base", "core", "other")),
        )
    }

    @Test
    fun enablingAnExpansionAndItsRequirementTogetherIsAllowed() {
        assertEquals(
            listOf("base", "core", "gorm", "other"),
            changed(listOf("base", "core"), "base", "core", "other", "gorm"),
        )
    }

    @Test
    fun theOptionalPacksAreTheOnesACampaignMayLeaveOut() {
        assertEquals(listOf(expansion, other), registry.optional)
        assertEquals(expansion, registry.find("gorm"))
        assertEquals(null, registry.find("nope"))
    }
}
