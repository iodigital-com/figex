package com.iodigital.figex.models.figma

import kotlinx.serialization.Serializable

@Serializable
internal data class FigmaComponent(
    val key: String,
    val name: String,
    val componentSetId: String?,
)