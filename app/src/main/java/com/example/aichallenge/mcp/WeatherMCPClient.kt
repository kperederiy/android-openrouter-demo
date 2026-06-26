package com.example.aichallenge.mcp

import android.content.Context

class WeatherMCPClient(
    context: Context
) {

    private val server =
        WeatherMCPServer(context)

    /**
     * Универсальный вызов любого MCP-инструмента.
     */
    suspend fun callTool(
        name: String,
        arguments: Map<String, Any>
    ): Map<String, Any> {

        return server.handleRequest(

            method = "tools/call",

            params = mapOf(

                "name" to name,

                "arguments" to arguments
            )
        )
    }

    /**
     * Получить текущую погоду.
     */
    suspend fun getWeather(
        city: String
    ): String {

        val result =
            callTool(

                "weather",

                mapOf(
                    "city" to city
                )
            )

        return result["content"]
            ?.toString()
            ?: ""
    }

    /**
     * SEARCH TOOL
     */
    suspend fun searchWeather(
        query: String,
        range: String
    ): Map<String, Any> {

        return callTool(

            "search",

            mapOf(

                "query" to query,

                "range" to range
            )
        )
    }

    /**
     * SUMMARIZE TOOL
     */
    suspend fun summarizeWeather(
        data: Map<String, Any>,
        format: String = "text"
    ): Map<String, Any> {

        val args =
            data.toMutableMap()

        args["format"] =
            format

        return callTool(

            "summarize",

            args
        )
    }

    /**
     * SAVE TOOL
     */
    suspend fun saveReport(
        content: String,
        filename: String =
            "weather_report.txt"
    ): Map<String, Any> {

        return callTool(

            "saveToFile",

            mapOf(

                "content" to content,

                "filename" to filename
            )
        )
    }

    /**
     * Полный MCP Pipeline.
     */
    suspend fun runPipeline(
        query: String,
        range: String,
        filename: String =
            "weather_report.txt"
    ): Map<String, Any> {

        return callTool(

            "pipeline",

            mapOf(

                "query" to query,

                "range" to range,

                "filename" to filename,

                "format" to "text"
            )
        )
    }
}