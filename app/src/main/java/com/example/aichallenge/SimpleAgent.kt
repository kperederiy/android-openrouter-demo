package com.example.aichallenge

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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

    companion object {

        private const val MODEL = "openai/gpt-4o-mini"

        private const val CONTEXT_WINDOW = 128000

        private const val INPUT_PRICE_PER_TOKEN =
            0.15 / 1_000_000

        private const val OUTPUT_PRICE_PER_TOKEN =
            0.60 / 1_000_000

        private const val MAX_SHORT_MEMORY = 10
    }

    private val gson = Gson()

    private val client = OkHttpClient()

    private val memory = MemoryLayers()

    private val shortMemoryFile =
        File(
            context.filesDir,
            "short_memory.json"
        )

    private val workingMemoryFile =
        File(
            context.filesDir,
            "working_memory.json"
        )

    private val longTermMemoryFile =
        File(
            context.filesDir,
            "long_term_memory.json"
        )

    init {

        loadShortTermMemory()

        loadWorkingMemory()

        loadLongTermMemory()
    }

    fun processRequest(
        userRequest: String,
        onSuccess: (
            answer: String,
            stats: AgentStats
        ) -> Unit,
        onError: (String) -> Unit
    ) {

        memory.shortTermMemory.add(
            ChatMessage(
                role = "user",
                content = userRequest
            )
        )

        updateWorkingMemory(userRequest)

        updateLongTermMemory(userRequest)

        trimShortTermMemory()

        saveShortTermMemory()

        val json = JSONObject()

        json.put(
            "model",
            MODEL
        )

        val messagesArray = JSONArray()

        messagesArray.put(
            JSONObject().apply {

                put("role", "system")

                put(
                    "content",
                    buildMemoryPrompt()
                )
            }
        )

        memory.shortTermMemory.forEach {

            messagesArray.put(
                JSONObject().apply {

                    put(
                        "role",
                        it.role
                    )

                    put(
                        "content",
                        it.content
                    )
                }
            )
        }

        json.put(
            "messages",
            messagesArray
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

                            val jsonObject =
                                JSONObject(responseBody)

                            val answer =
                                jsonObject
                                    .getJSONArray("choices")
                                    .getJSONObject(0)
                                    .getJSONObject("message")
                                    .getString("content")

                            memory.shortTermMemory.add(
                                ChatMessage(
                                    role = "assistant",
                                    content = answer
                                )
                            )

                            trimShortTermMemory()

                            saveShortTermMemory()

                            val usage =
                                jsonObject.optJSONObject("usage")

                            val promptTokens =
                                usage?.optInt(
                                    "prompt_tokens",
                                    0
                                ) ?: 0

                            val completionTokens =
                                usage?.optInt(
                                    "completion_tokens",
                                    0
                                ) ?: 0

                            val totalTokens =
                                usage?.optInt(
                                    "total_tokens",
                                    0
                                ) ?: 0

                            val historyTokens =
                                estimateHistoryTokens()

                            val cost =
                                promptTokens *
                                        INPUT_PRICE_PER_TOKEN +
                                        completionTokens *
                                        OUTPUT_PRICE_PER_TOKEN

                            val usagePercent =
                                (
                                        historyTokens.toDouble()
                                                / CONTEXT_WINDOW * 100
                                        ).toInt()

                            val stats =
                                AgentStats(
                                    promptTokens,
                                    completionTokens,
                                    totalTokens,
                                    historyTokens,
                                    cost,
                                    usagePercent,
                                    if (usagePercent > 80)
                                        "Память заполнена"
                                    else
                                        "Память в норме",
                                    "MEMORY_LAYERS"
                                )

                            onSuccess(
                                answer,
                                stats
                            )

                        } catch (e: Exception) {

                            onError(
                                "Ошибка обработки ответа: ${e.message}"
                            )
                        }
                    }
                }
            )
    }

    private fun buildMemoryPrompt(): String {

        return buildString {

            append(
                "LONG TERM MEMORY\n"
            )

            longTermEntries().forEach {

                append(
                    "${it.key}: ${it.value}\n"
                )
            }

            append("\n")

            append(
                "WORKING MEMORY\n"
            )

            workingEntries().forEach {

                append(
                    "${it.key}: ${it.value}\n"
                )
            }
        }
    }

    private fun updateWorkingMemory(
        text: String
    ) {

        val lower =
            text.lowercase()

        if (lower.contains("бюджет")) {

            memory.workingMemory["budget"] =
                text
        }

        if (lower.contains("срок")) {

            memory.workingMemory["deadline"] =
                text
        }

        if (lower.contains("используем")) {

            memory.workingMemory["technology"] =
                text
        }

        if (lower.contains("цель")) {

            memory.workingMemory["goal"] =
                text
        }

        saveWorkingMemory()
    }

    private fun updateLongTermMemory(
        text: String
    ) {

        val lower =
            text.lowercase()

        if (lower.contains("меня зовут")) {

            memory.longTermMemory["name"] =
                text
        }

        if (lower.contains("предпочитаю")) {

            memory.longTermMemory["preference"] =
                text
        }

        if (lower.contains("решили")) {

            memory.longTermMemory["decision"] =
                text
        }

        saveLongTermMemory()
    }

    private fun trimShortTermMemory() {

        if (
            memory.shortTermMemory.size <=
            MAX_SHORT_MEMORY
        ) return

        val recent =
            memory.shortTermMemory.takeLast(
                MAX_SHORT_MEMORY
            )

        memory.shortTermMemory.clear()

        memory.shortTermMemory.addAll(
            recent
        )
    }

    private fun estimateHistoryTokens(): Int {

        return memory.shortTermMemory.sumOf {

            (it.content.length / 4)
        }
    }

    private fun saveShortTermMemory() {

        shortMemoryFile.writeText(
            gson.toJson(
                memory.shortTermMemory
            )
        )
    }

    private fun loadShortTermMemory() {

        if (!shortMemoryFile.exists())
            return

        val type =
            object :
                TypeToken<
                        MutableList<ChatMessage>
                        >() {}.type

        val loaded =
            gson.fromJson<
                    MutableList<ChatMessage>
                    >(
                shortMemoryFile.readText(),
                type
            )

        memory.shortTermMemory.addAll(
            loaded
        )
    }

    private fun saveWorkingMemory() {

        workingMemoryFile.writeText(
            gson.toJson(
                memory.workingMemory
            )
        )
    }

    private fun loadWorkingMemory() {

        if (!workingMemoryFile.exists())
            return

        val type =
            object :
                TypeToken<
                        MutableMap<String, String>
                        >() {}.type

        val loaded =
            gson.fromJson<
                    MutableMap<String, String>
                    >(
                workingMemoryFile.readText(),
                type
            )

        memory.workingMemory.putAll(
            loaded
        )
    }

    private fun saveLongTermMemory() {

        longTermMemoryFile.writeText(
            gson.toJson(
                memory.longTermMemory
            )
        )
    }

    private fun loadLongTermMemory() {

        if (!longTermMemoryFile.exists())
            return

        val type =
            object :
                TypeToken<
                        MutableMap<String, String>
                        >() {}.type

        val loaded =
            gson.fromJson<
                    MutableMap<String, String>
                    >(
                longTermMemoryFile.readText(),
                type
            )

        memory.longTermMemory.putAll(
            loaded
        )
    }

    private fun longTermEntries() =
        memory.longTermMemory

    private fun workingEntries() =
        memory.workingMemory

    fun clearMemory() {

        memory.shortTermMemory.clear()
        memory.workingMemory.clear()
        memory.longTermMemory.clear()

        saveShortTermMemory()
        saveWorkingMemory()
        saveLongTermMemory()
    }

    fun getShortTermMemory(): List<ChatMessage> {

        return memory.shortTermMemory.toList()
    }

    fun getWorkingMemory(): Map<String, String> {

        return memory.workingMemory.toMap()
    }

    fun getLongTermMemory(): Map<String, String> {

        return memory.longTermMemory.toMap()
    }
}