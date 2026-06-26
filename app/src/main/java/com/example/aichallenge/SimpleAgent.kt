package com.example.aichallenge

import android.content.Context
import com.example.aichallenge.mcp.WeatherMCPClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class SimpleAgent(

    private val apiKey: String,

    context: Context

) {

    companion object {

        private const val MODEL =
            "openai/gpt-4o-mini"
    }

    private val client =
        OkHttpClient()

    private val weatherClient =
        WeatherMCPClient(context)

    /**
     * Последняя сформированная сводка
     */
    private var lastSummary = ""

    /**
     * Запуск полного MCP Pipeline
     */
    fun runWeatherPipeline(

        range: String,

        onSuccess: (String) -> Unit,

        onError: (String) -> Unit

    ) {

        CoroutineScope(Dispatchers.IO).launch {

            try {

                val result =

                    weatherClient.runPipeline(

                        query = "Брянск",

                        range = range,

                        filename = "weather_report.txt"
                    )

                if (result["status"] == "success") {

                    lastSummary =
                        result["summary"]
                            ?.toString()
                            ?: ""

                    onSuccess(

                        result["content"]
                            .toString()
                    )

                } else {

                    onError(

                        result["content"]
                            .toString()
                    )
                }

            } catch (e: Exception) {

                onError(

                    e.message
                        ?: "Pipeline error"
                )
            }
        }
    }

    /**
     * Главная точка входа агента
     */
    fun processRequest(

        userRequest: String,

        onSuccess: (String) -> Unit,

        onError: (String) -> Unit

    ) {

        val text =
            userRequest.lowercase()

        //------------------------------------------------------
        // Пайплайн
        //------------------------------------------------------

        if ("пайплайн" in text) {

            val range = when {

                "недел" in text ->
                    "7d"

                "2 дня" in text ->
                    "48h"

                else ->
                    "24h"
            }

            runWeatherPipeline(

                range,

                onSuccess,

                onError
            )

            return
        }

        //------------------------------------------------------
        // Последняя сводка
        //------------------------------------------------------

        if ("показать сводку" in text) {

            if (lastSummary.isBlank()) {

                onError(
                    "Сводка ещё не сформирована."
                )

            } else {

                onSuccess(lastSummary)
            }

            return
        }

        //------------------------------------------------------
        // Сохранение последней сводки
        //------------------------------------------------------

        if ("сохранить отчет" in text) {

            CoroutineScope(
                Dispatchers.IO
            ).launch {

                try {

                    val result =

                        weatherClient.saveReport(

                            lastSummary,

                            "weather_report.txt"
                        )

                    onSuccess(

                        result["content"]
                            .toString()
                    )

                } catch (e: Exception) {

                    onError(

                        e.message
                            ?: "Save error"
                    )
                }
            }

            return
        }

        //------------------------------------------------------
        // Текущая погода
        //------------------------------------------------------

        if ("погода сейчас" in text) {

            CoroutineScope(
                Dispatchers.IO
            ).launch {

                try {

                    val weather =

                        weatherClient
                            .getWeather("Брянск")

                    onSuccess(weather)

                } catch (e: Exception) {

                    onError(

                        e.message
                            ?: "Weather error"
                    )
                }
            }

            return
        }

        //------------------------------------------------------
        // Погода за день
        //------------------------------------------------------

        if ("погода за день" in text) {

            CoroutineScope(
                Dispatchers.IO
            ).launch {

                try {

                    val search =

                        weatherClient
                            .searchWeather(

                                "Брянск",

                                "24h"
                            )

                    val summary =

                        weatherClient
                            .summarizeWeather(
                                search
                            )

                    lastSummary =
                        summary["summary"]
                            .toString()

                    onSuccess(lastSummary)

                } catch (e: Exception) {

                    onError(

                        e.message
                            ?: "Summary error"
                    )
                }
            }

            return
        }
        //------------------------------------------------------
        // Средняя температура
        //------------------------------------------------------

        if ("средняя температура" in text) {

            CoroutineScope(Dispatchers.IO).launch {

                try {

                    val search =
                        weatherClient.searchWeather(
                            query = "Брянск",
                            range = "24h"
                        )

                    val average =
                        search["averageTemperature"]
                            ?.toString()
                            ?: "Нет данных"

                    onSuccess(
                        "Средняя температура за последние 24 часа: $average°C"
                    )

                } catch (e: Exception) {

                    onError(
                        e.message ?: "Ошибка получения средней температуры"
                    )
                }
            }

            return
        }

        //------------------------------------------------------
        // Полная статистика
        //------------------------------------------------------

        if ("статистика погоды" in text) {

            CoroutineScope(Dispatchers.IO).launch {

                try {

                    val result =
                        weatherClient.runPipeline(

                            query = "Брянск",

                            range = "24h",

                            filename = "weather_report.txt"
                        )

                    lastSummary =
                        result["summary"]
                            ?.toString()
                            ?: ""

                    onSuccess(
                        result["content"]
                            .toString()
                    )

                } catch (e: Exception) {

                    onError(
                        e.message ?: "Ошибка получения статистики"
                    )
                }
            }

            return
        }

        //------------------------------------------------------
        // Если специальных команд нет —
        // обращаемся к LLM
        //------------------------------------------------------

        callLLM(

            prompt = userRequest,

            onSuccess = onSuccess,

            onError = onError
        )
    }
    /**
     * Запрос к OpenRouter
     */
    private fun callLLM(

        prompt: String,

        onSuccess: (String) -> Unit,

        onError: (String) -> Unit

    ) {

        val json = JSONObject()

        json.put(
            "model",
            MODEL
        )

        val messages = JSONArray()

        messages.put(

            JSONObject().apply {

                put(
                    "role",
                    "user"
                )

                put(
                    "content",
                    prompt
                )
            }
        )

        json.put(
            "messages",
            messages
        )

        val body = RequestBody.create(

            "application/json"
                .toMediaType(),

            json.toString()
        )

        val request =

            Request.Builder()

                .url(
                    "https://openrouter.ai/api/v1/chat/completions"
                )

                .addHeader(
                    "Authorization",
                    "Bearer $apiKey"
                )

                .addHeader(
                    "Content-Type",
                    "application/json"
                )

                .post(body)

                .build()

        client.newCall(request)

            .enqueue(

                object : Callback {

                    override fun onFailure(

                        call: Call,

                        e: IOException

                    ) {

                        onError(
                            "Ошибка сети: ${e.message}"
                        )
                    }

                    override fun onResponse(

                        call: Call,

                        response: Response

                    ) {

                        val body =
                            response.body?.string()
                                ?: ""

                        if (!response.isSuccessful) {

                            onError(
                                "HTTP ${response.code}\n$body"
                            )

                            return
                        }

                        try {

                            val answer =

                                JSONObject(body)

                                    .getJSONArray("choices")

                                    .getJSONObject(0)

                                    .getJSONObject("message")

                                    .getString("content")

                            onSuccess(answer)

                        } catch (e: Exception) {

                            onError(
                                e.message ?: "Ошибка обработки ответа"
                            )
                        }
                    }
                }
            )
    }
}