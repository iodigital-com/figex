package com.iodigital.figex

import com.iodigital.figex.api.FigmaApi
import com.iodigital.figex.ext.walk
import com.iodigital.figex.models.figex.FigExArgbColor
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
) {
    val context = createTemplateContext(
        file = file,
        defaultMode = export.defaultMode ?: "",
        values = values,
        filter = export.filter,
    ) + export.templateVariables
    val template = root.makeChild(export.templatePath)
    val destination = root.makeChild(export.destinationPath)
    info(tag = tag, "  ${template.absolutePath} => ${destination.absolutePath}...")
    val result = jinjava.render(template.readText(), context)
    destination.writeText(result)
}
