package com.iodigital.figex.api

import com.iodigital.figex.serializer.DefaultJson
import com.iodigital.figex.utils.verbose
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json

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

expect fun createPlatformClient(block: HttpClientConfig<*>.() -> Unit): HttpClient