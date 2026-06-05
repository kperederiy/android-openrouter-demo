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

            val userPrompt = """
                Придумай идею мобильного приложения для студентов.
                Опиши назначение приложения и основные функции.
            """.trimIndent()

            var llmResponse by remember {
                mutableStateOf("Нажмите кнопку для получения ответов")
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
                            text = "Сравнение Temperature: 0 / 0.7 / 1.2",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {

                                isLoading = true
                                llmResponse = "Получаем ответы..."

                                val result = StringBuilder()

                                sendRequest(
                                    apiKey = apiKey,
                                    userPrompt = userPrompt,
                                    temperature = 0.0,
                                    onSuccess = { answer0 ->

                                        result.append(
                                            """
                                            
====================================
Temperature = 0
====================================

$answer0

                                            """.trimIndent()
                                        )

                                        sendRequest(
                                            apiKey = apiKey,
                                            userPrompt = userPrompt,
                                            temperature = 0.7,
                                            onSuccess = { answer07 ->

                                                result.append(
                                                    """
                                                    
====================================
Temperature = 0.7
====================================

$answer07

                                                    """.trimIndent()
                                                )

                                                sendRequest(
                                                    apiKey = apiKey,
                                                    userPrompt = userPrompt,
                                                    temperature = 1.2,
                                                    onSuccess = { answer12 ->

                                                        result.append(
                                                            """
                                                            
====================================
Temperature = 1.2
====================================

$answer12
                                                            """.trimIndent()
                                                        )

                                                        runOnUiThread {

                                                            llmResponse =
                                                                result.toString()

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
                                            onError = { error ->

                                                runOnUiThread {
                                                    llmResponse = error
                                                    isLoading = false
                                                }
                                            }
                                        )
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
                            Text("Получить все ответы")
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        if (isLoading) {
                            CircularProgressIndicator()
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Ответы модели:",
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

        return properties.getProperty("api_key") ?: ""
    }

    private fun sendRequest(
        apiKey: String,
        userPrompt: String,
        temperature: Double,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {

        val client = OkHttpClient()

        val json = JSONObject().apply {

            put("model", "openai/gpt-4o-mini")

            put("temperature", temperature)

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

                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {

                    onError(
                        """
HTTP ${response.code}

$responseBody
                        """.trimIndent()
                    )
                    return
                }

                try {

                    val answer = JSONObject(responseBody)
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")

                    onSuccess(answer)

                } catch (e: Exception) {

                    onError(
                        """
Ошибка обработки ответа

${e.message}

$responseBody
                        """.trimIndent()
                    )
                }
            }
        })
    }
}