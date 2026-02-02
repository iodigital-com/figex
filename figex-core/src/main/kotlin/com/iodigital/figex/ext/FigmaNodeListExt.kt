package com.iodigital.figex.ext

import com.iodigital.figex.api.FigmaApi
import com.iodigital.figex.exceptions.UnsupportedExternalLinkException
import com.iodigital.figex.models.figma.FigmaNodesList
import com.iodigital.figex.models.figma.FigmaVariableReference
import com.iodigital.figex.models.figma.FigmaVariableValue
import com.iodigital.figex.models.figma.FigmaVariableValueCollection
import com.iodigital.figex.utils.warning
import kotlin.collections.mapNotNullTo

internal suspend fun FigmaNodesList.resolveNestedReferences(
    api: FigmaApi,
    path: List<String>,
    ignoreUnsupportedLinks: Boolean,
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
        val resolvedNested = api.loadNodes(nestedReferences.mapNotNull { it.plainIdOrNull(ignoreUnsupportedLinks) }.toSet())
        val nodes = nodes.mapValues { (key, value) ->
            val mappedValues = value.document.valuesByMode?.mapNotNull { (mode, value) ->
                value.resolveSingle(mode, this + resolvedNested, path + key, ignoreUnsupportedLinks)?.let { resolvedValue ->
                    mode to resolvedValue
                }
            }?.toMap()

            val boundVariablesByMode = value.document.boundVariables?.mapValues { (key, values) ->
                val resolvedValues = values.mapNotNull { it.resolveMulti(resolvedNested, path + key, ignoreUnsupportedLinks) } + emptyMap()
                resolvedValues.reduceRight { map, acc -> acc + map }
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
    path: List<String>,
    ignoreUnsupportedLinks: Boolean,
): FigmaVariableValue? =
    if (this is FigmaVariableValue.Reference) {
        val id = reference.atPath(path).plainIdOrNull(ignoreUnsupportedLinks) ?: return null
        val resolved = requireNotNull(values.nodes[id]) {
            "Missing resolved value for $id"
        }
        val valueForMode = resolved.document.valuesByMode?.get(mode)
        requireNotNull(valueForMode ?: resolved.document.valuesByMode?.values?.first()) {
            "Failed to get first value for ${resolved.document.name}"
        }
    } else {
        this
    }

private fun FigmaVariableValue.resolveMulti(
    values: FigmaNodesList,
    path: List<String>,
    ignoreUnsupportedLinks: Boolean,
): Map<String, FigmaVariableValue>? =
    if (this is FigmaVariableValue.Reference) {
        val id = reference.atPath(path).plainIdOrNull(ignoreUnsupportedLinks) ?: return null
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