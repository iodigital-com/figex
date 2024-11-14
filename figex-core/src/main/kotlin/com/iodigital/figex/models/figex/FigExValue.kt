package com.iodigital.figex.models.figex

import com.iodigital.figex.ext.camel
import com.iodigital.figex.ext.kebab
import com.iodigital.figex.ext.pascal
import com.iodigital.figex.ext.snake
import com.iodigital.figex.models.Contextable
import com.iodigital.figex.toNameObject
import kotlin.reflect.KClass

data class FigExValue<T : Any>(
    val name: String,
    val byMode: Map<String, T>,
    val type: KClass<*>,
) : Contextable {

    fun copyWithModeAliases(aliases: Map<String, String>) = copy(
        byMode = byMode.mapKeys { (mode, _) -> aliases[mode] ?: mode }
    )

    fun getValue(mode: String) = byMode[mode]

    override fun toContext() = byMode.map { (key, value) ->
        val k = if (key.first().isDigit()) "_$key" else key
        k.snake() to value.toContext()
    }.toMap() + mapOf(
        "name" to name.toNameObject(),
        "modes" to byMode.entries.map { (mode, value) ->
            mapOf(
                "name" to mode.toNameObject(),
            ) + value.toContext()
        }
    )

    fun toContext(defaultMode: String) = toContext() +
            (byMode[defaultMode] ?: byMode.values.firstOrNull()).let {
                if (it is Contextable) {
                    it.toContext()
                } else {
                    mapOf("value" to it.toString())
                }
            }

    private fun Any.toContext(): Map<String, Any> = if (this is Contextable) {
        toContext()
    } else {
        mapOf("value" to toString())
    }
}