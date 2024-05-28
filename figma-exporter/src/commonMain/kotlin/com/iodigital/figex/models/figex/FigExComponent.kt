package com.iodigital.figex.models.figex

import com.iodigital.figex.models.Contextable

data class FigExComponent(
    val setKey: String?,
    val key: String,
    val setName: String?,
    val name: String,
    val setId: String?,
    val id: String
) : Contextable {

    val fullName = listOf(setName, name).joinToString("/")

    override fun toContext() = mapOf(
        "name" to name,
        "setName" to (setName ?: ""),
        "fullName" to fullName,
        "setKey" to (setKey ?: ""),
        "key" to key,
        "id" to id,
        "setId" to (setId ?: ""),
    )
}