package com.iodigital.figex.models.figma

import kotlinx.serialization.Serializable

@Serializable
internal data class FigmaComponentSet(
    val key: String,
    val name: String,
)