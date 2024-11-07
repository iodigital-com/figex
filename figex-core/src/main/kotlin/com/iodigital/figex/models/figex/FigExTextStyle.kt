package com.iodigital.figex.models.figex

import com.iodigital.figex.models.Contextable

data class FigExTextStyle(
    val fontFamily: String = "serif",
    val fontPostScriptName: String = "serif",
    val fontSize: Float = 14f,
    val fontWeight: Int = 500,
    val fontStyle: String = "Regular",
    val letterSpacing: Float = 0f,
    val lineHeightPercent: Float = 104f,
    val lineHeightPercentFontSize: Float = 127f,
    val lineHeightPx: Float = 18f,
    val lineHeightUnit: String = "PIXELS",
    val textAlignHorizontal: String = "LEFT",
    val textAlignVertical: String = "TOP",
    val textAutoResize: String = "WIDTH_AND_HEIGHT",
) : Contextable {
    override fun toContext() = mapOf(
        "font_family" to fontFamily,
        "font_post_script_name" to fontPostScriptName,
        "font_size" to fontSize,
        "font_style" to fontStyle,
        "font_weight" to fontWeight,
        "letter_spacing" to letterSpacing,
        "line_height_percent" to lineHeightPercent,
        "line_height_percent_font_size" to lineHeightPercentFontSize,
        "line_height_px" to lineHeightPx,
        "line_height_unit" to lineHeightUnit,
        "text_align_horizontal" to textAlignHorizontal,
        "text_align_vertical" to textAlignVertical,
        "text_auto_resize" to textAutoResize,
    )
}