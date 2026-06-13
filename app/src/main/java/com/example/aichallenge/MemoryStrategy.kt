package com.example.aichallenge

enum class MemoryStrategy(
    val title: String
) {
    SLIDING_WINDOW("Sliding"),
    STICKY_FACTS("Sticky Facts"),
    BRANCHING("Branching")
}