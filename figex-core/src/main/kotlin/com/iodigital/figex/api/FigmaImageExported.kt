package com.iodigital.figex.api

import com.iodigital.figex.models.figex.FigExIconFormat
import java.io.OutputStream

interface FigmaImageExporter {
    suspend fun downloadImage(id: String, scale: Float, format: FigExIconFormat, out: OutputStream)
}