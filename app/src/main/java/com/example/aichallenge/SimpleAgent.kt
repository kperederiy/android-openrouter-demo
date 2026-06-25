package com.example.aichallenge

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
    private val apiKey: String
) {

    companion object {
        private const val MODEL =
            "openai/gpt-4o-mini"
    }

    private val client = OkHttpClient()

    private val weatherClient =
        WeatherMCPClient()

    /**
     * Явный вызов MCP инструмента из UI
     */
    fun processToolRequest(
        city: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {

        CoroutineScope(Dispatchers.IO).launch {

            try {

                val result =
                    weatherClient.callTool(
                        "weather",
                        mapOf(
                            "city" to city
                        )
                    )

                onSuccess(result)

            } catch (e: Exception) {

                onError(
                    e.message ?: "Tool error"
                )
            }
        }
    }

    /**
     * Основной вход агента
     */
    fun processRequest(
        userRequest: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {

        val city =
            extractCity(userRequest)

        if (city != null) {

            processWeatherRequest(
                city = city,
                originalRequest = userRequest,
                onSuccess = onSuccess,
                onError = onError
            )

            return
        }

        callLLM(
            prompt = userRequest,
            onSuccess = onSuccess,
            onError = onError
        )
    }

    /**
     * Агент вызывает MCP weather,
     * затем передает результат в LLM
     */
    private fun processWeatherRequest(
        city: String,
        originalRequest: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {

        CoroutineScope(Dispatchers.IO).launch {

            try {

                val weatherResult =
                    weatherClient.callTool(
                        "weather",
                        mapOf(
                            "city" to city
                        )
                    )

                val prompt =
                    """
                    Пользователь задал вопрос:

                    $originalRequest

                    MCP инструмент weather вернул:

                    $weatherResult

                    Используй эти данные и ответь пользователю.
                    """.trimIndent()

                callLLM(
                    prompt = prompt,
                    onSuccess = onSuccess,
                    onError = onError
                )

            } catch (e: Exception) {

                onError(
                    e.message ?: "Tool error"
                )
            }
        }
    }

    /**
     * Отправка запроса в OpenRouter
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

        val body =
            RequestBody.create(
                "application/json".toMediaType(),
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

                        val responseBody =
                            response.body?.string()
                                ?: ""

                        if (!response.isSuccessful) {

                            onError(
                                "HTTP ${response.code}\n$responseBody"
                            )

                            return
                        }

                        try {

                            val answer =
                                JSONObject(responseBody)
                                    .getJSONArray("choices")
                                    .getJSONObject(0)
                                    .getJSONObject("message")
                                    .getString("content")

                            onSuccess(answer)

                        } catch (e: Exception) {

                            onError(
                                "Ошибка обработки ответа: ${e.message}"
                            )
                        }
                    }
                }
            )
    }

    /**
     * Поиск города в запросе пользователя
     */
    private fun extractCity(
        text: String
    ): String? {

        val regex =
            Regex(
                "погод[ауы]?.*?в\\s+([A-Za-zА-Яа-я-]+)",
                RegexOption.IGNORE_CASE
            )

        return regex.find(text)
            ?.groupValues
            ?.getOrNull(1)
    }
}