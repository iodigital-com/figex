package com.iodigital.figex.models.figex

import com.iodigital.figex.models.Contextable
import kotlin.math.roundToInt

data class FigExArgbColor(
    val a: Float,
    val g: Float,
    val b: Float,
    val r: Float,
) : Contextable {

    override fun toContext() = mapOf(
        "a" to a,
        "r" to r,
        "g" to g,
        "b" to b,
        "a255" to a.to255(),
        "r255" to r.to255(),
        "g255" to g.to255(),
        "b255" to b.to255(),
        "argb" to listOf(
            a.to255().toString(radix = 16),
            r.to255().toString(radix = 16),
            g.to255().toString(radix = 16),
            b.to255().toString(radix = 16),
        ).joinToString("") {
            if (it.length == 1) {
                "0$it"
            } else {
                it
            }
        }
    )

    private fun Float.to255() = (this * 255).roundToInt().coerceIn(0, 255)
}