package com.iodigital.figex.models.figma

import com.iodigital.figex.exceptions.UnsupportedExternalLinkException
import kotlinx.serialization.Serializable

@Serializable
internal data class FigmaVariableReference(
    val type: String,
    val id: String,
    val usage: String? = null,
) {
    fun atPath(path: List<String>) = WithPath(
        type = type,
        id = id,
        path = path,
        usage = usage,
    )

    data class WithPath(
        val type: String,
        val id: String,
        val path: List<String>,
        val usage: String? = null,
    ) {
        val plainId
            get() = if (id.contains("/")) {
                throw UnsupportedExternalLinkException(path = path, usage = usage, id = id)
            } else {
                id.removePrefix("VariableID:")
            }

        fun plainIdOrNull(ignoreUnsupportedLinks: Boolean) =
            try {
                plainId
            } catch (e: UnsupportedExternalLinkException) {
                if (!ignoreUnsupportedLinks) throw e
                null
            }
    }
}