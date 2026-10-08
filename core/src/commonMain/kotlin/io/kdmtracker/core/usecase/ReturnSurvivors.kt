package io.kdmtracker.core.usecase

import io.kdmtracker.core.domain.SettlementRecord
import io.kdmtracker.core.domain.SurvivorStatus

/**
 * Resolves the returning survivors (Settlement Phase step 2).
 *
 * Every living survivor's light (`herida firme`) and heavy (`herida grave`) injury boxes reset and
 * their tokens clear; armor is left as-is and survival/insanity are **not** refilled. Everything
 * else — attributes (permanent injuries and improvements), disorders, fighting arts, XP, courage
 * and understanding — persists, and is corrected through the Survivor Editor, never here.
 */
object ReturnSurvivors {
    /** Returns [record] with every living survivor healed and de-tokened. */
    fun run(record: SettlementRecord): SettlementRecord =
        record.withSurvivors(
            record.state.survivors.map { survivor ->
                if (survivor.status == SurvivorStatus.DEAD) {
                    survivor
                } else {
                    survivor.copy(
                        wounds = survivor.wounds.mapValues { (_, wound) -> wound.copy(light = false, heavy = false) },
                        tokens = emptySet(),
                    )
                }
            },
        )
}
