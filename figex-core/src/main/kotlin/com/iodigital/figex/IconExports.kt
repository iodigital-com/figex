package com.iodigital.figex

import com.iodigital.figex.api.FigmaApi
import com.iodigital.figex.api.FigmaImageExporter
import com.iodigital.figex.ext.asFigExComponent
import com.iodigital.figex.models.figex.FigExComponent
import com.iodigital.figex.models.figex.FigExConfig
import com.iodigital.figex.models.figex.FigExIconFormat
import com.iodigital.figex.models.figma.FigmaFile
import com.iodigital.figex.utils.debug
import com.iodigital.figex.utils.info
import com.iodigital.figex.utils.verbose
import com.iodigital.figex.utils.warning
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.File

private const val tag = "FigEx/Icons"

suspend fun loadComponents(
    file: FigmaFile,
    api: FigmaApi,
) = api.loadComponentsFromSets(file.componentSets.keys) + file.components.map { (id, component) ->
    component.asFigExComponent(id = id)
}.also {
    info(tag, "Components: ${it.size}")
}

internal suspend fun performIconExport(
    root: File,
    export: FigExConfig.Export.Icons,
    file: FigmaFile,
    components: List<FigExComponent>,
    exporter: FigmaImageExporter,
) = withContext(Dispatchers.IO) {
    //region Make destination
    val destinationRoot = root.makeChild(export.destinationPath)
    if (export.clearDestination) {
        warning(tag = tag, "  Clearing destination: ${destinationRoot.absolutePath}")
        destinationRoot.deleteRecursively()
    }
    info(tag = tag, "  Creating destination: ${destinationRoot.absolutePath}")
    destinationRoot.mkdirs()
    //endregion
    //region Scales
    val scales = export.rasterScales.takeIf { export.format.isRaster }
        ?: FigExConfig.Export.Icons.androidScales.takeIf { export.useAndroidRasterScales && export.format.isRaster }
        ?: listOf(FigExConfig.Export.Icons.Scale(1f))
    //endregion
    // Export files
    components.asSequence().flatMap { component ->
        scales.map {
            Triple(component, it, createTemplateContext(file, it, component))
        }
    }.filter { (_, _, context) ->
        filter(filter = export.filter, context = context)
    }.toList().map { (component, scale, context) ->
        val name = jinjava.render(export.fileNames, context)
            .trim()
            .replace("\n", "")

        Triple(
            component,
            scale,
            "${scale.namePrefix}$name${scale.nameSuffix}.${export.format.suffix}"
        )
    }.also {
        it.distinctBy { it.first.key }
    }.map { (component, scale, name) ->
        async {
            val start = System.currentTimeMillis()
            verbose(tag = tag, message = "  Downloading: ${component.fullName}@${scale.scale}x")
            val outFile = destinationRoot.makeChild(name)
            outFile.parentFile.mkdirs()
            outFile.outputStream().use { out ->
                exporter.downloadImage(
                    id = component.id,
                    format = export.format,
                    out = out,
                    scale = scale.scale,
                )
            }
            debug(
                tag = tag,
                message = "  Downloaded: ${component.fullName}@${scale.scale}x => ${outFile.absolutePath} (${System.currentTimeMillis() - start}ms)"
            )
        }
    }.forEach { deferred ->
        deferred.await()
    }
    //endregion
}