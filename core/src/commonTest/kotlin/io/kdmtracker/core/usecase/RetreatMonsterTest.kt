package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.HybridLogicalClock
import io.kdmtracker.core.domain.Settlement
import io.kdmtracker.core.domain.SettlementPhase
import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.SyncMeta
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.rules.StepResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * S8: a hunt event can make the beast step one space *away* from the party (e.g. `Asustadizo`:
 * "el Antílope Bramador se mueve 1 espacio alejándose de los supervivientes"). This is the
 * counterpart of [MoveMonster], which only steps the beast *toward* the party.
 */
class RetreatMonsterTest {
    private fun started(): SettlementRecord {
        val record =
            SettlementRecord(
                settlement =
                    Settlement(
                        id = Ulid.generate(95L) { 0 },
                        campaignId = Ulid.generate(96L) { 0 },
                        name = "Kingsport",
                        lanternYear = 1,
                        phase = SettlementPhase.HUNT,
                        currentStep = 1,
                        endeavors = 0,
                        population = 4,
                        settlementEvent = null,
                        sync = SyncMeta(HybridLogicalClock(0, 0)),
                    ),
            )
        val setup = SetUpHunt.run(record, "Screaming Antelope", level = 1, players = 1)
        return assertIs<StepResult.Ok<SettlementRecord>>(setup).value
    }

    @Test
    fun retreatMonsterStepsOneSpaceAwayFromTheParty() {
        // beast starts at index 4 (Antelope L1), party at 0 -> retreat to 3
        val before = started().state.hunt.monsterPosition
        val moved = assertIs<StepResult.Ok<SettlementRecord>>(RetreatMonster.run(started())).value
        assertEquals(before - 1, moved.state.hunt.monsterPosition)
    }

    @Test
    fun retreatMonsterNeverGoesBelowTheStartSpace() {
        // walk the beast down to index 0, then a further retreat is a no-op (stays at 0)
        var record = started()
        while (record.state.hunt.monsterPosition > 0) {
            record = assertIs<StepResult.Ok<SettlementRecord>>(RetreatMonster.run(record)).value
        }
        assertEquals(0, record.state.hunt.monsterPosition)
        // another retreat keeps it at the Start space
        val again = assertIs<StepResult.Ok<SettlementRecord>>(RetreatMonster.run(record)).value
        assertEquals(0, again.state.hunt.monsterPosition)
    }

    @Test
    fun retreatMonsterBlocksBeforeSetup() {
        val empty =
            SettlementRecord(
                settlement =
                    Settlement(
                        id = Ulid.generate(97L) { 0 },
                        campaignId = Ulid.generate(98L) { 0 },
                        name = "Kingsport",
                        lanternYear = 1,
                        phase = SettlementPhase.HUNT,
                        currentStep = 1,
                        endeavors = 0,
                        population = 4,
                        settlementEvent = null,
                        sync = SyncMeta(HybridLogicalClock(0, 0)),
                    ),
            )
        assertIs<StepResult.Blocked>(RetreatMonster.run(empty))
    }
}
