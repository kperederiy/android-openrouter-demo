package com.example.aichallenge

data class AgentStats(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
    val historyTokens: Int,
    val estimatedCost: Double,
    val contextUsagePercent: Int,
    val contextWarning: String,
    val strategy: String
)