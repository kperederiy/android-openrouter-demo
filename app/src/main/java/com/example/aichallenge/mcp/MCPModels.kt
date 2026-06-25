package com.example.aichallenge.mcp

data class JsonRpcRequest(
    val jsonrpc: String = "2.0",
    val method: String,
    val params: Map<String, Any>,
    val id: String
)

data class JsonRpcResponse(
    val jsonrpc: String = "2.0",
    val result: Any?,
    val error: Any?,
    val id: String
)