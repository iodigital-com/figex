package com.iodigital.figex.models.figex

import kotlin.test.Test
import kotlin.test.assertEquals

class FigExValueTest {

    private fun <T : Any> value(byMode: Map<String, T>, type: kotlin.reflect.KClass<*>) =
        FigExValue(name = "Some Name", byMode = byMode, type = type)

    @Test
    fun `WHEN value is a color THEN context type is color`() {
        val ctx = value(mapOf("light" to FigExArgbColor(a = 1f, g = 0f, b = 0f, r = 0f)), FigExArgbColor::class).toContext()
        assertEquals("color", ctx["type"])
    }

    @Test
    fun `WHEN value is a float THEN context type is float`() {
        val ctx = value(mapOf("light" to 1f), Float::class).toContext()
        assertEquals("float", ctx["type"])
    }

    @Test
    fun `WHEN value is a string THEN context type is string`() {
        val ctx = value(mapOf("light" to "hello"), String::class).toContext()
        assertEquals("string", ctx["type"])
    }

    @Test
    fun `WHEN value is a boolean THEN context type is boolean`() {
        val ctx = value(mapOf("light" to true), Boolean::class).toContext()
        assertEquals("boolean", ctx["type"])
    }

    @Test
    fun `WHEN value is a text style THEN context type is text_style`() {
        val ctx = value(mapOf("light" to FigExTextStyle()), FigExTextStyle::class).toContext()
        assertEquals("text_style", ctx["type"])
    }
}
