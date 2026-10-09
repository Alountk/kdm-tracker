package io.kdmtracker.app.ui

import io.kdmtracker.core.domain.HitLocation
import io.kdmtracker.core.domain.Survivor
import io.kdmtracker.core.domain.isInsane
import io.kdmtracker.core.usecase.AssignGear

/** Whether a survivor's insanity has reached the ruleset's threshold. */
enum class MentalState { SANE, INSANE }

/** One primary attribute as the sheet shows it: the design's abbreviation and the base value. */
data class AttributeRow(
    val label: String,
    val value: Int,
)

/** One hit location's armour and its two injury boxes. */
data class HitLocationRowModel(
    val location: HitLocation,
    val label: String,
    val armor: Int,
    val light: Boolean,
    val heavy: Boolean,
)

/** One survival token chip. */
data class TokenChip(
    val token: String,
    val label: String,
    val selected: Boolean,
)

/**
 * The survivor sheet's display values, derived once from a survivor and the ruleset scalars.
 *
 * Pure: it takes the numbers it needs rather than a `Ruleset`, so tests build one directly. Labels
 * are the design's Spanish copy.
 */
data class SurvivorSheetModel(
    val name: String,
    val initial: String,
    val huntXp: Int,
    val huntXpMax: Int,
    val survival: Int,
    val survivalLimit: Int,
    val canSpendSurvival: Boolean,
    val insanity: Int,
    val mentalState: MentalState,
    val attributes: List<AttributeRow>,
    val hitLocations: List<HitLocationRowModel>,
    val tokenChips: List<TokenChip>,
    val gearSlots: Int,
) {
    companion object {
        /** The three tokens the design's chips offer, in the design's order. */
        private val DESIGN_TOKENS = listOf("Esquivar", "Sprint", "Forzar")

        /** The design's hit-location order (Cabeza, Brazos, Torso, Cintura, Piernas). */
        private val HIT_LOCATION_ORDER =
            listOf(HitLocation.HEAD, HitLocation.ARMS, HitLocation.TORSO, HitLocation.WAIST, HitLocation.LEGS)

        private val HIT_LOCATION_LABELS =
            mapOf(
                HitLocation.HEAD to "Cabeza",
                HitLocation.ARMS to "Brazos",
                HitLocation.TORSO to "Torso",
                HitLocation.WAIST to "Cintura",
                HitLocation.LEGS to "Piernas",
            )

        /** Builds the sheet's display model. */
        fun from(
            survivor: Survivor,
            survivalLimit: Int,
            huntXpTrackMax: Int,
            insanityThreshold: Int,
        ): SurvivorSheetModel {
            val extras = survivor.tokens.filterNot { it in DESIGN_TOKENS }.sorted()
            return SurvivorSheetModel(
                name = survivor.name,
                initial =
                    survivor.name
                        .trim()
                        .take(1)
                        .uppercase(),
                huntXp = survivor.huntXp,
                huntXpMax = huntXpTrackMax,
                survival = survivor.survival,
                survivalLimit = survivalLimit,
                canSpendSurvival = survivor.survival < survivalLimit,
                insanity = survivor.insanity,
                mentalState = if (survivor.isInsane(insanityThreshold)) MentalState.INSANE else MentalState.SANE,
                attributes =
                    listOf(
                        AttributeRow("MOV", survivor.attributes.movement),
                        AttributeRow("PREC", survivor.attributes.accuracy),
                        AttributeRow("FZA", survivor.attributes.strength),
                        AttributeRow("EVA", survivor.attributes.evasion),
                        AttributeRow("SUE", survivor.attributes.luck),
                        AttributeRow("VEL", survivor.attributes.speed),
                    ),
                hitLocations =
                    HIT_LOCATION_ORDER.map { location ->
                        val wound = survivor.wounds[location]
                        HitLocationRowModel(
                            location = location,
                            label = HIT_LOCATION_LABELS.getValue(location),
                            armor = wound?.armor ?: 0,
                            light = wound?.light == true,
                            heavy = wound?.heavy == true,
                        )
                    },
                tokenChips =
                    (DESIGN_TOKENS + extras).map { token ->
                        TokenChip(token = token, label = token, selected = token in survivor.tokens)
                    },
                gearSlots = AssignGear.MAX_GEAR_SLOTS,
            )
        }
    }
}
