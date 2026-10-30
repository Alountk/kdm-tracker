package io.kdmtracker.core.domain

/**
 * The material a resource is made of — the settlement summary's four buckets.
 *
 * This is the **aggregate** dimension, and it is not [ResourceCategory]. A resource's category is
 * a rules concern (which sheet section it belongs to); its material is what the summary sums by.
 * A monster's fur and the generic monster hide are both [HIDE], so they add into one cell rather
 * than splitting across two.
 *
 * @property names the bucket's label in each language, matching the design's own labels.
 * @property basicId the plain resource of this material the grids adjust. A material has several
 * basic resources (a monster skull is also a bone), so the material alone does not name one.
 */
enum class ResourceMaterial(
    val names: LocalizedText,
    val basicId: ResourceId,
) {
    BONE(LocalizedText(es = "Hueso", en = "Bone"), ResourceId("bone.monster")),
    ORGAN(LocalizedText(es = "Órgano", en = "Organ"), ResourceId("organ.monster")),
    HIDE(LocalizedText(es = "Pellejo", en = "Hide"), ResourceId("hide.monster")),
    SCRAP(LocalizedText(es = "Chatarra", en = "Scrap"), ResourceId("scrap")),
}
