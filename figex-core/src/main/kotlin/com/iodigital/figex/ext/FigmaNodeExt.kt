package com.iodigital.figex.ext

import com.iodigital.figex.models.figex.FigExArgbColor
import com.iodigital.figex.models.figex.FigExComponent
import com.iodigital.figex.models.figex.FigExTextStyle
import com.iodigital.figex.models.figex.FigExValue
import com.iodigital.figex.models.figma.FigmaNode
import com.iodigital.figex.models.figma.FigmaNode.ResolvedType.*
import com.iodigital.figex.models.figma.FigmaVariableValue

internal fun FigmaNode.asFigExValue(): FigExValue<*> = with(document) {
    val values = valuesByMode ?: throw IllegalArgumentException("No values for $name")

    return when (resolvedType) {
        FigmaNode.ResolvedType.String -> FigExValue(
            name = name,
            byMode = values.mapValues { (_, value) -> value.asString() },
            type = String::class,
        )

        Color -> FigExValue(
            name = name,
            byMode = values.mapValues { (_, value) -> value.asColor() },
            type = FigExArgbColor::class,
        )

        FigmaNode.ResolvedType.Float -> FigExValue(
            name = name,
            byMode = values.mapValues { (_, value) -> value.asFloat() },
            type = Float::class,
        )

        FigmaNode.ResolvedType.Boolean -> FigExValue(
            name = name,
            byMode = values.mapValues { (_, value) -> value.asBoolean() },
            type = Boolean::class,
        )

        null -> throw IllegalArgumentException("Tried to map $name as value but missing resolvedType")
    }
}

internal fun FigmaNode.asFigExComponents() = components.map { (id, component) ->
    val set = componentSets.entries.firstOrNull { (id2, _) -> id2 == component.componentSetId }
    component.asFigExComponent(id = id, set = set?.run { key to value })
}

internal fun FigmaNode.asFigExTextStyle(): FigExValue<FigExTextStyle> = with(document) {
    val default = FigExTextStyle()

    val base = FigExTextStyle(
        fontSize = style?.fontSize ?: default.fontSize,
        fontWeight = style?.fontWeight ?: default.fontWeight,
        fontFamily = style?.fontFamily ?: default.fontFamily,
        fontPostScriptName = style?.fontPostScriptName ?: default.fontPostScriptName,
        letterSpacing = style?.letterSpacing ?: default.letterSpacing,
        lineHeightPercent = style?.lineHeightPercent ?: default.lineHeightPercent,
        lineHeightPercentFontSize = style?.lineHeightPercentFontSize
            ?: default.lineHeightPercentFontSize,
        lineHeightPx = style?.lineHeightPx ?: default.lineHeightPx,
        textAlignHorizontal = style?.textAlignHorizontal ?: default.textAlignHorizontal,
        textAlignVertical = style?.textAlignVertical ?: default.textAlignVertical,
        textAutoResize = style?.textAutoResize ?: default.textAutoResize,
        lineHeightUnit = style?.lineHeightUnit ?: default.lineHeightUnit,
    )

    if (boundValuesByMode.isNullOrEmpty()) {
        return FigExValue(name = name, byMode = mapOf("base" to base), type = FigExTextStyle::class)
    }

    val modes = boundValuesByMode.values.map { it.keys }
    require(modes.distinct().size == 1) { "Expected all bound values to have the same modes" }
    val definitiveModes = modes.first()

    return FigExValue(
        name = name,
        type = FigExTextStyle::class,
        byMode = definitiveModes.associateWith { mode ->
            base.copy(
                fontSize = boundValuesByMode["fontSize"]?.get(mode)?.asFloat()
                    ?: base.fontSize,
                fontWeight = boundValuesByMode["fontWeight"]?.get(mode)?.asInt()
                    ?: base.fontWeight,
                fontFamily = boundValuesByMode["fontFamily"]?.get(mode)?.asString()
                    ?: base.fontFamily,
                fontPostScriptName = boundValuesByMode["fontPostScriptName"]?.get(mode)?.asString()
                    ?: base.fontPostScriptName,
                letterSpacing = boundValuesByMode["letterSpacing"]?.get(mode)?.asFloat()
                    ?: base.letterSpacing,
                lineHeightPercent = boundValuesByMode["lineHeightPercent"]?.get(mode)?.asFloat()
                    ?: base.lineHeightPercent,
                lineHeightPercentFontSize = boundValuesByMode["lineHeightPercentFontSize"]?.get(mode)
                    ?.asFloat() ?: base.lineHeightPercentFontSize,
                lineHeightPx = boundValuesByMode["lineHeightPx"]?.get(mode)?.asFloat()
                    ?: base.lineHeightPx,
                textAlignHorizontal = boundValuesByMode["textAlignHorizontal"]?.get(mode)
                    ?.asString()
                    ?: base.textAlignHorizontal,
                textAlignVertical = boundValuesByMode["textAlignVertical"]?.get(mode)?.asString()
                    ?: base.textAlignVertical,
                textAutoResize = boundValuesByMode["textAutoResize"]?.get(mode)?.asString()
                    ?: base.textAutoResize,
                lineHeightUnit = boundValuesByMode["lineHeightUnit"]?.get(mode)?.asString()
                    ?: base.lineHeightUnit,
            )
        }
    )
}


