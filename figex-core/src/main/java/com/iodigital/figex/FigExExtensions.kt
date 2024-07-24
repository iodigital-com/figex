package com.iodigital.figex

import org.gradle.api.Project
import java.io.File

open class FigExExtension(project: Project) {
    var configFile: File? = null
    var figmaToken: String? = null
    var debugLogs: Boolean = false
    var verboseLogs: Boolean = false
    var ignoreUnsupportedLinks: Boolean = false
}