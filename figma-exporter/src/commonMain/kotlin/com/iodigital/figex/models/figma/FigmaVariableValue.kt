package com.iodigital.figex.models.figma

import com.iodigital.figex.serializer.VariableValueSerializer
import kotlinx.serialization.Serializable

@Serializable(with = VariableValueSerializer::class)
internal sealed class FigmaVariableValue {

    @Serializable
    data class ColorValue(
        val a: Float,
        val r: Float,
        val g: Float,
        val b: Float,
    ) : FigmaVariableValue()

    @Serializable
    data class FloatValue(
        val value: Float
    ): FigmaVariableValue()

    @Serializable
    data class StringValue(
        val value: String
    ): FigmaVariableValue()

    @Serializable
    data class Reference(
        val reference: FigmaVariableReference
    ): FigmaVariableValue()
}