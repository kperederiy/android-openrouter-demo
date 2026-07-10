package com.example.aichallenge

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class OpenRouterClient(

    private val apiKey: String

) {

    companion object {

        private const val MODEL =

            "openai/gpt-4o-mini"

    }

    private val client =

        OkHttpClient()

    fun generate(

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
}