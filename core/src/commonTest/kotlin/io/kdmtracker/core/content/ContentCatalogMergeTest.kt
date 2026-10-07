package io.kdmtracker.core.content

import io.kdmtracker.core.domain.SettlementAction
import io.kdmtracker.core.domain.SettlementLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContentCatalogMergeTest {
    private fun pack(
        id: String,
        locations: List<SettlementLocation>,
    ) = ContentPack(
        id = id,
        version = "1.0.0",
        name = id,
        bundle = ContentBundle(locations = locations),
    )

    private fun ok(result: CatalogResult): ContentCatalog {
        assertTrue(result is CatalogResult.Ok, "expected Ok but was $result")
        return result.catalog
    }

    @Test
    fun modeledLocationWinsOverBareRegardlessOfPackOrder() {
        val bare = SettlementLocation("Armería")
        val modeled = SettlementLocation("Armería", buildCost = mapOf("Bone" to 3))

        val forward =
            ok(ContentCatalogComposer.compose(listOf(pack("bare", listOf(bare)), pack("modeled", listOf(modeled)))))
        val reversed =
            ok(ContentCatalogComposer.compose(listOf(pack("modeled", listOf(modeled)), pack("bare", listOf(bare)))))

        assertEquals(mapOf("Bone" to 3), forward.locationDefinitions.single { it.name == "Armería" }.buildCost)
        assertEquals(mapOf("Bone" to 3), reversed.locationDefinitions.single { it.name == "Armería" }.buildCost)
    }

    @Test
    fun modeledActionsAlsoWinOverBare() {
        val bare = SettlementLocation("Armería")
        val modeled =
            SettlementLocation(
                "Armería",
                actions = listOf(SettlementAction(name = "Build")),
            )

        val catalog =
            ok(ContentCatalogComposer.compose(listOf(pack("bare", listOf(bare)), pack("modeled", listOf(modeled)))))

        assertTrue(
            catalog.locationDefinitions
                .single { it.name == "Armería" }
                .actions
                .isNotEmpty(),
        )
    }

    @Test
    fun bareOverlapKeepsFirstOccurrenceByPackOrder() {
        val first = SettlementLocation("Armería", buildEndeavors = 1)
        val second = SettlementLocation("Armería", buildEndeavors = 2)

        val forward = ok(ContentCatalogComposer.compose(listOf(pack("a", listOf(first)), pack("b", listOf(second)))))
        val reversed = ok(ContentCatalogComposer.compose(listOf(pack("b", listOf(second)), pack("a", listOf(first)))))

        assertEquals(1, forward.locationDefinitions.single { it.name == "Armería" }.buildEndeavors)
        assertEquals(2, reversed.locationDefinitions.single { it.name == "Armería" }.buildEndeavors)
    }
}
