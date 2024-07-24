package com.iodigital.figex.models.figma

import kotlinx.serialization.Serializable

@Serializable
internal data class FigmaNodesList(
    val nodes: Map<String, FigmaNode>
) {
    operator fun plus(other: FigmaNodesList): FigmaNodesList = FigmaNodesList(
        nodes + other.nodes
    )
}