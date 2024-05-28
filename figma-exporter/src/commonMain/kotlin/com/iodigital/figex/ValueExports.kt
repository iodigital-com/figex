package com.iodigital.figex

import com.iodigital.figex.api.FigmaApi
import com.iodigital.figex.ext.walk
import com.iodigital.figex.models.figex.FigExArgbColor
import com.iodigital.figex.models.figex.FigExConfig
import com.iodigital.figex.models.figex.FigExTextStyle
import com.iodigital.figex.models.figex.FigExValue
import com.iodigital.figex.models.figma.FigmaFile
import com.iodigital.figex.utils.info
import com.iodigital.figex.utils.verbose
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

    info(tag, "Loading variables and styles:")
    info(tag, "Colors: ${values.count { it.type == FigExArgbColor::class }}")
    info(tag, "Floats: ${values.count { it.type == Float::class }}")
    info(tag, "Strings: ${values.count { it.type == String::class }}")
    info(tag, "Text styles: ${values.count { it.type == FigExTextStyle::class }}")

    values.forEach {
        verbose(tag = tag, message = "  ${it.name} (${it.type})")
    }

    values
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
        values = values
    ) + export.templateVariables
    val template = root.makeChild(export.templatePath)
    val destination = root.makeChild(export.destinationPath)
    info(tag = tag, "  ${template.absolutePath} => ${destination.absolutePath}...")
    val result = jinjava.render(template.readText(), context)
    destination.writeText(result)
}
