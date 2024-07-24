package com.iodigital.figex.jinjava

import com.hubspot.jinjava.interpret.JinjavaInterpreter
import com.hubspot.jinjava.lib.filter.Filter

internal class ReplaceSpecialChars : Filter {
    override fun getName() = "replaceSpecialChars"

    override fun filter(`var`: Any?, interpreter: JinjavaInterpreter?, vararg args: String?): Any {
        return `var`.toString().map {
            if (it.isLetterOrDigit()) it else (args.firstOrNull() ?: "")
        }.joinToString("")
    }
}