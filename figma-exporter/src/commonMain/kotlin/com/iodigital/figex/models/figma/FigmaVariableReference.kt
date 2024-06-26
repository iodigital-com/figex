package com.iodigital.figex.models.figma

import kotlinx.serialization.Serializable

@Serializable
internal data class FigmaVariableReference(
    val type: String,
    val id: String,
) {
    fun atPath(path: List<String>) = WithPath(
        type = type,
        id = id,
        path = path
    )

    data class WithPath(
        val type: String,
        val id: String,
        val path: List<String>,
    ) {
        val plainId
            get() = if (id.contains("/")) {
                throw UnsupportedOperationException(
                    """
                        References to external libraries are not supported at the moment, see https://github.com/iodigital-com/figex/issues/1
                        Affected variable: ${(path + id).joinToString(" -> ")}
                    """.trimIndent()
                )
            } else {
                id.removePrefix("VariableID:")
            }
    }
}