package com.example.aichallenge

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
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

        private const val MAX_RECENT_MESSAGES = 10
    }

    private val client = OkHttpClient()

    private val gson = Gson()

    private val historyType =
        object : TypeToken<MutableList<ChatMessage>>() {}.type

    private val historyFile =
        File(context.filesDir, "history.json")

    private val branchesFile =
        File(
            context.filesDir,
            "branches.json"
        )


    private val factsFile =
        File(
            context.filesDir,
            "facts.json"
        )

    private var currentStrategy =
        MemoryStrategy.SLIDING_WINDOW

    private val messages =
        mutableListOf<ChatMessage>()

    private val facts =
        mutableMapOf<String, String>()

    private val branches =
        mutableMapOf<String, DialogBranch>()

    private var currentBranchId = "main"

    init {

        loadHistory()

        loadFacts()

        loadBranches()

        if (!branches.containsKey("main")) {

            branches["main"] =
                DialogBranch(
                    id = "main",
                    messages =
                        messages.toMutableList()
                )

            saveBranches()
        }
    }

    fun processRequest(
        userRequest: String,
        onSuccess: (
            answer: String,
            stats: AgentStats
        ) -> Unit,
        onError: (String) -> Unit
    ) {

        messages.add(
            ChatMessage(
                role = "user",
                content = userRequest
            )
        )

        updateFacts(userRequest)

        saveHistory()

        val json = JSONObject()

        json.put(
            "model",
            MODEL
        )

        val messagesArray = JSONArray()

        when(currentStrategy) {

            MemoryStrategy.SLIDING_WINDOW -> {

                messages
                    .takeLast(MAX_RECENT_MESSAGES)
                    .forEach {
                        messagesArray.put(
                            JSONObject().apply {

                                put("role", it.role)
                                put("content", it.content)
                            }
                        )
                    }
            }

            MemoryStrategy.STICKY_FACTS -> {

                if(facts.isNotEmpty()) {

                    messagesArray.put(
                        JSONObject().apply {

                            put(
                                "role",
                                "system"
                            )

                            put(
                                "content",
                                buildFactsPrompt()
                            )
                        }
                    )
                }

                messages
                    .takeLast(MAX_RECENT_MESSAGES)
                    .forEach {
                        messagesArray.put(
                            JSONObject().apply {

                                put("role", it.role)
                                put("content", it.content)
                            }
                        )
                    }
            }

            MemoryStrategy.BRANCHING -> {

                messages.forEach {
                    messagesArray.put(
                        JSONObject().apply {

                            put("role", it.role)
                            put("content", it.content)
                        }
                    )
                }
            }
        }

        when(currentStrategy) {

            MemoryStrategy.SLIDING_WINDOW ->
                applySlidingWindow()

            MemoryStrategy.STICKY_FACTS -> {

                applySlidingWindow()
            }

            MemoryStrategy.BRANCHING -> {

                branches[currentBranchId]
                    ?.messages
                    ?.apply {

                        clear()
                        addAll(messages)
                    }
            }
        }

        json.put(
            "messages",
            messagesArray
        )

        val body = RequestBody.create(
            "application/json".toMediaType(),
            json.toString()
        )

        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/chat/completions")
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
            .enqueue(object : Callback {

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

                        val jsonObject =
                            JSONObject(responseBody)

                        val answer =
                            jsonObject
                                .getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content")

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

                        messages.add(
                            ChatMessage(
                                role = "assistant",
                                content = answer
                            )
                        )

                        if (
                            currentStrategy ==
                            MemoryStrategy.SLIDING_WINDOW
                        ) {
                            applySlidingWindow()
                        }

                        if (
                            currentStrategy ==
                            MemoryStrategy.BRANCHING
                        ) {

                            branches[currentBranchId] =
                                DialogBranch(
                                    id = currentBranchId,
                                    messages =
                                        messages.toMutableList()
                                )

                            saveBranches()
                        }

                        saveHistory()

                        val historyTokens =
                            estimateHistoryTokens()

                        val cost =
                            promptTokens *
                                    INPUT_PRICE_PER_TOKEN +
                                    completionTokens *
                                    OUTPUT_PRICE_PER_TOKEN

                        val usagePercent =
                            ((historyTokens.toDouble()
                                    / CONTEXT_WINDOW) * 100)
                                .toInt()

                        val warning =
                            when {
                                usagePercent >= 95 ->
                                    "❌ Контекст почти переполнен"

                                usagePercent >= 80 ->
                                    "⚠ История занимает $usagePercent% контекста"

                                else ->
                                    "Контекст в норме"
                            }

                        val stats =
                            AgentStats(
                                promptTokens =
                                    promptTokens,

                                completionTokens =
                                    completionTokens,

                                totalTokens =
                                    totalTokens,

                                historyTokens =
                                    historyTokens,

                                estimatedCost =
                                    cost,

                                contextUsagePercent =
                                    usagePercent,

                                contextWarning =
                                    warning,

                                strategy =
                                    currentStrategy.name
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
            })
    }

    private fun estimateHistoryTokens(): Int {

        return messages.sumOf {

            val words =
                it.content
                    .trim()
                    .split("\\s+".toRegex())
                    .size

            (words * 1.3).toInt()
        }
    }

    private fun saveHistory() {

        try {

            historyFile.writeText(
                gson.toJson(messages)
            )

        } catch (e: Exception) {

            e.printStackTrace()
        }
    }

    private fun loadHistory() {

        try {

            if (!historyFile.exists())
                return

            val loadedMessages:
                    MutableList<ChatMessage> =
                gson.fromJson(
                    historyFile.readText(),
                    historyType
                )

            messages.clear()

            messages.addAll(
                loadedMessages
            )

        } catch (e: Exception) {

            e.printStackTrace()
        }
    }
    fun setStrategy(
        strategy: MemoryStrategy
    ) {
        currentStrategy = strategy
    }

    fun createCheckpoint(
        branchName: String
    ) {

        branches[branchName] =
            DialogBranch(
                id = branchName,
                messages =
                    messages.toMutableList()
            )

        saveBranches()
    }

    fun switchBranch(
        branchName: String
    ) {

        branches[currentBranchId] =
            DialogBranch(
                id = currentBranchId,
                messages =
                    messages.toMutableList()
            )

        val branch =
            branches[branchName]
                ?: return

        messages.clear()

        messages.addAll(
            branch.messages
        )

        currentBranchId =
            branchName

        saveHistory()
    }
    private fun loadFacts() {

        if (!factsFile.exists())
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
                factsFile.readText(),
                type
            )

        facts.putAll(loaded)
    }

    private fun saveFacts() {

        factsFile.writeText(
            gson.toJson(facts)
        )
    }

    private fun updateFacts(
        userText: String
    ) {

        val text =
            userText.lowercase()

        if (
            text.contains("меня зовут")
        ) {

            facts["name"] =
                userText
        }

        if (
            text.contains("моя цель")
        ) {

            facts["goal"] =
                userText
        }

        if (
            text.contains("бюджет")
        ) {

            facts["budget"] =
                userText
        }



        if (
            text.contains("срок")
        ) {

            facts["deadline"] =
                userText
        }



        if (
            text.contains("используем")
        ) {

            facts["technology"] =
                userText
        }



        if (
            text.contains("ограничение")
        ) {

            facts["restriction"] =
                userText
        }



        if (
            text.contains("решили")
        ) {

            facts["decision"] =
                userText
        }

        saveFacts()
    }

    private fun buildFactsPrompt(): String {

        return buildString {

            append(
                "Известные факты:\n"
            )

            facts.forEach {

                append(
                    "${it.key}: ${it.value}\n"
                )
            }
        }
    }

    private fun applySlidingWindow() {

        if (
            messages.size <= MAX_RECENT_MESSAGES
        ) return

        val recent =
            messages.takeLast(MAX_RECENT_MESSAGES)

        messages.clear()

        messages.addAll(recent)
    }

    fun loadDemoConversation() {

        val demoMessages = listOf(

            "Нужно собрать ТЗ на приложение доставки еды.",
            "Проект делаем под Android.",
            "Используем Kotlin и Jetpack Compose.",
            "Бюджет ограничен 5000 долларов.",
            "Важно выпустить MVP за 2 месяца.",
            "Нужна регистрация по номеру телефона.",
            "Оплата через Stripe.",
            "Нужна история заказов.",
            "Админ-панель пока не нужна.",
            "Главная цель — проверить спрос.",
            "Интерфейс должен быть простым.",
            "Целевая аудитория — студенты.",
            "Основной рынок — Амстердам.",
            "Запомни все требования.",
            "Сформируй краткое ТЗ."
        )

        messages.clear()

        demoMessages.forEach {

            messages.add(
                ChatMessage(
                    role = "user",
                    content = it
                )
            )
        }

        saveHistory()
    }

    private fun loadBranches() {

        try {

            if (!branchesFile.exists())
                return

            val type =
                object :
                    TypeToken<
                            MutableMap<String, DialogBranch>
                            >() {}.type

            val loaded:
                    MutableMap<String, DialogBranch> =
                gson.fromJson(
                    branchesFile.readText(),
                    type
                )

            branches.clear()

            branches.putAll(loaded)

        } catch (e: Exception) {

            e.printStackTrace()
        }
    }

    private fun saveBranches() {

        try {

            branchesFile.writeText(
                gson.toJson(branches)
            )

        } catch (e: Exception) {

            e.printStackTrace()
        }
    }

    fun getBranches(): List<String> {

        return branches.keys.toList()
    }

    fun getCurrentBranch(): String {

        return currentBranchId
    }

    fun createBranchFromCurrent(
        branchName: String
    ) {

        branches[branchName] =
            DialogBranch(
                id = branchName,
                messages =
                    messages.toMutableList()
            )

        saveBranches()
    }

}