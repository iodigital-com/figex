package com.iodigital.figex.api

import com.iodigital.figex.models.figma.FigmaNode
import com.iodigital.figex.models.figma.FigmaVariableReference
import com.iodigital.figex.models.figma.FigmaVariableValue
import com.iodigital.figex.models.figma.FigmaVariableValueCollection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AliasResolutionTest {

    private val tokens = "VariableCollectionId:2:2"
    private val theme = "VariableCollectionId:5:2"
    private val primitives = "VariableCollectionId:1:2"

    private val brandA = "2:0"
    private val brandB = "2:1"
    private val light = "5:0"
    private val dark = "5:1"
    private val primitive = "1:0"

    private val red = color(1f)
    private val blue = color(0.5f)
    private val green = color(0.25f)

    private fun color(r: Float) = FigmaVariableValue.ColorValue(a = 1f, r = r, g = 0f, b = 0f)

    private fun ref(id: String) = FigmaVariableValue.Reference(
        FigmaVariableReference(type = "VARIABLE_ALIAS", id = "VariableID:$id")
    )

    private fun variable(
        collection: String,
        vararg values: Pair<String, FigmaVariableValue>,
    ) = FigmaNode(
        document = FigmaNode.NodeDocument(
            resolvedType = FigmaNode.ResolvedType.Color,
            name = values.toString(),
            variableCollectionId = collection,
            valuesByMode = values.toMap(),
        ),
        components = emptyMap(),
        componentSets = emptyMap(),
    )

    private fun Map<String, FigmaNode>.resolved(id: String) =
        resolveGraph(this, ignoreUnsupportedLinks = false)
            .getValue(id).document.valuesByMode

    @Test
    fun `GIVEN cross collection chain WHEN resolving THEN mode of first collection sticks`() {
        // Tokens -> Theme -> Tokens -> Primitives
        val graph = mapOf(
            "p:red" to variable(primitives, primitive to red),
            "p:blue" to variable(primitives, primitive to blue),
            "p:green" to variable(primitives, primitive to green),
            "t:brand-color" to variable(tokens, brandA to ref("p:red"), brandB to ref("p:blue")),
            "t:brand-dark" to variable(tokens, brandA to ref("p:green"), brandB to ref("p:green")),
            "th:surface" to variable(theme, light to ref("t:brand-color"), dark to ref("t:brand-dark")),
            "t:label" to variable(tokens, brandA to ref("th:surface"), brandB to ref("th:surface")),
        )

        assertEquals(
            expected = mapOf(brandA to red, brandB to blue),
            actual = graph.resolved("t:label"),
        )
    }

    @Test
    fun `GIVEN collection not yet in chain WHEN resolving THEN default mode is used`() {
        val graph = mapOf(
            "p:red" to variable(primitives, primitive to red),
            "p:blue" to variable(primitives, primitive to blue),
            "th:surface" to variable(theme, light to ref("p:red"), dark to ref("p:blue")),
            "t:label" to variable(tokens, brandA to ref("th:surface"), brandB to ref("th:surface")),
        )

        assertEquals(
            expected = mapOf(brandA to red, brandB to red),
            actual = graph.resolved("t:label"),
        )
    }

    @Test
    fun `GIVEN same collection alias WHEN resolving THEN mode is kept`() {
        val graph = mapOf(
            "t:base" to variable(tokens, brandA to red, brandB to blue),
            "t:alias" to variable(tokens, brandA to ref("t:base"), brandB to ref("t:base")),
        )

        assertEquals(
            expected = mapOf(brandA to red, brandB to blue),
            actual = graph.resolved("t:alias"),
        )
    }

    @Test
    fun `GIVEN single mode target WHEN resolving THEN its only value is used`() {
        val graph = mapOf(
            "p:red" to variable(primitives, primitive to red),
            "t:alias" to variable(tokens, brandA to ref("p:red"), brandB to ref("p:red")),
        )

        assertEquals(
            expected = mapOf(brandA to red, brandB to red),
            actual = graph.resolved("t:alias"),
        )
    }

    @Test
    fun `GIVEN cyclic alias WHEN resolving THEN fails`() {
        val graph = mapOf(
            "t:a" to variable(tokens, brandA to ref("t:b")),
            "t:b" to variable(tokens, brandA to ref("t:a")),
        )

        assertFailsWith<IllegalArgumentException> {
            graph.resolved("t:a")
        }
    }

    @Test
    fun `GIVEN text style with bound variable WHEN resolving THEN value per mode is produced`() {
        val style = FigmaNode(
            document = FigmaNode.NodeDocument(
                type = FigmaNode.Type.Text,
                name = "Body",
                boundVariables = mapOf(
                    "fontSize" to FigmaVariableValueCollection.List(listOf(ref("th:surface"))),
                ),
            ),
            components = emptyMap(),
            componentSets = emptyMap(),
        )
        val graph = mapOf(
            "p:red" to variable(primitives, primitive to red),
            "p:blue" to variable(primitives, primitive to blue),
            "th:surface" to variable(theme, light to ref("p:red"), dark to ref("p:blue")),
            "style" to style,
        )

        assertEquals(
            expected = mapOf("fontSize" to mapOf(light to red, dark to blue)),
            actual = resolveGraph(graph, ignoreUnsupportedLinks = false)
                .getValue("style").document.boundValuesByMode,
        )
    }

    @Test
    fun `GIVEN graph with referenced nodes WHEN resolving selected ids THEN only those are returned`() {
        val graph = mapOf(
            "p:red" to variable(primitives, primitive to red),
            "t:alias" to variable(tokens, brandA to ref("p:red")),
        )

        assertEquals(
            expected = setOf("t:alias"),
            actual = resolveGraph(graph, ignoreUnsupportedLinks = false, ids = setOf("t:alias")).keys,
        )
    }
}
