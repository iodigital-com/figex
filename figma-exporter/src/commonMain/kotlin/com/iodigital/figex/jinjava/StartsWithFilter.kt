package com.iodigital.figex.jinjava

import com.hubspot.jinjava.interpret.JinjavaInterpreter
import com.hubspot.jinjava.lib.filter.Filter

internal class StartsWithFilter : Filter {
    override fun getName() = "startsWith"

    override fun filter(`var`: Any?, interpreter: JinjavaInterpreter?, vararg args: String?): Any {
        return `var`.toString().startsWith(
            prefix = args[0] ?: "",
            ignoreCase = args.getOrNull(1)?.toBoolean() ?: false
        )
    }
}