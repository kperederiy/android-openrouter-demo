package com.example.aichallenge

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class SimpleAgent(
    private val apiKey: String
) {

    private val client = OkHttpClient()

    fun processRequest(
        userRequest: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {

        val json = JSONObject().apply {

            put("model", "openai/gpt-4o-mini")

            put(
                "messages",
                JSONArray().put(
                    JSONObject().apply {
                        put("role", "user")
                        put("content", userRequest)
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

                    onSuccess(answer)

                } catch (e: Exception) {

                    onError(
                        "Ошибка обработки ответа: ${e.message}"
                    )
                }
            }
        })
    }
}