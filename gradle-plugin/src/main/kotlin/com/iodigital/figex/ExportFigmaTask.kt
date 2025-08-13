package com.iodigital.figex

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class ExportFigmaTask : DefaultTask() {


    override fun getDescription() = "Exports the Figma files"

    private val extension = project.extensions.findByType(FigExExtension::class.java)

    @TaskAction
    fun export() {
        with(extension ?: FigExExtension(project)) {
            requireNotNull(
                configFiles.orEmpty().plus(configFile).filterNotNull().takeUnless { it.isEmpty() }
            ) {
                "FigEx config file not configured, add `figex { configFile = File(...) }`"
            }.distinctBy { it.absolutePath }.forEach { configFile ->
                FigEx.exportBlocking(
                    configFile = configFile,
                    figmaToken = requireNotNull(figmaToken) { "Figma token not configured, add `figex { figmaToken = File(...).readText().trim() }`" },
                    debugLogs = debugLogs,
                    verboseLogs = verboseLogs,
                    ignoreUnsupportedLinks = ignoreUnsupportedLinks,
                )
            }
        }
    }
}