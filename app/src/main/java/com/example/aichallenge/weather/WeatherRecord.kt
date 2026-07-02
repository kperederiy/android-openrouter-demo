package com.example.aichallenge.weather

data class WeatherRecord(
    val timestamp: String,
    val temperature: Double,
    val condition: String,
    val humidity: Int,
    val windSpeed: Double
)