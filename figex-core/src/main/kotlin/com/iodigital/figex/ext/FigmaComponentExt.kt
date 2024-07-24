package com.iodigital.figex.ext

import com.iodigital.figex.models.figex.FigExComponent
import com.iodigital.figex.models.figma.FigmaComponent
import com.iodigital.figex.models.figma.FigmaComponentSet

internal fun FigmaComponent.asFigExComponent(
    id: String,
    set: Pair<String, FigmaComponentSet>? = null
) = FigExComponent(
    setKey = set?.second?.key,
    setId = set?.first,
    setName = set?.second?.name,
    key = key,
    id = id,
    name = name,
)