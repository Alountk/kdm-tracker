package io.kdmtracker.core.domain

import io.kdmtracker.core.content.ContentIdentity
import io.kdmtracker.core.rules.DerivedValues
import io.kdmtracker.core.ruleset.Ruleset
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/**
 * The **extra** settlement record the phase steps read, separate from [Settlement].
 *
 * It holds only what the settlement aggregate does not own — the [Timeline], the
 * unlocked [InnovationCollection], the built [LocationSet], the resource ledger and
 * the live counters the milestone rules inspect. Scalars that belong to [Settlement]
 * (`name`, `lanternYear`, `population`, `phase`, `currentStep`, `endeavors`, `sync`)
 * are never duplicated here; [SettlementRecord] composes the full step-facing view.
 *
 * @property resourceEntries the append-only resource ledger; storage is projected from it.
 * @property deaths the survivors lost so far.
 * @property births the survivors born so far.
 * @property phaseActionsUsed the action identifiers already spent this Settlement Phase, so
 *   once-per-phase actions (such as Innovate) can be refused on a repeat attempt.
 * @property gear the ids of the gear the settlement has crafted.
 * @property principles the chosen principle label per [PrincipleCategory] (step 5/6 decisions).
 * @property resolvedMilestones the milestone ids the table has already resolved.
 */
@Serializable
data class SettlementState(
    val timeline: Timeline = Timeline(),
    val innovations: InnovationCollection = InnovationCollection(),
    val locations: LocationSet = LocationSet(),
    val resourceEntries: List<ResourceEntry> = emptyList(),
    val deaths: Int = 0,
    val births: Int = 0,
    val phaseActionsUsed: Set<String> = emptySet(),
    @Serializable(with = GearIdListSerializer::class)
    val gear: List<GearId> = emptyList(),
    val principles: Map<PrincipleCategory, String> = emptyMap(),
    val resolvedMilestones: Set<MilestoneId> = emptySet(),
    val survivors: List<Survivor> = emptyList(),
    val hunt: HuntState = HuntState(),
    val showdown: ShowdownState = ShowdownState(),
    val specialShowdown: Boolean = false,
) {
    /** Whether [key] has already been used during the current Settlement Phase. */
    fun usedInPhase(key: String): Boolean = key in phaseActionsUsed

    /**
     * Returns a copy marking [key] as used this phase. Idempotent: a key already
     * present leaves the state unchanged.
     */
    fun withPhaseAction(key: String): SettlementState =
        if (key in phaseActionsUsed) this else copy(phaseActionsUsed = phaseActionsUsed + key)

    /** Returns a copy that also holds [id] in its gear, deduped by id. */
    fun withGear(id: GearId): SettlementState = if (id in gear) this else copy(gear = gear + id)
}

/** The storage lines projected from this state's resource ledger. */
fun SettlementState.storage(): List<StorageLine> = Storage.project(resourceEntries)

/**
 * The settlement's survival limit: the ruleset base plus the survival limit bonus of
 * every unlocked innovation. Derived, never stored.
 */
fun SettlementState.survivalLimit(ruleset: Ruleset): Int =
    DerivedValues.survivalLimit(ruleset, innovations.survivalLimitBonus())

/**
 * Reads both shapes the settlement's crafted-gear list has ever been written in.
 *
 * The new shape stores each entry as an `{"id": …}` object. The old one stored the gear's
 * display name, because that *was* the identity — so it is mapped through [ContentIdentity]. An
 * old name the table does not know survives under a synthesized `unknown.…` id rather than
 * being dropped: a discarded entry is crafted gear the settlement silently loses.
 */
internal object GearIdListSerializer : KSerializer<List<GearId>> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("io.kdmtracker.core.domain.SettlementState.gear") {
            element("gear", ListSerializer(JsonElement.serializer()).descriptor)
        }

    override fun serialize(
        encoder: Encoder,
        value: List<GearId>,
    ) {
        val json = encoder as? JsonEncoder ?: error("SettlementState.gear is persisted as JSON")
        json.encodeJsonElement(
            kotlinx.serialization.json.JsonArray(value.map { JsonObject(mapOf("id" to JsonPrimitive(it.value))) }),
        )
    }

    override fun deserialize(decoder: Decoder): List<GearId> {
        val json = decoder as? JsonDecoder ?: error("SettlementState.gear is persisted as JSON")
        return json.decodeJsonElement().jsonArray.map { element ->
            val id =
                when (element) {
                    is JsonPrimitive ->
                        ContentIdentity.gearIdFor(element.content)?.value
                            ?: ContentIdentity.synthesizedId(element.content)
                    is JsonObject -> element.getValue("id").jsonPrimitive.content
                    else -> error("a crafted gear needs an id, or a legacy name to resolve one")
                }
            GearId(id)
        }
    }
}
