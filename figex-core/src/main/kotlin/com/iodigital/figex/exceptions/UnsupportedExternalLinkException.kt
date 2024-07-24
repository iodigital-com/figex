package com.iodigital.figex.exceptions

class UnsupportedExternalLinkException(
    path: List<String>,
    id: String,
    usage: String?,
) : Exception() {

    val description = "${(path).joinToString(" -> ")} | $usage = $id"

    override val message: String
        get() = """
        References to external libraries are not supported at the moment, see https://github.com/iodigital-com/figex/issues/1
        Run figex with -i or set ignoreUnsupportedLinks to true to ignore this error 
        
        Affected variable: $description
    """.trimIndent()
}