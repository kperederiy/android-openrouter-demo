package com.example.aichallenge

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class OllamaClient {

    companion object {

        private const val URL =
            "http://10.0.2.2:11434/api/generate"

        private const val MODEL =
            "mistral:7b-instruct-v0.3-q4_K_M"
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

        json.put(
            "prompt",
            prompt
        )

        json.put(
            "stream",
            false
        )

        val body =

            RequestBody.create(

                "application/json".toMediaType(),

                json.toString()

            )

        val request =

            Request.Builder()

                .url(URL)

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
                            response.body?.string() ?: ""

                        if (!response.isSuccessful) {

                            onError(
                                "HTTP ${response.code}\n$responseBody"
                            )

                            return
                        }

                        try {

                            val answer =

                                JSONObject(responseBody)

                                    .getString("response")

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