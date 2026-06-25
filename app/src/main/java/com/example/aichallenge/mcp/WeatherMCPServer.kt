package com.example.aichallenge.mcp

import com.example.aichallenge.weather.WeatherService

class WeatherMCPServer {

    private val weatherService =
        WeatherService()

    suspend fun handleRequest(
        method: String,
        params: Map<String, Any>
    ): Map<String, String> {

        return when (method) {

            "tools/call" -> {

                val toolName =
                    params["name"] as String

                val arguments =
                    params["arguments"] as Map<String, Any>

                when (toolName) {

                    "weather" ->
                        weatherTool(arguments)

                    else ->
                        mapOf(
                            "content" to "Unknown tool"
                        )
                }
            }

            else ->
                mapOf(
                    "content" to "Unknown method"
                )
        }
    }

    private suspend fun weatherTool(
        args: Map<String, Any>
    ): Map<String, String> {

        val city =
            args["city"]?.toString()
                ?: error("city required")

        val result =
            weatherService.getWeather(city)

        return mapOf(
            "content" to result
        )
    }
}