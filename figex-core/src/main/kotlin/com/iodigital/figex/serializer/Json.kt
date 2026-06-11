package com.iodigital.figex.serializer

import com.iodigital.figex.models.figex.FigExConfig
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

@OptIn(ExperimentalSerializationApi::class)
internal val DefaultJson = Json {
    prettyPrint = true
    isLenient = true
    ignoreUnknownKeys = true
    explicitNulls = false
    decodeEnumsCaseInsensitive = true
}

@OptIn(ExperimentalSerializationApi::class)
internal val ConfigJson = Json {
    decodeEnumsCaseInsensitive = true
    serializersModule = SerializersModule {
        polymorphicDefaultDeserializer(FigExConfig.Export::class) { type ->
            when (type) {
                "values" -> FigExConfig.Export.Values.serializer()
                "icons" -> FigExConfig.Export.Icons.serializer()
                else -> throw IllegalArgumentException("No serializer for export type: $type")
            }
        }
    }
}