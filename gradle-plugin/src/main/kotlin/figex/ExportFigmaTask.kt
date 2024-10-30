package figex

import com.iodigital.figex.FigEx
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class ExportFigmaTask : DefaultTask() {


    override fun getDescription() = "Exports the Figma files"

    private val extension = project.extensions.findByType(FigExExtension::class.java)

    @TaskAction
    fun export() {
        with(extension ?: FigExExtension()) {
            FigEx.exportBlocking(
                configFile = requireNotNull(configFile) { "Figma config file not configured, add `figex { configFile = File(...) }`" },
                figmaToken = requireNotNull(figmaToken) { "Figma token not configured, add `figex { figmaToken = File(...).readText().trim() }`" },
                debugLogs = debugLogs,
                verboseLogs = verboseLogs,
                ignoreUnsupportedLinks = ignoreUnsupportedLinks,
            )
        }
    }
}