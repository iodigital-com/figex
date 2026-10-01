package com.iodigital.figex.api

import com.iodigital.figex.models.figma.FigmaNode
import com.iodigital.figex.models.figma.FigmaVariableValue
import com.iodigital.figex.utils.debug
import com.iodigital.figex.utils.warning

private const val tag = "FigEx/Aliases"

/**
 * Ids of all variables referenced by this node, either as alias in [FigmaNode.NodeDocument.valuesByMode]
 * or as bound variable in [FigmaNode.NodeDocument.boundVariables].
 */
internal fun FigmaNode.referencedIds(
    id: String,
    ignoreUnsupportedLinks: Boolean,
): Set<String> {
    val referenced = document.valuesByMode?.values.orEmpty()
    val bound = document.boundVariables?.values.orEmpty().flatten()

    return (referenced + bound).mapNotNull {
        if (it is FigmaVariableValue.Reference) {
            it.reference.atPath(listOf(id)).plainIdOrNull(ignoreUnsupportedLinks)
        } else {
            null
        }
    }.toSet()
}

/**
 * Resolves all aliases of the nodes with the given [ids] against the raw node [graph].
 *
 * Aliases are resolved the way Figma does: once a mode is chosen for a collection along an alias
 * chain it sticks, so `Tokens(brand) -> Theme -> Tokens -> Primitives` resolves the second
 * `Tokens` hop with the same brand mode. A collection not yet seen in the chain uses its default
 * (first) mode.
 */
internal fun resolveGraph(
    graph: Map<String, FigmaNode>,
    ignoreUnsupportedLinks: Boolean,
    ids: Set<String> = graph.keys,
): Map<String, FigmaNode> = graph
    .filterKeys { it in ids }
    .mapValues { (id, node) -> node.resolve(id, graph, ignoreUnsupportedLinks) }

private fun FigmaNode.resolve(
    id: String,
    graph: Map<String, FigmaNode>,
    ignoreUnsupportedLinks: Boolean,
): FigmaNode {
    val collection = document.variableCollectionId

    val valuesByMode = document.valuesByMode?.mapNotNull { (mode, value) ->
        value.resolve(
            graph = graph,
            modeContext = modeContextOf(collection, mode),
            path = listOf(id),
            ignoreUnsupportedLinks = ignoreUnsupportedLinks,
        )?.let { mode to it }
    }?.toMap()

    val boundValuesByMode = document.boundVariables?.mapValues { (property, values) ->
        values.mapNotNull {
            it.resolveAllModes(
                graph = graph,
                path = listOf(id, property),
                ignoreUnsupportedLinks = ignoreUnsupportedLinks,
            )
        }.fold(emptyMap<String, FigmaVariableValue>()) { acc, byMode -> byMode + acc }
    }

    return copy(
        document = document.copy(
            valuesByMode = valuesByMode,
            boundValuesByMode = boundValuesByMode,
        )
    )
}

private fun FigmaVariableValue.resolve(
    graph: Map<String, FigmaNode>,
    modeContext: Map<String, String>,
    path: List<String>,
    ignoreUnsupportedLinks: Boolean,
): FigmaVariableValue? {
    if (this !is FigmaVariableValue.Reference) return this

    val id = reference.atPath(path).plainIdOrNull(ignoreUnsupportedLinks) ?: return null
    require(id !in path) { "Cyclic alias: ${(path + id).joinToString(" -> ")}" }
    val target = requireNotNull(graph[id]) { "Missing resolved value for $id" }
    val values = target.document.valuesByMode
    require(!values.isNullOrEmpty()) { "Expected at least one value for ${target.document.name} but is 0" }

    val collection = target.document.variableCollectionId
    val contextMode = collection?.let { modeContext[it] }
    val mode = when {
        contextMode != null && contextMode in values -> contextMode

        contextMode != null -> values.keys.first().also {
            warning(
                tag = tag,
                message = "Mode $contextMode missing for ${target.document.name} in $collection, using $it (${(path + id).joinToString(" -> ")})"
            )
        }

        values.size == 1 -> values.keys.first()

        else -> values.keys.first().also {
            debug(
                tag = tag,
                message = "Using default mode $it of $collection for ${target.document.name} (${(path + id).joinToString(" -> ")})"
            )
        }
    }

    return values.getValue(mode).resolve(
        graph = graph,
        modeContext = modeContext + modeContextOf(collection, mode),
        path = path + id,
        ignoreUnsupportedLinks = ignoreUnsupportedLinks,
    )
}

/**
 * Resolves a bound variable (e.g. of a text style) for every mode of the referenced variable.
 */
private fun FigmaVariableValue.resolveAllModes(
    graph: Map<String, FigmaNode>,
    path: List<String>,
    ignoreUnsupportedLinks: Boolean,
): Map<String, FigmaVariableValue>? {
    check(this is FigmaVariableValue.Reference) {
        "Expected ${FigmaVariableValue.Reference::class.simpleName} value, but was ${this::class.simpleName}"
    }

    val id = reference.atPath(path).plainIdOrNull(ignoreUnsupportedLinks) ?: return null
    val target = requireNotNull(graph[id]) { "Missing resolved value for $id" }
    val values = target.document.valuesByMode
    require(!values.isNullOrEmpty()) { "Expected at least one value for ${target.document.name} but is 0" }

    val collection = target.document.variableCollectionId
    return values.mapNotNull { (mode, value) ->
        value.resolve(
            graph = graph,
            modeContext = modeContextOf(collection, mode),
            path = path + id,
            ignoreUnsupportedLinks = ignoreUnsupportedLinks,
        )?.let { mode to it }
    }.toMap()
}

private fun modeContextOf(collection: String?, mode: String) =
    if (collection != null) mapOf(collection to mode) else emptyMap()
