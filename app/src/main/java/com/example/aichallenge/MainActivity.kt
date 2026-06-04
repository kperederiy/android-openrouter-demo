package com.example.aichallenge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Properties

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val apiKey = getApiKey()

        setContent {

            val prompts = mapOf(

                "Прямой ответ" to """
                    Дан массив чисел: [3, 7, 2, 9, 4, 8, 1].
                    Найти второе по величине число в массиве.
                """.trimIndent(),

                "Решай пошагово" to """
                    Дан массив чисел: [3, 7, 2, 9, 4, 8, 1].
                    Решай пошагово.
                    Найти второе по величине число в массиве.
                """.trimIndent(),

                "Сгенерируй промпт" to """
                    Составь оптимальный промпт для решения задачи:

                    Дан массив чисел: [3, 7, 2, 9, 4, 8, 1].
                    Найти второе по величине число в массиве.
                """.trimIndent(),

                "Группа экспертов" to """
                    Решите задачу группой экспертов.

                    Эксперт 1 — аналитик.
                    Эксперт 2 — инженер.
                    Эксперт 3 — критик.

                    Задача:
                    Дан массив чисел: [3, 7, 2, 9, 4, 8, 1].
                    Найти второе по величине число.
                """.trimIndent()
            )

            var selectedMethod by remember {
                mutableStateOf("Прямой ответ")
            }

            var llmResponse by remember {
                mutableStateOf("Ответ появится здесь")
            }

            var isLoading by remember {
                mutableStateOf(false)
            }

            MaterialTheme {

                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {

                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {

                        Text(
                            text = "AI Advent Challenge",
                            style = MaterialTheme.typography.headlineSmall
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Выберите способ решения:",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        prompts.keys.forEach { method ->

                            Row(
                                modifier = Modifier.fillMaxWidth()
                            ) {

                                RadioButton(
                                    selected = selectedMethod == method,
                                    onClick = {
                                        selectedMethod = method
                                    }
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = method
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {

                                isLoading = true

                                sendRequest(
                                    apiKey = apiKey,
                                    userPrompt = prompts[selectedMethod] ?: "",
                                    onSuccess = { answer ->

                                        runOnUiThread {
                                            llmResponse = answer
                                            isLoading = false
                                        }
                                    },
                                    onError = { error ->

                                        runOnUiThread {
                                            llmResponse = error
                                            isLoading = false
                                        }
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Получить ответ")
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        if (isLoading) {
                            CircularProgressIndicator()
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Ответ модели:",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            Text(
                                text = llmResponse,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    private fun getApiKey(): String {

        val properties = Properties()

        assets.open("secrets.properties").use {
            properties.load(it)
        }

        return properties.getProperty("api_key")
    }


    private fun sendRequest(
        apiKey: String,
        userPrompt: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {

        val client = OkHttpClient()

        val json = JSONObject().apply {

            put("model", "openai/gpt-4o-mini")

            put(
                "messages",
                JSONArray().put(
                    JSONObject().apply {
                        put("role", "user")
                        put("content", userPrompt)
                    }
                )
            )
        }

        val body = RequestBody.create(
            "application/json".toMediaType(),
            json.toString()
        )

        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(
                call: Call,
                e: IOException
            ) {
                onError("Ошибка сети: ${e.message}")
            }

            override fun onResponse(
                call: Call,
                response: Response
            ) {

                val responseBody = response.body?.string()

                try {

                    val answer = JSONObject(responseBody)
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")

                    onSuccess(answer)

                } catch (e: Exception) {

                    onError(
                        "Ошибка обработки ответа:\n\n${e.message}\n\n$responseBody"
                    )
                }
            }
        })
    }
}