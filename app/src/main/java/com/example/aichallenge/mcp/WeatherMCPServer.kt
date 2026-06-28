package com.example.aichallenge.mcp

import android.content.Context
import android.util.Log
import com.example.aichallenge.weather.FileSaver
import com.example.aichallenge.weather.WeatherSearchService
import com.example.aichallenge.weather.WeatherService
import com.example.aichallenge.weather.WeatherSummarizer

class WeatherMCPServer(
    private val context: Context
) {

    companion object {

        private const val TAG =
            "WeatherPipeline"
    }

    private val weatherService =
        WeatherService()

    private val searchService =
        WeatherSearchService(context)

    private val summarizer =
        WeatherSummarizer()

    private val fileSaver =
        FileSaver(context)

    suspend fun handleRequest(method: String, params: Map<String, Any>): Map<String, Any> {

        return try {

            Log.d(TAG, "Обработка запроса: $method")

            // Извлекаем имя инструмента без префикса
            val toolName = when {
                method == "tools/call" -> {
                    val name = params["name"] as? String ?: ""
                    name.substringAfterLast(".")
                }
                else -> method
            }

            when (method) {

                "tools/call" -> {

                    val toolName =
                        params["name"] as String

                    val arguments =
                        params["arguments"]
                                as Map<String, Any>

                    when (toolName) {

                        "weather" ->
                            weatherTool(arguments)

                        "search" ->
                            searchTool(arguments)

                        "summarize" ->
                            summarizeTool(arguments)

                        "saveToFile" ->
                            saveToFileTool(arguments)

                        "pipeline" ->
                            pipelineTool(arguments)

                        else ->
                            mapOf(
                                "status" to "error",
                                "content" to
                                        "Unknown tool"
                            )
                    }
                }

                else ->
                    mapOf(
                        "status" to "error",
                        "content" to
                                "Unknown method"
                    )
            }

        } catch (e: Exception) {

            mapOf(
                "status" to "error",
                "content" to
                        (e.message ?: "Unknown error")
            )
        }
    }

    /**
     * Получение текущей погоды
     */
    private suspend fun weatherTool(
        args: Map<String, Any>
    ): Map<String, Any> {

        val city =
            args["city"]?.toString()
                ?: "Брянск"

        val result =
            weatherService.getWeather(city)

        return mapOf(

            "status" to "success",

            "content" to result
        )
    }

    /**
     * SEARCH TOOL
     */
    private fun searchTool(
        args: Map<String, Any>
    ): Map<String, Any> {

        return try {

            Log.d(
                TAG,
                "SEARCH started"
            )

            val query =
                args["query"]
                    ?.toString()
                    ?: "Брянск"

            val range =
                args["range"]
                    ?.toString()
                    ?: "24h"

            val result =
                searchService.search(
                    query,
                    range
                )

            Log.d(
                TAG,
                "SEARCH finished"
            )

            result

        } catch (e: Exception) {

            Log.e(
                TAG,
                "SEARCH error",
                e
            )

            mapOf(

                "status" to "error",

                "content" to
                        (e.message ?: "Search error")
            )
        }
    }
    /**
     * SUMMARIZE TOOL
     */
    private fun summarizeTool(
        args: Map<String, Any>
    ): Map<String, Any> {

        return try {

            Log.d(
                TAG,
                "SUMMARIZE started"
            )

            val format =
                args["format"]
                    ?.toString()
                    ?: "text"

            val result =
                summarizer.summarize(
                    args,
                    format
                )

            Log.d(
                TAG,
                "SUMMARIZE finished"
            )

            result

        } catch (e: Exception) {

            Log.e(
                TAG,
                "SUMMARIZE error",
                e
            )

            mapOf(

                "status" to "error",

                "content" to
                        (e.message ?: "Summarize error")
            )
        }
    }

    /**
     * SAVE TOOL
     */
    private fun saveToFileTool(
        args: Map<String, Any>
    ): Map<String, Any> {

        return try {

            Log.d(
                TAG,
                "SAVE started"
            )

            val content =
                args["content"]
                    ?.toString()
                    ?: ""

            val filename =
                args["filename"]
                    ?.toString()
                    ?: "weather_report.txt"

            val result =
                fileSaver.save(
                    content,
                    filename
                )

            Log.d(
                TAG,
                "SAVE finished"
            )

            result

        } catch (e: Exception) {

            Log.e(
                TAG,
                "SAVE error",
                e
            )

            mapOf(

                "status" to "error",

                "content" to
                        (e.message ?: "Save error")
            )
        }
    }

    /**
     * PIPELINE
     *
     * SEARCH
     *      ↓
     * SUMMARIZE
     *      ↓
     * SAVE
     */
    private fun pipelineTool(
        args: Map<String, Any>
    ): Map<String, Any> {

        Log.d(
            TAG,
            "Pipeline started"
        )

        return try {

            //--------------------------------------------------
            // STEP 1
            //--------------------------------------------------

            Log.d(
                TAG,
                "Step 1 SEARCH"
            )

            val searchResult =
                searchTool(args)

            if (searchResult["status"] != "success") {

                return mapOf(

                    "status" to "error",

                    "step" to "search",

                    "content" to
                            searchResult["content"].toString()
                )
            }

            //--------------------------------------------------
            // STEP 2
            //--------------------------------------------------

            Log.d(
                TAG,
                "Step 2 SUMMARIZE"
            )

            val summarizeArgs =
                mutableMapOf<String, Any>()

            summarizeArgs.putAll(
                searchResult
            )

            summarizeArgs["format"] =
                args["format"]
                    ?: "text"

            val summaryResult =
                summarizeTool(
                    summarizeArgs
                )

            if (summaryResult["status"] != "success") {

                return mapOf(

                    "status" to "error",

                    "step" to "summarize",

                    "content" to
                            summaryResult["content"].toString()
                )
            }

            //--------------------------------------------------
            // STEP 3
            //--------------------------------------------------

            Log.d(
                TAG,
                "Step 3 SAVE"
            )

            val saveResult =
                saveToFileTool(

                    mapOf(

                        "content" to
                                summaryResult["summary"].toString(),

                        "filename" to
                                (args["filename"]
                                    ?: "weather_report.txt")
                    )
                )

            if (saveResult["status"] != "success") {

                return mapOf(

                    "status" to "error",

                    "step" to "save",

                    "content" to
                            saveResult["content"].toString()
                )
            }

            Log.d(
                TAG,
                "Pipeline SUCCESS"
            )

            mapOf(

                "status" to "success",

                "content" to
                        """
✅ Пайплайн успешно выполнен

🔍 Поиск данных выполнен

📊 Сводка сформирована

💾 Файл сохранён

${saveResult["filePath"]}
                        """.trimIndent(),

                "search_result" to
                        searchResult,

                "summarize_result" to
                        summaryResult,

                "save_result" to
                        saveResult,

                "summary" to
                        summaryResult["summary"].toString(),

                "filePath" to
                        saveResult["filePath"].toString()
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Pipeline error",
                e
            )

            mapOf(

                "status" to "error",

                "step" to "pipeline",

                "content" to
                        (e.message ?: "Pipeline error")
            )
        }
    }
}