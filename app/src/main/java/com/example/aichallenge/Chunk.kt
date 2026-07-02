package com.example.aichallenge

import kotlinx.serialization.Serializable

@Serializable
data class Chunk(

    val chunkId: String,

    val source: String,

    val fileName: String,

    val title: String,

    val section: String,

    val text: String,

    val embedding: List<Float> = emptyList()
)