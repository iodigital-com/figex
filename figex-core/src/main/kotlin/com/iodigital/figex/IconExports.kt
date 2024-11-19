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
    val destinationRoot = root.makeChild(export.destinationPath)
    info(tag = tag, "  Creating destination: ${destinationRoot.absolutePath}")
    destinationRoot.mkdirs()
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

        ExportSet(
            component = component,
            scale = scale,
            name = "${scale.namePrefix}$name${scale.nameSuffix}.${export.format.suffix}",
            context = context
        )
    }.also { export ->
        export.distinctBy { it.component.key }
    }.map { exportSet ->
        async {
            val start = System.currentTimeMillis()
            verbose(
                tag = tag,
                message = "  Downloading: ${exportSet.component.fullName}@${exportSet.scale.scale}x"
            )

            val outFile = destinationRoot.makeChild(exportSet.name)

            downloadImage(
                export = export,
                exportSet = exportSet,
                outFile = outFile,
                exporter = exporter
            )
            generateCompanionFile(
                export = export,
                exportSet = exportSet,
                outFile = outFile,
                root = root
            )

            debug(
                tag = tag,
                message = "  Downloaded: ${exportSet.component.fullName}@${exportSet.scale.scale}x => ${outFile.absolutePath} (${System.currentTimeMillis() - start}ms)"
            )
        }
    }.forEach { deferred ->
        deferred.await()
    }
    //endregion
}

private suspend fun downloadImage(
    export: FigExConfig.Export.Icons,
    exportSet: ExportSet,
    outFile: File,
    exporter: FigmaImageExporter
) {
    outFile.parentFile.mkdirs()
    outFile.outputStream().use { out ->
        exporter.downloadImage(
            id = exportSet.component.id,
            format = export.format,
            out = out,
            scale = exportSet.scale.scale,
        )
    }
}

private fun generateCompanionFile(
    export: FigExConfig.Export.Icons,
    exportSet: ExportSet,
    outFile: File,
    root: File
) {
    val fileName = export.companionFileName
        ?: COMPANION_FILENAME_XCODE_ASSETS.takeIf { export.useXcodeAssetCompanionFile }
        ?: return

    val fileContent = export.companionFileTemplatePath?.let { root.makeChild(it).readText() }
        ?: xcodeAssetsContentJSON

    verbose(tag = tag, message = "  Generating companion file: ${exportSet.component.fullName}")
    val companionFile = outFile.parentFile.makeChild(fileName)
    companionFile.parentFile.mkdirs()

    if (export.useXcodeAssetCompanionFile) {
        val parent = outFile.parentFile.parentFile
        val parentContentsJSON = parent.makeChild("Contents.json")
        parentContentsJSON.writeText(xcodeAssetsFolderContentJSON)
    }

    val companionFileContent = jinjava.render(
        fileContent,
        exportSet.context + mapOf(
            "file_name" to exportSet.name,
            "file_name_relative" to (outFile.relativeToOrNull(companionFile)
                ?.normalizeAndRelativize()?.path ?: exportSet.name)
        )
    )

    companionFile.writeText(companionFileContent)
}

private data class ExportSet(
    val component: FigExComponent,
    val scale: FigExConfig.Export.Icons.Scale,
    val name: String,
    val context: Map<String, Any>
)
