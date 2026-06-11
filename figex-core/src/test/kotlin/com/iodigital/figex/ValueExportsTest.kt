package com.iodigital.figex

import com.iodigital.figex.models.figex.FigExArgbColor
import com.iodigital.figex.models.figex.FigExConfig
import com.iodigital.figex.models.figex.FigExValue
import com.iodigital.figex.models.figma.FigmaChild
import com.iodigital.figex.models.figma.FigmaFile
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValueExportsTest {

    private fun figmaFile() = FigmaFile(
        document = FigmaChild(id = "0", type = "DOCUMENT", name = "doc"),
        schemaVersion = 0,
        name = "Test",
        lastModified = "now",
        version = "1",
        fileKey = "key",
    )

    private fun color(name: String, r: Float, g: Float, b: Float) = FigExValue(
        name = name,
        byMode = mapOf("light" to FigExArgbColor(a = 1f, g = g, b = b, r = r)),
        type = FigExArgbColor::class,
    )

    private fun floatValue(name: String, value: Float) = FigExValue(
        name = name,
        byMode = mapOf("light" to value),
        type = Float::class,
    )

    private fun newRoot(): File = File.createTempFile("figex-values", "").let {
        it.delete()
        it.mkdirs()
        it
    }

    @Test
    fun `WHEN fileNames is set THEN one file is written per value passing the filter`() {
        val root = newRoot()
        File(root, "color.txt.figex").writeText("name={{ name.original }} r={{ r }} type={{ type }}")

        val export = FigExConfig.Export.Values(
            templatePath = "color.txt.figex",
            destinationPath = "out",
            defaultMode = "light",
            fileNames = "{{ name.snake }}.txt",
            filter = "{% if type == 'color' %} true {% else %} false {% endif %}",
        )

        performValuesExport(
            root = root,
            export = export,
            file = figmaFile(),
            values = listOf(
                color("Brand Primary", r = 1f, g = 0f, b = 0f),
                color("Brand Secondary", r = 0f, g = 1f, b = 0f),
                floatValue("Spacing Small", 4f),
            ),
            components = emptyList(),
            templates = emptyMap(),
        )

        val out = File(root, "out")
        val primary = File(out, "brand_primary.txt")
        val secondary = File(out, "brand_secondary.txt")

        assertTrue(primary.exists())
        assertTrue(secondary.exists())
        // The float must be filtered out -> no third file
        assertEquals(setOf("brand_primary.txt", "brand_secondary.txt"), out.list()!!.toSet())
        assertEquals("name=Brand Primary r=1.0 type=color", primary.readText())
    }

    @Test
    fun `WHEN fileNames is null THEN a single combined file is written`() {
        val root = newRoot()
        File(root, "all.txt.figex").writeText("{% for color in colors %}{{ color.name.original }};{% endfor %}")

        val export = FigExConfig.Export.Values(
            templatePath = "all.txt.figex",
            destinationPath = "all.txt",
            defaultMode = "light",
        )

        performValuesExport(
            root = root,
            export = export,
            file = figmaFile(),
            values = listOf(color("One", 0f, 0f, 0f), color("Two", 0f, 0f, 0f)),
            components = emptyList(),
            templates = emptyMap(),
        )

        val single = File(root, "all.txt")
        assertTrue(single.exists())
        assertFalse(File(root, "one.txt").exists())
        assertEquals("One;Two;", single.readText())
    }
}
