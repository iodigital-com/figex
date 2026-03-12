package com.iodigital.figex.ext

import com.iodigital.figex.models.figex.FigExConfig.Export
import com.iodigital.figex.models.figex.FigExIconFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FigExConfigExtKtTest {

    // FindFilter

    @Test
    fun `WHEN no dollar sign is present THEN literal filter for Values export is returned`() {
        val export = Export.Values(templatePath = "template.txt", filter = "name == 'color'")
        val result = export.findFilter(emptyMap())
        assertEquals("name == 'color'", result)
    }

    @Test
    fun `WHEN no dollar sign is present THEN literal filter for Icons export is returned`() {
        val export = Export.Icons(format = FigExIconFormat.Svg, filter = "name == 'icon'")
        val result = export.findFilter(emptyMap())
        assertEquals("name == 'icon'", result)
    }

    @Test
    fun `WHEN no filter is present THEN default filter value is returned`() {
        val export = Export.Values(templatePath = "template.txt")
        val result = export. findFilter(emptyMap())
        assertEquals("true", result)
    }

    @Test
    fun `WHEN reference is correct for Values export THEN correct filter is resolved`() {
        val export = Export.Values(templatePath = "template.txt", filter = "\$colors")
        val filters = mapOf("colors" to "type == 'COLOR'")
        val result = export. findFilter(filters)
        assertEquals("type == 'COLOR'", result)
    }

    @Test
    fun `WHEN reference is correct for Icons export THEN correct filter is resolved`() {
        val export = Export.Icons(format = FigExIconFormat.Svg, filter = "\$myIcons")
        val filters = mapOf("myIcons" to "component == 'icon'")
        val result = export. findFilter(filters)
        assertEquals("component == 'icon'", result)
    }

    @Test
    fun `WHEN filter does not exists THEN error is thrown`() {
        val export = Export.Values(templatePath = "template.txt", filter = "\$missing")
        assertFailsWith<IllegalArgumentException> {
            export. findFilter(emptyMap())
        }
    }

    @Test
    fun `WHEN referenced filter does not exist THEN error is thrown`() {
        val export = Export.Icons(format = FigExIconFormat.Svg, filter = "\$unknown")
        assertFailsWith<IllegalArgumentException> {
            export. findFilter(mapOf("other" to "value"))
        }
    }

    @Test
    fun `WHEN no dollar sign present THEN filter is not resolved`() {
        val export = Export.Values(templatePath = "template.txt", filter = "colors")
        val filters = mapOf("colors" to "type == 'COLOR'")
        val result = export. findFilter(filters)
        assertEquals("colors", result)
    }

    @Test
    fun `WHEN dollar sign is empty THEN nothing happens`() {
        val export = Export.Values(templatePath = "template.txt", filter = "\$")
        val filters = mapOf("" to "empty-key-value")
        val result = export. findFilter(filters)
        assertEquals("empty-key-value", result)
    }

    // FindFileNames

    @Test
    fun `WHEN fileNames has no dollar sign THEN literal filter is returned`() {
        val export = Export.Icons(format = FigExIconFormat.Svg, fileNames = "name == 'icon'")
        val result = export.findFileNames(emptyMap())
        assertEquals("name == 'icon'", result)
    }

    @Test
    fun `WHEN fileNames reference is correct THEN correct filter is resolved`() {
        val export = Export.Icons(format = FigExIconFormat.Svg, fileNames = "\$myIcons")
        val filters = mapOf("myIcons" to "component == 'icon'")
        val result = export. findFileNames(filters)
        assertEquals("component == 'icon'", result)
    }
}