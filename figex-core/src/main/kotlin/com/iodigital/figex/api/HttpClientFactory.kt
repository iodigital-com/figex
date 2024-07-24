package com.iodigital.figex.api

import com.iodigital.figex.serializer.DefaultJson
import com.iodigital.figex.utils.cacheDir
import com.iodigital.figex.utils.critical
import com.iodigital.figex.utils.debug
import com.iodigital.figex.utils.verbose
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import okhttp3.Cache
import java.io.File

internal object HttpClientFactory {

    fun createClient() = createPlatformClient {
        expectSuccess = true
        install(HttpCache)
        install(Logging) {
            level = LogLevel.HEADERS
            logger = object : Logger {
                override fun log(message: String) {
                    verbose(tag = "HTTP", message = message)
                }
            }
        }
        install(ContentEncoding) {
            deflate(1.0f)
            gzip(0.9f)
        }
        install(ContentNegotiation) {
            json(DefaultJson)
        }
    }
}

fun createPlatformClient(block: HttpClientConfig<*>.() -> Unit) = HttpClient(OkHttp) {
    engine {
        addNetworkInterceptor {
            // KTOR adds "Content-Length: 0" to all requests....Figma returns 400 in this case
            val baseRequest = it.request()
            val request = baseRequest.newBuilder()
                .run {
                    if (baseRequest.header(HttpHeaders.ContentLength) == "0") {
                        removeHeader(HttpHeaders.ContentLength)
                    } else {
                        this
                    }
                }
                .build()
            it.proceed(request)
        }

        addInterceptor {
            val request = it.request()
            debug(tag = "HTTP", message = "==> ${request.method} ${request.url}")
            try {
                val response = it.proceed(request)
                debug(tag = "HTTP", message = "<== ${response.code} ${request.url}")
                response
            } catch (e: Exception) {
                critical(tag = "HTTP", message = "<== ${request.url}: ${e.message}")
                throw e
            }
        }

        config {
            readTimeout(120, java.util.concurrent.TimeUnit.SECONDS).cache(
                Cache(
                    directory = File(cacheDir, "http"),
                    maxSize = 256L * 1024L * 1024L // 256 MiB
                )
            )
        }
    }

    block()
}