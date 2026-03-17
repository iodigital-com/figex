package com.iodigital.figex.api

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * Figma's Images API sometimes omits fill-rule="evenodd" from SVG paths that need it,
 * causing compound paths (with cutout shapes) to render as solid blobs in Android
 * VectorDrawables. This utility detects compound paths (multiple subpaths) and adds
 * fill-rule="evenodd" when missing.
 */
object SvgCompoundPathFix {

    fun addMissingFillRuleToCompoundPaths(svgFile: File) {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(svgFile)
        val paths = doc.getElementsByTagName("path")
        var modified = false

        for (i in 0 until paths.length) {
            val path = paths.item(i) as? org.w3c.dom.Element ?: continue
            val d = path.getAttribute("d").orEmpty()
            val fillRule = path.getAttribute("fill-rule").orEmpty()

            if (fillRule.isEmpty() && hasMultipleSubpaths(d)) {
                path.setAttribute("fill-rule", "evenOdd")
                modified = true
            }
        }

        if (modified) {
            val transformer = TransformerFactory.newInstance().newTransformer()
            transformer.transform(DOMSource(doc), StreamResult(svgFile))
        }
    }

    private fun hasMultipleSubpaths(d: String): Boolean {
        var count = 0
        for (char in d) {
            if (char == 'M' || char == 'm') {
                count++
                if (count > 1) return true
            }
        }
        return false
    }
}