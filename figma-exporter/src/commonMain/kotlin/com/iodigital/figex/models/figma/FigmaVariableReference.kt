package com.iodigital.figex.models.figma

import kotlinx.serialization.Serializable

@Serializable
internal data class FigmaVariableReference(
    val type: String,
    val id: String,
) {
    val plainId get() = id.removePrefix("VariableID:")
}