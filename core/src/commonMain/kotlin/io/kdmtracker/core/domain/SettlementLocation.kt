package io.kdmtracker.core.domain

import io.kdmtracker.core.content.ContentIdentity
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * A settlement location the settlement can build.
 *
 * The location carries everything the rules layer can enforce when building it —
 * its prerequisites, its build cost, and the actions it offers once built.
 *
 * @property id the location's stable, language-neutral identity.
 * @property names the location's localized display names.
 * @property requires the ids of the locations that must already be built.
 * @property buildEndeavors the number of endeavors required to build the location.
 * @property buildCost the resources required to build the location, as id → count.
 * @property actions the actions the location offers once built.
 * @property recipes the gear this location can craft once built.
 */
@Serializable
data class SettlementLocation(
    val id: LocationId,
    val names: LocalizedText,
    val requires: List<LocationId> = emptyList(),
    val buildEndeavors: Int = 0,
    val buildCost: Map<ResourceId, Int> = emptyMap(),
    val actions: List<SettlementAction> = emptyList(),
    val recipes: List<GearRecipe> = emptyList(),
)

/**
 * The settlement locations that have been built, keyed by [SettlementLocation.id].
 *
 * The set holds content only; the location definitions (with their [SettlementLocation.requires])
 * are passed in by the caller, so this type stays catalog-independent.
 */
@Serializable(with = LocationSetSerializer::class)
data class LocationSet(
    val built: List<LocationId> = emptyList(),
) {
    /**
     * Whether [location] can be built: every one of its
     * [SettlementLocation.requires] is already in [built].
     */
    fun canBuild(location: SettlementLocation): Boolean = location.requires.all { it in built }

    /**
     * Returns a set that also holds [location], but only when [canBuild] allows
     * it and its id is not already present. Otherwise returns this set
     * unchanged, making repeated builds idempotent.
     */
    fun with(location: SettlementLocation): LocationSet =
        if (location.id in built || !canBuild(location)) this else copy(built = built + location.id)
}

/**
 * Reads both shapes a [LocationSet] has ever been written in.
 *
 * The new shape stores each built location as an `{"id": …}` object. The old one stored its
 * display name, because that *was* the identity — so it is mapped through [ContentIdentity]. An
 * old name the table does not know is **not** dropped: it survives under a synthesized
 * `unknown.…` id, because a discarded location is a build the user silently loses.
 */
object LocationSetSerializer : KSerializer<LocationSet> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("io.kdmtracker.core.domain.LocationSet") {
            element("built", ListSerializer(JsonElement.serializer()).descriptor)
        }

    override fun serialize(
        encoder: Encoder,
        value: LocationSet,
    ) {
        val json = encoder as? JsonEncoder ?: error("LocationSet is persisted as JSON")
        json.encodeJsonElement(
            JsonObject(
                mapOf(
                    "built" to JsonArray(value.built.map { JsonObject(mapOf("id" to JsonPrimitive(it.value))) }),
                ),
            ),
        )
    }

    override fun deserialize(decoder: Decoder): LocationSet {
        val json = decoder as? JsonDecoder ?: error("LocationSet is persisted as JSON")
        val fields = json.decodeJsonElement().jsonObject
        val built =
            fields["built"]?.jsonArray.orEmpty().map { element ->
                val id =
                    when (element) {
                        is JsonPrimitive ->
                            ContentIdentity.locationIdFor(element.content)?.value
                                ?: ContentIdentity.synthesizedId(element.content)
                        is JsonObject -> element.getValue("id").jsonPrimitive.content
                        else -> error("a built location needs an id, or a legacy name to resolve one")
                    }
                LocationId(id)
            }
        return LocationSet(built)
    }
}
