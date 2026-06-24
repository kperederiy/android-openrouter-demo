package com.example.aichallenge

import com.example.aichallenge.mcp.MCPTool
import com.example.aichallenge.mcp.MockMCPClient
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class SimpleAgent(
    private val apiKey: String
) {

    companion object {

        private const val MODEL =
            "openai/gpt-4o-mini"
    }

    private val client = OkHttpClient()

    private val mcpClient = MockMCPClient()

    fun loadTools(
        onSuccess: (List<MCPTool>) -> Unit,
        onError: (String) -> Unit
    ) {

        mcpClient.getTools(
            onSuccess,
            onError
        )
    }

    fun processRequest(
        userRequest: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {

        val prompt = userRequest

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