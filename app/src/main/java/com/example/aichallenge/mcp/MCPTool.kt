package com.example.aichallenge.mcp

data class MCPTool(
    val name: String,
    val description: String,
    val parameters: List<MCPParameter>
)

data class MCPParameter(
    val name: String,
    val type: String,
    val description: String,
    val required: Boolean = true
)