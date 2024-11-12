package com.iodigital.figex.models.figex

import com.iodigital.figex.models.Contextable
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable

@Serializable
data class FigExConfig(
    val figmaFileKey: String,
    val modeAliases: Map<String, String> = emptyMap(),
    val exports: List<@Polymorphic Export>,
) {
    @Serializable
    sealed class Export {

        @Serializable
        data class Values(
            val templatePath: String,
            val destinationPath: String,
            val defaultMode: String? = null,
            val filter: String = "true",
            val templateVariables: Map<String, String> = emptyMap(),
        ) : Export()

        @Serializable
        data class Icons(
            val componentPrefix: String = "",
            val destinationPath: String,
            val clearDestination: Boolean = false,
            val filter: String = "true",
            val fileNames: String = "{{ full_name }}",
            val format: FigExIconFormat,
            val rasterScales: List<Scale>? = null,
            val useAndroidRasterScales: Boolean = false,
        ) : Export() {
            companion object {
                val androidScales = listOf(
                    Scale(1f, "drawable-mdpi/"),
                    Scale(1.5f, "drawable-hdpi/"),
                    Scale(2f, "drawable-xhdpi/"),
                    Scale(3f, "drawable-xxhdpi/"),
                    Scale(4f, "drawable-xxxhdpi/"),
                )
            }

            @Serializable
            data class Scale(
                val scale: Float,
                val namePrefix: String = "",
                val nameSuffix: String = "",
            ) : Contextable {
                override fun toContext() = mapOf(
                    "scale" to scale,
                    "namePrefix" to namePrefix,
                    "nameSuffix" to nameSuffix
                )
            }
        }
    }
}