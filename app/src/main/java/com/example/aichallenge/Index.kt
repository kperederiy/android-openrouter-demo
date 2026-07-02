package com.example.aichallenge

import kotlinx.serialization.Serializable

@Serializable
data class Index(

    val chunks: List<Chunk>
)