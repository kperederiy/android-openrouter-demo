package com.example.aichallenge

import android.content.Context

class IndexSearcher(

    private val context: Context,

    private val embeddingService: EmbeddingService

) {

    suspend fun search(

        question: String,

        topK: Int = 10

    ): List<SearchResult> {

        val storage = IndexStorage(context)

        val index = storage.loadIndex()

        if (index == null) {
            android.util.Log.e("IndexSearcher", "Index is null")
            return emptyList()
        }

        if (index.chunks.isEmpty()) {
            android.util.Log.e("IndexSearcher", "Index has no chunks")
            return emptyList()
        }

        android.util.Log.d("IndexSearcher", "Searching for: $question")
        android.util.Log.d("IndexSearcher", "Total chunks in index: ${index.chunks.size}")

        val questionEmbedding =

            embeddingService.createEmbedding(question)

        android.util.Log.d("IndexSearcher", "Question embedding size: ${questionEmbedding.size}")

        val results = index.chunks

            .map { chunk ->

                val similarity =

                    CosineSimilarity.calculate(

                        questionEmbedding,

                        chunk.embedding
                    )

                android.util.Log.d(
                    "IndexSearcher",
                    "Chunk ${chunk.chunkId}: similarity = $similarity"
                )

                SearchResult(

                    chunk = chunk,

                    similarity = similarity
                )
            }

            .sortedByDescending {

                it.similarity
            }

            .take(topK)

        android.util.Log.d(
            "IndexSearcher",
            "Top similarity: ${results.firstOrNull()?.similarity ?: 0.0}"
        )

        return results
    }
}