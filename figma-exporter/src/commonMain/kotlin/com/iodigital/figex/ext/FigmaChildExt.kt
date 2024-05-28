package com.iodigital.figex.ext

import com.iodigital.figex.models.figma.FigmaChild
import com.iodigital.figex.models.figma.FigmaVariableReference


internal fun FigmaChild.walk() = doWalk(listOf()).distinctBy { it.id }

private fun FigmaChild.doWalk(path: List<String>): List<FigmaVariableReference> {
    return boundVariables + children.flatMap { it.doWalk(path + name) }
}