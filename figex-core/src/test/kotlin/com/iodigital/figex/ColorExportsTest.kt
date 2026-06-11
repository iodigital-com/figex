package com.iodigital.figex

import com.iodigital.figex.models.figex.FigExArgbColor
import com.iodigital.figex.models.figex.FigExConfig.Export.Colors.Appearance
import com.iodigital.figex.models.figex.FigExValue
import com.iodigital.figex.models.figex.XcodeColorAsset
import com.iodigital.figex.serializer.DefaultJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ColorExportsTest {

    // a, g, b, r order matches FigExArgbColor's constructor
    private fun color(a: Float, r: Float, g: Float, b: Float) = FigExArgbColor(a = a, g = g, b = b, r = r)

    private fun colorValue(name: String, byMode: Map<String, FigExArgbColor>) = FigExValue(
        name = name,
        byMode = byMode,
        type = FigExArgbColor::class,
    )

    private fun parse(json: String) = DefaultJson.decodeFromString<XcodeColorAsset>(json)

    // resolveColorAppearances

    @Test
    fun `WHEN mode is null THEN first available color is used`() {
        val value = colorValue("brand", mapOf("light" to color(1f, 0.1f, 0.2f, 0.3f)))
        val entries = resolveColorAppearances(value, listOf(Appearance()))

        assertEquals(1, entries.size)
        assertEquals(color(1f, 0.1f, 0.2f, 0.3f), entries.single().color)
        assertNull(entries.single().luminosity)
        assertNull(entries.single().contrast)
    }

    @Test
    fun `WHEN a mapped mode is missing THEN that appearance is skipped`() {
        val value = colorValue("brand", mapOf("light" to color(1f, 0f, 0f, 0f)))
        val entries = resolveColorAppearances(
            value,
            listOf(
                Appearance(mode = "light"),
                Appearance(mode = "dark", luminosity = "dark"),
            )
        )

        assertEquals(1, entries.size)
        assertNull(entries.single().luminosity)
    }

    @Test
    fun `WHEN no mapped mode has a value THEN no entries are produced`() {
        val value = colorValue("brand", mapOf("light" to color(1f, 0f, 0f, 0f)))
        val entries = resolveColorAppearances(value, listOf(Appearance(mode = "dark", luminosity = "dark")))

        assertTrue(entries.isEmpty())
    }

    // buildColorContentsJson

    @Test
    fun `WHEN single base appearance THEN one universal entry without appearances tag`() {
        val json = buildColorContentsJson(
            listOf(ColorAppearanceEntry(luminosity = null, contrast = null, color = color(1f, 0.5f, 0.25f, 0f)))
        )
        val asset = parse(json)

        assertEquals(1, asset.colors.size)
        val entry = asset.colors.single()
        assertNull(entry.appearances)
        assertEquals("universal", entry.idiom)
        assertEquals("srgb", entry.color.colorSpace)
        assertEquals("1.000", entry.color.components.alpha)
        assertEquals("0.500", entry.color.components.red)
        assertEquals("0.250", entry.color.components.green)
        assertEquals("0.000", entry.color.components.blue)
        assertEquals("Figex", asset.info.author)
    }

    @Test
    fun `WHEN light and dark appearances THEN dark entry carries luminosity tag`() {
        val json = buildColorContentsJson(
            listOf(
                ColorAppearanceEntry(null, null, color(1f, 1f, 1f, 1f)),
                ColorAppearanceEntry(luminosity = "dark", contrast = null, color = color(1f, 0f, 0f, 0f)),
            )
        )
        val asset = parse(json)

        assertEquals(2, asset.colors.size)
        assertNull(asset.colors[0].appearances)
        assertEquals(
            listOf(XcodeColorAsset.Appearance(appearance = "luminosity", value = "dark")),
            asset.colors[1].appearances
        )
    }

    @Test
    fun `WHEN luminosity and contrast are set THEN both tags are emitted`() {
        val json = buildColorContentsJson(
            listOf(ColorAppearanceEntry(luminosity = "dark", contrast = "high", color = color(1f, 0f, 0f, 0f)))
        )
        val asset = parse(json)

        assertEquals(
            listOf(
                XcodeColorAsset.Appearance(appearance = "luminosity", value = "dark"),
                XcodeColorAsset.Appearance(appearance = "contrast", value = "high"),
            ),
            asset.colors.single().appearances
        )
    }
}
