package io.kdmtracker.core.domain

import kotlinx.serialization.Serializable

/** Where a survivor is in the lantern-year loop. `DEAD` survivors are kept in the roster. */
enum class SurvivorStatus { ALIVE, DEPARTING, DEAD }

/** The five hit locations a survivor carries armor and wounds on. */
enum class HitLocation { HEAD, TORSO, ARMS, WAIST, LEGS }

/**
 * A hit location's armor and its light/heavy injury boxes.
 *
 * `light` is the sheet's *herida firme* box and `heavy` the *herida grave* box; both reset when the
 * survivors return (Settlement Phase step 2), while [armor] does not.
 */
@Serializable
data class WoundState(
    val armor: Int = 0,
    val light: Boolean = false,
    val heavy: Boolean = false,
)

/** A survivor's six printed attributes. */
@Serializable
data class Attributes(
    val movement: Int = 0,
    val accuracy: Int = 0,
    val strength: Int = 0,
    val evasion: Int = 0,
    val luck: Int = 0,
    val speed: Int = 0,
)

/** A survivor's weapon proficiency: the weapon type, its XP and the specialist/master marks. */
@Serializable
data class WeaponProficiency(
    val type: String = "",
    val xp: Int = 0,
    val specialist: Boolean = false,
    val master: Boolean = false,
)

/**
 * A settlement survivor, mirroring the official survivor record sheet as data.
 *
 * Every field is defaulted so a partial record decodes; the mandatory identity is only [id]. Vitals,
 * attributes, wounds, progression and limitations are all stored (the sheet's fields); the maximum
 * survival is the settlement's survival limit (derived, not stored), and the Valor/Understanding
 * milestone abilities are derived later, not stored.
 *
 * @property nameBonusApplied whether the one-time "+1 survival when named" bonus has been applied.
 * @property brainTrauma the sheet's brain-trauma (Cerebro) mark; checked when insanity drops below 0.
 * @property wounds the armor + injury boxes per hit location; a missing key reads as an all-zero box.
 * @property tokens the hunt tokens (e.g. bleeding) removed when the survivor returns.
 * @property gear the assigned gear names; their survivor-scope modifiers feed the effective attributes.
 * @property status whether the survivor is alive, departing (chosen for the hunt) or dead.
 */
@Serializable
data class Survivor(
    val id: Ulid,
    val name: String = "",
    val sex: Sex = Sex.OTHER,
    val nameBonusApplied: Boolean = false,
    val retired: Boolean = false,
    val survival: Int = 0,
    val insanity: Int = 0,
    val brainTrauma: Boolean = false,
    val attributes: Attributes = Attributes(),
    val wounds: Map<HitLocation, WoundState> = emptyMap(),
    val huntXp: Int = 0,
    val weapon: WeaponProficiency = WeaponProficiency(),
    val courage: Int = 0,
    val understanding: Int = 0,
    val disorders: List<String> = emptyList(),
    val fightingArts: List<String> = emptyList(),
    val notes: String = "",
    val tokens: Set<String> = emptySet(),
    val gear: List<String> = emptyList(),
    val status: SurvivorStatus = SurvivorStatus.ALIVE,
)
