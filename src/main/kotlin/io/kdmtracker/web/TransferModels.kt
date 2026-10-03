package io.kdmtracker.web

data class ExportDocument(
    val schemaVersion: Int? = null,
    val exportedAt: String? = null,
    val settlements: List<ExportSettlement>? = null,
)

data class ExportSettlement(
    val name: String? = null,
    val lanternYear: Int? = null,
    val notes: String? = null,
    val phase: String? = null,
    val currentStep: Int? = null,
    val survivors: List<ExportSurvivor>? = null,
    val resources: List<ExportResource>? = null,
    val innovations: List<ExportInnovation>? = null,
    val log: List<ExportLogEntry>? = null,
)

data class ExportSurvivor(
    val name: String? = null,
    val sex: String? = null,
    val age: Int? = null,
    val movement: Int? = null,
    val speed: Int? = null,
    val accuracy: Int? = null,
    val strength: Int? = null,
    val dexterity: Int? = null,
    val evasion: Int? = null,
    val armor: Int? = null,
    val insanity: Int? = null,
    val courage: Int? = null,
    val understanding: Int? = null,
    val xp: Int? = null,
    val alive: Boolean? = null,
    val notes: String? = null,
)

data class ExportResource(
    val name: String? = null,
    val category: String? = null,
    val quantity: Int? = null,
)

data class ExportInnovation(
    val name: String? = null,
    val unlockedInYear: Int? = null,
    val notes: String? = null,
)

data class ExportLogEntry(
    val lanternYear: Int? = null,
    val title: String? = null,
    val body: String? = null,
    val createdAt: String? = null,
)
