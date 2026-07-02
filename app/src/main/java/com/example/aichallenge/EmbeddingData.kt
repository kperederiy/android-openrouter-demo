package com.example.aichallenge

import kotlinx.serialization.Serializable

@Serializable
data class EmbeddingData(

    val embedding: List<Float>
)