package io.kdmtracker.app.ui

import io.kdmtracker.core.domain.Survivor
import io.kdmtracker.core.domain.SurvivorStatus
import io.kdmtracker.core.domain.Ulid
import io.kdmtracker.core.usecase.AssignGear

/**
 * The pure derivations for the "Supervivientes que Parten" body. The body renders; this file
 * rules. The hunt-party limit: the current rule, `population` capped at four.
 */
fun departingLimit(population: Int): Int = population.coerceAtMost(4)

/** The departing slot list in roster order — only `DEPARTING` survivors hold a slot. */
fun departingIds(survivors: List<Survivor>): List<Ulid> =
    survivors.filter { it.status == SurvivorStatus.DEPARTING }.map { it.id }

/**
 * The swap picker's candidates: the alive survivors only. Dead survivors never appear anywhere in
 * this body, and a survivor already holding a slot is not a candidate for another slot.
 */
fun swapPickerCandidates(survivors: List<Survivor>): List<Survivor> =
    survivors.filter { it.status == SurvivorStatus.ALIVE }

/** Swap in place: [replacement] takes [slot]'s position and every other slot keeps its order. */
fun swapDepartingSlot(
    departing: List<Ulid>,
    slot: Int,
    replacement: Ulid,
): List<Ulid> = departing.mapIndexed { index, id -> if (index == slot) replacement else id }

/** Add: the picked survivor takes the next empty slot, appended after the current ones. */
fun addToDeparting(
    departing: List<Ulid>,
    addition: Ulid,
): List<Ulid> = departing + addition

/** How many `Sin asignar` cards to draw: the party filled up to [limit], never negative. */
fun emptySlotCount(
    limit: Int,
    departingCount: Int,
): Int = (limit - departingCount).coerceAtLeast(0)

/** The gear strip's count label: the assigned gear counted against the 3×3 grid's nine slots. */
fun gearGridLabel(gear: List<String>): String = "Cuadrícula ${gear.size}/${AssignGear.MAX_GEAR_SLOTS}"

/**
 * The quarry card's `Vida / IA` stat box — a **verbatim inert design value**. There is no
 * monster-stat model (`MonsterCard` carries no Vida/Dureza fields) → backlog.
 */
const val QUARRY_LIFE_AI = 8

/**
 * The quarry card's `Dureza` stat box — a **verbatim inert design value**. No monster-stat model
 * exists → backlog (same as [QUARRY_LIFE_AI]).
 */
const val QUARRY_TOUGHNESS = 8
