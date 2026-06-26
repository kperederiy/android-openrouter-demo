package com.example.aichallenge.weather

import android.content.Context

class WeatherSearchService(
    private val context: Context
) {

    private val aggregator =
        WeatherAggregator(context)

    fun search(
        query: String,
        range: String
    ): Map<String, Any> {

        return try {

            val data = when (range) {

                "48h" ->
                    aggregator.getLast48Hours()

                "7d" ->
                    aggregator.getLast7Days()

                else ->
                    aggregator.getLast24Hours()
            }

            mapOf(

                "status" to "success",

                "content" to
                        "Найдено ${data.size} записей",

                "count" to data.size,

                "query" to query,

                "range" to range,

                "timestamp" to
                        System.currentTimeMillis(),

                "data" to data
            )

        } catch (e: Exception) {

            mapOf(

                "status" to "error",

                "content" to
                        (e.message ?: "Ошибка поиска")
            )
        }
    }
}