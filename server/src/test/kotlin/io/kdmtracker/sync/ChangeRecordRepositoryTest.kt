package io.kdmtracker.sync

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.dao.DataIntegrityViolationException

@DataJpaTest
class ChangeRecordRepositoryTest {
    @Autowired
    private lateinit var repository: ChangeRecordRepository

    private fun record(
        campaignId: String = "campaign-1",
        entityType: String = "settlement",
        entityId: String = "settlement-1",
        op: String = "UPSERT",
        rev: Long = 1,
    ) = ChangeRecordEntity(
        campaignId = campaignId,
        entityType = entityType,
        entityId = entityId,
        op = op,
        payload = """{"name":"A"}""",
        updatedAtMillis = 1_700_000_000_000,
        updatedAtCounter = 3,
        baseRev = null,
        rev = rev,
    )

    @Test
    fun savesAndReadsBackARecord() {
        val saved = repository.saveAndFlush(record())
        assertNotNull(saved.id)

        val found = repository.findById(saved.id!!).orElseThrow()
        assertEquals("campaign-1", found.campaignId)
        assertEquals("settlement", found.entityType)
        assertEquals("settlement-1", found.entityId)
        assertEquals("UPSERT", found.op)
        assertEquals("""{"name":"A"}""", found.payload)
        assertEquals(1_700_000_000_000, found.updatedAtMillis)
        assertEquals(3, found.updatedAtCounter)
        assertNull(found.baseRev)
        assertEquals(1, found.rev)

        val byTriple = repository.findByCampaignIdAndEntityTypeAndEntityId("campaign-1", "settlement", "settlement-1")
        assertEquals(saved.id, byTriple?.id)
    }

    @Test
    fun findsRecordsWithRevGreaterThanSinceOrderedByRev() {
        repository.saveAll(
            listOf(
                record(entityId = "settlement-3", rev = 3),
                record(entityId = "settlement-1", rev = 1),
                record(entityId = "settlement-2", rev = 2),
            ),
        )
        repository.flush()

        val delta = repository.findByCampaignIdAndRevGreaterThanOrderByRevAsc("campaign-1", 1)
        assertEquals(listOf(2L, 3L), delta.map { it.rev })
    }

    @Test
    fun maxRevReturnsHighestRevForTheCampaignAndNullWhenEmpty() {
        repository.saveAll(
            listOf(
                record(entityId = "settlement-1", rev = 5),
                record(entityId = "settlement-2", rev = 9),
                record(campaignId = "campaign-2", entityId = "settlement-9", rev = 2),
            ),
        )
        repository.flush()

        assertEquals(9L, repository.maxRev("campaign-1"))
        assertEquals(2L, repository.maxRev("campaign-2"))
        assertNull(repository.maxRev("campaign-empty"))
    }

    @Test
    fun uniqueCampaignEntityTripleIsEnforced() {
        repository.saveAndFlush(record(rev = 1))
        repository.save(record(rev = 2))

        assertThrows(DataIntegrityViolationException::class.java) {
            repository.flush()
        }
    }
}
