package com.example.aichallenge

import android.content.Context
import com.google.gson.Gson
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

    private val userProfile = UserProfile()

    private var profileEnabled = true

    private val shortMemoryFile =
        File(context.filesDir, "short_memory.md")

    private val workingMemoryFile =
        File(context.filesDir, "working_memory.md")

    private val longTermMemoryFile =
        File(context.filesDir, "long_term_memory.md")

    private val profileFile =
        File(context.filesDir, "user_profile.md")

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

        updateUserProfile(userRequest)

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
                                    "MEMORY_LAYERS",
                                    profileEnabled
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

            if (profileEnabled) {

                append(
                    "USER PROFILE\n"
                )

                append(
                    "Name: ${userProfile.name}\n"
                )

                append(
                    "Style: ${userProfile.responseStyle}\n"
                )

                append(
                    "Format: ${userProfile.responseFormat}\n"
                )

                append(
                    "Restrictions: ${userProfile.restrictions}\n\n"
                )
            }

            append(
                "LONG TERM MEMORY\n"
            )

            memory.longTermMemory.forEach {

                append(
                    "${it.key}: ${it.value}\n"
                )
            }

            append("\n")

            append(
                "WORKING MEMORY\n"
            )

            memory.workingMemory.forEach {

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

        val markdown =
            buildString {

                append("# Short Term Memory\n\n")

                memory.shortTermMemory.forEach {

                    append(
                        "${it.role.uppercase()}: ${it.content}\n\n"
                    )
                }
            }

        shortMemoryFile.writeText(
            markdown
        )
    }

    private fun loadShortTermMemory() {

        if (!shortMemoryFile.exists())
            return

        val lines =
            shortMemoryFile.readLines()

        memory.shortTermMemory.clear()

        lines.forEach { line ->

            when {

                line.startsWith("USER:") -> {

                    memory.shortTermMemory.add(
                        ChatMessage(
                            role = "user",
                            content =
                                line.removePrefix(
                                    "USER:"
                                ).trim()
                        )
                    )
                }

                line.startsWith(
                    "ASSISTANT:"
                ) -> {

                    memory.shortTermMemory.add(
                        ChatMessage(
                            role = "assistant",
                            content =
                                line.removePrefix(
                                    "ASSISTANT:"
                                ).trim()
                        )
                    )
                }
            }
        }
    }

    private fun saveWorkingMemory() {

        val markdown =
            buildString {

                append("# Working Memory\n\n")

                memory.workingMemory.forEach {

                    append(
                        "- ${it.key}: ${it.value}\n"
                    )
                }
            }

        workingMemoryFile.writeText(
            markdown
        )
    }

    private fun loadWorkingMemory() {

        if (!workingMemoryFile.exists())
            return

        memory.workingMemory.clear()

        workingMemoryFile
            .readLines()
            .forEach { line ->

                if (
                    !line.startsWith("- ")
                ) return@forEach

                val content =
                    line.removePrefix("- ")

                val separator =
                    content.indexOf(":")

                if (separator == -1)
                    return@forEach

                val key =
                    content.substring(
                        0,
                        separator
                    ).trim()

                val value =
                    content.substring(
                        separator + 1
                    ).trim()

                memory.workingMemory[key] =
                    value
            }
    }

    private fun saveLongTermMemory() {

        val markdown =
            buildString {

                append(
                    "# Long Term Memory\n\n"
                )

                memory.longTermMemory.forEach {

                    append(
                        "- ${it.key}: ${it.value}\n"
                    )
                }
            }

        longTermMemoryFile.writeText(
            markdown
        )
    }

    private fun loadLongTermMemory() {

        if (!longTermMemoryFile.exists())
            return

        memory.longTermMemory.clear()

        longTermMemoryFile
            .readLines()
            .forEach { line ->

                if (
                    !line.startsWith("- ")
                ) return@forEach

                val content =
                    line.removePrefix("- ")

                val separator =
                    content.indexOf(":")

                if (separator == -1)
                    return@forEach

                val key =
                    content.substring(
                        0,
                        separator
                    ).trim()

                val value =
                    content.substring(
                        separator + 1
                    ).trim()

                memory.longTermMemory[key] =
                    value
            }
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

    private fun saveUserProfile() {

        val markdown =
            buildString {

                append("# User Profile\n\n")

                append(
                    "- name: ${userProfile.name}\n"
                )

                append(
                    "- response_style: ${userProfile.responseStyle}\n"
                )

                append(
                    "- response_format: ${userProfile.responseFormat}\n"
                )

                append(
                    "- restrictions: ${userProfile.restrictions}\n"
                )
            }

        profileFile.writeText(
            markdown
        )
    }

    private fun loadUserProfile() {

        if (!profileFile.exists())
            return

        profileFile.readLines()
            .forEach { line ->

                if (
                    !line.startsWith("- ")
                )
                    return@forEach

                val content =
                    line.removePrefix("- ")

                val separator =
                    content.indexOf(":")

                if (separator == -1)
                    return@forEach

                val key =
                    content.substring(
                        0,
                        separator
                    ).trim()

                val value =
                    content.substring(
                        separator + 1
                    ).trim()

                when(key) {

                    "name" ->
                        userProfile.name = value

                    "response_style" ->
                        userProfile.responseStyle =
                            value

                    "response_format" ->
                        userProfile.responseFormat =
                            value

                    "restrictions" ->
                        userProfile.restrictions =
                            value
                }
            }
    }

    private fun updateUserProfile(
        text: String
    ) {

        val lower =
            text.lowercase()

        if (
            lower.contains("меня зовут")
        ) {

            userProfile.name = text
        }

        if (
            lower.contains("отвечай кратко")
        ) {

            userProfile.responseStyle =
                "concise"
        }

        if (
            lower.contains("отвечай подробно")
        ) {

            userProfile.responseStyle =
                "detailed"
        }

        if (
            lower.contains("используй списки")
        ) {

            userProfile.responseFormat =
                "list"
        }

        if (
            lower.contains("без кода")
        ) {

            userProfile.restrictions =
                "no_code"
        }

        saveUserProfile()
    }

    fun enableUserProfile() {

        profileEnabled = true
    }

    fun disableUserProfile() {

        profileEnabled = false
    }

    fun isUserProfileEnabled(): Boolean {

        return profileEnabled
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