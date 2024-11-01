package com.iodigital.figex.models.figma

import com.iodigital.figex.models.Contextable
import kotlinx.serialization.Serializable

@Serializable
data class FigmaFile internal constructor(
    internal val document: FigmaChild,
    internal val components: Map<String, FigmaComponent> = emptyMap(),
    internal val componentSets: Map<String, FigmaComponentSet> = emptyMap(),
    internal val styles: Map<String, FigmaStyle> = emptyMap(),
    internal val schemaVersion: Int,
    internal val name: String,
    internal val lastModified: String,
    internal val version: String,
    internal val fileKey: String? = null,
) : Contextable {
    override fun toContext() = mapOf(
        "file" to name,
        "last_modified" to lastModified,
        "version" to version,
        "file_key" to (fileKey ?: ""),
        "file_url" to "https://www.figma.com/design/$fileKey",
    )
}