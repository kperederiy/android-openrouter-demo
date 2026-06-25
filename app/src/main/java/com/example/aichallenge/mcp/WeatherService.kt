package com.example.aichallenge.weather

import okhttp3.OkHttpClient
import okhttp3.Request

class WeatherService {

    private val client = OkHttpClient()

    suspend fun getWeather(
        city: String
    ): String {

        val url =
            "https://wttr.in/${city}?format=%l:+%t+%C"

        val request =
            Request.Builder()
                .url(url)
                .header(
                    "Accept",
                    "text/plain"
                )
                .header(
                    "User-Agent",
                    "curl"
                )
                .build()

        client.newCall(request)
            .execute()
            .use { response ->

                return response.body?.string()
                    ?.trim()
                    ?: "Нет данных"
            }
    }
}