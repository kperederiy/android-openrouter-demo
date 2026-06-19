package com.example.aichallenge

import android.content.Context
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

    private val client = OkHttpClient()

    private val memory = MemoryLayers()

    private val userProfile = UserProfile()

    private val taskContext = TaskContext()

    private val stateMachine = TaskStateMachine()

    private var profileEnabled = true

    private val shortMemoryFile =
        File(context.filesDir, "short_memory.md")

    private val workingMemoryFile =
        File(context.filesDir, "working_memory.md")

    private val longTermMemoryFile =
        File(context.filesDir, "long_term_memory.md")

    private val profileFile =
        File(context.filesDir, "user_profile.md")

    private val taskContextFile =
        File(context.filesDir, "task_context.md")

    init {

        loadShortTermMemory()

        loadWorkingMemory()

        loadLongTermMemory()

        loadUserProfile()

        loadTaskContext()
    }

    fun processRequest(
        userRequest: String,
        updateTask: Boolean = true,
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

        if (updateTask) {
            updateCurrentTask(userRequest)
        }

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

                            taskContext.lastResult =
                                answer

                            saveTaskContext()

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

        val stateInstruction = when(taskContext.currentState) {

            TaskState.PLANNING -> """

CURRENT PHASE: PLANNING

Goal:
Collect requirements and create an implementation plan.

Last completed step:
${taskContext.lastResult}

Expected action:
- analyze requirements
- ask clarifying questions if needed
- create detailed implementation plan
- DO NOT write production code yet
- wait until transition to EXECUTION

"""

            TaskState.EXECUTION -> """

CURRENT PHASE: EXECUTION

Goal:
Implement the approved plan.

Last completed step:
${taskContext.lastResult}

Expected action:
- write code
- create files and artifacts
- follow the approved plan
- do NOT return to planning
- do NOT ask for requirements again unless critical information is missing

"""

            TaskState.VALIDATION -> """

CURRENT PHASE: VALIDATION

Goal:
Validate implementation quality.

Last completed step:
${taskContext.lastResult}

Expected action:
- review generated code
- identify defects
- verify compliance with plan
- create test scenarios
- suggest fixes if problems found

"""

            TaskState.DONE -> """

CURRENT PHASE: DONE

Goal:
Task is completed.

Last completed step:
${taskContext.lastResult}

Expected action:
- summarize result
- list completed work
- describe final outcome
- do not generate new implementation steps

"""
        }

        return buildString {

            if (profileEnabled) {

                append(
                    """
USER PROFILE

Name: ${userProfile.name}
Style: ${userProfile.responseStyle}
Format: ${userProfile.responseFormat}
Restrictions: ${userProfile.restrictions}

"""
                )
            }

            append(
                """
TASK CONTEXT

Current State:
${taskContext.currentState}

Current Task:
${taskContext.currentTask}

$stateInstruction

"""
            )

            append("LONG TERM MEMORY\n")

            memory.longTermMemory.forEach {

                append("${it.key}: ${it.value}\n")
            }

            append("\n")

            append("WORKING MEMORY\n")

            memory.workingMemory.forEach {

                append("${it.key}: ${it.value}\n")
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

        taskContext.currentTask = ""

        taskContext.lastResult = ""

        taskContext.paused = false

        taskContext.currentState =
            TaskState.PLANNING

        saveShortTermMemory()

        saveWorkingMemory()

        saveLongTermMemory()

        saveTaskContext()

        userProfile.name = ""

        userProfile.responseStyle =
            "neutral"

        userProfile.responseFormat =
            "detailed"

        userProfile.restrictions = ""

        saveUserProfile()
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

    private fun saveTaskContext() {

        val markdown =
            buildString {

                append("# Task Context\n\n")

                append(
                    "- state: ${taskContext.currentState}\n"
                )

                append(
                    "- task: ${taskContext.currentTask}\n"
                )

                append(
                    "- result: ${taskContext.lastResult}\n"
                )

                append(
                    "- paused: ${taskContext.paused}\n"
                )
            }

        taskContextFile.writeText(
            markdown
        )
    }

    private fun loadTaskContext() {

        if (!taskContextFile.exists())
            return

        taskContextFile
            .readLines()
            .forEach { line ->

                if (!line.startsWith("- "))
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

                    "state" -> {

                        taskContext.currentState =
                            TaskState.valueOf(value)
                    }

                    "task" -> {

                        taskContext.currentTask =
                            value
                    }

                    "result" -> {

                        taskContext.lastResult =
                            value
                    }

                    "paused" -> {

                        taskContext.paused =
                            value.toBoolean()
                    }
                }
            }
    }

    fun getCurrentState(): TaskState {

        return taskContext.currentState
    }

    fun isPaused(): Boolean {

        return taskContext.paused
    }

    fun moveForward() {

        if (
            !stateMachine.canMoveForward(
                taskContext.currentState
            )
        ) {
            return
        }

        taskContext.currentState =
            stateMachine.next(
                taskContext.currentState
            )

        saveTaskContext()
    }

    fun moveBackward() {

        if (
            !stateMachine.canMoveBack(
                taskContext.currentState
            )
        ) {
            return
        }

        taskContext.currentState =
            stateMachine.previous(
                taskContext.currentState
            )

        saveTaskContext()
    }

    fun pauseTask() {

        taskContext.paused = true

        saveTaskContext()
    }

    fun resumeTask() {

        taskContext.paused = false

        saveTaskContext()
    }

    private fun updateCurrentTask(
        request: String
    ) {

        taskContext.currentTask =
            request

        saveTaskContext()
    }

    fun canMoveForward(): Boolean {

        return stateMachine.canMoveForward(
            taskContext.currentState
        )
    }

    fun canMoveBack(): Boolean {

        return stateMachine.canMoveBack(
            taskContext.currentState
        )
    }

    fun getTaskContext(): TaskContext {

        return taskContext.copy()
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

    fun getDebugPrompt(): String {

        return buildMemoryPrompt()
    }
}