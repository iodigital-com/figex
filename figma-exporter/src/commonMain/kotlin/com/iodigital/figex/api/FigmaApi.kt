package com.iodigital.figex.api

import com.android.ide.common.vectordrawable.Svg2Vector
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import java.lang.Math.random
import javax.imageio.ImageIO
import kotlin.time.Duration.Companion.seconds

class FigmaApi(
    private val token: String,
    private val fileKey: String,
    private val scope: CoroutineScope,
) : FigmaImageExporter {
    private val tag = "FigmaApi"
    private val httpClient by lazy { HttpClientFactory.createClient() }
    private val rateLimitReached = MutableStateFlow(false)
    private val requestCount = MutableStateFlow(0)

    init {
        scope.launch {
            requestCount.combine(rateLimitReached) { count, rateLimit ->
                status("$count active requests" + if (rateLimit) ", waiting for rate limit" else "")
            }.collect()
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
    }.body()

    internal suspend fun loadVariable(
        references: List<FigmaVariableReference>
    ): List<FigExValue<*>> = withRateLimit {
        loadNodes(
            ids = references.map { it.plainId }.toSet()
        ).nodes.map { (_, node) ->
            node.asFigExValue()
        }
    }

    internal suspend fun loadTextStyles(
        ids: Set<String>
    ) = withRateLimit {
        loadNodes(
            ids = ids
        ).nodes.filter { (_, value) ->
            value.document.type == FigmaNode.Type.Text
        }.map { (_, node) ->
            node.asFigExTextStyle()
        }
    }

    internal suspend fun loadComponentsFromSets(
        ids: Set<String>
    ) = withRateLimit {
        loadNodes(
            ids = ids
        ).nodes.filter { (_, value) ->
            value.document.type == FigmaNode.Type.ComponentSet
        }.flatMap { (_, node) ->
            node.asFigExComponents()
        }
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
            withCached.resolveNestedReferences(this)
        }
    }

    private suspend fun <T> withRateLimit(block: suspend () -> T): T = try {
        requestCount.update { it + 1 }
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
            withRateLimit(block)
        } else {
            throw e
        }
    } finally {
        requestCount.update { it - 1 }
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

        try {
            requestCount.update { it + 1 }
            val tmpFile = File(tmpDir, listOf(random(), id, scale, format).hashCode().toString())
            tmpFile.deleteOnExit()
            (if (format in listOf(Webp, AndroidXml)) tmpFile.outputStream() else out).use {
                httpClient.get(downloadUrl).bodyAsChannel().copyTo(it)
            }

            if (format == AndroidXml) {
                debug(tag = tag, message = "  Inline converting SVG => Android XML: $tmpFile")
                Svg2Vector.parseSvgToXml(tmpFile.toPath(), out)
            }

            if (format == Webp) {
                debug(tag = tag, message = "  Inline converting PNG => WEBP: $tmpFile")
                val png = ImageIO.read(tmpFile)
                ImageIO.write(png, "webp", out)
            }
        } finally {
            requestCount.update { it - 1 }
        }
    }
}