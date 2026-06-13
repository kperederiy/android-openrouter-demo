package com.example.aichallenge

data class DialogBranch(
    val id: String,
    val messages: MutableList<ChatMessage> = mutableListOf()
)