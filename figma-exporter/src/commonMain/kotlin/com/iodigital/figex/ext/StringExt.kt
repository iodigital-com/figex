package com.iodigital.figex.ext


private val camelRegex = Regex("(_.)")
private val snakeRegex = Regex("(_+)")

internal fun String.snake() = map {
    if (it.isLetter() || it.isDigit()) {
        it.lowercase()
    } else {
        "_"
    }
}.joinToString("").replace(snakeRegex, "_")

internal fun String.camel() = snake().replace(camelRegex) {
    it.value.drop(1).uppercase()
}

internal fun String.kebab() = snake().replace("_", "-")

internal fun String.pascal() = camel().replaceFirstChar { it.uppercase() }