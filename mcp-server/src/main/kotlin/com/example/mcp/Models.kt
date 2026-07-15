package com.example.mcp

data class McpRequest(

    val tool: String,

    val arguments: Map<String, String> = emptyMap()

)

data class McpResponse(

    val success: Boolean,

    val result: String

)