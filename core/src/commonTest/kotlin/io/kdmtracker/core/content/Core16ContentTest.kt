package io.kdmtracker.core.content

import io.kdmtracker.core.domain.Campaign
import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.SyncMeta
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.ruleset.RulesetOverrides
import io.kdmtracker.core.transfer.TransferMapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Core16ContentTest {
    private fun catalog(): ContentCatalog {
        val result = ContentCatalogComposer.compose(listOf(Core16Content.pack))
        assertTrue(result is CatalogResult.Ok, "expected Ok but was $result")
        return result.catalog
    }

    @Test
    fun packComposesAndIsTheCoreEdition() {
        val pack = Core16Content.pack

        assertEquals("core-1.6", pack.id)
        assertTrue(pack.requires.isEmpty(), "the core edition must not require another pack")
        assertTrue(ContentCatalogComposer.compose(listOf(pack)) is CatalogResult.Ok)
    }

    @Test
    fun packProvidesTheSeedLocations() {
        val locations = catalog().locations
        val expected =
            listOf(
                "Lantern Hoard",
                "Bone Smith",
                "Skinnery",
                "Organ Grinder",
                "Weapon Crafter",
                "Leather Worker",
                "Stone Circle",
                "Exhausted Lantern Hoard",
                "Barber Surgeon",
                "Blacksmith",
                "Catarium",
                "Dragon Armory",
                "Gormchymist",
                "Gormery",
                "Mask Maker",
                "Plumery",
                "Silk Mill",
                "Skyreef Sanctuary",
                "Wet Resin Crafter",
            )

        assertTrue(locations.containsAll(expected), "missing: ${expected - locations}")
    }

    @Test
    fun packProvidesTheSeedInnovations() {
        assertTrue("Language" in catalog().innovations)
    }

    @Test
    fun packProvidesTheSeedResources() {
        val resources = catalog().resources

        assertTrue(resources.containsAll(listOf("Bone", "Hide", "Organ", "Scrap")), "was $resources")
    }

    @Test
    fun packProvidesTheSeedQuarriesAndNemeses() {
        val catalog = catalog()

        assertTrue("White Lion" in catalog.quarries, "was ${catalog.quarries}")
        assertTrue(
            catalog.nemeses.containsAll(listOf("The Butcher", "King's Man")),
            "was ${catalog.nemeses}",
        )
    }

    @Test
    fun campaignDefaultHasNoEnabledPacks() {
        val campaign =
            Campaign(
                id = Ulid.generate(1) { 0 },
                name = "Grey Havens",
                rulesetId = "core-1.6",
                rulesetOverrides = RulesetOverrides(),
                settlementIds = emptyList(),
                sync = SyncMeta(HybridLogicalClock(0, 0), rev = 0),
            )

        assertTrue(campaign.enabledPacks.isEmpty())
    }

    @Test
    fun campaignWithEnabledPacksRoundTripsThroughTransferMapper() {
        val campaign =
            Campaign(
                id = Ulid.generate(1) { 0 },
                name = "Grey Havens",
                rulesetId = "core-1.6",
                rulesetOverrides = RulesetOverrides(),
                settlementIds = emptyList(),
                sync = SyncMeta(HybridLogicalClock(0, 0), rev = 0),
                enabledPacks = listOf("core-1.6", "gorm"),
            )

        val (roundTripped, _) = TransferMapper.toDomain(TransferMapper.toDto(campaign, emptyList()))

        assertEquals(listOf("core-1.6", "gorm"), roundTripped.enabledPacks)
        assertEquals(campaign, roundTripped)
    }
}
