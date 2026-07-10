package com.example.aichallenge

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.system.measureTimeMillis

class BenchmarkRunner(

    private val simpleAgent: SimpleAgent,

    private val ragAgent: RagAgent

) {

    suspend fun runBenchmark(
        provider: LlmProvider,
        onProgress: (Int, Int) -> Unit
    ): List<BenchmarkResult> {

        // Переключаем модель перед запуском
        when (provider) {
            LlmProvider.OPEN_ROUTER -> simpleAgent.useOpenRouter()
            LlmProvider.OLLAMA -> simpleAgent.useOllama()
        }

        val results = mutableListOf<BenchmarkResult>()

        val questions = BenchmarkQuestions.questions

        questions.forEachIndexed { index, question ->

            onProgress(
                index + 1,
                questions.size
            )

            var simpleAnswer = ""

            val simpleTime = measureTimeMillis {

                simpleAnswer = askSimpleAgent(
                    question.question
                )
            }

            var ragAnswer = ""

            val ragTime = measureTimeMillis {

                ragAnswer = askRagAgent(
                    question.question
                )
            }

            results.add(

                BenchmarkResult(

                    id = question.id,

                    question = question.question,

                    simpleAnswer = simpleAnswer,

                    ragAnswer = ragAnswer,

                    simpleTimeMs = simpleTime,

                    ragTimeMs = ragTime
                )
            )
        }

        return results
    }

    private suspend fun askSimpleAgent(

        question: String

    ): String =

        suspendCancellableCoroutine { continuation ->

            simpleAgent.processRequest(

                userRequest = question,

                onSuccess = {

                    continuation.resume(it)
                },

                onError = {

                    continuation.resume(it)
                }
            )
        }

    private suspend fun askRagAgent(

        question: String

    ): String =

        suspendCancellableCoroutine { continuation ->

            ragAgent.processRequest(

                question = question,

                onSuccess = {

                    continuation.resume(it)
                },

                onError = {

                    continuation.resume(it)
                }
            )
        }
}