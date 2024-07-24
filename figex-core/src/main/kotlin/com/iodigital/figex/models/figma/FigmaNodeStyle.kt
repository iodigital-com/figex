package com.iodigital.figex.models.figma

import kotlinx.serialization.Serializable

@Serializable
internal data class FigmaNodeStyle(
    val fontFamily: String? = null,
    val fontPostScriptName: String? = null,
    val fontSize: Float? = null,
    val fontWeight: Int? = null,
    val letterSpacing: Float? = null,
    val lineHeightPercent: Float? = null,
    val lineHeightPercentFontSize: Float? = null,
    val lineHeightPx: Float? = null,
    val lineHeightUnit: String? = null,
    val textAlignHorizontal: String? = null,
    val textAlignVertical: String? = null,
    val textAutoResize: String? = null,
)