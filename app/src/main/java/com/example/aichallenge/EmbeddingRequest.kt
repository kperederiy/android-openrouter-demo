package com.example.aichallenge

import kotlinx.serialization.Serializable

@Serializable
data class EmbeddingRequest(

    val model: String,

    val input: String
)