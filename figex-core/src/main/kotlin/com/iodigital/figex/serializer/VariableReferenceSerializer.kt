package com.iodigital.figex.serializer

import com.iodigital.figex.models.figma.FigmaVariableReference
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

internal object VariableReferenceSerializer : KSerializer<List<FigmaVariableReference>> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("binding", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): List<FigmaVariableReference> {
        val tree = (decoder as JsonDecoder).decodeJsonElement() as JsonObject
        return tree.keys.flatMap { key ->
            val value = requireNotNull(tree[key])
            value.getReferences(decoder.json).map { it.copy(usage = key) }
        }
    }

    override fun serialize(encoder: Encoder, value: List<FigmaVariableReference>) =
        throw UnsupportedOperationException("Not implemented")

    private fun JsonElement.getReferences(json: Json): List<FigmaVariableReference> = when (this) {
        is JsonArray -> json.decodeFromJsonElement<List<FigmaVariableReference>>(this)

        is JsonObject -> if ("type" in keys) {
            listOf(json.decodeFromJsonElement<FigmaVariableReference>(this))
        } else {
            values.map { it.getReferences(json) }.flatten()
        }

        else -> throw IllegalStateException("Dead end: $this")
    }
}
