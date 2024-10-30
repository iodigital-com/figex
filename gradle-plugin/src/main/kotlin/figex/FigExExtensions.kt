package figex

import java.io.File

open class FigExExtension {
    var configFile: File? = null
    var figmaToken: String? = null
    var debugLogs: Boolean = false
    var verboseLogs: Boolean = false
    var ignoreUnsupportedLinks: Boolean = false
}