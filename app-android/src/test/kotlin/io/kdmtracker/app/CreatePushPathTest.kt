package io.kdmtracker.app

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.sync.CampaignSummary
import io.kdmtracker.data.SqlDelightCampaignStore
import io.kdmtracker.data.db.KdmDatabase
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pins the create → push path over the **real** [SqlDelightCampaignStore] (Ruling AT).
 *
 * The in-memory fake store has no outbox, so the existing repository tests cannot see that `create`
 * enqueues the new campaign and its settlement for sync. This test uses a temp-file JDBC database so
 * the store's outbox is exercised for real, and asserts both records carry the server-issued id.
 */
class CreatePushPathTest {
    private val databaseFile: File = File.createTempFile("kdm-create-push", ".db").apply { deleteOnExit() }
    private val driver: JdbcSqliteDriver = JdbcSqliteDriver("jdbc:sqlite:${databaseFile.absolutePath}")

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun createEnqueuesTheCampaignAndSettlementOutboxForTheServerId() {
        KdmDatabase.Schema.create(driver)
        val store = SqlDelightCampaignStore(KdmDatabase(driver))
        val serverId = Ulid.generate(1_000L) { 5 }.value
        val repository =
            SettlementRepository(
                store = store,
                createCampaign = { name -> CampaignSummary(serverId, name, "OWNER", "", emptyList()) },
            )

        val card = repository.create("Gloom")

        assertEquals(serverId, card.campaignId, "the card must use the server-issued id")
        val pending = store.pending(serverId)
        assertEquals(2, pending.size, "create must enqueue the campaign and its settlement")
        assertEquals(
            setOf("campaign", "settlement"),
            pending.map { it.entityType }.toSet(),
            "the outbox must hold one campaign row and one settlement row",
        )
        assertTrue(pending.all { it.campaignId == serverId }, "both records must be scoped to the server id")
        assertEquals(
            serverId,
            pending.single { it.entityType == "campaign" }.entityId,
            "the campaign outbox row must use the server-issued id",
        )
        assertEquals(
            repository.settlement.id.value,
            pending.single { it.entityType == "settlement" }.entityId,
            "the settlement outbox row must use the created settlement's id",
        )
    }
}
