package com.example.aichallenge.mcp

import org.json.JSONObject

class MockMCPClient {

    fun getTools(
        onSuccess: (List<MCPTool>) -> Unit,
        onError: (String) -> Unit
    ) {

        try {

            val response =
                MockMCPServer.getToolsResponse()

            val json =
                JSONObject(response)

            val toolsArray =
                json.getJSONObject("result")
                    .getJSONArray("tools")

            val tools =
                mutableListOf<MCPTool>()

            for (i in 0 until toolsArray.length()) {

                val tool =
                    toolsArray.getJSONObject(i)

                tools.add(
                    MCPTool(
                        tool.getString("name"),
                        tool.getString("description")
                    )
                )
            }

            onSuccess(tools)

        } catch (e: Exception) {

            onError(e.message ?: "Unknown error")
        }
    }
}