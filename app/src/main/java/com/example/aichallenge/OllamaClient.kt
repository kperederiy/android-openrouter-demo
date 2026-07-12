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

class OllamaClient {

    private val client =

        OkHttpClient()

    fun chat(

        messages: List<ChatMessage>,

        onSuccess: (String) -> Unit,

        onError: (String) -> Unit

    ) {

        val json = JSONObject()

        //--------------------------------------------------
        // модель
        //--------------------------------------------------

        json.put(

            "model",

            OllamaConfig.MODEL

        )

        //--------------------------------------------------
        // сообщения
        //--------------------------------------------------

        val messagesJson = JSONArray()

        messages.forEach { message ->

            val item = JSONObject()

            item.put(

                "role",

                message.role

            )

            item.put(

                "content",

                message.text

            )

            messagesJson.put(item)
        }

        json.put(

            "messages",

            messagesJson

        )

        //--------------------------------------------------
        // stream
        //--------------------------------------------------

        json.put(

            "stream",

            false

        )

        //--------------------------------------------------
        // options
        //--------------------------------------------------

        val options = JSONObject()

        options.put(

            "temperature",

            OllamaConfig.TEMPERATURE

        )

        options.put(

            "num_predict",

            OllamaConfig.MAX_TOKENS

        )

        options.put(

            "num_ctx",

            OllamaConfig.NUM_CTX

        )

        json.put(

            "options",

            options

        )

        //--------------------------------------------------
        // request
        //--------------------------------------------------

        val body =

            RequestBody.create(

                "application/json".toMediaType(),

                json.toString()

            )

        val request =

            Request.Builder()

                .url(

                    OllamaConfig.URL

                )

                .post(body)

                .build()

        //--------------------------------------------------
        // execute
        //--------------------------------------------------

        client.newCall(request)

            .enqueue(

                object : Callback {

                    override fun onFailure(

                        call: Call,

                        e: IOException

                    ) {

                        onError(

                            "Ошибка Ollama: ${e.message}"

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

                                    .getJSONObject(

                                        "message"

                                    )

                                    .getString(

                                        "content"

                                    )

                            onSuccess(answer)

                        } catch (e: Exception) {

                            onError(

                                "Ошибка ответа Ollama: ${e.message}"

                            )
                        }
                    }
                }
            )
    }
}