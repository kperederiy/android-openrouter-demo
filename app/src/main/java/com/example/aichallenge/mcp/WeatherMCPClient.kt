package com.example.aichallenge.mcp

import android.content.Context

class WeatherMCPClient (
    context: Context
) {

    private val server =
        WeatherMCPServer(context)

    suspend fun callTool(
        name: String,
        arguments: Map<String, Any>
    ): String {

        val result =
            server.handleRequest(
                "tools/call",
                mapOf(
                    "name" to name,
                    "arguments" to arguments
                )
            )

        return result["content"] ?: ""
    }

    suspend fun getAggregatedWeather(): String {

        return callTool(
            "weather_aggregated",
            mapOf(
                "range" to "24h",
                "format" to "text"
            )
        )
    }
}