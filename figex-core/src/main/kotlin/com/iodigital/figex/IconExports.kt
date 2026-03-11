package com.iodigital.figex

import com.iodigital.figex.api.FigmaApi
import com.iodigital.figex.api.FigmaImageExporter
import com.iodigital.figex.ext.asFigExComponent
import com.iodigital.figex.models.figex.FigExComponent
import com.iodigital.figex.models.figex.FigExConfig
import com.iodigital.figex.models.figex.FigExConfig.Export.Icons.Companion.COMPANION_FILENAME_XCODE_ASSETS
import com.iodigital.figex.models.figma.FigmaFile
import com.iodigital.figex.utils.debug
import com.iodigital.figex.utils.info
import com.iodigital.figex.utils.verbose
import io.ktor.util.normalizeAndRelativize
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
    val destinations = export.destinationPaths.takeIf { it.isNotEmpty() } ?: listOf(export.destinationPath)
    val destinationRoots = destinations.map {
        root.makeChild(it)
    }
    info(tag = tag, "  Creating destinations: ${destinationRoots.map { it.absolutePath }}")
    destinationRoots.forEach { it.mkdirs() }
    //endregion
    //region Scales
    val scales = export.rasterScales.takeIf { export.format.isRaster }
        ?: FigExConfig.Export.Icons.androidScales.takeIf { export.useAndroidRasterScales && export.format.isRaster }
        ?: listOf(FigExConfig.Export.Icons.Scale(1f))
    //endregion
    //region Export files
    components.asSequence().flatMap { component ->
        scales.map {
            Triple(component, it, createTemplateContext(file, it, component))
        }
    }.filter { (_, _, context) ->
        filter(filter = export.filter, context = context)
    }.toList().map { (component, scale, context) ->
        val name = jinjava.render(export.fileNames, context).trim().replace("\n", "")

        ComponentExport(
            component = component,
            scale = scale,
            name = "${scale.namePrefix}$name${scale.nameSuffix}.${export.format.suffix}",
            context = context
        )
    }.distinctBy {
        it.component.key to it.scale
    }.groupBy {
        it.scale
    }.toList().map { (scale, exportSets) ->
        async {
            val start = System.currentTimeMillis()
            verbose(
                tag = tag,
                message = "  Downloading with ${scale.scale}x: ${exportSets.joinToString { it.component.fullName }}"
            )

            val exportSetsWithFiles = exportSets.map { componentExport ->
                componentExport to destinationRoots.map { it.makeChild(componentExport.name) }
            }

            downloadImages(
                export = export,
                componentExports = exportSetsWithFiles.map { (e, files) -> e to files.first() },
                exporter = exporter,
                scale = scale,
            )

            exportSetsWithFiles.forEach { (exportSet, outFiles) ->
                val primaryFile = outFiles.first()
                outFiles.drop(1).forEach { additionalFile ->
                    additionalFile.parentFile.mkdirs()
                    primaryFile.copyTo(additionalFile, overwrite = true)
                }

                outFiles.forEach { outFile ->
                    generateCompanionFile(
                        export = export,
                        componentExport = exportSet,
                        outFile = outFile,
                        root = root
                    )
                }

                debug(
                    tag = tag,
                    message = "  Downloaded: ${exportSet.component.fullName}@${exportSet.scale.scale}x => ${outFiles.joinToString { it.absolutePath }} (${System.currentTimeMillis() - start}ms)"
                )
            }
        }
    }.forEach { deferred ->
        deferred.await()
    }
    //endregion
}

private suspend fun downloadImages(
    export: FigExConfig.Export.Icons,
    scale: FigExConfig.Export.Icons.Scale,
    componentExports: List<Pair<ComponentExport, File>>,
    exporter: FigmaImageExporter
) {
    componentExports.map { (_, file) -> file.parentFile }.distinct().forEach { it.mkdirs() }

    exporter.downloadImages(
        ids = componentExports.map { it.first.component.id },
        format = export.format,
        scale = scale.scale,
        out = { id, download ->
            val (_, outFile) = requireNotNull(componentExports.first { (exportSet, _) -> exportSet.component.id == id }) { "No export set for id $id found" }
            outFile.outputStream().use {
                download(it)
            }
        }
    )
}

private fun generateCompanionFile(
    export: FigExConfig.Export.Icons,
    componentExport: ComponentExport,
    outFile: File,
    root: File
) {
    val fileName = export.companionFileName
        ?: COMPANION_FILENAME_XCODE_ASSETS.takeIf { export.useXcodeAssetCompanionFile }
        ?: return

    val fileContent = export.companionFileTemplatePath?.let { root.makeChild(it).readText() }
        ?: xcodeAssetsContentJSON

    verbose(
        tag = tag,
        message = "  Generating companion file: ${componentExport.component.fullName}"
    )
    val companionFile = outFile.parentFile.makeChild(fileName)
    companionFile.parentFile.mkdirs()

    if (export.useXcodeAssetCompanionFile) {
        val parent = outFile.parentFile.parentFile
        val parentContentsJSON = parent.makeChild("Contents.json")
        parentContentsJSON.writeText(xcodeAssetsFolderContentJSON)
    }

    val companionFileContent = jinjava.render(
        fileContent,
        componentExport.context + mapOf(
            "file_name" to componentExport.name,
            "file_name_relative" to (outFile.relativeToOrNull(companionFile)
                ?.normalizeAndRelativize()?.path ?: componentExport.name)
        )
    )

    companionFile.writeText(companionFileContent)
}

private data class ComponentExport(
    val component: FigExComponent,
    val scale: FigExConfig.Export.Icons.Scale,
    val name: String,
    val context: Map<String, Any>
)
