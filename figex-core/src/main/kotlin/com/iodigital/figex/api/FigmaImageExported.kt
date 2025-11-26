package com.iodigital.figex.api

import com.iodigital.figex.models.figex.FigExIconFormat
import java.io.OutputStream

interface FigmaImageExporter {
    suspend fun downloadImages(
        ids: List<String>,
        scale: Float,
        format: FigExIconFormat,
        out: suspend (String, suspend (OutputStream) -> Unit) -> Unit
    )
}