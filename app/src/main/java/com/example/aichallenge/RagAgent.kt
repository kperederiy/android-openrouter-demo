package com.example.aichallenge

import android.content.Context
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

        threshold = 0.75,

        topKAfter = 3
    )

    fun processRequest(

        question: String,

        onSuccess: (String) -> Unit,

        onError: (String) -> Unit

    ) {

        Thread {

            try {

                //--------------------------------------------------
                // 1. Query Rewrite
                //--------------------------------------------------

                val rewrittenQuestion =

                    queryRewriter.rewrite(question)

                //--------------------------------------------------
                // 2. Vector Search
                //--------------------------------------------------

                val searchResults = runBlocking {

                    indexSearcher.search(

                        question = rewrittenQuestion,

                        topK = 10
                    )
                }

                //--------------------------------------------------
                // 3. Similarity Filter
                //--------------------------------------------------

                val filteredResults =

                    similarityFilter.filter(

                        searchResults
                    )

                //--------------------------------------------------
                // 4. Извлекаем Chunk
                //--------------------------------------------------

                val chunks =

                    filteredResults.map {

                        it.chunk
                    }

                //--------------------------------------------------
                // 5. Build Prompt
                //--------------------------------------------------

                val prompt =

                    promptBuilder.buildPrompt(

                        question = question,

                        chunks = chunks
                    )

                //--------------------------------------------------
                // 6. Ask LLM
                //--------------------------------------------------

                simpleAgent.processRequest(

                    userRequest = prompt,

                    onSuccess = onSuccess,

                    onError = onError
                )

            } catch (e: Exception) {

                onError(

                    e.message ?: "Ошибка RAG"
                )
            }

        }.start()
    }
}