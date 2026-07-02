package com.example.aichallenge

import kotlinx.serialization.Serializable

@Serializable
data class EmbeddingResponse(

    val data: List<EmbeddingData>
)