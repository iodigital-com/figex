package com.iodigital.figex.ext

import com.iodigital.figex.api.FigmaApi
import com.iodigital.figex.models.figma.FigmaNodesList
import com.iodigital.figex.models.figma.FigmaVariableValue

internal suspend fun FigmaNodesList.resolveNestedReferences(api: FigmaApi): FigmaNodesList {
    val includedIds = nodes.keys.toList()
    val nestedReferences = nodes.flatMap { (_, value) ->
        val referenced = value.document.valuesByMode?.mapNotNull { (_, value) ->
            if (value is FigmaVariableValue.Reference) value.reference else null
        } ?: emptyList()
        val bound = value.document.boundVariables?.flatMap { (_, value) ->
            value.mapNotNull {
                if (it is FigmaVariableValue.Reference) it.reference else null
            }
        } ?: emptyList()
        referenced + bound
    }

    return if (nestedReferences.isEmpty()) {
        this
    } else {
        val resolvedNested = api.loadNodes(nestedReferences.map { it.plainId }.toSet())
        val nodes = nodes.mapValues { (_, value) ->
            val mappedValues = value.document.valuesByMode?.mapValues { (mode, value) ->
                value.resolveSingle(mode, this + resolvedNested)
            }

            val boundVariablesByMode = value.document.boundVariables?.mapValues { (key, values) ->
                require(values.size == 1) { "Expected excactly one value for ${value.document.name} -> boundVariables -> $key but got ${values.size}" }
                values.first().resolveMulti(resolvedNested)
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

private fun FigmaVariableValue.resolveSingle(mode: String, values: FigmaNodesList) =
    if (this is FigmaVariableValue.Reference) {
        val id = reference.plainId
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

private fun FigmaVariableValue.resolveMulti(values: FigmaNodesList) =
    if (this is FigmaVariableValue.Reference) {
        val id = reference.plainId
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