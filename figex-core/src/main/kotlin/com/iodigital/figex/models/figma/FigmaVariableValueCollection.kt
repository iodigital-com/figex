package com.iodigital.figex.models.figma

import com.iodigital.figex.serializer.VariableValueCollectionSerializer
import kotlinx.serialization.Serializable

@Serializable(with = VariableValueCollectionSerializer::class)
internal sealed class FigmaVariableValueCollection : Collection<FigmaVariableValue> {

    data class List(
        val values: kotlin.collections.List<FigmaVariableValue>
    ) : FigmaVariableValueCollection(),
        kotlin.collections.List<FigmaVariableValue> by values

    class Map(
        val values: kotlin.collections.Map<String, FigmaVariableValue>
    ) : FigmaVariableValueCollection(),
        Collection<FigmaVariableValue> by values.values
}