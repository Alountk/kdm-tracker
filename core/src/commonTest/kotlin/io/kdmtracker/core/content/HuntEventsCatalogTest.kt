package io.kdmtracker.core.content

import io.kdmtracker.core.domain.HuntEvent
import io.kdmtracker.core.domain.HuntEventKind
import kotlin.test.Test
import kotlin.test.assertEquals

class HuntEventsCatalogTest {
    private fun pack(
        id: String,
        vararg events: HuntEvent,
    ) = ContentPack(id = id, version = "1", name = id, bundle = ContentBundle(huntEvents = events.toList()))

    private fun ok(result: CatalogResult): ContentCatalog {
        kotlin.test.assertTrue(result is CatalogResult.Ok, "expected Ok but was $result")
        return result.catalog
    }

    @Test
    fun huntEventsMergeAcrossPacksByKindAndName() {
        val a = pack("a", HuntEvent("Random", "t", HuntEventKind.BASIC))
        val b =
            pack(
                "b",
                HuntEvent("Random", "t", HuntEventKind.BASIC),
                HuntEvent("Herbs", "t", HuntEventKind.SPECIAL),
            )

        val catalog = ok(ContentCatalogComposer.compose(listOf(a, b)))

        assertEquals(2, catalog.huntEventDefinitions.size)
    }

    @Test
    fun huntEventsForIncludesBasicSpecialAndTheQuarry() {
        val catalog =
            ok(
                ContentCatalogComposer.compose(
                    listOf(
                        pack(
                            "a",
                            HuntEvent("Basic", "t", HuntEventKind.BASIC),
                            HuntEvent("Herbs", "t", HuntEventKind.SPECIAL),
                            HuntEvent("Lion", "t", HuntEventKind.MONSTER, monster = "White Lion"),
                            HuntEvent("Phoenix", "t", HuntEventKind.MONSTER, monster = "Phoenix"),
                        ),
                    ),
                ),
            )

        assertEquals(
            listOf("Basic", "Herbs", "Lion"),
            catalog.huntEventsFor("White Lion").map { it.name },
        )
    }
}
