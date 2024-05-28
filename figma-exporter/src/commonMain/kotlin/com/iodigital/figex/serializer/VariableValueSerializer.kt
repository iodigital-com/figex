package com.iodigital.figex.serializer

import com.iodigital.figex.models.figma.FigmaVariableReference
import com.iodigital.figex.models.figma.FigmaVariableValue
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
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.float
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonPrimitive

internal object VariableValueSerializer : KSerializer<FigmaVariableValue> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("value", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): FigmaVariableValue {
        val tree = (decoder as JsonDecoder).decodeJsonElement()
        return tree.getValue(decoder.json)
    }

    override fun serialize(encoder: Encoder, value: FigmaVariableValue) =
        throw UnsupportedOperationException("Not implemented")

    private fun JsonElement.getValue(json: Json): FigmaVariableValue = when (this) {
        is JsonPrimitive -> when {
            isString -> FigmaVariableValue.StringValue(content)
            floatOrNull != null -> FigmaVariableValue.FloatValue(float)
            else -> throw UnsupportedOperationException("Unable to map as primitive: $this")
        }

        is JsonObject -> if ("a" in keys && "g" in keys && "r" in keys && "b" in keys) {
            FigmaVariableValue.ColorValue(
                a = this["a"]!!.jsonPrimitive.float,
                r = this["r"]!!.jsonPrimitive.float,
                g = this["g"]!!.jsonPrimitive.float,
                b = this["b"]!!.jsonPrimitive.float,
            )
        } else if ("type" in keys && "id" in keys) {
            json.decodeFromJsonElement<FigmaVariableReference>(this).let { FigmaVariableValue.Reference(it) }
        } else {
            throw IllegalStateException("Unable to map as object: $this")
        }

        else -> throw IllegalStateException("Dead end: $this")
    }
}
