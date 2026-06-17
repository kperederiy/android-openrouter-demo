package com.example.aichallenge

data class MemoryLayers(

    val shortTermMemory:
    MutableList<ChatMessage> =
        mutableListOf(),

    val workingMemory:
    MutableMap<String, String> =
        mutableMapOf(),

    val longTermMemory:
    MutableMap<String, String> =
        mutableMapOf()
)