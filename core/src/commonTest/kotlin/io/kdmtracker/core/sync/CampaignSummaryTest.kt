package io.kdmtracker.core.sync

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class CampaignSummaryTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun summary(
        role: String = "OWNER",
        ownerUsername: String = "ada",
        sharedWith: List<String> = emptyList(),
    ) = CampaignSummary(
        id = "campaign-1",
        name = "First Cradle",
        role = role,
        ownerUsername = ownerUsername,
        sharedWith = sharedWith,
    )

    @Test
    fun ownerAloneIsBadgedOwned() {
        val banner = CampaignBadges.of(summary(role = "OWNER", sharedWith = emptyList()))

        assertEquals(CampaignBadge.OWNED, banner.badge)
        assertEquals("Tuyo", banner.label)
    }

    @Test
    fun ownerSharingWithOthersIsBadgedSharedByMe() {
        val banner =
            CampaignBadges.of(
                summary(role = "OWNER", sharedWith = listOf("bob", "carol")),
            )

        assertEquals(CampaignBadge.SHARED_BY_ME, banner.badge)
        assertEquals("Compartido con: bob, carol", banner.label)
    }

    @Test
    fun nonOwnerIsBadgedSharedWithMe() {
        val banner =
            CampaignBadges.of(
                summary(role = "EDITOR", ownerUsername = "ada", sharedWith = listOf("me")),
            )

        assertEquals(CampaignBadge.SHARED_WITH_ME, banner.badge)
        assertEquals("Compartido con vos por ada", banner.label)
    }

    @Test
    fun viewerIsAlsoBadgedSharedWithMe() {
        val banner =
            CampaignBadges.of(
                summary(role = "VIEWER", ownerUsername = "ada", sharedWith = emptyList()),
            )

        assertEquals(CampaignBadge.SHARED_WITH_ME, banner.badge)
        assertEquals("Compartido con vos por ada", banner.label)
    }

    @Test
    fun campaignSummaryRoundTripsThroughJson() {
        val original =
            summary(
                role = "OWNER",
                ownerUsername = "ada",
                sharedWith = listOf("bob", "carol"),
            )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<CampaignSummary>(encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun campaignSummaryDeserializesFromServerShape() {
        val payload =
            """
            {
              "id": "campaign-1",
              "name": "First Cradle",
              "role": "EDITOR",
              "ownerUsername": "ada",
              "sharedWith": ["bob"]
            }
            """.trimIndent()

        val decoded = json.decodeFromString<CampaignSummary>(payload)

        assertEquals("campaign-1", decoded.id)
        assertEquals("First Cradle", decoded.name)
        assertEquals("EDITOR", decoded.role)
        assertEquals("ada", decoded.ownerUsername)
        assertEquals(listOf("bob"), decoded.sharedWith)
    }
}
