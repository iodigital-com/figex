package com.iodigital.figex.serializer

import com.iodigital.figex.models.figma.FigmaVariableValue
import com.iodigital.figex.models.figma.FigmaVariableValueCollection
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

internal object VariableValueCollectionSerializer : KSerializer<FigmaVariableValueCollection> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("value", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): FigmaVariableValueCollection {
        val tree = (decoder as JsonDecoder).decodeJsonElement()
        return tree.getValue(decoder.json)
    }

    override fun serialize(encoder: Encoder, value: FigmaVariableValueCollection) =
        throw UnsupportedOperationException("Not implemented")

    private fun JsonElement.getValue(json: Json): FigmaVariableValueCollection = when (this) {
        is JsonArray -> FigmaVariableValueCollection.List(
            json.decodeFromJsonElement<List<FigmaVariableValue>>(this)
        )

        is JsonObject -> FigmaVariableValueCollection.Map(
            json.decodeFromJsonElement<Map<String, FigmaVariableValue>>(this)
        )

        else -> throw IllegalStateException("Dead end: $this")
    }
}
