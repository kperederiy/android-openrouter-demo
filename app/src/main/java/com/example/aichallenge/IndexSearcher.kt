package com.example.aichallenge

import android.content.Context

class IndexSearcher(

    private val context: Context,

    private val embeddingService: EmbeddingService

) {

    suspend fun search(

        question: String,

        topK: Int = 3

    ): List<Chunk> {

        val storage = IndexStorage(context)

        val index = storage.loadIndex()

        if (index == null) {
            return emptyList()
        }

        if (index.chunks.isEmpty()) {
            return emptyList()
        }

        val questionEmbedding =
            embeddingService.createEmbedding(question)

        val scoredChunks =
            index.chunks.map { chunk ->

                val similarity =
                    CosineSimilarity.calculate(

                        questionEmbedding,

                        chunk.embedding
                    )

                chunk to similarity
            }

        return scoredChunks

            .sortedByDescending { pair ->

                pair.second

            }

            .take(topK)

            .map { pair ->

                pair.first

            }
    }
}