package io.kdmtracker.sync

import io.kdmtracker.auth.CampaignMemberRepository
import io.kdmtracker.auth.CampaignRepository
import io.kdmtracker.auth.UserRepository
import io.kdmtracker.core.domain.Ulid
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import kotlin.test.assertEquals

/**
 * Proves the dev seeder is idempotent and produces the exact campaign id the Android app seeds, so
 * the demo sign-in + sync works without manual setup.
 */
@SpringBootTest
@ActiveProfiles("dev")
class DevDataSeederTest {
    @Autowired private lateinit var seeder: DevDataSeeder

    @Autowired private lateinit var users: UserRepository

    @Autowired private lateinit var campaigns: CampaignRepository

    @Autowired private lateinit var members: CampaignMemberRepository

    @Test
    fun `seeds the demo user campaign and membership idempotently`() {
        val expectedCampaignId = Ulid.generate(0L) { 0 }.value

        // The context startup already ran the seeder once; run it again to prove idempotency.
        seeder.run(org.springframework.boot.DefaultApplicationArguments())

        val user = users.findByUsername(DevDataSeeder.DEV_USERNAME)
        requireNotNull(user) { "demo user should exist" }
        val campaign = campaigns.findById(expectedCampaignId).orElse(null)
        requireNotNull(campaign) { "demo campaign with the app's id should exist" }
        assertEquals(1, members.findByUserId(user.id!!).size)
    }
}
