package com.example.aichallenge.mcp

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.IOException

class MCPClient(
    private val serverUrl: String
) {

    private val client = OkHttpClient()

    fun getTools(
        onSuccess: (List<MCPTool>) -> Unit,
        onError: (String) -> Unit
    ) {

        val requestJson = JSONObject().apply {

            put("jsonrpc", "2.0")
            put("id", "1")
            put("method", "tools/list")
        }

        val body =
            RequestBody.create(
                "application/json".toMediaType(),
                requestJson.toString()
            )

        val request =
            Request.Builder()
                .url(serverUrl)
                .post(body)
                .build()

        client.newCall(request)
            .enqueue(object : Callback {

                override fun onFailure(
                    call: Call,
                    e: IOException
                ) {

                    onError(
                        "Ошибка соединения: ${e.message}"
                    )
                }

                override fun onResponse(
                    call: Call,
                    response: Response
                ) {

                    val responseText =
                        response.body?.string() ?: ""

                    if (!response.isSuccessful) {

                        onError(
                            "HTTP ${response.code}"
                        )

                        return
                    }

                    try {

                        val root =
                            JSONObject(responseText)

                        val toolsArray =
                            root
                                .getJSONObject("result")
                                .getJSONArray("tools")

                        val tools =
                            mutableListOf<MCPTool>()

                        for (i in 0 until toolsArray.length()) {

                            val item =
                                toolsArray.getJSONObject(i)

                            tools.add(
                                MCPTool(
                                    name = item.getString("name"),
                                    description = item.getString("description")
                                )
                            )
                        }

                        onSuccess(tools)

                    } catch (e: Exception) {

                        onError(
                            "Ошибка парсинга: ${e.message}"
                        )
                    }
                }
            })
    }
}