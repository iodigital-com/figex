package com.iodigital.figex.models.figma

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.JsonPrimitive

@Serializable
internal data class FigmaNode(
    val document: NodeDocument,
    val components: Map<String, FigmaComponent>,
    val componentSets: Map<String, FigmaComponentSet>,
) {

    @Serializable
    data class NodeDocument(
        val resolvedType: ResolvedType? = null,
        val type: Type? = null,
        val name: String,
        val valuesByMode: Map<String, FigmaVariableValue>? = null,
        val boundVariables: Map<String, List<FigmaVariableValue>>? = null,
        val boundValuesByMode: Map<String, Map<String, FigmaVariableValue>>? = null,
        val style: FigmaNodeStyle? = null,
    )

    @Serializable
    enum class ResolvedType {
        String, Color, Float
    }

    @Serializable
    enum class Type {
        @SerialName("TEXT")
        Text,
        @SerialName("RECTANGLE")
        Rectangle,
        @SerialName("COMPONENT_SET")
        ComponentSet
    }
}