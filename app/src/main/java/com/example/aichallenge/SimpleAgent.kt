package com.example.aichallenge

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

class SimpleAgent(
    private val context: Context,
    private val apiKey: String
) {

    companion object {

        private const val MODEL = "openai/gpt-4o-mini"

        private const val CONTEXT_WINDOW = 128000

        private const val INPUT_PRICE_PER_TOKEN =
            0.15 / 1_000_000

        private const val OUTPUT_PRICE_PER_TOKEN =
            0.60 / 1_000_000
    }

    private val client = OkHttpClient()

    private val gson = Gson()

    private val historyType =
        object : TypeToken<MutableList<ChatMessage>>() {}.type

    private val historyFile =
        File(context.filesDir, "history.json")

    private val messages =
        mutableListOf<ChatMessage>()

    init {
        loadHistory()
    }

    fun processRequest(
        userRequest: String,
        onSuccess: (
            answer: String,
            stats: AgentStats
        ) -> Unit,
        onError: (String) -> Unit
    ) {

        messages.add(
            ChatMessage(
                role = "user",
                content = userRequest
            )
        )

        saveHistory()

        val json = JSONObject()

        json.put(
            "model",
            MODEL
        )

        val messagesArray = JSONArray()

        messages.forEach {

            messagesArray.put(
                JSONObject().apply {

                    put("role", it.role)
                    put("content", it.content)
                }
            )
        }

        json.put(
            "messages",
            messagesArray
        )

        val body = RequestBody.create(
            "application/json".toMediaType(),
            json.toString()
        )

        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/chat/completions")
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
            .enqueue(object : Callback {

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
                        response.body?.string() ?: ""

                    if (!response.isSuccessful) {

                        onError(
                            "HTTP ${response.code}\n$responseBody"
                        )

                        return
                    }

                    try {

                        val jsonObject =
                            JSONObject(responseBody)

                        val answer =
                            jsonObject
                                .getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content")

                        val usage =
                            jsonObject.optJSONObject("usage")

                        val promptTokens =
                            usage?.optInt(
                                "prompt_tokens",
                                0
                            ) ?: 0

                        val completionTokens =
                            usage?.optInt(
                                "completion_tokens",
                                0
                            ) ?: 0

                        val totalTokens =
                            usage?.optInt(
                                "total_tokens",
                                0
                            ) ?: 0

                        messages.add(
                            ChatMessage(
                                role = "assistant",
                                content = answer
                            )
                        )

                        saveHistory()

                        val historyTokens =
                            estimateHistoryTokens()

                        val cost =
                            promptTokens *
                                    INPUT_PRICE_PER_TOKEN +
                                    completionTokens *
                                    OUTPUT_PRICE_PER_TOKEN

                        val usagePercent =
                            ((historyTokens.toDouble()
                                    / CONTEXT_WINDOW) * 100)
                                .toInt()

                        val warning =
                            when {
                                usagePercent >= 95 ->
                                    "❌ Контекст почти переполнен"

                                usagePercent >= 80 ->
                                    "⚠ История занимает $usagePercent% контекста"

                                else ->
                                    "Контекст в норме"
                            }

                        val stats =
                            AgentStats(
                                promptTokens =
                                    promptTokens,

                                completionTokens =
                                    completionTokens,

                                totalTokens =
                                    totalTokens,

                                historyTokens =
                                    historyTokens,

                                estimatedCost =
                                    cost,

                                contextUsagePercent =
                                    usagePercent,

                                contextWarning =
                                    warning
                            )

                        onSuccess(
                            answer,
                            stats
                        )

                    } catch (e: Exception) {

                        onError(
                            "Ошибка обработки ответа: ${e.message}"
                        )
                    }
                }
            })
    }

    private fun estimateHistoryTokens(): Int {

        return messages.sumOf {

            val words =
                it.content
                    .trim()
                    .split("\\s+".toRegex())
                    .size

            (words * 1.3).toInt()
        }
    }

    private fun saveHistory() {

        try {

            val json =
                gson.toJson(messages)

            historyFile.writeText(json)

        } catch (e: Exception) {

            e.printStackTrace()
        }
    }

    private fun loadHistory() {

        try {

            if (!historyFile.exists())
                return

            val json =
                historyFile.readText()

            val loadedMessages:
                    MutableList<ChatMessage> =
                gson.fromJson(
                    json,
                    historyType
                )

            messages.clear()

            messages.addAll(
                loadedMessages
            )

        } catch (e: Exception) {

            e.printStackTrace()
        }
    }

    fun clearHistory() {

        messages.clear()

        saveHistory()
    }

    fun getHistorySize(): Int {

        return messages.size
    }

    fun getHistoryTokens(): Int {

        return estimateHistoryTokens()
    }
}