package com.iodigital.figex.models.figex

import kotlinx.serialization.Serializable

@Serializable
enum class FigExIconFormat {
    Svg, Pdf, AndroidXml, Png, Webp;

    val suffix
        get() = when (this) {
            Svg -> "svg"
            Webp -> "webp"
            Png -> "png"
            Pdf -> "pdf"
            AndroidXml -> "xml"
        }
}