package com.example.aichallenge

import android.content.Context
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

class SimpleAgent(
    private val context: Context,
    private val apiKey: String
) {

    private val client = OkHttpClient()

    private val historyFile =
        File(context.filesDir, "history.json")

    private val messages =
        mutableListOf<ChatMessage>()

    init {
        loadHistory()
    }

    fun processRequest(
        userRequest: String,
        onSuccess: (String) -> Unit,
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
            "openai/gpt-4o-mini"
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

                        messages.add(
                            ChatMessage(
                                role = "assistant",
                                content = answer
                            )
                        )

                        saveHistory()

                        onSuccess(answer)

                    } catch (e: Exception) {

                        onError(
                            "Ошибка обработки ответа: ${e.message}"
                        )
                    }
                }
            })
    }

    private fun saveHistory() {

        try {

            val jsonArray = JSONArray()

            messages.forEach {

                jsonArray.put(
                    JSONObject().apply {

                        put("role", it.role)
                        put("content", it.content)
                    }
                )
            }

            historyFile.writeText(
                jsonArray.toString()
            )

        } catch (_: Exception) {
        }
    }

    private fun loadHistory() {

        try {

            if (!historyFile.exists()) {
                return
            }

            val content =
                historyFile.readText()

            val jsonArray =
                JSONArray(content)

            messages.clear()

            for (i in 0 until jsonArray.length()) {

                val item =
                    jsonArray.getJSONObject(i)

                messages.add(
                    ChatMessage(
                        role = item.getString("role"),
                        content = item.getString("content")
                    )
                )
            }

        } catch (_: Exception) {
        }
    }

    fun clearHistory() {

        messages.clear()

        saveHistory()
    }
}