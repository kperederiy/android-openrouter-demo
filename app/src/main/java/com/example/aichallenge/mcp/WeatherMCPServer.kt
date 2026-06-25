package com.example.aichallenge.mcp

import android.content.Context
import com.example.aichallenge.weather.WeatherAggregator
import com.example.aichallenge.weather.WeatherService

class WeatherMCPServer {

    private val weatherService =
        WeatherService()

    private lateinit var context: Context

    fun initialize(
        context: Context
    ) {
        this.context = context
    }

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

                    "weather_aggregated" ->
                        aggregatedWeatherTool(arguments)

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

    private fun aggregatedWeatherTool(
        args: Map<String, Any>
    ): Map<String, String> {

        val format =
            args["format"]
                ?.toString()
                ?: "text"

        val aggregator =
            WeatherAggregator(
                context
            )

        val result =
            aggregator.getWeatherSummary()

        return mapOf(
            "content" to result
        )
    }
}