package com.iodigital.figex

import com.hubspot.jinjava.Jinjava
import com.hubspot.jinjava.JinjavaConfig
import com.iodigital.figex.jinjava.LowercaseFilter
import com.iodigital.figex.jinjava.ReplaceSpecialChars
import com.iodigital.figex.jinjava.StartsWithFilter
import com.iodigital.figex.models.figex.FigExArgbColor
import com.iodigital.figex.models.figex.FigExComponent
import com.iodigital.figex.models.figex.FigExConfig
import com.iodigital.figex.models.figex.FigExTextStyle
import com.iodigital.figex.models.figex.FigExValue
import com.iodigital.figex.models.figma.FigmaFile
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.reflect.KClass

internal val jinjava by lazy {
    Jinjava(JinjavaConfig.newBuilder().build()).also {
        it.registerFilter(LowercaseFilter())
        it.registerFilter(ReplaceSpecialChars())
        it.registerFilter(StartsWithFilter())
    }
}

internal fun File.makeChild(path: String): File {
    val replaced = path.replace("~", System.getProperty("user.home"))
    return if (replaced.startsWith("/")) {
        File(replaced)
    } else {
        File(this, replaced)
    }
}

internal fun createTemplateContext(
    file: FigmaFile,
    defaultMode: String,
    values: List<FigExValue<*>>,
) = mapOf(
    "colors" to values.subContextFor(defaultMode, FigExArgbColor::class),
    "dimens" to values.subContextFor(defaultMode, Float::class),
    "strings" to values.subContextFor(defaultMode, String::class),
    "text_styles" to values.subContextFor(defaultMode, FigExTextStyle::class),
) + createTemplateContext(file)

internal fun createTemplateContext(
    file: FigmaFile,
    scale: FigExConfig.Export.Icons.Scale,
    component: FigExComponent,
) = component.toContext() + mapOf("scale" to scale.toContext()) + createTemplateContext(file)

private fun createTemplateContext(file: FigmaFile) = mapOf(
    "date" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'").format(Date()),
    "figma" to file.toContext()
)

private fun List<FigExValue<*>>.subContextFor(defaultMode: String, type: KClass<*>) =
    filter { it.type == type }.map { it.toContext(defaultMode) }