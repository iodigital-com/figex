package com.iodigital.figex.api

import com.android.ide.common.vectordrawable.Svg2Vector
import com.iodigital.figex.exceptions.UnsupportedExternalLinkException
import com.iodigital.figex.ext.asFigExComponents
import com.iodigital.figex.ext.asFigExTextStyle
import com.iodigital.figex.ext.asFigExValue
import com.iodigital.figex.ext.resolveNestedReferences
import com.iodigital.figex.models.figex.FigExIconFormat
import com.iodigital.figex.models.figex.FigExIconFormat.AndroidXml
import com.iodigital.figex.models.figex.FigExIconFormat.Pdf
import com.iodigital.figex.models.figex.FigExIconFormat.Png
import com.iodigital.figex.models.figex.FigExIconFormat.Svg
import com.iodigital.figex.models.figex.FigExIconFormat.Webp
import com.iodigital.figex.models.figex.FigExValue
import com.iodigital.figex.models.figma.FigmaFile
import com.iodigital.figex.models.figma.FigmaImageExport
import com.iodigital.figex.models.figma.FigmaNode
import com.iodigital.figex.models.figma.FigmaNodesList
import com.iodigital.figex.models.figma.FigmaVariableReference
import com.iodigital.figex.utils.cacheDir
import com.iodigital.figex.utils.debug
import com.iodigital.figex.utils.status
import com.iodigital.figex.utils.warning
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.jvm.javaio.copyTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.lang.Math.random
import java.util.Arrays
import java.util.Collections.emptyList
import java.util.Collections.emptyMap
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import kotlin.time.Duration.Companion.seconds

