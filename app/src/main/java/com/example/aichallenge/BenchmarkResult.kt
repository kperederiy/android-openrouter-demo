package com.example.aichallenge

import kotlinx.serialization.Serializable

@Serializable
data class BenchmarkResult(

    val id: Int,

    val question: String,

    val simpleAnswer: String,

    val ragAnswer: String,

    val simpleTimeMs: Long,

    val ragTimeMs: Long
)