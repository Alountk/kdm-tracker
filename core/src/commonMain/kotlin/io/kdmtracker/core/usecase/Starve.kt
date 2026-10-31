package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.Storage
import io.kdmtracker.core.rules.StepResult
import kotlin.random.Random

/**
 * Starvation (rulebook "Fase de Caza", pp. 61-63): a hunt that fails or ends on the starvation space
 * costs the settlement a d5 of its stored resources.
 *
 * The use case only records what is owed; the player chooses what to discard through
 * [DiscardForStarvation]. The debt never exceeds what the storage still holds, so a poor storage
 * owes less than the roll.
 */
object Starve {
    private const val SIDES = 5

    /** One d5: 1 to 5. */
    fun roll(random: Random = Random.Default): Int = random.nextInt(1, SIDES + 1)

    /**
     * Returns [StepResult.Ok] with [roll] added to the owed amount, capped by the stored resources not
     * already owed, or [StepResult.Blocked] with `"invalid starvation roll"` when [roll] is not 1 to 5.
     */
    fun run(
        record: SettlementRecord,
        roll: Int,
    ): StepResult<SettlementRecord> {
        if (roll !in 1..SIDES) {
            return StepResult.Blocked("invalid starvation roll")
        }
        val owed = record.state.starvationOwed
        val room = (Storage.total(record.state.resourceEntries) - owed).coerceAtLeast(0)
        return StepResult.Ok(
            record.copy(state = record.state.copy(starvationOwed = owed + minOf(roll, room))),
        )
    }
}
