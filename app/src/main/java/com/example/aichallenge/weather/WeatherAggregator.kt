package com.example.aichallenge.weather

import android.content.Context
import java.time.Instant

class WeatherAggregator(
    context: Context
) {

    private val store =
        WeatherDataStore(context)

    /**
     * Все записи
     */
    fun loadAll(): List<WeatherRecord> {

        return store.loadAll()
    }

    /**
     * Последние 24 часа
     */
    fun getLast24Hours(): List<WeatherRecord> {

        return filterByHours(24)
    }

    /**
     * Последние 48 часов
     */
    fun getLast48Hours(): List<WeatherRecord> {

        return filterByHours(48)
    }

    /**
     * Последние 7 суток
     */
    fun getLast7Days(): List<WeatherRecord> {

        return filterByHours(24 * 7)
    }

    /**
     * Общий метод фильтрации
     */
    private fun filterByHours(
        hours: Int
    ): List<WeatherRecord> {

        val now =
            Instant.now()

        return store.loadAll().filter {

            val recordTime =
                Instant.parse(it.timestamp)

            now.epochSecond -
                    recordTime.epochSecond <=
                    hours * 3600L
        }
    }

    /**
     * Средняя температура
     */
    fun getAverageTemperature(): Double {

        val data =
            getLast24Hours()

        if (data.isEmpty()) {

            return 0.0
        }

        return data
            .map {
                it.temperature
            }
            .average()
    }

    /**
     * Минимальная и максимальная температура
     */
    fun getMinMax(): Pair<Double, Double> {

        val data =
            getLast24Hours()

        if (data.isEmpty()) {

            return Pair(
                0.0,
                0.0
            )
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

    /**
     * Универсальная сводка
     */
    fun getWeatherSummary(
        range: String = "24h"
    ): String {

        val data =
            when (range) {

                "48h" ->
                    getLast48Hours()

                "7d" ->
                    getLast7Days()

                else ->
                    getLast24Hours()
            }

        if (data.isEmpty()) {

            return "Нет данных"
        }

        val avgTemp =
            data.map {
                it.temperature
            }.average()

        val minTemp =
            data.minOf {
                it.temperature
            }

        val maxTemp =
            data.maxOf {
                it.temperature
            }

        val avgHumidity =
            data.map {
                it.humidity
            }.average()

        val avgWind =
            data.map {
                it.windSpeed
            }.average()

        val conditions =
            data.groupingBy {

                it.condition

            }.eachCount()

        val mostFrequentCondition =
            conditions.maxByOrNull {

                it.value

            }?.key ?: "Неизвестно"

        return buildString {

            appendLine(
                "Погода в Брянске"
            )

            appendLine()

            appendLine(
                "Период: $range"
            )

            appendLine(
                "Количество измерений: ${data.size}"
            )

            appendLine()

            appendLine(
                "Средняя температура: %.1f°C"
                    .format(avgTemp)
            )

            appendLine(
                "Минимальная температура: %.1f°C"
                    .format(minTemp)
            )

            appendLine(
                "Максимальная температура: %.1f°C"
                    .format(maxTemp)
            )

            appendLine()

            appendLine(
                "Средняя влажность: %.0f%%"
                    .format(avgHumidity)
            )

            appendLine(
                "Средняя скорость ветра: %.1f м/с"
                    .format(avgWind)
            )

            appendLine()

            appendLine(
                "Преобладающая погода: $mostFrequentCondition"
            )
        }
    }
}