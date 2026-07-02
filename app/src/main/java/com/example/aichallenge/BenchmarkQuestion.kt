package com.example.aichallenge

import kotlinx.serialization.Serializable

@Serializable
data class BenchmarkQuestion(

    val id: Int,

    val question: String
)