package com.iodigital.figex.models.figma

import kotlinx.serialization.Serializable

@Serializable
data class FigmaImageExport(
    val err: String?,
    val images: Map<String, String>,
)