package com.iodigital.figex

import com.iodigital.figex.api.FigmaApi
import com.iodigital.figex.ext.findFileNames
import com.iodigital.figex.ext.findFilter
import com.iodigital.figex.ext.walk
import com.iodigital.figex.models.figex.FigExArgbColor
import com.iodigital.figex.models.figex.FigExComponent
import com.iodigital.figex.models.figex.FigExConfig
import com.iodigital.figex.models.figex.FigExTextStyle
import com.iodigital.figex.models.figex.FigExValue
import com.iodigital.figex.models.figma.FigmaFile
import com.iodigital.figex.utils.critical
import com.iodigital.figex.utils.info
import com.iodigital.figex.utils.verbose
import com.iodigital.figex.utils.warning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.File

private const val tag = "FigEx/Values"

internal suspend fun loadValues(
    file: FigmaFile,
    api: FigmaApi,
    config: FigExConfig,
) = withContext(Dispatchers.IO) {
    val tree = file.document.walk()
    val deferredValues = listOf(
        async { api.loadVariable(tree) },
        async { api.loadTextStyles(file.styles.keys) },
    )

    val values = deferredValues.flatMap {
        it.await()
    }.map {
        it.copyWithModeAliases(config.modeAliases)
    }

    val modes = values.flatMap { it.byMode.keys }.distinct()
    val unnamedModes = modes.filter { it.matches("\\d+:\\d+".toRegex()) }

    logExportStats(values, modes)
    logUnnamedModes(values, unnamedModes)
    logValues(values)

    values
}

private fun logExportStats(values: List<FigExValue<*>>, modes: List<String>) {
    info(tag, "Loading variables and styles:")
    info(tag, "  Colors: ${values.count { it.type == FigExArgbColor::class }}")
    info(tag, "  Floats: ${values.count { it.type == Float::class }}")
    info(tag, "  Strings: ${values.count { it.type == String::class }}")
    info(tag, "  Booleans: ${values.count { it.type == Boolean::class }}")
    info(tag, "  Text styles: ${values.count { it.type == FigExTextStyle::class }}")
    info(tag, "  Available modes: ${modes.joinToString()}")
}

private fun logUnnamedModes(values: List<FigExValue<*>>, unnamedModes: List<String>) {
    if (unnamedModes.isEmpty()) return

    critical(
        tag, """
            
            Some modes do not have aliases yet!
            
            FigEx is unable to see the name you gave a mode in Figma, instead only the mode id is known. You can configured an alias for each mode in 
            the config file by adding a `modeAliases` object:
            
            {
              "figmaFileKey": "...",
              "modeAliases": {
                "mode:id": "name",
${unnamedModes.joinToString("\n") { "                \"$it\": \"name\"," }}
                ...
              },
              "exports": [
                ...
              ]
            }
                        
            Below you find a list of variables using these modes, determine the names of the missing modes
            by comparing the values to your Figma file and add the aliases to the config!
            
            
        """.trimIndent()
    )


    warning(tag, "  Unnamed modes:")
    unnamedModes.forEach {
        warning(tag, "    $it")
    }

    val sampleForEachMode = values
        .groupBy { it.byMode.keys.toList().sorted() }
        .filter { (modes, _) -> modes.any { it in unnamedModes } }

    warning(tag, "\n  Sample usages for each mode:")
    warning(
        tag,
        "\n    {variable name} => {mode id}={value for mode}, {mode id}={value for mode}, ...\n"
    )
    sampleForEachMode.forEach { (modes, samples) ->
        samples.forEach { sample ->
            val valuesByModel = modes
                .associateWith { sample.byMode[it] }
                .map { (mode, value) -> "$mode=$value" }
                .joinToString()

            warning(tag, "    ${sample.name} => $valuesByModel")
        }
    }
    warning(tag, "\n")
}

private fun logValues(values: List<FigExValue<*>>) = values.forEach {
    verbose(tag = tag, message = "  ${it.name} (${it.type})")
}

internal fun performValuesExport(
    root: File,
    export: FigExConfig.Export.Values,
    file: FigmaFile,
    values: List<FigExValue<*>>,
    components: List<FigExComponent>,
    templates: Map<String, String>,
) {
    val defaultMode = export.defaultMode ?: ""
    val filterStr = export.findFilter(templates)
    val context = createTemplateContext(
        file = file,
        defaultMode = defaultMode,
        values = values,
        components = components,
        filter = filterStr,
    ) + export.templateVariables
    val template = root.makeChild(export.templatePath)
    val destinations = export.destinationPaths.takeIf { it.isNotEmpty() } ?: listOf(export.destinationPath)
    val destinationRoots = destinations.map {
        root.makeChild(it)
    }

    if (export.fileNames == null) {
        info(tag = tag, "  ${template.absolutePath} => ${destinationRoots.map { it.absolutePath }}...")
        val result = jinjava.render(template.readText(), context)
        destinationRoots.forEach {
            it.writeText(result)
        }
        return
    }

    // Per-value mode: render the template once for each value that passes the filter, writing one
    // file per value. The current value's fields are spread on top of the shared context so the
    // template can access them directly (e.g. `name.original`, `r`, `argb`, `type`).
    destinationRoots.forEach { it.mkdirs() }
    val templateText = template.readText()
    val fileNamesTemplate = export.findFileNames(templates)

    var written = 0
    values.distinctBy { it.type to it.name }.forEach { value ->
        val itemContext = context + value.toContext(defaultMode)
        if (!filter(filter = filterStr, context = itemContext)) return@forEach

        val name = jinjava.render(fileNamesTemplate, itemContext).trim().replace("\n", "")
        val content = jinjava.render(templateText, itemContext)
        destinationRoots.forEach { destRoot ->
            val outFile = destRoot.makeChild(name)
            outFile.parentFile.mkdirs()
            outFile.writeText(content)
        }
        written++
    }
    info(tag = tag, "  Exported $written file(s) per value to ${destinationRoots.map { it.absolutePath }}")
}
