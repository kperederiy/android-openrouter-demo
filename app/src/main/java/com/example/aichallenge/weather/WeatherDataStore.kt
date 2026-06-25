package com.example.aichallenge.weather

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class WeatherDataStore(
    private val context: Context
) {

    private val gson = Gson()

    private val file =
        File(
            context.filesDir,
            "weather_history.json"
        )

    fun save(record: WeatherRecord) {

        val data = loadAll().toMutableList()

        data.add(record)

        file.writeText(
            gson.toJson(data)
        )
    }

    fun loadAll(): List<WeatherRecord> {

        if (!file.exists()) {
            return emptyList()
        }

        val json = file.readText()

        val type =
            object : TypeToken<List<WeatherRecord>>() {}.type

        return gson.fromJson(
            json,
            type
        ) ?: emptyList()
    }
}