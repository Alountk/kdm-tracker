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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer

/**
 * An innovation the settlement can unlock.
 *
 * @property id the innovation's stable, language-neutral identity.
 * @property names the innovation's localized display names.
 * @property description the rules text shown on the innovation card.
 * @property consequences the names of the cards the innovation adds to the deck.
 * @property survivalLimitBonus the survival limit increase the innovation grants.
 */
@Serializable(with = InnovationSerializer::class)
data class Innovation(
    val id: InnovationId,
    val names: LocalizedText,
    val description: String,
    val consequences: List<String> = emptyList(),
    val survivalLimitBonus: Int = 0,
)

/**
 * The innovations a settlement has unlocked, deduped by [Innovation.id].
 *
 * The collection holds content only; anything derived from it — such as the
 * [InnovationDeck] — lives on the deck, never hardcoded here.
 */
@Serializable
data class InnovationCollection(
    val innovations: List<Innovation> = emptyList(),
) {
    /**
     * Returns a collection that also holds [innovation], unless an innovation
     * with the same [Innovation.id] is already present. The first-seen
     * innovation wins; the later one is ignored.
     */
    fun with(innovation: Innovation): InnovationCollection =
        if (innovations.any { it.id == innovation.id }) this else copy(innovations = innovations + innovation)

    /** The sum of every unlocked innovation's survival limit bonus. */
    fun survivalLimitBonus(): Int = innovations.sumOf { it.survivalLimitBonus }

    companion object {
        /**
         * Resolves [names] against [catalog].
         *
         * A name resolves through either language, so a caller may pass what it displays
         * (`Lenguaje` on a Spanish screen, `Language` on an English one) and reach the same
         * innovation. Unknown-name rule: a name absent from [catalog] is ignored, never
         * invented and never an error. The result keeps the order [names] gives, and a
         * repeated name resolves to the same innovation only once.
         */
        fun of(
            names: Collection<String>,
            catalog: List<Innovation>,
        ): InnovationCollection =
            names
                .mapNotNull { name -> catalog.firstOrNull { it.names.es == name || it.names.en == name } }
                .fold(InnovationCollection()) { collection, innovation -> collection.with(innovation) }
    }
}

/**
 * The deck a settlement draws from when it innovates: exactly the
 * [Innovation.consequences] of the unlocked innovations, deduped and sorted.
 * Innovation names are not cards and never appear in the deck.
 */
data class InnovationDeck(
    val cards: List<String>,
) {
    companion object {
        fun of(collection: InnovationCollection): InnovationDeck =
            InnovationDeck(
                collection.innovations
                    .flatMap { it.consequences }
                    .distinct()
                    .sorted(),
            )
    }
}

/**
 * Reads both shapes an [Innovation] has ever been written in.
 *
 * The new shape carries the stable [InnovationId] and localized [Innovation.names]. The old one
 * carried only a display name, because that *was* the identity — so it is mapped through
 * [ContentIdentity] and keeps its description and consequences verbatim. An old name the table
 * does not know survives under a synthesized `unknown.…` id rather than being dropped.
 */
object InnovationSerializer : KSerializer<Innovation> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("io.kdmtracker.core.domain.Innovation") {
            element<String>("id")
            element("names", LocalizedText.serializer().descriptor)
            element<String>("description")
            element("consequences", ListSerializer(serializer<String>()).descriptor)
            element<Int>("survivalLimitBonus")
        }

    override fun serialize(
        encoder: Encoder,
        value: Innovation,
    ) {
        val json = encoder as? JsonEncoder ?: error("Innovation is persisted as JSON")
        json.encodeJsonElement(
            JsonObject(
                mapOf(
                    "id" to JsonPrimitive(value.id.value),
                    "names" to Json.encodeToJsonElement(LocalizedText.serializer(), value.names),
                    "description" to JsonPrimitive(value.description),
                    "consequences" to JsonArray(value.consequences.map { JsonPrimitive(it) }),
                    "survivalLimitBonus" to JsonPrimitive(value.survivalLimitBonus),
                ),
            ),
        )
    }

    override fun deserialize(decoder: Decoder): Innovation {
        val json = decoder as? JsonDecoder ?: error("Innovation is persisted as JSON")
        val fields = json.decodeJsonElement().jsonObject
        val legacyName = fields["name"]?.jsonPrimitive?.content
        val id =
            fields["id"]?.jsonPrimitive?.content
                ?: legacyName?.let { name ->
                    ContentIdentity.innovationIdFor(name)?.value ?: ContentIdentity.synthesizedId(name)
                }
                ?: error("an innovation needs an id, or a legacy name to resolve one")
        val names =
            fields["names"]?.let { Json.decodeFromJsonElement(LocalizedText.serializer(), it) }
                ?: legacyName?.let { LocalizedText(es = it) }
                ?: error("an innovation needs names, or a legacy name to derive them from")
        return Innovation(
            id = InnovationId(id),
            names = names,
            description = fields["description"]?.jsonPrimitive?.content ?: "",
            consequences = fields["consequences"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
            survivalLimitBonus = fields["survivalLimitBonus"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
        )
    }
}
