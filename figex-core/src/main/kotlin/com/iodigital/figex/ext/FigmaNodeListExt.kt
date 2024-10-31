package com.iodigital.figex.ext

import com.iodigital.figex.api.FigmaApi
import com.iodigital.figex.models.figma.FigmaNodesList
import com.iodigital.figex.models.figma.FigmaVariableValue

internal suspend fun FigmaNodesList.resolveNestedReferences(
    api: FigmaApi,
    path: List<String>
): FigmaNodesList {
    val nestedReferences = nodes.flatMap { (name, value) ->
        val referenced = value.document.valuesByMode?.mapNotNull { (_, it) ->
            if (it is FigmaVariableValue.Reference) it.reference.atPath(path + name) else null
        } ?: emptyList()
        val bound = value.document.boundVariables?.flatMap { (_, it) ->
            it.mapNotNull { if (it is FigmaVariableValue.Reference) it.reference.atPath(path + name) else null }
        } ?: emptyList()

        referenced + bound
    }

    return if (nestedReferences.isEmpty()) {
        this
    } else {
        val resolvedNested = api.loadNodes(nestedReferences.map { it.plainId }.toSet())
        val nodes = nodes.mapValues { (key, value) ->
            val mappedValues = value.document.valuesByMode?.mapValues { (mode, value) ->
                value.resolveSingle(mode, this + resolvedNested, path + key)
            }

            val boundVariablesByMode = value.document.boundVariables?.mapValues { (key, values) ->
                values.map { it.resolveMulti(resolvedNested, path + key) }
                    .reduceRight { map, acc -> acc + map }
            }

            value.copy(
                document = value.document.copy(
                    valuesByMode = mappedValues,
                    boundValuesByMode = boundVariablesByMode,
                )
            )
        }

        copy(nodes = nodes)
    }
}

private fun FigmaVariableValue.resolveSingle(
    mode: String,
    values: FigmaNodesList,
    path: List<String>
) = if (this is FigmaVariableValue.Reference) {
    val id = reference.atPath(path).plainId
    val resolved = requireNotNull(values.nodes[id]) {
        "Missing resolved value for $id"
    }
    val valueForMode = resolved.document.valuesByMode?.get(mode)
    require(valueForMode != null || resolved.document.valuesByMode?.values?.size == 1) {
        "Expected a match for mode $mode or exactly one mode for ${resolved.document.name} as it's referenced by another variable"
    }
    requireNotNull(valueForMode ?: resolved.document.valuesByMode?.values?.first()) {
        "Failed to get first value for ${resolved.document.name}"
    }
} else {
    this
}

private fun FigmaVariableValue.resolveMulti(values: FigmaNodesList, path: List<String>) =
    if (this is FigmaVariableValue.Reference) {
        val id = reference.atPath(path).plainId
        val resolved = requireNotNull(values.nodes[id]) {
            "Missing resolved value for $id"
        }
        require((resolved.document.valuesByMode?.size ?: 0) > 0) {
            "Expected at least one value for ${resolved.document.name} but is 0"
        }
        requireNotNull(resolved.document.valuesByMode) {
            "Null after passed check"
        }
    } else {
        throw IllegalStateException("Expected ${FigmaVariableValue.Reference::class.simpleName} value, but was ${this::class.simpleName}")
    }