package com.iodigital.figex.models.figex

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Serializable model of an Xcode `*.colorset/Contents.json` file. Building the JSON through this
 * model (instead of string templating) guarantees the generated file is always valid JSON.
 */
@Serializable
internal data class XcodeColorAsset(
    val colors: List<Entry>,
    val info: Info = Info(),
) {
    @Serializable
    data class Entry(
        val idiom: String = "universal",
        val appearances: List<Appearance>? = null,
        val color: Color,
    )

    @Serializable
    data class Appearance(
        val appearance: String,
        val value: String,
    )

    @Serializable
    data class Color(
        @SerialName("color-space") val colorSpace: String = "srgb",
        val components: Components,
    )

    @Serializable
    data class Components(
        val alpha: String,
        val red: String,
        val green: String,
        val blue: String,
    )

    @Serializable
    data class Info(
        val author: String = "Figex",
        val version: Int = 1,
    )
}
