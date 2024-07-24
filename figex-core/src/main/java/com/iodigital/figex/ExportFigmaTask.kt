package com.iodigital.figex

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

open class ExportFigmaTask : DefaultTask() {


    override fun getDescription() = "Exports the Figma files"

    @TaskAction
    fun export() {
        val extension = project.extensions.findByType(FigExExtension::class.java)
            ?: FigExExtension(project)

        with(extension) {
            FigEx.exportBlocking(
                configFile = requireNotNull(configFile) { "Figma config file not configured, add `figex { configFile = File(...) }`" },
                figmaToken = requireNotNull(figmaToken) { "Figma token not configured, add `figex { figmaToken = File(...) }`" },
                debugLogs = debugLogs,
                verboseLogs = verboseLogs,
                ignoreUnsupportedLinks = ignoreUnsupportedLinks,
            )
        }
    }
}