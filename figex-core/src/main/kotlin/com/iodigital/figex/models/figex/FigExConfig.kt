package com.iodigital.figex.models.figex

import com.iodigital.figex.models.Contextable
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable

@Serializable
data class FigExConfig(
    val figmaFileKey: String,
    val modeAliases: Map<String, String> = emptyMap(),
    val exports: List<@Polymorphic Export>,
    val templates: Map<String, String> = emptyMap(),
) {
    @Serializable
    sealed class Export {

        @Serializable
        data class Values(
            val templatePath: String,
            val destinationPath: String = "",
            val destinationPaths: List<String> = emptyList(),
            val defaultMode: String? = null,
            val filter: String = "true",
            val templateVariables: Map<String, String> = emptyMap(),
        ) : Export()

        @Serializable
        data class Icons(
            val componentPrefix: String = "",
            val destinationPath: String = "",
            val destinationPaths: List<String> = emptyList(),
            val clearDestination: Boolean = false,
            val filter: String = "true",
            val fileNames: String = "{{ full_name }}",
            val format: FigExIconFormat,
            val rasterScales: List<Scale>? = null,
            val useAndroidRasterScales: Boolean = false,
            val useXcodeAssetCompanionFile: Boolean = false,
            val companionFileName: String? = null,
            val companionFileTemplatePath: String? = null,
        ) : Export() {
            companion object {
                val androidScales = listOf(
                    Scale(1f, "drawable-mdpi/"),
                    Scale(1.5f, "drawable-hdpi/"),
                    Scale(2f, "drawable-xhdpi/"),
                    Scale(3f, "drawable-xxhdpi/"),
                    Scale(4f, "drawable-xxxhdpi/"),
                )

                internal const val COMPANION_FILENAME_XCODE_ASSETS = "Contents.json"
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

        @Serializable
        data class Colors(
            val destinationPath: String = "",
            val destinationPaths: List<String> = emptyList(),
            val clearDestination: Boolean = false,
            val filter: String = "true",
            val fileNames: String = "{{ name.original }}",
            val appearances: List<Appearance> = listOf(Appearance()),
            val templatePath: String? = null,
        ) : Export() {

            /**
             * Maps a Figma [mode] to an Xcode color-asset appearance. An entry with neither
             * [luminosity] nor [contrast] is the base ("any appearance") color. When [mode] is
             * `null` the first available mode of the color is used.
             */
            @Serializable
            data class Appearance(
                val mode: String? = null,
                val luminosity: String? = null,
                val contrast: String? = null,
            )
        }
    }
}