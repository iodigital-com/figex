package com.iodigital.figex

import com.iodigital.figex.utils.critical
import com.iodigital.figex.utils.status
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds

private const val tokenEnvVar = "FIGMA_TOKEN"

fun main(args: Array<String>): Unit = runBlocking {
    try {
        val parser = ArgParser(programName = "figex")

        val token = System.getenv(tokenEnvVar)
            ?: File(".figmatoken").takeIf { it.exists() }?.readText()
            ?: let {
                println("Missing Figma token as FIGMA_TOKEN environment variable")
                exitProcess(127)
            }
        val configPath by parser.option(
            ArgType.String,
            shortName = "c",
            fullName = "config",
            description = "FigEx config path"
        ).required()
        val debug by parser.option(
            ArgType.Boolean,
            shortName = "d",
            fullName = "debug-logs",
            description = "Enable debug logs",
        )
        val verbose by parser.option(
            ArgType.Boolean,
            shortName = "v",
            fullName = "verbose-logs",
            description = "Enable debug and verbose logs",
        )
        val status by parser.option(
            ArgType.Boolean,
            shortName = "s",
            fullName = "no-status",
            description = "Hide live status",
        )
        val ignoreUnsupportedLinks by parser.option(
            ArgType.Boolean,
            shortName = "i",
            fullName = "ignore-unsupported-external-links",
            description = "Ignore unsupported external links",
        )
        parser.parse(args)


        FigEx.export(
            configFile = File(configPath),
            figmaToken = token,
            debugLogs = debug == true,
            verboseLogs = verbose == true,
            showStatus = status != true,
            ignoreUnsupportedLinks = ignoreUnsupportedLinks == true,
        )

        exitProcess(0)
    } catch (e: Exception) {
        // Delay so other logs are written out and we get a clear stack trace
        delay(1.seconds)
        status("", finalStatus = true)
        critical(tag = "Main", throwable = e, message = "Uncaught exception")
        exitProcess(128)
    }
}