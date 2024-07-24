package com.iodigital.figex.ext

import com.iodigital.figex.models.figex.FigExArgbColor
import com.iodigital.figex.models.figma.FigmaVariableValue
import kotlin.math.roundToInt

internal fun FigmaVariableValue.asFloat() = when (this) {
    is FigmaVariableValue.FloatValue -> value
    else -> throw IllegalStateException("Tried to interpret ${this::class.simpleName} as float")
}

internal fun FigmaVariableValue.asString() = when (this) {
    is FigmaVariableValue.StringValue -> value
    else -> throw IllegalStateException("Tried to interpret ${this::class.simpleName} as String")
}

internal fun FigmaVariableValue.asBoolean() = when (this) {
    is FigmaVariableValue.BooleanValue -> value
    else -> throw IllegalStateException("Tried to interpret ${this::class.simpleName} as String")
}

internal fun FigmaVariableValue.asInt() = when (this) {
    is FigmaVariableValue.FloatValue -> value.roundToInt()
    else -> throw IllegalStateException("Tried to interpret ${this::class.simpleName} as String")
}


internal fun FigmaVariableValue.asColor() = when (this) {
    is FigmaVariableValue.ColorValue -> FigExArgbColor(
        a = a,
        r = r,
        g = g,
        b = b,
    )

    else -> throw IllegalStateException("Tried to interpret ${this::class.simpleName} as color")
}