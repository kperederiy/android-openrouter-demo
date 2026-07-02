package com.example.aichallenge

import android.content.Context

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

    fun processRequest(

        question: String,

        onSuccess: (String) -> Unit,

        onError: (String) -> Unit

    ) {

        Thread {

            try {

                val chunks = kotlinx.coroutines.runBlocking {

                    indexSearcher.search(

                        question = question,

                        topK = 3
                    )
                }

                val prompt = promptBuilder.buildPrompt(

                    question = question,

                    chunks = chunks
                )

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