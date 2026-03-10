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
import com.iodigital.figex.utils.warning
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

private const val tag = "FigEx"

@Suppress("Unused", "MemberVisibilityCanBePrivate")
object FigEx {

    const val DEFAULT_DEBUG_LOGS = false
    const val DEFAULT_VERBOSE_LOGS = false
    const val DEFAULT_IGNORE_UNSUPPORTED_LINKS = false
    const val DEFAULT_SHOW_STATUS = true
    const val DEFAULT_IDS_CHUNK_SIZE = 60

    fun exportBlocking(
        configFile: File,
        figmaToken: String,
        debugLogs: Boolean = DEFAULT_DEBUG_LOGS,
        verboseLogs: Boolean = DEFAULT_VERBOSE_LOGS,
        ignoreUnsupportedLinks: Boolean = DEFAULT_IGNORE_UNSUPPORTED_LINKS,
        showStatus: Boolean = DEFAULT_SHOW_STATUS,
        idsChunkSize: Int = DEFAULT_IDS_CHUNK_SIZE,
    ) = runBlocking {
        export(
            configFile = configFile,
            figmaToken = figmaToken,
            debugLogs = debugLogs,
            verboseLogs = verboseLogs,
            showStatus = showStatus,
            ignoreUnsupportedLinks = ignoreUnsupportedLinks,
            idsChunkSize = idsChunkSize,
        )
    }

    suspend fun export(
        configFile: File,
        figmaToken: String,
        debugLogs: Boolean = DEFAULT_DEBUG_LOGS,
        verboseLogs: Boolean = DEFAULT_VERBOSE_LOGS,
        ignoreUnsupportedLinks: Boolean = DEFAULT_IGNORE_UNSUPPORTED_LINKS,
        showStatus: Boolean = DEFAULT_SHOW_STATUS,
        idsChunkSize: Int = DEFAULT_IDS_CHUNK_SIZE,
    ) {
        com.iodigital.figex.utils.showStatus = showStatus
        com.iodigital.figex.utils.debugLogs = debugLogs
        com.iodigital.figex.utils.verboseLogs = verboseLogs

        doExport(
            configFile = configFile,
            figmaToken = figmaToken,
            ignoreUnsupportedLinks = ignoreUnsupportedLinks,
            idsChunkSize = idsChunkSize,
        )
    }

    internal suspend fun doExport(
        configFile: File,
        figmaToken: String,
        ignoreUnsupportedLinks: Boolean,
        idsChunkSize: Int,
    ) = withContext(Dispatchers.IO) {
        val exportScope = CoroutineScope(Dispatchers.IO)
        try {
            startStatusAnimation(exportScope)
            info(tag = tag, "Using cache at $cacheDir")

            if (!configFile.exists()) {
                throw IllegalArgumentException("Config file does not exist: ${configFile.absolutePath}")
            }

            //filter out comments
            val configJson = configFile
                .readText()
                .lineSequence()
                .filter { !it.trim().startsWith("//") }
                .joinToString("\n")
            val config = ConfigJson.decodeFromString<FigExConfig>(configJson)
            val api = FigmaApi(
                    token = figmaToken,
                    fileKey = config.figmaFileKey,
                    scope = exportScope,
                    ignoreUnsupportedLinks = ignoreUnsupportedLinks,
                    idsChunkSize = idsChunkSize,
                )
            val file = loadFigmaFile(config = config, api = api)
            val components = async {
                loadComponents(api = api, file = file).distinctBy { it.key }
            }
            val values = async {
                loadValues(config = config, api = api, file = file)
            }

            listOf(
                launch {
                    performValueExports(
                        root = configFile.absoluteFile.parentFile,
                        file = file,
                        config = config,
                        values = values.await(),
                        components = components.await(),
                    )
                },
                launch {
                    performIconExports(
                        root = configFile.absoluteFile.parentFile,
                        file = file,
                        config = config,
                        components = components.await(),
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
        components: List<FigExComponent>,
    ) = config.exports.mapNotNull {
        it as? FigExConfig.Export.Values
    }.forEach {
        performValuesExport(
            export = it,
            file = file,
            values = values,
            components = components,
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
        val iconExports = config.exports.mapNotNull {
            it as? FigExConfig.Export.Icons
        }

        // Clear all destinations first, multiple might have same destination
        iconExports.forEach { export ->
            if (export.clearDestination) {
                val destinations = export.destinationPaths.takeIf { it.isNotEmpty() } ?: listOf(export.destinationPath)
                val destinationRoots = destinations.map {
                    root.makeChild(it)
                }
                warning(tag = tag, "  Clearing destination: ${destinationRoots.map { it.absolutePath }}")
                destinationRoots.forEach {
                    it.deleteRecursively()
                }
            }
        }

        iconExports.map { export ->
            launch {
                performIconExport(
                    export = export,
                    file = file,
                    components = components,
                    root = root,
                    exporter = exporter,
                )
            }
        }.joinAll()
    }
}