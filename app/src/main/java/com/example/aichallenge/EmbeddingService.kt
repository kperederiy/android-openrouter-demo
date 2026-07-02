package com.example.aichallenge

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class EmbeddingService(

    private val apiKey: String
) {

    companion object {

        private const val MODEL =
            "openai/text-embedding-3-small"

        private const val URL =
            "https://openrouter.ai/api/v1/embeddings"
    }

    private val client = OkHttpClient()

    private val json = Json {

        ignoreUnknownKeys = true
    }

    suspend fun createEmbedding(
        chunk: Chunk
    ): Chunk = withContext(Dispatchers.IO) {

        val requestBody = EmbeddingRequest(

            model = MODEL,

            input = chunk.text
        )

        val body = json
            .encodeToString(requestBody)
            .toRequestBody(
                "application/json".toMediaType()
            )

        val request = Request.Builder()

            .url(URL)

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
            .execute()
            .use { response ->

                if (!response.isSuccessful) {

                    throw Exception(
                        "HTTP ${response.code}"
                    )
                }

                val responseBody =
                    response.body?.string()
                        ?: throw Exception(
                            "Пустой ответ сервера"
                        )

                val embeddingResponse =
                    json.decodeFromString<EmbeddingResponse>(
                        responseBody
                    )

                val embedding =
                    embeddingResponse
                        .data
                        .first()
                        .embedding

                chunk.copy(

                    embedding = embedding
                )
            }
    }

    suspend fun createEmbedding(
        text: String
    ): List<Float> = withContext(Dispatchers.IO) {

        val requestBody = EmbeddingRequest(

            model = MODEL,

            input = text
        )

        val body = json
            .encodeToString(requestBody)
            .toRequestBody(
                "application/json".toMediaType()
            )

        val request = Request.Builder()

            .url(URL)

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
            .execute()
            .use { response ->

                if (!response.isSuccessful) {

                    throw Exception(
                        "HTTP ${response.code}"
                    )
                }

                val responseBody =
                    response.body?.string()
                        ?: throw Exception(
                            "Пустой ответ сервера"
                        )

                val embeddingResponse =
                    json.decodeFromString<EmbeddingResponse>(
                        responseBody
                    )

                embeddingResponse
                    .data
                    .first()
                    .embedding
            }
    }
}