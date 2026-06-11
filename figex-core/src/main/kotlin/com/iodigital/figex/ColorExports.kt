package com.iodigital.figex

import com.iodigital.figex.ext.findFileNames
import com.iodigital.figex.ext.findFilter
import com.iodigital.figex.models.figex.FigExArgbColor
import com.iodigital.figex.models.figex.FigExConfig
import com.iodigital.figex.models.figex.FigExValue
import com.iodigital.figex.models.figex.XcodeColorAsset
import com.iodigital.figex.utils.info
import com.iodigital.figex.utils.verbose
import com.iodigital.figex.utils.warning
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Locale

private const val tag = "FigEx/Colors"

// Xcode color sets list every field explicitly (color-space, idiom, info), so defaults must be
// encoded. Two-space indent matches the format Xcode writes.
@OptIn(ExperimentalSerializationApi::class)
private val colorAssetJson = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
    encodeDefaults = true
    // Omit `appearances` entirely for the base color instead of writing `"appearances": null`,
    // which Xcode rejects.
    explicitNulls = false
}

/**
 * A single resolved Xcode appearance for a color: the [luminosity]/[contrast] tags (both `null`
 * for the base "any appearance" color) and the [color] value to use for it.
 */
internal data class ColorAppearanceEntry(
    val luminosity: String?,
    val contrast: String?,
    val color: FigExArgbColor,
) {
    fun toContext() = mapOf(
        "luminosity" to (luminosity ?: ""),
        "contrast" to (contrast ?: ""),
        "color" to (color.toContext() + mapOf(
            "red" to color.r.asXcodeComponent(),
            "green" to color.g.asXcodeComponent(),
            "blue" to color.b.asXcodeComponent(),
            "alpha" to color.a.asXcodeComponent(),
        ))
    )
}

internal fun performColorExport(
    root: File,
    export: FigExConfig.Export.Colors,
    values: List<FigExValue<*>>,
    templates: Map<String, String>,
) {
    //region Make destination
    val destinations = export.destinationPaths.takeIf { it.isNotEmpty() } ?: listOf(export.destinationPath)
    val destinationRoots = destinations.map { root.makeChild(it) }
    info(tag = tag, "  Creating destinations: ${destinationRoots.map { it.absolutePath }}")
    destinationRoots.forEach { it.mkdirs() }
    // Catalog level Contents.json so the folder is a valid `.xcassets`
    destinationRoots.forEach { it.makeChild("Contents.json").writeText(xcodeAssetsFolderContentJSON) }
    //endregion

    val colors = values.filter { it.type == FigExArgbColor::class }.distinctBy { it.name }
    val filter = export.findFilter(templates)
    val fileNamesTemplate = export.findFileNames(templates)
    val overrideTemplate = export.templatePath?.let { root.makeChild(it).readText() }

    var exported = 0
    colors.forEach { color ->
        val context = color.toContext()
        if (!filter(filter = filter, context = context)) return@forEach

        val entries = resolveColorAppearances(color, export.appearances)
        if (entries.isEmpty()) {
            warning(tag, "  Skipping color '${color.name}': no value for any configured appearance mode")
            return@forEach
        }

        val name = jinjava.render(fileNamesTemplate, context).trim().replace("\n", "")
        val contents = overrideTemplate?.let {
            jinjava.render(it, context + mapOf("appearances" to entries.map { entry -> entry.toContext() }))
        } ?: buildColorContentsJson(entries)

        destinationRoots.forEach { destRoot ->
            val colorSet = destRoot.makeChild("$name.colorset")
            colorSet.mkdirs()
            colorSet.makeChild("Contents.json").writeText(contents)
            verbose(tag = tag, message = "  Exported color set: ${colorSet.absolutePath}")
        }
        exported++
    }
    info(tag = tag, "  Exported $exported color set(s)")
}

/**
 * Resolves the configured [appearances] against a [color], dropping any appearance whose source
 * mode has no value for this color.
 */
internal fun resolveColorAppearances(
    color: FigExValue<*>,
    appearances: List<FigExConfig.Export.Colors.Appearance>,
): List<ColorAppearanceEntry> = appearances.mapNotNull { appearance ->
    // A specified mode that has no value is skipped; only a null mode falls back to the first value.
    val value = if (appearance.mode != null) color.getValue(appearance.mode) else color.byMode.values.firstOrNull()
    (value as? FigExArgbColor)?.let {
        ColorAppearanceEntry(
            luminosity = appearance.luminosity,
            contrast = appearance.contrast,
            color = it,
        )
    }
}

/** Builds a valid `*.colorset/Contents.json` (sRGB, float components) for the given [entries]. */
internal fun buildColorContentsJson(entries: List<ColorAppearanceEntry>): String {
    val asset = XcodeColorAsset(
        colors = entries.map { entry ->
            val tags = buildList {
                entry.luminosity?.let { add(XcodeColorAsset.Appearance(appearance = "luminosity", value = it)) }
                entry.contrast?.let { add(XcodeColorAsset.Appearance(appearance = "contrast", value = it)) }
            }.ifEmpty { null }

            XcodeColorAsset.Entry(
                appearances = tags,
                color = XcodeColorAsset.Color(
                    components = XcodeColorAsset.Components(
                        alpha = entry.color.a.asXcodeComponent(),
                        red = entry.color.r.asXcodeComponent(),
                        green = entry.color.g.asXcodeComponent(),
                        blue = entry.color.b.asXcodeComponent(),
                    )
                )
            )
        }
    )
    return colorAssetJson.encodeToString(asset)
}

private fun Float.asXcodeComponent() = String.format(Locale.US, "%.3f", this)
