package com.iodigital.figex.models.figma

import com.iodigital.figex.serializer.VariableReferenceSerializer
import kotlinx.serialization.Serializable

@Serializable
internal data class FigmaChild(
    val children: List<FigmaChild> = emptyList(),
    val id: String,
    val type: String,
    val name: String,
    @Serializable(with = VariableReferenceSerializer::class) val boundVariables: List<FigmaVariableReference> = emptyList(),
)