package com.iodigital.figex.models.figex

import com.iodigital.figex.ext.camel
import com.iodigital.figex.ext.kebab
import com.iodigital.figex.ext.pascal
import com.iodigital.figex.ext.snake
import com.iodigital.figex.models.Contextable
import kotlin.reflect.KClass

data class FigExValue<T>(
    val name: String,
    val byMode: Map<String, T>,
    val type: KClass<*>,
) : Contextable {

    fun copyWithModeAliases(aliases: Map<String, String>) = copy(
        byMode = byMode.mapKeys { (mode, _) -> aliases[mode] ?: mode }
    )

    override fun toContext() = byMode.map { (key, value) ->
        val k = if (key.first().isDigit()) "_$key" else key
        k.snake() to if (value is Contextable) {
            value.toContext()
        } else {
            value.toString()
        }
    }.toMap() + mapOf(
        "name" to mapOf(
            "original" to name,
            "snake" to name.snake(),
            "camel" to name.camel(),
            "kebab" to name.kebab(),
            "pascal" to name.pascal(),
        )
    )

    fun toContext(defaultMode: String) = toContext() +
            (byMode[defaultMode] ?: byMode.values.firstOrNull()).let {
                if (it is Contextable) {
                    it.toContext()
                } else {
                    mapOf("value" to it.toString())
                }
            }
}