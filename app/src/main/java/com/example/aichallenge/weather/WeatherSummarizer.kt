package com.example.aichallenge.weather

class WeatherSummarizer {

    fun summarize(
        result: Map<String, Any>,
        format: String
    ): Map<String, Any> {

        return try {

            val list =
                result["data"]
                        as List<WeatherRecord>

            if (list.isEmpty()) {

                return mapOf(

                    "status" to "error",

                    "content" to
                            "Нет данных"
                )
            }

            val avgTemp =
                list.map {
                    it.temperature
                }.average()

            val min =
                list.minOf {
                    it.temperature
                }

            val max =
                list.maxOf {
                    it.temperature
                }

            val humidity =
                list.map {
                    it.humidity
                }.average()

            val wind =
                list.map {
                    it.windSpeed
                }.average()

            val summary = buildString {

                appendLine("Сводка погоды")

                appendLine()

                appendLine(
                    "Количество измерений: ${list.size}"
                )

                appendLine(
                    "Средняя температура: %.1f°C"
                        .format(avgTemp)
                )

                appendLine(
                    "Минимальная: %.1f°C"
                        .format(min)
                )

                appendLine(
                    "Максимальная: %.1f°C"
                        .format(max)
                )

                appendLine(
                    "Средняя влажность: %.0f%%"
                        .format(humidity)
                )

                appendLine(
                    "Средний ветер: %.1f м/с"
                        .format(wind)
                )
            }

            mapOf(

                "status" to "success",

                "content" to summary,

                "summary" to summary,

                "data" to list
            )

        } catch (e: Exception) {

            mapOf(

                "status" to "error",

                "content" to
                        (e.message ?: "Ошибка")
            )
        }
    }
}