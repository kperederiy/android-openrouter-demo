package com.example.aichallenge

import android.content.Context
import android.util.Log
import kotlinx.coroutines.runBlocking
class RagAgent(
    private val context: Context,
    private val embeddingService: EmbeddingService,
    private val simpleAgent: SimpleAgent
) {
    private val indexSearcher = IndexSearcher(
        context = context,
        embeddingService = embeddingService
    )
    private val promptBuilder = PromptBuilder()
    private val queryRewriter = QueryRewriter()

    // Снижаем порог до 0.3
    private val similarityFilter = SimilarityFilter(
        threshold = 0.3,  // Значительно снижаем порог
        topKAfter = 3
    )

    private val chatMemory = ChatMemory()

    fun processRequest(
        question: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {
                Log.d("RagAgent", "Processing question: $question")
                chatMemory.addUserMessage(question)

                val rewrittenQuestion = queryRewriter.rewrite(question)
                Log.d("RagAgent", "Rewritten: $rewrittenQuestion")

                val searchResults = runBlocking {
                    indexSearcher.search(
                        question = rewrittenQuestion,
                        topK = 10
                    )
                }

                Log.d("RagAgent", "Found ${searchResults.size} results")

                // Логируем максимальное сходство
                if (searchResults.isNotEmpty()) {
                    val maxSim = searchResults.maxOf { it.similarity }
                    Log.d("RagAgent", "Max similarity: $maxSim")
                }

                // Фильтруем результаты
                val filteredResults = similarityFilter.filter(searchResults)
                Log.d("RagAgent", "Filtered to ${filteredResults.size} results")

                val chunks = filteredResults.map { it.chunk }

                // ✅ ИСПРАВЛЕНО: Если нет релевантных чанков - возвращаем "Не знаю"
                // БЕЗ вызова SimpleAgent как fallback
                if (chunks.isEmpty()) {
                    Log.w("RagAgent", "No relevant chunks found (similarity < threshold)")

                    val answer = """
Ответ:
Не знаю. Пожалуйста, уточните вопрос.

Источники:
нет

Цитаты:
нет
                    """.trimIndent()

                    chatMemory.addAssistantMessage(answer)
                    onSuccess(answer)
                    return@Thread
                }

                // Строим промпт ТОЛЬКО с релевантными чанками
                val prompt = promptBuilder.buildPrompt(
                    question = question,
                    history = chatMemory.getMessages(),
                    chunks = chunks
                )

                Log.d("RagAgent", "Prompt length: ${prompt.length}")

                // Используем SimpleAgent ТОЛЬКО для генерации ответа на основе контекста
                simpleAgent.processRequest(
                    userRequest = prompt,
                    onSuccess = { answer ->
                        Log.d("RagAgent", "LLM answered successfully")
                        chatMemory.addAssistantMessage(answer)
                        onSuccess(answer)
                    },
                    onError = { error ->
                        Log.e("RagAgent", error)
                        onError(error)
                    }
                )

            } catch (e: Exception) {
                Log.e("RagAgent", "Error", e)
                onError(e.message ?: "Ошибка RAG")
            }
        }.start()
    }

    fun clearMemory() {
        chatMemory.clear()
    }

    fun getHistory(): List<ChatMessage> {
        return chatMemory.getMessages()
    }
}