package com.iodigital.figex.ext


private val camelRegex = Regex("(_.)")
private val snakeRegex = Regex("(_+)")

internal fun String.snake() = mapIndexed { index, ch ->
    when {
        ch.isLetter() && ch.isUpperCase() && index != 0 -> {
            "_${ch.lowercase()}"
        }
        ch.isLetter() || ch.isDigit() -> {
            ch.lowercase()
        }
        else -> {
            "_"
        }
    }
}.joinToString("").replace(snakeRegex, "_")

internal fun String.camel() = snake().replace(camelRegex) {
    it.value.drop(1).uppercase()
}

internal fun String.kebab() = snake().replace("_", "-")

internal fun String.pascal() = camel().replaceFirstChar { it.uppercase() }