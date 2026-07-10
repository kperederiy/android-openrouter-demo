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

            OllamaConfig.MODEL

        )


        json.put(

            "prompt",

            prompt

        )


        json.put(

            "stream",

            false

        )



        /*
            Параметры генерации
        */

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

                                    .getString("response")



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