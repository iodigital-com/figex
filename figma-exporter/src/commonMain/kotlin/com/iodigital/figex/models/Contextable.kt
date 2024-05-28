package com.iodigital.figex.models

internal interface Contextable {
    fun toContext(): Map<String, Any>
}