package com.example.aichallenge.mcp

import org.json.JSONArray
import org.json.JSONObject

object MockMCPServer {

    fun getToolsResponse(): String {

        return JSONObject().apply {

            put("jsonrpc", "2.0")

            put(
                "result",
                JSONObject().apply {

                    put(
                        "tools",
                        JSONArray().apply {

                            put(
                                JSONObject().apply {

                                    put(
                                        "name",
                                        "weather"
                                    )

                                    put(
                                        "description",
                                        "Получить погоду"
                                    )
                                }
                            )

                            put(
                                JSONObject().apply {

                                    put(
                                        "name",
                                        "calculator"
                                    )

                                    put(
                                        "description",
                                        "Выполнить вычисления"
                                    )
                                }
                            )

                            put(
                                JSONObject().apply {

                                    put(
                                        "name",
                                        "search"
                                    )

                                    put(
                                        "description",
                                        "Поиск информации"
                                    )
                                }
                            )
                        }
                    )
                }
            )

            put("id", "1")
        }.toString()
    }
}