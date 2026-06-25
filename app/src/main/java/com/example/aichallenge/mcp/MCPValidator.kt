package com.example.aichallenge.mcp

object MCPValidator {

    fun validateWeather(
        city: String,
        unit: String,
        lang: String
    ) {

        require(city.isNotBlank()) {
            "Город не указан"
        }

        require(
            unit == "metric" ||
                    unit == "imperial"
        ) {
            "unit должен быть metric или imperial"
        }

        require(
            lang == "ru" ||
                    lang == "en"
        ) {
            "lang должен быть ru или en"
        }
    }
}