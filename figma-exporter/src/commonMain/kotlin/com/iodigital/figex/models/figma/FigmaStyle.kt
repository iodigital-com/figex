package com.iodigital.figex.models.figma

import kotlinx.serialization.Serializable

@Serializable
internal data class FigmaStyle(
    val key: String,
    val name: String,
    val styleType: Type,
) {
    @Serializable
    enum class Type {
        Text,
        Fill,
        Effect,
    }
}