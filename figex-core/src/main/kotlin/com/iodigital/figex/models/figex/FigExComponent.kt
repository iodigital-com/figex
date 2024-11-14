package com.iodigital.figex.models.figex

import com.iodigital.figex.models.Contextable
import com.iodigital.figex.toNameObject

data class FigExComponent(
    val setKey: String?,
    val key: String,
    val setName: String?,
    val name: String,
    val setId: String?,
    val id: String
) : Contextable {

    val fullName = listOfNotNull(setName, name).joinToString("/")

    override fun toContext() = mapOf(
        "name" to name,
        "set_name" to (setName ?: ""),
        "full_name" to fullName,
        "set_key" to (setKey ?: ""),
        "key" to key,
        "id" to id,
        "set_id" to (setId ?: ""),
        "normalized_name" to fullName.map { if (it.isLetterOrDigit()) it else "_" }
            .joinToString("")
            .toNameObject(),
    )
}