@OptIn(FlowPreview::class)
class FigmaApi(
    private val token: String,
    private val fileKey: String,
    private val scope: CoroutineScope,
    private val ignoreUnsupportedLinks: Boolean,
) : FigmaImageExporter {
    private val tag = "FigmaApi"
    private val httpClient by lazy { HttpClientFactory.createClient() }
    private val rateLimitReached = MutableStateFlow(false)
    private val requestCount = MutableStateFlow(0)
    private val queueCount = MutableStateFlow(0)
    private val requestLeases = Semaphore(32)

    init {
        scope.launch {
            combine(requestCount, queueCount, rateLimitReached) { active, queue, rateLimit ->
                listOfNotNull(
                    "$active active requests",
                    if (queue > 0) "$queue queued requests" else null,
                    if (rateLimit) "waiting for rate limit" else null
                ).joinToString()
            }.sample(1.seconds).distinctUntilChanged().collect {
                status(it)
            }
        }
    }

    private val tmpDir = File(cacheDir, "tmp").also {
        it.mkdirs()
    }

    suspend fun loadFile(): FigmaFile = httpClient.get {
        status("Loading file $fileKey")
        figmaRequest("v1/files/$fileKey")
        onDownload { bytesSentTotal, contentLength ->
            status("Loading file $fileKey (${bytesSentTotal / 1024}KiB)")
        }
    }.body<FigmaFile>().copy(
        fileKey = fileKey
    )

    internal suspend fun loadVariable(
        references: List<FigmaVariableReference.WithPath>,
    ): List<FigExValue<*>> = loadNodes(
        ids = references.mapNotNull { it.plainIdOrNull }.toSet()
    ).nodes.map { (_, node) ->
        node.asFigExValue()
    }

    internal suspend fun loadTextStyles(
        ids: Set<String>
    ) = loadNodes(
        ids = ids
    ).nodes.filter { (_, value) ->
        value.document.type == FigmaNode.Type.Text
    }.map { (_, node) ->
        node.asFigExTextStyle()
    }

    internal suspend fun loadComponentsFromSets(
        ids: Set<String>
    ) = loadNodes(
        ids = ids
    ).nodes.filter { (_, value) ->
        value.document.type == FigmaNode.Type.ComponentSet
    }.flatMap { (_, node) ->
        node.asFigExComponents()
    }

    internal suspend fun loadNodes(
        ids: Set<String>
    ) = withRateLimit {
        if (ids.isEmpty()) {
            return@withRateLimit FigmaNodesList(emptyMap())
        }

        httpClient.get {
            figmaRequest("v1/files/$fileKey/nodes")
            parameter("ids", ids.joinToString(","))
        }.body<FigmaNodesList>().let { list ->
            val withCached = list.copy(nodes = list.nodes)
            withCached.resolveNestedReferences(this, emptyList())
        }
    }

    private suspend fun <T> withRateLimit(block: suspend () -> T): T {
        try {
            queueCount.update { it + 1 }
            requestLeases.acquire()
        } finally {
            queueCount.update { it - 1 }
        }

        try {
            requestCount.update { it + 1 }

            suspend fun doRequest(): T = try {
                if (rateLimitReached.value) {
                    debug(tag = tag, message = "Rate limit reached, waiting for delay...")
                    rateLimitReached.first { !it }
                }

                block()
            } catch (e: ClientRequestException) {
                if (e.response.status.value == 429) {
                    val first = rateLimitReached.getAndUpdate { true }
                    if (!first) {
                        val delay = 10.seconds
                        warning(tag = tag, message = "Rate limit reached, delaying for $delay")
                        delay(delay)
                        rateLimitReached.update { false }
                    }
                    doRequest()
                } else {
                    throw e
                }
            }

            return doRequest()
        } finally {
            requestLeases.release()
            requestCount.update { it - 1 }
        }
    }

    private fun HttpRequestBuilder.figmaRequest(path: String) {
        url("https://api.figma.com/${path.removePrefix("/")}")
        header("X-FIGMA-TOKEN", token)
    }

    override suspend fun downloadImage(
        id: String,
        scale: Float,
        format: FigExIconFormat,
        out: OutputStream
    ) = withContext(Dispatchers.IO) {
        val downloadUrl = withRateLimit {
            httpClient.get {
                figmaRequest("v1/images/$fileKey")
                parameter("ids", id)
                parameter("scale", scale)
                parameter(
                    key = "format",
                    value = when (format) {
                        Svg, AndroidXml -> "svg"
                        Png, Webp -> "png"
                        Pdf -> "pdf"
                    }
                )
            }.body<FigmaImageExport>().let { body ->
                require(body.err == null) { "Figma reported error while loading component $id: ${body.err}" }
                requireNotNull(body.images[id]) { "Missing download url for component $id" }
            }
        }

        val tmpFile = File(tmpDir, listOf(random(), id, scale, format).hashCode().toString())
        val tmpFile2 = File(tmpDir, listOf(random(), id, scale, format).hashCode().toString())
        tmpFile.deleteOnExit()
        tmpFile2.deleteOnExit()

        try {
            requestCount.update { it + 1 }
            (if (format in listOf(Webp, AndroidXml)) tmpFile.outputStream() else out).use {
                httpClient.get(downloadUrl).bodyAsChannel().copyTo(it)
            }

            if (format == AndroidXml) {
                debug(tag = tag, message = "  Inline converting SVG => Android XML: $tmpFile")
                require(tmpFile.length() > 0) { "Empty SVG file for $id" }
                Svg2Vector.parseSvgToXml(tmpFile.toPath(), out)
            }

            if (format == Webp) {
                debug(tag = tag, message = "  Inline converting PNG => WEBP: $tmpFile")

                // Check if cwebp is installed
                checkCwebp()

                // Use ProcessBuilder to call cwebp
                val process = ProcessBuilder(
                    "cwebp",
                    "-q", "80",
                    tmpFile.absolutePath,
                    "-o", tmpFile2.absolutePath
                ).start()

                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    // Capture error output for better diagnostics
                    val errorOutput = process.errorStream.bufferedReader().use { it.readText() }
                    throw IOException("WebP conversion failed with exit code $exitCode: $errorOutput")
                }

                tmpFile2.inputStream().use {
                    it.copyTo(out)
                }
            }
        } finally {
            out.close()
            tmpFile.delete()
            tmpFile2.delete()
            requestCount.update { it - 1 }
        }
    }

    private fun checkCwebp() = try {
        val checkProcess = ProcessBuilder("which", "cwebp").start()
        val exitCode = checkProcess.waitFor()

        if (exitCode != 0) {
            // Try with 'where' command for Windows
            val windowsCheckProcess = ProcessBuilder("where", "cwebp").start()
            val windowsExitCode = windowsCheckProcess.waitFor()

            if (windowsExitCode != 0) {
                throw IOException("cwebp is not installed or not in PATH. Please install WebP tools.")
            }
        }

        Unit
    } catch (e: Exception) {
        throw IOException("Failed to check if cwebp is installed: ${e.message}", e)
    }

    private val FigmaVariableReference.WithPath.plainIdOrNull
        get() = try {
            plainId
        } catch (e: UnsupportedExternalLinkException) {
            if (ignoreUnsupportedLinks) {
                warning(tag = tag, message = "Ignoring unsupported external link: ${e.description}")
                null
            } else {
                throw e
            }
        }
}