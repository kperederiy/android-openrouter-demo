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

    private val similarityFilter = SimilarityFilter(

        threshold = 0.7,

        topKAfter = 5
    )

    fun processRequest(

        question: String,

        onSuccess: (String) -> Unit,

        onError: (String) -> Unit

    ) {

        Thread {

            try {

                Log.d(
                    "RagAgent",
                    "Processing question: $question"
                )

                //--------------------------------------------------
                // 1. Query Rewrite
                //--------------------------------------------------

                val rewrittenQuestion =

                    queryRewriter.rewrite(question)

                Log.d(
                    "RagAgent",
                    "Rewritten: $rewrittenQuestion"
                )

                //--------------------------------------------------
                // 2. Vector Search
                //--------------------------------------------------

                val searchResults = runBlocking {

                    indexSearcher.search(

                        question = rewrittenQuestion,

                        topK = 10
                    )
                }

                Log.d(
                    "RagAgent",
                    "Found ${searchResults.size} results"
                )

                //--------------------------------------------------
                // 3. Similarity Filter
                //--------------------------------------------------

                val filteredResults =

                    similarityFilter.filter(
                        searchResults
                    )

                Log.d(
                    "RagAgent",
                    "Filtered to ${filteredResults.size} results"
                )

                //--------------------------------------------------
                // 4. Извлекаем Chunk
                //--------------------------------------------------

                val chunks =

                    filteredResults.map {

                        it.chunk
                    }

                //--------------------------------------------------
                // 5. Если релевантных документов нет
                //--------------------------------------------------

                if (chunks.isEmpty()) {

                    Log.w(
                        "RagAgent",
                        "No relevant chunks found"
                    )

                    onSuccess(

                        """
Ответ:
Не знаю. Пожалуйста, уточните вопрос.

Источники:
нет

Цитаты:
нет
                        """.trimIndent()

                    )

                    return@Thread
                }

                //--------------------------------------------------
                // 6. Build Prompt
                //--------------------------------------------------

                val prompt =

                    promptBuilder.buildPrompt(

                        question = question,

                        chunks = chunks
                    )

                Log.d(
                    "RagAgent",
                    "Prompt length: ${prompt.length}"
                )

                //--------------------------------------------------
                // 7. Ask LLM
                //--------------------------------------------------

                simpleAgent.processRequest(

                    userRequest = prompt,

                    onSuccess = { answer ->

                        Log.d(
                            "RagAgent",
                            "LLM answered successfully"
                        )

                        onSuccess(answer)
                    },

                    onError = { error ->

                        Log.e(
                            "RagAgent",
                            error
                        )

                        onError(error)
                    }
                )

            } catch (e: Exception) {

                Log.e(
                    "RagAgent",
                    "Error",
                    e
                )

                onError(

                    e.message ?: "Ошибка RAG"
                )
            }

        }.start()
    }
}