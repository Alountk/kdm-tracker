package io.kdmtracker.core.content

import kotlinx.serialization.Serializable

@Serializable
data class ContentPack(
    val id: String,
    val version: String,
    val name: String,
    val requires: List<String> = emptyList(),
    val provideInnovations: List<String> = emptyList(),
    val provideLocations: List<String> = emptyList(),
    val provideQuarries: List<String> = emptyList(),
    val provideNemeses: List<String> = emptyList(),
    val provideResources: List<String> = emptyList(),
)
