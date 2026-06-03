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
import org.json.JSONObject
import java.io.IOException

class MainActivity : ComponentActivity() {

    private val apiKey = BuildConfig.OPENROUTER_API_KEY
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            var userPrompt by remember {
                mutableStateOf("Привет! Представься одной фразой.")
            }

            var responseFormat by remember {
                mutableStateOf("Ответь строго в формате: Я — <кто ты>.")
            }

            var maxTokens by remember {
                mutableStateOf("20")
            }

            var stopSequence by remember {
                mutableStateOf(".")
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

                        OutlinedTextField(
                            value = userPrompt,
                            onValueChange = {
                                userPrompt = it
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text("Запрос")
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = responseFormat,
                            onValueChange = {
                                responseFormat = it
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text("Описание формата ответа")
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = maxTokens,
                            onValueChange = {
                                maxTokens = it
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text("Максимальное количество токенов")
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = stopSequence,
                            onValueChange = {
                                stopSequence = it
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text("Условие завершения (stop)")
                            }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {

                                isLoading = true

                                sendRequest(
                                    apiKey = apiKey,
                                    userPrompt = userPrompt,
                                    responseFormat = responseFormat,
                                    maxTokens = maxTokens.toIntOrNull() ?: 20,
                                    stopSequence = stopSequence,
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
                            Text("Отправить запрос")
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

    private fun sendRequest(
        apiKey: String,
        userPrompt: String,
        responseFormat: String,
        maxTokens: Int,
        stopSequence: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {

        val client = OkHttpClient()

        val fullPrompt =
            "$userPrompt\n$responseFormat\nЗаверши ответ при достижении условия."

        val json = JSONObject().apply {

            put("model", "openai/gpt-4o-mini")

            put("max_tokens", maxTokens)

            put("stop", org.json.JSONArray().put(stopSequence))

            put(
                "messages",
                org.json.JSONArray().put(
                    JSONObject().apply {
                        put("role", "user")
                        put("content", fullPrompt)
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