package com.iodigital.figex

import com.hubspot.jinjava.Jinjava
import com.hubspot.jinjava.JinjavaConfig
import com.iodigital.figex.ext.camel
import com.iodigital.figex.ext.kebab
import com.iodigital.figex.ext.pascal
import com.iodigital.figex.ext.snake
import com.iodigital.figex.jinjava.LowercaseFilter
import com.iodigital.figex.jinjava.ReplaceSpecialChars
import com.iodigital.figex.jinjava.StartsWithFilter
import com.iodigital.figex.models.figex.FigExArgbColor
import com.iodigital.figex.models.figex.FigExComponent
import com.iodigital.figex.models.figex.FigExConfig
import com.iodigital.figex.models.figex.FigExTextStyle
import com.iodigital.figex.models.figex.FigExValue
import com.iodigital.figex.models.figma.FigmaFile
import com.iodigital.figex.utils.verbose
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.reflect.KClass

private const val tag = "FigEx/Export"


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
    filter: String,
    values: List<FigExValue<*>>,
    components: List<FigExComponent>,
) = mapOf(
    "colors" to values.subContextFor(defaultMode, filter, FigExArgbColor::class),
    "floats" to values.subContextFor(defaultMode, filter, Float::class),
    "strings" to values.subContextFor(defaultMode, filter, String::class),
    "booleans" to values.subContextFor(defaultMode, filter, Boolean::class),
    "text_styles" to values.subContextFor(defaultMode, filter, FigExTextStyle::class),
    "icons" to components.map { it.toContext() }.filter { filter(filter = filter, context = it) }
) + createTemplateContext(file)

internal fun String.toNameObject() = mapOf(
    "original" to this,
    "snake" to this.snake(),
    "camel" to this.camel(),
    "kebab" to this.kebab(),
    "pascal" to this.pascal(),
)

internal fun createTemplateContext(
    file: FigmaFile,
    scale: FigExConfig.Export.Icons.Scale,
    component: FigExComponent,
) = component.toContext() + mapOf("scale" to scale.toContext()) + createTemplateContext(file)

private fun createTemplateContext(file: FigmaFile) = mapOf(
    "date" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'").format(Date()),
    "figma" to file.toContext()
)

private fun List<FigExValue<*>>.subContextFor(
    defaultMode: String,
    filter: String,
    type: KClass<*>,
) = filter { it.type == type }
    .distinctBy { it.name }
    .map { it.toContext(defaultMode) }
    .filter { filter(filter = filter, context = it) }

fun filter(filter: String, context: Map<String, Any>): Boolean {
    return jinjava.render(filter, context).filter { it.isLetter() }.lowercase().also {
        verbose(tag = tag, message = "Applying filter `${filter}` to `$context` => $it")
    } == "true"
}