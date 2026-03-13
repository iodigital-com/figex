package com.iodigital.figex.ext

import com.iodigital.figex.models.figex.FigExConfig

internal fun FigExConfig.Export.findFilter(templates: Map<String, String>): String = when (this) {
    is FigExConfig.Export.Icons -> this.filter
    is FigExConfig.Export.Values -> this.filter
}.findTemplateOrThis(templates)

internal fun FigExConfig.Export.Icons.findFileNames(templates: Map<String, String>): String =
    fileNames.findTemplateOrThis(templates)

private fun String.findTemplateOrThis(templates: Map<String, String>): String {
    if (startsWith("$")) {
        val key = drop(1)
        return templates.getOrElse(key) {
            throw IllegalArgumentException("Filter with id '$key' does not exist in the filter map")
        }
    }
    return this
}