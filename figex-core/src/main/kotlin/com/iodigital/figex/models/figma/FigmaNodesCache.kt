package com.iodigital.figex.models.figma

import kotlinx.serialization.Serializable

@Serializable
internal data class FigmaNodesCache(
    val lastModified: String,
    val nodes: Map<String, FigmaNode>
)
