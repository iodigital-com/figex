package com.iodigital.figex

import org.gradle.api.Project
import java.io.File

open class FigExExtension(@Suppress("UNUSED_PARAMETER") project: Project) {
    var configFiles: List<File>? = null
    var configFile: File? = null
    var figmaToken: String? = null
    var debugLogs: Boolean = FigEx.DEFAULT_DEBUG_LOGS
    var verboseLogs: Boolean = FigEx.DEFAULT_VERBOSE_LOGS
    var ignoreUnsupportedLinks: Boolean = FigEx.DEFAULT_IGNORE_UNSUPPORTED_LINKS
    var showStatus: Boolean = FigEx.DEFAULT_SHOW_STATUS
    var idsChunkSize: Int = FigEx.DEFAULT_IDS_CHUNK_SIZE
}