package com.iodigital.figex

import com.iodigital.figex.api.FigmaApi
import com.iodigital.figex.api.FigmaImageExporter
import com.iodigital.figex.models.figex.FigExComponent
import com.iodigital.figex.models.figex.FigExConfig
import com.iodigital.figex.models.figex.FigExValue
import com.iodigital.figex.models.figma.FigmaFile
import com.iodigital.figex.serializer.ConfigJson
import com.iodigital.figex.utils.cacheDir
import com.iodigital.figex.utils.info
import com.iodigital.figex.utils.startStatusAnimation
import com.iodigital.figex.utils.status
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val tag = "FigEx"

@Suppress("Unused", "MemberVisibilityCanBePrivate")
object FigEx {

    suspend fun export(
        configFile: File,
        figmaToken: String,
        debugLogs: Boolean = false,
        verboseLogs: Boolean = false,
        showStatus: Boolean = true,
    ) {
        com.iodigital.figex.utils.showStatus = showStatus
        com.iodigital.figex.utils.debugLogs = debugLogs
        com.iodigital.figex.utils.verboseLogs = verboseLogs
        doExport(configFile = configFile, figmaToken = figmaToken)
    }

    internal suspend fun doExport(
        configFile: File,
        figmaToken: String,
    ) = withContext(Dispatchers.IO) {
        val exportScope = CoroutineScope(Dispatchers.IO)
        try {
            startStatusAnimation(exportScope)
            info(tag = tag, "Using cache at $cacheDir")

            if (!configFile.exists()) {
                throw IllegalArgumentException("Config file does not exist: ${configFile.absolutePath}")
            }

            val configJson = configFile.readText()
            val config = ConfigJson.decodeFromString<FigExConfig>(configJson)
            val api =
                FigmaApi(token = figmaToken, fileKey = config.figmaFileKey, scope = exportScope)
            val file = loadFigmaFile(config = config, api = api)
            listOf(
                launch {
                    val values = loadValues(config = config, api = api, file = file)
                    performValueExports(
                        root = configFile.parentFile,
                        file = file,
                        config = config,
                        values = values,
                    )
                },
                launch {
                    val components = loadComponents(api = api, file = file)
                    performIconExports(
                        root = configFile.parentFile,
                        file = file,
                        config = config,
                        components = components,
                        exporter = api
                    )
                }
            ).joinAll()
            status("Export completed successfully.", finalStatus = true)
        } finally {
            exportScope.cancel("Export done")
        }
    }

    suspend fun loadFigmaFile(
        config: FigExConfig,
        api: FigmaApi,
    ): FigmaFile {
        info(tag, "Loading Figma file:")
        info(tag, "  Key: ${config.figmaFileKey}")

        val file = api.loadFile()

        info(tag, "  Name: ${file.name}")
        info(tag, "  Last modified: ${file.lastModified}")
        info(tag, "  ")

        return file
    }

    fun performValueExports(
        root: File,
        file: FigmaFile,
        config: FigExConfig,
        values: List<FigExValue<*>>,
    ) = config.exports.mapNotNull {
        it as? FigExConfig.Export.Values
    }.forEach {
        performValuesExport(
            export = it,
            file = file,
            values = values,
            root = root,
        )
    }

    suspend fun performIconExports(
        root: File,
        file: FigmaFile,
        config: FigExConfig,
        components: List<FigExComponent>,
        exporter: FigmaImageExporter,
    ) = withContext(Dispatchers.IO) {
        config.exports.mapNotNull {
            it as? FigExConfig.Export.Icons
        }.map {
            launch {
                performIconExport(
                    export = it,
                    file = file,
                    components = components,
                    root = root,
                    exporter = exporter,
                )
            }
        }.joinAll()
    }
}