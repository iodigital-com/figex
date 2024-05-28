package com.iodigital.figex.jinjava

import com.hubspot.jinjava.interpret.JinjavaInterpreter
import com.hubspot.jinjava.lib.filter.Filter

internal class LowercaseFilter : Filter {
    override fun getName() = "lowercase"

    override fun filter(`var`: Any?, interpreter: JinjavaInterpreter?, vararg args: String?): Any {
        return `var`.toString().lowercase()
    }
}