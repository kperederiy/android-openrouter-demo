package com.example.aichallenge

import kotlinx.serialization.Serializable

@Serializable
data class Document(

    val fileName: String,

    val source: String,

    val title: String,

    val content: String
)