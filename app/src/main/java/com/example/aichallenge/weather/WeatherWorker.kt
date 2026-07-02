package com.example.aichallenge.weather

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.time.Instant

class WeatherWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(
    context,
    params
) {

    override suspend fun doWork(): Result {

        return try {

            val service =
                WeatherService()

            val weather =
                service.getWeather(
                    "Bryansk"
                )

            val record =
                WeatherRecord(
                    timestamp =
                        Instant.now().toString(),
                    temperature =
                        extractTemp(weather),
                    condition = weather,
                    humidity = 0,
                    windSpeed = 0.0
                )

            WeatherDataStore(
                applicationContext
            ).save(record)

            Result.success()

        } catch (e: Exception) {

            Result.retry()
        }
    }

    private fun extractTemp(
        text: String
    ): Double {

        val regex =
            Regex("(-?\\d+)")

        return regex.find(text)
            ?.value
            ?.toDoubleOrNull()
            ?: 0.0
    }
}