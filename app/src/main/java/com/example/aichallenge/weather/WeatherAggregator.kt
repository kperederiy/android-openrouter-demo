package com.example.aichallenge.weather

import android.content.Context
import java.time.Instant

class WeatherAggregator(
    context: Context
) {

    private val store =
        WeatherDataStore(context)

    fun getLast24Hours(): List<WeatherRecord> {

        val now =
            Instant.now()

        return store.loadAll().filter {

            val recordTime =
                Instant.parse(it.timestamp)

            now.epochSecond -
                    recordTime.epochSecond <=
                    86400
        }
    }

    fun getAverageTemperature(): Double {

        val data =
            getLast24Hours()

        if (data.isEmpty()) {
            return 0.0
        }

        return data.map {
            it.temperature
        }.average()
    }

    fun getMinMax(): Pair<Double, Double> {

        val data =
            getLast24Hours()

        if (data.isEmpty()) {
            return Pair(0.0, 0.0)
        }

        return Pair(
            data.minOf {
                it.temperature
            },
            data.maxOf {
                it.temperature
            }
        )
    }

    fun getWeatherSummary(): String {

        val data =
            getLast24Hours()

        if (data.isEmpty()) {
            return "Нет данных за последние 24 часа"
        }

        val avg =
            getAverageTemperature()

        val (min, max) =
            getMinMax()

        val avgWind =
            data.map {
                it.windSpeed
            }.average()

        return """
Погода в Брянске за последние 24 часа

Записей: ${data.size}

Средняя температура: %.1f°C

Минимум: %.1f°C
Максимум: %.1f°C

Средний ветер: %.1f м/с
        """.trimIndent().format(
            avg,
            min,
            max,
            avgWind
        )
    }
}